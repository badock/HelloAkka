---
title: Session 1 - Akka, an implementation of the Actor model
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

In this session, we will provide a glimpse of the _Actor model_
proposed by Carl Hewitt in 1973. The goal of this session is to give
enough elements to understand the theoretical elements behind the
Akka library, rather than doing a course on the Actor model.  If you
want to learn more on the actor model, you can watch this interesting
[interview made during the Microsoft Lang.NEXT
2012](https://www.youtube.com/watch?v=7erJ1DV_Tlo) conference, where
Carl Hewitt explain in 45 minutes the principles of the actor model.

### I- The actor model

Developing distributed systems is not a simple task as it involves
dealing with problems such as concurrency, fault tolerance, and
scaling. Over the last decades, several models has been proposed to
organize computing resources working in an distributed systems. One of
them is the actor model proposed by Carl Hewitt in 1973 ([original
paper](https://eighty-twenty.org/files/Hewitt,%20Bishop,%20Steiger%20-%201973%20-%20A%20universal%20modular%20ACTOR%20formalism%20for%20artificial%20intelligence.pdf)),
which has inspired a language such as Erlang, or frameworks for
distributed systems such as Akka.

Roughly, the actor models proposes to organize distributed systems
according to the following principles:

 1. Everything in the system is an actor
 2. An actor can receive and send messages to other actor (including
    itself)
 3. An actor can process one message at a time
 4. at the end of 3., an actor can send one or several messages to one
    or several actors
 5. at the end of 3., an actor can changes its behavior for the next
    message
 6. at the end of 3., an actor can create new actors

Actors have private states that are not directly shared with other
actors. Instead, they collaborate via exchanges of messages. The lack
of shared states prevent the need for synchronization systems such as
lock or mutex. Thus, the actor model is a good fit for building
distributed systems or highly concurrent applications.

### II- How the actor model is implemented in Akka

Basically, a distributed application based on Akka should contains two kind of _entities_:
1. _Actor system_: represents an hierarchical structure of actors,
   providing facilities such as configuration handling, dealing with
   events, scheduling of tasks and fault handling. Every actor must be
   registered in an Actor system.
2. _Actor_: represents the "smallest unit of computation" in the
   distributed application. Each actor keeps its mutable states
   private and communicates with other actors in the same _actor
   system_, or in remote _actor system_ via message exchange.

##### Creating _Actor systems_

First, let's create an actor system. We pass a first argument that
corresponds to the name given to an actor system.  As a computer can
host several systems of actors, providing a name helps to identify
which systems of actor is targeted.

```java
import akka.actor.ActorSystem;

final ActorSystem system = ActorSystem.create("hello_akka");
```

Note that this actor systems did not take any configuration into
account, we can customize its behavior by passing some configuration
such as the network address, the network port, or the network
protocol. You can get more details on how to customize configuration
of an actor system
[here](https://doc.akka.io/docs/akka/2.5/general/configuration.html),
or you can look at how the examples provided in this tutorial are
configured
[here](https://github.com/badock/HelloAkka/blob/master/src/main/java/distributed/simple/config/ConfigurationGenerator.java).

##### Adding _Actors_ to the 'hello_akka' system

In order to be operational, Actors must inherits the []() class. Let say that we have created an _SampleActor_ class
that extends the _toto_ class, that we can summarize with the following pseudo-code (the real code will be introduced in session 2):

```java
import akka.actor.AbstractActor;
import akka.actor.Props;

public class SampleActor extends AbstractActor {

    private String name;

    // Function that enables to create actors
    static public Props props(String name) {
        return Props.create(SampleActor.class,
                () -> new SampleActor(name));
    }

    // Constructor
    public SampleActor(String name) {
        this.name = name;
    }

    // Message handling loop
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, msg -> {
                    // display the received message
                    System.out.println(this.name + " --> " + msg);
                })
                .build();
    }
}
```


We can spawn an instance of this Actor class with the following code:

```java
import akka.actor.ActorRef;

final ActorRef sampleActorRef =
                system.actorOf(SampleActor.props("bob"), "actor1");
```

In the previous code, "bob" is the value passed to the java
constructor, and "actor1" will be the name of the actor in the Actor
hierarchy of the _Actor system_ "hello_akka".

The tricks here is that we don't call explicitely the constructor of
the actors, but rather use a factory method that return an
intermediate class
[_Props_](https://doc.akka.io/api/akka/2.5/akka/actor/Props.html)
which will contains arguments that the actor systems will pass to the
_Actor system_. In return, the _Actor system_ returns an
[_ActorRef_](https://doc.akka.io/api/akka/2.5/akka/actor/ActorRef.html) :
we don't get a reference to the Actor object, __thus we cannot modify
its mutable states otherwise than sending messages to the actor__!

The following picture (extracted from the [_Akka documentation_](https://doc.akka.io/docs/akka/2.5/general/addressing.html)) describes how _ActorRef_ works: ![https://doc.akka.io/docs/akka/2.5/general/ActorPath.png](https://doc.akka.io/docs/akka/2.5/general/ActorPath.png)


##### Sending a message to an actor

One can send a message to an actor by calling the [tell]() method and
passing the value of the message and information about the sender:

```java
// Here the sender is not an actor, we pass null as 'sender' parameter
sampleActorRef.tell("ping", null);
```

The actor instantiated in the previous section will react to the
message by displaying the following message in the console:

```
bob --> pong
```


## A ping pong example

Let's modify the message handling function of the previous _SampleActor_:

```java
// Message handling loop
@Override
public Receive createReceive() {
    return receiveBuilder()
            .match(String.class, msg -> {
                System.out.println(this.name+" --> "+msg);
                String replyMsg;
                if (this.name.equals("bob")) {
                    replyMsg = "ping";
                } else {
                    replyMsg = "pong";
                }
                sender().tell(replyMsg, self());
            })
            .build();
}
```

Instantiating two actors with the following code will result in an
_"infinite loop of messages"_:
```java
final ActorSystem system = ActorSystem.create("hello_akka");

final ActorRef sampleActorRef =
        system.actorOf(SampleActor.props("bob"), "actor1");
final ActorRef sampleActorRef2 =
        system.actorOf(SampleActor.props("alice"), "actor2");

sampleActorRef.tell("ping", sampleActorRef2);
```

as illustrated by the following output:
```
bob --> pong
alice --> ping
bob --> pong
alice --> ping
bob --> pong
alice --> ping
bob --> pong
alice --> ping
bob --> pong
alice --> ping
bob --> pong
alice --> ping
[...]
```

## Next step: a simple client/server application

Congratulations! You can now proceed with the [next session](/session2)
