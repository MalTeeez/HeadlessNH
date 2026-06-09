package net.sxmaa.headlessnh;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.WorldClient;

/**
 * Drives the client through the menus and world loads, writing marker files an external orchestrator can watch
 * for progress.
 */
public final class IntegrationTestController {

    private IntegrationTestController() {}

    public static final String MARKER_MAIN_MENU = ".mainmenu.headlessnh";
    public static final String MARKER_SERVER_LOADED = ".serverloaded.headlessnh";
    public static final String MARKER_WORLD_LOADED = ".worldloaded.headlessnh";

    public enum Mode {
        SINGLEPLAYER,
        MULTIPLAYER,
        COMBINED
    }

    private enum Stage {
        MULTIPLAYER,
        SINGLEPLAYER,
        DONE
    }

    private static volatile Stage stage = null;

    private static boolean menuMultiplayerTriggered = false;
    private static boolean menuSingleplayerTriggered = false;
    private static boolean serverLoadHandled = false;
    private static boolean singleplayerLoadHandled = false;
    private static int connectFailures = 0;

    public static boolean isActive() {
        return !Boolean.getBoolean("headlessnh.active");
    }

    public static Mode mode() {
        if (Boolean.getBoolean("headlessnh.combined")) return Mode.COMBINED;
        if (Boolean.getBoolean("headlessnh.singleplayer")) return Mode.SINGLEPLAYER;
        return Mode.MULTIPLAYER;
    }

    private static Stage stage() {
        if (stage == null) {
            stage = (mode() == Mode.SINGLEPLAYER) ? Stage.SINGLEPLAYER : Stage.MULTIPLAYER;
        }
        return stage;
    }

    // Returns "singleplayer", "multiplayer" or null, handing out each at most once
    public static synchronized @Nullable String pollMainMenuAction() {
        if (isActive()) return null;
        return switch (stage()) {
            case MULTIPLAYER -> {
                if (!menuMultiplayerTriggered) {
                    menuMultiplayerTriggered = true;
                    yield "multiplayer";
                }
                yield null;
            }
            case SINGLEPLAYER -> {
                if (!menuSingleplayerTriggered) {
                    menuSingleplayerTriggered = true;
                    yield "singleplayer";
                }
                yield null;
            }
            default -> null;
        };
    }

    public static int connectRetryLimit() {
        return Integer.getInteger("headlessnh.connectRetries", 5);
    }

    // Retries a failed connection attempt until connectRetryLimit() is hit, then fails
    public static synchronized void onConnectionFailed() {
        if (isActive() || stage() != Stage.MULTIPLAYER) return;
        connectFailures++;
        if (connectFailures <= connectRetryLimit()) {
            HeadlessNH.LOG.warn("connection attempt {} failed, retrying...", connectFailures);
            menuMultiplayerTriggered = false;
            return;
        }
        fail("could not connect to the server after " + (connectFailures - 1) + " attempts");
    }

    public static void fail(String reason) {
        String message = "Integration test failed: " + reason;
        HeadlessNH.LOG.error(message);
        runOnMainThread(() -> { throw new Failure(message); });
    }

    public static final class Failure extends RuntimeException {

        Failure(String message) {
            super(message);
        }
    }

    public static void onGameStarted() throws IOException {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            writeMarker(mc, MARKER_MAIN_MENU);
        }
    }

    public static void onWorldLoaded() throws IOException {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) return;

        // Not driving the menus: keep the original behaviour of signalling every world load.
        if (isActive()) {
            writeMarker(mc, MARKER_WORLD_LOADED);
            return;
        }

        switch (stage()) {
            case MULTIPLAYER:
                if (!serverLoadHandled) {
                    serverLoadHandled = true;
                    if (mode() == Mode.COMBINED) {
                        writeMarker(mc, MARKER_SERVER_LOADED);
                        runOnMainThread(IntegrationTestController::disconnectAndAdvance);
                    } else {
                        writeMarker(mc, MARKER_WORLD_LOADED);
                        stage = Stage.DONE;
                    }
                }
                break;
            case SINGLEPLAYER:
                if (!singleplayerLoadHandled) {
                    singleplayerLoadHandled = true;
                    writeMarker(mc, MARKER_WORLD_LOADED);
                    stage = Stage.DONE;
                }
                break;
            default:
                break;
        }
    }

    private static void disconnectAndAdvance() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            if (mc.theWorld != null) {
                mc.theWorld.sendQuittingDisconnectingPacket();
            }
            mc.loadWorld((WorldClient) null);
            mc.displayGuiScreen(new GuiMainMenu());
        }
        // Flip the stage only after teardown, so a stray render frame can't emit the singleplayer marker while
        // the server world is still loaded
        stage = Stage.SINGLEPLAYER;
    }

    private static void writeMarker(Minecraft mc, String name) throws IOException {
        new File(mc.mcDataDir, name).createNewFile();
    }

    private static final Queue<Runnable> MAIN_THREAD_TASKS = new ConcurrentLinkedQueue<>();

    // Queue a task to run on the client (OpenGL context) thread; drained from the runTick inside Minecraft
    public static void runOnMainThread(Runnable task) {
        MAIN_THREAD_TASKS.add(task);
    }

    public static @Nullable Runnable pollForMainThreadTask() {
        return MAIN_THREAD_TASKS.poll();
    }
}
