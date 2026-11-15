"""A tiny pixel-art engine: sprites are drawn as character grids and painted from palette ramps.

A sprite is a list of equal-length strings. Each character is looked up in a legend:

    '.'                      transparent
    char -> ramp             an auto-shaded region painted from that ramp
    char -> (ramp, shade)    a pixel with a fixed shade (0 = darkest .. 5 = brightest)

Auto-shading gives every region the same lighting, so all sprites agree with each other:

  * light comes from the top left;
  * a pixel whose upper or left neighbour is outside its region (empty, or another ramp) is lit;
  * a pixel whose lower or right neighbour is outside its region is in shadow;
  * a dark outline, one pixel wide, is grown around the whole silhouette (items only).

Regions are compared by ramp, so a brass band across a stone body gets its own bevel. No dithering
and no randomness: the same grid always produces the same PNG.
"""
from PIL import Image

from palettes import TRANSPARENT

BASE, LIT, SHADOW, BRIGHT, DEEP = 3, 4, 2, 5, 1


def _cell(grid, x, y):
    if y < 0 or y >= len(grid) or x < 0 or x >= len(grid[y]):
        return "."
    return grid[y][x]


def _ramp_of(legend, ch):
    """Returns (ramp, fixed_shade_or_None) for a legend character, or (None, None) if empty."""
    if ch == ".":
        return None, None
    try:
        entry = legend[ch]
    except KeyError:
        raise KeyError(f"character {ch!r} is not in the sprite legend") from None
    if isinstance(entry, tuple):
        return entry[0], entry[1]
    return entry, None


def check_grid(grid, width=None, height=None, name="sprite"):
    height = height or len(grid)
    width = width or len(grid[0])
    if len(grid) != height:
        raise ValueError(f"{name}: expected {height} rows, got {len(grid)}")
    for i, row in enumerate(grid):
        if len(row) != width:
            raise ValueError(f"{name}: row {i} is {len(row)} wide, expected {width}: {row!r}")


def shade_at(grid, legend, x, y, inlay=""):
    """Shade index for the auto-shaded pixel at (x, y).

    Characters listed in `inlay` are flat details set into a surface (runes, seams of another
    colour): they do not count as a region boundary, so the surface around them is not bevelled.
    """
    ramp, fixed = _ramp_of(legend, _cell(grid, x, y))
    if fixed is not None:
        return fixed

    def outside(dx, dy):
        ch = _cell(grid, x + dx, y + dy)
        if ch in inlay and ch != ".":
            return False
        other, _ = _ramp_of(legend, ch)
        return other is not ramp

    up, left, down, right = outside(0, -1), outside(-1, 0), outside(0, 1), outside(1, 0)
    lit = up or left
    shadow = down or right
    if lit and shadow:
        # Thin features (1 px wide): take the brighter side when the top is exposed.
        return BASE if not (up and left) else LIT
    if up and left:
        return BRIGHT
    if lit:
        return LIT
    if down and right:
        return DEEP
    if shadow:
        return SHADOW
    return BASE


def render(grid, legend, outline=True, name="sprite", inlay=""):
    """Paints a character grid into an RGBA image of the same size."""
    check_grid(grid, name=name)
    h, w = len(grid), len(grid[0])
    img = Image.new("RGBA", (w, h), TRANSPARENT)
    px = img.load()

    for y in range(h):
        for x in range(w):
            ramp, _ = _ramp_of(legend, grid[y][x])
            if ramp is None:
                continue
            px[x, y] = ramp[shade_at(grid, legend, x, y, inlay)]

    if outline:
        # Grow a dark outline into the empty pixels around the silhouette. The colour is the
        # darkest shade of the neighbouring region (checked in a fixed order for determinism).
        for y in range(h):
            for x in range(w):
                if grid[y][x] != ".":
                    continue
                for dx, dy in ((0, 1), (1, 0), (0, -1), (-1, 0)):
                    ramp, _ = _ramp_of(legend, _cell(grid, x + dx, y + dy))
                    if ramp is not None:
                        px[x, y] = ramp[0]
                        break
    return img


def has_outline_room(grid):
    """True when no filled pixel touches the image border (so the grown outline is never clipped)."""
    h, w = len(grid), len(grid[0])
    for y in range(h):
        for x in range(w):
            if grid[y][x] != "." and (x in (0, w - 1) or y in (0, h - 1)):
                return False
    return True


def save(img, path):
    import os
    os.makedirs(os.path.dirname(path), exist_ok=True)
    # No metadata, fixed compression: byte-identical output on every run.
    img.save(path, format="PNG", optimize=False, compress_level=9)
