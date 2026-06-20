package com.dragoncore.optimize.asm;

import com.dragoncore.optimize.asm.transformer.EntityAITransformer;
import com.dragoncore.optimize.asm.transformer.TileEntityTickTransformer;
import com.dragoncore.optimize.asm.transformer.WorldTickTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * DragonOptimize 的 FML Core Mod 加载插件。
 *
 * <p>通过 ASM 在启动期完成以下关键注入：</p>
 * <ul>
 * <li>{@link WorldTickTransformer}：重定向 {@code WorldServer.tick()} 中串行的实体/tile 循环，
 * 将其派发至并行调度器。</li>
 * <li>{@link TileEntityTickTransformer}：让 TileEntity 的 tick 支持按区块分片并行执行。</li>
 * <li>{@link EntityAITransformer}：将重计算型 AI 任务异步化，避免阻塞主线程。</li>
 * </ul>
 */
@IFMLLoadingPlugin.Name("DragonOptimizeCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"com.dragoncore.optimize.asm"})
public class DragonOptimizeLoadingPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                WorldTickTransformer.class.getName(),
                TileEntityTickTransformer.class.getName(),
                EntityAITransformer.class.getName()
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
