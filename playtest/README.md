# Create: Golemancy 0.1.0-rc1 — playtest instance

This folder is for testing in a real launcher. (For a quick check you do not need it:
`./gradlew runClient` in the project folder starts a development client with every mod already in
place and an offline dev account.)

Contents:

- `golemancy-0.1.0-rc1.jar` — the built mod
- `mods.txt` — every required and optional mod with exact version and download page
- this README

You sign in and launch yourself; nothing here needs or stores an account.

## Prism Launcher

1. **Add Instance** → name it "Golemancy playtest" → Minecraft **1.21.1** → Mod loader
   **NeoForge**, version **21.1.266** → OK.
2. Select the instance → **Edit** → **Mods** → **Download mods**. Search and add, picking the
   versions listed in `mods.txt`: Create, Iron's Spells 'n Spellbooks, Iron's Lib, GeckoLib, Curios
   API, Player Animator, and optionally JEI and Create: Wizardry. Prism offers to add missing
   dependencies; accept, then check the versions against `mods.txt`.
3. Still on the Mods page: **Add file** → pick `golemancy-0.1.0-rc1.jar` from this folder.
4. Launch.

## CurseForge app

1. **Create Custom Profile** → Minecraft **1.21.1** → Modloader **NeoForge 21.1.266** → Create.
2. Open the profile → **Add More Content** → install Create, Iron's Spells 'n Spellbooks (this
   pulls Iron's Lib, GeckoLib, Curios and Player Animator), and optionally JEI and
   Create: Wizardry. Use the gear icon on each mod to pick the file version listed in `mods.txt`.
3. Profile → ⋮ → **Open Folder** → copy `golemancy-0.1.0-rc1.jar` into `mods/`.
4. Play.

## If the game refuses to start

NeoForge shows a screen naming the mod and the version it wanted. Compare with `mods.txt`; the
usual causes are a newer Iron's Spells than Create: Wizardry 0.5.0 accepts (needs below 3.17.0) or
a Create newer than 6.0.10. Golemancy itself accepts Create 6.0.10 up to (not including) 6.1.0,
Iron's Spells 1.21.1-3.16.3 or newer, GeckoLib 4.9.3 or newer.

Then follow `docs/PLAYTEST.md` in the project folder.
