# Decisions

Decisions, findings and SDD/prompt conflicts, newest last. "SDD" is `docs/SDD.md` v1.1.

## D-001 Mod ID and name availability (checked 2026-11-15)

- **"Create: Golemancy"** — no project with this name on Modrinth (API search, `create-golemancy`
  slug is 404) or CurseForge (web search). Free.
- **`golemancy` mod ID / "Golemancy" name — NOT fully free.** An unrelated mod called *Golemancy*
  (by emirik0l; Fabric, last release 1.2.4 for Minecraft 1.18.1, Feb 2022) holds the `golemancy`
  slug on both Modrinth and CurseForge, and its mod id is almost certainly `golemancy`. There is also
  a Quilt 1.19.2 "Golemancy Unofficial Port", a "Golemancy Origins" datapack and "Trickster
  Golemancy" (Fabric 1.21.1, mod id differs).
- Practical impact: none at runtime (different loader and game version, so the two can never be
  installed together). Impact on publishing: the site slug must be `create-golemancy`, and JEI/command
  namespaces would collide if that mod is ever ported to NeoForge 1.21.1.
- **Decision:** keep `golemancy` for now because the SDD (2.3) fixes it and changing a public id is
  on the "ask first" list. **Open question for the owner before any public build:** keep
  `golemancy`, or switch to `create_golemancy`. Changing later invalidates saved worlds and datapacks.

## D-002 Package, identity and licence defaults

- Java package `io.github.pism0.golemancy` (username supplied by the owner).
- Commits use a repo-local identity `pism0 <pism0@users.noreply.github.com>`; change with
  `git config user.name/user.email` if a different identity is wanted.
- `mod_license=All Rights Reserved` in `gradle.properties` as the conservative default. No licence
  was specified; this is the owner's call and a one-line change.

## D-003 SDD section 17 gates are missing from the exported document

The roadmap in SDD 17 is an embedded figure that did not survive export ("embedded content:
roadmap · 8 milestones with gates"). The gates used for M0–M4 are therefore derived from the
kickoff prompt's milestone notes, SDD 18 (scope) and SDD 20.1 (success criteria), and are written
out in `docs/PROGRESS.md`. If the original figure says something different, it wins.

## D-004 Where the Binding Altar code lives (SDD 3.1 vs kickoff prompt)

SDD 3.1 lists `block` for the Binding Altar and also says Create-touching code lives in
`integration.create`; the kickoff prompt says *all* Create-specific code goes in `integration.create`.
A kinetic block must extend Create classes, so:

- `block` holds the loader- and Create-agnostic binding logic (`BindingLogic`, `BindingRecipe`,
  input validation, spec building, repair).
- `integration.create` holds `BindingAltarBlock`, `BindingAltarBlockEntity`, its renderer/visual,
  goggle tooltip, stress registration and Ponder scenes.

## D-005 Spike 1: data components do not survive sequenced assembly → fixed frames confirmed

`SequencedAssemblyRecipe#advance` (Create 6.0.10-280) builds a fresh copy of the transitional item
on every step and sets only Create's own `SEQUENCED_ASSEMBLY` component; the final result is a copy
of the recipe's fixed output. `advance` is private. Carrying a modular spec would need a mixin into
Create internals or a custom recipe type plus custom deployer handling. **Decision:** v0.1 uses the
SDD 4.2 design (one frame recipe per material × frame; everything combinatorial is merged in code
at the Binding Altar). Per-slot parts move to a custom Assembly Platform in Milestone 5.
Regression test: `sequencedAssemblyDiscardsExtraComponents`. Details: `spikes/01_sequenced_assembly`.

## D-006 Spike 2: optional parts as hidden bones work and are cheap

GeckoLib bakes one model per file and shares it between entities, so bone visibility is applied per
render pass in `GeoEntityRenderer#preRender` with `GeoBone#setHidden`. Cost is a handful of map
lookups per rendered golem. **Decision:** both frame models ship the optional bones
(`opt_shoulder_plates`, `opt_back_banner`, `opt_head_crest`, `opt_weapon`), hidden in v0.1.
Details: `spikes/02_geckolib_optional_bones`.

## D-007 Spike 3 / SDD 7.3: spellcasting adapter is our own, on API classes only

- Iron's mob-casting path (`AbstractSpellCastingMob`, casting goals, `SummonManager`) is in the full
  mod only. `api.entity.IMagicEntity` is in the API jar but mentions a non-API class
  (`SyncedSpellData`) in its signature, so it cannot be implemented against `:api` alone.
- A plain `LivingEntity` can cast through `AbstractSpell#onCast(level, lvl, entity, CastSource.MOB,
  new MagicData(true))`, which is what Iron's own mobs end up calling.
- The summon manager does not give friendly-fire handling to non-summons.

**Decision:** `IronsSpellCaster` (in `integration.irons_spellbooks`) implements `ISpellCaster` with
API classes only and casts **instant** spells from the allow-list. Golemancy handles cooldown,
energy cost, line of sight and friendly fire itself. No Iron's internal class is used, so the
known-internals list (`integration/irons_spellbooks/KNOWN_INTERNALS.md`) is empty.

## D-008 Dependency pins that differ from "latest"

- **Create `6.0.10-280`**, not the newest Maven build (`6.0.11-338`). Create's wiki says to depend
  only on the CI build that corresponds to a published release; 6.0.10 is the latest release on
  Modrinth/CurseForge and `-280` is the build the wiki lists for it. Ponder `1.0.82`, Flywheel
  `1.0.6`, Registrate `MC1.21-1.3.0+67` are the versions the wiki pairs with it.
- **Create: Wizardry `1.21.1-0.5.0`** (latest *release*; `0.5.1-pre1` is a beta). Its
  `neoforge.mods.toml` caps Iron's Spells below 3.17 and names Create up to 6.0.10, which is one
  more reason not to move Create forward.
- Iron's Spells needs Curios and Player Animator at runtime; both are `localRuntime` only.

## D-009 `neoforge.mods.toml` ranges

Iron's Spells reports `1.21.1-3.16.3`. The range used is `[1.21.1-3.16.3,)` (Maven ordering compares
`1.21.1` then the `-3.16.3` qualifier numerically, so later 1.21.1 builds are accepted). Create is
`[6.0.10,6.1.0)`, GeckoLib `[4.9.3,)`. These are exercised by every dev run: the game refuses to
start if a range rejects the real version string. Result recorded in `docs/PROGRESS.md` (M1).
