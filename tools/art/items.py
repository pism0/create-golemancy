"""Item textures (16x16). Every sprite is a character grid painted by sprite.render().

Shared legend (see `legend()`):
    M  body material, auto-shaded        m  material seam (dark)       d  material side face
    l  material highlight (rivet)        B  brass, auto-shaded         r  brass rivet highlight
    C  copper, auto-shaded               A  andesite, auto-shaded      W  wood, auto-shaded
    K  dark recess (socket, eye)         1-5  glow ramp, fixed shade   G  glow, auto-shaded

Material variants are produced by painting the same grid with a different `M` ramp, and core
variants by a different glow ramp plus a small glyph overlay.
"""
import os

import palettes as P
from sprite import render, save

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
ITEM_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "golemancy", "textures", "item")

# Neutral light ramp for layers that the game tints at runtime (Dormant Golem, Remains).
TINTABLE = P.ramp("#4a4a4a", "#787878", "#a2a2a2", "#c8c8c8", "#e6e6e6", "#ffffff")


def legend(material=P.STONE, glow=P.ARCANE):
    return {
        "M": material, "m": (material, 1), "d": (material, 2), "l": (material, 5),
        "B": P.BRASS, "b": (P.BRASS, 1), "r": (P.BRASS, 5),
        "C": P.COPPER, "A": P.ANDESITE, "W": P.WOOD, "K": P.VOID,
        "G": glow,
        "1": (glow, 1), "2": (glow, 2), "3": (glow, 3), "4": (glow, 4), "5": (glow, 5),
    }


def overlay(grid, patch, x0, y0):
    """Returns a copy of `grid` with `patch` stamped at (x0, y0); '.' in the patch keeps the grid."""
    rows = [list(r) for r in grid]
    for dy, prow in enumerate(patch):
        for dx, ch in enumerate(prow):
            if ch != ".":
                rows[y0 + dy][x0 + dx] = ch
    return ["".join(r) for r in rows]


# ---------------------------------------------------------------------------------------------
# Parts (painted once per material)
# ---------------------------------------------------------------------------------------------
PLATE = [
    "................",
    "................",
    "..MMMMMMMMMMMM..",
    "..MlMMMMMMMMlM..",
    "..MMmMMMMMMMMm..",
    "..MMMMMMMMMMMM..",
    "..MMMMMMMMMMMM..",
    "..MMMMMMMMMMMM..",
    "..MlMMMMMMMMlM..",
    "..MMmMMMMMMMMm..",
    "..MMMMMMMMMMMM..",
    "..dddddddddddd..",
    "..mmmmmmmmmmmm..",
    "................",
    "................",
    "................",
]

LIMB = [
    "................",
    "..........MMM...",
    ".........MMMMM..",
    "........MMMMMM..",
    ".......MMMMMM...",
    "......MMMMMM....",
    ".....AAMMMM.....",
    "....AAAAMM......",
    "...MMAAAA.......",
    "..MMMMAA........",
    ".MMMMMM.........",
    ".MMMMMM.........",
    ".MMMMM..........",
    "..MMM...........",
    "................",
    "................",
]

HEAD = [
    "................",
    "................",
    "...MMMMMMMMMM...",
    "..MMMMMMMMMMMM..",
    "..MMMMMMMMMMMM..",
    "..MmmmmMMmmmmM..",
    "..MKKKKMMKKKKM..",
    "..MKKKKMMKKKKM..",
    "..MMMMMMMMMMMM..",
    "..MMMMMMMMMMMM..",
    "..MMMmmmmmmMMM..",
    "...MMMMMMMMMM...",
    "....MMMMMMMM....",
    "................",
    "................",
    "................",
]

TORSO_SHELL = [
    "................",
    "..MMMM....MMMM..",
    ".MMMMMMMMMMMMMM.",
    ".MMMMMMMMMMMMMM.",
    ".MMMMMBBBBMMMMM.",
    "..MMMBBKKBBMMM..",
    "..MMMBKKKKBMMM..",
    "..MMMBKKKKBMMM..",
    "..MMMBBKKBBMMM..",
    "..MMMMBBBBMMMM..",
    "...MMMMMMMMMM...",
    "...MMmMMMMmMM...",
    "...MMMMMMMMMM...",
    "....MMMMMMMM....",
    "................",
    "................",
]

PARTS = {"plate": PLATE, "limb": LIMB, "head": HEAD, "torso_shell": TORSO_SHELL}

# ---------------------------------------------------------------------------------------------
# Core housing and intermediates
# ---------------------------------------------------------------------------------------------
CORE_HOUSING = [
    "................",
    "................",
    ".....CCCCCC.....",
    "....CCCCCCCC....",
    "...CCCKKKKCCC...",
    "..CCCKKKKKKCCC..",
    "..CCKKKKKKKKCC..",
    "..BrBBBBBBBBrB..",
    "..BBBBBBBBBBBB..",
    "..CCKKKKKKKKCC..",
    "..CCCKKKKKKCCC..",
    "...CCCKKKKCCC...",
    "....CCCCCCCC....",
    ".....CCCCCC.....",
    "................",
    "................",
]

INCOMPLETE_CORE_HOUSING = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "..CC........CC..",
    "..CCC......CCC..",
    "..BrBBBBBBBBrB..",
    "..BBBBBBBBBBBB..",
    "..CCKKKKKKKKCC..",
    "..CCCKKKKKKCCC..",
    "...CCCKKKKCCC...",
    "....CCCCCCCC....",
    ".....CCCCCC.....",
    "................",
    "................",
]

# The two windows of the housing filled with a dull liquid (shades 1-3 only: inert, not glowing).
_WINDOW_TOP = ["1221", "122221", "12233221"]
_WINDOW_BOTTOM = ["12233221", "122221", "1221"]


def filled_housing():
    g = CORE_HOUSING
    g = overlay(g, [_WINDOW_TOP[0]], 6, 4)
    g = overlay(g, [_WINDOW_TOP[1]], 5, 5)
    g = overlay(g, [_WINDOW_TOP[2]], 4, 6)
    g = overlay(g, [_WINDOW_BOTTOM[0]], 4, 9)
    g = overlay(g, [_WINDOW_BOTTOM[1]], 5, 10)
    g = overlay(g, [_WINDOW_BOTTOM[2]], 6, 11)
    return g


# ---------------------------------------------------------------------------------------------
# Cores: a glowing orb held in a brass ring, with a school glyph
# ---------------------------------------------------------------------------------------------
CORE = [
    "................",
    "......BBBB......",
    "....BB2222BB....",
    "...B22333322B...",
    "..B2233333322B..",
    "..B2333333332B..",
    ".B223333333322B.",
    ".B233333333332B.",
    ".B233333333332B.",
    ".B223333333322B.",
    "..B2333333332B..",
    "..B2233333322B..",
    "...B22333322B...",
    "....BB2222BB....",
    "......BBBB......",
    "................",
]

GLYPHS = {
    # Arcane: an open diamond.
    "arcane": ["..55..", ".5555.", "55..55", "55..55", ".5555.", "..55.."],
    # Fire: a flame.
    "fire": ["..5...", "..55..", ".555..", ".5555.", "555555", ".5555."],
    # Frost: a six-armed flake.
    "frost": ["5.55.5", ".5555.", "55..55", "55..55", ".5555.", "5.55.5"],
}

# ---------------------------------------------------------------------------------------------
# Frames: a small assembled body with an empty core socket
# ---------------------------------------------------------------------------------------------
FRAME_GUARDIAN = [
    "................",
    ".....MMMMMM.....",
    ".....MKMMKM.....",
    "......MMMM......",
    "...MMMMMMMMMM...",
    "..MMMMMBBMMMMM..",
    "..MM.MBKKBM.MM..",
    ".MMMMMBKKBM.MM..",
    ".MMMM.MBBMM.MM..",
    ".MMMM.MMMMM.MM..",
    ".MMMM..MMM......",
    "..MM..MM.MM.....",
    "......MM.MM.....",
    "......MM.MM.....",
    ".....MMM.MMM....",
    "................",
]

FRAME_BRUTE = [
    "................",
    "................",
    "................",
    ".....MMMMMM.....",
    ".....MKMMKM.....",
    "..MMMMMMMMMMMM..",
    ".MMMMMMBBMMMMMM.",
    ".MMMMMBKKBMMMMM.",
    ".MMM.MBKKBM.MMM.",
    ".MMM.MMBBMM.MMM.",
    ".MMM.MMMMMM.MMM.",
    ".MMMM.MMMM.MMMM.",
    ".MMMM.MMMM.MMMM.",
    "..MM..MM.MM..MM.",
    "......MM.MM.....",
    "................",
]

INCOMPLETE_GUARDIAN = [
    "................",
    "................",
    "................",
    "................",
    "....MMMMMMMM....",
    "....MMMBBMMM....",
    "....MMBKKBMM....",
    "....MMBKKBMM....",
    "....MMMBBMMM....",
    ".....MMMMMM.....",
    "......MMMM......",
    "......MM.MM.....",
    "......MM.MM.....",
    "......MM.MM.....",
    ".....MMM.MMM....",
    "................",
]

INCOMPLETE_BRUTE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...MMMMMMMMMM...",
    "...MMMMBBMMMM...",
    "...MMMBKKBMMM...",
    "...MMMBKKBMMM...",
    "...MMMMBBMMMM...",
    "....MMMMMMMM....",
    ".....MMMMMM.....",
    ".....MMMMMM.....",
    "......MM.MM.....",
    "......MM.MM.....",
    "................",
]

FRAMES = {"guardian": FRAME_GUARDIAN, "brute": FRAME_BRUTE}
INCOMPLETE_FRAMES = {"guardian": INCOMPLETE_GUARDIAN, "brute": INCOMPLETE_BRUTE}

# ---------------------------------------------------------------------------------------------
# Lifecycle items. Dormant Golem and Remains are layered: layer 0 is tinted with the material
# colour and layer 1 with the core colour at runtime, layer 2 is drawn as is.
# ---------------------------------------------------------------------------------------------
DORMANT_BODY = [
    "................",
    "......MMMM......",
    ".....MMMMMM.....",
    ".....MKMMKM.....",
    "......MMMM......",
    "....MMMMMMMM....",
    "...MMMM..MMMM...",
    "...MMMM..MMMM...",
    "...MM.MMMM.MM...",
    "...MM.MMMM.MM...",
    "......MM.MM.....",
    "......MM.MM.....",
    "................",
    "................",
    "................",
    "................",
]

DORMANT_CORE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......GG.......",
    ".......GG.......",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]

DORMANT_BASE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "...BBBBBBBBBB...",
    "..BrBBBBBBBBrB..",
    "..BBBBBBBBBBBB..",
    "................",
]

REMAINS_BODY = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "........MM......",
    "...MMM.MMMM.....",
    "..MMKMM.MMM.M...",
    "..MMMMM..M.MMM..",
    ".MMMMMMM...MMM..",
    ".MMMmMMMM..MMMM.",
    ".MMMMMmMMMMMMMM.",
    "..MMMMMMMMMMMM..",
    "................",
    "................",
    "................",
]

REMAINS_CORE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".........GG.....",
    ".........GG.....",
    "................",
    "................",
    "................",
    "................",
    "................",
]

CONTROL_WAND = [
    "................",
    "...........44...",
    "..........4554..",
    "..........4554..",
    ".........BB44...",
    "........BBB.....",
    ".......BBB......",
    "......BBB.......",
    ".....BBB........",
    "....WWB.........",
    "...WWW..........",
    "..WWW...........",
    ".WWW............",
    ".WW.............",
    "................",
    "................",
]


def build():
    """Returns {relative texture name: Image} for every item texture."""
    out = {}

    for mat_name, ramp in P.MATERIALS.items():
        leg = legend(material=ramp)
        for kind, grid in PARTS.items():
            out[f"{mat_name}_{kind}"] = render(grid, leg, name=f"{mat_name}_{kind}")
        for frame, grid in FRAMES.items():
            out[f"frame/{mat_name}_{frame}"] = render(grid, leg, name=f"frame/{mat_name}_{frame}")
        for frame, grid in INCOMPLETE_FRAMES.items():
            out[f"incomplete_frame/{mat_name}_{frame}"] = render(grid, leg, name=f"incomplete_frame/{mat_name}_{frame}")

    out["core_housing"] = render(CORE_HOUSING, legend(), name="core_housing")
    out["incomplete_core_housing"] = render(INCOMPLETE_CORE_HOUSING, legend(), name="incomplete_core_housing")
    out["quenched_core_housing"] = render(filled_housing(), legend(glow=P.FROST), name="quenched_core_housing")
    out["molten_core_housing"] = render(filled_housing(), legend(glow=P.FIRE), name="molten_core_housing")

    for core, ramp in P.CORES.items():
        grid = overlay(CORE, GLYPHS[core], 5, 5)
        out[f"core/{core}"] = render(grid, legend(glow=ramp), name=f"core/{core}")

    tint = legend(material=TINTABLE, glow=TINTABLE)
    out["dormant_golem_body"] = render(DORMANT_BODY, tint, name="dormant_golem_body")
    out["dormant_golem_core"] = render(DORMANT_CORE, tint, name="dormant_golem_core")
    out["dormant_golem_base"] = render(DORMANT_BASE, legend(), name="dormant_golem_base")
    out["construct_remains_body"] = render(REMAINS_BODY, tint, name="construct_remains_body")
    out["construct_remains_core"] = render(REMAINS_CORE, tint, name="construct_remains_core")
    out["control_wand"] = render(CONTROL_WAND, legend(glow=P.ARCANE), name="control_wand")
    return out


def write_all():
    images = build()
    for name, img in images.items():
        save(img, os.path.join(ITEM_DIR, *name.split("/")) + ".png")
    return images


if __name__ == "__main__":
    print(f"wrote {len(write_all())} item textures to {ITEM_DIR}")
