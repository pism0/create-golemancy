"""Named colour ramps for every Golemancy texture.

Each ramp has six shades, index 0 = darkest (outline / deep shadow) .. 5 = brightest highlight.
All textures are built from these ramps, so changing a material's look means editing one ramp here
and re-running tools/art/build_all.py. Material variants (stone / clay, later iron / brass) are
made by swapping the ramp a template is painted with, never by redrawing.

Style notes (see KICKOFF_PROMPT.md "Style guide"):
  * Create side: muted, slightly desaturated industrial colours.
  * Iron's side: saturated school colours with a bright core for glow.
"""


def _hex(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def ramp(*hexes):
    assert len(hexes) == 6, "ramps have exactly six shades"
    return [_hex(h) for h in hexes]


TRANSPARENT = (0, 0, 0, 0)

# --- Body materials -------------------------------------------------------------------------
# Stone: cool grey cut stone, no moss, faint blue cast in the shadows.
STONE = ramp("#2b2e36", "#4a4f5a", "#6c727d", "#8d939c", "#aeb3b9", "#d0d3d6")
# Clay: warm terracotta, smoothed by hand; shadows lean red-brown.
CLAY = ramp("#4a2318", "#73392a", "#9a5238", "#b96d49", "#d18a60", "#e8b088")

# --- Create-side metals and woods -------------------------------------------------------------
BRASS = ramp("#4a3312", "#7a5a1e", "#a8842f", "#c9a544", "#e3c566", "#f6e59a")
COPPER = ramp("#4f2414", "#84412a", "#b3603b", "#d48356", "#eba77a", "#f9d0a8")
ANDESITE = ramp("#2f3230", "#4d524e", "#6e746f", "#8c938d", "#a9afa9", "#c7ccc6")
WOOD = ramp("#2a1a10", "#48301c", "#65452a", "#82603c", "#a07c52", "#bd9a6e")
IRON = ramp("#23262b", "#3f444c", "#5f666f", "#868d95", "#afb5ba", "#dfe2e4")
# Dark recesses: sockets, eye slits, the inside of a housing.
VOID = ramp("#0d0c12", "#17151f", "#211e2c", "#2c2839", "#383347", "#454055")

# --- Magic (Iron's side): saturated, bright centre -----------------------------------------
ARCANE = ramp("#2a1552", "#4b2a8f", "#7446c9", "#9b72ea", "#c3a6fa", "#efe4ff")
FIRE = ramp("#5a1206", "#9c2a0a", "#dc5411", "#f98a1e", "#ffc247", "#fff3b0")
FROST = ramp("#103a5c", "#1f6c94", "#3fa4c8", "#78d2e8", "#b5eef7", "#f0fdff")
# Neutral magic residue used on Remains and inert housings.
ASH = ramp("#1c1a1c", "#343034", "#4e494d", "#6b6468", "#8b8387", "#aea6a9")
PARCHMENT = ramp("#4b3a22", "#7a6238", "#a88c55", "#cdb37a", "#e6d3a2", "#f7ecc9")

MATERIALS = {
    "stone": STONE,
    "clay": CLAY,
}

CORES = {
    "arcane": ARCANE,
    "fire": FIRE,
    "frost": FROST,
}

# Flat RGB used by the game for tints and UI accents (kept here so data and art agree).
MATERIAL_TINT = {
    "stone": 0x8D939C,
    "clay": 0xB96D49,
}
CORE_TINT = {
    "arcane": 0x9B72EA,
    "fire": 0xF98A1E,
    "frost": 0x78D2E8,
}
