**Underlay** is a Minecraft mod that lets you place carpets (and more) under any block with space beneath it (e.g. chests, beds, signs, torches, etc.)!

Just right click a block face with a block to place it, (for example, under an upside-down stair), and you're done! Right click again with another item to replace it quickly, or Shift + right click with an empty hand to remove it. Works in **Survival** too!

![Preview](https://i.postimg.cc/50nR26v0/ezgif-1478728b20dbf1.gif)

---

### How does this work?
Underlay adds these things called "overlays" ~~but they're really more like underlays xd~~, essentially they are fake versions of the item you place - which lets you place two things in the same block space!

### For Developers
This mod adds an API that lets you mark your blocks as "yes you can behave like this too". Just add my mod as a dependency, either from JitPack or Modrinth's maven, import `com.dooji.underlay.UnderlayApi`, and register the blocks you want to be able to placed under stairs/doors etc. with `registerOverlayBlock`.
