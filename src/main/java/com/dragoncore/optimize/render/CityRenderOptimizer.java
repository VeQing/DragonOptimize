package com.dragoncore.optimize.render;

import com.dragoncore.optimize.config.DragonOptConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CityRenderOptimizer {

    private int renderedPlayers;
    private int lastRenderDistance = -1;
    private int lastParticleSetting = -1;
    private boolean emergencyMode;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        renderedPlayers = 0;
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!DragonOptConfig.cityRenderOptimizer) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || event.getEntityPlayer() == null) return;
        EntityPlayer player = event.getEntityPlayer();
        EntityPlayerSP self = mc.player;
        if (player == self) return;

        double distanceSq = self.getDistanceSq(player);
        int near = DragonOptConfig.cityPlayerNearDistance;
        int far = emergencyMode ? near : DragonOptConfig.cityPlayerFarDistance;
        int limit = emergencyMode ? DragonOptConfig.cityEmergencyRenderedPlayers : DragonOptConfig.cityMaxRenderedPlayers;

        if (distanceSq > far * far) {
            event.setCanceled(true);
            return;
        }
        if (renderedPlayers >= limit && distanceSq > near * near) {
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
        if (!DragonOptConfig.cityRenderOptimizer) {
            restore(mc.gameSettings);
            return;
        }

        int fps = Minecraft.getDebugFPS();
        emergencyMode = fps > 0 && fps < DragonOptConfig.cityLowFpsThreshold;
        if (emergencyMode) {
            remember(mc.gameSettings);
            mc.gameSettings.particleSetting = 2;
            mc.gameSettings.renderDistanceChunks = Math.min(mc.gameSettings.renderDistanceChunks,
                    DragonOptConfig.cityEmergencyRenderDistance);
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

    public boolean isEmergencyMode() {
        return emergencyMode;
    }

    public int getRenderedPlayers() {
        return renderedPlayers;
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
