#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 The halogenOS Project
"""
Converts frame-per-line CSV Glyph patterns into .glyph animations.

A CSV pattern holds one frame per line at a fixed frame rate, either one value per
zone group or one value per LED. Identical consecutive frames become a single step.
Every result is expanded back into frames and compared with its source before it is
written, so a conversion either is lossless or fails.
"""

import argparse
import csv
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

FORMAT = 1


@dataclass(frozen=True)
class Layout:
    codename: str
    leds: int
    groups: list[list[int]]

    @classmethod
    def load(cls, device: Path, groups: Path):
        frame = json.loads(device.read_text())
        return cls(frame["codename"], frame["frame"]["size"], json.loads(groups.read_text()))

    def expand(self, row: list[int]) -> tuple[int, ...]:
        if len(row) == self.leds:
            return tuple(row)
        if len(row) == len(self.groups):
            levels = [0] * self.leds
            for value, leds in zip(row, self.groups):
                for led in leds:
                    levels[led] = value
            return tuple(levels)
        raise ValueError(f"row with {len(row)} values fits neither {self.leds} LEDs nor {len(self.groups)} groups")


def read_frames(path: Path, layout: Layout) -> list[tuple[int, ...]]:
    with path.open(newline="") as source:
        rows = ([cell.strip() for cell in row] for row in csv.reader(source))
        return [layout.expand([int(cell) for cell in row if cell]) for row in rows if any(row)]


def frame_start(frame: int, fps: int) -> int:
    return round(frame * 1000 / fps)


def to_steps(frames: list[tuple[int, ...]], fps: int, peak: int) -> list[dict]:
    runs: list[tuple[int, tuple[int, ...]]] = []
    for index, frame in enumerate(frames):
        if not runs or runs[-1][1] != frame:
            runs.append((index, frame))
    ends = [start for start, _ in runs[1:]] + [len(frames)]
    return [
        {
            "duration": frame_start(end, fps) - frame_start(start, fps),
            "levels": [level(value, peak) for value in frame],
        }
        for (start, frame), end in zip(runs, ends)
    ]


def level(value: int, peak: int) -> float | int:
    scaled = round(value / peak, 5)
    return int(scaled) if scaled in (0, 1) else scaled


def to_frames(steps: list[dict], fps: int, peak: int) -> list[tuple[int, ...]]:
    frames, elapsed = [], 0
    for step in steps:
        first = round(elapsed * fps / 1000)
        elapsed += step["duration"]
        last = round(elapsed * fps / 1000)
        frames += [tuple(round(value * peak) for value in step["levels"])] * (last - first)
    return frames


def slug(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")


def convert(source: Path, category: str, layout: Layout, fps: int, peak: int, out: Path) -> Path:
    frames = read_frames(source, layout)
    steps = to_steps(frames, fps, peak)
    if to_frames(steps, fps, peak) != frames:
        raise ValueError(f"{source}: round trip differs from the source")
    target = out / category / f"{slug(source.stem)}.glyph"
    target.parent.mkdir(parents=True, exist_ok=True)
    animation = {"format": FORMAT, "device": layout.codename, "name": source.stem, "steps": steps}
    target.write_text(json.dumps(animation, separators=(",", ":")) + "\n")
    return target


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--device", type=Path, required=True, help="device.json of the target device")
    parser.add_argument("--groups", type=Path, required=True, help="JSON list of LED index lists, one per zone group column")
    parser.add_argument("--fps", type=int, default=60)
    parser.add_argument("--peak", type=int, default=4095, help="CSV value that means full brightness")
    parser.add_argument("--out", type=Path, required=True, help="directory that receives <category>/<name>.glyph")
    parser.add_argument("sources", nargs="+", metavar="CATEGORY=CSV", help="a CSV file and the category it belongs to")
    args = parser.parse_args()

    layout = Layout.load(args.device, args.groups)
    targets = {}
    for spec in args.sources:
        category, _, source = spec.partition("=")
        target = convert(Path(source), category, layout, args.fps, args.peak, args.out)
        if target in targets:
            raise ValueError(f"{source} and {targets[target]} both become {target}")
        targets[target] = source
    print(f"converted {len(targets)} patterns for {layout.codename}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
