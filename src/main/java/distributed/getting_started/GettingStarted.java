package distributed.getting_started;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 * Created by jonathan on 2/10/19.
 */
public class GettingStarted {

    public static void main(String args[]) {

        final ActorSystem system = ActorSystem.create("hello_akka");

        final ActorRef sampleActorRef =
                system.actorOf(SampleActor.props("bob"), "actor1");
        final ActorRef sampleActorRef2 =
                system.actorOf(SampleActor.props("alice"), "actor2");

        sampleActorRef.tell("ping", sampleActorRef2);
    }
}
