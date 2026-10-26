package com.corosus.watut.loader.fabric;

import com.corosus.coroutil.config.ConfigCoroUtil;
import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.WatutNetworking;
import com.corosus.watut.network.PacketNBTFromServer;
import com.corosus.watut.particle.ParticleRotating;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WatutModFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        com.corosus.watut.client.screen.ScreenParticleRenderer.configure((state,features) -> {
            var mc=net.minecraft.client.Minecraft.getInstance();
            return new net.minecraft.client.gui.render.GuiRenderer(state,features,List.of(
                new net.minecraft.client.gui.render.pip.GuiEntityRenderer(mc.getEntityRenderDispatcher()),
                new net.minecraft.client.gui.render.pip.GuiSkinRenderer(),new net.minecraft.client.gui.render.pip.GuiBookModelRenderer(),
                new net.minecraft.client.gui.render.pip.GuiBannerResultRenderer(mc.getAtlasManager()),new net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer()));
        });
		/*ClientPlayNetworking.registerGlobalReceiver(WatutNetworkingFabric.NBT_PACKET_ID, (client, handler, buf, responseSender) -> {
			CompoundTag nbt = buf.readNbt();
			client.execute(() -> {
				try {
					if (nbt.contains(WatutNetworking.NBTDataPlayerUUID)) {
						UUID uuid = UUID.fromString(nbt.getStringOr(WatutNetworking.NBTDataPlayerUUID, ""));
						WatutMod.getPlayerStatusManagerClient().receiveAny(uuid, nbt);
					} else if (nbt.contains(WatutNetworking.NBTDataServerConfig)) {
						WatutMod.getPlayerStatusManagerClient().receiveServerConfig(nbt);
					} else if (nbt.contains(WatutNetworking.NBTDataItemTransferItemStack)) {
						WatutMod.getPlayerStatusManagerClient().receiveItemMove(nbt);
					}

				} catch (Exception ex) {
					CULog.dbg("WATUT ERROR: packet with invalid uuid sent from server");
					CULog.dbg("full nbt data: " + nbt);
					if (ConfigCoroUtil.useLoggingDebug) {
						ex.printStackTrace();
					}
				}
			});
		});*/

		ClientPlayNetworking.registerGlobalReceiver(PacketNBTFromServer.TYPE, (payload, ctx) -> {
			CompoundTag nbt = payload.nbt();
			ctx.client().execute(() -> {
				try {
					if (nbt.contains(WatutNetworking.NBTDataPlayerUUID)) {
						UUID uuid = UUID.fromString(nbt.getStringOr(WatutNetworking.NBTDataPlayerUUID, ""));
						WatutMod.getPlayerStatusManagerClient().receiveAny(uuid, nbt);
					} else if (nbt.contains(WatutNetworking.NBTDataServerConfig)) {
						WatutMod.getPlayerStatusManagerClient().receiveServerConfig(nbt);
					} else if (nbt.contains(WatutNetworking.NBTDataItemTransferItemStack)) {
						WatutMod.getPlayerStatusManagerClient().receiveItemMove(nbt);
					}
				} catch (Exception ex) {
					CULog.dbg("WATUT ERROR: packet with invalid uuid sent from server");
					CULog.dbg("full nbt data: " + nbt);
					if (ConfigCoroUtil.useLoggingDebug) {
						ex.printStackTrace();
					}
				}
			});
		});
	}

}