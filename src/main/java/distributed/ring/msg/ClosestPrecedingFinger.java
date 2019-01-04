package distributed.ring.msg;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by jonathan on 11/21/18.
 */
public class ClosestPrecedingFinger implements Serializable {
    public BigInteger id;
    public ClosestPrecedingFinger(BigInteger id) {
        this.id = id;
    }
}
