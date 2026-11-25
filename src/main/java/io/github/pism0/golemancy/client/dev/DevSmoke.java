package io.github.pism0.golemancy.client.dev;

import io.github.pism0.golemancy.Golemancy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Development-only automated client smoke test. Inactive unless the JVM is started with
 * {@code -Dgolemancy.smoke=<script>} (the {@code clientSmoke} Gradle run does this).
 *
 * <p>When active it creates a fresh creative superflat world from the title screen, plays a script
 * (one step per line: {@code <tick> cmd <command>}, {@code <tick> shot <name>},
 * {@code <tick> quit}), saves screenshots to {@code screenshots/} and closes the game. This lets
 * a headless build check that the client reaches a world with every dependency loaded, and lets
 * art be reviewed in-game, without anyone at the keyboard.
 */
@EventBusSubscriber(modid = Golemancy.MOD_ID, value = Dist.CLIENT)
public final class DevSmoke {
    private DevSmoke() {
    }

    private static final String SCRIPT = System.getProperty("golemancy.smoke", "");
    private static final String WORLD = "golemancy_smoke";

    private record Step(int tick, String action, String argument) {
    }

    private static List<Step> steps;
    private static boolean worldRequested;
    private static boolean announced;
    private static int ticksInWorld;
    private static int nextStep;

    private static boolean enabled() {
        return !SCRIPT.isEmpty();
    }

    /**
     * Starts the scripted world once the game is sitting on its first menu. Polled from the client
     * tick rather than hooked on ScreenEvent.Opening: the initial screens are set while resources
     * are still loading, and relying on that event proved unreliable.
     */
    private static void maybeStartWorld(Minecraft mc) {
        if (worldRequested) return;
        boolean firstScreen = mc.screen instanceof TitleScreen
                || mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
        if (!firstScreen || mc.getOverlay() != null) return;
        worldRequested = true;
        mc.options.pauseOnLostFocus = false;
        mc.options.onboardAccessibility = false;
        Golemancy.LOGGER.info("[smoke] first screen: {}", mc.screen.getClass().getSimpleName());
        try {
            steps = loadScript(Path.of(SCRIPT));
            deleteOldWorld(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD));
            Golemancy.LOGGER.info("[smoke] creating world '{}' and running {} steps from {}", WORLD, steps.size(), SCRIPT);
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    rules, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261115L, false, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET)
                            .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
        } catch (Exception e) {
            Golemancy.LOGGER.error("[smoke] FAILED to start", e);
            mc.stop();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!announced) {
            announced = true;
            Golemancy.LOGGER.info("[smoke] active, script {}", SCRIPT);
        }
        maybeStartWorld(mc);
        if (steps == null) return;
        if (mc.level == null || mc.player == null || mc.player.connection == null) return;
        if (ticksInWorld == 0) {
            Golemancy.LOGGER.info("[smoke] in world");
        }
        ticksInWorld++;
        while (nextStep < steps.size() && steps.get(nextStep).tick <= ticksInWorld) {
            Step step = steps.get(nextStep++);
            switch (step.action) {
                case "cmd" -> {
                    Golemancy.LOGGER.info("[smoke] /{}", step.argument);
                    mc.player.connection.sendCommand(step.argument);
                }
                case "shot" -> {
                    mc.options.hideGui = step.argument.startsWith("nogui_");
                    Screenshot.grab(mc.gameDirectory, step.argument + ".png", mc.getMainRenderTarget(),
                            message -> Golemancy.LOGGER.info("[smoke] screenshot {}", step.argument));
                }
                case "gui" -> mc.options.hideGui = !Boolean.parseBoolean(step.argument);
                case "quit" -> {
                    Golemancy.LOGGER.info("[smoke] DONE");
                    mc.stop();
                }
                default -> Golemancy.LOGGER.warn("[smoke] unknown action {}", step.action);
            }
        }
    }

    private static List<Step> loadScript(Path path) throws IOException {
        List<Step> out = new ArrayList<>();
        for (String raw : Files.readAllLines(path)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+", 3);
            out.add(new Step(Integer.parseInt(parts[0]), parts[1], parts.length > 2 ? parts[2] : ""));
        }
        out.sort(Comparator.comparingInt(Step::tick));
        return out;
    }

    private static void deleteOldWorld(Path world) throws IOException {
        if (!Files.exists(world)) return;
        try (Stream<Path> walk = Files.walk(world)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
