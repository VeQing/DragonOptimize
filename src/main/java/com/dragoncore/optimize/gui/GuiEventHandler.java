package com.dragoncore.optimize.gui;

import com.dragoncore.optimize.DragonOptimize;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * 客户端 GUI 事件入口：注册热键并在按下时弹出面板。
 */
public class GuiEventHandler {

    public static final KeyBinding OPEN_PANEL = new KeyBinding(
            "dragonoptimize.key.open", Keyboard.KEY_O, "dragonoptimize.key.category");

    static {
        try {
            ClientRegistry.registerKeyBinding(OPEN_PANEL);
        } catch (Throwable t) {
            DragonOptimize.LOGGER.warn("[DragonOptimize] 热键注册失败：{}", t.toString());
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (OPEN_PANEL.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null) {
                mc.displayGuiScreen(new DragonOptGuiMain());
            }
        }
    }
}
