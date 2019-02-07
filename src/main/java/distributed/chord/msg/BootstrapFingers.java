package distributed.chord.msg;

import distributed.utils.ActorRefWithId;

import java.io.Serializable;

/**
 * Created by jonathan on 12/31/18.
 */
public class BootstrapFingers implements Serializable {
    public final ActorRefWithId predecessor;
    public final ActorRefWithId successor;

    public BootstrapFingers(ActorRefWithId predecessor, ActorRefWithId successor) {
        this.predecessor = predecessor;
        this.successor = successor;
    }
}
