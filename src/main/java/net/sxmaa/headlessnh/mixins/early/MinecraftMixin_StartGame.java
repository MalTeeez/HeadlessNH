package net.sxmaa.headlessnh.mixins.early;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.sxmaa.headlessnh.HeadlessNH;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin_StartGame {

    @Unique
    boolean headlessNH$triggeredInitial = false;

    @Unique
    private final Queue<Runnable> headlessNH$mainThreadTasks = new ConcurrentLinkedQueue<>();

    // to run on thread with opengl context
    @Unique
    private void headlessNH$runOnMainThread(Runnable task) {
        headlessNH$mainThreadTasks.add(task);
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    public void headlessNH$drainTasks(CallbackInfo ci) {
        Runnable task;
        while ((task = headlessNH$mainThreadTasks.poll()) != null) {
            task.run();
        }
    }

    @Inject(method = "startGame", at = @At("TAIL"))
    public void atStartedGame(CallbackInfo ci) throws IOException {
        HeadlessNH.onGameStarted();
    }

    @Inject(method = "displayGuiScreen", at = @At("TAIL"))
    public void changedScreen(GuiScreen guiScreenIn, CallbackInfo ci) {
        if (guiScreenIn instanceof GuiMainMenu mainMenu && !headlessNH$triggeredInitial) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    headlessNH$runOnMainThread(() -> mainMenu.actionPerformed(new GuiButton(1, 0, 0, null)));
                    headlessNH$triggeredInitial = true;
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            }).start();
        } else if (guiScreenIn instanceof GuiSelectWorld selectWorld) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    headlessNH$runOnMainThread(() -> selectWorld.actionPerformed(new GuiButton(3, 0, 0, null)));
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            }).start();
        } else if (guiScreenIn instanceof GuiCreateWorld createWorld) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    if (createWorld.field_146333_g == null || createWorld.field_146335_h == null) {
                        Thread.sleep(2500);
                    }
                    headlessNH$runOnMainThread(() -> createWorld.actionPerformed(new GuiButton(0, 0, 0, null)));
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            }).start();
        }
    }
}
