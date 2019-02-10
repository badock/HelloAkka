---
title: Session 0 - Getting Started with Akka
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

In this session, we will install a functional development environment
that will enable us to develop a distributed application based on the
Java language and the Akka framework.

### I- Installing dependencies

##### MacOS

```bash
brew cask install java
brew install maven
brew install graphviz
```

##### Linux (debian/ubuntu)

```bash
apt install openjdk-8 maven graphviz
```

### II- Clone the project from github

```bash
git clone https://github.com/badock/HelloAkka.git
```

### II- Configuring IntelliJ

##### Download IntelliJ community edition

[https://www.jetbrains.com/idea/](https://www.jetbrains.com/idea/)

##### Configuration

First, import the project as an IntelliJ project:

![screen1](/assets/img/session0/screen1.png)
![screen2](/assets/img/session0/screen2.png)

IntelliJ should add the project as a maven project. Now try to launch
the code for session0:

![screen3](/assets/img/session0/screen3.png)

The test program should launch, and some logs should appear in the
bottom of the IntelliJ window, as follow:
![screen4](/assets/img/session0/screen4.png)


## Next step: Akka, an implementation of the Actor model

Congratulations! You can now proceed with the [next session](/session1)
