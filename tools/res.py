"""Writes the small hand-made resources: theme, colours and the adaptive launcher icon's layers
(the painted background layer itself comes from tools/icon.py).

python tools/res.py
"""
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"

FILES = {
    "values/themes.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Errand" parent="Theme.AppCompat.Light.NoActionBar">
        <item name="android:windowBackground">@color/window</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="colorPrimary">@color/enamel</item>
        <item name="colorAccent">@color/enamel</item>
    </style>
</resources>
""",
    "values/colors.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="window">#FFE9DFCB</color>
    <color name="enamel">#FF2E5B4C</color>
</resources>
""",
    "mipmap-anydpi-v26/ic_launcher.xml": """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@android:color/transparent" />
    <monochrome android:drawable="@drawable/ic_monochrome" />
</adaptive-icon>
""",
    "drawable/ic_monochrome.xml": """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="M30,44l4,-8h18v8h-22z M56,36h18l4,8h-22z M30,47h22v11h-22z M56,47h22v11h-22z M30,61h22v18h-22z M56,61h22v18h-22z" />
</vector>
""",
}


def main():
    for name, text in FILES.items():
        f = RES / name
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(text, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
