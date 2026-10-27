# Create: Avionics

ComputerCraft peripherals for [Create: Simulated][simulated] and
[Create: Aeronautics][aeronautics]. Drop-in: requires unmodified Simulated
1.2.1+, no fork or patch needed.

**[📖 Docs][docs]** · **[Modrinth][modrinth]** · **[Releases][releases]**

## What's in the box

A full set of CC: Tweaked peripherals for every instrumentable Simulated
and Aeronautics block — flight sensors, scriptable controls, propulsion,
links, comms. The [docs site][docs] has the full API reference with units
and body-frame conventions; before writing tight control loops, read the
[guide on `mainThread` setters and the `parallel.waitForAll` idiom][mainthread-guide].

Avionics also adds blocks of its own:

- **Weather instruments**: thermometer, hygrometer, barometer, rain gauge and
  almanac, reading the air wherever they are, including from a flying ship.
- **Avionics controller**: connects every peripheral on a ship to the computer
  it is wired to, under stable names relative to the ship's bow, and acts as
  the ship's transponder.
- **Radar Mk.1**: a single forward beam that reports ships, terrain and their
  closing speed.

See the guides to the [ship bridge][bridge-guide] and the [instruments][instruments-guide].

## Override mechanism

A single Mixin cancels upstream's `ComputerCraftPeripherals#init()` and the
addon's `SimulatedCCIntegration` re-registers the entire peripheral set via
the published `SimPeripheralService` SPI. Additional Mixins add the missing
fields and getters to upstream BlockEntities so the readouts above resolve
against stock Simulated/Aeronautics jars.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.219+
- Create 6.0.10+
- Create: Simulated 1.2.1+
- Sable 1.1.0+
- CC: Tweaked 1.115+
- Create: Aeronautics 1.2.1+ *(optional, required for hot_air_burner and
  steam_vent)*
- Thermoo *(optional: temperature and humidity from the climate mods that use it)*
- Serene Seasons *(optional: the almanac's season calendar)*

Avionics now has blocks, so it is needed on clients as well as the server.

## Pairs well with

- [`ccc`](https://modrinth.com/resourcepack/ccc) — Create-styled CC textures
- [`create-computercraft`](https://modrinth.com/resourcepack/create-computercraft)

## License

MIT, see LICENSE.md and NOTICE.md.

[simulated]: https://github.com/Creators-of-Aeronautics/Simulated-Project
[aeronautics]: https://modrinth.com/mod/create-aeronautics
[docs]: https://solastrius.github.io/CreateAvionics/
[modrinth]: https://modrinth.com/mod/create-avionics
[releases]: https://github.com/SolAstrius/CreateAvionics/releases
[mainthread-guide]: https://solastrius.github.io/CreateAvionics/guide/mainthread.html
[bridge-guide]: https://solastrius.github.io/CreateAvionics/guide/ship_bridge.html
[instruments-guide]: https://solastrius.github.io/CreateAvionics/guide/instruments.html
