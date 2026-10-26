# AstroDroid

AstroDroid is an Android night-sky window. It uses the phone's orientation sensors to point a star map at the sky, with named stars, deep-sky objects, planets, and the Moon.

## Build

Open the project in Android Studio, or build a debug APK with:

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Sky catalog

The bundled JSON catalog is generated with `python3 tools/import_sky_catalog.py`. It contains named stars selected from HYG Database v4.1 and the Messier objects from OpenNGC. Solar-system sprite definitions use the same object schema; current positions are calculated by the app.

Catalog data is licensed under CC BY-SA 4.0. See the in-app **About & data credits** menu and `app/src/main/assets/sky_catalog_license.txt` for attribution and license details. The AstroDroid application source is MIT licensed; see `LICENSE`.

<img width="407" height="909" alt="image" src="https://github.com/user-attachments/assets/dcf774cb-6d4b-4cf1-b102-468f66c274fc" />
