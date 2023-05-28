package org.example;

import java.util.List;
import java.util.Set;

import org.example.model.Error;
import org.example.model.JoinRequest;
import org.example.model.JoinResponse;
import org.example.model.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Parser {
    private static final Set<String> JOIN_REQUEST_FIELDS =
            Set.of("username");
    private static final Set<String> MESSAGE_FIELDS =
            Set.of("fromUser", "message", "toUser");
    private final JSONParser parser;

    public Parser() {
        this.parser = new JSONParser();
    }

    public Object tryParse(String json) throws ParseException {
        JSONObject obj;
        synchronized (this) {
            obj = (JSONObject) parser.parse(json);
        }
        if (includes(JOIN_REQUEST_FIELDS, obj.keySet())) {
            return new JoinRequest(
                    (String) obj.get("username")
            );

        } else if (includes(MESSAGE_FIELDS, obj.keySet())) {
            return new Message(
                    (String) obj.get("fromUser"),
                    (String) obj.get("message"),
                    (String) obj.get("toUser")
            );

        }
        throw new ParseException(-1);
    }

    private boolean includes(Set<String> set1, Set<String> set2) {
        for (String s : set2) {
            if (!set1.contains(s))
                return false;
        }
        return true;
    }

    public String fromResponse(JoinResponse response) {
        JSONObject obj = new JSONObject();
        obj.put("username", response.username());
        obj.put("status", response.status());
        if (response.messages() != null) {
            JSONArray messagesArr = new JSONArray();
            for (Message message : response.messages()) {
                JSONObject messageObj = new JSONObject();
                messageObj.put("fromUser", message.fromUser());
                messageObj.put("message", message.message());
                messageObj.put("toUser", message.toUser());
                messagesArr.add(messageObj);
            }
            obj.put("messages", messagesArr);
        }
        return obj.toJSONString();
    }

    public String fromError(Error error) {
        JSONObject obj = new JSONObject();
        obj.put("code", error.code());
        obj.put("message", error.message());
        return obj.toJSONString();
    }

    public String fromMessage(Message message) {
        JSONObject obj = new JSONObject();
        obj.put("fromUser", message.fromUser());
        obj.put("message", message.message());
        obj.put("toUser", message.toUser());
        return obj.toJSONString();
    }
}
