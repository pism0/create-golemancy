---
name: minecraft-create-testing
description: How to test Create: Golemancy (Minecraft 1.21.1 NeoForge) in game and in GameTests on this machine, and how to build working Create machines by command. Use when launching the dev client, writing or running a tools/smoke script, driving the game window with computer use, writing GameTests, or setting up Create presses, deployers, spouts, mixers, crafters, funnels or motors for a test.
---

# Testing Golemancy in Minecraft, and building Create machines

## Gates first

- `./gradlew build` and `./gradlew runGameTestServer` (also with `-Pwith_wizardry=false`) are the
  real gates. Run both GameTest setups after any gameplay change. Each takes about 1.5 minutes.
- When you fix a bug found in game, write a GameTest that fails before the fix. Reproduce the exact
  in-game geometry where you can. Example: `EntityTests.bruteClosesLastGapToTarget` places the
  golem at the position it stalled at in game.
- Use the in-game client for visual checks, GUIs, rendering, Ponder and JEI, and for flows a
  GameTest cannot stage.

## Launching the dev client (no GPU)

- The machine has no GPU. Mesa llvmpipe sits in a private JDK copy at
  `%LOCALAPPDATA%\Programs\jdk-21-mesa` (opengl32.dll and libgallium_wgl.dll next to java.exe).
  `./gradlew runClient` does not work here, because Gradle forces its own toolchain JDK.
- Launch:
  `tools/run_client_direct.sh "$LOCALAPPDATA/Programs/jdk-21-mesa/bin/java.exe" <script> [timeout_s]`
  - `<script>` is a file name in `tools/smoke/` without the `.txt`.
  - Gradle prepares the classes and argument files (task `writeClientSmokeClasspath`); the script
    launches `net.neoforged.devlaunch.Main` itself.
  - The task copies the run's `-Dfml.modFolders`. Without it, Golemancy's resources load but its
    code does not.
  - Extra JVM flags go in `SMOKE_JVM_OPTS`, for example
    `SMOKE_JVM_OPTS=-Dgolemancy.debugTargets=true`.
  - Redirect the output to a scratchpad log and grep for `[smoke]`, `[CHAT]` and `ERROR`. A run
    takes about 30 s to reach the world, then follows the script.
- `run-smoke/options.txt` is written once if it is missing. It must start with `version:3955`;
  without that, key bindings are read as numbers. Do not put comments in it. It turns off menu
  blur, fancy graphics, clouds and pause on lost focus, and binds use to J and attack to K.

## Smoke scripts (`client/dev/DevSmoke.java`)

One step per line, `<tick> <action> <argument>`; `#` starts a comment. Ticks count from world
join. The script makes a fresh creative superflat world (ground at y=-61, you stand at y=-60).

| Action | Meaning |
|---|---|
| `cmd <command>` | sends a chat command, without the `/` |
| `shot <name>` | screenshot into `run-smoke/screenshots/`; a `nogui_` prefix hides the HUD |
| `gui true\|false` | shows or hides the HUD |
| `use x y z face` | the player's right-click with the held item on that block face (real client path) |
| `useentity <type id>` | right-clicks the nearest entity of that type within 8 blocks |
| `attackentity <type id>` | left-clicks the nearest entity of that type within 6 blocks |
| `sneak true\|false` | sets sneaking |
| `quit` | closes the game. Leave it out (as `interactive.txt` does) to keep the game open for computer use |

Tips:
- Deploy golems with `use` while the player holds the item, so the player becomes the owner.
  `/summon` gives an ownerless golem.
- To read results, use `cmd data get entity @e[type=…,limit=1] <path>` or
  `cmd execute if entity … run say …`, then grep the log for `[CHAT]`.
- Use `gamemode survival`, `effect give @s resistance infinite 4 true`, `gamerule
  doMobSpawning false` and `time set noon`. Husks don't burn in daylight, so they make good
  hostile targets.
- Use `NoAI:1b` to pin a target in place. Golems need AI, so set NoAI only for screenshot tours.
- Leave about 200 ticks between sections; the software renderer is slow.
- Existing scripts: `m1_items`, `m4_tour`, `interactive`, `c_factory`, `c_deployer_probe`,
  `c_frame_probe`, `c5_handsoff` (full automated line), `d_behaviour` (PLAYTEST D), `d_soldier_probe`,
  `d_soldier_timeline`, `gui_probe`, `server_check`.
- Without Create: Wizardry: `SMOKE_GRADLE_ARGS=-Pwith_wizardry=false tools/run_client_direct.sh …`.
- Give the client about 8 ticks between `item replace` and a `use`; with 2 ticks it sometimes
  still used the previous item.
- An Iron's scroll by command: `irons_spellbooks:scroll` with component
  `"irons_spellbooks:spell_container":{maxSpells:1,mustEquip:false,improved:false,spellWheel:false,data:[{id:"irons_spellbooks:firebolt",index:0,level:1,locked:true}]}`.

## Dedicated server

- The owner accepted the Minecraft EULA on 2026-11-25 (DECISIONS D-018). The `server` run uses
  `run-server/` (git-ignored): `eula.txt`, `server.properties` (`online-mode=false`, flat world),
  `ops.json` opping the offline player `Dev` (UUID 380df991-f603-344c-a090-369bad2a924a).
- Start it in the background: `./gradlew runServer --console=plain > <scratchpad>/server.log 2>&1 &`,
  then wait until the log shows `Done (`. Join with
  `SMOKE_JVM_OPTS=-Dgolemancy.smokeServer=localhost:25565 tools/run_client_direct.sh <java> <script>`.
  Scripts run as usual (ticks count from joining). Begin with `kill @e[type=!player]`: the world
  persists between runs. `server_check` (section D), `gui_probe` and `c5_handsoff` all pass there.
- Stop it by killing the java process whose command line has `serverRunVmArgs` (and its gradlew
  wrapper). The NeoForge version-check JSON error and the "chat can't be verified" toast are expected
  offline.
- The `shot` step logs the open screen (`screen: GolemScreen`); use that, not the image, to check a
  GUI opened, since a screenshot is the previous frame.

## Driving the game with computer use

- `request_access` finds the game only by the full exe path:
  `C:\Users\admin\AppData\Local\Programs\jdk-21-mesa\bin\java.exe`.
- Clicks in the 3D view turn the camera, because the mouse is captured. Aim with `/tp @s x y z yaw
  pitch` from chat and act with J and K. GUI screens can be clicked normally.
- The owner's elevated admin terminal and the pinned Claude window can cover the game. Ask the
  owner to minimise the terminal, then place the window with SetWindowPos (x=180, y=40, 1296x759,
  flags 0x0014).
- `type` sometimes pastes through the clipboard and once pasted the user's own clipboard. Type
  short strings (15 characters or fewer) and check what landed. Never repeat clipboard content you
  did not type.

## GameTest harness limits

- Structure templates are NBT files generated by `tools/make_structures.py`. Ponder schematics come
  from `tools/make_ponder.py`.
- `helper.makeMockPlayer` joins the server, and Iron's Spells then tries to sync config over a
  connection that does not exist. Use `gametest/TestPlayer.create(level, name)` instead: a real
  ServerPlayer with no connection that is never added to the level. Place it with `setPos`, because
  `moveTo` needs a connection.
- A player cannot be added to the level (`level.addNewPlayer`), even with an embedded connection:
  Curios fails with "Payload curios:sync_curios may not be sent to the client". So behaviour that
  needs `level.getPlayerByUUID` to find the owner (for example a Soldier assisting its owner) cannot
  be GameTested. Verify it in game with a smoke script, and GameTest the parts that do not need the
  owner in the level.
- Do not use NeoForge FakePlayer for "a player" in tests: Golemancy treats FakePlayers as machines.

## Golem AI pitfalls already found

- Vanilla navigation counts the node next to a target as reached. A wide golem (a Brute is 1.9
  blocks across) then stalls just outside melee reach, and `moveTo` returns false because the path
  is already done. `GolemJobs.MeleeJob` now steers straight at a nearby target once the path is
  done. Keep this in mind for any new wide frame or job.
- Debug AI with temporary `LOGGER.info` lines behind `Boolean.getBoolean("golemancy.debugTargets")`
  and pass the flag through `SMOKE_JVM_OPTS`. Remove the lines before committing.

## Building Create 6.0.10 machines by command

Placement:
- A press, deployer or spout sits 2 blocks above a depot. A mixer or press sits 2 blocks above a
  basin.
- A blaze burner goes under the basin:
  `setblock x y z create:blaze_burner[blaze=kindled]{fuelLevel:1,burnTimeRemaining:100000}`.
- Power: `create:creative_motor[facing=up]{ScrollValue:64}` sets 64 RPM. A motor drives only the
  block in front of its `facing` side.
  - Deployers `[facing=down,axis_along_first=false]` and presses `[facing=north]` run from a motor
    one block south of them, `facing=north` (that is what the working scripts use).
  - A mechanical saw that processes dropped items: `create:mechanical_saw[facing=up,axis_along_first=true]`
    with a motor beside it on the shaft axis (`facing=east` from the west side).
  - The sign of `Speed` is the rotation direction, not a fault. -64 is as good as 64; only 0 means
    unpowered or detached.
  - A mixer does not connect to a motor directly above it. Put a `create:cogwheel` beside the mixer
    and the motor under that cog.
  - Shafts and cogs pass rotation as in survival. Check that the speed is not 0 with
    `data get block … Speed`.
- **`/data merge` on a kinetic block detaches it from its network (speed 0).** Set NBT only in the
  `setblock` that places the block. Never merge into a running machine; place it again instead.

Loading machines:
- Deployers take items when the player right-clicks their working face (for a deployer facing down
  that is `use x y z down`). Clicking a side face sets the filter instead. Merging `HeldItem` kills
  the rotation (see above).
- Spouts can't be filled with a bucket (right-click returns PASS). Place them with the tank already
  filled:
  `setblock … create:spout{Tanks:[{TankContent:{Fluid:{id:"minecraft:water",amount:1000}}}]}`.
  Note the `Fluid` wrapper.
- Mechanical crafters take a whole stack in one click. Give each crafter one item, for example
  `item replace … with X 1`, then `use`.
- Crafter grids are linked with ConnectedInput NBT. The controller lists the member offsets:
  `{ConnectedInput:{Controller:1b,Data:[{X:0,Y:-1,Z:0},…]}}`. Members get `Controller:0b` and their
  offset to the controller. A chain of crafters pointing down and right worked.
- Mixers: a heated recipe needs a kindled blaze burner under the basin; an unheated one (the Frost
  Core: quenched housing + Arcane Essence + 2 packed ice) runs in a plain basin.

Moving items:
- Dropping items onto a machine is the quickest input: depots, basins, saws and funnels pick up item
  entities. Summon them still, just above the block:
  `summon item x.5 y z.5 {Item:{id:"…",count:N},Motion:[0.0,0.0,0.0]}`.
- Funnels: `facing` points AWAY from the block the funnel is attached to (a funnel on the north face
  of a block is `facing=north`). `create:andesite_funnel[facing=…,extracting=true]` pulls out of
  that block; leave out `extracting` to insert into it.
  - A belt hands items only to a funnel facing UP (lying on top of the inventory, at belt height); a
    wall funnel at a belt's end ignores the belt (`FunnelBlockEntity.supportsDirectBeltInput`).
  - An extracting wall funnel with nothing in front drops what it pulls as item entities in front.
  - A redstone block next to a funnel pauses it; remove the block to release it. Handy for checking
    what waits in an inventory before letting it out.
  - An andesite funnel moves one item at a time.
- Chutes: a chute pulls from the inventory directly above it and drops onto a belt or block below.
- Comparators: `minecraft:comparator[facing=D]` reads the block on its `D` side (facing=south
  reads the block to its south). Read it with `data get block … OutputSignal`.

Belts:
- Lay them the player's way: two `create:shaft[axis=z]` (for a belt along x), hold
  `create:belt_connector`, `use` the first shaft, then the second (teleport within ~5 blocks of each).
- The belt's direction depends on the motor: with a motor `facing=north` beside the start, ScrollValue
  64 made the belt run east (its Speed reads -64). Check by dumping the controller's `Inventory`
  (`data get block <start> Inventory`, entries have `Pos` along the belt and `Locked:1b` when a
  station holds them).
- A deployer with an empty hand HOLDs every item that reaches it (Create's BeltDeployerCallbacks),
  so stock deployers with stacks. Sequenced assembly works on a belt with stations in step order.
- A chute below a chest pulls all matching items as one stack; an item an inventory refuses waits at
  the belt end and blocks everything behind it.
- Raise the whole line one block (belt y=-59, stations y=-57) when the end must feed a block that
  sits on the ground.

Golemancy machines:
- The Binding Altar needs rotation from below: put a creative motor `facing=up` under it.
  Right-click with items to load it. Read the result with `data get block … Output`. Allow about
  200 ticks per bind at 64 RPM.
  - Automation sees slots 0–4 as inputs (any slot; the altar routes each item to the body, core,
    inscription or supply slot) and slot 5 as the output. Only the output can be extracted.
  - Binding starts as soon as a frame and a core are in. Any input that arrives later (a scroll)
    recomputes the plan and restarts the progress, so the scroll is still included.
  - Comparator: 15 while a finished golem waits, otherwise the progress (0–14).
  - `data get block …` shows `Status` (7 = binding), `Progress`, `Time` and `Inputs`/`Output`.
- `tools/smoke/c_factory.txt` builds the full PLAYTEST C station row; copy its layouts.
  `tools/smoke/c5_handsoff.txt` is a complete chest-to-golem line.

Reading Create's own code when a mechanic is unclear:
- The slim jar is in the Gradle cache (`find ~/.gradle/caches -name "create-1.21.1-6.0.10-280-slim.jar"`).
  Unzip the class you need into the scratchpad and run
  `"/c/Program Files/Java/jdk-21.0.11/bin/javap.exe" -c -p <class>`. Useful classes:
  `content/logistics/funnel/FunnelBlockEntity`, `content/kinetics/deployer/BeltDeployerCallbacks`,
  `content/kinetics/belt/...`. One look at the bytecode beat several blind test runs.

## Docs to keep up to date

After an in-game pass, record the results in `docs/PROGRESS.md` (the in-game check tables) and any
changed steps in `docs/PLAYTEST.md`. Save screenshots worth keeping to `art/previews/ingame/`.
After code changes, rebuild and copy the jar to `playtest/`.
