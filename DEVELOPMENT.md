# Development

## Building

```
./gradlew build
```

or, from anywhere in the suite, `mc-build cobble-jaws`.

Compiles against Pandorical's working tree as a subproject, and against minedew-fishing's built jar as an optional compile-only dependency. If minedew-fishing has not been built, the build stops and says so rather than quietly dropping the integration.

## Installing

Server-side. Drop the jar in the server's `mods/` alongside Pandorical. Players need Pandorical client-side to see the sharks' skins; without it the sharks are there, correctly sized and tempered, but look like dolphins.

Shark fishing needs minedew-fishing 1.1.0 or later, the version that opened its bite to other mods. An older one is refused at load rather than crashing on the first bite.

## The art

```
python3 generate_textures.py
python3 generate_icon.py
```

`generate_textures.py` reads the vanilla dolphin and baby dolphin sheets straight out of the Loom cache and recolours them against luminance, so the shading and the eyes land where the model expects them. The back and belly are picked by where each model box's faces sit on the sheet, not by colour, because a dolphin is nearly one blue all over; the boxes are copied from `DolphinModel` and `BabyDolphinModel`.

`generate_icon.py` paints a fin over a sea tinted from the vanilla water texture.

Both are idempotent and safe to re-run; neither needs the game running.

## What to check by hand

The smoke test (`./gradlew runClientGameTest`) proves the mod loads, its mixins bind and a world comes up. What it can't see:

- **A shark looks like a shark.** `/summon minecraft:dolphin ~ ~ ~ {Tags:["shark"]}` in water: dark back, white belly. Do the same with `{Tags:["shark"],Age:-24000}` for a pup, and check it still wears the right skin after it grows up.
- **It leaves you alone, then doesn't.** Swim up to one: nothing. Hit it: it comes for you. Summon a pup beside an adult and swim close: the adult comes for you.
- **It eats.** Summon a few cod beside one and wait. They are eaten and leave nothing floating.
- **The fishing.** With minedew-fishing installed, `/time set night` and `/weather rain` over deep ocean tilt the size roll toward large, which is the one that comes up alive.
