package distributed.chord.msg;

import distributed.utils.ActorRefWithId;

import java.io.Serializable;

/**
 * Created by jonathan on 11/21/18.
 */
public class Notify implements Serializable {
    public ActorRefWithId refWithId;
    public Notify(ActorRefWithId refWithId) {
        this.refWithId = refWithId;
    }
}
