"""Minimal NBT writer (enough for structure templates). Deterministic output.

Python values map to tags as follows:
    Byte(v) / Short(v) / Int(v) / Long(v) / Float(v) / Double(v)   explicit numeric tags
    int                 TAG_Int
    str                 TAG_String
    dict                TAG_Compound (insertion order kept)
    list                TAG_List (element type from the first element; empty lists are TAG_End lists)
    IntArray([...])     TAG_Int_Array
"""
import gzip
import struct


class _Num:
    def __init__(self, v):
        self.v = v


class Byte(_Num):
    pass


class Short(_Num):
    pass


class Int(_Num):
    pass


class Long(_Num):
    pass


class Float(_Num):
    pass


class Double(_Num):
    pass


class IntArray(list):
    pass


def _tag_id(v):
    if isinstance(v, Byte):
        return 1
    if isinstance(v, Short):
        return 2
    if isinstance(v, (Int, int)) and not isinstance(v, bool):
        return 3
    if isinstance(v, Long):
        return 4
    if isinstance(v, Float):
        return 5
    if isinstance(v, (Double, float)):
        return 6
    if isinstance(v, str):
        return 8
    if isinstance(v, IntArray):
        return 11
    if isinstance(v, list):
        return 9
    if isinstance(v, dict):
        return 10
    raise TypeError(f"cannot encode {type(v)} as NBT")


def _string(s):
    data = s.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def _payload(v):
    t = _tag_id(v)
    raw = v.v if isinstance(v, _Num) else v
    if t == 1:
        return struct.pack(">b", raw)
    if t == 2:
        return struct.pack(">h", raw)
    if t == 3:
        return struct.pack(">i", raw)
    if t == 4:
        return struct.pack(">q", raw)
    if t == 5:
        return struct.pack(">f", raw)
    if t == 6:
        return struct.pack(">d", raw)
    if t == 8:
        return _string(raw)
    if t == 11:
        return struct.pack(">i", len(raw)) + b"".join(struct.pack(">i", x) for x in raw)
    if t == 9:
        if not raw:
            return struct.pack(">bi", 0, 0)
        et = _tag_id(raw[0])
        return struct.pack(">bi", et, len(raw)) + b"".join(_payload(x) for x in raw)
    if t == 10:
        out = b""
        for key, value in raw.items():
            out += struct.pack(">b", _tag_id(value)) + _string(key) + _payload(value)
        return out + b"\x00"
    raise AssertionError


def dumps(root):
    """Encodes a root compound (unnamed) as gzip-compressed NBT."""
    body = struct.pack(">b", 10) + _string("") + _payload(root)
    # mtime=0 keeps the gzip header stable between runs.
    return gzip.compress(body, compresslevel=9, mtime=0)


def write(path, root):
    import os
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(dumps(root))
