"""GUI art: the golem configuration screen atlas and the mod logo.

The screen follows the feel of Create's panels (a light machined panel with a dark frame and brass
trim) without reusing any of its artwork: everything is drawn here from rectangles and the shared
palettes. Atlas layout (256x256), mirrored by constants in client/GolemScreen.java:

    (0,   0) 196x166  panel
    (0, 170)  28x24   tab, inactive        (30, 170) 28x24  tab, active
    (60, 170) 16x16   icon: overview       (78, 170) 16x16  icon: core      (96, 170) 16x16  icon: behaviour
    (0, 198) 102x10   bar frame
    (0, 210) 100x6    energy fill          (0, 218) 100x6   health fill     (0, 226) 100x6   integrity fill
"""
import os

from PIL import Image, ImageDraw

import items
import palettes as P
from sprite import render, save

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
GUI_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "golemancy", "textures", "gui")
LOGO_PATH = os.path.join(ROOT, "src", "main", "resources", "logo.png")

PANEL_W, PANEL_H = 196, 166
HEALTH = P.ramp("#4a0f14", "#7d1a1f", "#b32a2a", "#d84a3c", "#ee7a5e", "#ffc2a8")


def rect(draw, box, fill):
    x0, y0, x1, y1 = box
    draw.rectangle([x0, y0, x1 - 1, y1 - 1], fill=fill)


def bevel(draw, box, light, dark):
    """A one-pixel bevel: light on the top and left edges, dark on the bottom and right."""
    x0, y0, x1, y1 = box
    rect(draw, (x0, y0, x1, y0 + 1), light)
    rect(draw, (x0, y0, x0 + 1, y1), light)
    rect(draw, (x0, y1 - 1, x1, y1), dark)
    rect(draw, (x1 - 1, y0, x1, y1), dark)


def rivet(px, x, y):
    px[x, y] = P.BRASS[5]
    px[x + 1, y] = P.BRASS[3]
    px[x, y + 1] = P.BRASS[3]
    px[x + 1, y + 1] = P.BRASS[1]


def panel():
    img = Image.new("RGBA", (PANEL_W, PANEL_H), P.TRANSPARENT)
    d = ImageDraw.Draw(img)
    rect(d, (0, 0, PANEL_W, PANEL_H), P.WOOD[0])                       # dark frame
    rect(d, (1, 1, PANEL_W - 1, PANEL_H - 1), P.BRASS[3])              # brass trim
    bevel(d, (1, 1, PANEL_W - 1, PANEL_H - 1), P.BRASS[5], P.BRASS[1])
    rect(d, (3, 3, PANEL_W - 3, PANEL_H - 3), P.ANDESITE[5])           # machined face
    bevel(d, (3, 3, PANEL_W - 3, PANEL_H - 3), (236, 238, 234, 255), P.ANDESITE[3])
    # Header band
    rect(d, (5, 5, PANEL_W - 5, 20), P.ANDESITE[3])
    bevel(d, (5, 5, PANEL_W - 5, 20), P.ANDESITE[4], P.ANDESITE[1])
    # Sunken content area
    rect(d, (6, 23, PANEL_W - 6, PANEL_H - 6), P.ANDESITE[4])
    bevel(d, (6, 23, PANEL_W - 6, PANEL_H - 6), P.ANDESITE[2], (236, 238, 234, 255))
    px = img.load()
    for x, y in ((7, 7), (PANEL_W - 9, 7), (7, 16), (PANEL_W - 9, 16)):
        rivet(px, x, y)
    # Rounded outer corners
    for x, y in ((0, 0), (PANEL_W - 1, 0), (0, PANEL_H - 1), (PANEL_W - 1, PANEL_H - 1)):
        px[x, y] = P.TRANSPARENT
    return img


def tab(active):
    img = Image.new("RGBA", (28, 24), P.TRANSPARENT)
    d = ImageDraw.Draw(img)
    top = 0 if active else 3
    rect(d, (0, top, 28, 24), P.WOOD[0])
    rect(d, (1, top + 1, 27, 24), P.BRASS[3] if active else P.BRASS[2])
    rect(d, (2, top + 2, 26, 24), P.ANDESITE[5] if active else P.ANDESITE[3])
    bevel(d, (2, top + 2, 26, 25), (236, 238, 234, 255) if active else P.ANDESITE[4],
          P.ANDESITE[3] if active else P.ANDESITE[2])
    px = img.load()
    px[0, top] = P.TRANSPARENT
    px[27, top] = P.TRANSPARENT
    return img


def bar_frame():
    img = Image.new("RGBA", (102, 10), P.TRANSPARENT)
    d = ImageDraw.Draw(img)
    rect(d, (0, 0, 102, 10), P.VOID[0])
    rect(d, (1, 1, 101, 9), P.VOID[3])
    bevel(d, (1, 1, 101, 9), P.VOID[1], P.VOID[5])
    return img


def bar_fill(ramp):
    img = Image.new("RGBA", (100, 6), P.TRANSPARENT)
    d = ImageDraw.Draw(img)
    for y, shade in enumerate((5, 4, 3, 3, 2, 1)):
        rect(d, (0, y, 100, y + 1), ramp[shade])
    # A notch every 10 px so the bar reads as segments.
    for x in range(9, 100, 10):
        rect(d, (x, 0, x + 1, 6), ramp[1])
    return img


def atlas():
    img = Image.new("RGBA", (256, 256), P.TRANSPARENT)
    img.alpha_composite(panel(), (0, 0))
    img.alpha_composite(tab(False), (0, 170))
    img.alpha_composite(tab(True), (30, 170))
    stone = items.legend(material=P.STONE, glow=P.ARCANE)
    img.alpha_composite(render(items.HEAD, stone, name="icon_overview"), (60, 170))
    img.alpha_composite(render(items.overlay(items.CORE, items.GLYPHS["arcane"], 5, 5), stone, name="icon_core"), (78, 170))
    img.alpha_composite(render(items.CONTROL_WAND, stone, name="icon_behaviour"), (96, 170))
    img.alpha_composite(bar_frame(), (0, 198))
    img.alpha_composite(bar_fill(P.ARCANE), (0, 210))
    img.alpha_composite(bar_fill(HEALTH), (0, 218))
    img.alpha_composite(bar_fill(P.BRASS), (0, 226))
    return img


def logo():
    """128x128 mod icon: a stone construct head over an arcane core, in a brass frame."""
    img = Image.new("RGBA", (128, 128), P.TRANSPARENT)
    d = ImageDraw.Draw(img)
    rect(d, (4, 4, 124, 124), P.WOOD[0])
    rect(d, (8, 8, 120, 120), P.BRASS[3])
    bevel(d, (8, 8, 120, 120), P.BRASS[5], P.BRASS[1])
    rect(d, (12, 12, 116, 116), P.VOID[2])
    bevel(d, (12, 12, 116, 116), P.VOID[0], P.VOID[4])
    for x in (16, 108):
        for y in (16, 108):
            rect(d, (x, y, x + 4, y + 4), P.BRASS[4])
    legend = items.legend(material=P.STONE, glow=P.ARCANE)
    head = render(items.HEAD, legend, name="logo_head")
    core = render(items.overlay(items.CORE, items.GLYPHS["arcane"], 5, 5), legend, name="logo_core")
    img.alpha_composite(head.resize((96, 96), Image.NEAREST), (16, 6))
    img.alpha_composite(core.resize((48, 48), Image.NEAREST), (40, 68))
    return img


def write_all():
    import contact_sheet

    a = atlas()
    save(a, os.path.join(GUI_DIR, "golem_screen.png"))
    lg = logo()
    save(lg, LOGO_PATH)
    contact_sheet.make_sheet("Golemancy GUI: configuration screen atlas and mod logo",
                             [("golem_screen (atlas)", a), ("logo", lg)], "gui.png", scale=2, columns=2)
    return 2


if __name__ == "__main__":
    print("wrote", write_all(), "gui files")
