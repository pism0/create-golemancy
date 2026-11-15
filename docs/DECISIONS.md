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

## D-010 Spec ids vs item ids

SDD 4.3's example comments show `frame = golemancy:frame/stone_guardian` and
`core = golemancy:core/fire`. Those are the *item* ids. In the spec, `frame`, `material` and `core`
hold ids of the datapack registries (`golemancy:guardian`, `golemancy:stone`, `golemancy:fire`),
because the material is its own field and SDD 5.3 defines two frames (Guardian, Brute), not four.
The record shape, field names and order follow SDD 4.3 exactly. The item ↔ definition mapping is
data: a core definition names its item, and a `golemancy:binding` recipe names the frame and
material its frame item produces.

A shattered core (integrity 0, SDD 11.3) is stored as `core = golemancy:none`; "no owner" is the
nil UUID. Energy and health of a packed-up golem live in a second component,
`golemancy:construct_state`, not in the spec, because the spec is the design and SDD 4.3 fixes its
fields.

## D-011 Curated spell list (v0.1)

Four spells, all `CastType.INSTANT` in Iron's 3.16.3 and all aimed purely by where the caster is
looking, which is what makes them safe for a non-Iron's entity (D-007):

| Spell | School | Why |
| --- | --- | --- |
| `irons_spellbooks:firebolt` | Fire | Projectile along the look vector; the SDD's "one fire bolt". |
| `irons_spellbooks:icicle` | Ice | Same shape for the Frost Core; the SDD's "one ice projectile". |
| `irons_spellbooks:ray_of_frost` | Ice | Instant raycast; gives the Frost Core a second, heavier option. |
| `irons_spellbooks:shield` | Evocation, marked school-neutral | Defensive ward placed in front of the caster; the SDD's "one shield or ward". |

Rejected for v0.1: continuous spells (fire breath, cone of cold: need per-tick cast state),
teleports and dashes (need Iron's mob-only helpers), summons (need `SummonManager`), anything with a
recast or a player-only precondition. The Arcane Core has no spell slot (SDD 6.2), so a
school-neutral spell still needs a Fire or Frost core.

## D-012 No OpenGL on the build machine

The build VM has no GPU or OpenGL driver, so the client cannot be launched there (details in
PROGRESS.md, M1). A software renderer (Mesa llvmpipe for Windows) would make in-game checks and
screenshots possible, but it is a third-party binary download, so it was not installed without the
owner's approval. `./gradlew runClientSmoke -Psmoke=<script>` is ready for a machine with a GPU.

## D-013 The Binding Altar has no container screen

SDD 14 mentions "a Binding Altar screen" in its summary line, while 14.2 describes the feedback as
stage, missing inputs, rotation requirement, progress and output status, mirrored in the goggles
overlay. The altar is built like every other Create machine: items go in and out by hand, funnel,
chute, deployer or mechanical arm, and all of the 14.2 information is shown in Create's hover
overlay (without goggles) plus kinetic stats and catalyst level with goggles. A slot-based container
GUI would be the only one in a Create line and adds nothing the overlay lacks. **This is a judgement
call on an ambiguous line, flagged for the owner:** if a real GUI is wanted it is an additive change.

## D-014 Recipe viewers: JEI now, EMI through its JEI compatibility

The kickoff prompt lists JEI as the dependency and EMI as optional. v0.1 ships a JEI plugin
(Binding Altar category with bind and rebuild pages). There is no dedicated EMI plugin; a native
one is a small follow-up. **Flagged for the owner** because SDD 18 lists "JEI and EMI categories".

## D-015 Manufacturing choices where the SDD left room

- **Parts:** stone plates are sawn from smooth stone, clay plates pressed from clay blocks; limbs are
  compacted (2 plates + andesite alloy); heads and torso shells are mechanical crafting.
- **Core housing:** from a copper sheet (SDD 6.3 allows brass or copper; copper keeps tier 1
  reachable before brass).
- **Spout step:** the base path may not depend on Create: Wizardry and Iron's ships no fluid, so the
  housing is filled with water (Arcane, Frost) or lava (Fire), giving two intermediates (Quenched /
  Molten Core Housing). The school is decided in the basin: Arcane Essence + amethyst (heated),
  + blaze powder (heated), or + packed ice (cold).
- **Two frames from one torso shell:** the Brute sequence starts with an extra plate. Create picks a
  sequenced-assembly recipe by the deployer's held item, so both recipes can share the same start
  item (covered by `sequencedAssemblyBuildsFrames`).
- **Rebuilding Remains:** 4 repair items of the material + 1 fuel item. A core placed alongside
  replaces the old one and is the "dedicated repair" that restores integrity to 100% (SDD 11.3);
  a shattered golem needs one. A different core type drops the installed spells.
- **Create: Wizardry (data only):** mana fills a housing straight into an Arcane Core; a mana
  bucket is a high-energy fuel; mana in the altar's tank is a catalyst that doubles binding speed
  (tag `golemancy:binding_catalysts`, optional entry). All conditional or optional.

## D-016 Iron's spell configuration is loaded lazily

Iron's builds its per-spell configuration on `OnDatapackSyncEvent`, i.e. the first time a player
joins (or on `/reload`). Before that every spell reports the global default school (evocation).
An automated Binding Altar can run before anyone logs in, so `IronsSpellCaster.schoolOf` falls back
to the spell's built-in school when the configured value is just the global default. Found by the
GameTests (which run with no player). Side effect: a modpack that deliberately moves an allow-listed
spell *to* evocation is read as its built-in school.
