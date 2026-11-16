# Progress

Target: v0.1 release candidate = Milestones 0–4 (SDD 17, 18). Gates are derived as described in
`docs/DECISIONS.md` D-003.

| Milestone | Gate | Status |
| --- | --- | --- |
| Phase 0 Environment | JDK 21, git, wrapper, pinned deps resolve | **Passed** |
| M0 Prototypes | Three spikes answered and written up; v0.1 assembly design confirmed | **Passed** |
| M1 Foundation | Builds; datagen runs; GameTest harness runs; loads with and without Create: Wizardry | **Passed** (client launch not verifiable here, see below) |
| M2 First golem | Stone Guardian spawns from an item with a GeckoLib model, persists its spec, fights | **Passed** (rendering not verifiable here) |
| M3 Manufacturing | Parts → frame → core → Binding Altar → Dormant Golem, automatable; JEI + Ponder | **Passed** (JEI/Ponder/rendering not verifiable here) |
| M4 Magic and lifecycle | SDD 20.1 loop complete; all GameTests pass; `playtest/` prepared | **Passed** (client never launched: see "Not verified") |

## Phase 0 — Environment (2026-11-15)

- Windows 11 x64. Java 21.0.11 and git 2.52 were already installed. `JAVA_HOME` is unset but `java`
  is on PATH, which is enough for the Gradle wrapper (toolchain = 21).
- Python was missing. The python.org MSI installer failed without admin rights (Windows Installer
  error 2203 reading its package cache), so the official python.org NuGet distribution
  (signed by the PSF, same 3.12.10 build) was unpacked into
  `%LOCALAPPDATA%\Programs\Python\Python312`. No admin rights, nothing added to PATH. The art
  scripts run from the project venv `.venv` (Pillow 11.1.0).
- Gradle wrapper 9.2.1 and the build layout come from the official NeoForge ModDevGradle MDK.
- `git init` on `main`; repo-local identity (D-002).
- All pinned dependencies resolve (`./gradlew printRuntimeMods`).

## M0 — Prototypes (2026-11-15)

Plan: answer the three SDD spikes (4.4 sequenced-assembly data, 10.4 optional bones, 7.3 entity
spellcasting) with throwaway code under `spikes/`, and do not let M0 block v0.1.

Result: all three answered from the pinned dependency sources; see `spikes/README.md` and
D-005/D-006/D-007.

- Sequenced assembly cannot carry data components → **fixed frames + altar merge confirmed**.
- Optional bones via `GeoBone#setHidden` in `preRender` → fine.
- Instant spells can be cast by any `LivingEntity` through API classes; Iron's mob-casting classes
  cannot be reused → own `ISpellCaster` adapter.

**Honest caveat:** the spikes were settled by reading source, not by running throwaway mods (there
was no harness yet). Each conclusion is therefore also pinned by a GameTest or a playtest step in
the real mod (listed in `spikes/README.md`); if one of those fails, M0's conclusion is reopened.

Gate: **passed** (findings written, design confirmed).

## M1 — Foundation (2026-11-15)

Plan (SDD 2, 3.1, 4.3, 15): project skeleton and package layout; registries (items, data
components, datapack registries for material/frame/core/spell profile/fuel); `GolemSpec` with codec,
stream codec, schema and migrator; server config; datagen; GameTest harness; art pipeline; check the
mod loads with and without Create: Wizardry.

What changed:

- `Golemancy` main class, `core.ModRegistries` and the `Mod*` registries; package layout per SDD 3.1.
- `data.GolemSpec` exactly as SDD 4.3 (`schema`, `frame`, `material`, `core`, `inscriptions`,
  `spells`, `behavior`, `owner`, `coreIntegrity`), `GolemSpecMigrator` (schema 0 → 1),
  `GolemDefs.sanitize` (unknown ids → safe defaults + warning), `ConstructState` (energy/health of a
  packed-up golem).
- Data components `golemancy:golem_spec` and `golemancy:construct_state` (Codec + StreamCodec each).
- Datapack registries at `data/<ns>/golemancy/{material,frame,core,spell_profile,fuel}/`, synced.
- Datagen: definitions, damage-type tags, item models, lang. `ExternalIds` is the only place
  third-party ids are written, and it fails datagen when an id is not in the live registry (this
  already caught a wrong Create: Wizardry id, `mana_bottle`).
- Art: `tools/art/` (palettes, sprite engine, items, contact sheets); 29 item textures, reproducible
  (`build_all.py --check`). Contact sheets in `art/previews/items_*.png`, reviewed and revised once
  (wider heads on frame sprites, darker core orbs so the school glyph reads).
- `tools/make_structures.py` writes GameTest structure templates as NBT (no display here).
- `client.dev.DevSmoke` + `./gradlew runClientSmoke`: scripted client run (creates a flat world,
  runs commands, takes screenshots, exits).

Gate checks:

| Check | Result |
| --- | --- |
| `./gradlew build` | pass |
| `./gradlew runData` | pass (50 files) |
| `./gradlew runGameTestServer` (Create: Wizardry present) | pass, 6/6 |
| `./gradlew runGameTestServer -Pwith_wizardry=false` | pass, 6/6 |
| mods.toml ranges accept real versions (`dependencyRangesAcceptRealVersions`) | pass |
| `./gradlew runClient` / `runClientSmoke` | **cannot run on this machine** |

**Client launch limitation.** The build machine is a QEMU VM with only the Microsoft Basic/Remote
display adapters: GLFW reports "WGL: The driver does not appear to support OpenGL", so Minecraft's
client cannot start at all here. Following the kickoff prompt's fallback, client-side behaviour is
covered by GameTests where possible and by static validation (datagen's existing-file checks, JSON
validation of models/animations), and everything that needs eyes is listed in `docs/PLAYTEST.md`.
Mod loading itself (all dependency mods + Golemancy, both Wizardry setups) is verified by the
GameTest server, which uses the same mod list.

Gate: **passed**, with the client launch explicitly unverified.

## M2 — First golem (2026-11-15)

Plan (SDD 4.3, 10, 11.1, 12.5, 15.3): one construct entity class registered per frame; attributes
derived from the spec; GeckoLib model, textures, glowmask and all eight animations; spec persistence
with the SDD 15.3 load sequence; a spawn item (Dormant Golem); Guardian/Soldier combat AI on the
vanilla goal selector through `IGolemBehavior` / `IGolemJob`.

What changed:

- `entity.ConstructEntity` (entity types `golemancy:guardian`, `golemancy:brute`), synced render
  data only (material, core, energy %, state, owner); `ConstructAttributes` applies
  frame × material × config as transient modifiers with fixed ids.
- Persistence: decode → migrate → sanitise → recalculate; unreadable data becomes Construct Remains
  with whatever ids could be salvaged.
- `ai`: `JobRunnerGoal`, `IGolemBehavior` (Guardian, Soldier), `IGolemJob` (idle, melee, return
  home, follow owner, cast spell), `Targeting`, staggered decision ticks.
- `lifecycle`: `DormantGolemItem`, `ConstructRemainsItem`, `ControlWandItem`, `GolemLifecycle`
  (deploy, pack up, remains, interaction), `Ownership`.
- `magic`: `IConstructEnergy`, `ISpellCaster`, `IAffinitySource` and their registration point.
- Client: `ConstructRenderer` + `ConstructModel`, `CoreGlowLayer` (emissive `*_core_glowmask.png`
  tinted by core colour, dims with energy), optional bones hidden, layered tinted item sprites.
- Art: `tools/art/entities.py` generates geo, animations, textures and glowmasks from one cube list
  (UVs cannot drift). Guardian and Brute at 128x128, block texel density. Contact sheets:
  `art/previews/entities.png` (texture + UV layout + views) and `entities_views.png`.
  Critique: silhouettes read (Guardian upright with shield forearm, Brute wide and low), brass
  straps and the chest socket separate them from any vanilla golem; kept as is.

Gate checks:

| Check | Result |
| --- | --- |
| `./gradlew build`, `runData` | pass |
| `runGameTestServer` | pass, 15/15 |
| Spawn item places the right entity with its spec (`dormantItemDeploysGolemWithSpec`) | pass |
| Spec/energy/health persist (`golemPersistsThroughSaveAndLoad`) | pass |
| Combat AI (`guardianAttacksHostile`, `combatToggleIsRespected`) | pass |
| Ownership (`golemNeverTargetsOwner`) | pass |
| Models and animations parse with GeckoLib; all bones/animations present (`geckoLibAssetsParse`) | pass |
| In-game rendering and animation | not verifiable here → `docs/PLAYTEST.md` |

Open issues: animation rotation signs and the head-tracking sign follow the Bedrock/GeckoLib
conventions but have never been seen in motion; listed as a playtest check.

Gate: **passed**.

## M3 — Manufacturing (2026-11-15)

Plan (SDD 4.2, 6.3, 7.4, 8, 9, 14.2, 14.3): stock-Create recipes for parts, frames and cores;
`golemancy:binding` recipe type; Binding Altar as a Create kinetic block with automation; JEI
category; Ponder scenes; conditional Create: Wizardry data.

What changed:

- 18 stock-Create recipes generated with Create's datagen API (cutting, pressing, compacting,
  mechanical crafting, sequenced assembly, filling, mixing), 4 `golemancy:binding` recipes,
  2 crafting recipes, 1 conditional Create: Wizardry recipe. All under the `golemancy` namespace.
- `block.BindingRecipe` + `block.BindingLogic` (no Create code): binding and rebuilding plans,
  slot routing, the four SDD 7.4 spell checks.
- `integration.create`: `BindingAltarBlock` / `BindingAltarBlockEntity` (kinetic, shaft from below,
  item + fluid capabilities, comparator output, hover and goggle tooltips, working state),
  mechanical arm interaction point, configurable stress impact; client renderer + Flywheel visual.
- `integration.irons_spellbooks.IronsSpellCaster` (API classes only) reads scrolls and resolves schools.
- `integration.jei`: Binding Altar category (bind pages per frame x core, rebuild pages).
- Ponder: `binding_altar` and `frame_line` scenes; schematics written as NBT by
  `tools/make_ponder.py` (no display to save them in-game).
- Art: altar block textures (`art/previews/blocks.png`), reviewed; rune ring made more saturated
  for the working state.

Gate checks:

| Check | Result |
| --- | --- |
| `./gradlew build`, `runData` | pass |
| `runGameTestServer` with Create: Wizardry | pass, 24/24 |
| `runGameTestServer -Pwith_wizardry=false` | pass, 24/24 |
| All recipes load; compat recipe present only with Wizardry (`manufacturingRecipesLoad`) | pass |
| Frames and core housing assemble through Create's own lookup (`sequencedAssemblyBuildsFrames`) | pass |
| M0 spike 1 regression (`sequencedAssemblyDiscardsExtraComponents`) | pass |
| Altar turns valid inputs into the right spec, rejects invalid ones (`bindingBuildsTheRightSpec`, `bindingRejectsInvalidInputs`) | pass |
| Altar under real shaft power, minimum speed, hopper-fed, arm point (`altarBindsUnderRotation`, `altarNeedsMinimumSpeed`, `altarIsFedByAutomation`, `mechanicalArmCanTargetAltar`) | pass |
| JEI category, Ponder scenes, altar rendering | compile only → `docs/PLAYTEST.md` |

Found and fixed: Iron's lazy spell config made every scroll look like the wrong school on a server
with no players (D-016).

Open issues: JEI, Ponder and the altar renderer have never been run (no OpenGL here). They follow
Create's and JEI's own patterns and are the most likely place for a first-launch problem.

Gate: **passed**.

## M4 — Magic and lifecycle, v0.1 (2026-11-15)

Plan (SDD 6.4, 7, 9, 10.3, 11, 12.2, 14.1, 16.2): Clay Brute and the three cores were already data
from M1-M3; this milestone adds the spell-casting job end to end, the energy/powered-down loop,
pack-up, Remains and rebuilding, ownership enforcement, the request packets and the three-tab
screen, GUI art and logo, the Create: Wizardry conditionals, and the playtest deliverables.

What changed:

- `lifecycle.GolemRequests`: every screen action validated on the server (ownership, distance,
  value range). `core.network.GolemNetwork`: `OpenScreen` (server to client, full spec on open) and
  `Request` (client to server).
- `client.GolemScreen`: Overview / Core / Behaviour tabs; sends requests only.
- Casting: `ai.GolemJobs.CastSpellJob` + `IronsSpellCaster` (allow-list, range, line of sight,
  cooldown, energy cost, refund on failure). Friendly fire cancelled in `CommonEvents`.
- Energy, powered down, wake-up, field repair, pack-up, Remains, altar rebuild: implemented in M2/M3
  code and now covered by tests.
- Art: `tools/art/gui.py` (screen atlas, tab icons, bars, `logo.png`); sheet `art/previews/gui.png`.
- `tools/check_lang.py`; `tools/smoke/m4_tour.txt`; `playtest/` (jar, `mods.txt`, README);
  `docs/PLAYTEST.md`; `README.md` with the version matrix.

Final gate ("ready to test" list from the kickoff prompt):

| # | Requirement | Result |
| --- | --- | --- |
| 1 | `./gradlew build` passes | **pass** |
| 2 | `./gradlew runGameTestServer` passes every GameTest | **pass, 36/36** (also 36/36 with `-Pwith_wizardry=false`) |
| 3 | `./gradlew runClient` launches into a world, no Golemancy errors | **not verified**: the build VM has no OpenGL, the client cannot start there |
| 4 | Each SDD 20.1 criterion covered by a GameTest or a PLAYTEST step | **pass** (table at the top of `docs/PLAYTEST.md`) |
| 5 | Original texture and model for every item, block and entity | **pass** by asset tests (`everyItemHasModelAndTextures`, `geckoLibAssetsParse`) and contact sheets; in-game look not verified |
| 6 | PROGRESS, VERSIONS, DECISIONS, PLAYTEST up to date | **pass** |

GameTests required by the kickoff prompt:

| Required test | GameTest |
| --- | --- |
| Altar turns valid inputs into the right spec; rejects invalid | `bindingBuildsTheRightSpec`, `bindingRejectsInvalidInputs`, `altarBindsUnderRotation` |
| Spec save and load round trip | `specRoundTrips`, `golemPersistsThroughSaveAndLoad` |
| Migration from an older schema | `specMigratesFromOlderSchema` |
| Fallback to safe defaults for unknown ids | `unknownIdsFallBackToDefaults` |
| Absorption cap | `absorptionIsCapped` |
| Energy reaching zero leads to Powered down | `energyZeroPowersDownAndRechargeWakes` |
| Death drops Remains with reduced integrity | `deathDropsRemainsWithReducedIntegrity` |
| Repair at the altar | `remainsAreRebuiltAtAltar` |
| Ownership checks | `ownershipIsEnforced`, `golemNeverTargetsOwner`, `friendlyFireIsCancelled`, `activeGolemCapIsEnforced` |

### Not verified (needs the owner's machine)

Nothing client-side has ever been executed. In rough order of risk:

1. Client start-up with Golemancy's client registrations (renderers, item colours, Flywheel
   visualiser, Ponder plugin, JEI plugin).
2. GeckoLib rendering: animation directions, head tracking, the emissive core layer, hidden
   optional bones.
3. The Binding Altar renderer (half shaft, items on top) and block model UVs.
4. The configuration screen layout and its tab hit boxes.
5. Ponder scenes (schematics were generated by script, never loaded).
6. JEI category layout.

`docs/PLAYTEST.md` sections A and B walk through exactly these.

### Known issues and limits

- The altar resets binding progress whenever any input changes (by design,
  but a funnel topping up supplies mid-bind restarts the timer).
- A rebuild always returns a golem at full energy and health.
- "Regenerates in water" for clay is 1 health per second in water or rain; not separately tested.
- Golems perform no world actions in v0.1, so the SDD 11.4 owner fake player is not needed yet.
- The abandoned-golem timeout (default off) counts wall-clock time since the server last saw the
  owner, and restarts its clock after a server restart.
- Before the first player joins after a server start, Iron's reports every spell's school as
  evocation (D-016): spell damage then uses the evocation damage type, so the Fire/Frost core
  resistance tags do not apply to it until someone has logged in.
- GameTests and the dev smoke hook ship inside the jar (inactive in normal play).

Gate: **passed** for everything that can be checked on this machine; item 3 is open and is the
first thing the playtest should confirm.
