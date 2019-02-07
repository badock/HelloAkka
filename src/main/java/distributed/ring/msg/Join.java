package distributed.ring.msg;

import distributed.utils.ActorRefWithId;

import java.io.Serializable;

/**
 * Created by jonathan on 11/21/18.
 */
public class Join implements Serializable {
    public ActorRefWithId ref;
    public Join(ActorRefWithId ref) {
        this.ref = ref;
    }
}
