# Create: Avionics

ComputerCraft peripherals for [Create: Simulated][simulated] and
[Create: Aeronautics][aeronautics]. Drop-in: requires unmodified Simulated
1.2.1+, no fork or patch needed.

## Peripherals

**Net-new**
- `analog_transmission` — read and drive the rotational signal of an Analog
  Transmission, with optional override of redstone input
- `physics_assembler` — query and command a Physics Assembler
- `portable_engine` — fuel state, burn time, generated speed, stress capacity
- `steering_wheel` — held state, angle, target angle, clamp parameters
- `throttle_lever` — read state, drive lever position
- `laser_pointer` — direction, range, firing state, color (read/write),
  rainbow toggle
- `laser_sensor` — power readout, closest pointer hit distance
- `rope_winch` — current length, bounds, kinetic-driven movement speed
- `directional_gearshift` — source axis, left/right power state, current
  mode (`forward` / `reverse` / `stop` / `neutral`)
- `hot_air_burner` *(requires Aeronautics)* — gas output, target amount,
  balloon coupling, boiler efficiency
- `steam_vent` *(requires Aeronautics)* — same surface as `hot_air_burner`,
  scoped to steam
- `wooden_propeller` / `andesite_propeller` / `smart_propeller`
  *(requires Aeronautics)* — kinetic and rotation speed, thrust, airflow,
  active state, axis. Also matched by the shared `propeller` type.
- `propeller_bearing` *(requires Aeronautics)* — kinetic, rotation, and
  angular speed, thrust/airflow, sail power, stress, thrust handedness
  (read/write), assembly state, assemble/disassemble commands.
- `gyroscopic_propeller_bearing` *(requires Aeronautics)* — same surface
  as `propeller_bearing` plus `setTilt` / `setStrictTilt` for scripted axis
  control. Also matched by the shared `propeller_bearing` type.
- `mounted_potato_cannon` *(requires Aeronautics)* — aim vector, muzzle
  position, cogwheel speed, obstruction state, ammo type and count

**Extended readouts**
- `gimbal_sensor` — body-frame angular velocity, gravity, linear acceleration;
  Rad-suffixed angle variants
- `altitude_sensor` — vertical speed
- `velocity_sensor` — sensing axis
- `navigation_table` — distance, closure rate, orientation, heading, target
  metadata, forward-error bearing
- `optical_sensor` — laser range, no-hit detection
- `linked_typewriter` — `key` and `key_up` events on attached computers

The full upstream peripheral set (`directional_link`, `docking_connector`,
`modulating_link`, `nameplate`, `swivel_bearing`, `torsion_spring`) is
republished verbatim so this addon owns the entire CC surface. See NOTICE.md
for attribution.

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

## Pairs well with

- [`ccc`](https://modrinth.com/resourcepack/ccc) — Create-styled CC textures
- [`create-computercraft`](https://modrinth.com/resourcepack/create-computercraft)

## License

MIT, see LICENSE.md and NOTICE.md.

[simulated]: https://github.com/Creators-of-Aeronautics/Simulated-Project
[aeronautics]: https://modrinth.com/mod/create-aeronautics
