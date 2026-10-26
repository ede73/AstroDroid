# Sky catalog import

Run `python3 tools/import_sky_catalog.py` from the project root to rebuild
`app/src/main/assets/sky_catalog.json` from the pinned HYG v4.1 and OpenNGC
revisions in the importer.

The importer currently selects the named stars in `star_names.txt` and all
110 Messier objects. It writes one common object schema; `kind` is a display
category, and designations such as M 31 or NGC 224 are identifiers on that
record, not separate Kotlin types. Solar-system records keep their visual
metadata in the same JSON and get their changing coordinates from the app's
ephemeris calculations.

The bundled catalog is adapted from two CC BY-SA 4.0 datasets. Keep the
attribution in the app's About menu and the terms in
`app/src/main/assets/sky_catalog_license.txt` when updating the export. The
application source code has a separate license.
