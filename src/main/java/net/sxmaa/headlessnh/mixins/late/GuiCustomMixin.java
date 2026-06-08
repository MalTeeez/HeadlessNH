package net.sxmaa.headlessnh.mixins.late;

import net.sxmaa.headlessnh.HeadlessNH;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lumien.custommainmenu.gui.GuiCustom;
import lumien.custommainmenu.lib.actions.ActionOpenGUI;

@Mixin(value = GuiCustom.class, remap = false)
public class GuiCustomMixin {

    @Unique
    boolean headlessNH$triggeredMainMenu = false;

    @Dynamic
    @Inject(method = "func_73866_w_", at = @At("TAIL"))
    private void afterInitGui(CallbackInfo ci) {
        if (!headlessNH$triggeredMainMenu && Boolean.getBoolean("headlessnh.active")) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    headlessNH$triggeredMainMenu = true;
                    HeadlessNH.runOnMainThread(
                        () -> new ActionOpenGUI(
                            Boolean.getBoolean("headlessnh.singleplayer") ? "singleplayer" : "multiplayer")
                                .perform(null, (GuiCustom) ((Object) this)));
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            }).start();
        }
    }
}
