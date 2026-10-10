# Non Create Addon

A companion **Fabric addon for Minecraft 1.21.11** (mod id `non-create-addon`) that adds a few
recipes and quality-of-life features to
[NeedsofNature](https://www.curseforge.com/minecraft/mc-mods/needsofnature) and
[Create (ZurrTum's Create Fly port)](https://github.com/ZurrTum/Create-Fly).

**Download:** grab the compiled mod from the
[Releases](https://github.com/SetsunaYuki787/non-create-addon/releases) page and drop it into
`mods/`.

It is a companion mod: apart from one item texture and a tiny placeholder block it ships no content
of its own, and it does nothing useful unless both of those mods are installed.

---

## 中文简介

这是 **Minecraft 1.21.11（Fabric）** 的附属模组，给 [NeedsofNature](https://www.curseforge.com/minecraft/mc-mods/needsofnature)
和 [Create（ZurrTum 的 Create Fly 移植版）](https://github.com/ZurrTum/Create-Fly) 补上几项内容：

1. **安山合金**：`2 铁锭 + 2 精液瓶` → `9 安山合金`（Create 原有配方一并禁用）
2. **玫瑰石英**：`1 精液瓶 + 8 红石` → `9 玫瑰石英`（原配方禁用）
3. **黄铜锭**：`1 铜锭 + 200 mB 精液` → `2 黄铜锭`，需要加热的 Create 搅拌器（原配方禁用）；
   任意生物的精液瓶都可使用——倒进分液器后统一取兜底色，因此仍然拒绝原版药水
4. **种马取精器**：动画结束后向周围 **3×3** 区域输出一次红石脉冲，可用 Create 部署器自动化
5. **`non-create-addon:animation_speed`**：1/2/3 级让 NoN 动画以 **1.5× / 2× / 2.5×** 播放，**总时长同步缩短**；
   穿 Create 的**纸板盔甲**每件给一层（上限 3 级），脱掉即消失
6. **安山合金塞子**：`1 钻石 + 1 安山合金` 合成，放进 Trinkets 的**肛门/阴道**槽位（`legs/a`、`legs/v`），
   戴上后等同于 Create 的**工程师护目镜**（贴图沿用 NoN 铁塞子的形状，头部改为安山合金配色）

依赖：Minecraft 1.21.11、Fabric Loader 0.19.5、Fabric API 0.141.6+1.21.11、NeedsofNature 1.5.2+1.21.11、
Create 6.0.9-5、GeckoLib 5.4.5；**塞子需要 [Trinkets](https://modrinth.com/mod/trinkets) 3.11.3**（没有它也照常合成，只是戴不上）。
从 [Releases](https://github.com/SetsunaYuki787/non-create-addon/releases)
下载 `non-create-addon-1.0.0.jar` 放进 `mods/` 即可。

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

### 5. `non-create-addon:animation_speed`

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

The level can also be applied manually, e.g. `/effect give @s non-create-addon:animation_speed 60 2`.

### 6. Andesite plug

A wearable plug for NeedsofNature's **anal** and **vaginal** slots (`trinkets:legs/a` and
`trinkets:legs/v`), crafted from `1 diamond + 1 andesite alloy`.

While it is worn it counts as Create's **engineer goggles**, so the goggles' in-world information
(Ponder hints, contraption details, goggles overlays) shows up without wearing a helmet — the effect
is granted by registering an "is wearing goggles" predicate with Create, not by occupying the head
slot.

The texture keeps NeedsofNature's `iron_plug` shape but paints the insertable head in andesite
alloy's colours, matching the recipe.

Wearing it needs [Trinkets](https://modrinth.com/mod/trinkets); without that mod the plug is still
craftable, it just has no slot to go into.

## Requirements

| mod | version used |
|-----|--------------|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.141.6+1.21.11 |
| NeedsofNature | 1.5.2+1.21.11 |
| Create (Create Fly) | 6.0.9-5 |
| GeckoLib | 5.4.5 (needed by NeedsofNature) |
| Trinkets | 3.11.3 (optional, needed to wear the andesite plug) |

## Install

Drop `non-create-addon-1.0.0.jar` into your `mods/` folder. Client and server.

## Repository layout

```
src/com/nonaddon/            mod code (initializer, effect, cardboard armor, plug, pulse block)
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
* `res/data/create/recipe/non-create-addon_condition_probe.json` is a small diagnostic recipe that
  verifies the "override a recipe from another mod" mechanism still works; if it ever fails to
  load, the disabled recipes above have stopped working too.
* A companion mod: it registers an invisible pulse block and ships one item with its texture, and
  it does nothing useful unless both NeedsofNature and Create are installed.
* Licensed under the MIT License — see [LICENSE](LICENSE).

