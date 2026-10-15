# Cobble Jaws

Sharks in the warm seas, built out of the dolphin the game already has. Leave one alone and it leaves you alone. Hit it, or swim too close to its pup, and it comes for you.

## What This Mod Does

The ocean has dolphins, squid, fish and a turtle on the beach, and nothing in it that is a reason to look down when you are swimming. This puts a shark in the water: something that minds its own business, eats the fish, and is very much worth not annoying.

Server-side; Pandorical carries the client's half.

## The Shark

| | Shark | Dolphin, for comparison |
|---|---|---|
| Where | warm, lukewarm and deep lukewarm ocean, ocean, deep ocean | the same |
| Size | a little bigger than a dolphin | |
| Health | 23 | 10 |
| Bite | 4.6 | 3 |
| Temper | ignores you unless hit, or unless you come near its pup | ignores you unless hit |
| Eats | fish, squid, small animals in the water | nothing |
| Breathes | water | air |

**Passive unless provoked.** A shark will swim right past you. Hit one and it turns on you, and so does the rest of its pod.

**Near a pup, it is provoked already.** Sharks come in pods of one or two, and now and then one is a pup. Come within eight blocks of a pup and any adult shark near it comes for you, the way a polar bear does over its cub.

**It hunts.** Every so often a shark looks around for something to eat: cod, salmon, tropical fish, squid, glow squid, or any small animal in the water with it, such as a chicken, a rabbit, a frog or a calf that has wandered in. It eats what it kills, so nothing is left floating, and a meal heals it. It leaves pufferfish alone, and it leaves pets alone.

**It is not a dolphin.** A shark breathes water, so it never breaks off a chase to surface. It gives no Dolphin's Grace when you swim beside it, won't take a fish from your hand, and doesn't push items around.

**Killing one** drops whitefish: raw cod, more for a bigger shark, on top of the cod a dolphin already drops. A pup drops nothing.

## None Of It Is A New Animal

A shark is a `minecraft:dolphin`. A server-side mod can't hand a vanilla client a new creature to draw, so a new animal would be invisible to anybody not running the mod themselves. What a client *can* be told is that a familiar animal is a different size and wearing a different skin. So a shark is a dolphin, resized through the game's scale attribute and reskinned through Pandorical's entity overlay, with a dark back and a white belly.

Everything that follows from that is deliberate. It swims, leaps, follows boats and calls its pod in when hit exactly as a dolphin does, because it is one. A command that looks for dolphins finds sharks too.

**It is still called a Shark.** Death messages, block-tip's card and anywhere else the game names an animal say Shark. It is not a custom name, which would float over the animal and stop it ever despawning.

Which dolphin is a shark is written on it as a scoreboard tag, `shark`. Tags are saved with the entity by the game itself, survive this mod being removed, and can be set by hand. A shark is summoned by tag:

```
/summon minecraft:dolphin ~ ~ ~ {Tags:["shark"]}
```

**The spawn egg hatches a dolphin**, as its name says. Used on a shark, it hatches a shark pup.

## Spawning

Sharks are added on top of the dolphins, not in place of them. Each ocean that has dolphins gets one more dolphin spawn, and a matching share of the pods that arrive are sharks, so the dolphins are as common as they were. Sharks end up half as common as dolphins in the warm seas and as common as them in the open ocean. A pod is all sharks or all dolphins, never a mix.

## Fishing For Them

With [Minedew Fishing](https://github.com/fatlard1993/minedew-fishing) 1.1.0 or later installed, a shark can take your bait wherever sharks live: about one bite in thirty, and one in sixteen in deep water. It fights its own way on the bar, with hard lunges and every so often a frenzy, and its size is rolled like any fish's, with rain, night and deep water all pushing it bigger.

- **Small or medium:** landed like a fish and cut into **whitefish fillets**, two for a small one and three for a medium one.
- **Large or trophy:** not landed. It comes up out of the water **alive, angry and coming at you**, bigger than a wild shark (a trophy is half again as big) and set on you. The fillets are whatever you can take off it. The first one earns **We're Gonna Need a Bigger Boat**.

Without Minedew Fishing there is no shark fishing; the sharks are simply in the sea.

## Pandorical

Pandorical is required on the server; the mod will not load without it. The shark skins are sent to Pandorical clients as entity overlays.

**Without Pandorical client-side**, a player still meets the sharks with the right size, temper, appetite and drops. They just look like dolphins, because the client was never handed the skin. Nothing breaks, but the dolphin you thought you could swim up to is the thing to look out for.

## Development

Installing and the art pipeline are in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
