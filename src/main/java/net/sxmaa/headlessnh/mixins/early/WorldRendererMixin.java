package net.sxmaa.headlessnh.mixins.early;

import java.io.IOException;

import net.minecraft.client.renderer.WorldRenderer;
import net.sxmaa.headlessnh.IntegrationTestController;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "updateRenderer", at = @At("RETURN"))
    public void updateRenderer(CallbackInfo ci) throws IOException {
        IntegrationTestController.onWorldLoaded();
    }
}
