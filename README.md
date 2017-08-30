# solft
Basic Solace Fault Tolerance library providing leader election for applications.

## BUILDING

This is a Maven project referencing all public libraries, so you will need
either internet access to public repositories or cached libraries in your
local repository. Building should be as simple as running the command:

        mvn install

A convenience startup script is provided in the `bin/` directory.

The executable requires a properties file with all variables defining connectivity
to Solace (further documentation below). Other variables can be
specified on the commandline and include:

`bin/run-example.sh <propsfile> <solace-exclusive-queue> `
- path/to/conn.properties
- Solace exclusive queue name


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

        bin/run-example.sh src/main/resources/localtest.properties myAppExcQ
        
The example simply connects to a Solace broker, binds to the named exclusive-queue 
(and provisions it if it does not exist), and alerts whenever it changes state to MASTER or SLAVE.

## USING THE LIBRARY

The sample application isn't really useful by itself. To use in your own code you will want 
to make use of the `FTMgr` class and the `SolaceConnection` class.

### SolaceConnection

First you will need a connection Solace session.

This is a barebones connector that will either create a new connection from properties, 
or it can leverage an existing Solace session instance.

```java
    public SolaceConnection(JCSMPSession sharedSession) throws JCSMPException;

    public SolaceConnection(Properties connectionProperties) throws JCSMPException;
```

### FTMgr

This is the actual manager class that binds to the Solace exclusive queue and sends you 
notifications when your leadership mode has changed. This is as simple as handing it a `SolaceConnection`,
pointing it at a Solace exclusive-queue and waiting for the events:

```java
        try {
            final FTMgr ftMgr = new FTMgr(solaceConnection);
            ftMgr.start(queueName,
                    new FTEventListener() {
                        @Override
                        public void onActive() {
                            logger.info("STATE CHANGE TO ACTIVE");
                        }

                        @Override
                        public void onPassive() {
                            logger.info("STATE CHANGE TO PASSIVE");
                        }
                    });
            // ...
        }
        catch(JCSMPException e) {
            e.printStackTrace();
        }
```
