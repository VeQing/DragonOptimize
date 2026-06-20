package com.dragoncore.optimize.render;

import com.dragoncore.optimize.config.DragonOptConfig;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class RenderEventHandler {

    private int renderedParticles;

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<?> event) {
        if (DragonOptConfig.disableEntityShadows && event.getEntity() != null) {
        }
    }

    @SubscribeEvent
    public void onFogColor(EntityViewRenderEvent.FogColors event) {
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (DragonOptConfig.lazyChunkLoad) {
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        renderedParticles = 0;
    }

    @SubscribeEvent
    public void onPlaySound(PlaySoundAtEntityEvent event) {
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
    }
}
