package net.sxmaa.headlessnh;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Unique;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = HeadlessNH.MODID, version = Tags.VERSION, name = "HeadlessNH", acceptedMinecraftVersions = "[1.7.10]")
public class HeadlessNH {

    public static final String MODID = "headlessnh";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "net.sxmaa.headlessnh.ClientProxy", serverSide = "net.sxmaa.headlessnh.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    public static void onGameStarted() throws IOException {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            File file = new File(Minecraft.getMinecraft().mcDataDir, ".mainmenu.headlessnh");
            file.createNewFile();
        }
    }

    public static void onWorldLoaded() throws IOException {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.theWorld != null) {
            File file = new File(Minecraft.getMinecraft().mcDataDir, ".worldloaded.headlessnh");
            file.createNewFile();
        }
    }

    @Unique
    public static final Queue<Runnable> mainThreadTasks = new ConcurrentLinkedQueue<>();

    // to run on thread with opengl context
    @Unique
    public static void runOnMainThread(Runnable task) {
        mainThreadTasks.add(task);
    }

    public static @Nullable Runnable pollForMainThreadTask() {
        return mainThreadTasks.poll();
    }
}
