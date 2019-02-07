package distributed.ring.fingers;

import distributed.utils.ActorRefWithId;
import distributed.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * Created by jonathan on 12/1/18.
 */
public class KnowledgeTable {

    private ActorRefWithId selfRef;
    private ActorRefWithId predecessor;
    private ActorRefWithId successor;


    public KnowledgeTable(ActorRefWithId selfRef) {
        // Init attributes
        this.selfRef = selfRef;
        this.predecessor = selfRef;
        this.successor = selfRef;
    }

    public void setPredecessor(ActorRefWithId node) {
        this.predecessor = node;
        if (node != null) {
            this.updateIfRelevant(node);
        }
    }

    public ActorRefWithId getPredecessor() {
        return this.predecessor;
    }

    public ActorRefWithId getSuccessor() {
        return this.successor;
    }

    public boolean updateIfRelevant(ActorRefWithId refWithId) {

        boolean updated = false;

        if (this.predecessor == null || this.predecessor.Id.equals(this.selfRef.Id)) {
            this.predecessor = refWithId;
            updated = true;
        }

        if (Utils.isInCircularInterval(refWithId.Id, this.predecessor.Id, this.selfRef.Id, false, false)) {
            this.predecessor = refWithId;
            updated = true;
        }

        if (this.successor == null || this.successor.Id.equals(this.selfRef.Id)) {
            this.successor = refWithId;
            updated = true;
        }

        if (Utils.isInCircularInterval(refWithId.Id, this.selfRef.Id, this.successor.Id, false, false)) {
            this.successor = refWithId;
            updated = true;
        }

        return updated;
    }
}
