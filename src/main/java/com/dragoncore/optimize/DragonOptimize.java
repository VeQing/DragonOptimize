package com.dragoncore.optimize;

import com.dragoncore.optimize.config.DragonOptConfig;
import com.dragoncore.optimize.core.DragonOptimizeCore;
import com.dragoncore.optimize.dragoncore.DragonCoreCompat;
import com.dragoncore.optimize.gui.GuiEventHandler;
import com.dragoncore.optimize.gui.ModGuiFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DragonOptimize - 面向 Minecraft 1.12.2 / Java 8 的客户端性能优化模组。
 *
 * <p>主要特性：</p>
 * <ul>
 * <li>可配置的性能 / 品质预设（性能 / 平衡 / 品质 / 自定义）</li>
 * <li>并行实体 tick、TileEntity tick、区块 tick 调度，缓解 1.12.2 单核瓶颈</li>
 * <li>针对龙核 (DragonCore) 服务器的兼容性检测与协议适配</li>
 * <li>客户端 GUI 用于热键切换预设与细项调优</li>
 * </ul>
 */
@Mod(
        modid = DragonOptimize.MODID,
        name = "DragonOptimize",
        version = "1.0.0",
        dependencies = "required-after:forge@[14.23.5.2847,);after:dragoncore@[1.0,)",
        acceptableRemoteVersions = "*",
        guiFactory = "com.dragoncore.optimize.gui.ModGuiFactory",
        useMetadata = true
)
public class DragonOptimize {

    public static final String MODID = "dragonoptimize";
    public static final String NAME = "DragonOptimize";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static DragonOptimize INSTANCE;

    public DragonOptimizeCore core;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        DragonOptConfig.init(event.getSuggestedConfigurationFile());
        core = new DragonOptimizeCore();
        core.bootstrap();
        MinecraftForge.EVENT_BUS.register(new GuiEventHandler());
        LOGGER.info("[DragonOptimize] preInit 完成，启用的预设：{}", DragonOptConfig.currentPreset());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded("dragoncore")) {
            DragonCoreCompat.initialize();
        } else {
            LOGGER.info("[DragonOptimize] 未检测到龙核 (DragonCore)，兼容层未启用。");
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        core.onServerStarting();
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        core.onServerStopping();
    }
}
