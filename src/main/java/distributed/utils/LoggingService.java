package distributed.utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by jonathan on 2/6/19.
 */
public class LoggingService {
    private static HashMap<String, String> infrastructureDescription = new HashMap<String, String>();
    private static String outputFileName = "topology.txt";
    private static String topology = "flat";

    public static void updatePeerDescription(String peerID, String peerDescription) {
        infrastructureDescription.put(peerID, peerDescription);
        generateGraphiz();
    }

    public static void setTopology(String newTopology) {
        topology = newTopology;
    }

    public static void generateGraphiz() {

        List<String> types = infrastructureDescription.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .map(v -> new BigInteger(v))
                .collect(Collectors.toList())
                .stream()
                .sorted(Comparator.naturalOrder())
                .map(v -> v.toString())
                .collect(Collectors.toList());


        double radius = 2 / Math.sin(2 * Math.PI / types.size());
        double graphSize = radius * 2;

        String result = "digraph world {\n" +
                "  size=\"" + graphSize + ", " + graphSize + "\";\n";

        result += "  graph [layout = neato];\n";

        if (topology.equals("ring")) {
            int i = 0;
            for(String id: types) {
                double inRadians = 2.0 * Math.PI * ((1.0 * i) / types.size());
                double sine = Math.sin(inRadians);
                double cose = Math.cos(inRadians);
                double x = sine * radius;
                double y = cose * radius;
                result += id + "  [pos=\"" + x + "," + y + "!\"] ;\n";
                i ++;
            }
        }

        for(Map.Entry<String, String> e : infrastructureDescription.entrySet()) {
            result += "  " + e.getValue();
        }
        result += "}";

        try {
            PrintWriter out = new PrintWriter(outputFileName);
            out.println(result);
            out.close();

        } catch (FileNotFoundException e) {
            System.err.println("Could not open file '"+outputFileName+"'");
        }

    }
}
