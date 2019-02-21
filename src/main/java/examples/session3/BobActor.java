package examples.session3;

import akka.actor.AbstractActor;
import akka.actor.Props;
import examples.session3.msg.Response;
import examples.session3.msg.RunFoo;

/**
 * Created by jonathan on 2/21/19.
 */
public class BobActor extends AbstractActor {

    static public Props props() {
        return Props.create(BobActor.class, () -> new BobActor());
    }


    public BobActor() {
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RunFoo.class, msg -> {
                    // getSender() -> provides an ActorRef to the actor that have emitted the message
                    // getSelf() -> provides an ActorRef to the current actor
                    getSender().tell(new Response(42), getSelf());
                })
                .build();
    }
}
