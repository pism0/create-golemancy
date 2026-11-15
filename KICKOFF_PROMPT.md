# Claude Code kickoff prompt: Create: Golemancy v0.1

> **Before you paste this:** put the design document in the repo at `docs/SDD.md` (the file that came with this prompt). Start Claude Code from an empty project folder.

---

You're the lead developer on **Create: Golemancy**, a Minecraft 1.21.1 NeoForge addon. It lets players manufacture golems on Create production lines and animate them with Iron's Spells 'n Spellbooks magic.

The full design is in `docs/SDD.md` (v1.1). It's the source of truth: read all of it before writing any code, and re-read the relevant section before each milestone. If the SDD and this prompt ever disagree, follow the SDD and record the conflict in `docs/DECISIONS.md`.

## Goal

Deliver the **v0.1 release candidate**: Milestones 0 to 4 in SDD section 17, with the scope in SDD section 18. It must be ready for me to playtest.

"Ready to test" means all of the following are true:

1. `./gradlew build` passes with no errors.
2. `./gradlew runGameTestServer` passes every GameTest.
3. `./gradlew runClient` launches into a world with Create, Iron's Spells, GeckoLib, JEI and Golemancy loaded, and the log has no Golemancy errors.
4. Each criterion in SDD section 20.1 is either covered by a GameTest or listed in `docs/PLAYTEST.md` as a manual step with exact instructions.
5. Every item, block and entity has an original texture and model in the art style described below. No missing-texture checkerboards and no placeholder cubes.
6. `docs/PROGRESS.md`, `docs/VERSIONS.md`, `docs/DECISIONS.md` and `docs/PLAYTEST.md` are up to date.

Work milestone by milestone. Don't start a milestone until the previous milestone's gate passes.

## Ground rules

**Ask me before you:**
- install anything that needs admin or sudo rights, or changes system-wide settings
- accept the Minecraft EULA (`eula.txt`) for any server run. I'll confirm. Until then, use `runClient` and `runGameTestServer` only.
- sign in to any account, or download anything that needs a login or payment
- cut a feature from the v0.1 scope, or add one that isn't in it
- change a public data format (the `GolemSpec` codec, recipe JSON schema, mod ID) after Milestone 2

**Never:**
- compile against Create: Wizardry, or ship any of its code or assets. It's runtime-only for testing; integration is data-only (SDD section 9).
- copy, trace, recolour, crop or edit any texture, model, sound or other asset from Create, Iron's Spells, vanilla Minecraft or any other mod (see Art below)
- hardcode Iron's Spells or Create item ids in Java. Put them in datagen and tags, after checking them against the pinned version.
- mark a milestone done when its gate hasn't passed

**Don't stop between milestones** to ask whether to continue. Stop only when you're blocked on something in the "ask me" list, or when a gate still fails after three honest attempts at a fix. In that case, write up what you tried in `docs/PROGRESS.md` and tell me.

## Phase 0: Environment setup

1. **Check what's installed first:** `java -version`, `git --version`, OS and architecture.
2. **JDK 21:** if it's missing, install Eclipse Temurin 21. Prefer a user-level install (SDKMAN on macOS/Linux, or the Temurin zip unpacked into the user profile on Windows). Ask before using a system package manager that needs admin rights.
3. **Gradle:** use the Gradle wrapper only. Don't install Gradle globally.
4. **IDE:** not needed for you to work. Set the project up so it imports cleanly into IntelliJ IDEA (ModDevGradle generates run configurations). If I want IntelliJ Community installed, I'll say so; then use the official installer or JetBrains Toolbox, and ask first.
5. **Minecraft for testing:** you don't need a launcher or a Minecraft account. NeoForge's ModDevGradle downloads Minecraft and runs a development client and server with an offline dev username. All dependency mods come from Maven as runtime dependencies, so `runClient` *is* the test install.
6. **Real-launcher playtest instance:** at the end of Milestone 4, also prepare `playtest/` with:
   - the built Golemancy jar
   - a `mods.txt` listing every required and optional mod, with exact versions and download pages
   - a short README for importing into Prism Launcher or the CurseForge app

   I'll sign in and launch it myself.
7. **Version control:** `git init`. Commit at least once per milestone with a clear message. Never force-push or rewrite history.

## Dependencies and versions

Look up the current releases for **Minecraft 1.21.1 NeoForge**, then pin exact versions in `gradle.properties` and record each one with its source URL in `docs/VERSIONS.md`:

| Dependency | How it's used | Notes |
| --- | --- | --- |
| NeoForge 21.1.x | Loader | Use ModDevGradle |
| Create 6.x for 1.21.1 | `implementation` | Get the Maven coordinates and repository from Create's addon developer docs |
| Iron's Spells 'n Spellbooks 1.21.1 | `compileOnly` on the `:api` artifact, plus `localRuntime` on the full mod | Setup is at iron.wiki/developers |
| Iron's Lib | `localRuntime` | Version must match Iron's Spells |
| GeckoLib 4.x for 1.21.1 | `implementation` | |
| JEI for 1.21.1 | Compile against the API, `localRuntime` the mod | EMI optional |
| Create: Wizardry 1.21.1 0.5.x | `localRuntime` only, via Curse Maven or Modrinth Maven | Never compileOnly |

- Iron's version strings look like `1.21.1-3.16.1`. Write `neoforge.mods.toml` dependency ranges that accept the real strings, and test that the mod loads.
- Before writing any recipe, list the Iron's Spells and Create: Wizardry items and fluids you plan to use. Check each id against the pinned jars and record it in `docs/VERSIONS.md`.

## Project conventions

- Mod ID `golemancy`. Java package `io.github.<GITHUB_USERNAME>.golemancy` (ask me for the username if it isn't in git config). Before the first commit, search CurseForge and Modrinth to confirm the ID and the name "Create: Golemancy" aren't taken, and note the result in `docs/DECISIONS.md`.
- Follow the package layout in SDD section 3.1. All Create-specific code goes in `integration.create`; all Iron's-specific code goes in `integration.irons_spellbooks`.
- On 1.21.1, item data uses **data components**, with a `Codec` and a `StreamCodec` per type. Don't hand-write NBT.
- All JSON content (recipes, tags, materials, cores, frames, spell profiles, fuels, lang, models, blockstates, loot) comes from datagen. Commit the generated output.
- `GolemSpec` follows SDD section 4.3 exactly, including the `schema` field and `GolemSpecMigrator`.
- All game logic runs server-side. Client packets are requests that the server validates (SDD section 16.2).
- Write a GameTest for every gate that can be automated, plus at least these:
  - Binding Altar turns valid inputs into a Dormant Golem with the right spec, and rejects invalid inputs
  - spec save and load round trip
  - migration from an older schema
  - fallback to safe defaults when an id is unknown
  - absorption cap
  - energy reaching zero leads to Powered down
  - death drops Remains with reduced integrity
  - repair at the altar
  - ownership checks

## Milestone plan

Follow SDD section 17. For each milestone:

1. Re-read the SDD sections involved and write a short plan in `docs/PROGRESS.md`.
2. Implement it, run datagen, build.
3. Run GameTests. Run `runClient` headless if you can; if there's no display, check the client log from a timed launch, or rely on GameTests and say so.
4. Make the art for anything new in that milestone (don't save all the art for the end).
5. Check the gate, then update `PROGRESS.md` with the gate result, what changed and any open issues.
6. Commit.

Notes for each milestone:

- **M0 Prototypes:** the three SDD spikes, built as small throwaway code under `spikes/`. Write up the findings in `docs/DECISIONS.md`. If modular sequenced assembly is fragile, confirm the fixed-frame plus altar design in SDD section 4.2 and move on. That design is already the plan for v0.1, so M0 must not block progress for long.
- **M1 Foundation:** project skeleton, registries, datagen, GameTest harness, version checks. The mod must also load without Create: Wizardry; test both setups.
- **M2 First golem:** Stone Guardian entity with GeckoLib model and animations, `GolemSpec` persistence, a spawn item, combat AI.
- **M3 Manufacturing:** parts, frame and core recipes using stock Create recipe types. Binding Altar as a Create kinetic block, with a `golemancy:binding` recipe type, JEI category and Ponder scenes. If you can't build Ponder schematics headlessly, write them as structure NBT with a small script, and note how they were made.
- **M4 Magic and lifecycle (v0.1):**
  - Clay Brute; Arcane, Fire and Frost cores; construct energy and fuels
  - the curated spell list (pick 3 to 5 spells that work when cast by an entity, and document why)
  - Powered down state, pack-up, Construct Remains, repair
  - ownership rules and the three-tab configuration screen
  - conditional Create: Wizardry recipes and fuels
  - the `playtest/` folder

## Art: textures and models

All art is **original**, made by you, in a style that sits naturally next to Create and Iron's Spells. Golemancy's look is **Create-built bodies with Iron's-style glowing magic**: brass, stone and rivets on the outside; cores and runes that glow in their school's colour.

### Pipeline

- Generate every texture with reproducible Python scripts (Pillow) in `tools/art/`. Each script builds its textures from named palettes and drawn shapes, then writes PNGs into `src/main/resources/assets/golemancy/textures/`. Re-running the scripts must reproduce the textures exactly.
- Keep palettes in one file, `tools/art/palettes.py`, so a material's colours can change in one place. Make material variants (stone, clay, and later iron or brass) by swapping palettes on a shared template, not by redrawing them.
- Write GeckoLib entity models as `.geo.json` and animations as `.animation.json` under `assets/golemancy/geo/` and `assets/golemancy/animations/`. They must open and edit cleanly in Blockbench (GeckoLib plugin), so I can refine them later. Give every bone a clear name, and include the optional component bones described in SDD section 10.4, hidden by default.
- Write block and item models as standard JSON through datagen. The Binding Altar should look like a Create machine: a casing, a visible shaft connection, and a magical top.
- After each batch, render a **contact sheet** to `art/previews/` showing each texture at 1× and 8× (nearest-neighbour), with entity textures shown next to their UV layout. Open each sheet with your image-reading tool and critique it before moving on: readability at 1×, consistency with the other textures, clean outlines, no stray pixels. Revise until it holds up. If `runClient` can take screenshots in your environment, check the art in-game too.

### Style guide

Use these conventions; don't copy any specific texture.

- **Resolution:** 16×16 for blocks and items. Entity textures at the smallest power-of-two size that fits the UV layout (likely 64×64 or 128×128). Use the same texel density on entities as on blocks.
- **Create side (bodies, parts, Binding Altar):**
  - muted, slightly desaturated industrial materials: andesite greys, warm brass yellows, copper oranges, dark wood browns
  - chunky, readable mechanical detail: plates, rivets, seams, bolts
  - light from the top left; ramps of 4 to 6 shades per material; no noisy dithering
  - items have a clear dark outline
- **Iron's Spells side (cores, runes, scrolls, magic effects):**
  - more saturated, with glowing accents and arcane glyph motifs
  - colours follow the school: Fire is orange-red, Ice is pale cyan, and the Arcane Core is a neutral violet
  - glowing parts use a separate GeckoLib glowmask texture (`*_glowmask.png`) so cores and runes are emissive in-game
- **Constructs:**
  - chunky, readable silhouettes
  - Guardian: upright and defensive, with broad shoulders and a shield-like forearm
  - Brute: wide, low and heavy-armed
  - Stone is cool grey with moss-free cut-stone blocks; clay is warm terracotta with hand-shaped, smoothed forms
  - the core shows as a glowing socket in the chest
  - don't make anything that resembles the vanilla Iron Golem or any other existing mod's mob

**Reference rule:** you may open the dependency jars only to check texture resolution, palette range and outline conventions. Never copy, trace, recolour, crop or derive any file from them. Everything you ship has to be something you could show was drawn from scratch by your scripts.

### v0.1 asset list

**Items**
- for stone and clay: torso shell, limb, head and plate
- core housing; Arcane, Fire and Frost cores (with glow)
- frames: Stone Guardian, Stone Brute, Clay Guardian, Clay Brute
- Dormant Golem item (a small statuette look, tinted by material)
- Construct Remains
- Control Wand
- any intermediate (transitional) items used in sequenced assembly

**Blocks:** Binding Altar (model, textures, idle and working states).

**Entities:** Guardian and Brute `.geo.json` models; stone and clay textures for each; a core glowmask; animations for idle, walk, attack, cast, hurt, activate, dormant and death.

**GUI:** the configuration screen background and tab icons, styled to match Create's GUI panels without copying them.

**Other:** an English lang file with every name and tooltip; the mod icon (`logo.png`, 128×128). Use vanilla sound events for v0.1; no custom audio.

## Final report

When Milestone 4's gate passes, stop and reply with:
- a short summary of what was built
- the gate results for M0 to M4
- known issues and anything cut or changed, with the reasons
- the exact commands for me to build and run
- where the playtest instructions and contact sheets are

Then wait for my playtest feedback before starting Milestone 5.
