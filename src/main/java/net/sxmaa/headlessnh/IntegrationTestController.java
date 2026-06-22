package net.sxmaa.headlessnh;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;

/**
 * Drives the client through the menus and world loads, writing marker files an external orchestrator can watch
 * for progress.
 */
public final class IntegrationTestController {

    private IntegrationTestController() {}

    public static final String MARKER_MAIN_MENU = ".mainmenu.headlessnh";
    public static final String MARKER_SERVER_LOADED = ".serverloaded.headlessnh";
    public static final String MARKER_WORLD_LOADED = ".worldloaded.headlessnh";

    // Marker file names default to the constants above but can be overridden individually so an orchestrator can
    // pick its own filenames.
    private static String markerMainMenuName() {
        return System.getProperty("headlessnh.marker.mainmenu", MARKER_MAIN_MENU);
    }

    private static String markerServerLoadedName() {
        return System.getProperty("headlessnh.marker.serverloaded", MARKER_SERVER_LOADED);
    }

    private static String markerWorldLoadedName() {
        return System.getProperty("headlessnh.marker.worldloaded", MARKER_WORLD_LOADED);
    }

    // Directory the marker files are written into; defaults to the Minecraft data dir when unset.
    private static File markerDir(Minecraft mc) {
        String configured = System.getProperty("headlessnh.markerdir");
        return (configured != null && !configured.isEmpty()) ? new File(configured) : mc.mcDataDir;
    }

    // Optional gate files (resolved under the marker dir): when set the controller blocks after a stage's marker
    // has been written until the gate file appears, letting an external orchestrator step the test forward one
    // stage at a time. When unset the stage proceeds immediately.
    private static @Nullable String gateMainMenuName() {
        return emptyToNull(System.getProperty("headlessnh.gate.mainmenu"));
    }

    private static @Nullable String gateServerLoadedName() {
        return emptyToNull(System.getProperty("headlessnh.gate.serverloaded"));
    }

    private static @Nullable String gateWorldLoadedName() {
        return emptyToNull(System.getProperty("headlessnh.gate.worldloaded"));
    }

    // How long to wait for a gate file before failing the test, in milliseconds. 0 or negative waits forever.
    public static long gateTimeoutMillis() {
        return Long.getLong("headlessnh.gate.timeout", 0L);
    }

    private static final long GATE_POLL_INTERVAL_MILLIS = 250L;

    private static @Nullable String emptyToNull(@Nullable String value) {
        return (value != null && !value.isEmpty()) ? value : null;
    }

    public enum Mode {
        SINGLEPLAYER,
        MULTIPLAYER,
        COMBINED,
        NONE
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

    // Tracks whether we expect the in-game menu to open; only a menu seen while this is set drives a teardown.
    private static volatile boolean teardownRequested = false;

    public static synchronized boolean consumeTeardownRequest() {
        boolean requested = teardownRequested;
        teardownRequested = false;
        return requested;
    }

    public static boolean isActive() {
        return !Boolean.getBoolean("headlessnh.active");
    }

    public static Mode mode() {
        if (Boolean.getBoolean("headlessnh.combined")) return Mode.COMBINED;
        if (Boolean.getBoolean("headlessnh.singleplayer")) return Mode.SINGLEPLAYER;
        return Mode.NONE;
    }

    private static Stage stage() {
        if (stage == null) {
            stage = switch (mode()) {
                case MULTIPLAYER, COMBINED -> Stage.MULTIPLAYER;
                case SINGLEPLAYER -> Stage.SINGLEPLAYER;
                case NONE -> Stage.DONE;
            };
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

    // How long to stay in the multiplayer world after joining before tearing down and advancing, in milliseconds.
    public static long serverJoinSettleMillis() {
        return Long.getLong("headlessnh.delay.serverjoin", 7500L);
    }

    // How long to wait on the main menu before clicking the next button, in milliseconds.
    public static long mainMenuSettleMillis() {
        return Long.getLong("headlessnh.delay.mainmenu", 500L);
    }

    // How long to stay in the singleplayer world after loading before tearing down and advancing, in milliseconds.
    public static long singleplayerSettleMillis() {
        return Long.getLong("headlessnh.delay.singleplayer", 7500L);
    }

    // How long to wait after writing any marker before proceeding, in milliseconds.
    public static long markerCooldownMillis() {
        return Long.getLong("headlessnh.delay.cooldown", 0L);
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
        fail("could not connect to server after " + (connectFailures - 1) + " attempts");
    }

    public static void awaitMainMenuGate() throws InterruptedException {
        awaitGate(gateMainMenuName());
    }

    // Returns false (and fails the test) only on timeout; true when the gate appears or isn't configured.
    private static boolean awaitGate(@Nullable String gateName) throws InterruptedException {
        if (gateName == null) return true;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return true;
        File gate = new File(markerDir(mc), gateName);
        long timeout = gateTimeoutMillis();
        long deadline = timeout > 0 ? System.currentTimeMillis() + timeout : Long.MAX_VALUE;
        HeadlessNH.LOG.info("Waiting for gate file {}", gate);
        while (!gate.exists()) {
            if (System.currentTimeMillis() >= deadline) {
                fail("timed out after " + timeout + "ms waiting for gate file " + gate);
                return false;
            }
            Thread.sleep(GATE_POLL_INTERVAL_MILLIS);
        }
        HeadlessNH.LOG.info("Gate file {} appeared, proceeding", gate);
        return true;
    }

    // Logs each setting's resolved value and whether it came from a system property or the built-in default.
    public static void logSettings() {
        Minecraft mc = Minecraft.getMinecraft();
        HeadlessNH.LOG.info("HeadlessNH settings:");
        HeadlessNH.LOG.info("  automation: active={} mode={} stage={}", !isActive(), mode(), stage());
        logBool("headlessnh.active");
        logBool("headlessnh.combined");
        logBool("headlessnh.singleplayer");
        logInt("headlessnh.connectRetries", connectRetryLimit());
        logMillis("headlessnh.delay.mainmenu", mainMenuSettleMillis());
        logMillis("headlessnh.delay.serverjoin", serverJoinSettleMillis());
        logMillis("headlessnh.delay.singleplayer", singleplayerSettleMillis());
        logMillis("headlessnh.delay.cooldown", markerCooldownMillis());
        logMillis("headlessnh.gate.timeout", gateTimeoutMillis());
        logStr("headlessnh.markerdir", mc != null ? markerDir(mc).getPath() : "<unknown>");
        logStr("headlessnh.marker.mainmenu", markerMainMenuName());
        logStr("headlessnh.marker.serverloaded", markerServerLoadedName());
        logStr("headlessnh.marker.worldloaded", markerWorldLoadedName());
        logStr("headlessnh.gate.mainmenu", String.valueOf(gateMainMenuName()));
        logStr("headlessnh.gate.serverloaded", String.valueOf(gateServerLoadedName()));
        logStr("headlessnh.gate.worldloaded", String.valueOf(gateWorldLoadedName()));
    }

    private static void logBool(String key) {
        logLine(key, String.valueOf(Boolean.getBoolean(key)));
    }

    private static void logInt(String key, int resolved) {
        logLine(key, String.valueOf(resolved));
    }

    private static void logMillis(String key, long resolved) {
        logLine(key, resolved + "ms");
    }

    private static void logStr(String key, String resolved) {
        logLine(key, resolved);
    }

    private static void logLine(String key, String resolved) {
        String raw = System.getProperty(key);
        if (raw != null) {
            HeadlessNH.LOG.info("  {} = {} (system property \"{}\")", key, resolved, raw);
        } else {
            HeadlessNH.LOG.info("  {} = {} (default)", key, resolved);
        }
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

    private static boolean mainMenuMarkerWritten = false;

    public static void onGameStarted() throws IOException {
        if (!isActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            writeMarker(mc, markerMainMenuName());
        }
    }

    public static synchronized boolean onMainMenuReached() throws IOException {
        if (mainMenuMarkerWritten) return false;
        mainMenuMarkerWritten = true;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            writeMarker(mc, markerMainMenuName());
        }
        return true;
    }

    public static void onWorldLoaded() throws IOException {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) return;

        // Not driving the menus: keep the original behaviour of signalling every world load.
        if (isActive()) {
            writeMarker(mc, markerWorldLoadedName());
            return;
        }

        // Arm the settle timer off the player being in the world.
        if (mc.thePlayer == null) return;

        switch (stage()) {
            case MULTIPLAYER:
                if (!serverLoadHandled) {
                    serverLoadHandled = true;
                    runOnMainThread(
                        () -> disconnectAndAdvance(
                            serverJoinSettleMillis(),
                            markerServerLoadedName(),
                            gateServerLoadedName()));
                }
                break;
            case SINGLEPLAYER:
                // Only treat this as the singleplayer world load once we're actually in a singleplayer world; while
                // the multiplayer server is being torn down render frames can still fire with stage == SINGLEPLAYER.
                if (!singleplayerLoadHandled && mc.isSingleplayer()) {
                    singleplayerLoadHandled = true;
                    stage = Stage.DONE;
                    runOnMainThread(
                        () -> disconnectAndAdvance(
                            singleplayerSettleMillis(),
                            markerWorldLoadedName(),
                            gateWorldLoadedName()));
                }
                break;
            default:
                break;
        }
    }

    private static void disconnectAndAdvance(long settleMillis, String markerName, @Nullable String gateName) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            new Thread(() -> {
                try {
                    HeadlessNH.LOG
                        .info("Trigger reached for {}, settling {}ms before writing marker", markerName, settleMillis);
                    Thread.sleep(settleMillis);
                    writeMarker(mc, markerName);
                    Thread.sleep(markerCooldownMillis());

                    // Bail without advancing if the gate timed out (already reported as a failure).
                    if (!awaitGate(gateName)) return;

                    runOnMainThread(() -> {
                        teardownRequested = true;
                        // displayInGameMenu() no-ops when a screen is already open, so open the menu directly to
                        // guarantee the teardown fires even if a stray menu was already up.
                        mc.displayGuiScreen(new GuiIngameMenu());

                        // Flip the stage only after teardown, so a stray render frame can't emit the singleplayer
                        // marker while the server world is still loaded
                        stage = Stage.SINGLEPLAYER;
                    });
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private static void writeMarker(Minecraft mc, String name) throws IOException {
        File dir = markerDir(mc);
        dir.mkdirs();
        if (new File(dir, name).createNewFile()) {
            HeadlessNH.LOG.info("Created marker {} at {}", name, dir);
            return;
        }
        throw new RuntimeException("Failed to create HeadlessNH marker \"" + name + "\" at " + dir);
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
