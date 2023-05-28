package org.example.model;

import java.util.List;

public record JoinResponse (
        String username,
        int status,
        List<Message> messages
) { }
