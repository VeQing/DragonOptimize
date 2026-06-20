package com.dragoncore.optimize.proxy;

import com.dragoncore.optimize.gui.GuiEventHandler;
import com.dragoncore.optimize.render.CityRenderOptimizer;
import com.dragoncore.optimize.render.RenderEventHandler;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;

public class ClientProxy extends CommonProxy {

    public static KeyBinding KEY_OPEN_PANEL;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        KEY_OPEN_PANEL = new KeyBinding(
                "dragonoptimize.key.open",
                Keyboard.KEY_O,
                "dragonoptimize.key.category");
        ClientRegistry.registerKeyBinding(KEY_OPEN_PANEL);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new GuiEventHandler());
        MinecraftForge.EVENT_BUS.register(new RenderEventHandler());
        MinecraftForge.EVENT_BUS.register(new CityRenderOptimizer());
    }
}
