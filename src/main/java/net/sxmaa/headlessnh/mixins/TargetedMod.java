package net.sxmaa.headlessnh.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

import cpw.mods.fml.common.Loader;

public enum TargetedMod implements ITargetMod {

    ANGELICA("com.gtnewhorizons.angelica.loading.AngelicaTweaker", "angelica");

    private final TargetModBuilder builder;
    private final String id;

    TargetedMod(String modId) {
        this(null, modId, null);
    }

    TargetedMod(String coreModClass, String modId) {
        this(coreModClass, modId, null);
    }

    TargetedMod(String coreModClass, String modId, String targetClass) {
        this.id = modId;
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass)
            .setModId(modId)
            .setTargetClass(targetClass);
    }

    public boolean isLoaded() {
        return Loader.isModLoaded(id);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
