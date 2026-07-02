# Void Mod

A Fabric mod for Minecraft 1.21.11 that replaces the blade of the **netherite sword** and the head of the **netherite spear** with a live end-portal void texture, while giving both weapons a custom hilt/handle reskin.

## Features

- **End-portal blade** — the blade region of the netherite sword and the head of the netherite spear render with the animated end-portal void effect
- **Custom hilt texture** — the handle of both weapons uses a custom reskin separate from the blade
- **Enchant glint on handle only** — if the weapon is enchanted, the glint appears only on the hilt, not over the void blade
- **Inventory + in-hand support** — the effect works correctly in the GUI, when held in first/third person, and when dropped on the ground

## Requirements

- Minecraft 1.21.11
- [Fabric Loader](https://fabricmc.net/) 0.15.0+
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 21+

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11
2. Drop `voidmod-*.jar` into your `.minecraft/mods` folder alongside Fabric API
3. Launch the game — the netherite sword and netherite spear will automatically use the void texture

## Building from source

```bash
git clone https://github.com/abdyzam50-rgb/voidblade.git
cd voidblade
./gradlew build
# Output jar is in build/libs/
```

## How it works

The mod hooks into Fabric's `ModelLoadingPlugin` to replace the item model for the netherite sword and spear at bake time. It pre-bakes two sets of quads per weapon:

- **Hilt quads** — opaque pixels rendered with a translucent render type so the custom handle texture shows correctly in all lighting conditions
- **Portal blade quads** — transparent texels in the blade/head region rendered with `RenderTypes.endPortal()`, producing the live void effect

Side extrusions are selectively suppressed to avoid geometry artifacts along the diagonal spear shaft.

## License

CC0-1.0 — public domain, do whatever you want with it.
