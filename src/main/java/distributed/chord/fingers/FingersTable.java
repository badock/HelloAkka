package distributed.chord.fingers;

import akka.actor.ActorRef;
import distributed.chord.util.ActorRefWithId;
import distributed.chord.util.Utils;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * Created by jonathan on 12/1/18.
 */
public class FingersTable {

    public ArrayList<Finger> fingers;
    private ActorRefWithId selfRef;
    private ActorRefWithId predecessor;

    public FingersTable(ActorRefWithId selfRef) {
        // Init attributes
        this.fingers = new ArrayList<Finger>();
        this.selfRef = selfRef;
        this.predecessor = selfRef;

        // Init fingers
        BigInteger start = selfRef.Id;
        int fingerSize = Utils.getModuloFactor();
        for (int i=0; i<fingerSize; i++) {
            Finger newFinger;
            if (i < (fingerSize-1)) {
                BigInteger end = start.add(new BigInteger("2").pow(i)).mod(Utils.getModulo());
                newFinger  = new Finger(start, end, false);
                start = end;
            } else {
                newFinger = new Finger(start, selfRef.Id.subtract(new BigInteger("1")), true);
            }
            fingers.add(newFinger);
        }
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
        return this.fingers.get(0).getNode();
    }

    public ActorRefWithId getClosestPrecedingFinger(BigInteger id) {
        for (int i=fingers.size()-1; i>=0; i--) {
            Finger finger = fingers.get(i);
            if (finger.getNode() != null) {
                BigInteger finger_id = finger.getNode().Id;
                if(Utils.isInCircularInterval(finger_id, this.selfRef.Id, id, true, false)) {
                    return finger.getNode();
                }
            }
        }
        return selfRef;
    }

    public boolean updateIfRelevant(ActorRefWithId refWithId) {
        boolean fingerUpdated = false;
        for(Finger finger : fingers) {
            fingerUpdated = finger.updateIfCloserToStart(refWithId) || fingerUpdated;
        }
        if(fingerUpdated) {
            System.out.println(this.selfRef.Id+"> I found a new finger!");
        }
        if (this.predecessor == null || this.predecessor.Id.equals(this.selfRef.Id)) {
            this.predecessor = refWithId;
        }

        if (Utils.isInCircularInterval(refWithId.Id, this.predecessor.Id, this.selfRef.Id, false, false)) {
            this.predecessor = refWithId;
        }

        return fingerUpdated;
    }
}
