package com.aspectran.aspectow.appmon.engine.relay.redis;

import com.aspectran.aspectow.appmon.engine.relay.CommandOptions;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayer;
import com.aspectran.aspectow.appmon.engine.relay.RelaySession;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_FOCUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_LOAD_PREVIOUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;
import static com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager.CONTROL_JOIN;
import static com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager.CONTROL_RELEASE;

public class RemoteMessageRelayer implements MessageRelayer {

    private final MessageRelayManager messageRelayManager;

    public RemoteMessageRelayer(MessageRelayManager messageRelayManager) {
        this.messageRelayManager = messageRelayManager;
    }

    @Override
    public void relay(String message) {

    }

    @Override
    public void relay(RelaySession relaySession, String message) {

    }

    @Override
    public RelaySession getLocalRelaySession(String sessionId) {
        return null;  // always return null for remote relayer
    }

        /**
     * Handles control messages from the cluster.
     * @param nodeId the ID of the node that sent the message
     * @param message the control message
     */
    public void handleControlMessage(String nodeId, @NonNull String message) {
        if (message.startsWith(CONTROL_JOIN)) {
            String appId = (message.length() > CONTROL_JOIN.length() + 1 ?
                    message.substring(CONTROL_JOIN.length() + 1) : null);
            messageRelayManager.subscribe(nodeId, appId);
        } else if (message.startsWith(CONTROL_RELEASE)) {
            String appId = (message.length() > CONTROL_RELEASE.length() + 1 ?
                    message.substring(CONTROL_RELEASE.length() + 1) : null);
            messageRelayManager.unsubscribe(nodeId, appId);
        } else if (message.startsWith("command:")) {
            handleCommandMessage(message);
        }
    }

    private void handleCommandMessage(@NonNull String message) {
        CommandOptions commandOptions = new CommandOptions(message);
        switch (commandOptions.getCommand()) {
            case COMMAND_REFRESH:
            case COMMAND_LOAD_PREVIOUS:
                refreshData(commandOptions);
                break;
            case COMMAND_FOCUS:
                focus(commandOptions);
                break;
        }
    }

    public void focus(@NonNull CommandOptions commandOptions) {
        String sessionId = commandOptions.getSessionid();
        if (sessionId != null) {
            RelaySession session = messageRelayManager.findRelaySession(sessionId);
            if (session != null) {
                session.setFocusedAppId(commandOptions.getAppId());
            }
        }
    }

    private void refreshData(@NonNull CommandOptions commandOptions) {
        String sessionId = commandOptions.getSessionid();
        if (sessionId != null) {
            RelaySession session = messageRelayManager.findRelaySession(sessionId);
            if (session != null) {
                if (!commandOptions.hasTimeZone()) {
                    commandOptions.setTimeZone(session.getTimeZone());
                }
                List<String> messages = messageRelayManager.getNewMessages(session, commandOptions);
                for (String message : messages) {
                    relay(session, message);
                }
            }
        }
    }

}
