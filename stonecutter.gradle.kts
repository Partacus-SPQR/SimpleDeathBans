plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.15.5" apply false
}

stonecutter active "26.2" /* [SC] DO NOT EDIT */

stonecutter parameters {
    swaps["mod_version"] = "\"" + property("mod.version") + "\";"
    swaps["minecraft"] = "\"" + node.metadata.version + "\";"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}
