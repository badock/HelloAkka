package distributed;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import distributed.msg.DiscoverNewPeer;
import distributed.msg.Greeting;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;

public class Peer extends AbstractActor {
    //#greeter-messages
    static public Props props(String message) {
        return Props.create(Peer.class, () -> new Peer(message));
    }

    private final String peerName;
    private final ArrayList<ActorRef> peers = new ArrayList<ActorRef>();

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
                .match(Greeting.class, msg -> {
                    System.out.println(this.peerName+"> "+getSender()+" told me '"+msg.message+"'");
                    TimeUnit.SECONDS.sleep(1);
                    getSender().tell(new Greeting("Hello again!"), getSelf());
                })
                .build();
    }
}
