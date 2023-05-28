package org.example;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ParserTest {

    @Test
    void test() throws ParseException {
        JSONParser parser = new JSONParser();
        String jsonMessage = """
                {
                    "fromUser": "Alice",
                    "message": "Hello, Bob",
                    "toUser": null
                }
                """;
        JSONObject jsonObject = (JSONObject) parser.parse(jsonMessage);
        assertEquals("Alice", jsonObject.get("fromUser"));
        assertEquals("Hello, Bob", jsonObject.get("message"));
        assertNull(jsonObject.get("toUser"));
    }
}
