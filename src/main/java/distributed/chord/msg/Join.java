package distributed.chord.msg;

import distributed.chord.util.ActorRefWithId;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by jonathan on 11/21/18.
 */
public class Join implements Serializable {
    public ActorRefWithId ref;
    public Join(ActorRefWithId ref) {
        this.ref = ref;
    }
}
