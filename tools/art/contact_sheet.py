"""Contact sheets: each texture at 1x (on a light and a dark slot) and enlarged (nearest-neighbour).

Written to art/previews/. They are review aids only and are not shipped in the mod jar.
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
PREVIEW_DIR = os.path.join(ROOT, "art", "previews")

SHEET_BG = (38, 40, 46, 255)
SLOT_LIGHT = (139, 139, 139, 255)   # vanilla inventory slot grey
SLOT_DARK = (24, 24, 28, 255)
BIG_BG = (58, 61, 70, 255)
TEXT = (225, 225, 230, 255)


def _composite(bg_color, img, scale=1):
    big = img.resize((img.width * scale, img.height * scale), Image.NEAREST) if scale != 1 else img
    bg = Image.new("RGBA", big.size, bg_color)
    bg.alpha_composite(big)
    return bg


def make_sheet(title, entries, filename, scale=8, columns=6, extra=None):
    """entries: list of (label, Image). extra: optional {label: Image} drawn beside the big view
    (used for entity UV layouts)."""
    extra = extra or {}
    max_w = max(img.width for _, img in entries)
    max_h = max(img.height for _, img in entries)
    extra_w = max((e.width for e in extra.values()), default=0)
    pad = 8
    cell_w = max(max_w * scale + max_w * 2 + extra_w + pad * 4 + (pad if extra_w else 0), 150)
    cell_h = max(max_h * scale, max((e.height for e in extra.values()), default=0)) + 14 + pad * 2
    columns = min(columns, len(entries))
    rows = (len(entries) + columns - 1) // columns

    sheet = Image.new("RGBA", (columns * cell_w + pad, rows * cell_h + 26 + pad), SHEET_BG)
    draw = ImageDraw.Draw(sheet)
    draw.text((pad, 8), title, fill=TEXT)

    for i, (label, img) in enumerate(entries):
        cx = pad + (i % columns) * cell_w
        cy = 26 + (i // columns) * cell_h
        draw.text((cx, cy), label, fill=TEXT)
        y = cy + 14
        # 1x on light and dark slots
        sheet.alpha_composite(_composite(SLOT_LIGHT, img), (cx, y))
        sheet.alpha_composite(_composite(SLOT_DARK, img), (cx, y + img.height + 4))
        # enlarged
        bx = cx + img.width + pad
        sheet.alpha_composite(_composite(BIG_BG, img, scale), (bx, y))
        if label in extra:
            sheet.alpha_composite(extra[label], (bx + img.width * scale + pad, y))

    os.makedirs(PREVIEW_DIR, exist_ok=True)
    path = os.path.join(PREVIEW_DIR, filename)
    sheet.convert("RGB").save(path, format="PNG")
    return path
