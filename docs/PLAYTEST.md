# Playtest guide (v0.1 release candidate)

Everything server-side is covered by GameTests (`./gradlew runGameTestServer`, 36 tests).
Sections A and B were checked in-game on 2026-11-25 under software rendering (results and the bugs
fixed: `docs/PROGRESS.md`, "In-game check"); they are still worth a quick look on real hardware.
Sections C and D have not been played yet. **If anything in A or B is wrong on your machine, stop
and send me the log.**

Run from the project folder:

```bash
./gradlew runClient
```

Fast alternative for section B (no clicking, takes screenshots, then exits):

```bash
./gradlew runClientSmoke -Psmoke=m4_tour
```

Screenshots are written to `run-smoke/screenshots/`.

## SDD 20.1 criteria: where each one is verified

| # | Criterion | Automated (GameTest) | Manual |
| --- | --- | --- | --- |
| 1 | Process stone or clay into parts on Create machines | `manufacturingRecipesLoad` (recipes exist and load) | C1 |
| 2 | Assemble a frame through sequenced assembly | `sequencedAssemblyBuildsFrames` | C2 |
| 3 | Make a core with spout and basin steps | `manufacturingRecipesLoad`, `sequencedAssemblyBuildsFrames` (housing) | C3 |
| 4 | Bind frame, core and scroll at the altar, fed by funnels or arms | `bindingBuildsTheRightSpec`, `bindingRejectsInvalidInputs`, `altarBindsUnderRotation`, `altarIsFedByAutomation`, `mechanicalArmCanTargetAltar` | C4 |
| 5 | Deploy, set Guardian or Soldier, see it cast | `dormantItemDeploysGolemWithSpec`, `guardianAttacksHostile`, `golemCastsInstalledSpell`, `ownershipIsEnforced` (profile request) | D1–D3 |
| 6 | Recharge, pack up, redeploy with state kept | `energyZeroPowersDownAndRechargeWakes`, `packUpAndRedeployKeepsState` | D4–D5 |
| 7 | Lose it, recover the Remains, rebuild | `deathDropsRemainsWithReducedIntegrity`, `remainsAreRebuiltAtAltar` | D6 |
| 8 | Repeat 1–4 with no manual input | `altarIsFedByAutomation` (altar end), 1–3 are stock Create recipes | C5 |

## A. First launch (5 minutes)

1. `./gradlew runClient`. The game reaches the title screen. **Check:** no crash, no error screen
   about missing or mismatched mods.
2. Mods list: Create, Iron's Spells 'n Spellbooks, GeckoLib, JEI, Create: Wizardry and
   **Create: Golemancy 0.1.0-rc1** (with its logo: a stone head over a violet core) are present.
3. Create a new creative world (superflat is fine). **Check:** the world loads.
4. Search the log `run/logs/latest.log` for `golemancy` and for `ERROR`. **Check:** no error
   mentioning golemancy, GeckoLib models, Ponder or JEI plugins.
5. Optional: repeat 1–4 with `./gradlew runClient -Pwith_wizardry=false` (no Create: Wizardry).

## B. Art and rendering (10 minutes)

Open the creative tab **Create: Golemancy**.

1. **Items.** Every item has a texture (no purple-black checkerboard). Parts, housings, cores (three
   colours with a glyph), frames, Control Wand, Binding Altar.
2. **Dormant Golems.** The tab lists 12 ready-made golems. The statuette is tinted by material
   (grey stone, terracotta clay) and its chest dot by core colour (violet, orange, cyan).
3. **Deploy** a "Dormant Stone Guardian" (fire core) on the ground. **Check:**
   - it stands upright facing you, plays a wake-up animation for 2 seconds, then idles;
   - its chest socket and eyes glow orange, also at night (`/time set midnight`);
   - the body is grey cut stone with brass straps and a shield on its left forearm;
   - no extra parts are visible (no banner on its back, no crest, no mace: those optional bones
     must stay hidden).
4. Deploy a "Dormant Clay Brute". **Check:** wide, low, terracotta, heavy arms.
5. Walk away so they follow or return home. **Check:** legs and arms swing while walking, the head
   turns toward what it looks at (not away from it, not upside down).
6. **Binding Altar.** Place one. **Check:** casing with a wood panel, stone dish with a violet rune
   ring on top; a short shaft sticks out underneath (place it on a shaft or creative motor to see
   it turn).
7. Open the altar's **Ponder** (hold W on the item in JEI/inventory). **Check:** two scenes exist
   ("Binding golems at the Binding Altar" and, on a Torso Shell or Frame, "Assembling Construct
   Frames") and play to the end.
8. **JEI.** Look up uses of the Binding Altar. **Check:** a "Binding Altar" category with bind pages
   (frame + core + optional scroll → Dormant Golem) and rebuild pages (Remains + repair items +
   fuel). Look up a frame's recipe: Create's Sequenced Assembly page shows the steps.

If an animation looks mirrored or a limb bends the wrong way, tell me which animation (idle, walk,
attack, cast, hurt, activate, dormant, death) and which golem; the fix is a sign in
`tools/art/entities.py`.

## C. Manufacturing in survival-style play (30–40 minutes)

Use creative for materials if you like; the point is that the machines accept the recipes.

- **C1 Parts.** Mechanical Saw: smooth stone → 2 Stone Plates. Mechanical Press: clay block → 2 Clay
  Plates. Basin + Press: 2 plates + andesite alloy → Limb. Mechanical Crafters: 2×2 plates → Head;
  3×3 plates with an andesite casing in the middle → Torso Shell.
- **C2 Frame.** Put a Stone Torso Shell on a belt or depot. Deployers in order: Stone Limb, Stone
  Limb, Stone Head; then a Mechanical Press; then a Deployer with a Core Housing. **Check:** the item
  becomes "Incomplete Stone Guardian Frame" with a step tooltip, then a Stone Guardian Frame.
  For a Brute, the first deployer holds a Stone Plate instead.
- **C3 Core.** Core Housing: copper sheet → deploy amethyst shard → press → deploy Arcane Essence.
  Spout water onto a housing → Quenched Core Housing; lava → Molten Core Housing. Heated basin
  (Blaze Burner) with a mixer: Molten housing + Arcane Essence + 2 blaze powder → Fire Core.
  Quenched + 2 Arcane Essence + amethyst shard (heated) → Arcane Core. Quenched + Arcane Essence +
  2 packed ice (no heat) → Frost Core.
- **C4 Binding.** Altar on top of a vertical shaft at 32 RPM or more. Right-click it with the frame,
  the core, and (optionally) an Iron's scroll of Firebolt, Icicle, Ray of Frost or Shield.
  **Check:** hovering the altar shows "Ready to bind" and a progress percentage; particles spiral
  in; after about 10 seconds at 32 RPM a Dormant Golem floats on top; empty-hand right-click takes it.
  Try a wrong combination (Icicle scroll with a Fire Core): the hover text says why it is refused.
- **C5 Hands-off.** Funnels or a chute feeding frame, core and scroll into the altar; a funnel or
  mechanical arm pulling from it. **Check:** only the finished golem comes out; inputs are never
  pulled back out. A comparator on the altar gives 15 while a golem is waiting.
- **With Create: Wizardry:** spout 500 mB of its mana onto a Core Housing → Arcane Core directly.
  Pipe mana into the altar: goggles show "Catalyst" and binding runs twice as fast.

## D. Golem behaviour (20 minutes, survival mode for combat)

- **D1 Configure.** Right-click your golem with the Control Wand (or an empty hand). **Check:** a
  panel with three tabs opens: Overview (frame, material, owner, health bar), Core (core, school,
  energy bar, drain, integrity bar, spell), Behaviour (profile, move mode, combat, radius −/+,
  spells on/off, set home, pack up). Every button changes the text straight away.
- **D2 Guardian / Soldier.** Guardian + Stay: spawn a zombie 8 blocks away; the golem engages and
  walks back to its home afterwards. Soldier + Follow: it follows you and attacks what you hit.
- **D3 Casting.** A golem with a Fire Core and Firebolt throws firebolts at hostiles more than
  3 blocks away and switches to melee up close. It never damages you. Turn "Spells: OFF" and it
  stops casting.
- **D4 Energy.** `/data merge entity @e[type=golemancy:guardian,limit=1,sort=nearest] {GolemEnergy:0f}`
  → within a second it slumps, stops glowing and smokes ("Powered down"). Right-click it with
  redstone, glowstone dust, blaze powder or Arcane Essence: it wakes up.
- **D5 Pack up.** Sneak + right-click with the wand (or empty hand): you get a Dormant Golem whose
  tooltip shows its energy; deploy it elsewhere and check energy, health and settings are kept.
- **D6 Death and rebuild.** `/kill` the golem (or let mobs do it). **Check:** a collapse animation,
  then a "... Remains" item that does not despawn, tooltip "Core integrity: 75%". At the altar:
  Remains + 4 of the material's repair item (cobblestone/stone for stone, clay balls for clay) + one
  fuel item → Dormant Golem. Add a core of the same type to restore integrity to 100%.
  After four deaths without a new core the core shatters and a core is required.
- **D7 Ownership (needs a second player or `/op`).** Another player cannot open the screen, pack
  up, or redeploy your golem ("This golem is not yours"); they can feed it fuel.

## Useful commands

```
/summon golemancy:guardian ~ ~ ~ {GolemSpec:{schema:1,frame:"golemancy:guardian",material:"golemancy:clay",core:"golemancy:frost"}}
/give @s golemancy:dormant_golem[golemancy:golem_spec={schema:1,frame:"golemancy:brute",material:"golemancy:stone",core:"golemancy:fire",spells:[{spell:"irons_spellbooks:firebolt",level:1}]}]
```

Server config (per world): `saves/<world>/serverconfig/golemancy-server.toml`.
