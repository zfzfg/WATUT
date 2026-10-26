package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerOnPress {

    @Inject(method = "onButton", at = @At("TAIL"))
    private void onPress(long window, net.minecraft.client.input.MouseButtonInfo button, int pAction, CallbackInfo info) {
        WatutMod.getPlayerStatusManagerClient().onMouse(pAction != 0);
    }
}