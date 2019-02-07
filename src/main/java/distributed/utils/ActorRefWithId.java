package distributed.utils;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by jonathan on 12/31/18.
 */
public class ActorRefWithId implements Serializable {
    public final BigInteger Id;
    public final ActorRef ref;

    public ActorRefWithId(BigInteger Id, ActorRef ref) {
        this.Id = Id;
        this.ref = ref;
    }
}
