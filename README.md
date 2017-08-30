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
                public void onActive() {
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
 
First you will need a connected Solace session. You can either create a new session just for solft by passing an 
instance of `Properties`, or more likely you'd pass a shared Solace session, into your `FTMgr` constructor. 
You use the `FTMgr` to join the FT-cluster and listen for events when cluster leadership state changes occur.

To listen for these events you must implement a simple `FTEventListener` interface and pass the listener into 
the `start()` method when starting the `FTMgr`. All applications joining the same named cluster on the same 
Solace broker will be part of the same FT-cluster, where a single application is elected leader until it unbinds 
from the cluster for any reason. In that event, the cluster elects a new member and an `FTEventListener.onActive()` 
event is raised for that instance notifying it of this role change.
