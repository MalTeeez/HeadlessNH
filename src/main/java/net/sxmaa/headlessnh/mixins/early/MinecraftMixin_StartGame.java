package net.sxmaa.headlessnh.mixins.early;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;
import net.sxmaa.headlessnh.IntegrationTestController;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin_StartGame {

    @Unique
    boolean headlessNH$triggeredWorldSelection = false;

    @Unique
    boolean headlessNH$triggeredWorldCreation = false;

    @Unique
    boolean headlessNH$triggeredMultiplayerAddServer = false;

    @Unique
    boolean headlessNH$triggeredMultiplayerJoinServer = false;

    @Unique
    boolean headlessNH$triggeredMultiplayer = false;

    @Inject(method = "runTick", at = @At("HEAD"))
    public void headlessNH$drainTasks(CallbackInfo ci) {
        Runnable task;
        while ((task = IntegrationTestController.pollForMainThreadTask()) != null) {
            task.run();
        }
    }

    @Inject(method = "startGame", at = @At("TAIL"))
    public void atStartedGame(CallbackInfo ci) throws IOException {
        IntegrationTestController.onGameStarted();
    }

    @Inject(method = "displayGuiScreen", at = @At("TAIL"))
    public void changedScreen(GuiScreen guiScreenIn, CallbackInfo ci) {
        if (!Boolean.getBoolean("headlessnh.active")) return;
        if (guiScreenIn instanceof GuiMainMenu mainMenu) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    String action = IntegrationTestController.pollMainMenuAction();
                    if (action == null) return;
                    // Singleplayer is 1, Multiplayer is 2
                    int buttonId = action.equals("singleplayer") ? 1 : 2;
                    IntegrationTestController.runOnMainThread(() -> mainMenu.actionPerformed(new GuiButton(buttonId, 0, 0, null)));
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            }).start();
        } else if (guiScreenIn instanceof GuiSelectWorld selectWorld && !headlessNH$triggeredWorldSelection) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    headlessNH$triggeredWorldSelection = true;
                    IntegrationTestController.runOnMainThread(() -> selectWorld.actionPerformed(new GuiButton(3, 0, 0, null)));
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            }).start();
        } else if (guiScreenIn instanceof GuiCreateWorld createWorld && !headlessNH$triggeredWorldCreation) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    if (createWorld.field_146333_g == null || createWorld.field_146335_h == null) {
                        Thread.sleep(2500);
                    }
                    headlessNH$triggeredWorldCreation = true;
                    IntegrationTestController.runOnMainThread(() -> createWorld.actionPerformed(new GuiButton(0, 0, 0, null)));
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            }).start();
        } else if (guiScreenIn instanceof GuiMultiplayer multiplayer && !headlessNH$triggeredMultiplayer) {
            headlessNH$triggeredMultiplayer = true;
            new Thread(() -> {
                try {
                    if (!headlessNH$triggeredMultiplayerAddServer) {
                        Thread.sleep(500);
                        headlessNH$triggeredMultiplayerAddServer = true;
                        IntegrationTestController.runOnMainThread(() -> {
                            multiplayer.field_146806_v = true;
                            multiplayer.field_146811_z = new ServerData(
                                I18n.format("selectServer.defaultName", new Object[0]),
                                "");
                            multiplayer.field_146811_z.serverName = "locahost!";
                            multiplayer.field_146811_z.serverIP = "127.0.0.1";
                            multiplayer.confirmClicked(true, 0);
                        });
                    }
                    if (!headlessNH$triggeredMultiplayerJoinServer) {
                        Thread.sleep(500);
                        headlessNH$triggeredMultiplayerJoinServer = true;
                        IntegrationTestController.runOnMainThread(() -> multiplayer.func_146790_a(0));
                        Thread.sleep(250);
                        IntegrationTestController.runOnMainThread(() -> multiplayer.actionPerformed(multiplayer.field_146809_s));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            }).start();
        } else if (guiScreenIn instanceof GuiDisconnected) {
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    IntegrationTestController.onConnectionFailed();
                    // Re-arm the join (without re-adding the server, to avoid duplicate list entries) in case we
                    // retry, then return to the title screen so the menu flow drives the next step
                    headlessNH$triggeredMultiplayer = false;
                    headlessNH$triggeredMultiplayerJoinServer = false;
                    IntegrationTestController.runOnMainThread(
                        () -> Minecraft.getMinecraft()
                            .displayGuiScreen(new GuiMainMenu()));
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }
            }).start();
        }
    }
}
