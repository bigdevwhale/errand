"""Makes the launcher icon and the README logo from the painted artwork in tools/icon-art.png:
a kraft-paper parcel tied with string and a red stamp on enamel green, painted in a nostalgic anime style.

python tools/icon.py

Writes the adaptive icon's background layer (res/drawable-nodpi/ic_launcher_background.webp, the whole painting:
the parcel sits inside the launcher's safe zone) and docs/logo.png with rounded corners.
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "tools/icon-art.png"
S = 1024
LAYER = 432  # 108 dp at xxxhdpi


def main():
    art = Image.open(ART).convert("RGB").resize((S, S), Image.LANCZOS)

    res = ROOT / "app/src/main/res/drawable-nodpi"
    res.mkdir(parents=True, exist_ok=True)
    art.resize((LAYER, LAYER), Image.LANCZOS).save(res / "ic_launcher_background.webp", quality=90, method=6)

    mask = Image.new("L", (S * 4, S * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S * 4 - 1, S * 4 - 1], radius=int(S * 4 * 0.22), fill=255)
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    out.paste(art, (0, 0), mask.resize((S, S), Image.LANCZOS))
    (ROOT / "docs").mkdir(exist_ok=True)
    out.save(ROOT / "docs/logo.png", optimize=True)


if __name__ == "__main__":
    main()
