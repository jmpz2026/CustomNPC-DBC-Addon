> **This is an unofficial fork.** It is **not** the official CustomNPC+ DBC Addon.
> The original mod, its code and its assets belong to **KAMKEEL (Kam)**, **bigguy345 (Goatee)**
> and **somehussar (Hussar)**. Official project: https://github.com/KAMKEEL/CustomNPC-DBC-Addon
> — please report bugs of the original mod there, and bugs of this fork here.


## 👋 Welcome to the CustomNPC+ DBC Addon Repository.

----------------

<a href="https://discord.gg/pQqRTvFeJ5"> <img src="https://img.shields.io/badge/KAMKEEL_Discord-7289DA?style=for-the-badge&logo=discord&logoColor=white" width="400" height="60"> </a>
<a href="https://ko-fi.com/kamkeel"> <img src="https://img.shields.io/badge/Support_Me_|_Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white" alt="Support Me"  width="400" height="60"> </a>

[![Download CustomNPC+](https://img.shields.io/badge/CustomNPC+-0081CB?style=for-the-badge&logo=material-ui&logoColor=white)](https://modrinth.com/mod/customnpc-plus)
[![Download MPM+](https://img.shields.io/badge/MorePlayerModels+-0081CB?style=for-the-badge&logo=material-ui&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/moreplayermodels-plus)
[![Download PluginMod](https://img.shields.io/badge/Plugin_Mod-0081CB?style=for-the-badge&logo=material-ui&logoColor=white)](https://github.com/KAMKEEL/Plugin-Mod)

----------------

### 🔧 About this fork
This repository is a **fork** of [KAMKEEL/CustomNPC-DBC-Addon](https://github.com/KAMKEEL/CustomNPC-DBC-Addon),
maintained by Machitos for the **Dragon Block Resurrection** server. It is based on upstream `1.1.5`
(`95d3d1d6`) and only adds the changes listed below; everything else is the original work.

**Credits — all credit for the mod goes to its authors:**
- **KAMKEEL (Kam)** — creator of CustomNPC+ and of this addon
- **bigguy345 (Goatee)** — author
- **somehussar (Hussar)** — author
- **@PopsicleNate** — textures
- **@mtbarr** — bug fixes
- **kla_xi** — Chinese language file
- **Noppes** — original author of CustomNPCs
- and every contributor of the [original repository](https://github.com/KAMKEEL/CustomNPC-DBC-Addon/graphs/contributors)

All rights to the original work remain with its authors. This fork is distributed only to Dragon Block
Resurrection players through the server launcher, **with the permission of KAMKEEL** (granted on
2026-10-05), on the condition that credit is given and that it is not published on mod hosting sites
such as CurseForge or Modrinth.

**Changes in 1.1.6 (fork):**
- Support for extra playable races added to JRMCore at runtime by other mods (race IDs
  6 and up): race ranges are read from `JRMCoreH.Races` instead of being fixed to 0..5
  (`DBCRace.isValidRace`, forms, scripting API, racial skill costs).
- Custom forms on an extra race no longer throw a `NullPointerException`: its attribute
  formula is asked to the mod that adds the race through `kamkeel.npcdbc.compat.ExtraRaces`
  (soft dependency, resolved by reflection; without it the base attribute is used).
- The form editor, the parent-form picker and the Divine config list the extra races.
- Build toolchain updated so the project compiles with the current GTNH infrastructure.

### ⬇️ Downloads
- **Modrinth**: [NONE]()
- **CurseForge**: [NONE]()

### 🔹 Installation
This mod is an addon mod to **CustomNPC+**. It requires **DBC**, **JRMCore**, **JBRAClient**, and **CustomNPC+** to be installed alongside it.
It also requires a Mixin Mod like UniMixins.

## Cloning / Compiling / Building
1. Run gradlew build in console and wait BUILD SUCCESSFUL
2. Import project into IntelliJ
3. Wait for the IntelliJ to build the environment
