package com.corosus.watut.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of the player's chat status.
 * Ordinal matches the network protocol of WATUT.
 */
public enum PlayerChatState {
    NONE,
    CHAT_FOCUSED,
    CHAT_TYPING;

    private static final Map<Integer, PlayerChatState> LOOKUP = new HashMap<>();

    static {
        for (PlayerChatState state : values()) {
            LOOKUP.put(state.ordinal(), state);
        }
    }

    public static PlayerChatState fromOrdinal(int ordinal) {
        return LOOKUP.getOrDefault(ordinal, NONE);
    }
}
