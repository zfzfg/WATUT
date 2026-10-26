package com.corosus.watut.client.emf;

import com.corosus.watut.PlayerStatus;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.Map;

/**
 * After WATUT has posed the vanilla player model, hand the posed parts to EMF Compat Core.
 * The core puts them back once a resource pack animation has run. Body and legs stay with EMF.
 */
public final class EmfPoseCapture {

    private static final String CORE_MOD = "emf_compat_core";
    private static final String ADDON_MOD = "emf_compat_watut";

    private EmfPoseCapture() {
    }

    public static void capture(PlayerModel<?> model, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        WatutMod mod = WatutMod.instance();
        if (mod == null || !mod.isModInstalled(CORE_MOD) || mod.isModInstalled(ADDON_MOD)) {
            return;
        }
        if (!EmfPoseBridge.available()) {
            return;
        }
        if (!ConfigClient.emfCompatibility || EmfPoseBridge.isLocalPlayerInFirstPerson(player.getUUID())) {
            EmfPoseBridge.clear(player.getUUID());
            return;
        }

        boolean arms = false;
        boolean head = false;
        if (isPosing(player, model)) {
            PlayerStatus status = WatutMod.getPlayerStatusManagerClient().getStatus(player);
            boolean gui = ConfigClient.showPlayerAnimation_Gui
                    && ConfigServerControlledSyncedToClient.showPlayerAnimation_Gui
                    && status.getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE;
            boolean typing = ConfigClient.showPlayerAnimation_Typing
                    && ConfigServerControlledSyncedToClient.showPlayerAnimation_Typing
                    && status.getPlayerChatState() == PlayerStatus.PlayerChatState.CHAT_TYPING;
            boolean idle = ConfigClient.showPlayerAnimation_Idle
                    && ConfigServerControlledSyncedToClient.showPlayerAnimation_Idle
                    && status.isIdle();
            boolean lerping = status.isLerping();
            arms = gui || typing || lerping;
            head = gui || idle || lerping;
        }

        if (!arms && !head) {
            EmfPoseBridge.clear(player.getUUID());
            return;
        }

        Object left = arms ? EmfPoseBridge.snapshot(model.leftArm, false) : null;
        Object right = arms ? EmfPoseBridge.snapshot(model.rightArm, false) : null;
        Map<String, Object> parts = Map.of();
        if (head) {
            Object hat = EmfPoseBridge.snapshot(model.hat, true);
            Object headPose = EmfPoseBridge.snapshot(model.head, true);
            if (hat != null && headPose != null) {
                parts = Map.of("head", headPose, "hat", hat, "headwear", hat);
            }
        }
        if ((arms && (left == null || right == null)) || (head && parts.isEmpty())) {
            EmfPoseBridge.clear(player.getUUID());
            return;
        }

        Vector3f bodyBase = (arms && ConfigClient.emfArmBodyFollow)
                ? new Vector3f(model.body.x, model.body.y, model.body.z)
                : null;
        EmfPoseBridge.save(player.getUUID(), left, right, parts, bodyBase);
    }

    /**
     * Same gates as {@code PlayerStatusManagerClient.setupRotationsHook}: master flags,
     * the inventory paper doll, and players WATUT is not animating.
     */
    private static boolean isPosing(Player player, PlayerModel<?> model) {
        if (!ConfigClient.showPlayerAnimations || !ConfigServerControlledSyncedToClient.showPlayerAnimations) {
            return false;
        }
        if (model.head.yRot > Math.PI) {
            return false;
        }
        PlayerStatusManagerClient manager = WatutMod.getPlayerStatusManagerClient();
        if (manager == null || !manager.shouldAnimate(player)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return !(player == mc.player && mc.screen instanceof EffectRenderingInventoryScreen && player.isAlive());
    }
}
