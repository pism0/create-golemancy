# Create: Golemancy — Software Design Document v1.1

Nov 15, 2026 · @Colin

## 0. Document status

Version 1.1 cuts the first release to one custom block, two golems and three cores, and adds the data and lifecycle design that v1.0 left open.

| Field | Value |
| --- | --- |
| Project | Create: Golemancy |
| Document version | 1.1 (revises 1.0, Initial Architecture Proposal) |
| Status | Revised architecture, pre-prototype |
| Target | Minecraft 1.21.1, NeoForge, Java 21 |

### Changes from v1.0

| Area | v1.0 | v1.1 |
| --- | --- | --- |
| First release | 4 golems, 5 custom machines, modular parts, spells | 2 golems, 3 cores, 1 custom block (Binding Altar), stock Create for everything else |
| Machines | 7 new machines | Most stages mapped to stock Create; only the Binding Altar is new in v0.1 |
| Item data | NBT and entity data | 1.21 data components defined with Codecs, with a schema version |
| Assembly | Not specified | New section 4: Golem Chassis item that builds up its data through sequenced assembly |
| Create: Wizardry | Optional where practical | No compile dependency; conditional data-driven recipes only |
| Iron's Spells | Stable API, isolate internals | API jar only; check mob-casting reuse before building custom casting |
| Elements | Acid, Arcane as an affinity | Affinities map to Iron's real schools; acid removed |
| Naming | Iron Golem, Stone Golem | Construct names that don't clash with vanilla mobs |
| Lifecycle | Not covered | New section 11: item form, death and recovery, repair, ownership |
| Roadmap | M2 depended on M3 | New Milestone 0 spike; M2 uses an item or command, manufacturing moves to M3 |
| Tooling | Not covered | Datagen, GameTests, Ponder and JEI in the milestones that need them |

## 1. Executive summary

Create: Golemancy is a NeoForge addon where players manufacture golems on real Create production lines and animate them with Iron's Spells magic.

### 1.1 Overview

The mod builds on three existing systems:

- **Create:** mechanical processing, transport, sequenced assembly and automation.
- **Iron's Spells 'n Spellbooks:** schools of magic, magical resources, spells and progression.
- **Create: Wizardry (optional):** existing bridges between Create machinery and Iron's Spells, such as magical fluid processing.

Golemancy adds a fourth layer: construct engineering. Golems are manufactured entities with a body material, components, a magical core, inscriptions and a behaviour profile, rather than craftable mobs.

### 1.2 Design philosophy

Physical engineering through Create plus magical engineering through Iron's Spells produces programmable golems.

The mod rewards factory design, exploration for materials, magical progression, experimentation and careful resource management. A late-game player should be able to run a line that produces specialised golems with little manual work.

Two rules follow from this:

1. **Stock Create first.** A manufacturing stage uses existing Create machines unless they genuinely cannot express it.
2. **Reuse, don't rebuild.** Magic comes from Iron's Spells resources and schools, not a parallel magic system.

### 1.3 Goals

1. Golem types inspired by classic tabletop golems, as original Golemancy designs.
2. A modular construction system driven by data.
3. Magical cores tied to Iron's Spells schools, with spellcasting.
4. Manufacturing through stock Create machinery, with few custom blocks.
5. Fully automatable production.
6. Programmable behaviour that respects server and claim permissions.
7. Interaction with the world and with Create logistics.
8. A progression that pushes players toward more complex factories.
9. A complete playable loop in v0.1 before expanding.

## 2. Technical foundation

The project targets one Minecraft version and one loader, with each dependency version pinned exactly in Gradle.

### 2.1 Supported environment

| Component | Target | How it is used |
| --- | --- | --- |
| Minecraft | 1.21.1 | Only supported version for v0.x |
| Loader | NeoForge | Only supported loader for v0.x |
| Java | 21 |  |
| Create | Exact pinned 1.21.1 release | Required; compiled against |
| Iron's Spells 'n Spellbooks | Exact pinned 1.21.1 release | Required; compiled against the API jar only |
| Iron's Lib | Version matching Iron's Spells | Required at runtime by Iron's Spells |
| Create: Wizardry | 1.21.1 0.5.x (pre-1.0) | Optional; data-only, never compiled against |
| GeckoLib | 1.21.1 NeoForge release | Required; entity models and animation |
| JEI or EMI | Current 1.21.1 release | Optional; recipe display |
| Curios | Current 1.21.1 release | Optional; deferred past v0.1 |
| Build | Gradle with NeoForge ModDevGradle |  |

Forge 1.20.1 is out of scope until after v1.0.

### 2.2 Dependency strategy

**Required:** Minecraft, NeoForge, Create, Iron's Spells (with Iron's Lib), GeckoLib.

**Optional:** Create: Wizardry, JEI or EMI, Curios.

- **Version pinning.** Each required dependency is pinned to one exact version per Golemancy release, listed in a version matrix in the README.
- **Version ranges.** Iron's Spells reports versions such as `1.21.1-3.16.1`. Test the `neoforge.mods.toml` ranges against the real version strings so a plain lower bound does not cause false startup failures.
- **Iron's Spells.** Compile against the `:api` artifact. Any class used from the full mod goes behind an adapter in `integration.irons_spellbooks` and is listed in a known-internals file.
- **Create: Wizardry.** It is pre-1.0 and All Rights Reserved. Golemancy never compiles against it or ships its assets. Integration is data-only (section 9).
- **Core rule.** Golemancy must load and offer a complete manufacturing path with only the required dependencies installed.

### 2.3 Naming and identity

- Mod ID `golemancy`. Before the first public build, confirm that the ID and the display name "Create: Golemancy" are free on CurseForge and Modrinth.
- Java package `io.github.<owner>.golemancy`, or a domain the author controls. Not `com.golemancy`.

## 3. Architecture

Golem logic depends on Golemancy's own interfaces. Code that touches Create or Iron's Spells lives in `integration`, behind adapters.

### 3.1 Package structure

```text
io.github.<owner>.golemancy
├── core          registry, config, events, networking, datagen
├── data          GolemSpec, component types, codecs, schema versioning
├── construction  materials, components, chassis item, assembly recipe types
├── magic         cores, affinities, construct energy, inscriptions
├── entity        golem entity, attributes, damage handling, animation
├── lifecycle     dormant item, death and recovery, repair, ownership
├── ai            behaviours, jobs, work areas, scheduler
├── block         Binding Altar (v0.1); later custom machines
└── integration
    ├── create            kinetic blocks, assembly hooks, Ponder
    ├── irons_spellbooks  school mapping, spell casting adapter
    ├── create_wizardry   conditional recipes only
    ├── jei | emi         recipe categories
    └── curios            deferred
```

### 3.2 Adapter interfaces

| Interface | Purpose | Default implementation |
| --- | --- | --- |
| `ISpellCaster` | Cast a spell from a golem at a target | Iron's Spells adapter |
| `IAffinitySource` | Resolve an affinity to a school and its damage types | Iron's Spells school registry |
| `IConstructEnergy` | Store, drain and recharge construct energy | Golemancy internal |
| `IKineticRequirement` | Check speed and stress for a processing stage | Create adapter |

### 3.3 What can and can't be isolated

- **Iron's Spells** has a stable API jar, so isolation is realistic. Internal classes are allowed only through `integration.irons_spellbooks` and are listed by name.
- **Create** has no stable public API for addons. Custom kinetic blocks must extend Create's own block and block-entity classes, and sequenced assembly hooks touch Create internals. Accept this, keep that code in `integration.create`, and budget a porting pass for each Create update.
- **Create: Wizardry** is reached only through data, so there is no code to isolate.

## 4. Data model and assembly pipeline

Create's sequenced assembly builds fixed frames, one recipe per material. All combinatorial choices (core, inscriptions, configuration) are merged in code at the Binding Altar. This keeps the recipe count linear instead of multiplying every material by every core.

### 4.1 The problem

Sequenced assembly turns a fixed input into a fixed output and does not carry arbitrary item data from step to step. A fully modular golem (11 materials × component variants × 9 cores × runes) would need a recipe per combination. That is unmaintainable and makes JEI useless.

### 4.2 Chosen design (v0.1)

1. **Parts.** Stock Create processing turns raw materials into material-specific parts: plates, limbs, torso shell, core housing.
2. **Frame.** A sequenced assembly recipe per material and frame type turns a torso shell into a **Construct Frame** item, such as `golemancy:frame/stone_guardian`. The material is fixed by the recipe.
3. **Core.** Cores are made separately on a Create line: housing, spout-filled magical fluid, a stabilising step.
4. **Binding.** The Binding Altar takes one frame, one core and optional inscription items. Code builds a `GolemSpec` from the inputs and outputs a **Dormant Golem** item that carries it.
5. **Activation.** Using the Dormant Golem item places the entity, which copies the spec into its own saved data.

Only step 4 needs custom code. Steps 1 to 3 are plain Create recipes in datagen and work with any Create automation.

&#91;embedded content: manufacturing pipeline · 3 input lines into one altar\]

Frames and cores are built on separate stock Create lines. They meet only at the Binding Altar, the one point where the combinatorial spec is built in code.

### 4.3 GolemSpec

`GolemSpec` is an immutable record with a `Codec` for saving and a `StreamCodec` for networking. It is registered as the `golemancy:golem_spec` data component on Dormant Golem items and saved in the entity's data.

```java
public record GolemSpec(
    int schema,                       // data version, starts at 1
    ResourceLocation frame,           // golemancy:frame/stone_guardian
    ResourceLocation material,        // golemancy:stone (data-driven)
    ResourceLocation core,            // golemancy:core/fire
    List<ResourceLocation> inscriptions,
    List<InstalledSpell> spells,      // spell id + level, curated list only
    BehaviorConfig behavior,          // profile, home, radius, flags
    UUID owner,
    float coreIntegrity               // 0..1, used by death and repair
) {}
```

Rules:

- Data components replace NBT for items on 1.21.1. Don't hand-write NBT tags.
- Every load runs `GolemSpecMigrator`, which upgrades older `schema` values step by step.
- An unknown material, core or spell id (for example a datapack removed it) falls back to a safe default and logs a warning. It never crashes the server.
- The spec is the single source of truth. Attributes are recalculated from it on load, never saved separately.

### 4.4 Later: true modular assembly

Per-slot components (swapping arms, legs, attachments) need a custom deployer recipe type that merges data components onto the transitional item instead of replacing it. Milestone 0 includes a spike to test this against Create's sequenced assembly. If it is fragile, modular parts move to a custom Assembly Platform block in Milestone 5.

## 5. Golem construction system

A golem is defined by its frame, material and core in v0.1. Per-slot components and equipment come later on the same `GolemSpec`.

### 5.1 Construction inputs by release

| Input | v0.1 | Later |
| --- | --- | --- |
| Body material | Set by the frame recipe | Same |
| Frame type | 2 frames (Guardian, Brute) | More frames, including stationary turret |
| Core | 3 cores | Full core list (section 6) |
| Inscriptions | 1 inscription slot | Multiple rune slots by tier |
| Per-slot components | None | Head, arms, legs, armour, weapon, utility (section 4.4) |
| Equipment | None | Held weapon and armour slots; Curios-style accessories |
| Behaviour | Guardian or Soldier | Full profile list (section 12) |

### 5.2 Materials

Materials are data-driven JSON in `data/<namespace>/golemancy/material/`, so datapacks and modpacks can add or rebalance them. v0.1 ships stone and clay; the rest are planned for later tiers.

| Material | Character | Tier |
| --- | --- | --- |
| Clay | Cheap, heavy, regenerates in water | 1 (v0.1) |
| Stone | High durability and strength | 1 (v0.1) |
| Copper | Cheap utility constructs | 2 |
| Brass | Precision and mechanical utility | 2 |
| Iron | High armour and strength | 2 |
| Gold | High magical conductivity, fragile | 3 |
| Amethyst | Spell power amplification | 3 |
| Obsidian | Magic resistance | 3 |
| Flesh | Regeneration, biological | 4 |
| Netherite | Exceptional durability | 5 |
| Dragonbone | Advanced magical properties | 5 |

Material fields:

- `health_multiplier`, `armor_multiplier`, `speed_multiplier`, `knockback_resistance`
- `conductivity` (scales spell power and energy efficiency)
- `affinity` (optional Iron's school id)
- `absorbs` and `resists` (damage type tags, section 10.3)
- `repair_item` (item or tag used for repair, section 11.3)
- `texture` (model texture for the frame)

### 5.3 Frames

A frame sets the body shape, model, base attributes and which slots exist.

| Frame | Shape | Role | Slots (later) |
| --- | --- | --- | --- |
| Guardian | Bipedal, medium | Defends a home position | Head, arms, legs, armour, 1 rune |
| Brute | Bipedal, large | Melee combat | Head, arms, legs, armour, weapon, 1 rune |
| Turret (later) | Stationary | Ranged spellcasting | Head, core, 2 runes |
| Worker (later) | Small | Utility jobs | Arms, legs, utility attachment |

## 6. Magical core system

Each core maps to one Iron's Spells school, or to none for the Arcane Core. A golem uses a single resource, construct energy, which recharges from configured fuels.

### 6.1 Core purpose

The core animates the golem, stores its energy, sets its affinity and allows spellcasting. A frame without a core can't be bound or activated.

### 6.2 Core types

| Core | Iron's school | Role | Release |
| --- | --- | --- | --- |
| Arcane Core | None (neutral) | Basic animation, no spells | v0.1 |
| Fire Core | Fire | Fire spells, fire resistance | v0.1 |
| Frost Core | Ice | Ice spells, slowing attacks | v0.1 |
| Lightning Core | Lightning | Lightning spells | Tier 3 |
| Holy Core | Holy | Healing and protection | Tier 4 |
| Nature Core | Nature | Environmental and poison | Tier 4 |
| Blood Core | Blood | Life drain and regeneration | Tier 4 |
| Ender Core | Ender | Teleportation | Tier 5 |
| Eldritch Core | Eldritch | Advanced effects | Tier 5 |

Iron's has no Arcane school; Arcane Essence is an item. The Arcane Core is Golemancy's neutral core and only uses Arcane Essence as an ingredient. The school mapping lives in core JSON (`"school": "irons_spellbooks:fire"`), so addon schools can be supported by datapacks.

### 6.3 Core manufacturing (example: Fire Core)

This runs entirely on stock Create machines.

1. Press brass or copper sheets and assemble a Core Housing (sequenced assembly: deploy amethyst shard, press, deploy Arcane Essence).
2. Spout-fill the housing with the school's fluid or essence (Fire: blaze-based fluid or fire essence).
3. Stabilise it in a heated basin mix with Arcane Essence.
4. The output is a finished Fire Core.

Exact Iron's item and fluid ids are checked against the pinned version during Milestone 1 and set in datagen, never hardcoded in Java.

### 6.4 Construct energy

Golemancy uses one internal resource. It does not draw on Iron's player mana, and it does not use Create: Wizardry fluids directly.

| Field | Meaning |
| --- | --- |
| `max_energy` | From core and material conductivity |
| `passive_drain` | Per AI cycle while active (not per tick) |
| `spell_cost_multiplier` | Converts a spell's Iron's mana cost into construct energy |
| `fuels` | Data-driven list: item or fluid, energy per unit |

- Recharging uses a fuel item (hand-fed or by deployer), or a fluid at a recharge point in a later milestone. Create: Wizardry fluids can be added as fuels through conditional data.
- At zero energy a golem goes **dormant**: it stops moving, keeps its spec, and wakes when recharged. It does not die.
- All energy logic is server-side. The client gets only a synced percentage for the UI.

## 7. Iron's Spells integration

Golemancy compiles against Iron's API jar. It casts only spells on a tested allow-list, and checks whether Iron's existing spellcasting-mob code can be reused before writing its own.

### 7.1 Build setup

```groovy
compileOnly  "io.redspace:irons_spellbooks:${irons_spells_version}:api"
localRuntime "io.redspace:irons_spellbooks:${irons_spells_version}"
localRuntime "io.redspace:irons_lib:${irons_lib_version}"
```

### 7.2 Resources used

Arcane Essence, school essences, inks, scrolls and runestone-type items as manufacturing inputs. Exact ids are checked against the pinned Iron's version in Milestone 1 and referenced only from datagen and tags.

### 7.3 Spellcasting: investigate before building

Iron's already ships mobs that cast spells, and it has a summon-manager system for ownership. Before Milestone 4, answer these questions:

- [ ] Is the mob spellcasting path (the casting goal, spell data, cooldowns) in the API jar or only in the full mod?
- [ ] Can a non-Iron's entity use it without extending Iron's base mob class?
- [ ] Does the summon manager give projectile ownership and friendly-fire handling for free?

If yes, `ISpellCaster` wraps Iron's code. If not, Golemancy implements a casting goal that handles target choice, line of sight, cooldown, energy cost, ownership and friendly fire. Either way the golem AI sees only `ISpellCaster`.

### 7.4 Spell installation

v0.1 installs a spell at the Binding Altar by adding a scroll as an inscription input. Later, a Rune Engraver step re-inscribes a dormant golem.

Validation, all server-side:

1. The spell is on the allow-list.
2. The spell's school matches the core, or the spell is marked school-neutral.
3. The frame has a free spell slot.
4. The spell level is at or below the core tier's cap.

An installed spell is stored in `GolemSpec.spells`. Casting is a separate behaviour that must be enabled.

### 7.5 Spell compatibility profiles

Each allowed spell has a JSON profile: `targeting` (self, ally, enemy, area), `range`, `min_cooldown`, `energy_cost` and `requires_los`. Spells not on the list can't be installed.

| Category | v0.1 example | Golem use |
| --- | --- | --- |
| Offensive | One fire bolt and one ice projectile spell | Ranged combat |
| Defensive | One shield or ward spell | Self-protection |
| Healing | Deferred | Self-repair, ally support |
| Movement | Deferred | Teleport, reposition |

The specific spell ids are picked during Milestone 4 from what works when cast by an entity.

### 7.6 Custom spells (later)

Golem-specific spells (Construct Repair, Golem Shield, Core Stabilisation, Spirit Recall) are registered through Iron's spell registration API in their own package, separate from golem AI.

## 8. Create integration

Every manufacturing stage in v0.1 except binding runs on stock Create machines. A custom machine is added only when a stage needs logic that stock recipes can't express.

### 8.1 Stage mapping

| Stage | v1.0 proposal | v1.1 implementation | Release |
| --- | --- | --- | --- |
| Process raw materials | Golem Foundry | Crushing, milling, pressing, mixing | v0.1 |
| Make parts | Component Fabricator | Pressing, cutting, mechanical crafting | v0.1 |
| Build frame | Assembly Platform | Sequenced assembly on belts | v0.1 |
| Magical infusion | Arcane Infusion Chamber | Spout filling and heated basin mixing | v0.1 |
| Inscription | Rune Engraver | Scroll added at the Binding Altar | v0.1 |
| Bind core and activate | Spirit Binding Chamber + Activation Station | **Binding Altar** (one custom block) | v0.1 |
| Re-inscribe a dormant golem | Rune Engraver | Rune Engraver (custom block) | Milestone 5 |
| Per-slot modular assembly | Assembly Platform | Custom deployer recipe or Assembly Platform (section 4.4) | Milestone 5 |
| Recharge in the field | Not specified | Recharge Pylon (custom block) | Milestone 6 |

### 8.2 Binding Altar

The Binding Altar is a kinetic block-entity, built on Create's kinetic base classes.

- **Inputs:** one frame, one core, up to one inscription item. Accepts items from funnels, chutes, deployers and mechanical arms.
- **Requirements:** minimum rotation speed and stress (configurable). Optionally a fluid in an attached tank, such as Arcane Essence fluid or a Create: Wizardry fluid when present.
- **Process:** validates the inputs, takes the items, runs for a configurable time, then outputs a Dormant Golem item that carries the built `GolemSpec`.
- **Output:** extracted by funnel or mechanical arm, so a full line needs no manual step.
- **Recipe:** a custom `golemancy:binding` recipe type in JSON, so modpacks can change inputs and timing. It shows in JEI and EMI.

### 8.3 Sequenced assembly example: Stone Guardian frame

1. Start with a Stone Torso Shell (pressed and cut from polished stone).
2. Deploy two stone limbs.
3. Deploy a stone head.
4. Press to fix the joints.
5. Deploy a Core Housing socket.
6. One loop; the output is the Stone Guardian Frame.

The example uses only deploying and pressing steps, so it is easy to read on a belt and to automate.

### 8.4 Mechanical requirements

Rotation speed, processing time, fluid and heat requirements are set in recipe JSON and config. A requirement is added only when it changes how a player designs the line.

### 8.5 Ponder

Players expect Ponder scenes in a Create addon, so each custom block ships with one. v0.1 needs a Binding Altar scene and a scene for the frame assembly line.

## 9. Create: Wizardry integration

Create: Wizardry support is data only: extra recipes and fuels that load when the mod is present. Golemancy never compiles against it, calls its classes or ships its assets.

### 9.1 Why data-only

- It is pre-1.0 (0.5.x on 1.21.1), so its content and ids may still change.
- It is All Rights Reserved, so Golemancy can't copy or redistribute any of its code or assets.
- Its value to Golemancy is its outputs (processed magical fluids and items). Those can be consumed through recipes without touching its code.

### 9.2 Mechanism

Every integration recipe and fuel entry is generated by datagen with a load condition:

```json
"neoforge:conditions": [
  { "type": "neoforge:mod_loaded", "modid": "create_wizardry" }
]
```

If the mod is missing or an id no longer exists, only that recipe is skipped. The base recipes still give a complete path.

### 9.3 Planned integration points

| Golemancy stage | Create: Wizardry input | Effect |
| --- | --- | --- |
| Core infusion (spout step) | Its processed magical fluids | Alternative to the base fluid; may give a cheaper or faster recipe |
| Construct energy fuel | Its magical fluids | Extra fuel entries at higher energy per unit |
| Binding Altar fluid | Its mana-type fluid | Alternative fluid requirement |

### 9.4 Before implementing

- [ ] List the fluids and items actually present in the pinned 0.5.x release.
- [ ] Confirm the mod id string used for the load condition.
- [ ] Check for recipe conflicts where both mods process the same input.

The v1.0 list of systems to reuse (essence processing, ink production, automated spellcasting) is only adopted where this check confirms it exists.

## 10. Golem entity system

There is one entity type per frame, and the material sets texture and stats. Constructs are named "Material Frame" (Stone Guardian, Iron Brute) so none clash with vanilla mobs.

### 10.1 Construct types

| Construct | Frame + material | Role | Signature | Release |
| --- | --- | --- | --- | --- |
| Stone Guardian | Guardian + stone | Defends a position | Slowing strikes, high physical resistance | v0.1 |
| Clay Brute | Brute + clay | Heavy melee | Absorbs Nature and poison damage; regenerates in water | v0.1 |
| Iron Brute | Brute + iron | Heavy guardian | Absorbs fire damage | Tier 2 |
| Flesh Guardian | Guardian + flesh | Combat | Absorbs lightning damage | Tier 4 |

The golems are inspired by classic tabletop golems but are original designs. If any mechanics are borrowed closely from the D&D 5e SRD 5.1 (CC-BY 4.0), credit it in the README.

**Naming rule:** avoid "Iron Golem", "Snow Golem" and "Copper Golem". Vanilla uses those names, which confuses commands, JEI searches and later ports.

### 10.2 Attributes

Attributes are calculated from `GolemSpec` whenever a golem loads or its spec changes:

```latex
\text{final} = \text{frame base} \times \text{material multiplier} \times \text{config multiplier}
```

Covered: health, armour, attack damage, attack speed, movement speed, knockback resistance, spell power (from conductivity), energy capacity, and carrying capacity for worker frames. They are applied as attribute modifiers with fixed ids, so recalculating never stacks them.

### 10.3 Damage, resistance and absorption

All interactions are driven by damage-type tags, not hardcoded checks:

- `golemancy:absorbed_by/<material>` tags list damage types that heal instead of harm, such as vanilla fire types plus Iron's fire school damage for iron.
- `golemancy:resisted_by/<material>` tags list damage types that are reduced.
- Iron's school damage-type ids are confirmed in Milestone 1 and added to the tags through datagen.

Absorption limits stop healing loops:

- Absorbed damage heals a configurable fraction (default 50%).
- Absorption healing is capped per time window (default 20 health per 10 seconds).
- Damage beyond the cap is treated as normal damage.

There is no acid damage type in vanilla or Iron's, so the v1.0 clay acid absorption becomes Nature and poison absorption.

### 10.4 Animation and visuals (GeckoLib)

- v0.1 animations: idle, walk, attack, cast, hurt, activate (waking up), dormant (powered down), death.
- Later: core-instability effect when energy is low or integrity is damaged.
- Material: one texture per material per frame, chosen from the spec.
- Components (later): every optional part is a bone in the frame model, shown or hidden from the spec. Milestone 0 tests this for cost and correctness.

## 11. Golem lifecycle

A golem is never lost for good by accident. It can be packed back into an item, it goes dormant instead of dying when out of energy, and when destroyed it leaves Remains that can be rebuilt for less than a new golem.

### 11.1 States

| State | Form | Ticks AI | Enters by | Leaves by |
| --- | --- | --- | --- | --- |
| Dormant item | Item with `GolemSpec` | No | Binding Altar output; owner packs up a golem | Owner uses the item on a block |
| Active | Entity | Yes | Activation; recharge from Powered down | Energy hits zero; health hits zero; owner packs it up |
| Powered down | Entity | Minimal (no pathfinding) | Energy hits zero | Recharged |
| Remains | Item with `GolemSpec`, core integrity reduced | No | Health hits zero | Repaired at the Binding Altar |

### 11.2 Pack up and redeploy

- The owner shift-uses the Control Wand (or an empty hand, configurable) on an active golem to turn it into a Dormant Golem item. The spec, energy and installed spells are kept.
- Dormant items stack to 1 and can be stored, moved by Create logistics and shown in JEI.
- This is also the main performance lever: golems not in use don't tick.

### 11.3 Death, recovery and repair

- **On death** the golem drops a **Construct Remains** item carrying its spec, with `coreIntegrity` reduced by a configurable step (default 0.25). Equipment and inventory drop normally.
- **At zero integrity** the core shatters: the Remains keep the frame but lose the core and spells, which must be replaced.
- **Rebuilding** Remains at the Binding Altar costs repair materials (the material's `repair_item`) and a fuel, not a full frame and core.
- **Field repair:** using the material's repair item on an active golem restores health. Each use has a cooldown and an energy cost, which prevents trivial tanking.
- **Core integrity** recovers only through a dedicated repair recipe, so repeated deaths have a real cost.

### 11.4 Ownership and permissions

- Each golem stores an owner UUID and an optional team.
- Only the owner, their team, or an operator can open the configuration screen, pack up the golem or change its spec.
- Golems never target their owner, the owner's team or allied golems. Golem-versus-player combat follows the server's PvP setting.
- Any world action (break, place, open a container) runs as a fake player that carries the owner's identity and fires the normal NeoForge events. Claim and protection mods (FTB Chunks and similar) then apply to golems just as they do to the owner.
- Abandoned golems (owner offline longer than a configurable time) go to Powered down. They are not deleted.

## 12. Golem programming and AI

v0.1 ships two combat profiles, Guardian and Soldier. Work profiles arrive in Milestone 6 on the same job framework, using explicit permissions for every inventory and block they touch.

### 12.1 Behaviour profiles

| Profile | Responsibility | Release |
| --- | --- | --- |
| Guardian | Holds a home position, engages hostiles inside a radius, returns home | v0.1 |
| Soldier | Follows the owner or patrols, attacks hostiles | v0.1 |
| Hauler | Moves items between linked inventories | Milestone 6 |
| Miner | Mines blocks inside a work area, deposits output | Milestone 6 |
| Farmer | Plants and harvests crops in a work area | Milestone 7 |
| Lumberjack | Harvests and replants trees | Milestone 7 |
| Artificer | Feeds and empties linked Create machines | Milestone 7 |
| Builder | Places supplied blocks from a schematic | Deferred |

### 12.2 Control Wand and configuration

The Control Wand item handles in-world assignments: set home, set a two-point work area, link an inventory, link a recharge point. Settings go in the configuration screen (section 14). Configurable options:

- home position and radius
- target rules (hostiles only; or hostiles plus listed entity types)
- combat on or off
- follow, stay or patrol
- linked inventories and item filters (Milestone 6)
- recharge point (Milestone 6)

### 12.3 Work areas

- Set by two points with the Control Wand, or by preset sizes in the GUI.
- The maximum volume and distance from home are server config values.
- The area is stored as a bounding box in `BehaviorConfig`. Block scans inside it are cached and refreshed on a schedule.

### 12.4 Logistics and permissions

- A golem only touches inventories that were explicitly linked to it, through standard NeoForge item-handler capabilities. Create depots, vaults and chests all work this way.
- No world-wide inventory search.
- World actions use the owner fake player (section 11.4).

### 12.5 AI architecture

| Interface | Role |
| --- | --- |
| `IGolemBehavior` | A profile: chooses which job runs next |
| `IGolemJob` | One unit of work (attack target, return home, haul stack) with start, tick and finish |
| `IGolemWorkArea` | Area bounds plus cached scan results |
| `IGolemInventoryAccess` | Linked inventories, filtered and permission-checked |
| `IGolemEnergyHandler` | Energy drain and recharge (wraps `IConstructEnergy`) |

- Built on the vanilla goal selector for v0.1 (simple, and well documented). Moving to the Brain system is reconsidered at Milestone 6 if job complexity calls for it.
- Decisions run on a staggered interval (default every 10 ticks, offset per golem). Movement and animation still update every tick.
- New jobs are added without changing the entity class.

## 13. Manufacturing progression

Tiers are gated by what a player can process and find: Create machine tiers, Iron's materials and exploration. No experience levels are involved.

| Tier | Constructs and cores | Gated by | Release |
| --- | --- | --- | --- |
| 1. Basic | Stone Guardian, Clay Brute; Arcane Core | Basic Create (press, deployer, sequenced assembly), Arcane Essence | v0.1 |
| 1+. Elemental basics | Fire Core, Frost Core; 1 spell slot | Spout and heated mixing, school essences, scrolls | v0.1 |
| 2. Industrial | Iron and brass constructs; Hauler; Recharge Pylon | Brass processing, linked logistics | Milestone 6 |
| 3. Elemental | Lightning Core; gold, amethyst, obsidian materials; Rune Engraver | Higher-rarity Iron's materials | Milestones 5–7 |
| 4. Advanced | Flesh Guardian; Holy, Nature, Blood cores; 2 rune slots | Iron's mid-game materials, exploration | Milestone 7 |
| 5. Legendary | Netherite, Dragonbone; Ender and Eldritch cores; multiple spell slots | Late-game exploration, boss materials | After v1.0 |

Elemental cores move into v0.1 at a basic level so the first release delivers the Create-plus-magic idea. A golem without spells would undersell the mod.

## 14. User interface and recipe discovery

v0.1 has a three-tab golem screen, a Binding Altar screen, JEI and EMI categories, and Ponder scenes. The full eight-tab screen grows with the features it shows.

### 14.1 Golem configuration screen

| Tab | Shows | Release |
| --- | --- | --- |
| Overview | Name, frame, material, health, owner | v0.1 |
| Core | Core type, school, energy bar, drain, integrity, installed spells | v0.1 |
| Behaviour | Profile, home, radius, combat toggle, follow/stay | v0.1 |
| Work area | Bounds and preview | Milestone 6 |
| Inventory | Carried items, filters, linked inventories | Milestone 6 |
| Components | Installed parts per slot | Milestone 5 |
| Statistics | Kills, blocks mined, items moved | Milestone 7 |

The screen sends requests only. The server validates every change against ownership and the spec before applying it.

### 14.2 Machine feedback

The Binding Altar shows the current stage, missing inputs, the rotation and fluid requirement, progress and output status. Create goggles show the same information as a tooltip overlay, matching how Create machines report.

### 14.3 Recipe discovery

- JEI and EMI categories for the Binding Altar, plus Create's own sequenced-assembly display for frame and core lines.
- Each JEI or EMI entry for a frame or core links to its full sequence.
- Ponder scenes for every custom block and the main frame line (section 8.5).

## 15. Data, configuration and persistence

Balance lives in datapacks and server config. All content data is generated by datagen, and saved golem data is versioned and checked on every load.

### 15.1 Data-driven definitions

| Data | Location | Generated by |
| --- | --- | --- |
| Materials | `data/*/golemancy/material/` | Datagen |
| Cores | `data/*/golemancy/core/` | Datagen |
| Frames | `data/*/golemancy/frame/` | Datagen |
| Spell profiles | `data/*/golemancy/spell_profile/` | Datagen |
| Fuels | `data/*/golemancy/fuel/` | Datagen |
| Recipes (Create and `golemancy:binding`) | `data/*/recipe/` | Datagen |
| Damage tags | `data/golemancy/tags/damage_type/` | Datagen |

All of these are synced to clients as datapack registries where the client needs them, for tooltips and JEI.

### 15.2 Server configuration

- maximum active golems per player (default 8)
- maximum work area volume and distance from home
- energy drain multiplier; durability multiplier
- absorption fraction and cap
- spellcasting on or off; golem PvP on or off
- autonomous combat on or off
- inventory and world interaction on or off
- processing speed multiplier; repair cost multiplier; integrity loss on death
- abandoned-golem timeout

Server config is synced to clients for the UI. The client never decides any of these values.

### 15.3 Persistence

A golem keeps its spec, energy, inventory, behaviour and work assignments through chunk unloads, server restarts and multiplayer sessions. Dimension changes are supported for packed-up items only in v0.1; entity travel through portals is disabled until tested.

On load:

1. Decode the spec with its Codec. If decoding fails, log the error and replace the golem with Construct Remains carrying whatever could be read. Never delete it silently.
2. Run `GolemSpecMigrator` up to the current schema.
3. Swap unknown ids for safe defaults and log a warning.
4. Recalculate attributes from the spec.

## 16. Performance and multiplayer

The server owns all golem and machine state. AI decisions run on a staggered schedule, and golems not in use are packed into items or powered down so they cost almost nothing.

### 16.1 Performance measures

| Cost | Measure |
| --- | --- |
| Many golems | Per-player cap; pack-up to items; Powered down state skips pathfinding |
| AI decisions | Every 10 ticks by default, offset per golem to spread load |
| Pathfinding | Limited range; reuse paths to home and linked inventories; back off after repeated failures |
| Block scans | Cached per work area and refreshed on a schedule, never every tick |
| Inventory access | Linked inventories only, no world search |
| Spell targeting | Checked at the decision interval, with line-of-sight raycasts only for the chosen target |
| Networking | Sync only what the UI and renderer need (energy %, animation state, visible parts); full spec sent on open-screen |

**Performance budget to test in Milestone 6:** 50 active golems on one server should add no more than 2 ms per tick. Measure with Spark.

### 16.2 Server authority

The client never authorises item use, spellcasting, golem creation, spec changes or ownership changes. Every network packet from the client is a request that the server checks against ownership, distance and the current spec.

## 17. Development roadmap

Milestone 0 tests the two riskiest design choices before any content is built, and v0.1 ships when Milestone 4's gate passes.

&#91;embedded content: roadmap · 8 milestones with gates, v0.1 at M4\]

Each milestone moves on only when its gate passes. GameTests check the gates for Milestones 1 to 4, so a later change that breaks an earlier gate fails the build.

Changes from v1.0: Milestone 0 is new. Milestone 2 now uses a spawn item or command, because manufacturing doesn't exist until Milestone 3. Lifecycle features join magic in Milestone 4, so v0.1 has a complete loop.

## 18. v0.1 release scope

v0.1 is the end of Milestone 4: a complete manufacture, bind, deploy, fight and recover loop, fully automatable, with one custom block.

### Included

- Two frames (Guardian, Brute) in two materials (stone, clay)
- Three cores: Arcane, Fire, Frost
- Binding Altar (kinetic, automatable, JSON recipe type)
- Stock-Create recipes for parts, frames and cores
- One inscription slot; a curated list of 3 to 5 spells
- Construct energy with fuels; Powered down state
- Pack-up, Construct Remains, repair at the altar
- Guardian and Soldier profiles; Control Wand (home, radius)
- Three-tab configuration screen
- Ownership, team and PvP rules
- JEI and EMI categories; Ponder scenes for the altar and the frame line
- Conditional Create: Wizardry recipes and fuels
- Datagen for all content; GameTests for binding, persistence and migration

### Deferred

| Feature | Earliest |
| --- | --- |
| Per-slot modular components and equipment | Milestone 5 |
| Rune Engraver; multiple rune slots | Milestone 5 |
| Iron and brass materials; Hauler; Recharge Pylon | Milestone 6 |
| Miner; work areas; linked inventories | Milestone 6 |
| Lightning, Holy, Nature and Blood cores; Flesh Guardian | Milestone 7 |
| Farmer, Lumberjack, Artificer | Milestone 7 |
| Curios accessories | Milestone 7 |
| Builder; fully programmable logic | After v1.0 |
| Dragonbone, Netherite; Ender and Eldritch cores | After v1.0 |
| Golem squads, factory-wide coordination | After v1.0 |
| Arbitrary spell support | Not planned |
| Forge 1.20.1 port | After v1.0 |

## 19. Technical risks and mitigations

The two biggest risks are modular data through sequenced assembly and spellcasting by non-player entities. Milestone 0 and the Milestone 4 investigation test both before any content depends on them.

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Item data can't move through sequenced assembly | Modular design blocked | Fixed frames plus altar merge (section 4.2); M0 spike for the modular path |
| Iron's spells misbehave when cast by entities | Broken or exploitable spells | Allow-list with profiles; reuse Iron's mob casting where possible |
| Create internal changes on update | Kinetic blocks and assembly hooks break | All Create code in `integration.create`; exact pin; porting pass per update |
| Create: Wizardry changes before 1.0 | Integration recipes fail | Data-only with load conditions; base path never depends on it |
| Iron's version-string range rejection | Mod refuses to load | Test `neoforge.mods.toml` ranges against real version strings |
| Saved data corruption or old schema | Lost golems, server crash | Codecs, schema version, migrator, fallback to Remains |
| AI tick cost | Server lag | Staggered decisions, caps, pack-up, Spark budget at M6 |
| Griefing through golems | Claim bypass | Owner fake player, normal events, ownership checks |
| Absorption healing loops | Immortal golems | Fraction plus per-window cap |
| Recipe id conflicts with Create or addons | Recipes overwritten | All recipes under the `golemancy` namespace |
| Scope growth | Never ships | v0.1 scope fixed in section 18; new features go to the deferred list |

## 20. Definition of success and long-term vision

v0.1 succeeds when a player can run the whole loop below on an automated line using only the required dependencies.

### 20.1 v0.1 success criteria

1. Gather stone or clay and process it into parts on Create machines.
2. Assemble a frame through sequenced assembly.
3. Make a core with spout and basin steps using Iron's Spells resources.
4. Bind frame, core and a scroll at the Binding Altar, fed by funnels or mechanical arms.
5. Deploy the golem, set Guardian or Soldier, and see it cast its installed spell.
6. Recharge it, pack it up and redeploy it with its state kept.
7. Lose it in combat, recover the Remains and rebuild it.
8. Repeat steps 1 to 4 with no manual input.

### 20.2 Long-term vision

Players eventually run whole magical manufacturing facilities: specialised construct lines, repair and maintenance stations, recharge networks, siege constructs, mobile factories and command networks for golem squads.

Create provides the machinery, Iron's Spells provides the magic, and Create: Wizardry can connect them further. Golemancy turns the combination into construct engineering at factory scale.
