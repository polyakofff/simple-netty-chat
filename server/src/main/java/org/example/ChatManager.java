package org.example;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.example.model.Error;
import org.example.model.JoinRequest;
import org.example.model.JoinResponse;
import org.example.model.Message;
import org.json.simple.parser.ParseException;

public class ChatManager implements MessageHandler {

    public static final int MAX_USERS = 100;
    public static final int MAX_MESSAGES = 50;
    private static final int JOIN_SUCCESS = 0;
    private static final int JOIN_ALREADY_JOINED = 1;
    private static final int JOIN_MAX_USERS_IN_CHAT = 2;
    private static final int NO_SUCH_USER = 1;
    private final InternalLogger logger = InternalLoggerFactory.getInstance(this.getClass());
    private final Map<String, User> activeUsers;
    private final Queue<Message> messageHistory;
    private final Parser parser;

    public ChatManager() {
        activeUsers = new ConcurrentHashMap<>();
        messageHistory = new ArrayDeque<>();
        messageHistory.add(new Message(
                null,
                "(Начало чата)",
                null
        ));
        parser = new Parser();
    }

    @Override
    public void handle(String messageStr, User user) {
        logger.debug("handling");
        try {
            Object obj = parser.tryParse(messageStr);
            if (obj instanceof JoinRequest) { // join request
                logger.debug("handling join request: {}", messageStr);
                handleJoinRequest((JoinRequest) obj, user);

            } else { // message
                logger.debug("handling message: {}", messageStr);
                handleMessage((Message) obj, user);

            }
        } catch (ParseException ex) {
            logger.error("exception while handling: {}", ex.getUnexpectedObject());

        }
    }

    private void handleJoinRequest(JoinRequest request, User user) {
        String username = request.username();
        if (activeUsers.containsKey(username)) { // already joined
            logger.warn("{} has already joined", username);
            joinWhenAlreadyJoined(username, user);
            return;
        }
        if (activeUsers.size() == MAX_USERS) { // max users in chat
            logger.warn("max users in chat");
            joinWhenMaxUsersInChat(username, user);
            return;
        }
        // new user
        activeUsers.put(username, user);
        user.setLogoutAction(() -> activeUsers.remove(username));
        joined(username, user);

        logger.info("{} joined successfully", username);

        Message message = new Message(
                null,
                "(" + username + " присоединился)",
                null
        );
        newBroadcastMessage(message);
        newMessageInHistory(message);
    }

    private void joined(String username, User user) {
        List<Message> messages = messageHistory.stream()
                .filter(m -> m.toUser() == null)
                .toList();
        JoinResponse response = new JoinResponse(
                username,
                JOIN_SUCCESS,
                messages
        );
        String responseJson = parser.fromResponse(response);
        user.send(responseJson);
    }

    private void joinWhenAlreadyJoined(String username, User user) {
        JoinResponse response = new JoinResponse(
                username,
                JOIN_ALREADY_JOINED,
                null
        );
        String responseJson = parser.fromResponse(response);
        user.send(responseJson);
    }

    private void joinWhenMaxUsersInChat(String username, User user) {
        JoinResponse response = new JoinResponse(
                username,
                JOIN_MAX_USERS_IN_CHAT,
                null
        );
        String responseJson = parser.fromResponse(response);
        user.send(responseJson);
    }

    private void handleMessage(Message message, User user) {
        String fromUser = message.fromUser();
        String toUser = message.toUser();
        if (toUser != null) {
            if (!activeUsers.containsKey(toUser)) {
                logger.warn("no such user");
                messageWhenNoSuchUser(toUser, user);
                return;
            }

            newMessage(message, activeUsers.get(fromUser));
            if (!fromUser.equals(toUser))
                newMessage(message, activeUsers.get(toUser));
            newMessageInHistory(message);

        } else {
            newBroadcastMessage(message);
            newMessageInHistory(message);
        }
    }

    private void messageWhenNoSuchUser(String toUser, User user) {
        Error error = new Error(
                NO_SUCH_USER,
                "Нет пользователя с именем " + toUser
        );
        String errorJson = parser.fromError(error);
        user.send(errorJson);
    }

    private void newBroadcastMessage(Message message) {
        String messageJson = parser.fromMessage(message);
        activeUsers.forEach((username, user) -> {
            user.send(messageJson);
        });
    }

    private void newMessage(Message message, User user) {
        String messageJson = parser.fromMessage(message);
        user.send(messageJson);
    }

    private synchronized void newMessageInHistory(Message message) {
        messageHistory.add(message);
        if (messageHistory.size() > MAX_MESSAGES)
            messageHistory.poll();
    }

    public Map<String, User> getActiveUsers() {
        return Collections.unmodifiableMap(activeUsers);
    }

    public List<Message> getMessageHistory() {
        return messageHistory.stream().toList();
    }
}
