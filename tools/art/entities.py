"""Entity models, textures, glowmasks and animations for the two frames (Guardian, Brute).

Everything is generated from the cube lists below so the model and its texture can never drift
apart:

  * geo/<model>.geo.json                       Bedrock geometry (opens in Blockbench, GeckoLib plugin)
  * animations/<model>.animation.json          Bedrock animations
  * textures/entity/<model>_<material>.png     one per material, same UV layout (palette swap)
  * textures/entity/<model>_core_glowmask.png  greyscale emissive mask, tinted with the core colour

Conventions: units are pixels (16 = one block, the same texel density as blocks), the model faces
north (-Z), +X is the construct's left. Box UV. Bone names are stable API for animations and for
the renderer (optional component bones start with `opt_` and are hidden by default, SDD 10.4).
"""
import json
import os

from PIL import Image, ImageDraw

import palettes as P
from sprite import save

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "golemancy")

# How each material's surface is worked. Same template, same UVs, different finish.
SURFACE = {"stone": "cut", "clay": "smooth"}


# ---------------------------------------------------------------------------------------------
# Model definitions
# ---------------------------------------------------------------------------------------------
def cube(origin, size, role="plain"):
    return {"origin": list(origin), "size": list(size), "role": role}


def bone(name, parent, pivot, cubes=()):
    return {"name": name, "parent": parent, "pivot": list(pivot), "cubes": list(cubes)}


GUARDIAN = {
    "name": "guardian",
    "bounds": (3.5, 3.0, 1.2),
    "bones": [
        bone("root", None, (0, 0, 0)),
        bone("body", "root", (0, 13, 0), [
            cube((-6, 11, -3), (12, 4, 6), "hips"),
            cube((-8, 15, -4), (16, 12, 8), "chest"),
        ]),
        bone("core", "body", (0, 21, -4), [
            cube((-3, 18, -5), (6, 6, 1), "core_ring"),
        ]),
        bone("head", "body", (0, 27, 0), [
            cube((-4, 27, -4), (8, 7, 8), "head"),
            cube((-5, 31, -5), (10, 2, 2), "brow"),
        ]),
        bone("arm_left", "body", (10, 25, 0), [
            cube((8, 23, -4), (6, 6, 8), "pauldron"),
            cube((9, 15, -3), (5, 8, 6), "plain"),
            cube((9, 13, -3), (5, 2, 6), "joint"),
            cube((8, 5, -4), (7, 8, 8), "fist"),
            cube((6, 3, -6), (11, 13, 2), "shield"),
        ]),
        bone("arm_right", "body", (-10, 25, 0), [
            cube((-14, 23, -4), (6, 6, 8), "pauldron"),
            cube((-14, 15, -3), (5, 8, 6), "plain"),
            cube((-14, 13, -3), (5, 2, 6), "joint"),
            cube((-15, 5, -4), (7, 8, 8), "fist"),
        ]),
        bone("leg_left", "root", (3.5, 11, 0), [
            cube((1, 3, -3), (5, 8, 6), "leg"),
            cube((0, 0, -5), (7, 3, 8), "foot"),
        ]),
        bone("leg_right", "root", (-3.5, 11, 0), [
            cube((-6, 3, -3), (5, 8, 6), "leg"),
            cube((-7, 0, -5), (7, 3, 8), "foot"),
        ]),
        # Optional components (hidden in v0.1; see SDD 10.4 and docs/DECISIONS.md D-006).
        bone("opt_shoulder_plates", "body", (0, 29, 0), [
            cube((8, 29, -5), (7, 1, 10), "brass"),
            cube((-15, 29, -5), (7, 1, 10), "brass"),
        ]),
        bone("opt_back_banner", "body", (0, 27, 4), [
            cube((-1, 15, 4), (2, 22, 1), "wood"),
            cube((-4, 25, 5), (8, 12, 1), "cloth"),
        ]),
        bone("opt_head_crest", "head", (0, 34, 0), [
            cube((-1, 34, -4), (2, 3, 8), "brass"),
        ]),
        bone("opt_weapon", "arm_right", (-11, 6, 0), [
            cube((-12, 6, -13), (2, 2, 9), "wood"),
            cube((-13, 5, -17), (4, 4, 4), "iron"),
        ]),
    ],
}

BRUTE = {
    "name": "brute",
    "bounds": (4.5, 2.5, 1.0),
    "bones": [
        bone("root", None, (0, 0, 0)),
        bone("body", "root", (0, 9, 0), [
            cube((-7, 7, -4), (14, 4, 8), "hips"),
            cube((-11, 11, -6), (22, 12, 12), "chest"),
            cube((-8, 23, -3), (16, 3, 9), "hump"),
        ]),
        bone("core", "body", (0, 17, -6), [
            cube((-3, 14, -7), (6, 6, 1), "core_ring"),
        ]),
        bone("head", "body", (0, 22, -4), [
            cube((-4, 20, -9), (8, 6, 7), "head"),
        ]),
        bone("arm_left", "body", (14, 21, 0), [
            cube((11, 17, -5), (8, 8, 10), "pauldron"),
            cube((12, 10, -4), (7, 7, 8), "plain"),
            cube((12, 8, -4), (7, 2, 8), "joint"),
            cube((11, 0, -5), (9, 8, 10), "fist"),
        ]),
        bone("arm_right", "body", (-14, 21, 0), [
            cube((-19, 17, -5), (8, 8, 10), "pauldron"),
            cube((-19, 10, -4), (7, 7, 8), "plain"),
            cube((-19, 8, -4), (7, 2, 8), "joint"),
            cube((-20, 0, -5), (9, 8, 10), "fist"),
        ]),
        bone("leg_left", "root", (4, 7, 0), [
            cube((1, 0, -4), (6, 7, 8), "leg"),
        ]),
        bone("leg_right", "root", (-4, 7, 0), [
            cube((-7, 0, -4), (6, 7, 8), "leg"),
        ]),
        bone("opt_shoulder_plates", "body", (0, 25, 0), [
            cube((13, 25, -2), (4, 3, 4), "brass"),
            cube((-17, 25, -2), (4, 3, 4), "brass"),
        ]),
        bone("opt_back_banner", "body", (0, 26, 6), [
            cube((-1, 12, 6), (2, 22, 1), "wood"),
            cube((-4, 22, 7), (8, 12, 1), "cloth"),
        ]),
        bone("opt_head_crest", "head", (0, 26, -5), [
            cube((-1, 26, -8), (2, 2, 6), "brass"),
        ]),
        bone("opt_weapon", "arm_right", (-15, 3, 0), [
            cube((-17, 3, -16), (2, 2, 11), "wood"),
            cube((-19, 1, -22), (6, 6, 6), "iron"),
        ]),
    ],
}

MODELS = {"guardian": GUARDIAN, "brute": BRUTE}


# ---------------------------------------------------------------------------------------------
# UV packing (box UV, shelf packer, deterministic)
# ---------------------------------------------------------------------------------------------
def all_cubes(model):
    out = []
    for b in model["bones"]:
        for c in b["cubes"]:
            out.append(c)
    return out


def pack(model):
    """Assigns c['uv'] to every cube and returns the texture size (power of two, square)."""
    cubes = all_cubes(model)
    order = sorted(range(len(cubes)),
                   key=lambda i: (-(cubes[i]["size"][2] + cubes[i]["size"][1]),
                                  -(cubes[i]["size"][0] + cubes[i]["size"][2]), i))
    for size in (32, 64, 128, 256):
        x = y = shelf = 0
        ok = True
        placed = {}
        for i in order:
            w, h, d = cubes[i]["size"]
            bw, bh = 2 * (w + d), d + h
            if bw > size:
                ok = False
                break
            if x + bw > size:
                x = 0
                y += shelf
                shelf = 0
            if y + bh > size:
                ok = False
                break
            placed[i] = (x, y)
            x += bw
            shelf = max(shelf, bh)
        if ok:
            for i, uv in placed.items():
                cubes[i]["uv"] = [uv[0], uv[1]]
            return size
    raise ValueError("model does not fit a 256x256 texture")


def faces(c):
    """Face rectangles (x, y, w, h) in texture space for a packed cube."""
    u, v = c["uv"]
    w, h, d = c["size"]
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),      # front
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),  # back
    }


# ---------------------------------------------------------------------------------------------
# Painting
# ---------------------------------------------------------------------------------------------
FACE_BASE = {"up": 4, "north": 3, "east": 3, "west": 3, "south": 2, "down": 1}


def _clamp(s):
    return max(0, min(5, s))


def paint_face(px, rect, ramp, face, surface="cut", bevel=True):
    """Fills one face with a bevelled slab of `ramp`. Light from the top left, no dithering."""
    x0, y0, w, h = rect
    base = FACE_BASE[face]
    for y in range(h):
        for x in range(w):
            s = base
            if bevel and w >= 3 and h >= 3:
                if y == 0 or x == 0:
                    s = base + 1
                if y == h - 1 or x == w - 1:
                    s = base - 1
                if surface == "smooth" and w >= 6 and h >= 6:
                    # Hand-smoothed clay: a soft, wide roll-off instead of a crisp edge.
                    if (y == 1 or x == 1) and not (y == h - 1 or x == w - 1):
                        s = max(s, base + 1) if (y == 0 or x == 0) else base
                    if (y == h - 2 or x == w - 2) and not (y == 0 or x == 0) and not (y == h - 1 or x == w - 1):
                        s = base - 1 if (y == h - 2 and x == w - 2) else s
            px[x0 + x, y0 + y] = ramp[_clamp(s)]
    if surface == "cut" and w >= 8 and h >= 8:
        # Cut-stone courses: horizontal seams with staggered vertical joints.
        course = 5
        row = 0
        for y in range(course, h - 2, course):
            for x in range(1, w - 1):
                px[x0 + x, y0 + y] = ramp[_clamp(base - 1)]
            row += 1
        row = 0
        for top in range(0, h, course):
            offset = (w // 2) if row % 2 == 0 else (w // 4)
            for x in (offset, offset + w // 2):
                if 1 < x < w - 2:
                    for y in range(top + 1, min(top + course, h - 1)):
                        if (y - 0) % course != 0:
                            px[x0 + x, y0 + y] = ramp[_clamp(base - 1)]
            row += 1
    elif surface == "smooth" and w >= 8 and h >= 8:
        # A couple of shallow thumb marks, placed by rule (not random).
        for cx, cy in ((w // 4, h // 3), (w - w // 3, h - h // 3)):
            px[x0 + cx, y0 + cy] = ramp[_clamp(base - 1)]
            px[x0 + cx + 1, y0 + cy] = ramp[_clamp(base - 1)]
            px[x0 + cx, y0 + cy + 1] = ramp[_clamp(base + 1)]


def band(px, rect, ramp, rows, base=3):
    """Horizontal band (e.g. a brass strap) across a face, with a lit top edge."""
    x0, y0, w, _h = rect
    for i, y in enumerate(rows):
        for x in range(w):
            shade = base + 1 if i == 0 else (base - 1 if i == len(rows) - 1 and len(rows) > 1 else base)
            px[x0 + x, y0 + y] = ramp[_clamp(shade)]


def rivet(px, x, y, ramp=P.BRASS):
    px[x, y] = ramp[5]
    px[x + 1, y + 1] = ramp[1]


def paint_cube(px, glow, c, material, surface):
    """Paints all six faces of a cube according to its role. `glow` is the glowmask pixel access."""
    role = c["role"]
    f = faces(c)
    ramps = {"brass": P.BRASS, "wood": P.WOOD, "iron": P.IRON, "cloth": P.ARCANE, "joint": P.ANDESITE,
             "core_ring": P.BRASS}
    ramp = ramps.get(role, material)
    surf = surface if ramp is material else "plain"

    for face, rect in f.items():
        paint_face(px, rect, ramp, face, surf)

    w, h, d = c["size"]
    if role == "joint":
        for face in ("north", "east", "west", "south"):
            x0, y0, fw, fh = f[face]
            for x in range(1, fw - 1, 2):
                px[x0 + x, y0 + fh // 2] = P.ANDESITE[1]

    elif role == "core_ring":
        # Brass ring around a dark socket; the socket itself glows through the glowmask.
        x0, y0, fw, fh = f["north"]
        for y in range(1, fh - 1):
            for x in range(1, fw - 1):
                px[x0 + x, y0 + y] = P.VOID[2 if (x + y) % 2 == 0 else 1]
                edge = x in (1, fw - 2) or y in (1, fh - 2)
                glow[x0 + x, y0 + y] = (255, 255, 255, 150 if edge else 255)
        for x, y in ((0, 0), (fw - 1, 0), (0, fh - 1), (fw - 1, fh - 1)):
            px[x0 + x, y0 + y] = P.BRASS[1]

    elif role == "head":
        x0, y0, fw, fh = f["north"]
        eye_y = y0 + fh // 2 - 1
        # Brow shadow
        for x in range(1, fw - 1):
            px[x0 + x, eye_y - 1] = material[1]
        # Two eye slits (dark), lit through the glowmask.
        for ex in (x0 + 1, x0 + fw - 3):
            for dx in range(2):
                for dy in range(2):
                    px[ex + dx, eye_y + dy] = P.VOID[1]
                    glow[ex + dx, eye_y + dy] = (255, 255, 255, 255 if dy == 0 else 170)
        # Jaw seam
        for x in range(2, fw - 2):
            px[x0 + x, y0 + fh - 2] = material[1]

    elif role in ("chest", "hump"):
        # Brass strap across the chest with rivets, front and back.
        for face in ("north", "south"):
            x0, y0, fw, fh = f[face]
            if fh >= 8:
                band(px, f[face], P.BRASS, [1, 2])
                for x in range(2, fw - 2, 4):
                    rivet(px, x0 + x, y0 + 1)

    elif role == "hips":
        for face in ("north", "south", "east", "west"):
            band(px, f[face], P.BRASS, [0, 1], base=2)

    elif role == "pauldron":
        for face in ("north", "south", "east", "west"):
            x0, y0, fw, fh = f[face]
            band(px, f[face], P.BRASS, [fh - 2, fh - 1], base=3)
        x0, y0, fw, fh = f["up"]
        rivet(px, x0 + 1, y0 + 1)
        rivet(px, x0 + fw - 3, y0 + fh - 3)

    elif role == "fist":
        # Knuckle seams on the lower part, brass wrist band on top.
        for face in ("north", "south", "east", "west"):
            x0, y0, fw, fh = f[face]
            band(px, f[face], P.BRASS, [0])
            for x in range(2, fw - 1, 3):
                for y in range(fh - 3, fh - 1):
                    px[x0 + x, y0 + y] = material[1]

    elif role == "leg":
        # Andesite knee band.
        for face in ("north", "south", "east", "west"):
            band(px, f[face], P.ANDESITE, [2, 3], base=3)

    elif role == "foot":
        for face in ("north", "south", "east", "west"):
            x0, y0, fw, fh = f[face]
            for x in range(fw):
                px[x0 + x, y0 + fh - 1] = material[0]

    elif role == "shield":
        # Brass-rimmed slab with a central boss: reads as a shield from the front.
        for face in ("north", "south"):
            x0, y0, fw, fh = f[face]
            for x in range(fw):
                for y in range(fh):
                    if x in (0, fw - 1) or y in (0, fh - 1):
                        px[x0 + x, y0 + y] = P.BRASS[4 if (x == 0 or y == 0) else 2]
            cx, cy = x0 + fw // 2, y0 + fh // 2
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    px[cx + dx, cy + dy] = P.BRASS[3]
            px[cx - 1, cy - 1] = P.BRASS[5]
            px[cx + 1, cy + 1] = P.BRASS[1]
            for x, y in ((1, 1), (fw - 3, 1), (1, fh - 3), (fw - 3, fh - 3)):
                rivet(px, x0 + x, y0 + y)
        for face in ("up", "down", "east", "west"):
            paint_face(px, f[face], P.BRASS, face, "plain")

    elif role == "brow":
        x0, y0, fw, fh = f["north"]
        for x in range(fw):
            px[x0 + x, y0 + fh - 1] = material[1]


def paint_model(model, material_name):
    size = model["tex_size"]
    tex = Image.new("RGBA", (size, size), P.TRANSPARENT)
    glow = Image.new("RGBA", (size, size), P.TRANSPARENT)
    px, gpx = tex.load(), glow.load()
    material = P.MATERIALS[material_name]
    for c in all_cubes(model):
        paint_cube(px, gpx, c, material, SURFACE[material_name])
    return tex, glow


# ---------------------------------------------------------------------------------------------
# geo.json
# ---------------------------------------------------------------------------------------------
def geo_json(model):
    bones = []
    for b in model["bones"]:
        entry = {"name": b["name"]}
        if b["parent"]:
            entry["parent"] = b["parent"]
        entry["pivot"] = b["pivot"]
        if b["cubes"]:
            entry["cubes"] = [{"origin": c["origin"], "size": c["size"], "uv": c["uv"]} for c in b["cubes"]]
        bones.append(entry)
    bw, bh, by = model["bounds"]
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.{model['name']}",
                "texture_width": model["tex_size"],
                "texture_height": model["tex_size"],
                "visible_bounds_width": bw,
                "visible_bounds_height": bh,
                "visible_bounds_offset": [0, by, 0],
            },
            "bones": bones,
        }],
    }


# ---------------------------------------------------------------------------------------------
# Animations. Rotation is [x, y, z] in degrees. Sign convention (Bedrock):
#   limbs hanging below their pivot swing FORWARD with negative X;
#   things standing above their pivot (body, head) pitch FORWARD / look DOWN with positive X.
# ---------------------------------------------------------------------------------------------
def _keys(pairs):
    return {f"{t:g}": v for t, v in pairs}


def animations(model):
    brute = model["name"] == "brute"
    swing = 22 if brute else 28          # leg swing
    arm_swing = 12 if brute else 18
    slump = 28 if brute else 34          # dormant forward slump
    period = 1.4 if brute else 1.1

    def loop(length, bones):
        return {"loop": True, "animation_length": length, "bones": bones}

    def once(length, bones, hold=False):
        out = {"animation_length": length, "bones": bones}
        if hold:
            out["loop"] = "hold_on_last_frame"
        return out

    q = period / 4
    walk_leg_l = _keys([(0, [swing, 0, 0]), (q, [0, 0, 0]), (2 * q, [-swing, 0, 0]), (3 * q, [0, 0, 0]), (period, [swing, 0, 0])])
    walk_leg_r = _keys([(0, [-swing, 0, 0]), (q, [0, 0, 0]), (2 * q, [swing, 0, 0]), (3 * q, [0, 0, 0]), (period, [-swing, 0, 0])])
    walk_arm_l = _keys([(0, [-arm_swing, 0, 0]), (2 * q, [arm_swing, 0, 0]), (period, [-arm_swing, 0, 0])])
    walk_arm_r = _keys([(0, [arm_swing, 0, 0]), (2 * q, [-arm_swing, 0, 0]), (period, [arm_swing, 0, 0])])

    dormant_pose = {
        "body": {"rotation": [slump, 0, 0]},
        "head": {"rotation": [24, 0, 0]},
        "arm_left": {"rotation": [-slump + 6, 0, -4]},
        "arm_right": {"rotation": [-slump + 6, 0, 4]},
    }

    if brute:
        attack = {
            "body": {"rotation": _keys([(0, [0, 0, 0]), (0.25, [-14, 0, 0]), (0.45, [22, 0, 0]), (0.8, [0, 0, 0])])},
            "arm_left": {"rotation": _keys([(0, [0, 0, 0]), (0.25, [-150, 0, -8]), (0.45, [-35, 0, 0]), (0.8, [0, 0, 0])])},
            "arm_right": {"rotation": _keys([(0, [0, 0, 0]), (0.25, [-150, 0, 8]), (0.45, [-35, 0, 0]), (0.8, [0, 0, 0])])},
            "head": {"rotation": _keys([(0, [0, 0, 0]), (0.25, [-10, 0, 0]), (0.45, [12, 0, 0]), (0.8, [0, 0, 0])])},
        }
        attack_len = 0.8
    else:
        attack = {
            "body": {"rotation": _keys([(0, [0, 0, 0]), (0.15, [-6, -18, 0]), (0.3, [10, 16, 0]), (0.6, [0, 0, 0])])},
            "arm_right": {"rotation": _keys([(0, [0, 0, 0]), (0.15, [-125, 0, 12]), (0.3, [-40, 0, 0]), (0.6, [0, 0, 0])])},
            "arm_left": {"rotation": _keys([(0, [0, 0, 0]), (0.15, [-35, 0, -6]), (0.3, [-50, 0, -6]), (0.6, [0, 0, 0])])},
        }
        attack_len = 0.6

    return {
        "format_version": "1.8.0",
        "animations": {
            "idle": loop(4.0, {
                "body": {"rotation": _keys([(0, [0, 0, 0]), (2, [1.5, 0, 0]), (4, [0, 0, 0])])},
                "head": {"rotation": _keys([(0, [0, 0, 0]), (2, [-1.5, 0, 0]), (4, [0, 0, 0])])},
                "arm_left": {"rotation": _keys([(0, [0, 0, -1]), (2, [-2, 0, -3]), (4, [0, 0, -1])])},
                "arm_right": {"rotation": _keys([(0, [0, 0, 1]), (2, [-2, 0, 3]), (4, [0, 0, 1])])},
            }),
            "walk": loop(period, {
                "leg_left": {"rotation": walk_leg_l},
                "leg_right": {"rotation": walk_leg_r},
                "arm_left": {"rotation": walk_arm_l},
                "arm_right": {"rotation": walk_arm_r},
                "body": {
                    "rotation": _keys([(0, [3, 0, 2]), (2 * q, [3, 0, -2]), (period, [3, 0, 2])]),
                    "position": _keys([(0, [0, 0, 0]), (q, [0, -0.6, 0]), (2 * q, [0, 0, 0]), (3 * q, [0, -0.6, 0]), (period, [0, 0, 0])]),
                },
            }),
            "attack": once(attack_len, attack),
            "cast": once(1.0, {
                "body": {"rotation": _keys([(0, [0, 0, 0]), (0.3, [-8, 0, 0]), (0.7, [-8, 0, 0]), (1, [0, 0, 0])])},
                "head": {"rotation": _keys([(0, [0, 0, 0]), (0.3, [-12, 0, 0]), (0.7, [-12, 0, 0]), (1, [0, 0, 0])])},
                "arm_left": {"rotation": _keys([(0, [0, 0, 0]), (0.3, [-85, 0, -22]), (0.7, [-95, 0, -22]), (1, [0, 0, 0])])},
                "arm_right": {"rotation": _keys([(0, [0, 0, 0]), (0.3, [-85, 0, 22]), (0.7, [-95, 0, 22]), (1, [0, 0, 0])])},
            }),
            "hurt": once(0.3, {
                "body": {"rotation": _keys([(0, [0, 0, 0]), (0.1, [-9, 0, 3]), (0.3, [0, 0, 0])])},
                "head": {"rotation": _keys([(0, [0, 0, 0]), (0.1, [-12, 0, -4]), (0.3, [0, 0, 0])])},
            }),
            "activate": once(2.0, {
                "body": {"rotation": _keys([(0, [slump, 0, 0]), (0.6, [slump, 0, 0]), (1.5, [-4, 0, 0]), (2, [0, 0, 0])])},
                "head": {"rotation": _keys([(0, [24, 0, 0]), (0.4, [24, 0, 0]), (1.1, [-10, 0, 0]), (2, [0, 0, 0])])},
                "arm_left": {"rotation": _keys([(0, [-slump + 6, 0, -4]), (0.8, [-slump + 6, 0, -4]), (1.5, [6, 0, -10]), (2, [0, 0, 0])])},
                "arm_right": {"rotation": _keys([(0, [-slump + 6, 0, 4]), (0.8, [-slump + 6, 0, 4]), (1.5, [6, 0, 10]), (2, [0, 0, 0])])},
            }),
            "dormant": loop(2.0, dormant_pose),
            "death": once(1.5, {
                "root": {"position": _keys([(0, [0, 0, 0]), (0.5, [0, -1, 0]), (1.2, [0, -5, 0])])},
                "body": {"rotation": _keys([(0, [0, 0, 0]), (0.4, [-10, 0, 4]), (1.2, [62, 0, 6])])},
                "head": {"rotation": _keys([(0, [0, 0, 0]), (0.4, [-14, 0, 0]), (1.2, [30, 0, -12])])},
                "arm_left": {"rotation": _keys([(0, [0, 0, 0]), (0.5, [-20, 0, -30]), (1.2, [-70, 0, -18])])},
                "arm_right": {"rotation": _keys([(0, [0, 0, 0]), (0.5, [-20, 0, 30]), (1.2, [-70, 0, 18])])},
                "leg_left": {"rotation": _keys([(0, [0, 0, 0]), (1.2, [-38, 0, -6])])},
                "leg_right": {"rotation": _keys([(0, [0, 0, 0]), (1.2, [-24, 0, 6])])},
            }, hold=True),
        },
    }


ANIMATION_NAMES = ["idle", "walk", "attack", "cast", "hurt", "activate", "dormant", "death"]
BONE_NAMES = ["root", "body", "core", "head", "arm_left", "arm_right", "leg_left", "leg_right",
              "opt_shoulder_plates", "opt_back_banner", "opt_head_crest", "opt_weapon"]


# ---------------------------------------------------------------------------------------------
# Previews
# ---------------------------------------------------------------------------------------------
def uv_layout(model, scale=2):
    """The UV islands outlined and colour-coded by bone, for the contact sheet."""
    size = model["tex_size"]
    img = Image.new("RGBA", (size * scale, size * scale), (20, 20, 24, 255))
    draw = ImageDraw.Draw(img)
    colours = [(231, 111, 81), (42, 157, 143), (233, 196, 106), (38, 70, 83), (244, 162, 97), (120, 110, 200),
               (90, 170, 90), (200, 90, 150), (110, 110, 110), (150, 150, 150), (180, 180, 120), (120, 180, 180)]
    for i, b in enumerate(model["bones"]):
        col = colours[i % len(colours)]
        for c in b["cubes"]:
            for rect in faces(c).values():
                x, y, w, h = rect
                draw.rectangle([x * scale, y * scale, (x + w) * scale - 1, (y + h) * scale - 1],
                               fill=col + (255,), outline=(0, 0, 0, 255))
    return img


def ortho(model, tex, view="front", scale=4, include_optional=False, glow=None, glow_rgb=None):
    """A flat orthographic render (front = looking at the north faces, side = the east faces)."""
    cubes = []
    for b in model["bones"]:
        if b["name"].startswith("opt_") and not include_optional:
            continue
        cubes.extend(b["cubes"])
    xs = [c["origin"][0] for c in cubes] + [c["origin"][0] + c["size"][0] for c in cubes]
    ys = [c["origin"][1] for c in cubes] + [c["origin"][1] + c["size"][1] for c in cubes]
    zs = [c["origin"][2] for c in cubes] + [c["origin"][2] + c["size"][2] for c in cubes]
    min_x, max_x, min_y, max_y, min_z, max_z = min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)

    source = tex
    if glow is not None and glow_rgb is not None:
        source = tex.copy()
        tint = Image.new("RGBA", tex.size, glow_rgb + (255,))
        source.paste(tint, (0, 0), glow.split()[3])

    if view == "front":
        w_px, order = int(max_x - min_x), sorted(cubes, key=lambda c: -c["origin"][2])
    else:
        w_px, order = int(max_z - min_z), sorted(cubes, key=lambda c: c["origin"][0])
    h_px = int(max_y - min_y)
    img = Image.new("RGBA", (w_px + 2, h_px + 2), (0, 0, 0, 0))
    for c in order:
        f = faces(c)
        ox, oy, oz = c["origin"]
        w, h, d = c["size"]
        if view == "front":
            x, y, fw, fh = f["north"]
            dest = (int(max_x - (ox + w)) + 1, int(max_y - (oy + h)) + 1)
        else:
            x, y, fw, fh = f["east"]
            dest = (int(max_z - (oz + d)) + 1, int(max_y - (oy + h)) + 1)
        img.alpha_composite(source.crop((x, y, x + fw, y + fh)), dest)
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


# ---------------------------------------------------------------------------------------------
def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def write_all():
    import contact_sheet

    count = 0
    entries = []
    extra = {}
    for name, model in MODELS.items():
        model["tex_size"] = pack(model)
        write_json(os.path.join(ASSETS, "geo", f"{name}.geo.json"), geo_json(model))
        write_json(os.path.join(ASSETS, "animations", f"{name}.animation.json"), animations(model))
        count += 2
        glow = None
        for material in P.MATERIALS:
            tex, glow = paint_model(model, material)
            save(tex, os.path.join(ASSETS, "textures", "entity", f"{name}_{material}.png"))
            count += 1
            label = f"{name}_{material} ({model['tex_size']}px)"
            entries.append((label, tex))
            # UV layout + front and side renders (with a fire-coloured core) beside the texture.
            layout = uv_layout(model)
            front = ortho(model, tex, "front", glow=glow, glow_rgb=(249, 138, 30))
            side = ortho(model, tex, "side", glow=glow, glow_rgb=(249, 138, 30))
            opt = ortho(model, tex, "front", include_optional=True, glow=glow, glow_rgb=(155, 114, 234))
            h = max(layout.height, front.height, side.height, opt.height)
            strip = Image.new("RGBA", (layout.width + front.width + side.width + opt.width + 36, h), (38, 40, 46, 255))
            x = 0
            for part in (layout, front, side, opt):
                strip.alpha_composite(part, (x, 0))
                x += part.width + 12
            extra[label] = strip
        save(glow, os.path.join(ASSETS, "textures", "entity", f"{name}_core_glowmask.png"))
        count += 1
        entries.append((f"{name}_core_glowmask", glow))

    # A second sheet with large front / side views only, for judging silhouette and detail.
    views = []
    for name, model in MODELS.items():
        for material in P.MATERIALS:
            tex, glow = paint_model(model, material)
            for core, rgb in (("fire", (249, 138, 30)), ("frost", (120, 210, 232))):
                if (material == "stone") != (core == "fire"):
                    continue
                front = ortho(model, tex, "front", scale=1, glow=glow, glow_rgb=rgb)
                side = ortho(model, tex, "side", scale=1, glow=glow, glow_rgb=rgb)
                both = Image.new("RGBA", (front.width + side.width + 4, max(front.height, side.height)), (0, 0, 0, 0))
                both.alpha_composite(front, (0, 0))
                both.alpha_composite(side, (front.width + 4, 0))
                views.append((f"{name} {material} + {core} core (front, side)", both))
    contact_sheet.make_sheet("Golemancy entities: orthographic views", views, "entities_views.png",
                             scale=7, columns=2)

    contact_sheet.make_sheet(
        "Golemancy entities: texture (1x, 3x) | UV layout by bone | front | side | front with optional bones",
        entries, "entities.png", scale=3, columns=1, extra=extra)
    return count


if __name__ == "__main__":
    print("wrote", write_all(), "entity files")
