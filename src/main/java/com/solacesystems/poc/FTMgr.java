package com.solacesystems.poc;

import com.solacesystems.jcsmp.JCSMPException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FTMgr {

    final private SolaceConnection connection;

    public FTMgr(SolaceConnection connection) {
        this.connection = connection;
    }

    public void start(String exclusiveQueueName, final FTEventListener listener) throws JCSMPException {
        // Everyone starts out as slave, Solace doesn't event for listeners initially bound as backup
        listener.onBackup();
        connection.bindExclusive(
                exclusiveQueueName,
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
        String queueName = args[1];

        try {
            final SolaceConnection conn = new SolaceConnection(props);
            final FTMgr ftMgr = new FTMgr(conn);
            ftMgr.start(queueName,
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
