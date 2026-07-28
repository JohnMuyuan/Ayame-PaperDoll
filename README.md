# Ayame PaperDoll (Muyuan Fork)

> **This is an unofficial, independently maintained fork. It is NOT the official Ayame PaperDoll release.**
> Upstream project: [AyameMC/Ayame-PaperDoll](https://github.com/AyameMC/Ayame-PaperDoll) by HappyRespawnanchor.
> Upstream is itself a fork of [Extra Player Renderer](https://modrinth.com/mod/extraplayerrenderer) by LucunJi.

A highly configurable paper doll HUD mod for **Minecraft 26.1**, supporting **Fabric** and **NeoForge**.
It renders your player model in a corner of the screen with extensive customization: position, size,
rotation modes (including Rotation Unlock/Lock), name tag controls, lighting, effect particles, and a
Visual Configuration Editor.

## Distinguishing this fork from upstream

- The mod list display name is **`Ayame PaperDoll (Muyuan Fork)`** — upstream shows `Ayame PaperDoll`.
- The version scheme is **`<upstream-base>+muyuan.<fork-release>+<mc-version>`** (e.g. `4.4.3.1+muyuan.1+26.1`).
- The internal mod id stays `ayame_paperdoll` intentionally so configs and saves remain compatible with
  upstream. **Do not install this fork side-by-side with the official build.**

## Features inherited from upstream

- Paper doll rendered in any screen corner (presets) or at a custom offset
- Rich config screen accessible via Mod Menu / NeoForge mod list, or a keybind
- Toggle keybind (default **F8**)
- Rotation modes: Unlock, Lock, Full/Half player-motion sync
- Name tag rendering with independent mirroring, size and offset
- Posture offsets (sneak / swim / crawl / elytra)
- World-light based shading, hurt flash, hand swing, effect particles
- Visual Configuration Editor (drag to move/rotate, wheel to scale)

## Screenshots (upstream)

![Default Position and Style](https://cdn.modrinth.com/data/cached_images/6e7af18771d006eda3077d33800250bc80cdd647.png)
![Config Screen](https://cdn.modrinth.com/data/cached_images/44e1b59e5019df8f03c34fa5d677841eacc26896.png)
![Config Screen](https://cdn.modrinth.com/data/cached_images/c3e90083ab02fc63bda0da3bab7b851f8308382e.png)
![Visual Configuration Editor](https://cdn.modrinth.com/data/cached_images/0bda8a26fc0822669f736d4caa0fee140354bdb2.png)

## Building

Requirements: **JDK 25**.

```bash
./gradlew build        # or: sh build.sh (also collects jars into ./result/)
```

Artifacts land in `fabric/build/libs/` and `neoforge/build/libs/`.

## Versioning & upstream sync

This fork was cut from upstream version `4.4.3.1` for Minecraft `26.1`.
Fork releases append `+muyuan.N` to the upstream base version so the lineage is always visible.
Changes specific to this fork are listed in [CHANGELOG.md](CHANGELOG.md).

## License

Licensed under **LGPL-3.0-or-later**, inherited from upstream (see `COPYING` and `COPYING.LESSER`).
Copyright for upstream code: LucunJi (original author of Extra Player Renderer), HappyRespawnanchor
(Ayame PaperDoll), and contributors. Fork modifications: JohnMuyuan.

## Links

- This fork: <https://github.com/JohnMuyuan/Ayame-PaperDoll>
- Upstream source: <https://github.com/AyameMC/Ayame-PaperDoll>
- Upstream pages (for reference): [Modrinth](https://modrinth.com/mod/ayame-paperdoll) ·
  [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ayame-paperdoll) ·
  [McMod](https://www.mcmod.cn/class/17015.html)
- Original mod: [Extra Player Renderer](https://modrinth.com/mod/extraplayerrenderer)
