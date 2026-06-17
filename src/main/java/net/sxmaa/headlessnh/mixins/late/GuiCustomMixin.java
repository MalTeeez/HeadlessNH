package net.sxmaa.headlessnh.mixins.late;

import java.io.IOException;

import net.sxmaa.headlessnh.HeadlessNH;
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
                HeadlessNH.LOG
                    .info("Main menu reached, settling {}ms", IntegrationTestController.mainMenuSettleMillis());
                Thread.sleep(IntegrationTestController.mainMenuSettleMillis());
                if (IntegrationTestController.onMainMenuReached()) {
                    Thread.sleep(IntegrationTestController.markerCooldownMillis());
                    IntegrationTestController.awaitMainMenuGate();
                }
                String action = IntegrationTestController.pollMainMenuAction();
                if (action == null) return;
                IntegrationTestController
                    .runOnMainThread(() -> new ActionOpenGUI(action).perform(null, (GuiCustom) ((Object) this)));
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
