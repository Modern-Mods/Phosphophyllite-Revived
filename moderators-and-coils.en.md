# Adding moderators and coils

Bigger Reactors keeps its reactor moderators, turbine coils and fluid transitions in
[NeoForge data maps](https://docs.neoforged.net/docs/resources/server/datamaps/). Anything registered
there is synced to the client automatically, so tooltips and the JEI categories update on their own.

There are two ways to add entries: a datapack (works on any install, no extra mods) and KubeJS
(useful when you want to compute values or reuse a script across packs).

The examples below add one moderator and one coil that are absurdly good — every value is `100` — so
you can immediately tell whether your entry was picked up.

| Entry | Block |
| --- | --- |
| Moderator | `minecraft:dirt` |
| Coil | `minecraft:grass_block` |

## Datapack

Data map files always live under the namespace of the data map itself, not your pack's namespace.
That means the paths are the same no matter what you call your datapack.

### Moderator

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

### Coil

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

### Notes

- Keys accept tags too: `"#c:storage_blocks/iron"` registers every block in that tag.
- Add `"replace": true` next to `"values"` to wipe every entry the mod ships before applying yours.
- To delete a single entry, list it under `"remove"`:

  ```json
  {
    "values": {},
    "remove": ["minecraft:glass"]
  }
  ```

- Fluid moderators go in `data/biggerreactors/data_maps/fluid/reactor_fluid_moderator.json` with the
  same four fields, and fluid transitions in `data/biggerreactors/data_maps/fluid/fluid_transition.json`.

## KubeJS

These are startup events, so the file belongs in `kubejs/startup_scripts/`.

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

The argument order matches the JSON fields:

- `moderators` → `add(block, absorption, efficiency, moderation, conductivity)`
- `fluids` → `add(fluid, absorption, efficiency, moderation, conductivity)`
- `coils` → `add(block, efficiency, extractionRate, bonus)`
- `transitions` → `add(liquid, gas, latentHeat, boilingPoint, liquidThermalConductivity, gasThermalConductivity, turbineMultiplier)`

### The rest of the API

```js
BiggerReactorEvents.registry('moderators', event => {
    event.add('#c:storage_blocks/iron', 0.5, 0.8, 1.5, 1.0)
    event.remove('minecraft:glass')
})

BiggerReactorEvents.registry('fluids', event => {
    event.add('minecraft:lava', 0.1, 0.3, 1.2, 0.2)
    event.remove('minecraft:water')
})

BiggerReactorEvents.registry('coils', event => {
    event.add('minecraft:netherite_block', 1.5, 0.9, 1.2)
    event.remove('minecraft:gold_block')
})

BiggerReactorEvents.registry('transitions', event => {
    event.add('minecraft:water', 'biggerreactors:steam', 1000, 373, 0.001, 0.01, 1.0)
    event.remove('minecraft:water')
})
```

The `moderators` event also accepts `addFluid` and `removeFluid`, which do the same thing as `fluids`.

Every target has aliases, so `moderator`, `moderators`, `moderador` and `moderadores` all reach the
same event, and so do `coil`/`coils`/`bobina`/`bobinas`, `fluid`/`fluids`/`fluido`/`fluidos` and
`transition`/`transitions`/`transicion`/`transiciones`.

Scripted entries are applied after the data maps, which means a KubeJS `add` overrides a datapack
entry for the same block, and a KubeJS `remove` wins over both.

## Migrating from 1.20.1

Your 1.20.1 files are read as they are. Nothing has to be converted by hand.

### Registries

In 1.20.1 moderators, coils and transitions were loose JSON5 files inside a datapack. Those files
still work: drop them into a datapack the way you always did and the mod converts them to the new
format when the world loads.

| What | Where it goes |
| --- | --- |
| Moderators | `data/<pack>/ebcr/moderators/**/*.json5` |
| Coils | `data/<pack>/ebest/coils/**/*.json5` |
| Transitions | `data/<pack>/transitions/**/*.json5` |

The old formats are accepted whole, comments, unquoted keys, trailing commas and all:

```json5
{
    // moderator
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
    // coil
    type: "tag",
    location: "forge:storage_blocks/netherite",
    efficiency: 1.7,
    extractionRate: 0.8,
    bonus: 1.08,
}
```

```json5
{
    // transition
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

Conversion details:

- The `forge:` tag namespace is rewritten to `c:` on its own, which is what 1.21.1 uses.
- `type: "tag"` and `type: "fluidtag"` become tag references; `registry` and `fluid` become direct
  references.
- `type: "fluid"` and `"fluidtag"` go to the fluid moderator registry.
- Plain `.json` files are accepted too, they do not have to be `.json5`.
- The migration runs on every datapack reload, so `/reload` is enough to see your changes.

On startup the log tells you how many entries were converted:

```
[Worker-Main-1/INFO] [ne.ro.bi.BiggerReactors/]: Migrated 3 legacy registry entries (1 moderators, 0 fluid moderators, 1 coils, 1 transitions)
```

### Mod config

Copy `biggerreactors-client.toml` and `biggerreactors-server.toml` from 1.20.1 into `config/` and
they work untouched: the format and every field are identical between the two versions.

## Checking that it worked

Both registries log their size on every world load:

```
[Server thread/INFO] [ne.ro.bi.BiggerReactors/]: Loaded 31 moderator entries
[Server thread/INFO] [ne.ro.bi.BiggerReactors/]: Loaded 7 coil entries
```

In game, hold the block and enable advanced tooltips (F3+H) — a registered moderator or coil says so
in its tooltip, and both show up in their JEI category.
