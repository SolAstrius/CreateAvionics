# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.6.0] - 2026-09-01

### Added

- **`Create_GantryShaft` can now see a carriage while it is moving.** A gantry
  carriage stops being a block the instant it assembles — Create anchors the
  contraption at the carriage's own position and removes its blocks from the
  world — so every position readback went nil for the entire duration of a
  move, which is exactly when a control program needs one. The peripheral now
  looks for the contraption entity first and falls back to a block scan, using
  Create's own rail-membership test, so positions stay live end to end and are
  fractional rather than whole while travelling.
- `Create_GantryShaft`: `getCarriage`, returning position, state and the
  applicable one of id/error/remaining in a single read. Preferred over the
  individual getters in a control loop — every field comes from one
  observation, so they cannot disagree the way separate polls straddling a
  tick boundary can.
- `Create_GantryShaft`: `getState`, distinguishing `empty`, `parked`,
  `moving`, `stalled` and `failed`. Previously every one of those answered nil
  and there was no way to tell "no carriage" from "blocked" from "assembly
  failed".
- `Create_GantryShaft`: `isAssembled`, `isStalled`, `getRemainingMovement`,
  `getLastAssemblyError`, `getRailStart`, and `disassemble`. This brings the
  gantry to parity with the other contraption controllers — piston, rope
  pulley, bearing and elevator pulley all had assembly state and an error
  readback already. `getRailStart` exposes the origin rail indices are
  measured from, so waypoints can be anchored in world space instead of
  silently shifting when a player extends the rail at the start end.
- `Create_GantryShaft` now queues `gantry_departed`, `gantry_arrived`,
  `gantry_stalled` and `gantry_assembly_failed`, so a controller can wait on
  an event instead of polling. Nothing upstream fires on any of these; the
  rail is polled for edges every five ticks, and only on shafts that actually
  have a computer attached.

### Fixed

- **`Create_GantryShaft`: `hasCarriage` and `getCarriagePosition` were looking
  for the wrong block.** The carriage-detection test was inverted — it
  required `GantryCarriageBlock.FACING` to point back at the shaft, when in
  Create it points *away* from it. The predicate therefore never matched a
  carriage on the queried rail at all; what it did match was a carriage one
  block away belonging to a *different* rail two blocks away. Since direction
  iteration starts at `DOWN`, the practical effect was "reports the carriage
  below the rail, never its own" — most visible on a stacked gantry, where
  rail, carriage and second rail are exactly that geometry. Reported in #24.
- **`Create_GantryShaft`: rail length and index ran past the end of the rail.**
  The rail walk accepted any neighbouring shaft on the same *axis*; Create
  joins shafts into one rail only on exact facing equality. Two abutting
  shafts facing opposite ways are two rails, and sneak-placing against an
  existing shaft deliberately gives you the opposite facing — so this was one
  keystroke away, not a corner case. Affected `getRailLength`, `getRailIndex`
  and the origin `getCarriagePosition` measures from.
- **`Create_GantryShaft` could load, and generate, chunks from a Lua call.**
  The rail walk called `getBlockState` up to 256 times per direction with no
  load check, and `Level.getBlockState` resolves its chunk with
  `requireChunk = true`. A rail pointing into ungenerated terrain would
  therefore generate it, synchronously, on the server thread. The walk now
  stops at unloaded chunks, as Create's own does.
- **`Create_GantryShaft` documented a redstone behaviour that does not
  exist.** `isPowered` claimed powering a shaft inverts the carriage's
  direction of travel and `getMovementSpeed` claimed to factor that inversion
  in. Create's `getPinionMovementSpeed` reads no redstone state whatsoever. A
  powered gantry shaft stops translating its carriage and transmits rotation
  into the carriage's output shaft instead — the docs had it backwards from a
  real mechanic. Also now documented: the ±0.49 blocks/tick clamp, and that
  `canAssembleOn` answers per-shaft, with `single` shafts never able to
  assemble and the two ends requiring opposite signs.

### Docs

- `altitude_sensor.getVerticalSpeed` documented a distinction no Lua caller
  can observe. The note said "server-side only — returns 0 on the client
  side", which describes the block entity: the finite difference is
  accumulated in a tick handler that returns early on a client level. But
  CC:Tweaked runs Lua on the server, so a peripheral call never sees the
  client copy. Reworded to what is actually observable — one tick of lag, and
  0 until the sensor has ticked twice after being placed or loaded. Same
  wording leak removed from four `gas_provider` balloon getters ("or 0 if no
  server-side balloon" → "or 0 if no balloon is attached"); in a peripheral
  call an attached balloon is always the server one.

## [0.5.2] - 2026-07-25

### Added

- **Releases now publish to CurseForge.** The CurseForge listing had been
  stuck on 0.4.0 since 2026-05-11 while Modrinth carried through 0.5.1, so
  anyone browsing from a launcher's CurseForge index saw a build three
  releases old. Note that Create Aeronautics — which supplies the required
  `simulated` dependency — is published only on Modrinth, and still has to
  be installed from there.

### Fixed

- **Runs on Create: Simulated 1.3.0.** `mods.toml` has always declared
  `simulated [1.2.1,)`, but 1.3.0 renamed `getLaserRange` to
  `getRaycastLength` and retyped the recovery-compass placer component from
  `String` to `UUID`, so the jar would not in fact have run against it. Both
  are bridged: one jar now covers Simulated 1.2.1 through 1.3.0, and Sable
  1.1.x through 2.0.x, with no change to any Lua-visible value.
- `mounted_potato_cannon`: `getAimingVector` and `getBarrelPos` returned
  body/local-frame values instead of the world-frame values their docs
  promise — the aim came back as a fixed body axis (e.g. `{1, 0, 0}`) and
  the muzzle position carried raw sub-level local coordinates. Both now
  project out of the host sub-level (aim rotated into world frame, muzzle
  position projected to world frame), and pass through unchanged when the
  cannon isn't on a sub-level ([#18], [#22]).
- `linked_typewriter`: the `key` event fired before the keypress had been
  applied and carried an "alive" flag read from the pre-update state. It now
  fires once the key has registered, with `true` as the third argument.

### Changed

- Startup logs when Create: Simulated's own ComputerCraft registration is
  suppressed, alongside Avionics' own registration lines. Avionics replaces
  that registration wholesale, and previously nothing in the log said so —
  which made reports about missing or misbehaving Simulated peripherals hard
  to read.
- Internal: the Create-side peripheral table moved out of
  `ComputerBehaviourMixin` into `CreatePeripheralRegistry`; the kinetic SCADA
  pack now routes entirely through `KineticReadback` so its two declarations
  cannot drift; the three CC integration classes share one `add()` helper.
  No behavioural change.
- CI compiles and tests against the ends of the declared upstream ranges
  (Simulated 1.2.1/1.3.0 × Sable 1.1.1/2.0.3), so an upstream rename fails a
  build instead of a user's launch. Adds the first tests in the repo.

## [0.5.1] - 2026-06-09

### Fixed

- **0.5.0 crashed on launch** for everyone: `PeripheralComposition`
  resolved Simulated's portable-engine block-entity holder during mod
  construction, before block-entity registries are bound, throwing
  "Trying to access unbound value". Suppliers are now parked and resolved
  on first lookup. 0.5.0 has been withdrawn from Modrinth and GitHub
  releases — upgrade directly from 0.4.0 to 0.5.1.
- Composed peripherals now report their generic type names: portable
  engines answer `peripheral.hasType(name, "inventory")` and are returned
  by `peripheral.find("inventory")`. 0.5.0 composed only the methods, not
  the types, so scripts that locate inventories by type still missed them
  (follow-up to [#15]).

## [0.5.0] - 2026-06-09 [YANKED]

### Added

- `swivel_bearing`: locking-mode control and assembly diagnostics —
  `isLocked`, `getLockingMode`, `setLockingMode` (one of `locked_always`,
  `locked_default`, `unlocked_default`, `unlocked_always`), and
  `getLastAssemblyException` (the message from the most recent failed
  assembly attempt, or nil). Public API
  `ink.astrius.create_avionics.api.simulated.SwivelBearingExt` —
  mixin-supplied interface for downstream addons.
- Kinetic SCADA pack on bare kinetic blocks: a new `KineticSource` generic
  source applies the full pack to every `KineticBlockEntity` that has no
  explicit peripheral (encased shafts, gearboxes, mixers, …). Thanks
  @TechTastic ([#6]).
- Generic-source methods now compose onto capability-supplied peripherals:
  blocks whose peripheral Avionics supplies via the NeoForge capability
  (e.g. `portable_engine`) also expose matching generic packs instead of
  shadowing them.

### Changed

- **Breaking**: the per-block `getStressCapacity` of the kinetic SCADA pack
  is renamed to `getStressContribution` (on `KineticPeripheral`,
  `SimKineticPeripheral`, and `KineticSource`). The old name collided with
  `Create_Stressometer.getStressCapacity`, which reports the network-wide
  total and keeps its name.
- `gas_provider.getGasType()`: third-party `LiftingGasType` implementations
  now return their class's simple name instead of `"unknown"`. Stock types
  still return `"steam"` / `"default"`.
- Simulated-side peripherals deduplicated against the originals Simulated
  ships: `altitude_sensor`, `gimbal_sensor`, and `linked_typewriter` now
  extend their upstream counterparts; standalone duplicates of the
  nameplate, modulating-link, directional-link, docking-connector,
  nav-table, optical-sensor, and velocity-sensor peripherals were removed
  in favor of upstream's, with method parity preserved. Thanks @TechTastic
  ([#6]).

### Fixed

- Portable engines are inventory peripherals again: the capability-supplied
  peripheral no longer drops CC: Tweaked's generic inventory methods
  ([#15]).

### Docs

- The kinetic SCADA pack is declared once on the `KineticScadaSurface`
  interface; the cct-javadoc fork gained interface scanning
  (1.10.0-mainthread.4) so `@LuaFunction` default methods on interfaces
  document correctly.
- `StressGauge`: use `@code` instead of `@link` for non-peripheral types so
  references render instead of breaking.

## [0.4.0] - 2026-05-11

### Added

- Optional integration with [Create: Offroad](https://maven.ryanhcode.dev/).
  Adds the `wheel_mount` peripheral.
- `wheel_mount`: kinetic SCADA surface plus two independent script overrides.
  `setSteering(-1..1)` / `clearSteering` bypasses the side-redstone read,
  `setBrake(0..1)` / `clearBrake` bypasses the top-of-block redstone read.
  Both are persisted across save/load and synced to the client so the
  wheel visuals match server-side physics. Reads expose tire presence and
  radius, suspension extension, angular velocity, contact-block friction,
  and a `isLiftedUp` flag.
- Public API `ink.astrius.create_avionics.api.offroad.WheelMountExt` —
  mixin-supplied interface for downstream addons driving the overrides
  from Java.

## [0.3.1] - 2026-05-11

### Added

- `gyroscopic_propeller_bearing`: persistent manual-target override that
  replaces the bearing's automatic gravity tracking. `setManualTarget`,
  `clearManualTarget`, `getManualTarget`. State is synced to the client
  and persisted across save/load. The bearing's 12° cone clamp, redstone
  power gate, and stabilization-strength gate still apply on top.
  `setManualTarget` rejects non-finite components and zero-length vectors
  to avoid propagating NaN through Sable physics.
- `gyroscopic_propeller_bearing`: diagnostic reads — `getBlockNormal`,
  `getTiltAngle` (degrees off normal), `getStabilizationStrength` (the
  `[0, 1]` gain combining "contraption assembled / spinning ≥ 1 RPM /
  not in disassembly slowdown").
- Public API
  `ink.astrius.create_avionics.api.aero.GyroscopicPropellerBearingExt`
  — mixin-supplied interface for downstream addons driving the override
  from Java.

### Removed

- `gyroscopic_propeller_bearing.setTilt` and
  `gyroscopic_propeller_bearing.setStrictTilt`. These never appeared on
  the live peripheral in 0.3.0 — CC silently dropped them because
  `List<Double>` is not a supported `@LuaFunction` parameter type. Even
  with marshalling corrected they would not have delivered on their
  documented contract: they are single-tick step primitives that the
  bearing's own physics tick overwrites on the next tick. Use
  `setManualTarget` instead.

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

[#6]: https://github.com/SolAstrius/CreateAvionics/pull/6
[#15]: https://github.com/SolAstrius/CreateAvionics/issues/15
[#18]: https://github.com/SolAstrius/CreateAvionics/issues/18
[#22]: https://github.com/SolAstrius/CreateAvionics/issues/22

[Unreleased]: https://github.com/SolAstrius/CreateAvionics/compare/v0.6.0...HEAD
[0.6.0]: https://github.com/SolAstrius/CreateAvionics/compare/v0.5.2...v0.6.0
[0.5.2]: https://github.com/SolAstrius/CreateAvionics/compare/v0.5.1...v0.5.2
[0.5.1]: https://github.com/SolAstrius/CreateAvionics/compare/v0.5.0...v0.5.1
[0.5.0]: https://github.com/SolAstrius/CreateAvionics/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/SolAstrius/CreateAvionics/compare/v0.3.1...v0.4.0
[0.3.1]: https://github.com/SolAstrius/CreateAvionics/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/SolAstrius/CreateAvionics/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/SolAstrius/CreateAvionics/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/SolAstrius/CreateAvionics/releases/tag/v0.2.0
