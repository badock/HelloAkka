---
title: Session 2 - Developing a simple master/slave application
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

In this session, we will reuse the concepts introduced in the [session
1](/session1) to build a simple master/slave distributed system. While
there are two roles (one master and several slaves), masters and
slaves will run the same program. In a sense, this application can be
seen as a _"peer to peer like"_ application.

### I- Architecture of the application

The following figure illustrates what we want to achieve in this session:

![/assets/img/session2/master_slave.png](/assets/img/session2/master_slave.png)

In this example, the distributed system contains five peers, one of the peer is the master node (30938), and four peers are slaves (41537, 57496, 31003, 53462).

We would like that:
1. The master node registers slave nodes
2. A new slave node should greet the master node
3. When any node (master and slavers) receive a message, they should wait one second and greet again to the node that have contacted them.


### II- How to implement such a system with Akka?

This example as been implemented and pushed on Github in the [HelloAkka repository](https://github.com/badock/HelloAkka), in the [src/main/java/distributed/simple](https://github.com/badock/HelloAkka/tree/master/src/main/java/distributed/simple) folder.

First, we need to define the protocol used by actors to communicate with each other. In the previous session, actors were exchanging _String values_, while this is sufficient for a simple system, it becomes complicated to exchange messages that contain several fields of value. An alternative is to define serializable classe that will contains the attributes of the message, such as the [DiscoverNewPeer](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/msg/DiscoverNewPeer.java) and [Greeting](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/msg/Greeting.java) classes:

1. _DiscoverNewPeer_: is a message sent by a new slave peer to the
   master peer
   ([here](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/Main.java#L51)),
   in order to join the distributed system. It does not contain any
   field, and its purpose is to provide an _ActorRef_ of the new slave
   to the master node, so that the master can send messages to the new
   slave.
2. _Greeting_: is a message that contains a value (such as "hello" or
   "hello again"). Master and slaves can rely on this class to
   exchange messages, such as when [the master contact a new
   slave](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/Peer.java#L59)
   and when [a master or a slave reply respectively to a slave or to
   the
   master](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/Peer.java#L64)

In order to implement this distributed system, the master will need to remember who are the peer that have established a contact with him. We implement this beahavior by adding a list of peers to the master:

```java
private final ArrayList<ActorRef> peers = new ArrayList<ActorRef>();
```

and adding a reference to a joining peer to this list as follow:
```java
.match(DiscoverNewPeer.class, newPeer -> {
   this.peers.add(getSender()); // (statement 1)
   getSender().tell(new Greeting("Hello!"), getSelf()); // (statement 2)
})
```

In the previous snippet of code, we implement two things:
1. _(statement 1)_: we get a reference to the sender of the message via the
   __getSender__ method. The reference is then added to the list of
   peers.
2. _(statement 2)_: we reply to the sender, in order to initiate the communication ("hello", "hello again", ...) as stated at the beginning of this session.

This covers the objectives 1 and 2 of the distributed application, to implement the objective 3, we do the following:

```java
.match(Greeting.class, msg -> {
    TimeUnit.SECONDS.sleep(1); // (statement 3)
    getSender().tell(new Greeting("Hello again!"), getSelf()); // (statement 4)
})
```

With the preceding snippet, we implement two behaviors
1. _(statement 3)_: a peer (master and slaves) wait one second before replying. Please note that ___this line will block the execution of the actor for one second, thus it is not a good practice.___
2. _(statement 4)_: we reply to the sender, in order to initiate the communication ("hello", "hello again", ...) as stated at the beginning of this session.

With the code contained in these two snippets, we succesfully
implement a distributed system where slaves join a master node : in
first time, the master welcome the slave nodes with an "hello" message
and then periodically the master node and the slave say "hello again"
every seconds.

The full code of the example is as follow:

```java
package distributed.simple;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import distributed.simple.msg.DiscoverNewPeer;
import distributed.simple.msg.Greeting;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;

public class Peer extends AbstractActor {

    static public Props props(String peerName) {
        return Props.create(Peer.class, () -> new Peer(peerName));
    }

    private final String peerName;
    private final ArrayList<ActorRef> peers = new ArrayList<ActorRef>();

    public Peer(String peerName) {
        this.peerName = peerName;
        distributed.simple.Peer selfReference = this;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DiscoverNewPeer.class, newPeer -> {
                    System.out.println(this.peerName + "> found new peer: " + getSender());
                    this.peers.add(getSender());
                    getSender().tell(new Greeting("Hello!"), getSelf());
                })
                .match(Greeting.class, msg -> {
                    System.out.println(this.peerName + "> " + getSender() + " told me '" + msg.message + "'");
                    TimeUnit.SECONDS.sleep(1);
                    getSender().tell(new Greeting("Hello again!"), getSelf());
                })
                .build();
    }
}

```

This simple example can be run with IntelliJ by running the [distributed.simple.Test](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/Test.java) class:

![/assets/img/session2/run_test_class.png](/assets/img/session2/run_test_class.png)

##### Main, Test, Peer : explanation on how to bootstrap the distributed system

In order to facilitate users to get started with this example, we
provide three classes in the __distributed.simple__ package:

1. [_Peer_](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/Peer.java): this class contains the code executed by each peer of
   the distributed system.
2. [_Main_](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/Main.java): this class instantiates one actor base on the previous
   _Peer_ class. This class contains a _main_ method that takes few
   arguments. According to the arguments passed to the class, the
   nodes ran by the class will be a _master node_ or a _slave
   node_. __One can create a real distributed systems by running several
   instances of these classes on several computers__.
3. [_Test_](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/Test.java): this class runs several instances of the _Main_ class
   on a same computer : one master node and several _slaves_. __This
   class is usefull for testing locally if the distributed system
   behaves correctly__.
   

One can run the _Main_ class on several computers to create distributed application. First, launch a server on a first computer (let say it has the __192.168.1.3_ ip address) that will use the 8000 port:
```
mvn exec:java -Dexec.mainClass=distributed.Main -Dexec.args="server 0.0.0.0 8000"
```

now, on an other computer, run a slave node that will also use port
8000, and provide some arguments that will enable him to contact the
master node:

```
mvn exec:java -Dexec.mainClass=distributed.Main -Dexec.args="client 8000 192.168.1.3"
```

If you are interested in how this process of bootstraping the
distributed system works, you may take a look at the code located
[here](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/Main.java#L48).

__Examples in other sections will reuse the same class structure.__

##### topology.txt : how to debug your distributed system ? (local execution only)

## Next step: Towards a Ring topology

Congratulations! You can now proceed with the [next session](/session3)
