package distributed;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import distributed.msg.DiscoverNewPeer;
import distributed.msg.Greeting;

import java.util.ArrayList;

public class Peer extends AbstractActor {
    //#greeter-messages
    static public Props props(String message) {
        return Props.create(Peer.class, () -> new Peer(message));
    }

//    //#greeter-messages
//    static public class DiscoverNewPeer {
//        public DiscoverNewPeer() {
//        }
//    }

    static public class Greet {
        public final String message;
        public Greet(String message) {
            this.message = message;
        }
    }

    static public class Broadcast {
        public final String message;
        public Broadcast(String message) {
            this.message = message;
        }
    }
    //#greeter-messages

    private final String peerName;
    private final ArrayList<ActorRef> peers = new ArrayList<ActorRef>();
    private String greeting = "";

    public Peer(String message) {
        this.peerName = message;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DiscoverNewPeer.class, newPeer -> {
                    System.out.println(this.peerName+"> found new peer: "+getSender());
                    this.peers.add(getSender());
                    getSender().tell(new Greeting("Hello!"), getSelf());
                })
                .match(Broadcast.class, msg -> {
                    for (ActorRef ref: this.peers) {
                        System.out.println(this.peerName+"> I am sending a message to: "+ref+" -> "+msg.message);
                        ref.tell(new Greeting(msg.message), getSelf());
                    }
                })
                .match(Greeting.class, msg -> {
                    System.out.println(this.peerName+"> "+getSender()+" told me '"+msg.message+"'");
                })
                .build();
    }
}
