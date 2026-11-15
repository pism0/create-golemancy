"""Rebuilds every Golemancy texture, model and contact sheet from scratch.

    .venv/Scripts/python tools/art/build_all.py          (Windows)
    .venv/bin/python tools/art/build_all.py              (macOS / Linux)

Deterministic: running it twice produces byte-identical files (checked by `--check`).
"""
import hashlib
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))

import contact_sheet  # noqa: E402
import items  # noqa: E402

ROOT = items.ROOT
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "golemancy")

# Optional stages, added as the milestones that need them land.
OPTIONAL = []
for _name in ("blocks", "entities", "gui", "logo"):
    try:
        OPTIONAL.append(__import__(_name))
    except ModuleNotFoundError:
        pass


def build():
    written = {}
    item_images = items.write_all()
    written["items"] = len(item_images)

    def group(prefix_filter):
        return [(n, img) for n, img in item_images.items() if prefix_filter(n)]

    contact_sheet.make_sheet(
        "Golemancy items: parts", group(lambda n: n.split("_", 1)[0] in ("stone", "clay") and "/" not in n),
        "items_parts.png", columns=4)
    contact_sheet.make_sheet(
        "Golemancy items: housings and cores",
        group(lambda n: "housing" in n or n.startswith("core/")), "items_cores.png", columns=4)
    contact_sheet.make_sheet(
        "Golemancy items: frames", group(lambda n: n.startswith(("frame/", "incomplete_frame/"))),
        "items_frames.png", columns=4)
    contact_sheet.make_sheet(
        "Golemancy items: lifecycle",
        group(lambda n: n.startswith(("dormant", "construct_remains", "control_wand"))) + items_composites(item_images),
        "items_lifecycle.png", columns=4)

    for module in OPTIONAL:
        written[module.__name__] = module.write_all()
    return written


def items_composites(images):
    """Tinted previews of the layered items, as the game draws them."""
    import palettes as P
    from PIL import Image, ImageChops

    def tint(img, rgb):
        solid = Image.new("RGBA", img.size, ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255))
        return ImageChops.multiply(img, solid)

    out = []
    for material, mtint in P.MATERIAL_TINT.items():
        for core, ctint in P.CORE_TINT.items():
            canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            canvas.alpha_composite(tint(images["dormant_golem_body"], mtint))
            canvas.alpha_composite(tint(images["dormant_golem_core"], ctint))
            canvas.alpha_composite(images["dormant_golem_base"])
            out.append((f"dormant {material}+{core}", canvas))
        canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        canvas.alpha_composite(tint(images["construct_remains_body"], mtint))
        canvas.alpha_composite(tint(images["construct_remains_core"], P.CORE_TINT["fire"]))
        out.append((f"remains {material}+fire", canvas))
    return out


def digest():
    h = hashlib.sha256()
    for base in (os.path.join(ASSETS, "textures"), os.path.join(ASSETS, "geo"), os.path.join(ASSETS, "animations")):
        for root, _dirs, files in sorted(os.walk(base)):
            for f in sorted(files):
                path = os.path.join(root, f)
                h.update(os.path.relpath(path, ASSETS).replace("\\", "/").encode())
                with open(path, "rb") as fh:
                    h.update(fh.read())
    return h.hexdigest()


if __name__ == "__main__":
    result = build()
    first = digest()
    print("built:", result)
    if "--check" in sys.argv:
        build()
        second = digest()
        print("reproducible:", first == second, first[:16])
        if first != second:
            sys.exit(1)
