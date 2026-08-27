# Ayame PaperDoll: JohnMuyuan Edition — Fabric 1.20.1

Backport of the Fabric 26.1 mod in this repository to Minecraft 1.20.1. Same features, no new ones.

## Build

```
cd fabric-1.20.1
./gradlew build
```

Output: `build/libs/ayame-paperdoll-johnmuyuan-edition-fabric-1.0.0+1.20.1.jar`

Requires Fabric Loader 0.16.14+ and Fabric API 0.92.x for 1.20.1. Mod Menu 7.x is optional.
The build itself runs on Gradle 9.5 (its own wrapper) with a Java 17 toolchain; Fabric Loom 1.17.20
and official Mojang mappings, so the sources keep the same class/field names as the 26.1 version.

## What changed against the 26.1 sources

1.20.1 has no GUI render-state pipeline, so the doll cannot be rendered into an off-screen
picture-in-picture texture and blitted. It is drawn straight into the HUD instead, the way
`InventoryScreen.renderEntityInInventory` draws the inventory player. Consequences:

* **Mirroring** is a negative X scale on the model matrix instead of a flipped texture blit.
  With `mirrored` on and `name_tag_mirrored` off, the name tag gets its own unmirrored pass, as before.
* **Name tags** are drawn by the mod itself (position, scale and offsets included), and the vanilla
  name tag is suppressed while the doll renders (`EntityRendererMixin`).
* **`max_refresh_rate`** no longer does anything — there is no cached texture to reuse — so it is
  kept only for config-file compatibility and hidden from the config screen.
* **`display_priority`**: 1.20.1 exposes a single HUD hook, so `DEFAULT` and `HIGH` draw at the same
  point; `HIGHEST` still draws on top of open screens (`ScreenMixin`).
* **Config screen widget skin**: the custom widget textures needed the 1.20.2+ GUI sprite system,
  which does not exist in 1.20.1. The config screen uses vanilla widget textures.
* Effect/totem particles on the doll, the visual config editor, presets, world light, poses,
  rotation modes, spectator switching and the config file format are unchanged.

Config file: `config/ayame_paperdoll_v0.json` — the same format as the 26.1 version.
