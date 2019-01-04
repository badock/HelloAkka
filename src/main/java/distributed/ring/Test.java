package distributed.ring;

import java.util.concurrent.TimeUnit;
//import

/**
 * Created by jonathan on 12/30/18.
 */
public class Test {
    public static void main(String[] args) {
        System.out.println("Testing with many peers");

        // Create server
        int server_port = 9000;
        String[] server_args = new String[]{"server", "127.0.0.1", Integer.toString(server_port)};
        Main.server(server_args);

        // Create few clients
        int nbClients = 20;
        for(int i=1; i<nbClients+1; i++) {
            int client_port = server_port + i;
            String[] client_args = new String[]{"client", "127.0.0.1", Integer.toString(client_port), "127.0.0.1", Integer.toString(server_port)};
            Main.client(client_args);
        }

        while(true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
