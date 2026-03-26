plugins {
    id("fabric-loom")
}

version = "${property("mod.version")}+${stonecutter.current.version}"
base.archivesName = property("mod.id") as String

// MC 26.1 ships unobfuscated - no mappings needed
val isUnobfuscated = stonecutter.current.version == "26.1"
val javaVersion = if (isUnobfuscated) 25 else 21

repositories {
    maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    if (!isUnobfuscated) {
        add("mappings", loom.officialMojangMappings())
    }
    
    val modImpl = if (isUnobfuscated) "implementation" else "modImplementation"
    val modCompOnly = if (isUnobfuscated) "compileOnly" else "modCompileOnly"
    val modRunOnly = if (isUnobfuscated) "runtimeOnly" else "modRuntimeOnly"
    
    add(modImpl, "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    add(modImpl, "net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    
    // OPTIONAL - Cloth Config (not available for MC 26.1)
    if (!isUnobfuscated) {
        add(modCompOnly, "me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth_config")}") {
            exclude(group = "net.fabricmc.fabric-api")
        }
        add(modRunOnly, "me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth_config")}") {
            exclude(group = "net.fabricmc.fabric-api")
        }
    }
    
    // OPTIONAL - ModMenu
    add(modRunOnly, "com.terraformersmc:modmenu:${property("deps.modmenu")}")
    add(modCompOnly, "com.terraformersmc:modmenu:${property("deps.modmenu")}")
}

// Exclude Cloth Config screen from 26.1 (Cloth Config not available)
if (isUnobfuscated) {
    sourceSets.main {
        java.exclude("**/config/ClothConfigScreen.java")
    }
}

loom {
    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true")
        runDir = "../../run"
    }
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))
        inputs.property("java_version", javaVersion)

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),
            "java_version" to javaVersion
        )

        filesMatching("fabric.mod.json") { expand(props) }
    }
    
    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    // Builds all versions into a shared folder
    register<Copy>("buildAndCollect") {
        group = "build"
        if (isUnobfuscated) {
            from(jar.map { it.archiveFile })
        } else {
            from(named("remapJar").map { (it as Jar).archiveFile }, named("remapSourcesJar").map { (it as Jar).archiveFile })
        }
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
    
    compileJava {
        options.release = javaVersion
    }
}
