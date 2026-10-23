#!/usr/bin/env python3
"""Writes the structures the game tests are built on.

Every test starts on an empty platform: a floor of grass blocks with air above. The tests
place everything else themselves, so the structures stay simple and can be generated.

    python3 tools/generate_test_structures.py
"""
import gzip
import io
import struct
from pathlib import Path

# world_version of Minecraft 26.3 (version.json in the server jar)
DATA_VERSION = 5023
OUT = Path(__file__).resolve().parent.parent / "common/src/gametest/resources/data/workingvillagers/structure"

# (name, width, height, depth)
PLATFORMS = [
    ("platform", 24, 10, 24),
    ("small_platform", 12, 6, 12),
    ("tall_platform", 24, 20, 24),
]

TAG_END, TAG_BYTE, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 1, 3, 8, 9, 10


class Int(int):
    pass


def write_payload(out, value):
    if isinstance(value, Int):
        out.write(struct.pack(">i", value))
    elif isinstance(value, str):
        data = value.encode("utf-8")
        out.write(struct.pack(">H", len(data)))
        out.write(data)
    elif isinstance(value, list):
        element = tag_type(value[0]) if value else TAG_END
        out.write(struct.pack(">bi", element, len(value)))
        for item in value:
            write_payload(out, item)
    elif isinstance(value, dict):
        for key, item in value.items():
            out.write(struct.pack(">b", tag_type(item)))
            write_payload(out, key)
            write_payload(out, item)
        out.write(struct.pack(">b", TAG_END))
    else:
        raise TypeError(value)


def tag_type(value):
    if isinstance(value, Int):
        return TAG_INT
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, list):
        return TAG_LIST
    if isinstance(value, dict):
        return TAG_COMPOUND
    raise TypeError(value)


def platform(width, height, depth):
    palette = [{"Name": "minecraft:grass_block", "Properties": {"snowy": "false"}}, {"Name": "minecraft:air"}]
    blocks = []
    for x in range(width):
        for y in range(height):
            for z in range(depth):
                blocks.append({"pos": [Int(x), Int(y), Int(z)], "state": Int(0 if y == 0 else 1)})
    return {
        "DataVersion": Int(DATA_VERSION),
        "size": [Int(width), Int(height), Int(depth)],
        "palette": palette,
        "blocks": blocks,
        "entities": [],
    }


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, width, height, depth in PLATFORMS:
        raw = io.BytesIO()
        raw.write(struct.pack(">b", TAG_COMPOUND))
        write_payload(raw, "")
        write_payload(raw, platform(width, height, depth))
        # mtime=0 so the file only changes when the structure does
        with gzip.GzipFile(OUT / f"{name}.nbt", "wb", mtime=0) as file:
            file.write(raw.getvalue())
        print(f"{name}.nbt: {width}x{height}x{depth}")


if __name__ == "__main__":
    main()
