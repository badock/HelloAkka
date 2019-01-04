package distributed.chord.util;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Created by jonathan on 12/31/18.
 */
public class Utils {

    public static int getModuloFactor() {
        return 16;
    }

    public static BigInteger getModulo() {
        return new BigInteger("2").pow(getModuloFactor());
    }

    public static BigInteger addressToUniqueIntegerIdentifer(String entry) throws java.security.NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(entry.getBytes());
        byte[] digest = md.digest();

        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        BigInteger bigInt = new BigInteger(hexString.toString(), 16);
        BigInteger result = bigInt.mod(getModulo());
        return result;
    }

    public static boolean isInCircularInterval(BigInteger n, BigInteger start, BigInteger end, boolean includingStart, boolean includingEnd) {
        boolean isInInterval;
        if (start.compareTo(end) == -1 && start.compareTo(n) <= 0 && end.compareTo(n) >= 0) {
            isInInterval = true;
        } else if (start.compareTo(end) == 1 && (start.compareTo(n) <= 0 || end.compareTo(n) >= 0)) {
            isInInterval = true;
        } else {
            isInInterval = false;
        }

        if (!includingStart && n.compareTo(start) == 0) {
            isInInterval = false;
        }
        if (!includingEnd && n.compareTo(end) == 0) {
            isInInterval = false;
        }
        return isInInterval;
    }

    public static void main(String[] args) {
        BigInteger id1 = new BigInteger("252398656909985898163143036404709987016164614721");
        BigInteger id2 = new BigInteger("765867561945405489417444973306495942222311117014");
        BigInteger id3 = new BigInteger("852581913717387862641134322056355051643470248154");

        System.out.println(!isInCircularInterval(id1, id2, id3, false, false));
        System.out.println(isInCircularInterval(id1, id3, id2, false, false));

        System.out.println(isInCircularInterval(id2, id1, id3, false, false));
        System.out.println(!isInCircularInterval(id2, id3, id1, false, false));

        System.out.println(isInCircularInterval(id3, id2, id1, false, false));
        System.out.println(!isInCircularInterval(id3, id1, id2, false, false));


        System.out.println(!isInCircularInterval(id1, id1, id2, false, false));
        System.out.println(isInCircularInterval(id1, id1, id2, true, false));
        System.out.println(!isInCircularInterval(id2, id1, id2, false, false));
        System.out.println(isInCircularInterval(id2, id1, id2, false, true));

    }
}
