"""Static check: every translation key the Java code uses literally exists in en_us.json.

Catches typos that would otherwise only show up as raw keys in-game (the client cannot be run on
the build machine). Keys built at runtime (e.g. "golemancy.profile." + name) are checked by prefix.

    python tools/check_lang.py
"""
import json
import os
import re
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
LANG = os.path.join(ROOT, "src", "generated", "resources", "assets", "golemancy", "lang", "en_us.json")
SRC = os.path.join(ROOT, "src", "main", "java")

KEY = re.compile(r'"((?:golemancy|item\.golemancy|block\.golemancy|entity\.golemancy|itemGroup\.golemancy)\.[a-z0-9_.]+)"\s*(\+)?')


def main():
    with open(LANG, encoding="utf-8") as fh:
        lang = json.load(fh)
    missing = []
    checked = 0
    for root, _dirs, files in os.walk(SRC):
        if os.sep + "datagen" in root or os.sep + "dev" in root:
            continue  # the generators define keys, they do not consume them
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(root, name)
            text = open(path, encoding="utf-8").read()
            for match in KEY.finditer(text):
                key, concatenated = match.group(1), match.group(2)
                checked += 1
                if concatenated or key.endswith("."):
                    prefix = key if key.endswith(".") else key + "."
                    if not any(k.startswith(prefix) or k.startswith(key) for k in lang):
                        missing.append((os.path.relpath(path, ROOT), key + "*"))
                elif key not in lang:
                    missing.append((os.path.relpath(path, ROOT), key))
    for path, key in sorted(set(missing)):
        print(f"MISSING {key}   ({path})")
    print(f"checked {checked} key uses against {len(lang)} translations: {len(set(missing))} missing")
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
