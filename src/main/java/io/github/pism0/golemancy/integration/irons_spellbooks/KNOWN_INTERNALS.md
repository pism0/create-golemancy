# Iron's Spells 'n Spellbooks: known internals

SDD 2.2: any class used from the full mod (outside `io.redspace.ironsspellbooks.api`) must sit
behind an adapter in this package and be listed here.

**Currently: none.** Golemancy compiles against the `:api` artifact only, so a non-API reference
would not even compile.

API classes used (pinned 1.21.1-3.16.3):

| Class | Used for |
| --- | --- |
| `api.registry.SpellRegistry` (`REGISTRY`) | Look up a spell by id |
| `api.spells.AbstractSpell` | `getCastType`, `isEnabled`, `getMinLevel`/`getMaxLevel`, `getManaCost`, `getSchoolType`, `checkPreCastConditions`, `onCast`, `onServerCastComplete` |
| `api.spells.CastSource`, `api.spells.CastType` | `CastSource.MOB`, `CastType.INSTANT` |
| `api.magic.MagicData` | `new MagicData(true)` (mob magic data) |
| `api.spells.ISpellContainer`, `api.spells.SpellSlot` | Read the spell on a scroll; write one in tests |
| `api.registry.SchoolRegistry`, `api.spells.SchoolType` | School id and display name |
| `api.config.SpellConfigParameter`, `api.config.DefaultConfig` | Built-in school of a spell before Iron's lazy config is loaded |
| `api.registry.AttributeRegistry` (`SPELL_POWER`) | Scale spell power by conductivity and core integrity |

Note: several API classes mention non-API types in *other* methods (for example
`MagicData.getSyncedData()` returns `capabilities.magic.SyncedSpellData`, and
`SpellRegistry.none()` returns `spells.NoneSpell`). Those methods are deliberately not called.
