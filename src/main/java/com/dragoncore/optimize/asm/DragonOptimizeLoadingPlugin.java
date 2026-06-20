package com.dragoncore.optimize.asm;

import com.dragoncore.optimize.asm.transformer.EntityAITransformer;
import com.dragoncore.optimize.asm.transformer.TileEntityTickTransformer;
import com.dragoncore.optimize.asm.transformer.WorldTickTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

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
