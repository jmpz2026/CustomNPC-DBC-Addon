# npcdbc — Optimizaciones de rendimiento

Rama `optimized-version`, rehecha desde cero sobre `main-version`. Objetivo: más FPS sin
cambiar el aspecto por defecto. Lo que sí cambia el aspecto queda detrás de ajustes cuyo
valor de fábrica es el comportamiento original; el modo Ligero del pack los baja.

---

## Ramas: normal y optimizada

| Rama | Jar | Qué lleva |
|---|---|---|
| `main-version` | `npcdbc-1.1.6.jar` (normal) | El fork de DBR sin optimizaciones |
| `optimized-version` | `npcdbc-1.1.6.jar` (optimizado) | Todo lo de `main-version` + los cambios de este documento |

- Las dos ramas **solo difieren en las optimizaciones**. Todo lo que no sea de rendimiento
  entra primero en `main-version` y después se mergea a `optimized-version`
  (`git checkout optimized-version && git merge main-version`). Nunca al revés.
- Los dos jars se llaman igual y tienen el mismo modid: van en carpetas de salida distintas
  (`normal/` y `optimizado/`) y nunca juntos en `mods/`.

## Cómo compilar

Gradle corre con **JDK 17 o 21** (el toolchain de compilación es Java 8, lo pone Gradle):

```bash
export JAVA_HOME=<ruta a un JDK 17/21>
./gradlew build
```

---

## Sin cambio visual (Normal y Ligero)

1. **Caché de `ResourceLocation`** (`client/utils/RLCache`). El render armaba la misma ruta y
   un `ResourceLocation` nuevo por parte del cuerpo, por frame y por entidad DBC. Ahora sale de
   una caché: `ModelDBC`, `CNPCAnimationHelper`, `DBCHair`, `DBCHorns`, brazos de Arcosian,
   textura de Potara y las texturas de `EntityAura` (que el aura vuelve a fijar en cada frame).
2. **Aura** (`AuraRenderer`). Un `Random` compartido en vez de un `new Random()` por llamada y
   por paso del bucle interno (~100 por aura y frame), y un solo `bindTexture` por aura en vez
   de uno por paso.
3. **Datos de jugador en el cliente** (`ClientCache.getClientData`). `CacheHashMap.get`
   recorre el mapa entero para caducar entradas en cada llamada, y `DBCData.get` se llama
   muchas veces por frame y jugador. La búsqueda va por `getOrDefault` y el barrido corre como
   mucho una vez por segundo; las entradas siguen caducando a los `CacheLife` minutos. Además,
   `MixinEntityCusPar` busca los datos una vez por partícula en vez de dos.
4. **Stencil** (`PlayerDataUtil.useStencilBuffer`). Con `Enable Outlines` apagado, un outline
   que no se va a dibujar ya no monta el stencil ni pasa la entidad al pase intermedio.

## Ajustes con costo visual (sección `Rendering` de `client.cfg`)

| Ajuste (campo) | Fábrica | Ligero | Efecto |
|---|---|---|---|
| `Model Detail Max Distance` (`ModelDetailMaxDistance`) | `0` | `24` | Más allá de N bloques no se dibuja la cara de los NPC (6 a 11 draw calls). `0` = siempre. |
| `Aura Max Distance` (`AuraMaxDistance`) | `0` | `32` | Más allá de N bloques el aura del addon hace la mitad de pasadas, con la opacidad compensada (`1 - (1 - a)²`) para que no se vea más fina. `0` = detalle completo. |
| `Aura Particle Density` (`AuraParticleDensity`) | `100` | `50` | % de rondas de partículas de formas custom que se generan. Son entidades reales: menos CPU, RAM y GC. En las GUI nunca se recorta. |

Solo Normal (Ligero ya apaga bloom y outlines; el modo no los toca):

| Ajuste (campo) | Fábrica | Efecto |
|---|---|---|
| `Outline Max Distance` (`OutlineMaxDistance`) | `0` | Más allá de N bloques no se dibuja el outline ni se monta su stencil. `0` = siempre. |
| `Bloom Low Resolution` (`BloomLowResolution`) | `false` | La cadena del bloom empieza a cuarto de resolución: se salta los dos pases de blur más caros. El brillo sale algo más blando. |
| `Bloom Max Levels` (`BloomMaxLevels`) | `0` | Tope de niveles de mip del bloom. Menos = más barato y halo más corto. `0` = todos. |

Los shaders del bloom y los framebuffers no se tocan: un blur reescrito ya dejó la pantalla
en negro en algunas GPU (`66587d46`, `fdf98aed`), y `PostProcessing.init` también monta el
stencil del framebuffer principal. Solo cambia qué niveles recorre `bloom()`.

Las distancias se miden contra la cámara (`renderViewEntity`) y no aplican en las vistas
previas de GUI (`client/utils/RenderLOD`).

En el pack de DBR, `RecortesTerceros` de DbrMod pone los valores de la columna Ligero en
memoria, solo si el campo sigue en su valor de fábrica, y los devuelve al volver a Normal.
Con el jar normal los campos no existen y no pasa nada.
