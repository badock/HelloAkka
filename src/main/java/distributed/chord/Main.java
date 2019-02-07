package distributed.chord;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import distributed.chord.config.ConfigurationGenerator;
import distributed.chord.msg.IWantToJoin;
import distributed.utils.Utils;

import java.math.BigInteger;

/**
 * Created by jonathan on 11/21/18.
 */

public class Main {

    public static void client(String[] args) {
        System.out.println("Launching client");

        // make a Config with just your special setting
        String address = args[1];
        String port = args[2];
        Config myConfig = ConfigFactory.parseString(ConfigurationGenerator.generateConfig(address, port));

        // load the normal config stack (system props, then application.conf, then reference.conf)
        Config regularConfig = ConfigFactory.load();

        // override regular stack with myConfig
        Config combined = myConfig.withFallback(regularConfig);

        // put the result in between the overrides (system props) and defaults again
        Config complete = ConfigFactory.load(combined);

        // create ActorSystem
        final ActorSystem system = ActorSystem.create("hello_akka", complete);

        try {
            final BigInteger actorId = Utils.addressToUniqueIntegerIdentifer(address+"_"+port);
            final ActorRef greeter1 =
                    system.actorOf(Peer.props(actorId), "peerActor");

            String remote_address = args[3];
            String remote_port = args[4];

            ActorSelection selection =
                    system.actorSelection("akka.tcp://hello_akka@"+remote_address+":"+remote_port+"/user/peerActor");

            selection.tell(new IWantToJoin(actorId), greeter1);
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void server(String[] args) {
        System.out.println("Launching server");

        // make a Config with just your special setting
        String address = args[1];
        String port = args[2];
        Config myConfig = ConfigFactory.parseString(ConfigurationGenerator.generateConfig(address, port));

        // load the normal config stack (system props, then application.conf, then reference.conf)
        Config regularConfig = ConfigFactory.load();

        // override regular stack with myConfig
        Config combined = myConfig.withFallback(regularConfig);

        // put the result in between the overrides (system props) and defaults again
        Config complete = ConfigFactory.load(combined);

        // create ActorSystem
        final ActorSystem system = ActorSystem.create("hello_akka", complete);

        try {
            final BigInteger actorId = Utils.addressToUniqueIntegerIdentifer(address+"_"+port);
            final ActorRef greeter1 =
                    system.actorOf(Peer.props(actorId), "peerActor");

        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length == 3 && args[0].equals("server")) {
            server(args);
        } else if (args.length == 5 && args[0].equals("client")) {
            client(args);
        } else {
            System.out.println("Run the program with the following arguments:");
            System.out.println("  * java Main server <local_address> <local_port>");
            System.out.println("  * java Main client <local_address> <local_port> <remote_address> <remote_port>");
        }
    }
}
