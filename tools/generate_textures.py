#!/usr/bin/env python3
"""Generates the 16x16 block texture (inventory icon of the machine) (no external dependencies). Run from the project root: python3 tools/generate_textures.py"""
import os, struct, zlib

OUT = "src/main/resources/assets/fldsmdfr/textures"

def png(path, px):
    raw = b"".join(b"\x00" + bytes([c for p in row for c in p]) for row in px)
    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) \
        + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "wb").write(data)

def blank():
    return [[(0, 0, 0, 0)] * 16 for _ in range(16)]

def rect(g, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                g[y][x] = c + (255,) if len(c) == 3 else c

def ell(g, cx, cy, rx, ry, c):
    for y in range(16):
        for x in range(16):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                g[y][x] = c + (255,)

def outline(g, c=(40, 25, 15)):
    o = [row[:] for row in g]
    for y in range(16):
        for x in range(16):
            if g[y][x][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and g[ny][nx][3] > 0:
                        o[y][x] = c + (255,)
                        break
    return o

def core_tex():
    g = [[(245, 244, 238, 255)] * 16 for _ in range(16)]
    rect(g, 0, 5, 15, 7, (236, 115, 30)); rect(g, 0, 11, 15, 12, (236, 115, 30))
    rect(g, 0, 0, 15, 0, (170, 170, 175)); rect(g, 0, 15, 15, 15, (170, 170, 175))
    for x in (3, 8, 13): g[9][x] = (90, 170, 255, 255)
    return g

if __name__ == "__main__":
    png(f"{OUT}/blocks/fldsmdfr_core.png", core_tex())
    print("textures generated (the foods and the machine are drawn by code, no textures needed)")
