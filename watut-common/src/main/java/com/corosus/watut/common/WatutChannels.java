package com.corosus.watut.common;

/**
 * Standardized network channels and NBT payload keys for WATUT communication.
 */
public final class WatutChannels {

    // Minecraft Custom Payload Channels
    public static final String CHANNEL_CLIENT_TO_SERVER = "watut:nbt_server";
    public static final String CHANNEL_SERVER_TO_CLIENT = "watut:nbt_client";

    // NBT Keys
    public static final String NBT_PLAYER_UUID = "playerUuid";
    public static final String NBT_PLAYER_GUI_STATUS = "playerGuiStatus";
    public static final String NBT_PLAYER_GUI_DONT_SEND_DETAILED_GUI_INFO = "dontSendDetailedGUIInfo";
    public static final String NBT_PLAYER_GUI_DONT_SEND_ITEM_INFO = "dontSendDetailedItemInfo";
    public static final String NBT_PLAYER_CHAT_STATUS = "playerChatStatus";
    public static final String NBT_PLAYER_TYPING_AMP = "playerTypingAmp";
    public static final String NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA = "screenCompressedPixelData";
    public static final String NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA_PACKET_COUNT = "screenCompressedPixelDataPacketCount";
    public static final String NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA_PACKET_INDEX = "screenCompressedPixelDataPacketIndex";
    public static final String NBT_PLAYER_SCREEN_COMPRESSED_PIXEL_DATA_SIZE = "screenCompressedPixelDataSize";
    public static final String NBT_PLAYER_SCREEN_WIDTH = "screenWidth";
    public static final String NBT_PLAYER_SCREEN_HEIGHT = "screenHeight";
    public static final String NBT_PLAYER_IDLE_TICKS = "playerIdleTicks";
    public static final String NBT_PLAYER_TICKS_TO_GO_IDLE = "playerTicksToGoIdle";
    public static final String NBT_PLAYER_MOUSE_X = "playerMouseX";
    public static final String NBT_PLAYER_MOUSE_Y = "playerMouseY";
    public static final String NBT_PLAYER_MOUSE_PRESSED = "playerMousePressed";

    // Item movement keys
    public static final String NBT_ITEM_TRANSFER_ITEMSTACK = "itemTransferItemStack";
    public static final String NBT_ITEM_TRANSFER_FROM_X = "itemTransferFromX";
    public static final String NBT_ITEM_TRANSFER_FROM_Y = "itemTransferFromY";
    public static final String NBT_ITEM_TRANSFER_FROM_Z = "itemTransferFromZ";
    public static final String NBT_ITEM_TRANSFER_TO_X = "itemTransferToX";
    public static final String NBT_ITEM_TRANSFER_TO_Y = "itemTransferToY";
    public static final String NBT_ITEM_TRANSFER_TO_Z = "itemTransferToZ";

    // Server-to-client configuration sync
    public static final String NBT_SERVER_CONFIG = "serverConfig";

    private WatutChannels() {}
}
