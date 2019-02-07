package distributed.simple;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import distributed.simple.msg.DiscoverNewPeer;
import distributed.simple.msg.Greeting;
import distributed.utils.LoggingService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;

public class Peer extends AbstractActor {

    static public Props props(String message) {
        return Props.create(Peer.class, () -> new Peer(message));
    }

    private final String peerName;
    private final ArrayList<ActorRef> peers = new ArrayList<ActorRef>();

    public Peer(String name) {
        this.peerName = name;
        distributed.simple.Peer selfReference = this;
        ActorSystem thisSystem = selfReference.getContext().getSystem();
        thisSystem.scheduler().schedule(
                Duration.ofMillis(1000),
                Duration.ofMillis(1000),
                () -> {
                    selfReference.debug();

                }, thisSystem.dispatcher());
    }

    public void debug() {
        String msg = "";
        ArrayList<String> identifiers = new ArrayList<String>();
//        ActorRef selfRef = self();

//        String myAddress = self().path().name()+"@"+self().path().address().host().get()+":"+self().path().address().port().get();

        for (ActorRef ref : this.peers) {
//            String identifier = ref.path().name()+"@"+ref.path().address().host().get()+":"+ref.path().address().port().get();
            identifiers.add(ref.path().name());
        }

        for (String s : identifiers) {
            msg += "  "+ peerName + " -> " + s + ";\n";
        }

        LoggingService.updatePeerDescription(peerName, msg);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DiscoverNewPeer.class, newPeer -> {
                    System.out.println(this.peerName + "> found new peer: " + getSender());
                    this.peers.add(getSender());
                    getSender().tell(new Greeting("Hello!"), getSelf());
                })
                .match(Greeting.class, msg -> {
                    System.out.println(this.peerName + "> " + getSender() + " told me '" + msg.message + "'");
                    TimeUnit.SECONDS.sleep(1);
                    getSender().tell(new Greeting("Hello again!"), getSelf());
                })
                .build();
    }
}
