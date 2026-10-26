#!/usr/bin/env python3
"""Rebuild the bundled, compact sky catalog from its upstream open datasets."""

import csv
import io
import json
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app/src/main/assets/sky_catalog.json"
HYG_REVISION = "c7f7f883fe678cc7680169a50ccd7dcc49b060ce"
OPENNGC_REVISION = "be3cd6c5498806a2fbb2f7e0452ef802dbb02be0"
STAR_NAMES_FILE = ROOT / "tools/star_names.txt"


def download(url):
    request = urllib.request.Request(url, headers={"User-Agent": "AstroDroid catalog importer"})
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read().decode("utf-8")


def named_stars():
    url = (f"https://raw.githubusercontent.com/astronexus/HYG-Database/{HYG_REVISION}/"
           "hyg/CURRENT/hygdata_v41.csv")
    wanted_names = {name.strip().casefold() for name in
                    STAR_NAMES_FILE.read_text(encoding="utf-8").splitlines() if name.strip()}
    stars = []
    for row in csv.DictReader(io.StringIO(download(url))):
        name = row["proper"].strip()
        if not name or name.casefold() not in wanted_names or not row["mag"]:
            continue
        catalog_ids = []
        for label, value in (("HIP", row["hip"]), ("HD", row["hd"]),
                             ("HR", row["hr"]), ("Bayer", row["bf"])):
            if value:
                catalog_ids.append(f"{label} {value}")
        stars.append({
            "id": f"hyg-{row['id']}",
            "name": name,
            "identifiers": catalog_ids,
            "raDeg": round(float(row["ra"]) * 15.0, 8),
            "decDeg": float(row["dec"]),
            "magnitude": float(row["mag"]),
            "kind": "STAR",
            "color": star_color(float(row["ci"]) if row["ci"] else None),
        })
    found_names = {star["name"].casefold() for star in stars}
    missing_names = wanted_names - found_names
    if missing_names:
        raise ValueError(f"HYG v4.1 did not contain requested names: {sorted(missing_names)}")
    return stars


def star_color(color_index):
    if color_index is None:
        return [1.0, 0.95, 0.8, 1.0]
    stops = [(-0.4, [0.56, 0.68, 1.0]), (0.0, [0.74, 0.82, 1.0]),
             (0.8, [1.0, 0.98, 0.84]), (1.5, [1.0, 0.73, 0.48]),
             (2.0, [1.0, 0.48, 0.32])]
    for (x0, c0), (x1, c1) in zip(stops, stops[1:]):
        if color_index <= x1:
            t = max(0.0, min(1.0, (color_index - x0) / (x1 - x0)))
            return [round(a + t * (b - a), 4) for a, b in zip(c0, c1)] + [1.0]
    return stops[-1][1] + [1.0]


def sexagesimal_to_degrees(value, hours=False):
    parts = value.strip().split(":")
    first = float(parts[0])
    sign = -1 if first < 0 or value.strip().startswith("-") else 1
    result = abs(first) + float(parts[1]) / 60.0
    if len(parts) > 2:
        result += float(parts[2]) / 3600.0
    result *= sign
    return result * 15.0 if hours else result


def object_kind(type_code):
    if type_code in {"G", "GPair", "GGroup", "GTrpl"}:
        return "GALAXY"
    if type_code in {"OCl", "GCl", "Cl+N", "*Ass"}:
        return "STAR_CLUSTER"
    if type_code in {"HII", "Neb", "EmN", "RfN", "DrkN", "SNR", "PN"}:
        return "NEBULA"
    return "OTHER"


def deep_sky_objects():
    base = f"https://raw.githubusercontent.com/mattiaverga/OpenNGC/{OPENNGC_REVISION}/database_files/"
    rows = []
    for filename in ("NGC.csv", "addendum.csv"):
        source_rows = csv.DictReader(io.StringIO(download(base + filename)), delimiter=";")
        rows.extend(source_rows)

    objects = []
    for row in rows:
        try:
            ra = sexagesimal_to_degrees(row["RA"], hours=True)
            dec = sexagesimal_to_degrees(row["Dec"])
        except (ValueError, KeyError, IndexError):
            continue
        magnitude = next((float(row[key]) for key in ("V-Mag", "B-Mag")
                          if row.get(key, "") not in ("", "nan")), None)
        messier = row.get("M", "").strip()
        if not messier:
            continue

        identifiers = []
        if messier:
            identifiers.append(f"M {int(messier):d}")
        for key, prefix in (("Name", ""), ("NGC", "NGC "), ("IC", "IC ")):
            value = row.get(key, "").strip()
            if value:
                identifier = value if key == "Name" else prefix + value
                if identifier not in identifiers:
                    identifiers.append(identifier)
        extra_names = [name.strip() for name in row.get("Common names", "").split(",") if name.strip()]
        name = extra_names[0] if extra_names else (identifiers[0] if identifiers else row["Name"])
        objects.append({
            "id": f"openngc-{row['Name']}",
            "name": name,
            "identifiers": identifiers + [n for n in extra_names[1:] if n not in identifiers],
            "raDeg": round(ra, 8),
            "decDeg": round(dec, 8),
            "magnitude": magnitude,
            "kind": object_kind(row.get("Type", "")),
            "color": kind_color(object_kind(row.get("Type", ""))),
            "sourceNotes": row.get("Sources", ""),
        })
    return objects


def kind_color(kind):
    return {
        "GALAXY": [0.72, 0.82, 1.0, 1.0],
        "STAR_CLUSTER": [1.0, 0.88, 0.62, 1.0],
        "NEBULA": [0.55, 0.78, 1.0, 1.0],
        "OTHER": [0.8, 0.8, 0.8, 1.0],
    }[kind]


def solar_system_objects():
    specs = [
        ("Sun", "sun", 0, 34, 134), ("Moon", "moon", 1, 32, 204),
        ("Mercury", "planet", 2, 24, 172), ("Venus", "planet", 3, 26, 169),
        ("Earth", "planet", 4, 26, 154), ("Mars", "planet", 5, 24, 152),
        ("Jupiter", "planet", 6, 28, 144), ("Saturn", "planet", 7, 28, 128),
        ("Uranus", "planet", 8, 24, 156), ("Neptune", "planet", 9, 24, 152),
    ]
    return [{
        "id": f"solar-system-{name.lower()}", "name": name, "identifiers": [],
        "raDeg": 0.0, "decDeg": 0.0, "magnitude": None, "kind": "SOLAR_SYSTEM",
        "dynamicPosition": dynamic,
        "visual": {"atlas": "celestial_atlas", "cell": cell,
                   "sizePx": size, "maxZoomSizePx": max_size},
    } for name, dynamic, cell, size, max_size in specs]


def main():
    stars = named_stars()
    deep_sky = deep_sky_objects()
    catalog = {
        "schemaVersion": 1,
        "sources": [
            {
                "id": "hyg-v41",
                "title": "HYG Database v4.1",
                "url": "https://github.com/astronexus/HYG-Database",
                "revision": HYG_REVISION,
                "license": "CC BY-SA 4.0",
                "attribution": "HYG Database by David Nash (Astronexus); compiled from Hipparcos, Yale Bright Star Catalog, and Gliese catalogs",
                "query": "Proper-name stars already represented by the app",
                "recordCount": len(stars),
            },
            {
                "id": "openngc",
                "title": "OpenNGC",
                "url": "https://github.com/mattiaverga/OpenNGC",
                "revision": OPENNGC_REVISION,
                "license": "CC BY-SA 4.0",
                "attribution": "OpenNGC, Mattia Verga; see per-record Sources in upstream database",
                "query": "All Messier objects, including M40 and M45 from addendum.csv",
                "recordCount": len(deep_sky),
            },
        ],
        "objects": stars + deep_sky + solar_system_objects(),
    }
    OUTPUT.write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote {len(catalog['objects'])} records to {OUTPUT}")


if __name__ == "__main__":
    main()
