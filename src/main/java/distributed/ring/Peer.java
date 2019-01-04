package distributed.ring;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.util.Timeout;
import distributed.ring.fingers.Finger;
import distributed.ring.fingers.FingersTable;
import distributed.ring.msg.*;
import distributed.ring.util.ActorRefWithId;
import distributed.ring.util.Utils;
import scala.compat.java8.FutureConverters;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executor;

import static akka.pattern.Patterns.ask;

public class Peer extends AbstractActor {

    static public Props props(BigInteger message) {
        return Props.create(Peer.class, () -> new Peer(message));
    }

    private final FingersTable fingers;
    private final ActorRefWithId selfRef;

    private Timeout timeout;

    public Peer(BigInteger actorId) {
        this.selfRef = new ActorRefWithId(actorId, self());
        timeout = Timeout.create(Duration.ofSeconds(5));

        // Init fingers
        this.fingers = new FingersTable(selfRef);

        // Schedule periodical events
        Peer selfReference = this;
        ActorSystem thisSystem = selfReference.getContext().getSystem();
        thisSystem.scheduler().schedule(
            Duration.ofMillis(1000),
            Duration.ofMillis(1000),
            () -> {
                selfReference.debug();
                selfReference.stabilize();
                selfReference.fixFinger();
                selfReference.notifySuccessor();

            }, thisSystem.dispatcher());
    }

    public void debug() {
        String msg = ("=== DEBUG: " + this.selfRef.Id+"\n");

        msg += "                                                            -- ";
        msg += "pred:  "+this.fingers.getPredecessor().Id;
        msg += "\n";
        msg += "                                                            -- ";
        msg += "succ:  "+this.fingers.getSuccessor().Id;

        System.out.println(msg);
    }

    public void join(ActorRefWithId nodeRef) {
        this.fingers.setPredecessor(null);

        final Executor ex = this.getContext().getSystem().dispatcher();

        Future<ActorRefWithId> future = findSuccessor(nodeRef.Id);
        FutureConverters.toJava(future).thenApplyAsync(successorRef -> {
            this.fingers.updateIfRelevant(successorRef);
            return true;
        }, ex);
    }

    public void stabilize() {
        ActorRefWithId successor = fingers.getSuccessor();
        if (successor != null) {
            final Executor ex = this.getContext().getSystem().dispatcher();
            Future<Object> futurePredecessor = ask(successor.ref, new GetPredecessor(), this.timeout);
            FutureConverters.toJava(futurePredecessor).thenApplyAsync(op -> {
                ActorRefWithId predecessorCandidateRef = (ActorRefWithId) op;

                if (Utils.isInCircularInterval(predecessorCandidateRef.Id, selfRef.Id, successor.Id, false, false)) {
                    this.fingers.updateIfRelevant(predecessorCandidateRef);
                    predecessorCandidateRef.ref.tell(new Notify(this.selfRef), this.self());
                }

                return true;
            }, ex);
        }
    }

    public void fixFinger() {
        Random random = new Random();
        Future<ActorRefWithId> futureSuccessor = findSuccessor(selfRef.Id);
        FutureConverters.toJava(futureSuccessor).thenApplyAsync(successor -> {
            this.fingers.updateIfRelevant(successor);
            return true;
        });
    }

    public void notifySuccessor() {
        ActorRefWithId successor = this.fingers.getSuccessor();
        if (successor != null) {
            successor.ref.tell(new Notify(this.selfRef), getSender());
        }
    }

    public void notify(ActorRefWithId predecessorCandidateRef) {
        ActorRefWithId predecessor = fingers.getPredecessor();
        if (predecessor == null || Utils.isInCircularInterval(predecessorCandidateRef.Id, predecessor.Id, this.selfRef.Id, false, false)) {
            this.fingers.setPredecessor(predecessorCandidateRef);
        }
    }

    public Future<ActorRefWithId> findSuccessor(BigInteger id) {
        Promise<ActorRefWithId> promiseResult = Futures.promise();

        final Executor ex = this.getContext().getSystem().dispatcher();
        Future<ActorRefWithId> future = findPredecessor(id);
        FutureConverters.toJava(future).thenApplyAsync(op -> {
            ActorRefWithId predecessorRef = (ActorRefWithId) op;

            Future<Object> futureSuccessor = ask(predecessorRef.ref, new GetSuccessor(), this.timeout);
            FutureConverters.toJava(futureSuccessor).thenApplyAsync(ocpf -> {
                ActorRefWithId successorRef = (ActorRefWithId) ocpf;
                promiseResult.success(successorRef);
                return true;
            }, ex);

            return true;
        }, ex);

        return promiseResult.future();
    }

    public Future<ActorRefWithId> findPredecessor(ActorRefWithId origin, BigInteger id) {
        final ExecutionContext ec = this.getContext().getSystem().dispatcher();
        ActorRefWithId remoteRef = fingers.getSuccessor();

        Promise<ActorRefWithId> promiseResult = Futures.promise();

        final Executor ex = this.getContext().getSystem().dispatcher();
        Future<Object> future = ask(remoteRef.ref, new GetSuccessor(), this.timeout);
        FutureConverters.toJava(future).thenApplyAsync(os -> {
            ActorRefWithId successorRef = (ActorRefWithId) os;

            boolean predecessor_found = false;

            if (Utils.isInCircularInterval(id, origin.Id, successorRef.Id, true, true)) {
                predecessor_found = true;
            }

            if (predecessor_found) {
                Future<Object> futureClosestPrecedingFinger = ask(remoteRef.ref, new ClosestPrecedingFinger(id), this.timeout);
                FutureConverters.toJava(futureClosestPrecedingFinger).thenApplyAsync(ocpf -> {
                    ActorRefWithId closestPrecedingFinger = (ActorRefWithId) ocpf;
                    promiseResult.success(closestPrecedingFinger);
                    return true;
                }, ex);
            } else {
                promiseResult.success(origin);
            }
            return true;
        }, ex);

        return promiseResult.future();
    }

    public Future<ActorRefWithId> findPredecessor(BigInteger id) {
        return findPredecessor(this.selfRef, id);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(IWantToJoin.class, msg -> {
                    System.out.println(this.selfRef.Id + "> found new peer: " + getSender());
                    ActorRefWithId refWithId = new ActorRefWithId(msg.id, getSender());
                    boolean updated = fingers.updateIfRelevant(refWithId);
                    getSender().tell(new Join(this.selfRef), self());
                })
                .match(Join.class, msg -> {
                    this.fingers.updateIfRelevant(msg.ref);
                    this.fingers.setPredecessor(msg.ref);
                    this.join(msg.ref);
                })
                .match(Notify.class, msg -> {
                    fingers.updateIfRelevant(msg.refWithId);
                    notify(msg.refWithId);
                })
                .match(GetSuccessor.class, msg -> {
                    ActorRefWithId successor = fingers.getSuccessor();
                    if (successor != null) {
                        getSender().tell(successor, self());
                    } else {
                        System.out.println(this.selfRef.Id + "> no successor");
                    }
                })
                .match(GetPredecessor.class, msg -> {
                    ActorRefWithId predecessor = fingers.getPredecessor();
                    if (predecessor != null) {
                        getSender().tell(predecessor, self());
                    } else {
                        System.out.println(this.selfRef.Id + "> no predecessor");
                    }
                })
                .match(ClosestPrecedingFinger.class, msg -> {
                    getSender().tell(fingers.getSuccessor(), self());
                })
                .build();
    }
}
