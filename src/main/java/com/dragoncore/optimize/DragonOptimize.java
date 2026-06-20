package com.dragoncore.optimize;

import com.dragoncore.optimize.config.DragonOptConfig;
import com.dragoncore.optimize.core.DragonOptimizeCore;
import com.dragoncore.optimize.dragoncore.DragonCoreCompat;
import com.dragoncore.optimize.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = DragonOptimize.MODID,
        name = "DragonOptimize",
        version = DragonOptimize.VERSION,
        dependencies = "required-after:forge@[14.23.5.2847,);after:dragoncore@[1.0,);*",
        acceptableRemoteVersions = "*",
        guiFactory = "com.dragoncore.optimize.gui.ModGuiFactory",
        useMetadata = true,
        clientSideOnly = true
)
public class DragonOptimize {

    public static final String MODID = "dragonoptimize";
    public static final String NAME = "DragonOptimize";
    public static final String VERSION = "1.0.1";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static DragonOptimize INSTANCE;

    @SidedProxy(
            clientSide = "com.dragoncore.optimize.proxy.ClientProxy",
            serverSide = "com.dragoncore.optimize.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    public DragonOptimizeCore core;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        DragonOptConfig.init(event.getSuggestedConfigurationFile());
        core = new DragonOptimizeCore();
        core.bootstrap();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        DragonCoreCompat.initialize();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (core != null) core.onServerStarting();
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        if (core != null) core.onServerStopping();
    }
}
