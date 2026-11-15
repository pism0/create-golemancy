"""Read-only inspection of the pinned dependency jars in the Gradle cache.

Used to verify ids (items, fluids, damage types, spells) against the pinned versions before they
are written into datagen, and to record mods.toml dependency declarations in docs/VERSIONS.md.
Nothing is copied out of the jars: this only lists names.

Usage:  python -I tools/inspect_jars.py <command> [args]
  deps                      mods.toml dependency blocks of irons_spellbooks, irons_lib, create_wizardry
  ls <mod> <regex>          list jar entries matching a regex
  cat <mod> <entry>         print a (text) entry
  items <mod>               item ids inferred from models/item + items/ definitions
  check <id> [<id> ...]     report whether namespace:path ids exist as item / fluid / damage_type / spell
"""
import os
import re
import sys
import zipfile

CACHE = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1")

MODS = {
    "irons_spellbooks": ("io.redspace", "irons_spellbooks", r"irons_spellbooks-1\.21\.1-[\d.]+\.jar$"),
    "irons_api": ("io.redspace", "irons_spellbooks", r"irons_spellbooks-1\.21\.1-[\d.]+-api\.jar$"),
    "irons_lib": ("io.redspace", "irons_lib", r"irons_lib-.*\.jar$"),
    "create_wizardry": ("maven.modrinth", "create-wizardry", r"create-wizardry-.*\.jar$"),
    "create": ("com.simibubi.create", "create-1.21.1", r"create-.*-slim\.jar$"),
    "ponder": ("net.createmod.ponder", "ponder-neoforge", r"ponder-neoforge-.*\.jar$"),
    "geckolib": ("software.bernie.geckolib", "geckolib-neoforge-1.21.1", r"geckolib-.*\.jar$"),
    "jei": ("mezz.jei", "jei-1.21.1-neoforge", r"jei-.*\.jar$"),
    "curios": ("maven.modrinth", "curios", r"curios-.*\.jar$"),
}


def jar_path(mod):
    group, artifact, pattern = MODS[mod]
    base = os.path.join(CACHE, group, artifact)
    hits = []
    for root, _dirs, files in os.walk(base):
        for f in files:
            if re.search(pattern, f) and "sources" not in f:
                hits.append(os.path.join(root, f))
    if not hits:
        raise SystemExit(f"jar for {mod} not found under {base}; run ./gradlew printRuntimeMods first")
    return sorted(hits)[-1]


def names(mod):
    with zipfile.ZipFile(jar_path(mod)) as z:
        return z.namelist()


def read(mod, entry):
    with zipfile.ZipFile(jar_path(mod)) as z:
        return z.read(entry).decode("utf-8", "replace")


def cmd_deps():
    for mod in ("irons_spellbooks", "irons_lib", "create_wizardry"):
        text = read(mod, "META-INF/neoforge.mods.toml")
        print(f"=== {mod}  ({os.path.basename(jar_path(mod))})")
        for m in re.finditer(r'(?m)^\s*(modId|version|versionRange|type|mandatory)\s*=\s*(.+)$', text):
            print("   ", m.group(1), "=", m.group(2).strip())


def item_ids(mod):
    ns_ids = set()
    for n in names(mod):
        m = re.match(r"assets/([a-z0-9_]+)/models/item/([a-z0-9_/]+)\.json$", n)
        if m:
            ns_ids.add(f"{m.group(1)}:{m.group(2)}")
    return ns_ids


def lang_keys(mod):
    import json
    keys = {}
    for n in names(mod):
        if re.match(r"assets/[a-z0-9_]+/lang/en_us\.json$", n):
            try:
                keys.update(json.loads(read(mod, n)))
            except Exception as e:  # noqa
                print("lang parse failed", n, e)
    return keys


def cmd_check(ids):
    all_mods = ["irons_spellbooks", "create_wizardry", "create"]
    items = {}
    langs = {}
    for mod in all_mods:
        try:
            items[mod] = item_ids(mod)
            langs[mod] = lang_keys(mod)
            langs[mod + "#names"] = set(names(mod))
        except SystemExit:
            items[mod] = set()
            langs[mod] = {}
            langs[mod + "#names"] = set()
    for full in ids:
        ns, path = full.split(":", 1)
        found = []
        for mod in all_mods:
            if full in items[mod]:
                found.append(f"item-model[{mod}]")
            for kind in ("item", "block", "fluid", "fluid_type", "spell"):
                if f"{kind}.{ns}.{path}" in langs[mod]:
                    found.append(f"lang:{kind}[{mod}]")
            if f"data/{ns}/damage_type/{path}.json" in langs[mod + "#names"]:
                found.append(f"damage_type[{mod}]")
            if f"assets/{ns}/blockstates/{path}.json" in langs[mod + "#names"]:
                found.append(f"blockstate[{mod}]")
        print(("OK      " if found else "MISSING ") + full + "  " + ", ".join(found))


def main(argv):
    if not argv:
        raise SystemExit(__doc__)
    c = argv[0]
    if c == "deps":
        cmd_deps()
    elif c == "ls":
        rx = re.compile(argv[2])
        for n in names(argv[1]):
            if rx.search(n):
                print(n)
    elif c == "cat":
        print(read(argv[1], argv[2]))
    elif c == "items":
        for i in sorted(item_ids(argv[1])):
            print(i)
    elif c == "check":
        cmd_check(argv[1:])
    else:
        raise SystemExit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
