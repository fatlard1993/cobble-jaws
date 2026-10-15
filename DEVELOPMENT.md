# Development

## Building

```
./gradlew build
```

or, from anywhere in the suite, `mc-build cobble-jaws`.

Compiles against Pandorical's working tree as a subproject, and against minedew-fishing's built jar as an optional compile-only dependency. If minedew-fishing has not been built, the build stops and says so rather than quietly dropping the integration.

## Installing

Server-side. Drop the jar in the server's `mods/` alongside Pandorical 15.21 or later, which is the version that lets a mod add an entity with a body of its own. Players need Pandorical 15.21 client-side to see sharks as sharks; without it they are shown dolphins.

Shark fishing needs minedew-fishing 1.1.0 or later, the version that opened its bite to other mods. An older one is refused at load rather than crashing on the first bite.

## The art

```
python3 generate_textures.py
python3 generate_icon.py
```

`generate_textures.py` writes the shark's model (`pandorical/entity_models/shark.json`) and paints its skin (`textures/entity/shark.png`) from one table of boxes, so a box cannot move on the sheet without its paint moving with it. The model's `animation` block names the part that turns where the shark looks and the three animations in `animations/` - `shark_idle`, `shark_swim` and `shark_bite` - which sweep the tail side to side with the fin a beat behind; Pandorical plays them, and nothing else moves the shark. It also recolours the dolphin spawn egg into the shark's.

`SharkPictured` photographs an adult and a pup, side on and from ahead, into `build/run/clientGameTest/screenshots/`: the way to see a change to the model.

`generate_icon.py` paints a fin over a sea tinted from the vanilla water texture.

Both are idempotent and safe to re-run; neither needs the game running.

## Tests

`./gradlew runClientGameTest` runs the smoke test and `SharkBehaviour`, which puts a shark in a glass tank and holds it to the readme: sized and named, breathing water, ignoring a swimmer until a pup is near, eating a cod without leaving it floating, drawn by the client from its own body, sent to a vanilla client as a dolphin, and a large one on a fishing line coming up alive and after you.

Both run in singleplayer, where the mod is on the client too. A client with only Pandorical, which is how players meet it on a server, builds a stand-in instead; that path is Pandorical's to test.

## What to check by hand

- **A shark looks like a shark**, and a pup like a pup that changes skin as it grows: `/summon cobble-jaws-justfatlard:shark ~ ~ ~ {Age:-24000}` in water.
- **The fishing.** With minedew-fishing installed, `/time set night` and `/weather rain` over deep ocean tilt the size roll toward large, which is the one that comes up alive.
