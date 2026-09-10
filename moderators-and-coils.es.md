# Agregar moderadores y bobinas

Bigger Reactors guarda los moderadores del reactor, las bobinas de la turbina y las transiciones de
fluidos en [data maps de NeoForge](https://docs.neoforged.net/docs/resources/server/datamaps/). Todo
lo que se registra ahí se sincroniza solo al cliente, así que los tooltips y las categorías de JEI se
actualizan sin hacer nada más.

Hay dos formas de agregar entradas: un datapack (funciona en cualquier instalación, sin mods extra) y
KubeJS (útil si querés calcular valores o reusar un script entre packs).

Los ejemplos de abajo agregan un moderador y una bobina absurdamente buenos — todos los valores son
`100` — para que se note al instante si la entrada se cargó.

| Entrada | Bloque |
| --- | --- |
| Moderador | `minecraft:dirt` |
| Bobina | `minecraft:grass_block` |

## Datapack

Los archivos de data map van siempre bajo el namespace del data map, no bajo el de tu pack. O sea que
las rutas son estas sin importar cómo se llame tu datapack.

### Moderador

`data/biggerreactors/data_maps/block/reactor_moderator.json`

```json
{
  "values": {
    "minecraft:dirt": {
      "absorption": 100.0,
      "efficiency": 100.0,
      "moderation": 100.0,
      "conductivity": 100.0
    }
  }
}
```

### Bobina

`data/biggerreactors/data_maps/block/turbine_coil.json`

```json
{
  "values": {
    "minecraft:grass_block": {
      "efficiency": 100.0,
      "extractionRate": 100.0,
      "bonus": 100.0
    }
  }
}
```

### Detalles

- Las claves también aceptan tags: `"#c:storage_blocks/iron"` registra todos los bloques de ese tag.
- Agregando `"replace": true` al lado de `"values"` se borra todo lo que trae el mod antes de aplicar
  lo tuyo.
- Para borrar una entrada puntual, listala en `"remove"`:

  ```json
  {
    "values": {},
    "remove": ["minecraft:glass"]
  }
  ```

- Los moderadores líquidos van en `data/biggerreactors/data_maps/fluid/reactor_fluid_moderator.json`
  con los mismos cuatro campos, y las transiciones en
  `data/biggerreactors/data_maps/fluid/fluid_transition.json`.

## KubeJS

Son eventos de startup, así que el archivo va en `kubejs/startup_scripts/`.

```js
BiggerReactorEvents.registry('moderators', event => {
    event.add('minecraft:dirt', 100, 100, 100, 100)
})

BiggerReactorEvents.registry('coils', event => {
    event.add('minecraft:grass_block', 100, 100, 100)
})

BiggerReactorEvents.registry('fluids', event => {
    event.add('minecraft:lava', 100, 100, 100, 100)
})

BiggerReactorEvents.registry('transitions', event => {
    event.add('minecraft:lava', 'biggerreactors:liquid_uranium', 100, 100, 100, 100, 100)
})
```

El orden de los argumentos es el mismo que el de los campos del JSON:

- `moderators` → `add(bloque, absorption, efficiency, moderation, conductivity)`
- `fluids` → `add(fluido, absorption, efficiency, moderation, conductivity)`
- `coils` → `add(bloque, efficiency, extractionRate, bonus)`
- `transitions` → `add(líquido, gas, latentHeat, boilingPoint, liquidThermalConductivity, gasThermalConductivity, turbineMultiplier)`

### El resto de la API

```js
BiggerReactorEvents.registry('moderadores', event => {
    event.add('#c:storage_blocks/iron', 0.5, 0.8, 1.5, 1.0)
    event.remove('minecraft:glass')
})

BiggerReactorEvents.registry('fluidos', event => {
    event.add('minecraft:lava', 0.1, 0.3, 1.2, 0.2)
    event.remove('minecraft:water')
})

BiggerReactorEvents.registry('bobinas', event => {
    event.add('minecraft:netherite_block', 1.5, 0.9, 1.2)
    event.remove('minecraft:gold_block')
})

BiggerReactorEvents.registry('transiciones', event => {
    event.add('minecraft:water', 'biggerreactors:steam', 1000, 373, 0.001, 0.01, 1.0)
    event.remove('minecraft:water')
})
```

El evento `moderators` también acepta `addFluid` y `removeFluid`, que hacen lo mismo que `fluids`.

Cada target tiene alias, así que `moderator`, `moderators`, `moderador` y `moderadores` llegan al
mismo evento, igual que `coil`/`coils`/`bobina`/`bobinas`, `fluid`/`fluids`/`fluido`/`fluidos` y
`transition`/`transitions`/`transicion`/`transiciones`.

Las entradas de script se aplican después de los data maps, así que un `add` de KubeJS pisa a la
entrada de un datapack para el mismo bloque, y un `remove` de KubeJS le gana a las dos.

## Migrar desde 1.20.1

Los archivos de configuración de la 1.20.1 se leen tal cual, no hace falta convertir nada a mano.

### Registros

En 1.20.1 los moderadores, las bobinas y las transiciones venían en archivos JSON5 sueltos dentro de
un datapack. Esos archivos siguen funcionando: metelos en un datapack como siempre y el mod los
convierte al formato nuevo al cargar el mundo.

| Qué | Dónde va |
| --- | --- |
| Moderadores | `data/<pack>/ebcr/moderators/**/*.json5` |
| Bobinas | `data/<pack>/ebest/coils/**/*.json5` |
| Transiciones | `data/<pack>/transitions/**/*.json5` |

Los formatos viejos se aceptan enteros, con comentarios, claves sin comillas y comas colgando:

```json5
{
    // moderador
    type: "registry",
    location: "minecraft:sponge",
    absorption: 0.42,
    efficiency: 0.43,
    moderation: 4.4,
    conductivity: 4.5,
}
```

```json5
{
    // bobina
    type: "tag",
    location: "forge:storage_blocks/netherite",
    efficiency: 1.7,
    extractionRate: 0.8,
    bonus: 1.08,
}
```

```json5
{
    // transición
    liquidType: "registry",
    liquid: "biggerreactors:liquid_uranium",
    liquidThermalConductivity: 3.3,
    gasType: "registry",
    gas: "biggerreactors:liquid_obsidian",
    gasThermalConductivity: 3.4,
    latentHeat: 33,
    boilingPoint: 1500.15,
    turbineMultiplier: 1.5,
}
```

Detalles de la conversión:

- El namespace `forge:` de los tags se reescribe solo a `c:`, que es el que usa 1.21.1.
- `type: "tag"` y `type: "fluidtag"` se convierten en referencias a tag; `registry` y `fluid`, en
  referencias directas.
- `type: "fluid"` y `"fluidtag"` van al registro de moderadores líquidos.
- Los archivos `.json` normales también se aceptan, no hace falta que sean `.json5`.
- La migración corre en cada recarga de datapacks, así que `/reload` alcanza para ver los cambios.

Al arrancar, el log te dice cuántas entradas se convirtieron:

```
[Worker-Main-1/INFO] [ne.ro.bi.BiggerReactors/]: Migrated 3 legacy registry entries (1 moderators, 0 fluid moderators, 1 coils, 1 transitions)
```

### Configuración del mod

`biggerreactors-client.toml` y `biggerreactors-server.toml` de la 1.20.1 se copian a `config/` y
funcionan sin tocar nada: el formato y todos los campos son idénticos entre las dos versiones.

## Cómo verificar que anduvo

Los dos registros logean su tamaño en cada carga de mundo:

```
[Server thread/INFO] [ne.ro.bi.BiggerReactors/]: Loaded 31 moderator entries
[Server thread/INFO] [ne.ro.bi.BiggerReactors/]: Loaded 7 coil entries
```

En el juego, agarrá el bloque y activá los tooltips avanzados (F3+H) — un moderador o una bobina
registrados lo dicen en el tooltip, y los dos aparecen en su categoría de JEI.
