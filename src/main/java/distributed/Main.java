package distributed;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.Config;
import distributed.config.ConfigurationGenerator;
import distributed.msg.DiscoverNewPeer;

import java.io.Serializable;

/**
 * Created by jonathan on 11/21/18.
 */

public class Main {

    public static void client(String[] args) {
        System.out.println("Launching client");

        // make a Config with just your special setting
        Config myConfig = ConfigFactory.parseString(ConfigurationGenerator.generateConfig("127.0.0.1", args[1]));

        // load the normal config stack (system props, then application.conf, then reference.conf)
        Config regularConfig = ConfigFactory.load();

        // override regular stack with myConfig
        Config combined = myConfig.withFallback(regularConfig);

        // put the result in between the overrides (system props) and defaults again
        Config complete = ConfigFactory.load(combined);

        // create ActorSystem
        final ActorSystem system = ActorSystem.create("hello_akka", complete);

        final ActorRef greeter1 =
                system.actorOf(Peer.props("greeter2>"), "greeter2");

        String remote_address = args[2];
        String remote_port = args[3];

        ActorSelection selection =
                system.actorSelection("akka.tcp://hello_akka@"+remote_address+":"+remote_port+"/user/greeter1");

        selection.tell(new DiscoverNewPeer(), greeter1);
    }

    public static void server(String[] args) {
        System.out.println("Launching server");

        // make a Config with just your special setting
        String port = args[1];
        Config myConfig = ConfigFactory.parseString(ConfigurationGenerator.generateConfig("127.0.0.1", port));

        // load the normal config stack (system props, then application.conf, then reference.conf)
        Config regularConfig = ConfigFactory.load();

        // override regular stack with myConfig
        Config combined = myConfig.withFallback(regularConfig);

        // put the result in between the overrides (system props) and defaults again
        Config complete = ConfigFactory.load(combined);

        // create ActorSystem
        final ActorSystem system = ActorSystem.create("hello_akka", complete);

        final ActorRef greeter1 =
                system.actorOf(Peer.props("greeter1>"), "greeter1");
    }

    public static void main(String[] args) {
//        String[] args1 = new String[2];
//        args1[0] = "server";
//        args1[1] = "1234";
//        args = args1;
        if (args.length == 2 && args[0].equals("server")) {
            server(args);
        } else if (args.length == 4 && args[0].equals("client")) {
            client(args);
        } else {
            System.out.println("Run the program with the following arguments:");
            System.out.println("  * java Main server <local_port>");
            System.out.println("  * java Main client <local_port> <remote_address> <remote_port>");
        }
    }
}
