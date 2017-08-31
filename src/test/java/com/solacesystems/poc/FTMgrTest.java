package com.solacesystems.poc;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPProperties;
import org.junit.Test;

import java.util.Properties;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class FTMgrTest {

    //@Test
    public void sampleFTAppTest() {
        SampleApplication fred = new SampleApplication("FRED");
        fred.start();

        SampleApplication barney = new SampleApplication("BARNEY");
        barney.start();

        assertTrue("Fred should be Active", fred.isActive());
        assertFalse("Barney should be Backup", barney.isActive());

        fred.stop();
        try { Thread.sleep(100); } catch(InterruptedException e) {}

        assertTrue("Barney should be Active", barney.isActive());
        assertFalse("Fred should be Backup", fred.isActive());

        barney.stop();

        assertFalse("Barney should be Backup", barney.isActive());
    }

    class SampleApplication implements  FTEventListener {
        final private String clusterName = "MyAppCluster";
        final private String instance;
        private boolean isActive;
        private FTMgr ftMgr;

        public SampleApplication(String instance) {
            this.instance = instance;
        }

        @Override
        public void onActive(BytesXMLMessage msg) {
            System.out.println(instance + " BECOMING MASTER");
            isActive = true;
        }
        @Override
        public void onBackup() {
            System.out.println(instance + " BECOMING BACKUP");
            isActive = false;
        }

        public boolean isActive() { return isActive; }

        public void start() {
            try {
                this.ftMgr = new FTMgr(getSampleProps());
                this.ftMgr.start(clusterName, this);
            }
            catch(JCSMPException e) {
                e.printStackTrace();
            }
        }

        public void stop() {
            this.ftMgr.stop();
        }

        private Properties getSampleProps() {
            Properties properties = new Properties();
            properties.setProperty(JCSMPProperties.HOST, "192.168.56.103,192.168.56.104");
            properties.setProperty(JCSMPProperties.USERNAME, "default");
            properties.setProperty(JCSMPProperties.VPN_NAME, "default");
            return properties;
        }
    }
}
