package com.dragoncore.optimize.gui;

import com.dragoncore.optimize.proxy.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class GuiEventHandler {

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (ClientProxy.KEY_OPEN_PANEL == null) return;
        if (!ClientProxy.KEY_OPEN_PANEL.isPressed()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        mc.displayGuiScreen(new DragonOptGuiMain());
    }
}
