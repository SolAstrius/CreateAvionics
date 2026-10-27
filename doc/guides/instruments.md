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
  8 hPa, thunder by 22.
- **Precipitation** is what is falling on the gauge right now; snow counts as its
  water equivalent. A gauge under a roof catches nothing.
- **The year** comes from [Serene Seasons] in the Overworld. Without it, or in
  other dimensions, the almanac's dial shows the moon instead: the unit is `moon`,
  the season methods return nil, and `getMoonPhase` still works.

[Thermoo]: https://modrinth.com/mod/thermoo
[Serene Seasons]: https://modrinth.com/mod/serene-seasons
