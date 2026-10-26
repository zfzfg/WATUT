package com.corosus.watut.common;

import java.util.UUID;

/**
 * Clean data model representing the active status of a tracked player.
 */
public class WatutPlayerStatus {

    private final UUID playerUuid;
    private PlayerGuiState guiState = PlayerGuiState.NONE;
    private PlayerChatState chatState = PlayerChatState.NONE;
    private int idleTicks = 0;
    private float mouseX = 0f;
    private float mouseY = 0f;
    private boolean mousePressed = false;
    private boolean idle = false;
    /** True once the client mod has reported idle ticks. The server then stops its own counter. */
    private boolean clientIdleAuthoritative = false;

    public WatutPlayerStatus(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public PlayerGuiState getGuiState() {
        return guiState;
    }

    public void setGuiState(PlayerGuiState guiState) {
        this.guiState = guiState;
    }

    public PlayerChatState getChatState() {
        return chatState;
    }

    public void setChatState(PlayerChatState chatState) {
        this.chatState = chatState;
    }

    public int getIdleTicks() {
        return idleTicks;
    }

    public void setIdleTicks(int idleTicks) {
        this.idleTicks = idleTicks;
    }

    public float getMouseX() {
        return mouseX;
    }

    public void setMouseX(float mouseX) {
        this.mouseX = mouseX;
    }

    public float getMouseY() {
        return mouseY;
    }

    public void setMouseY(float mouseY) {
        this.mouseY = mouseY;
    }

    public boolean isMousePressed() {
        return mousePressed;
    }

    public void setMousePressed(boolean mousePressed) {
        this.mousePressed = mousePressed;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    public boolean isClientIdleAuthoritative() {
        return clientIdleAuthoritative;
    }

    public void setClientIdleAuthoritative(boolean clientIdleAuthoritative) {
        this.clientIdleAuthoritative = clientIdleAuthoritative;
    }
}
