"""Writes the small hand-made resources: theme, colours and the launcher icon.

python tools/res.py
"""
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"

# A brown-paper parcel tied with string, a red "delivered" stamp on its corner.
PARCEL = """    <path android:fillColor="#33000000" android:pathData="M31,79h48l-3,4h-42z" />
    <path android:fillColor="#C99A5E" android:pathData="M30,44h48v35h-48z" />
    <path android:fillColor="#B3864D" android:pathData="M30,44h48v6h-48z" />
    <path android:fillColor="#DDB57C" android:pathData="M34,36h40l4,8h-48z" />
    <path android:fillColor="#F3EBDA" android:pathData="M52,36h4v43h-4z M30,58h48v3h-48z" />
    <path android:fillColor="#F3EBDA" android:pathData="M54,36c-5,-7 -14,-6 -12,-1c1,3 8,3 12,1z M54,36c5,-7 14,-6 12,-1c-1,3 -8,3 -12,1z" />
    <path android:fillColor="#B4372B" android:pathData="M69,69m-9,0a9,9 0,1 1,18 0a9,9 0,1 1,-18 0" />
    <path android:fillColor="#C99A5E" android:pathData="M69,69m-6.8,0a6.8,6.8 0,1 1,13.6 0a6.8,6.8 0,1 1,-13.6 0" />
    <path android:fillColor="#B4372B" android:pathData="M69,69m-5.6,0a5.6,5.6 0,1 1,11.2 0a5.6,5.6 0,1 1,-11.2 0" />
    <path android:fillColor="#F3EBDA" android:pathData="M65.5,69.2l1.4,-1.4l1.6,1.6l3.6,-3.6l1.4,1.4l-5,5z" />"""

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
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_monochrome" />
</adaptive-icon>
""",
    "drawable/ic_launcher_background.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- Enamel green with a thin cream frame, like the office sign. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#2E5B4C" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#00000000" android:strokeColor="#55F6EFDF" android:strokeWidth="1.2" android:pathData="M22,22h64v64h-64z" />
</vector>
""",
    "drawable/ic_launcher_foreground.xml": f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
{PARCEL}
</vector>
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
