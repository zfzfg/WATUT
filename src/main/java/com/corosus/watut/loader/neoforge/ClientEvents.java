package com.corosus.watut.loader.neoforge;

import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.WatutMod;
import com.corosus.watut.command.CommandWatutReloadJSON;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

public class ClientEvents {
    public ClientEvents() {
        com.corosus.watut.client.screen.ScreenParticleRenderer.configure((state,features) -> {
            var mc=net.minecraft.client.Minecraft.getInstance();
            return new net.minecraft.client.gui.render.GuiRenderer(state,features,java.util.List.of(
                new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState.class,()->new net.minecraft.client.gui.render.pip.GuiEntityRenderer(mc.getEntityRenderDispatcher())),
                new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(net.minecraft.client.renderer.state.gui.pip.GuiSkinRenderState.class,net.minecraft.client.gui.render.pip.GuiSkinRenderer::new),
                new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(net.minecraft.client.renderer.state.gui.pip.GuiBookModelRenderState.class,net.minecraft.client.gui.render.pip.GuiBookModelRenderer::new),
                new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(net.minecraft.client.renderer.state.gui.pip.GuiBannerResultRenderState.class,()->new net.minecraft.client.gui.render.pip.GuiBannerResultRenderer(mc.getAtlasManager())),
                new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(net.minecraft.client.renderer.state.gui.pip.GuiProfilerChartRenderState.class,net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer::new)));
        });
    }

    public void getRegisteredParticles(TextureAtlasStitchedEvent event) {
        ParticleRegistry.textureAtlasUpload(event.getAtlas());
    }

    public void onGameTick(ClientTickEvent.Post event) {
        WatutMod.getPlayerStatusManagerClient().tickGame();
    }

    public void onRegisterCommandsClient(RegisterClientCommandsEvent event) {
        CommandWatutReloadJSON.register(event.getDispatcher());
    }

    public void onMouse(InputEvent.MouseButton.Post event) {
        WatutMod.getPlayerStatusManagerClient().onMouse(event.getAction() != 0);
    }

    public void onKey(InputEvent.Key event) {
        WatutMod.getPlayerStatusManagerClient().onKey();
    }

}
