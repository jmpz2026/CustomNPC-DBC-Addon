# NpcDbcResu — Optimizaciones de rendimiento

Fork de [KAMKEEL/CustomNPC-DBC-Addon](https://github.com/KAMKEEL/CustomNPC-DBC-Addon)
basado en el commit `95d3d1d6` ("1.1.5"). Objetivo: **subir FPS manteniendo los
efectos visuales**, en lugar de desactivarlos como hacen los parches que vacían el
pipeline de shaders.

El código del mod no cambia de comportamiento por defecto en lo "gratis"; los
cambios con costo visual están detrás de campos tuneables con el valor vanilla como
referencia.

---

## Cómo compilar

Requiere **JDK 17 o 21** para correr Gradle (el toolchain de compilación es Java 8,
provisto por Gradle).

```bash
git clone -b optimized-1.1.5 https://github.com/jmpz2026/CustomNPC-DBC-Addon.git
cd CustomNPC-DBC-Addon
export JAVA_HOME=<ruta a un JDK 17/21>
./gradlew build
# jar -> build/libs/npcdbc-1.1.5.jar
```

### Fix de toolchain (commit `build:`)
El commit base usaba versiones de infra ya podadas del nexus de GTNH. Arreglado:

| Archivo | Antes | Ahora | Motivo |
|---|---|---|---|
| `settings.gradle` | convention `1.0.14` | `1.0.51` | 1.0.14 ya no existe en el nexus |
| `gradle-wrapper.properties` | Gradle `8.5` | `8.14.3` | requerido por gtnhgradle 1.0.51 |
| `dependencies.gradle` | NEI `2.5.4-GTNH` | `2.7.91-GTNH` | 2.5.4 podada (dep runtime-only) |

---

## Cambios de rendimiento

### 1. Bloom — `PostProcessing.java` + `blur.frag`
El bloom se mantiene activo, solo más barato. Campos tuneables (`PostProcessing`):

| Campo | Default | Efecto |
|---|---|---|
| `BLOOM_RES_SHIFT` | `1` | Downscale uniforme de toda la mip chain. `1` = cuarto-res (~4× menos píxeles bloom). `0` = look vanilla (media-res). |
| `MAX_BLOOM_LEVELS` | `6` | Tope de niveles de mip procesados (init + loops down/up). |

`blur.frag`: gaussiano de **11→7 taps** (~36% menos fetches por paso de blur; el
blur corre una vez por mip en ambas direcciones).

Idle sin cambios: el bloom sigue gateado por `processBloom`, solo trabaja cuando hay
aura/ki en pantalla.

### 2. Aura — `AuraRenderer.java`
`renderAura` corría 5 capas × 20 pasos = ~100 iteraciones por aura por frame.

**Gratis (sin cambio visual):**
- Un único `Random` compartido en vez de `new Random()` en el loop interno
  (~100 allocs/frame/aura) y en `renderAura`/`lightning`. Misma aleatoriedad, sin GC.
- `bindTexture(aura.text1)` una sola vez antes de los loops (antes se rebindeaba en
  cada iteración, más un bind duplicado).

**Tuneables (`AuraRenderer`, default = vanilla):**

| Campo | Default | Efecto |
|---|---|---|
| `AURA_MAX_LAYERS` | `5` | Menos capas = menos renders de modelo = más FPS, aura más fina. |
| `AURA_LAYER_STEP` | `0.05f` | Paso mayor = menos iteraciones del loop interno. |

### 3. Cache de ResourceLocation — `RLCache.java`
El path de render construía la misma ruta de textura cada frame vía concatenación de
strings + `new ResourceLocation(...)`, que re-parsea, valida, pasa a minúsculas y
aloca en cada llamada. Por cada parte del cuerpo, por frame, por player DBC.

**Gratis (sin cambio visual):** nuevo `RLCache` (HashMap estático, solo hilo cliente)
cachea las instancias por su string. `ResourceLocation` es inmutable → cache 100%
correcta; las rutas son un conjunto acotado → el mapa se mantiene pequeño. Aplicado a
**48 call sites**: `ModelDBC` (31) y `CNPCAnimationHelper` (17). Elimina el parse +
alloc + churn de GC. Escala con la cantidad de players DBC.

### 4. Low Spec Mode — master switch para PCs muy bajos
Toggle maestro en `ConfigDBCClient` para apagar de una toda la pila visual cara.

**Config (`Rendering`):**

| Campo / Config | Default | Efecto |
|---|---|---|
| `Low Spec Mode` | **`true`** | Cuando ON, fuerza OFF: bloom, outlines, auras del addon y partículas custom, ignorando los toggles individuales. Ponlo en `false` para que manden los toggles por feature (defaults = vanilla on). |
| `Enable Auras` | `true` | Off global de auras del addon (no afecta auras DBC nativas). Solo aplica con Low Spec Mode OFF. |
| `Enable Custom Particles` | `true` | Off de partículas custom del addon. Con OFF, caen al render base de DBC (sin cola del addon). Solo aplica con Low Spec Mode OFF. |

Gates (`ConfigDBCClient`): `bloomEnabled()`, `outlinesEnabled()`, `aurasEnabled()`,
`particlesEnabled()` = `!LowSpecMode && Enable<X>`. Usados en:
- Bloom → `PostProcessing.startBlooming`.
- Outlines → `RenderEventHandler` (player) + `MixinModelMPM` (NPC).
- Auras → `RenderEventHandler` render de `AuraRenderer` (player + NPC).
- Partículas → gateadas en la **fuente** (`MixinEntityCusPar`): si off, no se encolan
  (sin leak de cola, caen al render base DBC).

Botón **Low Spec Mode** añadido al GUI de inventario DBC (junto a Bloom/Outlines/Shaders).
Este build arranca con Low Spec Mode **ON** por default (pensado para PCs de muy bajos
recursos); el usuario lo apaga si su equipo aguanta.

### 5. Outline — gate de distancia (`OutlineRenderer.java`)
El outline es un pase extra caro (varios `model.render()` + 2 pasos de shader + bloom
por entidad) y apenas se nota lejos de la cámara.

**Tuneable (`ConfigDBCClient`, default = vanilla):**

| Campo / Config | Default | Efecto |
|---|---|---|
| `Outline Max Distance` (sección `Rendering`) | `0` | Distancia máx. (bloques) a la que se renderiza el outline de players y NPCs. `0` = sin límite (comportamiento idéntico al actual). `>0` = salta el pase (incluido el wrap de bloom) más allá de N bloques. |

Gateado en los 2 call sites: player en `RenderEventHandler`, NPC en `MixinModelMPM`.
Distancia vía `getDistanceSqToEntity` contra `renderViewEntity` — sin código GL nuevo,
no afecta drivers.

---

## Cómo ajustar
Los campos son `public static`. Para perfiles de calidad/rendimiento se pueden
exponer en `ConfigDBCClient` (pendiente) o setear desde otro punto de carga del mod.
Para volver al look vanilla del bloom: `BLOOM_RES_SHIFT = 0`.
