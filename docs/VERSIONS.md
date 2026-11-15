# Versions

Pinned in `gradle.properties`. Checked 2026-11-15 against the sources listed.

## Toolchain

| Tool | Version | Source |
| --- | --- | --- |
| JDK | 21 (Oracle 21.0.11 already installed; any JDK 21 works) | `java -version` |
| Gradle (wrapper only) | 9.2.1 | https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle |
| ModDevGradle | 2.0.148 | https://maven.neoforged.net/releases/net/neoforged/moddev/net.neoforged.moddev.gradle.plugin/ |
| Parchment | 2024.11.17 (1.21.1) | https://parchmentmc.org/docs/getting-started |
| Python (art pipeline only) | 3.12.10, user-level; Pillow 11.1.0 in `.venv` | https://www.python.org/ |

## Dependencies

| Dependency | Version | Scope | Source |
| --- | --- | --- | --- |
| Minecraft | 1.21.1 | — | — |
| NeoForge | 21.1.266 | loader | https://maven.neoforged.net/releases/net/neoforged/neoforge/ |
| Create | 6.0.10 (Maven build `6.0.10-280`, `:slim`) | `implementation` | https://wiki.createmod.net/developers/depend-on-create/neoforge-1.21.1 · https://maven.createmod.net |
| Ponder | 1.0.82+mc1.21.1 | `implementation` | same wiki page |
| Flywheel | 1.0.6 | `compileOnly` api / `runtimeOnly` | same wiki page |
| Registrate | MC1.21-1.3.0+67 | `implementation` | https://maven.ithundxr.dev/snapshots |
| Iron's Spells 'n Spellbooks | 1.21.1-3.16.3 | `compileOnly` `:api`, `localRuntime` full | https://iron.wiki/developers/ · https://code.redspace.io/releases |
| Iron's Lib | 1.21.1-2.3.0 | `localRuntime` | https://code.redspace.io/releases (Iron's requires `[1.21.1-2,1.21.1-3)`) |
| GeckoLib | 4.9.3 | `implementation` | https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/ |
| JEI | 19.70.0.469 | `compileOnly` api, `localRuntime` full | https://maven.blamejared.com/mezz/jei/ |
| Curios API | 9.5.1+1.21.1 | `localRuntime` (required by Iron's) | https://modrinth.com/mod/curios |
| Player Animator | 2.0.4+1.21.1-forge | `localRuntime` (required by Iron's) | https://modrinth.com/mod/playeranimator |
| Create: Wizardry | 1.21.1-0.5.0 | `localRuntime` only, toggle `-Pwith_wizardry=false` | https://modrinth.com/mod/create-wizardry |

Declared requirements read from the pinned jars (`python -I tools/inspect_jars.py deps`):

- Iron's Spells 3.16.3: NeoForge ≥ 21.0.0-beta, GeckoLib ≥ 4.7.5.1, Player Animator ≥ 2.0.1,
  Curios ≥ 9.0.5, Iron's Lib `[1.21.1-2,1.21.1-3)`.
- Iron's Lib 2.3.0: NeoForge ≥ 21.1.221, GeckoLib 4.7.5.1+.
- Create: Wizardry 0.5.0: Create 6.0.6–6.0.10, Iron's Spells `[1.21.1-3.15.0, 1.21.1-3.17.0)`,
  Iron's Lib, GeckoLib ≥ 4.7.0, Player Animator, Curios ≥ 9.4.2.

## Verified ids

Every third-party id used by datagen is listed here and was checked against the pinned jars with
`python -I tools/inspect_jars.py check <id>...`. Datagen additionally fails if an Iron's or Create
item id is not present in the live registry (see `ModIds`/`VerifiedIds` in datagen).

The table is filled in as recipes are added (Milestones 3 and 4).

| Id | Kind | Used for | Verified |
| --- | --- | --- | --- |
| *(see below — updated per milestone)* | | | |
