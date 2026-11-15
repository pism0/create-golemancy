"""Ponder schematics, written as structure NBT (there is no display to save them in-game).

Follows Create's conventions: a 5x5 base plate checkered with white concrete and snow blocks on
layer 0, the scene content above it. Positions must match
integration/create/client/GolemancyPonder.java.
"""
import os

import nbt
from make_structures import ROOT, structure

PONDER_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "golemancy", "ponder")


def base_plate(size=5):
    return [(x, 0, z, "minecraft:white_concrete" if (x + z) % 2 == 0 else "minecraft:snow_block")
            for x in range(size) for z in range(size)]


def binding_altar():
    blocks = base_plate()
    blocks.append((2, 1, 2, "create:shaft", {"axis": "y", "waterlogged": "false"}))
    blocks.append((2, 2, 2, "golemancy:binding_altar", {"working": "false"}))
    return structure((5, 3, 5), blocks)


def frame_line():
    blocks = base_plate()
    blocks.append((2, 1, 2, "create:depot"))
    blocks.append((2, 3, 2, "create:deployer", {"facing": "down", "axis_along_first": "false"}))
    blocks.append((2, 3, 3, "create:shaft", {"axis": "z", "waterlogged": "false"}))
    blocks.append((2, 3, 4, "create:cogwheel", {"axis": "z", "waterlogged": "false"}))
    return structure((5, 4, 5), blocks)


def write_all():
    nbt.write(os.path.join(PONDER_DIR, "binding_altar.nbt"), binding_altar())
    nbt.write(os.path.join(PONDER_DIR, "frame_line.nbt"), frame_line())
    return 2


if __name__ == "__main__":
    print("wrote", write_all(), "ponder schematics")
