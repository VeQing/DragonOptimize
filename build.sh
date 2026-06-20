#!/usr/bin/env bash
# DragonOptimize build script for Java 8
# Uses stub classes (no Forge dependency) to compile source code
# Only packages our own classes into the jar (not the stubs)

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
STUB_DIR="$BUILD_DIR/stubs"
STUB_CLASS_DIR="$BUILD_DIR/stub_classes"
CLASS_DIR="$BUILD_DIR/classes"
RELEASE_DIR="$PROJECT_DIR/release"
SRC_DIR="$PROJECT_DIR/src/main/java"

# Clean
rm -rf "$BUILD_DIR" "$RELEASE_DIR"
mkdir -p "$STUB_DIR" "$STUB_CLASS_DIR" "$CLASS_DIR" "$RELEASE_DIR"

echo "============================================"
echo " DragonOptimize Build"
echo "============================================"
echo ""
echo "Project: $PROJECT_DIR"
echo ""

# Create all stub classes (minimal Forge/Minecraft API)
echo "Creating stub classes..."

cat > "$STUB_DIR/Mod.java" << 'EOF'
package net.minecraftforge.fml.common;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String modid();
    String name() default "";
    String version() default "";
    String dependencies() default "";
    String acceptableRemoteVersions() default "";
    String guiFactory() default "";
    boolean useMetadata() default false;
    boolean clientSideOnly() default false;
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Instance { String value() default ""; }
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface EventHandler { }
}
EOF

cat > "$STUB_DIR/SubscribeEvent.java" << 'EOF'
package net.minecraftforge.fml.common.eventhandler;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent { }
EOF

cat > "$STUB_DIR/Event.java" << 'EOF'
package net.minecraftforge.fml.common.eventhandler;
public class Event {
    private boolean canceled;
    public boolean isCancelable() { return true; }
    public boolean isCanceled() { return canceled; }
    public void setCanceled(boolean cancel) { this.canceled = cancel; }
    public void setCanceled() { this.canceled = true; }
}
EOF

cat > "$STUB_DIR/EventBus.java" << 'EOF'
package net.minecraftforge.fml.common.eventhandler;
public class EventBus {
    public void register(Object target) {}
    public void unregister(Object target) {}
    public void post(Event event) {}
}
EOF

cat > "$STUB_DIR/MinecraftForge.java" << 'EOF'
package net.minecraftforge.common;
import net.minecraftforge.fml.common.eventhandler.EventBus;
public class MinecraftForge {
    public static final EventBus EVENT_BUS = new EventBus();
}
EOF

cat > "$STUB_DIR/FMLStateEvent.java" << 'EOF'
package net.minecraftforge.fml.common.event;
public class FMLStateEvent {}
EOF

cat > "$STUB_DIR/FMLPreInitializationEvent.java" << 'EOF'
package net.minecraftforge.fml.common.event;
public class FMLPreInitializationEvent extends FMLStateEvent {
    public java.io.File getSuggestedConfigurationFile() { return null; }
    public java.io.File getModConfigurationDirectory() { return null; }
}
EOF

cat > "$STUB_DIR/FMLInitializationEvent.java" << 'EOF'
package net.minecraftforge.fml.common.event;
public class FMLInitializationEvent extends FMLStateEvent {}
EOF

cat > "$STUB_DIR/Minecraft.java" << 'EOF'
package net.minecraft.client;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();
    public EntityPlayerSP player;
    public GameSettings gameSettings;
    public static Minecraft getMinecraft() { return INSTANCE; }
    public static int getDebugFPS() { return 0; }
}
EOF

cat > "$STUB_DIR/EntityPlayerSP.java" << 'EOF'
package net.minecraft.client.entity;
import net.minecraft.entity.player.EntityPlayer;
public class EntityPlayerSP extends EntityPlayer {
}
EOF

cat > "$STUB_DIR/EntityPlayer.java" << 'EOF'
package net.minecraft.entity.player;
public class EntityPlayer {
    public double posX;
    public double posY;
    public double posZ;
}
EOF

cat > "$STUB_DIR/GameSettings.java" << 'EOF'
package net.minecraft.client.settings;
public class GameSettings {
    public int particleSetting;
    public int renderDistanceChunks;
}
EOF

cat > "$STUB_DIR/RenderPlayerEvent.java" << 'EOF'
package net.minecraftforge.client.event;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;
public class RenderPlayerEvent extends Event {
    public EntityPlayer entityPlayer;
    public EntityPlayer getEntityPlayer() { return entityPlayer; }
    public static class Pre extends RenderPlayerEvent {}
    public static class Post extends RenderPlayerEvent {}
    public static class Specials extends RenderPlayerEvent {}
}
EOF

cat > "$STUB_DIR/TickEvent.java" << 'EOF'
package net.minecraftforge.fml.common.gameevent;
import net.minecraftforge.fml.common.eventhandler.Event;
public class TickEvent extends Event {
    public enum Type { WORLD, CLIENT, SERVER, PLAYER, RENDER }
    public enum Phase { START, END }
    public Type type;
    public Phase phase;

    public static class ClientTickEvent extends TickEvent {}
    public static class WorldTickEvent extends TickEvent {}
    public static class PlayerTickEvent extends TickEvent {}
    public static class RenderTickEvent extends TickEvent {}
    public static class ServerTickEvent extends TickEvent {}
}
EOF

echo "Stub classes created."
echo ""

# Compile stubs into SEPARATE directory (these won't go into the jar)
echo "Compiling stub classes..."
javac -d "$STUB_CLASS_DIR" -sourcepath "$STUB_DIR" -encoding UTF-8 "$STUB_DIR"/*.java
echo "Stub classes compiled."
echo ""

# Compile source into OWN classes directory
echo "Compiling source code..."
javac -d "$CLASS_DIR" -sourcepath "$SRC_DIR" -cp "$STUB_CLASS_DIR" -encoding UTF-8 \
    "$SRC_DIR/com/dragoncore/optimize/DragonOptimize.java" \
    "$SRC_DIR/com/dragoncore/optimize/config/OptConfig.java" \
    "$SRC_DIR/com/dragoncore/optimize/render/CityRenderOptimizer.java"
echo "Source code compiled."
echo ""

# Verify - show what classes were compiled
echo "Compiled classes:"
find "$CLASS_DIR" -name "*.class" -type f | sort
echo ""

# Copy resources (mcmod.info)
echo "Copying resources..."
cp "$PROJECT_DIR/src/main/resources/mcmod.info" "$CLASS_DIR/mcmod.info"

# Create MANIFEST.MF - important: extra blank line for jar manifest format
mkdir -p "$CLASS_DIR/META-INF"
printf 'Manifest-Version: 1.0\nImplementation-Title: DragonOptimize\nImplementation-Version: 1.0.1\nFMLModType: MOD\n' > "$CLASS_DIR/META-INF/MANIFEST.MF"

# Create jar - only our classes + resources (NO stub classes)
echo "Creating jar..."
cd "$CLASS_DIR"
jar cfm "$RELEASE_DIR/dragonoptimize-1.0.1.jar" "META-INF/MANIFEST.MF" .
cd "$PROJECT_DIR"

echo ""
echo "============================================"
echo " BUILD COMPLETE"
echo "============================================"
echo "Jar: $RELEASE_DIR/dragonoptimize-1.0.1.jar"
echo ""
ls -la "$RELEASE_DIR/dragonoptimize-1.0.1.jar"

echo ""
echo "Jar contents:"
jar tf "$RELEASE_DIR/dragonoptimize-1.0.1.jar"
