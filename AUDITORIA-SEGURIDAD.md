# Auditoría de seguridad — CustomNPC-DBC-Addon

Fecha: 2026-11-05 · Commit: `b909c43` (rama `main-version`. El PC está en `6abc33fa`, **51 commits por detrás**, limpio: `git pull`) · Alcance: los 82 commits propios del fork (razas extra, retirada de la Mente, rueda de formas, ficha de stats) sobre el addon de KAMKEEL. El resto (363 clases) es upstream.
Herramientas: revisión manual (skill `dbr-seguridad`)

## Resumen

Sin hallazgos. Los dos cambios que tocan paquetes del cliente validan en el servidor: elegir una forma de una raza extra solo acepta formas de esa raza (`ExtraRaces.isForm`) y el ascenso lo decide el nivel de habilidad racial que lee el servidor (`ExtraRaceLogic.ascendTarget` en DbrRazas), con las mismas comprobaciones de maestría que las razas originales. Mejorar la Mente se bloquea en el servidor.

| Severidad | Cantidad |
|---|---|
| Crítica | 0 |
| Alta | 0 |
| Media | 0 |
| Baja | 0 |

## Hallazgos

Ninguno.

## Variantes revisadas

- `DBCSelectForm`: la forma elegida se guarda solo si existe para la raza; no sube a nadie de forma.
- `MixinJRMCPacketHandlerServer.blockMindUpgrade`: cancela la mejora del índice 4 en el servidor.
- `MixinDBCPacketHandlerServer` (DbrRazas): ascenso con nivel racial, transformación instantánea y maestría comprobados.

## Lo que está bien

- Los comentarios del código dicen qué valida el servidor y qué solo pinta el cliente.

## Pendiente de verificar

- Los paquetes del upstream (formas custom, auras, estadísticas) no se revisaron uno a uno.
