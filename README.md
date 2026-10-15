# Cobble Jaws

Sharks in the warm seas. Leave one alone and it leaves you alone. Hit it, or swim too close to its pup, and it comes for you.

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

**It is not a dolphin.** A shark breathes water, so it never breaks off a chase to surface. It doesn't swim alongside you handing out Dolphin's Grace, won't take a fish from your hand or lead you to treasure, and doesn't push items around.

**Killing one** drops two or three whitefish (raw cod), cooked if it was on fire. A pup drops nothing.

**There is a Shark Spawn Egg**, with the other spawn eggs in creative. `/summon cobble-jaws-justfatlard:shark` works too.

## Built On The Dolphin

A shark is its own animal, `cobble-jaws-justfatlard:shark`, with its own spawns, loot and behaviour. What a player's game is told to draw is a dolphin in a shark's skin, with a dark back and a white belly: Pandorical lets a server's mod add a new mob as long as it stands on one the game already knows how to move, and the dolphin is the one built like a shark already. So it swims, leaps and follows boats exactly as a dolphin does.

## Spawning

Sharks are added on top of the dolphins, not in place of them: half as common as dolphins in the warm seas and as common as them in the open ocean, in pods of one or two.

## Fishing For Them

With [Minedew Fishing](https://github.com/fatlard1993/minedew-fishing) 1.1.0 or later installed, a shark can take your bait wherever sharks live: about one bite in thirty, and one in sixteen in deep water. It fights its own way on the bar, with hard lunges and every so often a frenzy, and its size is rolled like any fish's, with rain, night and deep water all pushing it bigger.

- **Small or medium:** landed like a fish and cut into **whitefish fillets**, two for a small one and three for a medium one.
- **Large or trophy:** not landed. It comes up out of the water **alive, angry and coming at you**, bigger than a wild shark (a trophy is half again as big) and set on you. The fillets are whatever you can take off it. The first one earns **We're Gonna Need a Bigger Boat**.

Without Minedew Fishing there is no shark fishing; the sharks are simply in the sea.

## Pandorical

Pandorical is required on the server, and on a player's client to see sharks as sharks; the mod will not load without it on the server.

**Without Pandorical client-side**, a player sees each shark as a dolphin, and it still bites like a shark. The skin is the one thing such a client can't be handed. A dolphin that comes for you is a better warning than a shark you can't see.

## Development

Installing and the art pipeline are in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
