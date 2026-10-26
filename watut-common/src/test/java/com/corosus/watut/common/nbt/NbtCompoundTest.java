package com.corosus.watut.common.nbt;

import com.corosus.watut.common.WatutChannels;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class NbtCompoundTest {

    @Test
    public void testRoundTripSerialization() throws IOException {
        NbtCompound compound = new NbtCompound();
        UUID playerUuid = UUID.randomUUID();

        compound.putString(WatutChannels.NBT_PLAYER_UUID, playerUuid.toString());
        compound.putInt(WatutChannels.NBT_PLAYER_GUI_STATUS, 7); // Chest
        compound.putInt(WatutChannels.NBT_PLAYER_IDLE_TICKS, 120);
        compound.putFloat(WatutChannels.NBT_PLAYER_MOUSE_X, 123.45f);
        compound.putFloat(WatutChannels.NBT_PLAYER_MOUSE_Y, 67.89f);
        compound.putBoolean(WatutChannels.NBT_PLAYER_MOUSE_PRESSED, true);
        compound.putByteArray("testBytes", new byte[]{1, 2, 3, 4, 5});

        // Child compound test
        NbtCompound child = new NbtCompound();
        child.putString("nestedKey", "nestedVal");
        compound.putCompound("childCompound", child);

        // Serialize
        byte[] bytes = compound.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Deserialize
        NbtCompound deserialized = NbtCompound.fromByteArray(bytes);

        assertEquals(playerUuid.toString(), deserialized.getString(WatutChannels.NBT_PLAYER_UUID));
        assertEquals(playerUuid, deserialized.getUUID(WatutChannels.NBT_PLAYER_UUID));
        assertEquals(7, deserialized.getInt(WatutChannels.NBT_PLAYER_GUI_STATUS));
        assertEquals(120, deserialized.getInt(WatutChannels.NBT_PLAYER_IDLE_TICKS));
        assertEquals(123.45f, deserialized.getFloat(WatutChannels.NBT_PLAYER_MOUSE_X), 0.001f);
        assertEquals(67.89f, deserialized.getFloat(WatutChannels.NBT_PLAYER_MOUSE_Y), 0.001f);
        assertTrue(deserialized.getBoolean(WatutChannels.NBT_PLAYER_MOUSE_PRESSED));
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, deserialized.getByteArray("testBytes"));
        assertEquals("nestedVal", deserialized.getCompound("childCompound").getString("nestedKey"));
    }

    @Test
    public void testUuidInjection() throws IOException {
        // Simulates client sending packet without playerUuid, and server injecting it
        NbtCompound clientPacket = new NbtCompound();
        clientPacket.putInt(WatutChannels.NBT_PLAYER_GUI_STATUS, 21); // Crafter

        byte[] clientBytes = clientPacket.toByteArray();

        // Server receives clientBytes
        NbtCompound serverReceived = NbtCompound.fromByteArray(clientBytes);
        assertFalse(serverReceived.contains(WatutChannels.NBT_PLAYER_UUID));

        // Server injects sender UUID
        UUID senderUuid = UUID.randomUUID();
        serverReceived.putUUID(WatutChannels.NBT_PLAYER_UUID, senderUuid);

        // Server relays back to clients
        byte[] relayBytes = serverReceived.toByteArray();
        NbtCompound otherClientReceived = NbtCompound.fromByteArray(relayBytes);

        assertTrue(otherClientReceived.contains(WatutChannels.NBT_PLAYER_UUID));
        assertEquals(senderUuid, otherClientReceived.getUUID(WatutChannels.NBT_PLAYER_UUID));
        assertEquals(21, otherClientReceived.getInt(WatutChannels.NBT_PLAYER_GUI_STATUS));
    }
}
