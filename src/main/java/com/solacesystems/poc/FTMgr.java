package com.solacesystems.poc;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPSession;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Instances of this class bind to a FT-cluster in a Solace Message Broker and manage FT-cluster membership
 * events for one binding. To implement Fault Tolerance within your application, you would typically have
 * several redundant instances of your application binding to the same FT-cluster, and behave appropriately
 * when the FTMgr instance signals your application to either become Active or become Backup.
 */
public class FTMgr {

    final private SolaceConnection connection;

    /**
     * Creates a new Solace session connected to a Solace Message Bus for use in joining
     * a FT-Cluster for Leader Election and role events.
     * @param solaceConnectionProperties Properties file containing Solace session properties.
     * @throws JCSMPException In the event of any error creating the Solace session and connecting it to a Solace Message Broker.
     */
    public FTMgr(Properties solaceConnectionProperties) throws JCSMPException {
        this.connection = new SolaceConnection(solaceConnectionProperties);
    }

    /**
     * Uses an existing Solace session connected to a Solace Message Bus for use in joining
     * a FT-Cluster for Leader Election and role events.
     * @param solaceSession An existing Solace session.
     * @throws JCSMPException In the event of any error creating the Solace session and connecting it to a Solace Message Broker.
     */
    public FTMgr(JCSMPSession solaceSession) throws JCSMPException {
        this.connection = new SolaceConnection(solaceSession);
    }

    /**
     * Bind to a FT cluster as a cluster member for leader election, listening for FT state change events.
     * @param ftClusterName Exclusive cluster for FT-selection.
     * @param listener Event listener to be invoked for any FT state event changes.
     * @throws JCSMPException In the event of any failures in connecting to Solace or binding to the cluster.
     */
    public void start(String ftClusterName, final FTEventListener listener) throws JCSMPException {
        // Everyone starts out as slave, Solace doesn't event for listeners initially bound as backup
        listener.onBackup();
        connection.bindExclusive(
                ftClusterName,
                new FTEventListener() {
                    @Override
                    public void onActive() {
                        listener.onActive();
                    }
                    @Override
                    public void onBackup() {
                        listener.onBackup();
                    }
                }
        );
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("    USAGE: <path/to/solace/conn.properties> <solace-exclusive-queue>");
            System.out.println("");
            System.out.println("");
            System.exit(1);
        }

        Properties props = readPropsFile(args[0]);
        String clusterName = args[1];

        try {
            final FTMgr ftMgr = new FTMgr(props);
            ftMgr.start(clusterName,
                    new FTEventListener() {
                        @Override
                        public void onActive() {
                            System.out.println("BECOMING MASTER");
                        }
                        @Override
                        public void onBackup() {
                            System.out.println("BECOMING BACKUP");
                        }
                    });

            while(true) {
                Thread.sleep(Long.MAX_VALUE);
            }
        }
        catch(JCSMPException e) {
            e.printStackTrace();
        }
        catch(InterruptedException iex) {
            iex.printStackTrace();
        }
    }

    static Properties readPropsFile(String name) {
        Properties props = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(name);
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return props;
    }
}
