package com.solacesystems.poc;

import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SolaceConnection {
    private static final Logger logger = LoggerFactory.getLogger(SolaceConnection.class);

    public SolaceConnection(JCSMPSession sharedSession) throws JCSMPException {
        session = sharedSession;
        validateCapabilities();
    }

    public SolaceConnection(Properties connectionProperties) throws JCSMPException {
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

    public void bindExclusive(String queueName, final FTEventListener listener) throws JCSMPException {
        provisionExclusiveQueue(queueName);

        final ConsumerFlowProperties queueProps = new ConsumerFlowProperties();
        queueProps.setEndpoint(JCSMPFactory.onlyInstance().createQueue(queueName));
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
                            listener.onPassive();
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
