# Milestone 0 spikes

Throwaway investigation code. **Nothing here is compiled into the mod.** Each spike answers one
question from the SDD; the conclusions are recorded in `docs/DECISIONS.md` (D-005 to D-007).

| Spike | SDD | Question | Answer |
| --- | --- | --- | --- |
| `01_sequenced_assembly` | 4.4, 19 | Can arbitrary item data (data components) ride through Create's sequenced assembly? | **No.** Confirms fixed frames + altar merge (SDD 4.2). |
| `02_geckolib_optional_bones` | 10.4 | Can optional parts be bones that are shown/hidden per entity from the spec, cheaply? | **Yes.** `GeoBone#setHidden` per render pass. |
| `03_irons_entity_casting` | 7.3 | Can a non-Iron's entity cast Iron's spells through the API jar? | **Yes, for instant spells**, via `AbstractSpell#onCast(..., CastSource.MOB, new MagicData(true))`. Iron's mob-casting classes are not reusable. |

## How each was checked

The spikes were answered by reading the pinned dependency sources (Create 6.0.10-280,
GeckoLib 4.9.3, Iron's Spells 1.21.1-3.16.3) and are then pinned down by GameTests in the real mod
so a dependency update that changes the answer fails the build:

- Spike 1 → `GolemancyGameTests#sequencedAssemblyDiscardsExtraComponents`
- Spike 2 → optional bones exist in both `.geo.json` models and are hidden by `ConstructRenderer`
  (visual check in `docs/PLAYTEST.md`)
- Spike 3 → `GolemancyGameTests#golemCastsInstalledSpell`
