# SuperLoot

A Fabric mod for **Minecraft Java 26.3** that scatters loot chests across the Overworld surface.

- The world is split into **256×256 block areas** (16×16 chunks each). Every area gets 0–3 chests, depending on your setting.
- Chests go on the **surface**: on the ground (under trees, not on top of them), or on the **sea, lake or river floor** when the spot is under water. Underwater chests are waterlogged, so there's no air bubble.
- Each chest is in a different chunk of its area, so they're spread out.
- If a spot is no good (a lava lake, for example), it tries up to 48 other spots nearby before giving up.
- Caves are not touched.

## Settings

Open them from **Mods → SuperLoot → (settings button)**. This needs [Mod Menu](https://modrinth.com/mod/modmenu) installed. You can also edit `config/superloot.json` directly, which is also how you change settings on a dedicated server.

| Setting | Options |
|---|---|
| **Chests per Area** | Low (0–1) · **Medium (1–2)** · High (2–3) |
| **Loot Quality** | **Common** (everyday supplies) · Mixed (about 1 in 4 chests is rare) · Rare (treasure) |

The defaults are in bold. Changes apply to **land that hasn't been generated yet**. Chests already in the world stay as they are.

## Loot

- **Common** (`data/superloot/loot_table/chests/common.json`): food, torches, coal, arrows, string, iron/gold/copper, redstone, lapis, and sometimes a stone/iron tool, bow, bucket, shield, saddle, name tag or enchanted book.
- **Rare** (`data/superloot/loot_table/chests/rare.json`): diamonds, emeralds, gold, XP bottles, ender pearls, enchanted diamond and iron gear, enchanted books, and sometimes golden apples, a totem of undying, heart of the sea, music discs, or (very rarely) an enchanted golden apple or netherite scrap.

To change what's in the chests, edit those two JSON files, or override them with a data pack using the same paths.

## Building the .jar

Pick **one** of these.

### Option A: let GitHub build it (no installs)
1. Create a new repository on github.com and upload everything in this folder, including the hidden `.github` folder.
2. Open the **Actions** tab. The "build" workflow runs on its own. If it doesn't, choose **build → Run workflow**.
3. When it turns green, open the run and download **SuperLoot-jar**. Inside is `superloot-1.0.0.jar`, which is the mod. Ignore any `-sources` jar.

### Option B: build on your computer
1. Install **Java 25** (e.g. Microsoft Build of OpenJDK 25, or Adoptium Temurin 25).
2. Open a terminal in this folder and run `gradlew build` (Windows) or `./gradlew build` (Mac/Linux).
3. The mod is at `build/libs/superloot-1.0.0.jar`.

To try it in a test game, run `gradlew runClient`.

## Playing

Install Fabric Loader for 26.3, then put these in your `mods` folder:
- `superloot-1.0.0.jar`
- [Fabric API](https://modrinth.com/mod/fabric-api) (required)
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional, for the settings button)

On a server, only the server needs SuperLoot. Players can join with an unmodded client.

## Publishing on Modrinth

1. On modrinth.com, choose **Create a project → Mod**, name it SuperLoot, and use `src/main/resources/assets/superloot/icon.png` as the icon.
2. Upload `superloot-1.0.0.jar` as a version with loader **Fabric** and game version **26.3**.
3. Add **Fabric API** as a *required* dependency and **Mod Menu** as *optional*.
4. Set environments to: server **required**, client **optional**.
5. The license is MIT (see `LICENSE`). Change it there and in `fabric.mod.json` if you prefer another one.

## Changing the version or names

- Mod version: `version=` in `gradle.properties`
- Minecraft / Fabric versions: `gradle.properties`, plus `"minecraft"` in `src/main/resources/fabric.mod.json`
- Chest rules (area size, attempts, Mixed rare chance): constants at the top of `src/main/java/com/superloot/world/LootChestFeature.java`
