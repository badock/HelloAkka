package distributed.chord.msg;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by jonathan on 11/21/18.
 */
public class IWantToJoin implements Serializable {
    public BigInteger id;
    public IWantToJoin(BigInteger id) {
        this.id = id;
    }
}
