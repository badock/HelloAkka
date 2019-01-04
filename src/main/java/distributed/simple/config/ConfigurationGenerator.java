package distributed.simple.config;

/**
 * Created by jonathan on 11/21/18.
 */
public class ConfigurationGenerator {
    public static String generateConfig(String address, String port) {
        return "akka {\n" +
                "  actor {\n" +
                "    provider = remote\n" +
                "    enable-additional-serialization-bindings = on\n"+
                "  }\n" +
                "  remote {\n" +
                "    enabled-transports = [\"akka.remote.netty.tcp\"]\n" +
                "    netty.tcp {\n" +
                "      hostname = \""+address+"\"\n" +
                "      port = "+port+"\n" +
                "    }\n" +
                "  }\n" +
                "} ";
    }
}
