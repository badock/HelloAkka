package distributed.getting_started;

import akka.actor.AbstractActor;
import akka.actor.Props;

/**
 * Created by jonathan on 2/10/19.
 */
public class SampleActor extends AbstractActor {

    private String name;

    // Function that enables to create actors
    static public Props props(String name) {
        return Props.create(SampleActor.class,
                () -> new SampleActor(name));
    }

    // Constructor
    public SampleActor(String name) {
        this.name = name;
    }

    // Message handling loop
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, msg -> {
                    System.out.println(this.name+" --> "+msg);
                    String replyMsg;
                    if (this.name.equals("bob")) {
                        replyMsg = "ping";
                    } else {
                        replyMsg = "pong";
                    }
                    sender().tell(replyMsg, self());
                })
                .build();
    }
}