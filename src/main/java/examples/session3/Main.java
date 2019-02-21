package examples.session3;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.util.Timeout;
import distributed.ring.Peer;
import examples.session3.msg.Response;
import examples.session3.msg.RunFoo;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.concurrent.Executor;

import static akka.pattern.Patterns.ask;

public class Main {
    public static void main(String[] args) {
        // create ActorSystem
        final ActorSystem system = ActorSystem.create("hello_akka");

        // create Bob
        ActorRef bob = system.actorOf(BobActor.props(), "bob");
        Timeout timeout = Timeout.create(Duration.ofSeconds(5));

        // Simulate Alice sending run-foo to Bob
        Future<Object> futureResponse = ask(bob, new RunFoo(), timeout);

        // Process asynchronously the response
        final Executor ex = system.dispatcher();
        FutureConverters.toJava(futureResponse).thenApplyAsync(responseObj -> {
            Response response = (Response) responseObj;
            System.out.println("bob's response: "+response.x);
            return true;
        }, ex);
    }
}
