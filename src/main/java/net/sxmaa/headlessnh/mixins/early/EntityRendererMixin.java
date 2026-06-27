package net.sxmaa.headlessnh.mixins.early;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(value = EntityRenderer.class)
public class EntityRendererMixin {

    @Definition(id = "prevFrameTime", field = "Lnet/minecraft/client/renderer/EntityRenderer;prevFrameTime:J")
    @Expression("? - this.prevFrameTime > 500")
    @ModifyExpressionValue(method = "updateCameraAndRender", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean preventOutOfFocusIngameMenu(boolean original) {
        return !Boolean.getBoolean("headlessnh.forcefocus");
    }
}
