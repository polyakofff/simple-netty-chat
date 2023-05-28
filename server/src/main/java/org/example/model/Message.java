package org.example.model;

import javax.annotation.Nullable;

public record Message (
        String fromUser,
        String message,
        @Nullable String toUser
) { }
