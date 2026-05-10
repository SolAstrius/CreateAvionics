# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3.0] - 2026-05-10

### Added

- Kinetic SCADA pack: a uniform peripheral surface for every kinetic block —
  `getSelfId`, `getSourceId`, `getSubnetworkAnchorId`, `getNetworkId`,
  `getKind`, `getSpeed`, `hasSource`, `isOverstressed`, `getStressImpact`,
  `getStressCapacity`. Implemented as a new `SimKineticPeripheral` base on
  the simulated side and `KineticPeripheral` on the Create side.
- Create-side peripherals (drop-in replacements for Create's own):
  `Create_CreativeMotor`, `Create_RotationSpeedController`,
  `Create_Speedometer`, `Create_Stressometer`, `Create_MechanicalBearing`,
  `Create_MechanicalPiston`, `Create_RopePulley`, `Create_ElevatorPulley`,
  `Create_ElevatorContact`, `Create_GantryShaft`,
  `Create_SequencedGearshift`.
- `nav_table` per-target metadata: compass, recovery compass, and map targets
  now expose lodestone position, recovery death-pos, and map id/bounds.
- Public API package `ink.astrius.create_avionics.api.create.*` exposing six
  mixin-supplied accessor interfaces (`LinearActuatorExt`,
  `MechanicalBearingExt`, `ElevatorContactExt`, `ElevatorPulleyExt`,
  `GantryShaftExt`, `SequencedGearshiftExt`) for downstream addons.

### Changed

- Several `@LuaFunction`s promoted to `mainThread = true` (notably on
  `physics_assembler` and `swivel_bearing` mutators). Calls still work but
  now yield and run on the server tick.

### Removed

- `propeller.getKineticSpeed()` — use `getSpeed()`.
- `propeller_bearing.getKineticSpeed()` — use `getSpeed()`.
- `propeller_bearing.getStressApplied()` — use `getStressImpact()`.
- `mounted_potato_cannon.getKineticSpeed()` — use `getSpeed()`.
- `analog_transmission.getInputSpeed()` — use `getSpeed()`.
- `analog_transmission.getStressApplied()` — use `getStressImpact()`.
- `analog_transmission.getOutputStressApplied()` — use `getOutputStressImpact()`.

### Fixed

- `gimbal_sensor.getLinearAcceleration` now subtracts gravity. A stationary
  sensor previously reported ~9.8 m/s² on its vertical axis; it now reports
  zero. Scripts that compensated manually must remove that compensation.
- `Create_Stressometer.getSubnetworkAnchorId` is now `mainThread = true`,
  matching the rest of the SCADA pack. The gauge's redeclaration was
  missing the dispatch flag and would race with the server thread when
  walking the kinetic graph.

### Docs

- Site-wide: every kinetic peripheral page now shows the full SCADA pack
  inherited from the abstract bases (~10 extra methods per page that were
  silently invisible before). Root cause was a generic-erasure bug in
  cct-javadoc; fixed in a fork
  ([`SolAstrius/cct-javadoc@131bfa1`](https://github.com/SolAstrius/cct-javadoc/commit/131bfa1)).
- `mainThread = true` methods now render with a styled "Yields" admonition
  auto-emitted from the annotation; the hand-written
  `<p>Yields until the next server tick.</p>` boilerplate has been
  stripped from sources to avoid the resulting duplication.
- Backfilled missing Javadoc on Create-side passthrough methods
  (`CreativeMotor.{set,get}GeneratedSpeed`, `RotationSpeedController.{set,get}TargetSpeed`,
  `SequencedGearshift.{rotate,move,isRunning}`) and the eight SCADA
  redeclarations on `Create_Stressometer`.

## [0.2.1] - 2026-05-08

### Added

- Docs site at <https://solastrius.github.io/CreateAvionics> with cct-javadoc
  + illuaminate, `@cc.module` annotations on all peripherals, units on
  thrust/airflow/sail_power/gas_output, and a parallel/mainthread guide.
- New sensor methods and unit documentation across altitude, gimbal, optical,
  laser, velocity, gas-provider, throttle-lever, nameplate.

### Changed

- Widen float returns to double across the Lua API for consistency.
- Mark state-mutating setters as `mainThread = true` (yields, runs on server
  tick).

### Fixed

- Off-thread state mutation on `setName`, `setTargetAmount`, `setLimit`.

## [0.2.0] - 2026-05-08

### Added

- Peripherals: `throttle_lever`, `laser_pointer`, `laser_sensor`,
  `rope_winch`, `propeller`, `propeller_bearing`, `gyroscopic`,
  `mounted_potato_cannon`, `directional_gearshift`.
- Mod metadata: icon, displayURL, NeoForge update checker.
- CI: build, release, and dependabot workflows; javadoc jar.

### Fixed

- `laser_pointer` published method name (`getLaserRange`).
- NeoForge version range and CI YAML quoting issues.

[Unreleased]: https://github.com/SolAstrius/CreateAvionics/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/SolAstrius/CreateAvionics/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/SolAstrius/CreateAvionics/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/SolAstrius/CreateAvionics/releases/tag/v0.2.0
