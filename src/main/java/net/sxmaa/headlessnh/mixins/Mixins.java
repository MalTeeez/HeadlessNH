package net.sxmaa.headlessnh.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // spotless:off
    JOIN_WORLD_ON_MAINMENU_LOAD(
        new MixinBuilder()
            .setPhase(Phase.EARLY)
            .addClientMixins(
                "MinecraftMixin"
            )),
    JOIN_WORLD_ON_MAINMENU_LOAD_CMM(
        new MixinBuilder()
            .setPhase(Phase.LATE)
            .addRequiredMod(TargetedMod.CUSTOM_MAIN_MENU)
            .addClientMixins(
                "GuiCustomMixin"
            )),
    NOTIFY_OF_FINISHED_WORLD_LOAD(
        new MixinBuilder()
            .setPhase(Phase.EARLY)
            .addExcludedMod(TargetedMod.ANGELICA)
            .addClientMixins(
                "WorldRendererMixin"
            )),
    NOTIFY_OF_FINISHED_WORLD_LOAD_ANGELICA(
        new MixinBuilder()
            .setPhase(Phase.LATE)
            .addRequiredMod(TargetedMod.ANGELICA)
            .addClientMixins(
                "CeleritasWorldRendererMixin"
            ))
    ;
    // spotless:on

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
