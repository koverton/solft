package com.solacesystems.poc;

import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This internal class provides the underlying Solace capabilities to implement application layer fault tolerant clustering.
 * Instances of this class are used by the {@link com.solacesystems.poc.FTMgr}. Applications should not need to
 * interact with this directly, they should merely instantiate one according to the available public constructors,
 * and pass the instance into the {@link com.solacesystems.poc.FTMgr#FTMgr(SolaceConnection)} constructor.
 */
class SolaceConnection {
    private static final Logger logger = LoggerFactory.getLogger(SolaceConnection.class);

    SolaceConnection(JCSMPSession sharedSession) throws JCSMPException {
        session = sharedSession;
        validateCapabilities();
    }

    SolaceConnection(Properties connectionProperties) throws JCSMPException {
        final JCSMPProperties solprops = new JCSMPProperties();
        for (String name : connectionProperties.stringPropertyNames()) {
            Object value = connectionProperties.getProperty(name);
            solprops.setProperty(name, value);
        }
        session = JCSMPFactory.onlyInstance().createSession(solprops);
        session.connect();
        validateCapabilities();
    }

    private void validateCapabilities() throws JCSMPException {
        boolean valid = (session.isCapable(CapabilityType.ACTIVE_FLOW_INDICATION)
                && session.isCapable(CapabilityType.SUB_FLOW_GUARANTEED)
                && session.isCapable(CapabilityType.ENDPOINT_MANAGEMENT));
        if (!valid)
            throw new JCSMPException("Session has insufficient capabilities to support application Fault Tolerance");
    }

    /**
     * Bind into an exclusive cluster; a leader is selected from all applications bound to a give cluster.
     * Whenever the Leader process unbinds for any reason a new Leader is elected from the rest of the
     * bound participants.
     * @param exclusiveClusterName Exclusive cluster for FT-selection.
     * @param listener Event listener to be invoked for any FT state event changes.
     * @throws JCSMPException
     */
    void bindExclusive(String exclusiveClusterName, final FTEventListener listener) throws JCSMPException {
        provisionExclusiveQueue(exclusiveClusterName);

        final ConsumerFlowProperties queueProps = new ConsumerFlowProperties();
        queueProps.setEndpoint(JCSMPFactory.onlyInstance().createQueue(exclusiveClusterName));
        queueProps.setActiveFlowIndication(true);
        session.createFlow(
                new XMLMessageListener() {
                    @Override
                    public void onReceive(BytesXMLMessage bytesXMLMessage) {
                        // Reserved for future use
                    }
                    @Override
                    public void onException(JCSMPException e) {
                    }
                },
                queueProps,
                null,
                new FlowEventHandler() {
                    @Override
                    public void handleEvent(Object o, FlowEventArgs args) {
                        if (args.getEvent().equals(FlowEvent.FLOW_ACTIVE)) {
                            listener.onActive();
                        }
                        else {
                            listener.onBackup();
                        }
                    }
                })
                .start();
    }

    private void provisionExclusiveQueue(String queueName) throws JCSMPException {
        final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
        final EndpointProperties endpointProps = new EndpointProperties();
        endpointProps.setPermission(EndpointProperties.PERMISSION_DELETE);
        endpointProps.setAccessType(EndpointProperties.ACCESSTYPE_EXCLUSIVE);
        session.provision(queue, endpointProps, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);
    }

    final private JCSMPSession session;
}
