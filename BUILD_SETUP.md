# Build Setup Instructions

## Prerequisites

1. **Java 21 JDK** - Required for Minecraft 1.21+
2. **Gradle** - Either installed globally or use the wrapper

## Setting Up Gradle Wrapper

If the Gradle wrapper jar is missing, you have two options:

### Option 1: Download from another Fabric mod
Copy the `gradle/wrapper/gradle-wrapper.jar` file from any existing Fabric mod project.

### Option 2: Install Gradle and generate wrapper
```bash
# If you have Gradle installed globally:
gradle wrapper --gradle-version 9.2.1

# Or download Gradle from https://gradle.org/releases/
```

### Option 3: Use Fabric Example Mod
1. Download the Fabric Example Mod from: https://github.com/FabricMC/fabric-example-mod
2. Copy the `gradle/wrapper/gradle-wrapper.jar` to this project's `gradle/wrapper/` folder

## Building the Mod

Once the Gradle wrapper is set up:

```bash
# Windows
.\gradlew build

# Linux/Mac
./gradlew build
```

## Running in Development

```bash
# Start Minecraft client with the mod
.\gradlew runClient

# Start Minecraft server with the mod
.\gradlew runServer
```

## Output

The built jar will be located at:
`build/libs/simpledeathbans-1.0.0.jar`

## Texture Required

Don't forget to create the resurrection totem texture!
See: `src/main/resources/assets/simpledeathbans/textures/item/TEXTURE_README.md`
