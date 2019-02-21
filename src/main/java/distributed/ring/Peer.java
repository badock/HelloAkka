package distributed.ring;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.util.Timeout;
import distributed.ring.neighbours.Neighbourhood;
import distributed.ring.msg.*;
import distributed.utils.ActorRefWithId;
import distributed.utils.Utils;
import distributed.utils.LoggingService;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import static akka.pattern.Patterns.ask;

public class Peer extends AbstractActor {

    static public Props props(BigInteger message) {
        return Props.create(Peer.class, () -> new Peer(message));
    }

    private final Neighbourhood neighbours;
    private final ActorRefWithId selfRef;

    private Timeout timeout;

    public Peer(BigInteger actorId) {
        this.selfRef = new ActorRefWithId(actorId, self());
        timeout = Timeout.create(Duration.ofSeconds(5));

        // Init neighbours
        this.neighbours = new Neighbourhood(selfRef);

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

        if (neighbours.getPredecessor() != null && !identifiers.contains(neighbours.getPredecessor().Id)) {
            identifiers.add(neighbours.getPredecessor().Id);
        }

        if (neighbours.getSuccessor() != null && !identifiers.contains(neighbours.getSuccessor().Id)) {
            identifiers.add(neighbours.getSuccessor().Id);
        }

        for (BigInteger i : identifiers) {
            msg += "  "+this.selfRef.Id+ " -> " + i + ";\n";
        }

        LoggingService.setTopology("ring");
        LoggingService.updatePeerDescription(""+this.selfRef.Id, msg);
    }

    public void stabilize() {
        ActorRefWithId successor = neighbours.getSuccessor();
        if (successor != null) {
            // Get an execution context for executing the future
            final Executor ex = this.getContext().getSystem().dispatcher();
            // Get a future that will resolve to the response of 'sucessor'
            Future<Object> futureX = ask(successor.ref, new GetPredecessor(), this.timeout);
            // Process the future asynchronously
            FutureConverters.toJava(futureX).thenApplyAsync(op -> {
                // Cast the response (Java Object) to an ActorRefWithId
                ActorRefWithId xRef = (ActorRefWithId) op;
                // Check if current node is between 'x' and 'successor'
                if (Utils.isInCircularInterval(xRef.Id, selfRef.Id, successor.Id, false, false)) {
                    // Current node may not know 'predecessorCandidateRef'
                    this.neighbours.updateIfRelevant(xRef);
                    // Other nodes may not know me
                    successor.ref.tell(new Notify(this.selfRef), this.self());
                    xRef.ref.tell(new Notify(this.selfRef), this.self());
                }
                return true;
            }, ex);
        }
    }

    public void notify(ActorRefWithId predecessorCandidateRef) {
        ActorRefWithId predecessor = neighbours.getPredecessor();
        if (predecessor == null || Utils.isInCircularInterval(predecessorCandidateRef.Id, predecessor.Id, this.selfRef.Id, false, false)) {
            this.neighbours.setPredecessor(predecessorCandidateRef);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(IWantToJoin.class, msg -> {
                    System.out.println(this.selfRef.Id + "> found new peer: " + getSender());
                    ActorRefWithId refWithId = new ActorRefWithId(msg.id, getSender());
                    boolean updated = neighbours.updateIfRelevant(refWithId);
                    getSender().tell(new Join(this.selfRef), self());
                })
                .match(Join.class, msg -> {
                    this.neighbours.updateIfRelevant(msg.ref);
                    this.neighbours.setPredecessor(msg.ref);
                })
                .match(Notify.class, msg -> {
                    neighbours.updateIfRelevant(msg.refWithId);
                    notify(msg.refWithId);
                })
                .match(GetSuccessor.class, msg -> {
                    ActorRefWithId successor = neighbours.getSuccessor();
                    if (successor != null) {
                        getSender().tell(successor, self());
                    } else {
                        System.out.println(this.selfRef.Id + "> no successor");
                    }
                })
                .match(GetPredecessor.class, msg -> {
                    ActorRefWithId predecessor = neighbours.getPredecessor();
                    if (predecessor != null) {
                        getSender().tell(predecessor, self());
                    } else {
                        System.out.println(this.selfRef.Id + "> no predecessor");
                    }
                })
                .build();
    }
}
