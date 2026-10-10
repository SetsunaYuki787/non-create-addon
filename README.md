# nonaddon

An unofficial **Fabric addon for Minecraft 1.21.11** that adds a few recipes and quality-of-life
features to [NeedsofNature](https://www.curseforge.com/minecraft/mc-mods/needsofnature) and
[Create (ZurrTum's Create Fly port)](https://github.com/ZurrTum/Create-Fly).

It is a companion mod: it contains no textures, models or content of its own beyond a tiny
placeholder block, and it does nothing useful unless both of those mods are installed.

---

## Features

### 1. Andesite alloy from liquid bottles

`2 iron ingots + 2 liquid bottles` → `9 create:andesite_alloy` (crafting table).

Create's own andesite alloy recipes (crafting + mixing, both the iron and the zinc variant) are
disabled, so this is now the way to make it.

### 2. Rose quartz from liquid bottles

`1 liquid bottle + 8 redstone` → `9 create:rose_quartz` (shapeless).

Create's original rose quartz recipe is disabled.

### 3. Brass from copper and liquid

`1 copper ingot + 200 mB of NoN liquid` → `2 create:brass_ingot`, in a heated **Create mixer**
(basin). Create's original brass mixing recipe is disabled.

Liquid bottles from *any* creature can be used: when a liquid bottle is drained in a Create item
drain, the resulting `create:potion` fluid is recoloured to NeedsofNature's fallback colour, so a
single recipe matches every liquid while still rejecting ordinary vanilla potions.

### 4. Horse collector redstone pulse

When the NeedsofNature **horse liquid collector** finishes its animation, it emits a single
redstone pulse into the surrounding 3×3 area, so the collector can be automated with Create
deployers, hoppers or any other redstone machinery.

### 5. `nonaddon:animation_speed`

A beneficial status effect (levels 1–3) that makes NeedsofNature animations play faster **and**
finish sooner:

| level | animation speed | total duration |
|-------|-----------------|----------------|
| 1     | 1.5×            | ÷ 1.5          |
| 2     | 2.0×            | ÷ 2            |
| 3     | 2.5×            | ÷ 2.5          |

Wearing Create's **cardboard armor** grants one level per worn piece (helmet, chestplate,
leggings, boots), capped at level 3 — so the fourth piece has no additional effect. Taking the
armor off removes the effect again. An effect handed out by something else is never stolen or
shortened.

The level can also be applied manually, e.g. `/effect give @s nonaddon:animation_speed 60 2`.

## Requirements

| mod | version used |
|-----|--------------|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.141.6+1.21.11 |
| NeedsofNature | 1.5.2+1.21.11 |
| Create (Create Fly) | 6.0.9-5 |
| GeckoLib | 5.4.5 (needed by NeedsofNature) |

## Install

Drop `nonaddon-1.0.0.jar` into your `mods/` folder. Client and server.

## Repository layout

```
src/com/nonaddon/            mod code (initializer, effect, cardboard armor, pulse block)
src/com/nonaddon/mixin/      mixins (drain colour, animation duration, collector interaction)
src/com/nonid/               small bridge into NeedsofNature's animation events
res/                         fabric.mod.json, mixins config, assets, data packs
res/data/create/recipe/      the original Create recipes, disabled via a false load condition
build.ps1                    build script used during development
```

## Building

This project was built without Gradle, using a plain `javac` + [TinyRemapper](https://github.com/FabricMC/tiny-remapper)
pipeline (`build.ps1`): compile against the named Minecraft jar plus the deobfuscated dependency
jars, remap the classes to intermediary, then add `res/` to the jar.

`build.ps1` expects that toolchain (Minecraft jar, mappings, remapper, deobfuscated dependency
jars) in a fixed local directory, so it will not run as-is on a fresh checkout. For normal
development use Fabric Loom instead and treat `res/` + `src/` as the actual project content.

## Notes

* Unofficial and unaffiliated with either project. NeedsofNature and Create are the work of their
  own authors and are **not** included here.
* `res/data/create/recipe/nonaddon_condition_probe.json` is a small diagnostic recipe that
  verifies the "override a recipe from another mod" mechanism still works; if it ever fails to
  load, the disabled recipes above have stopped working too.
* No license file yet — ask before reusing the code.
