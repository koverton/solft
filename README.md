# solft: Simple Fault Tolerance for Java Applications

This Java library leverages Solace's guaranteed delivery capabilities to provide simple 
Leader Election events to applications requiring fault tolerance. But it does require 
a Solace Message Broker is available for your application to connect to for this library to work.

If you are already using Solace, just pass your Solace session instance into the 
`FTMgr` and listen for status events. If you are not using Solace there are [several 
great reasons](https://solace.com/) why you might want to!

```java
    new FTMgr(solaceConnection)
        .start(ftClusterName,
            new FTEventListener() {
                @Override
                public void onActive(BytesXMLMessage m) {
                    logger.info("STATE CHANGE TO ACTIVE");
                }

                @Override
                public void onBackup() {
                    logger.info("STATE CHANGE TO BACKUP");
                }
            });
    // ...
```
## BUILDING

This is a Maven project referencing all public libraries, so you will need
either internet access to public repositories or cached libraries in your
local repository. Building should be as simple as running the command:

        mvn install

A convenience startup script is provided in the `bin/` directory.

The executable requires a properties file with all variables defining connectivity
to Solace (further documentation below). Other variables can be
specified on the commandline and include:

`bin/run-example.sh <propsfile> <solace-cluster-name> `
- `props-file`: Solace session properties file for connecting
- `cluster-name`: Solace FT cluster name


### Example Configuration Properties

```
# Solace session properties
host = 192.168.56.199,192.168.56.200
vpn_name = default
username = default
```

## RUNNING

After building the package can be run in place by leveraging MVN's understanding
of your classpath in the `bin/run-example.sh` script. For example:

`bin/run-example.sh src/main/resources/localtest.properties myCluster01`
        
The example simply connects to a Solace broker, binds to the FT-Cluster via a Solace exclusive-queue 
(and provisions it if it does not exist), and alerts whenever it changes state to MASTER or BACKUP.

## CODING

This Java library leverages Solace's guaranteed delivery capabilities to provide simple Leader Election events to applications requiring fault tolerance, by creating an instance of the FTMgr and binding a listener to it to receive FT role event updates.

To use it you must have a Solace Message Broker available for your application to connect to for this library to work. 

### Stateless Event Listener
 
First you will need a connected Solace session. You can either create a new session just for `solft` by passing an 
instance of `Properties`, or more likely you'd pass a shared Solace session, into your `FTMgr` constructor. 
You use the `FTMgr` to join the FT-cluster and listen for events when cluster leadership state changes occur.

To listen for these events you must implement a simple `FTEventListener` interface and pass the listener into 
the `start()` method when starting the `FTMgr`. All applications joining the same named cluster on the same 
Solace broker will be part of the same FT-cluster, where a single application is elected leader until it unbinds 
from the cluster for any reason. In that event, the cluster elects a new member and an `FTEventListener.onActive(msg)` 
event is raised for that instance notifying it of this role change.

For Stateless Listeners, the message object will always be `null`.

### Stateful Event Listener

With a Stateful Event Listener, an `onActive(msg)` event is received with the last output message sent by the previous 
Active member. This can be used to synchronize your newly-elected Active member to that state.

To run a Stateful Event Listener you start the `FTMgr` instance by calling `startStateful()` rather than `start()` and 
pass into it a topic subscription string that subscribes to all output from this application. Each instance must 
be configured with this to support Stateful Listeners therefore must all be able to publish output messages on a 
topic that can be subscribed to in this manner.

Here's an example.

Let's say I have an application that is setup to publish output messages to a _topic hierarchy_ with an 
agreed structure. In our example, let's say the agreed structure is:

        [SOURCE] / [SRC INST] / [DESTINATION] / [EVENT TYPE]

In our case, when my instance (#3) of app `AKNA` sends an OTD measurement to `IGALUK` the topic might look like this:

        AKNA / 3 / IGALUK / OTD

This allows us to configure all the instances of AKNA to subscribe to all output from any of the instances (and NOT 
from any other application instances) by starting our `FTMgr` instance with an output subscription like:

        AKNA/>

The code example becomes:

```java
    new FTMgr(solaceConnection)
        .startStateful(ftClusterName, "AKNA/>",
            new FTEventListener() {
                @Override
                public void onActive(BytesXMLMessage m) {
                    logger.info("STATE CHANGE TO ACTIVE with state: " + m.dump());
                }

                @Override
                public void onBackup() {
                    logger.info("STATE CHANGE TO BACKUP");
                }
            });
    // ...
```
