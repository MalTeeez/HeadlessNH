package net.sxmaa.headlessnh.mixins.late;

import java.io.IOException;

import net.sxmaa.headlessnh.HeadlessNH;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.CeleritasWorldRenderer", remap = false)
public class CeleritasWorldRendererMixin {

    @Dynamic
    @Inject(method = "drawChunkLayer", at = @At("RETURN"))
    public void onDrawChunkLayer(CallbackInfo ci) throws IOException {
        HeadlessNH.onWorldLoaded();
    }
}
