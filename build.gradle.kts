
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

minecraft {
    extraRunJvmArguments.addAll(
        "-Dmixin.debug.export=true",
        "-Dmixin.dumpTargetOnFailure=true",
        "-Dmixin.debug.verbose=true",
        "-Dheadlessnh.active=true",
        "-Dheadlessnh.singleplayer=true",
        "-Dheadlessnh.forcefocus=true",
    )
}
