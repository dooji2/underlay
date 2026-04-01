**Underlay** is a Minecraft mod that lets you place carpets (and more) under any block with space beneath it (e.g. chests, beds, signs, torches, etc.)!

Just **right click a block face with a block to place it**, (for example, under an upside-down stair), and you're done! Right click again with another item to replace it quickly, or left click it to remove it. Works in **Survival** too!

![Preview](https://i.postimg.cc/50nR26v0/ezgif-1478728b20dbf1.gif)

By default, you can place the following blocks as overlays: carpets, trapdoors, buttons, rails, slabs and pressure plates. **To expand this to other blocks**, such as grass, see "For Developers - Through Datapacks" (for players).

#### Join the Discord server for sneak peeks on upcoming updates!
[![Join now!](https://img.shields.io/badge/Join%20now-Discord-7289DA?logo=discord&logoColor=white&style=plastic)](https://discord.gg/UPmnyM9YcY)

---

### How does this work?
Underlay adds these things called "overlays" ~~but they're really more like underlays xd~~, essentially they are fake versions of the item you place - which lets you place two things in the same block space!

### How to add support for more blocks (or exclude some)
In your Minecraft folder you can find `config/Underlay/underlay.json`. Use `overlay_blocks` to add blocks, and `exclude_blocks` to exclude them, like this:
```
{
  "overlay_blocks": [
    "minecraft:short_grass",
    "mymod:shiny_block"
  ],
  "exclude_blocks": [
    "minecraft:oak_button",
    "minecraft:rail"
  ]
}
```
