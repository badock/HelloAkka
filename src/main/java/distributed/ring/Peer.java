package distributed.ring;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.util.Timeout;
import distributed.ring.fingers.KnowledgeTable;
import distributed.ring.msg.*;
import distributed.utils.ActorRefWithId;
import distributed.utils.Utils;
import distributed.utils.LoggingService;
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

    private final KnowledgeTable fingers;
    private final ActorRefWithId selfRef;

    private Timeout timeout;

    public Peer(BigInteger actorId) {
        this.selfRef = new ActorRefWithId(actorId, self());
        timeout = Timeout.create(Duration.ofSeconds(5));

        // Init fingers
        this.fingers = new KnowledgeTable(selfRef);

        // Schedule periodical events
        Peer selfReference = this;
        ActorSystem thisSystem = selfReference.getContext().getSystem();
        thisSystem.scheduler().schedule(
            Duration.ofMillis(1000),
            Duration.ofMillis(1000),
            () -> {
                selfReference.debug();
                selfReference.stabilize();
            }, thisSystem.dispatcher());
    }

    public void debug() {
        String msg = "";
        ArrayList<BigInteger> identifiers = new ArrayList<BigInteger>();

        if (fingers.getPredecessor() != null && !identifiers.contains(fingers.getPredecessor().Id)) {
            identifiers.add(fingers.getPredecessor().Id);
        }

        if (fingers.getSuccessor() != null && !identifiers.contains(fingers.getSuccessor().Id)) {
            identifiers.add(fingers.getSuccessor().Id);
        }

        for (BigInteger i : identifiers) {
            msg += "  "+this.selfRef.Id+ " -> " + i + ";\n";
        }

        LoggingService.setTopology("ring");
        LoggingService.updatePeerDescription(""+this.selfRef.Id, msg);
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

    public void notify(ActorRefWithId predecessorCandidateRef) {
        ActorRefWithId predecessor = fingers.getPredecessor();
        if (predecessor == null || Utils.isInCircularInterval(predecessorCandidateRef.Id, predecessor.Id, this.selfRef.Id, false, false)) {
            this.fingers.setPredecessor(predecessorCandidateRef);
        }
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
                .build();
    }
}
