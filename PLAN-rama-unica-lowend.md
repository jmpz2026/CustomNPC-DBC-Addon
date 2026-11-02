# Plan: one npcdbc branch that works on weak PCs

## Context

Players with old Intel iGPUs (log: lukitas2718, Intel HD 4000, driver 10.18.10.5161) get a black screen or the game never loads with the **normal** npcdbc jar. The **optimized** jar works for them.

Today there are 2 branches and 2 jars with the same name (`npcdbc-5.3.0.jar`):
- `main-version` is the normal jar. The launcher channels FULL and experimental ship it.
- `optimized-version` is main-version plus commit `c65354a4` (perf). The LITE channel ships it.

Why optimized works:
- DbrOptimizer, in light mode, sets `EnableOutlines=false` / `EnableBloom=false` by reflection.
- Only optimized makes `useStencilBuffer` respect that, through `outlineVisible()`.
- The normal jar still turns on stencil per entity, and DbrOptimizer did not request a stencil buffer on INTEL_OLD.

The real root is deeper: npcdbc trusts the GPU and never protects itself.

Goal:
- 1 branch (`main-version`) and 1 jar for every channel.
- npcdbc detects GPU capabilities and degrades by itself.
- A public quality API, which DbrOptimizer uses instead of reflection.
- A preset plus per-option manual overrides.

## User decisions (2026-11-01)

- Final branch: `main-version`. `optimized-version` is archived with a tag.
- Low-spec control: the most scalable option. npcdbc protects itself and exposes an API, and DbrOptimizer consumes it.
- Scope: black screen/stencil, NPE in the main menu, DbrOptimizer side, config unification.
- Launcher: the same jar on FULL, LITE and experimental.
- Config: a quality preset, plus each option editable by hand; a manual value wins.
- Tests: at the end, with the player's real machine.

## Faults found (main-version)

1. **Stencil ungated.** `glEnable(GL_STENCIL_TEST)`/`glClear(STENCIL)`/`glStencil*` only check `useStencilBuffer`. Places:
   - `RenderEventHandler.java` lines 96, 130, 172-403
   - `AuraRenderer.java:138`, `MixinRenderAura2.java:142`, `MixinModelMPM.java:95`, `MixinRenderNPCInterface.java:16`
   - `PlayerDataUtil.useStencilBuffer` (`:143-178`) ignores `PostProcessing.stencilAvailable` and `EnableOutlines`.
   - The player path and the hand path never `glDisable(GL_STENCIL_TEST)`. Only the NPC path does (`:352`).
2. **`detectStencil` catch returns true** (`PostProcessing.java:713`). It must be false.
3. **`PostProcessing.init`** (`:470`) mixes 2 jobs: `setupDepthAndStencil()` for the MAIN framebuffer, and allocation of the RGBA16F/MRT/blit FBOs.
   - No capability checks: no `ContextCapabilities`, and `gameSettings.fboEnable` is not checked.
   - `glCheckFramebufferStatus` only logs (`:541,551,576,634`). Incomplete FBOs stay in use.
   - Memory: skipping all of `init` black-screens transforms (commit e2f524c8). The 2 jobs must be split, not skipped.
4. **Shaders** (`ShaderHelper.java`):
   - A failed compile is only logged (`:192`). Link failure returns program 0 with leaks (`:154-163`).
   - `glValidateProgramARB` runs at load time (`:159`).
   - Nothing sets `bloomSupported=false`.
   - The `modern/*` shaders (#version 330) are compiled but never used.
5. **GL state leaks.**
   - `PostProcessing.bloom` (`:159-285`) has no try/finally, so the FBO, viewport and projection can stay changed.
   - Scissor is not restored on the return at `:166`.
   - The `startBlooming`/`endBlooming` callers (`RenderEventHandler:171`, `MixinModelMPM:94`) have no finally.
   - With FBOs off, `startBlooming` in the GUI binds `MAIN_BLOOM_BUFFER` with an invalid attachment. This is the likeliest black-screen path.
6. **OptiFine leak.** `MixinShaders.java:40` re-runs `init` without `delete()`.
7. **Menu NPE.** bspkrs draws a fake player. `DBCDataUniversal.get` takes the server branch, then `PlayerDataUtil.getDBCInfo` gets null from CNPC `PlayerData` and throws. This runs every frame of the menu.
8. **Dead code.**
   - `client/shader/FrameBuffer.java` is never constructed and has wrong `glTexImage2D` arguments.
   - 11 shaders declare `uniform int time` but get `glUniform1f` (memory: shader-time-uniform-float).
9. **Hidden coupling.** DbrOptimizer writes `ConfigDBCClient` fields by reflection (`RecortesTerceros.java:43-52`) and `PostProcessing.bloomLevel` (`NpcdbcBloom.java:81`). Nothing declares this contract.

## Steps

### F0 – Tracking
- Copy this plan to `CustomNPC-DBC-Addon/PLAN-rama-unica-lowend.md`, with a dated log and the next step.
- Add a pointer to it in the repo `CLAUDE.md`.

### F1 – Unify branches (git, local)
1. `git checkout main-version && git merge --ff-only optimized-version`. A fast-forward is possible because main has no commits that optimized lacks.
2. `git tag archive/optimized-version optimized-version`.
3. Move `OPTIMIZATIONS.md` to `docs/claude/rendimiento.md`, without the "Ramas" section, and add 1 index line in CLAUDE.md.
4. Push, change the GitHub default branch to main-version, and delete the remote `optimized-version`: **ask for confirmation before each one**.

### F2 – GPU capabilities (new: `client/shader/GlCaps.java`)
- Computed once after the display is created, and recomputed on `init` after a resize.
- Fields:
  - `fbo` = `OpenGlHelper.framebufferSupported && gameSettings.fboEnable`
  - `stencil` = `detectStencil()`, with the catch returning false
  - `floatTex` = GL30 or `ARB_texture_float`
  - `mrt` = `GL_MAX_DRAW_BUFFERS >= 3`
  - `blit` = GL30 or `EXT_framebuffer_blit`
  - `shaders` = blur/additiveCombine/downsample13 compiled and linked OK
- Debug overrides by system property for tests: `-Dnpcdbc.caps.nostencil`, `.nofloat`, `.nomrt`, `.noshaders`.
- One log line: `npcdbc GL caps: fbo=.. stencil=.. float=.. mrt=.. blit=.. shaders=..`.

### F3 – Root fixes for the black screen
1. **Split `PostProcessing.init`.**
   - `setupDepthAndStencil()` always runs when `GlCaps.fbo`.
   - The heavy FBOs are allocated only if `fbo && shaders`.
   - Format: `GL_RGBA16F` if `floatTex`, else `GL_RGBA8`.
   - MRT and blit only if supported. Without them, bloom is disabled.
   - Any status other than `GL_FRAMEBUFFER_COMPLETE` triggers `delete()`, `bloomSupported=false` and a log line.
   - Before allocating, check whether the aura FBOs (`:575`) are needed even without bloom; if not, allocate them lazily.
2. **ShaderHelper.**
   - A compile or link failure returns 0, frees the objects, and sets `GlCaps.shaders=false`.
   - Remove the load-time `glValidateProgramARB`.
   - Do not compile `modern/*`.
3. **Stencil.**
   - `useStencilBuffer` returns false if `!GlCaps.stencil`. It also takes `outlineVisible()`, which is already in optimized.
   - The outline uses the inverted hull when there is no stencil (already exists: `OutlineRenderer:239`).
   - The aura is drawn without a body mask.
   - Close `GL_STENCIL_TEST` on the player and hand paths.
4. **State restore.**
   - try/finally in `bloom()` and around each `startBlooming`…`endBlooming` pair (`RenderEventHandler`, `MixinModelMPM`). The finally rebinds the previous framebuffer, the viewport and the matrices.
   - Restore scissor in every case.
   - `startBlooming` returns if `!bloomSupported || !GlCaps.fbo`.
5. **OptiFine.** Call `delete()` before `init` in `MixinShaders`.
6. **Dead code.** Delete `FrameBuffer.java`. Change `uniform int time` to `float` in the 11 shaders.

### F4 – Menu NPE
- Return early in `RenderEventHandler.renderPotaraWhenFused` if `mc.theWorld == null`.
- `DBCDataUniversal.get`: with no client world, return a blank `DBCData`, not the server branch.
- Null check on `PlayerDataUtil.getDBCInfo(PlayerData)`.

### F5 – Quality: preset + manual + API
- Config `client.cfg`, Rendering section:
  - New `Graphics Quality` = `AUTO | HIGH | MEDIUM | LOW | VERY_LOW` (default `AUTO`).
  - The 6 options from optimized (`ModelDetailMaxDistance`, `AuraMaxDistance`, `AuraParticleDensity`, `OutlineMaxDistance`, `BloomLowResolution`, `BloomMaxLevels`) plus `EnableBloom`/`EnableOutlines` keep their keys.
  - Each option gets a "follow the preset" value (`-1`, or `default` for booleans). A manual value always wins.
  - `AUTO` = HIGH if all caps are OK, LOW if `!stencil || !floatTex || !mrt`, and VERY_LOW if `!fbo || !shaders`.
- Preset table (HIGH = today's look):
  - HIGH: everything on, distances 0.
  - MEDIUM: bloom lowres, 4 levels, aura 48.
  - LOW: outlines off, bloom off, face 24, aura 32, particles 50.
  - VERY_LOW: also particles 25 and aura 16.
- One resolver `RenderSettings` (new, `client/`). The code reads only from it, not from `ConfigDBCClient` directly. Resolution order: manual value > external override (API) > preset > default.
- Public API `kamkeel.npcdbc.api.client.RenderQualityApi`:
  - `API_VERSION`
  - `getCaps()` (read-only)
  - `setExternalPreset(Preset)` / `clearExternalPreset()`, runtime only, not saved
  - `setBloomLevel(int)`, which replaces the write to `PostProcessing.bloomLevel`
  - `getEffective()`
- Keep the old `ConfigDBCClient` fields public and synced for one version, so the current DbrOptimizer keeps working.

### F6 – DbrOptimizer (DbrServerPack)
- `RecortesTerceros` and `NpcdbcBloom`:
  - If `Class.forName("kamkeel.npcdbc.api.client.RenderQualityApi")` exists, light mode calls `setExternalPreset(LOW)` and normal mode calls `clearExternalPreset()`. Bloom uses `setBloomLevel`.
  - Otherwise, the current reflection is the fallback.
- No change to the stencil decision (INTEL_OLD keeps no stencil). npcdbc now handles that by itself.
- Update `DbrServerPack/docs/rendimiento/PLAN.md` (ln 236, §F8): there is only 1 jar, and the Android note.

### F7 – Release
- `modVersion` 5.4.0.
- `./gradlew build`, then 1 jar.
- In DBR-ASSETS, `mods/`, `lite/mods/` and `experimental/mods/` get the same jar, and each manifest gets its sha1/url.
- Commits in Spanish, Conventional Commits, with no AI attribution (dbr-git skill).
- **Confirm before pushing or publishing the manifests.**

## Verification
1. `./gradlew build` OK.
2. Local client, normal case (stencil + float). Check:
   - the caps log line is all true;
   - HIGH looks the same as today;
   - transforming into a custom form with outline gives no black screen (the e2f524c8 repro).
3. Simulated weak PC (`-Dforge.forceDisplayStencil=false -Dnpcdbc.caps.nostencil -Dnpcdbc.caps.nofloat -Dnpcdbc.caps.nomrt`, and FBO off in Video). Check:
   - menu, world, custom-form transform, aura, Form Wheel (bloom) and GUIs with player models;
   - no black screen and no `GL ERROR` from npcdbc;
   - AUTO resolves LOW/VERY_LOW.
4. Main menu with bspkrs: 0 `Couldn't render entity`.
5. DbrOptimizer: toggling light/normal mode moves the preset through the API (log line). With an old npcdbc jar, the reflection fallback still works.
6. Manual override: put `AuraMaxDistance=10` in the cfg and check that it wins over the preset.
7. Final step: send the jar to lukitas2718 (HD 4000) on LITE and on FULL, and check `fml-client-latest.log` (caps line, no NPE, no "did not end cleanly").

## Bitácora

- 2026-11-01: plan aprobado. Diagnóstico desde logs de lukitas2718 (HD 4000) y auditoría del código.

- 2026-11-02: F1 hecho en local (fast-forward, tag `archive/optimized-version`, doc a `docs/claude/rendimiento.md`).
- 2026-11-02: F2–F4 hechos (`0ffd2617`). Cambios frente al plan: `useStencilBuffer` no se toca porque también decide si se dibujan aura, contorno y partículas; solo `glEnable(GL_STENCIL_TEST)`/`glClear(STENCIL)` quedan detrás de `stencilAvailable` (`RenderEventHandler.beginStencil`). Se quitó el FBO de aura y `blankTexture` (nunca se usaban). No se cierra `GL_STENCIL_TEST` en el camino del jugador: sin poder probar, cambiar eso con stencil presente era arriesgado.
- 2026-11-02: F5 hecho (`77f7cc94`). Los campos de `ConfigDBCClient` son los valores efectivos; `RenderSettings.apply()` los calcula. Claves nuevas `Graphics Quality`, `Bloom`, `Outlines` (preset/on/off); `Enable Bloom/Outlines` migran. Botón "Calidad gráfica" en el menú de DBC.
- 2026-11-02: F6 hecho en DbrServerPack (`1ddca559`): `RecortesTerceros` pide `LOW` por la API. `NpcdbcBloom` sin cambios: `bloomLevel` sigue siendo contrato público.
- 2026-11-02: F7 parcial: versión 5.4.0 (`8ab7942f`), `build/libs/npcdbc-5.4.0.jar` compilado. Falta push, rama por defecto en GitHub, borrar la remota, versión de DbrServerPack y manifests de DBR-ASSETS (con confirmación).
- 2026-11-02: push de `main-version` y del tag; rama por defecto del fork = `main-version`; `optimized-version` borrada (local y remota). DbrServerPack 5.5.0 (`ccb24bc8`) con push. Despliegue armado en `_deploy/2026-11-02-npcdbc-gpu-vieja` (sustituye a `2026-11-01-modelo-gui`). DBR-ASSETS sin tocar.

## Próximo paso

Probar en juego según el LEEME del despliegue. Después, con OK del dueño, subirlo a DBR-ASSETS y al servidor, y pedirle a lukitas2718 su `fml-client-latest.log`.
