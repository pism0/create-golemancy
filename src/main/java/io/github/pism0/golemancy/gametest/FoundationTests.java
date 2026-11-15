package io.github.pism0.golemancy.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.ModDataComponents;
import io.github.pism0.golemancy.core.ModRegistries;
import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.data.InstalledSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Milestone 1 gate: data model, registries and dependency wiring. */
@GameTestHolder(Golemancy.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FoundationTests {
    private FoundationTests() {
    }

    static final String PLATFORM = "gametest/platform";

    static GolemSpec sampleSpec() {
        return new GolemSpec(GolemSpec.CURRENT_SCHEMA,
                Golemancy.id("brute"), Golemancy.id("clay"), Golemancy.id("fire"),
                List.of(ResourceLocation.parse("irons_spellbooks:scroll")),
                List.of(new InstalledSpell(ResourceLocation.parse("irons_spellbooks:firebolt"), 2)),
                new BehaviorConfig(BehaviorConfig.Profile.SOLDIER, Optional.of(new BlockPos(3, 64, -7)), 9, false,
                        BehaviorConfig.MoveMode.FOLLOW, false),
                UUID.fromString("0a1b2c3d-0000-4000-8000-123456789abc"), 0.75f);
    }

    @GameTest(template = PLATFORM)
    public static void requiredModsLoaded(GameTestHelper helper) {
        for (String mod : List.of("create", "irons_spellbooks", "geckolib", "ponder")) {
            helper.assertTrue(ModList.get().isLoaded(mod), "required mod not loaded: " + mod);
        }
        // Informational: the suite must pass both with and without Create: Wizardry.
        Golemancy.LOGGER.info("GameTests running with Create: Wizardry {}",
                ModList.get().isLoaded("create_wizardry") ? "PRESENT" : "ABSENT");
        helper.succeed();
    }

    /** SDD 2.2 / 19: the mods.toml ranges must accept the real Iron's version string. */
    @GameTest(template = PLATFORM)
    public static void dependencyRangesAcceptRealVersions(GameTestHelper helper) throws Exception {
        var golemancy = ModList.get().getModContainerById(Golemancy.MOD_ID).orElseThrow().getModInfo();
        for (var dependency : golemancy.getDependencies()) {
            var installed = ModList.get().getModContainerById(dependency.getModId());
            if (installed.isEmpty()) continue; // optional and absent
            var version = installed.get().getModInfo().getVersion();
            helper.assertTrue(dependency.getVersionRange().containsVersion(version),
                    "range " + dependency.getVersionRange() + " rejects " + dependency.getModId() + " " + version);
        }
        // A later Iron's build on the same Minecraft version must also be accepted.
        VersionRange irons = VersionRange.createFromVersionSpec("[1.21.1-3.16.3,)");
        helper.assertTrue(irons.containsVersion(new DefaultArtifactVersion("1.21.1-3.16.3")), "exact Iron's version rejected");
        helper.assertTrue(irons.containsVersion(new DefaultArtifactVersion("1.21.1-3.17.0")), "newer Iron's version rejected");
        helper.assertFalse(irons.containsVersion(new DefaultArtifactVersion("1.21.1-3.15.6")), "older Iron's version accepted");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void datapackRegistriesAreLoaded(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        for (String material : List.of("stone", "clay")) {
            helper.assertTrue(GolemDefs.material(access, Golemancy.id(material)).isPresent(), "missing material " + material);
        }
        for (String frame : List.of("guardian", "brute")) {
            helper.assertTrue(GolemDefs.frame(access, Golemancy.id(frame)).isPresent(), "missing frame " + frame);
        }
        for (String core : List.of("arcane", "fire", "frost")) {
            helper.assertTrue(GolemDefs.core(access, Golemancy.id(core)).isPresent(), "missing core " + core);
        }
        int profiles = access.registryOrThrow(ModRegistries.SPELL_PROFILE).size();
        helper.assertTrue(profiles >= 3 && profiles <= 5, "v0.1 ships 3 to 5 spell profiles, found " + profiles);
        helper.assertTrue(access.registryOrThrow(ModRegistries.FUEL).size() >= 1, "no fuels loaded");
        helper.succeed();
    }

    /** Spec save and load round trip: JSON codec, item component NBT and the network codec. */
    @GameTest(template = PLATFORM)
    public static void specRoundTrips(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        GolemSpec spec = sampleSpec();

        JsonElement json = GolemSpec.CODEC.encodeStart(JsonOps.INSTANCE, spec).getOrThrow();
        GolemSpec fromJson = GolemSpec.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        helper.assertTrue(spec.equals(fromJson), "JSON round trip changed the spec: " + fromJson);

        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(ModDataComponents.GOLEM_SPEC, spec);
        Tag saved = stack.save(access);
        ItemStack loaded = ItemStack.parse(access, saved).orElseThrow();
        helper.assertTrue(spec.equals(loaded.get(ModDataComponents.GOLEM_SPEC)), "item component round trip changed the spec");

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), access);
        GolemSpec.STREAM_CODEC.encode(buf, spec);
        GolemSpec fromNetwork = GolemSpec.STREAM_CODEC.decode(buf);
        helper.assertTrue(spec.equals(fromNetwork), "network round trip changed the spec");
        helper.succeed();
    }

    /** Migration from an older schema (the schema-0 prototype format). */
    @GameTest(template = PLATFORM)
    public static void specMigratesFromOlderSchema(GameTestHelper helper) {
        String v0 = """
                {
                  "frame": "golemancy:guardian",
                  "material": "golemancy:stone",
                  "core": "golemancy:fire",
                  "spells": ["irons_spellbooks:firebolt"],
                  "behaviour": {"profile": "soldier", "radius": 7},
                  "integrity": 0.5
                }""";
        GolemSpec spec = GolemSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(v0)).getOrThrow();
        helper.assertTrue(spec.schema() == GolemSpec.CURRENT_SCHEMA, "schema was not upgraded: " + spec.schema());
        helper.assertTrue(spec.coreIntegrity() == 0.5f, "integrity was not carried over: " + spec.coreIntegrity());
        helper.assertTrue(spec.spells().equals(List.of(new InstalledSpell(ResourceLocation.parse("irons_spellbooks:firebolt"), 1))),
                "spells were not upgraded: " + spec.spells());
        helper.assertTrue(spec.behavior().profile() == BehaviorConfig.Profile.SOLDIER && spec.behavior().radius() == 7,
                "behaviour was not carried over: " + spec.behavior());

        // Data from a newer Golemancy must be refused rather than misread.
        String future = "{\"schema\": 99, \"frame\": \"golemancy:guardian\", \"material\": \"golemancy:stone\", \"core\": \"golemancy:fire\"}";
        helper.assertTrue(GolemSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(future)).isError(),
                "a spec from a newer schema was accepted");
        helper.succeed();
    }

    /** Fallback to safe defaults when an id is unknown (for example a datapack removed it). */
    @GameTest(template = PLATFORM)
    public static void unknownIdsFallBackToDefaults(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        GolemSpec broken = new GolemSpec(GolemSpec.CURRENT_SCHEMA,
                ResourceLocation.parse("removedpack:spider"), ResourceLocation.parse("removedpack:cheese"),
                ResourceLocation.parse("removedpack:void"), List.of(),
                List.of(new InstalledSpell(ResourceLocation.parse("removedpack:doom"), 1),
                        new InstalledSpell(ResourceLocation.parse("irons_spellbooks:firebolt"), 1)),
                BehaviorConfig.DEFAULT, UUID.randomUUID(), 1f);

        GolemSpec fixed = GolemDefs.sanitize(broken, access);
        helper.assertTrue(fixed.frame().equals(GolemSpec.DEFAULT_FRAME), "frame fallback: " + fixed.frame());
        helper.assertTrue(fixed.material().equals(GolemSpec.DEFAULT_MATERIAL), "material fallback: " + fixed.material());
        helper.assertTrue(fixed.core().equals(GolemSpec.DEFAULT_CORE), "core fallback: " + fixed.core());
        helper.assertTrue(fixed.spells().size() == 1 && fixed.spells().getFirst().spell().getPath().equals("firebolt"),
                "unknown spell was not dropped: " + fixed.spells());
        helper.assertTrue(fixed.owner().equals(broken.owner()), "owner must survive sanitising");

        // A valid spec is returned untouched.
        GolemSpec valid = GolemSpec.create(Golemancy.id("guardian"), Golemancy.id("stone"), Golemancy.id("arcane"));
        helper.assertTrue(GolemDefs.sanitize(valid, access) == valid, "a valid spec was modified");
        // A shattered core is a legal state, not an unknown id.
        helper.assertTrue(GolemDefs.sanitize(valid.shattered(), access).core().equals(GolemSpec.NO_CORE), "NO_CORE was replaced");
        helper.succeed();
    }
}
