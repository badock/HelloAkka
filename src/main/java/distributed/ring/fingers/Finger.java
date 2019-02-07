package distributed.ring.fingers;

import distributed.utils.ActorRefWithId;
import distributed.utils.Utils;

import java.math.BigInteger;

/**
 * Created by jonathan on 12/31/18.
 */
public class Finger {
    public final BigInteger start;
    public final BigInteger end;
    private ActorRefWithId currentNode;

    public Finger(BigInteger start, BigInteger end, boolean isLastFinger) {
        this.start = start;
        this.end = end;
    }

    public void setNode(ActorRefWithId node) {
        this.currentNode = node;
    }

    public ActorRefWithId getNode() {
        return this.currentNode;
    }

    public boolean updateIfCloserToStart(ActorRefWithId refWithId) {
        boolean updateFinger = false;

        if (currentNode == null) {
            updateFinger = true;
        } else {
            if(Utils.isInCircularInterval(refWithId.Id, start, currentNode.Id, true, false)) {
                updateFinger = true;
            }
        }

        if (updateFinger) {
            this.currentNode = refWithId;
        }

        return updateFinger;
    }
}
