package net.sxmaa.headlessnh.mixins.late;

import net.sxmaa.headlessnh.IntegrationTestController;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lumien.custommainmenu.gui.GuiCustom;
import lumien.custommainmenu.lib.actions.ActionOpenGUI;

@Mixin(value = GuiCustom.class, remap = false)
public class GuiCustomMixin {

    @Dynamic
    @Inject(method = "func_73866_w_", at = @At("TAIL"))
    private void afterInitGui(CallbackInfo ci) {
        if (IntegrationTestController.isActive()) return;
        new Thread(() -> {
            try {
                Thread.sleep(500);
                String action = IntegrationTestController.pollMainMenuAction();
                if (action == null) return;
                IntegrationTestController
                    .runOnMainThread(() -> new ActionOpenGUI(action).perform(null, (GuiCustom) ((Object) this)));
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }
        }).start();
    }
}
