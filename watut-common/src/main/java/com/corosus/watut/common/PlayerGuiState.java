package com.corosus.watut.common;

import java.util.*;

/**
 * Representation of the player's open GUI screen.
 * Ordinal order matches the network protocol of WATUT.
 */
public enum PlayerGuiState {
    NONE,
    CHAT_SCREEN,
    INVENTORY,
    CRAFTING,
    ESCAPE,
    EDIT_SIGN,
    EDIT_BOOK,
    CHEST,
    ENCHANTING_TABLE,
    ANVIL,
    BEACON,
    BREWING_STAND,
    DISPENSER,
    FURNACE,
    GRINDSTONE,
    HOPPER,
    HORSE,
    LOOM,
    VILLAGER,
    COMMAND_BLOCK,
    MISC;

    private static final Map<Integer, PlayerGuiState> LOOKUP = new HashMap<>();
    private static final Set<PlayerGuiState> POINTING_GUIS = EnumSet.noneOf(PlayerGuiState.class);
    private static final Set<PlayerGuiState> TYPING_GUIS = EnumSet.noneOf(PlayerGuiState.class);
    private static final Set<PlayerGuiState> SOUND_MAKER_GUIS = EnumSet.noneOf(PlayerGuiState.class);

    static {
        for (PlayerGuiState state : values()) {
            LOOKUP.put(state.ordinal(), state);
            POINTING_GUIS.add(state);
            SOUND_MAKER_GUIS.add(state);
        }

        POINTING_GUIS.remove(NONE);
        POINTING_GUIS.remove(CHAT_SCREEN);
        POINTING_GUIS.remove(EDIT_BOOK);
        POINTING_GUIS.remove(EDIT_SIGN);
        POINTING_GUIS.remove(COMMAND_BLOCK);

        TYPING_GUIS.add(CHAT_SCREEN);
        TYPING_GUIS.add(EDIT_BOOK);
        TYPING_GUIS.add(EDIT_SIGN);
        TYPING_GUIS.add(COMMAND_BLOCK);

        SOUND_MAKER_GUIS.remove(NONE);
        SOUND_MAKER_GUIS.remove(CHAT_SCREEN);
        SOUND_MAKER_GUIS.remove(CHEST);
    }

    public static PlayerGuiState fromOrdinal(int ordinal) {
        return LOOKUP.getOrDefault(ordinal, NONE);
    }

    public boolean isPointing() {
        return POINTING_GUIS.contains(this);
    }

    public boolean isTyping() {
        return TYPING_GUIS.contains(this);
    }

    public boolean isSoundMaker() {
        return SOUND_MAKER_GUIS.contains(this);
    }
}
