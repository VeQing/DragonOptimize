package com.dragoncore.optimize;

import com.dragoncore.optimize.config.OptConfig;
import com.dragoncore.optimize.render.CityRenderOptimizer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = "dragonoptimize",
        name = "DragonOptimize",
        version = "1.0.1",
        acceptableRemoteVersions = "*",
        useMetadata = false,
        clientSideOnly = true
)
public class DragonOptimize {

    public static final String MODID = "dragonoptimize";
    public static final String VERSION = "1.0.1";

    @Mod.Instance("dragonoptimize")
    public static DragonOptimize INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        OptConfig.load();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (OptConfig.cityRenderOptimizer) {
            MinecraftForge.EVENT_BUS.register(new CityRenderOptimizer());
        }
    }
}
