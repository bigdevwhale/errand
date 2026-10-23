"""Draws the README logo from the launcher icon's shapes: a parcel tied with string and a red stamp on enamel green, as docs/logo.png.

python tools/icon.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
S = 1024
K = 4  # supersampling


def main():
    n = S * K
    u = n / 108
    im = Image.new("RGBA", (n, n), (0x2E, 0x5B, 0x4C, 255))
    d = ImageDraw.Draw(im)

    def rect(x0, y0, x1, y1, fill):
        d.rectangle([x0 * u, y0 * u, x1 * u, y1 * u], fill=fill)

    def poly(points, fill):
        d.polygon([(x * u, y * u) for x, y in points], fill=fill)

    def circle(cx, cy, r, fill):
        d.ellipse([(cx - r) * u, (cy - r) * u, (cx + r) * u, (cy + r) * u], fill=fill)

    cream = (0xF3, 0xEB, 0xDA)
    red = (0xB4, 0x37, 0x2B)
    paper = (0xC9, 0x9A, 0x5E)
    # The thin frame of the office sign.
    d.rectangle([22 * u, 22 * u, 86 * u, 86 * u], outline=(0x6E, 0x8F, 0x83), width=int(1.2 * u))
    # Shadow, box, lid.
    poly([(31, 79), (79, 79), (76, 83), (34, 83)], (0x22, 0x44, 0x38))
    rect(30, 44, 78, 79, paper)
    rect(30, 44, 78, 50, (0xB3, 0x86, 0x4D))
    poly([(34, 36), (74, 36), (78, 44), (30, 44)], (0xDD, 0xB5, 0x7C))
    # String and bow.
    rect(52, 36, 56, 79, cream)
    rect(30, 58, 78, 61, cream)
    d.ellipse([42 * u, 30 * u, 54 * u, 38 * u], fill=cream)
    d.ellipse([54 * u, 30 * u, 66 * u, 38 * u], fill=cream)
    d.ellipse([45 * u, 32 * u, 52 * u, 36 * u], fill=paper)
    d.ellipse([56 * u, 32 * u, 63 * u, 36 * u], fill=paper)
    # The stamp.
    circle(69, 69, 9, red)
    circle(69, 69, 6.8, paper)
    circle(69, 69, 5.6, red)
    poly([(65.5, 69.2), (66.9, 67.8), (68.5, 69.4), (72.1, 65.8), (73.5, 67.2), (68.5, 72.2)], cream)

    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.22), fill=255)
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    out = out.resize((S, S), Image.LANCZOS)
    (ROOT / "docs").mkdir(exist_ok=True)
    out.save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()
