"""Block textures (16x16) for the Binding Altar.

The altar reads as a Create machine with a magical top: an andesite-and-wood casing with brass
trim (shaft enters from below), a stone plinth, and a stone dish inlaid with an arcane rune circle
that lights up while a binding is in progress.

`altar_side` holds the whole side profile; the block model maps three strips of it:
    rows 0-2   dish rim      rows 3-5   plinth (inset 1 px)      rows 6-15  casing
"""
import os

from PIL import Image

import palettes as P
from sprite import render, save

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
BLOCK_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "golemancy", "textures", "block")


def legend(rune_shade, eye_ramp_shade):
    return {
        "M": P.STONE, "m": (P.STONE, 1),
        "A": P.ANDESITE, "i": (P.ANDESITE, 5), "j": (P.ANDESITE, 1),
        "B": P.BRASS, "r": (P.BRASS, 5), "b": (P.BRASS, 1),
        "W": P.WOOD, "w": (P.WOOD, 1),
        "K": P.VOID,
        "2": (P.ARCANE, rune_shade),
        "3": eye_ramp_shade,
    }


SIDE = [
    "BBBBBBBBBBBBBBBB",
    "MMMMMMMMMMMMMMMM",
    "MMMMMMMMMMMMMMMM",
    "KMMMMMMMMMMMMMMK",
    "KMMmMMMmMMmMMMMK",
    "KMMMMMMMMMMMMMMK",
    "BBBBBBBBBBBBBBBB",
    "AAAAAAAAAAAAAAAA",
    "AiAAAAAAAAAAAAiA",
    "AAjAWWWWWWWWAAAj",
    "AAAAWWWWWWWWAAAA",
    "AAAAWWwWWwWWAAAA",
    "AAAAWWWWWWWWAAAA",
    "AiAAWWWWWWWWAAiA",
    "AAjAAAAAAAAAAAAj",
    "AAAAAAAAAAAAAAAA",
]

TOP = [
    "MMMMMMMMMMMMMMMM",
    "MBBBBBBBBBBBBBBM",
    "MBMMMMMMMMMMMMBM",
    "MBMMMM2222MMMMBM",
    "MBMM22MMMM22MMBM",
    "MBMM2MM22MM2MMBM",
    "MBM2MM2MM2MM2MBM",
    "MBM2M2M33M2M2MBM",
    "MBM2M2M33M2M2MBM",
    "MBM2MM2MM2MM2MBM",
    "MBMM2MM22MM2MMBM",
    "MBMM22MMMM22MMBM",
    "MBMMMM2222MMMMBM",
    "MBMMMMMMMMMMMMBM",
    "MBBBBBBBBBBBBBBM",
    "MMMMMMMMMMMMMMMM",
]

CASING_TOP = [
    "BBBBBBBBBBBBBBBB",
    "BAAAAAAAAAAAAAAB",
    "BAiAAAAAAAAAAiAB",
    "BAAjAAAAAAAAAAjB",
    "BAAAAAAAAAAAAAAB",
    "BAAAAAAAAAAAAAAB",
    "BAAAAAAAAAAAAAAB",
    "BAAAAAAAAAAAAAAB",
    "BAAAAAAAAAAAAAAB",
    "BAAAAAAAAAAAAAAB",
    "BAAAAAAAAAAAAAAB",
    "BAAAAAAAAAAAAAAB",
    "BAiAAAAAAAAAAiAB",
    "BAAjAAAAAAAAAAjB",
    "BAAAAAAAAAAAAAAB",
    "BBBBBBBBBBBBBBBB",
]

# The shaft enters through the dark opening in the middle.
BOTTOM = [
    "AAAAAAAAAAAAAAAA",
    "AiAAAAAAAAAAAAiA",
    "AAjAAAAAAAAAAAAj",
    "AAAAAAAAAAAAAAAA",
    "AAAAABBBBBBAAAAA",
    "AAAABBKKKKBBAAAA",
    "AAAABKKKKKKBAAAA",
    "AAAABKKKKKKBAAAA",
    "AAAABKKKKKKBAAAA",
    "AAAABKKKKKKBAAAA",
    "AAAABBKKKKBBAAAA",
    "AAAAABBBBBBAAAAA",
    "AAAAAAAAAAAAAAAA",
    "AiAAAAAAAAAAAAiA",
    "AAjAAAAAAAAAAAAj",
    "AAAAAAAAAAAAAAAA",
]


def build():
    idle = legend(rune_shade=1, eye_ramp_shade=(P.VOID, 2))
    active = legend(rune_shade=3, eye_ramp_shade=(P.ARCANE, 5))
    return {
        "binding_altar_side": render(SIDE, idle, outline=False, name="altar_side"),
        "binding_altar_top": render(TOP, idle, outline=False, name="altar_top", inlay="23"),
        "binding_altar_top_active": render(TOP, active, outline=False, name="altar_top_active", inlay="23"),
        "binding_altar_casing_top": render(CASING_TOP, idle, outline=False, name="altar_casing_top"),
        "binding_altar_bottom": render(BOTTOM, idle, outline=False, name="altar_bottom"),
    }


def elevation(images, top_name):
    """A flat side-plus-top preview of the assembled block, for the contact sheet."""
    side = images["binding_altar_side"].copy()
    px = side.load()
    for y in range(3, 6):  # the plinth is inset by one pixel on each side
        px[0, y] = (0, 0, 0, 0)
        px[15, y] = (0, 0, 0, 0)
    canvas = Image.new("RGBA", (16, 34), (0, 0, 0, 0))
    canvas.alpha_composite(images[top_name], (0, 0))
    canvas.alpha_composite(side, (0, 18))
    return canvas


def write_all():
    import contact_sheet

    images = build()
    for name, img in images.items():
        save(img, os.path.join(BLOCK_DIR, name + ".png"))
    entries = list(images.items())
    entries.append(("altar idle (top / side)", elevation(images, "binding_altar_top")))
    entries.append(("altar working (top / side)", elevation(images, "binding_altar_top_active")))
    contact_sheet.make_sheet("Golemancy blocks: Binding Altar", entries, "blocks.png", columns=4)
    return len(images)


if __name__ == "__main__":
    print("wrote", write_all(), "block textures")
