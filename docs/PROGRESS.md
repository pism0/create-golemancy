# Progress

Target: v0.1 release candidate = Milestones 0–4 (SDD 17, 18). Gates are derived as described in
`docs/DECISIONS.md` D-003.

| Milestone | Gate | Status |
| --- | --- | --- |
| Phase 0 Environment | JDK 21, git, wrapper, pinned deps resolve | **Passed** |
| M0 Prototypes | Three spikes answered and written up; v0.1 assembly design confirmed | **Passed** |
| M1 Foundation | Builds; datagen runs; GameTest harness runs; loads with and without Create: Wizardry | **Passed** (client launch not verifiable here, see below) |
| M2 First golem | Stone Guardian spawns from an item with a GeckoLib model, persists its spec, fights | Not started |
| M3 Manufacturing | Parts → frame → core → Binding Altar → Dormant Golem, automatable; JEI + Ponder | Not started |
| M4 Magic and lifecycle | SDD 20.1 loop complete; all GameTests pass; `playtest/` prepared | Not started |

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
