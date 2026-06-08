package net.sxmaa.headlessnh;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sxmaa.headlessnh.mixins.Mixins;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixinLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
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

    @Override
    public String getMixinConfig() {
        return "mixins.headlessnh.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}
