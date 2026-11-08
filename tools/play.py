"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/*.jpg (1080 px wide renders of the demo office, see tools/shots.py) and writes
the 512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen is cut short under its caption.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
TITLE = ROOT / "app/src/main/res/font/russo_one.ttf"
TEXT = ROOT / "app/src/main/res/font/manrope_bold.ttf"

ENAMEL = (0x2E, 0x5B, 0x4C)
ENAMEL_DEEP = (0x22, 0x45, 0x39)
PAPER = (0xF3, 0xEB, 0xDA)
INK = (0x24, 0x1C, 0x14)
BRASS = (0xE8, 0xC2, 0x6A)
STAMP = (0xB4, 0x37, 0x2B)

# The order they appear in the store, with the caption over each: a paper line and a brass one.
PAGES = [
    ("1-office", ("Every shopping trip", "is a run"), ("Каждый поход", "в магазин — рейс")),
    ("2-waybill", ("The whole household", "on one waybill"), ("Вся семья —", "в одной накладной")),
    ("3-run", ("Walk the shop", "shelf by shelf"), ("Магазин —", "полка за полкой")),
    ("4-slip", ("Every run", "pays in tokens"), ("Каждый рейс", "платит жетонами")),
    ("7-depot", ("A bicycle, a sign,", "one day a van"), ("Велосипед, вывеска,", "однажды фургон")),
    ("8-file", ("Seven ranks", "and fourteen badges"), ("Семь званий", "и четырнадцать значков")),
    ("6-notebook", ("Where it's cheaper", "and what went up"), ("Где дешевле", "и что подорожало")),
    ("5-archive", ("Every waybill", "kept in the archive"), ("Каждая накладная", "хранится в архиве")),
]


def font(path, size):
    return ImageFont.truetype(str(path), size)


def fit(path, text, size, width):
    while font(path, size).getlength(text) > width:
        size -= 2
    return font(path, size)


def board(w, h):
    g = Image.new("RGB", (w, h), ENAMEL)
    d = ImageDraw.Draw(g)
    # A thin enamel-sign frame inside the edge.
    d.rectangle((18, 18, w - 19, h - 19), outline=(0x6B, 0x8E, 0x82), width=3)
    return g.convert("RGBA")


def lay(g, card, x, y, radius=36):
    """A screen on the board: a soft shadow and rounded corners like a phone."""
    sh = Image.new("RGBA", (card.width + 80, card.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((40, 52, 40 + card.width, 52 + card.height), radius, fill=(8, 22, 16, 140))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(20)), (x - 40, y - 40))
    mask = Image.new("L", card.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, card.width - 1, card.height - 1 + radius * 2), radius, fill=255)
    rgba = card.convert("RGBA")
    rgba.putalpha(mask)
    g.alpha_composite(rgba, (x, y))


def screenshot(src, caption):
    W, H = 1080, 1920
    g = board(W, H)
    d = ImageDraw.Draw(g)
    size = min(fit(TITLE, line, 88, W - 140).size for line in caption)
    title = font(TITLE, size)
    d.text((W / 2, 140), caption[0], font=title, fill=PAPER, anchor="mm")
    d.text((W / 2, 245), caption[1], font=title, fill=BRASS, anchor="mm")
    im = Image.open(src).convert("RGB")
    w = 860
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 350
    # The screen runs off the bottom edge, like a waybill that goes on.
    lay(g, im.crop((0, 0, w, H - top)), (W - w) // 2, top)
    return g.convert("RGB")


def logo(size):
    """The README logo has rounded corners; the enamel fills them so Play's own mask finds a full square."""
    im = Image.open(ROOT / "docs/logo.png").convert("RGBA")
    bg = Image.new("RGBA", im.size, ENAMEL + (255,))
    bg.alpha_composite(im)
    return bg.convert("RGB").resize((size, size), Image.LANCZOS)


def feature(lang):
    W, H = 1024, 500
    g = board(W, H)
    d = ImageDraw.Draw(g)
    # The office in its street, from the top of the main screen.
    office = Image.open(SHOTS / lang / "1-office.jpg").convert("RGB").crop((0, 0, 1080, 660))
    office = office.resize((round(office.width * 360 / office.height), 360), Image.LANCZOS)
    lay(g, office, W - office.width - 52, 70, radius=24)
    mark = logo(300).crop((60, 60, 240, 240)).resize((96, 96), Image.LANCZOS)
    g.alpha_composite(mark.convert("RGBA"), (52, 64))
    name, lines = ("Errand", ["Every shopping trip", "is a run"]) if lang == "en" else \
        ("Посыльный", ["Каждый поход", "в магазин — рейс"])
    left = W - office.width - 52 - 52 - 52
    d.text((52, 186), name, font=fit(TITLE, name, 72, left), fill=PAPER)
    d.rectangle((52, 282, 52 + left, 286), fill=ENAMEL_DEEP)
    sub = fit(TEXT, max(lines, key=lambda l: font(TEXT, 40).getlength(l)), 40, left)
    for i, l in enumerate(lines):
        d.text((52, 306 + i * 54), l, font=sub, fill=BRASS if i == 1 else PAPER)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    logo(512).save(OUT / "icon-512.png", optimize=True)
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.jpg", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
