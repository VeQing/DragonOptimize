package com.dragoncore.optimize.render;

import com.dragoncore.optimize.config.OptConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CityRenderOptimizer {

    private int renderedPlayers;
    private int lastRenderDistance = -1;
    private int lastParticleSetting = -1;
    private boolean emergencyMode;

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!OptConfig.cityRenderOptimizer) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) return;

        Object entity = event.entityPlayer;
        if (entity == null) return;
        if (entity == mc.player) return;

        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        EntityPlayerSP self = mc.player;
        double dx = self.posX - player.posX;
        double dy = self.posY - player.posY;
        double dz = self.posZ - player.posZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;

        int near = OptConfig.cityPlayerNearDistance;
        int far = emergencyMode ? near : OptConfig.cityPlayerFarDistance;
        int limit = emergencyMode ? OptConfig.cityEmergencyRenderedPlayers : OptConfig.cityMaxRenderedPlayers;

        if (distanceSq > (double) far * far) {
            event.setCanceled(true);
            return;
        }
        if (renderedPlayers >= limit && distanceSq > (double) near * near) {
            event.setCanceled(true);
            return;
        }
        renderedPlayers++;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) return;

        // 每帧重置计数
        renderedPlayers = 0;

        if (!OptConfig.cityRenderOptimizer) {
            restore(mc.gameSettings);
            return;
        }

        int fps = Minecraft.getDebugFPS();
        emergencyMode = fps > 0 && fps < OptConfig.cityLowFpsThreshold;

        if (emergencyMode) {
            remember(mc.gameSettings);
            mc.gameSettings.particleSetting = 2;
            mc.gameSettings.renderDistanceChunks = Math.min(mc.gameSettings.renderDistanceChunks,
                    OptConfig.cityEmergencyRenderDistance);
        } else {
            if (lastParticleSetting >= 0) {
                mc.gameSettings.particleSetting = lastParticleSetting;
                lastParticleSetting = -1;
            }
            if (lastRenderDistance >= 0) {
                mc.gameSettings.renderDistanceChunks = lastRenderDistance;
                lastRenderDistance = -1;
            }
        }
    }

    private void remember(GameSettings settings) {
        if (lastParticleSetting < 0) lastParticleSetting = settings.particleSetting;
        if (lastRenderDistance < 0) lastRenderDistance = settings.renderDistanceChunks;
    }

    private void restore(GameSettings settings) {
        emergencyMode = false;
        if (lastParticleSetting >= 0) {
            settings.particleSetting = lastParticleSetting;
            lastParticleSetting = -1;
        }
        if (lastRenderDistance >= 0) {
            settings.renderDistanceChunks = lastRenderDistance;
            lastRenderDistance = -1;
        }
    }
}
