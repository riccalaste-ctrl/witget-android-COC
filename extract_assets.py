#!/usr/bin/env python3
"""
Extract assets from iOS .xcassets to Android drawable folders.
Run this script from the clashboard-android directory.
"""

import os
import json
import shutil
from pathlib import Path

IOS_REPO_PATH = Path("C:/tmp/clash_widgets")  # Adjust if needed
ANDROID_RES_PATH = Path("app/src/main/res")

# Density mapping
DENSITY_MAP = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}

def extract_imageset(imageset_path: Path, output_name: str, category: str):
    """Extract a single .imageset to Android drawables."""
    contents_json = imageset_path / "Contents.json"
    if not contents_json.exists():
        return

    with open(contents_json) as f:
        contents = json.load(f)

    images = contents.get("images", [])
    for img in images:
        filename = img.get("filename")
        scale = img.get("scale", "1x")
        idiom = img.get("idiom", "universal")

        if not filename:
            continue

        src_file = imageset_path / filename
        if not src_file.exists():
            continue

        # Determine density based on scale
        density = "mdpi"
        if scale == "2x":
            density = "xhdpi"
        elif scale == "3x":
            density = "xxhdpi"
        elif scale == "4x":
            density = "xxxhdpi"

        # Output path
        dest_dir = ANDROID_RES_PATH / f"drawable-{density}" / category
        dest_dir.mkdir(parents=True, exist_ok=True)

        # Clean filename for Android (lowercase, underscores)
        android_name = output_name.lower().replace(" ", "_").replace("-", "_")
        if not android_name.endswith(".png"):
            android_name += ".png"

        dest_file = dest_dir / android_name

        try:
            shutil.copy2(src_file, dest_file)
            print(f"Copied: {src_file} -> {dest_file}")
        except Exception as e:
            print(f"Error copying {src_file}: {e}")

def process_xcassets(xcassets_path: Path, category: str):
    """Process all .imageset folders in an .xcassets directory."""
    print(f"Processing {xcassets_path} for category {category}")
    if not xcassets_path.exists():
        print(f"  Path does not exist: {xcassets_path}")
        return
    for imageset in xcassets_path.rglob("*.imageset"):
        name = imageset.stem  # folder name without .imageset
        print(f"  Found imageset: {name}")
        extract_imageset(imageset, name, category)

def main():
    print("Extracting assets from iOS repo...")

    # Building images (Home Village)
    buildings_home = IOS_REPO_PATH / "ClashDashWidget" / "Assets.xcassets" / "buildings_home"
    if buildings_home.exists():
        process_xcassets(buildings_home, "buildings")

    # Lab images
    lab_assets = IOS_REPO_PATH / "clash_widgets" / "Assets.xcassets" / "lab"
    if lab_assets.exists():
        process_xcassets(lab_assets, "lab")

    # Pets
    pets_assets = IOS_REPO_PATH / "clash_widgets" / "Assets.xcassets" / "pets"
    if pets_assets.exists():
        process_xcassets(pets_assets, "pets")

    # Builder Base
    bb_assets = IOS_REPO_PATH / "clash_widgets" / "Assets.xcassets" / "builder_base"
    if bb_assets.exists():
        process_xcassets(bb_assets, "builder_base")

    # Crafted Defenses
    crafted_assets = IOS_REPO_PATH / "ClashDashWidget" / "Assets.xcassets" / "crafted_defenses"
    if crafted_assets.exists():
        process_xcassets(crafted_assets, "crafted")

    # Extras (potions, etc.)
    extras_assets = IOS_REPO_PATH / "ClashDashWidget" / "Assets.xcassets" / "extras"
    if extras_assets.exists():
        process_xcassets(extras_assets, "extras")

    # Resources
    resources_assets = IOS_REPO_PATH / "clash_widgets" / "Assets.xcassets" / "resources"
    if resources_assets.exists():
        process_xcassets(resources_assets, "resources")

    # Town Hall
    th_assets = IOS_REPO_PATH / "clash_widgets" / "Assets.xcassets" / "town_hall"
    if th_assets.exists():
        process_xcassets(th_assets, "townhall")

    # Profile icons
    profile_assets = IOS_REPO_PATH / "clash_widgets" / "Assets.xcassets" / "profile"
    if profile_assets.exists():
        process_xcassets(profile_assets, "profile")

    # Leagues
    league_assets = IOS_REPO_PATH / "clash_widgets" / "Assets.xcassets" / "leagues"
    if league_assets.exists():
        process_xcassets(league_assets, "leagues")

    # JSON data files
    json_src = IOS_REPO_PATH / "clash_widgets" / "json"
    json_dest = Path("app/src/main/assets/json")
    if json_src.exists():
        json_dest.mkdir(parents=True, exist_ok=True)
        for json_file in json_src.glob("*.json"):
            shutil.copy2(json_file, json_dest / json_file.name)
            print(f"Copied JSON: {json_file.name}")

    # JSON maps
    maps_src = json_src / "json_maps"
    if maps_src.exists():
        maps_dest = json_dest / "json_maps"
        maps_dest.mkdir(parents=True, exist_ok=True)
        for json_file in maps_src.glob("*.json"):
            shutil.copy2(json_file, maps_dest / json_file.name)
            print(f"Copied JSON map: {json_file.name}")

    # Parsed JSON files
    parsed_src = json_src / "parsed_json_files"
    if parsed_src.exists():
        parsed_dest = json_dest / "parsed_json_files"
        parsed_dest.mkdir(parents=True, exist_ok=True)
        for json_file in parsed_src.glob("*.json"):
            shutil.copy2(json_file, parsed_dest / json_file.name)
            print(f"Copied parsed JSON: {json_file.name}")

    print("Asset extraction complete!")

if __name__ == "__main__":
    main()