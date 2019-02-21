---
title: Session 3 - Toward a distributed application based on Chord
excerpt: "SeDuCe is a research project that aims at building a scientific testbed to enable the study of both thermal and power management aspects in datacenters."
order: 1
---

<style>
#java-and-akka {
    font-size: 3em;
    color: white;
}

.feature {
    color: white;
}
</style>

In this session, we will reuse the principles introduced in the
previous session, and provide a structure to distributed application
we are creating, organized around a Ring topology. In a first time,
the ring topology will be simple : each peer will have a predecessor
and a successor, and the successor of the last peer will the first
peer. In a second time, this topology will evolve so that, in addition
to remembering its predecessor and sucessor, each peer will also
remember few other peers.

### I- Considerations on what is a ring of peers

The following figure depicts a ring containing 5 peers : in this
topology each peer has an ID, and a predecessor and a successor.
There is an order between peers' IDS : for a given peer, the ID of the
predecessor must be inferior to its ID, and the ID of the successor
must be greater that its ID, except for the last and first peer
(resp. peers _57496_ and _30938_ in the following figure) who are both
sucessors and predecessors.

![/assets/img/session3/ring.png](/assets/img/session3/ring.png)

To ensure that our topology will always be a ring, we define a
_stabilize_ function that will periodically ensure that IDs of peers
are ordered. We naively define the _stabilize_ function of peer with
_n_ ID, in the following pseudo-code:

```lua
function n.stabilize()
    x = n.successor.predecessor
    if n.id included_in [x.id, n.successor.id] then
        -- 'n' learned that 'x' exists
        n.update_neighbors(x)
        -- other nodes should learn that 'n' exists
        x.update_neighbors(n)
        n.successor.update_neighbors(n)
    end
end
```

In the previous snippet, the _update_neighbors_ function is in charge
of checking if a given peer is more relevant than existing predecessor
and successor of the peer with _n_ ID. We can define it as follow:

```lua
function n.update_neighbors(x)
    if x.id included_in [n.predecessor.id, n.id] then
        n.predecessor = x
    end
    if x.id included_in [n.id, n.successor.id] then
        n.successor = x
    end
end
```

With these two functions, we are able to build a ring topology, in
which peers will find their place.

### II- _"Ask pattern"_ in Akka : answer and response between two actors

Akka provides a pattern for synchronous messaging which is called
"[Ask pattern](https://doc.akka.io/docs/akka/2.5/actors.html)". It
enables to express situations where an actor need a response from an
other actor, such as in the following picture:

![/assets/img/session3/ring.png](https://g.gravizo.com/svg?
@startuml;
title \n Answer and response;
Alice -> Bob: [step1] run-foo;
activate Bob;
Bob -> Bob: [step2] int x = foo();
Bob -> Alice: [step3] response(x);
deactivate Bob;
@enduml
)


We will now implement this example. To do so, we first define the
protocol by two classes that represent the messages exchanged by the
actors:

```java
import java.io.Serializable;

public class RunFoo implements Serializable {
    public RunFoo() {
    }
}
```

```java
import java.io.Serializable;

public class Response implements Serializable {
    public int x;
    public Response(int x) {
        this.x = x;
    }
}
```

To implement _[step1]_, __Alice__ can call the _ask_ function to send
a message to Bob and expect a response from _Bob_:

```java
import akka.util.Timeout;
import static akka.pattern.Patterns.ask;

Timeout timeout = Timeout.create(Duration.ofSeconds(5));
Future<Object> futureResponse = ask(bob, new RunFoo(), timeout)
```

In _[step2]_ __Bob__ responds to __Alice__ by sending an instance of
the _Reponse_ class, whose _x_ attribute has been set to 42. This
behavior can be implemented by adding the following code to _Bob's_
message processing loop:

```java
// [...]
.match(RunFoo.class, msg -> {
    // getSender() -> provides an ActorRef to the actor that have emitted the message
    // getSelf() -> provides an ActorRef to the current actor
    getSender().tell(new Response(42), getSelf());
})
// [...]
```

In _[step3]_, __Alice__ receives and processes the answer from
__Bob__. While _Alice_ get a future "futureResponse" at the end of
_[step1]_, as Bob did not send yet any reponse, the result of the
futureResponse is "uncertain" yet. _Alice_ tackle this problem by
providing a function that should be applied to the response that _Bob_
will send.

```java
// Get the dispatcher of the current actor system
final Executor ex = this.getContext().getSystem().dispatcher();

FutureConverters.toJava(futureResponse).thenApplyAsync(responseObj -> {
    Response response = (Response) responseObj;
    System.out.println("bob's response: "+response.x);
    return true;
}, ex);
```

The code show in this Section can be found on this [Github
repository](https://github.com/badock/HelloAkka/tree/master/src/main/java/examples/session3). To
test it, just run the
[Main](https://github.com/badock/HelloAkka/blob/master/src/main/java/examples/session3/Main.java)
class.

### III- Building a Ring

As seen in [section I](#i--considerations-on-what-is-a-ring-of-peers),
we can maintain a ring structure if periodically we check that each
peer of the topology is at its correct place. We have proposed an
algorith that implements such a mechanism. You can find the code of
this Section on this [Github
repository](https://github.com/badock/HelloAkka/tree/master/src/main/java/distributed/ring).

First, we give an implementation of the __stabilize__ function that
levarages the _Ask pattern_ provided by Akka:

```java
public void stabilize() {
    ActorRefWithId successor = neighbours.getSuccessor();
    if (successor != null) {
        // Get an execution context for executing the future
        final Executor ex = this.getContext().getSystem().dispatcher();
        // Get a future that will resolve to the response of 'sucessor'
        Future<Object> futureX = ask(successor.ref, new GetPredecessor(), this.timeout);
        // Process the future asynchronously
        FutureConverters.toJava(futureX).thenApplyAsync(op -> {
            // Cast the response (Java Object) to an ActorRefWithId
            ActorRefWithId xRef = (ActorRefWithId) op;
            // Check if current node is between 'x' and 'successor'
            if (Utils.isInCircularInterval(xRef.Id, selfRef.Id, successor.Id, false, false)) {
                // Current node may not know 'predecessorCandidateRef'
                this.neighbours.updateIfRelevant(xRef);
                // Other nodes may not know me
                successor.ref.tell(new Notify(this.selfRef), this.self());
                xRef.ref.tell(new Notify(this.selfRef), this.self());
            }
            return true;
        }, ex);
    }
}
```

Each 


### IV- Implementing a Chord like topology


## Next step: Running the ring on Grid'5000

Congratulations! You can now proceed with the [next session](/session4)
