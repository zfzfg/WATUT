package com.corosus.watut.particle;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ParticleDynamic extends ParticleRotating {

    public ParticleRenderType particleRenderType;
    public float aspectRatio=1;

    public ParticleRenderType getRenderType() {
        return particleRenderType;
    }


    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, ParticleRenderType particleRenderType) {
        this(pLevel, pX, pY, pZ, particleRenderType, 1F);
    }

    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, ParticleRenderType particleRenderType, float brightness) {
        super(pLevel, pX, pY, pZ);
        this.particleRenderType = particleRenderType;
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        //this.quadSize = 1F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.setColor(this.getColorRed() * brightness, this.getColorGreen() * brightness, this.getColorBlue() * brightness);
    }

    public void setSize(float pWidth, float pHeight) {
        super.setSize(pWidth, pHeight);
    }

    public void tick() {
        super.tick();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
        }
    }

    @Override protected Layer getLayer() {
        return new Layer(true, net.minecraft.resources.Identifier.parse(particleRenderType.name()),
            net.minecraft.client.renderer.RenderPipelines.TRANSLUCENT_PARTICLE,
            net.minecraft.client.renderer.RenderPipelines.OIT_PARTICLE);
    }
    @Override protected float getU0() { return 0; }
    @Override protected float getU1() { return 1; }
    @Override protected float getV0() { return 0; }
    @Override protected float getV1() { return 1; }
    @Override public float getQuadSize(float partialTick) {
        return super.getQuadSize(partialTick) * (float)(ConfigClient.particleSizeScale *
            (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen ? 4 : 2));
    }
}
