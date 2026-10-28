---
module: [kind=guide] instruments
---

# Weather instruments

Five instruments read the air around them. Each is a block with a dial and a
peripheral; on a ship, the [avionics controller](ship_bridge.html) bridges them
like any other peripheral.

| Block | Type | Reads | Unit |
|---|---|---|---|
| Thermometer | `thermometer` | air temperature | `C` |
| Hygrometer | `hygrometer` | relative humidity, 0..1 | `rh` |
| Barometer | `barometer` | static pressure | `hPa` |
| Rain gauge | `rain_gauge` | precipitation rate, plus the total collected | `mm/h` |
| Almanac | `almanac` | progress through the year, or the moon's cycle | `year` or `moon` |

Every instrument has `getValue`, `getUnit` and `read`; see
[instrument](../peripheral/instrument.html). The rain gauge and almanac add methods of
their own.

Readings are taken once a second and cached on the block, so polling is cheap. An
instrument measures at its position in the world: on an airship, it reads the air
the airship is flying through, not the air of the plot the ship's blocks are kept
in.

## Where the readings come from

Instruments don't talk to climate mods directly. The `source` field of `read`
says where a value came from.

- **Temperature and humidity** come from [Thermoo]'s environment when it is
  installed. Any mod that plugs into Thermoo as a temperature provider is picked up
  this way. Without Thermoo they are estimated from the biome, height, time of day
  and weather.
- **Pressure** comes from Sable's dimension physics: the same pressure that airship
  buoyancy uses, so it falls with altitude exactly as lift does. Rain lowers it by
  8 hPa, thunder by 22. With a weather mod installed (below), its highs and lows
  replace those offsets, and the source reads `sable+<mod id>`.
- **Precipitation** is what is falling on the gauge right now; snow counts as its
  water equivalent. A gauge under a roof catches nothing. Vanilla rain is one
  switch for the whole world; a mod with local weather (Driftward's weather layer,
  for example) can say where it rains and how hard, and the source is then that
  mod's id.
- **The year** comes from [Serene Seasons] in the Overworld. Without it, or in
  other dimensions, the almanac's dial shows the moon instead: the unit is `moon`,
  the season methods return nil, and `getMoonPhase` still works.

## For mod authors: weather sources

A mod that keeps its own weather can feed the rain gauge and barometer without
depending on Avionics. During `InterModEnqueueEvent`, send a
`java.util.function.BiFunction<Level, BlockPos, double[]>` with method
`weather_source`:

```java
if (ModList.get().isLoaded("create_avionics")) {
    BiFunction<Level, BlockPos, double[]> source = MyWeather::sample;
    InterModComms.sendTo("create_avionics", "weather_source", () -> source);
}
```

- It is called on the server thread with the instrument's world position (outside
  any Sable sub-level), about once a second per rain gauge or barometer. Keep it
  cheap.
- Return `null` to leave the position to vanilla: another dimension, or your
  weather switched off. The gauge and barometer then behave as without your mod.
- Otherwise return `{kind, rate, pressure}`:
  - `kind`: 0 nothing falls, 1 rain, 2 snow. Decide roofs and cloud height
    yourself; Avionics doesn't check the sky for you.
  - `rate`: millimetres per hour, as liquid water for snow. Ignored when `kind` is 0.
  - `pressure`: hPa added to Sable's altitude curve, in place of the vanilla rain
    and thunder offsets. Return 0 if your weather has no highs and lows.
- Readings report the sending mod's id as their `source`. Only one source is used:
  the first message Avionics processes; any others are logged and ignored.

### The weather radar: `weather_field`

The [weather radar](ship_bridge.md#weather-radar) needs more than what falls on
one spot. It needs the precipitation *column*, out to 2048 blocks and over
chunks that aren't loaded. A mod can send a second function, with method
`weather_field`:

```java
BiFunction<Level, double[], double[]> field = MyWeather::columns;
InterModComms.sendTo("create_avionics", "weather_field", () -> field);
```

- It is called on the server thread with `{x0, z0, x1, z1, ...}`: world columns
  on a 64-block grid, up to 256 per call. The total is limited by the server's
  `[weather_radar] columnsPerTick` (default 32 per tick), and each column is
  asked again about every 10 seconds while a radar looks at it.
- It must never load a chunk. Return `null` to leave the level to
  `weather_source` and vanilla.
- Otherwise return `stride` values per column, where `stride` is the array's
  length divided by the number of columns. The fields go in this order, and new
  ones will only ever be appended. A shorter row, or `NaN` in any field, means
  unknown.
  0. `kind`: 0 none, 1 rain, 2 snow. What falls out of the cloud base,
     whatever roofs or terrain are under it.
  1. `rate`: mm/h as liquid water.
  2. `cloudBase`, 3. `cloudTop`: world y.
  4. `windX`, 5. `windZ`: blocks per second, x east and z south.
  6. `freezingLevel`: world y of the snow line in the air.
  7. `cloud`: cover, 0 to 1.
- Only the first `weather_field` is used, as with `weather_source`. The two are
  independent, and older versions of Avionics ignore `weather_field`.

[Thermoo]: https://modrinth.com/mod/thermoo
[Serene Seasons]: https://modrinth.com/mod/serene-seasons
