# Modelo de seguridad — CustomNPC-DBC-Addon

Hereda: `dbr-seguridad/references/modelo-amenazas.md` (INV-G1..G9).
Última revisión: 2026-11-05

## Qué hace y qué protege

Fork de CNPC-DBC-Addon (npcdbc): formas custom, auras y razas extra de DBC.

## Entradas

| Entrada | Quién la dispara | Guarda | Archivo |
|---|---|---|---|
| Elegir forma (`DBCSelectForm`) | cliente | la forma debe existir para la raza | `network/packets/player/form/` |
| Mejorar atributo (JRMCore) | cliente | la Mente se cancela | `mixins/late/impl/dbc/` |

## Invariantes propios

- **INV-1** — Ningún paquete sube de forma: el ascenso lo decide el nivel que guarda el servidor.

## Datos y secretos

- NBT de DBC del jugador.
- Secretos: ninguno.

## Dependencias de seguridad

- CustomNPC-Plus, JRMCore/DBC (mixins), DbrRazas.

## Hallazgos que importan acá

| Clase | Severidad base |
|---|---|
| Formas o stats regalados | Alta |

## Fuera de alcance

- Balance del juego.

## Riesgos aceptados

| Fecha | Riesgo | Por qué se acepta | Revisar |
|---|---|---|---|
| — | — | — | — |
