# Attribution

Create: Avionics redistributes portions of [Create: Simulated][simulated]
and [Create: Aeronautics][aeronautics] under the terms of the MIT License
(see LICENSE.md). Copyright on redistributed code remains with the original
authors.

## Redistributed source

The following files are derived from upstream Create: Simulated and have been
re-namespaced into `ink.astrius.create_avionics.*`:

```
compat/simulated/peripherals/AltitudeSensorPeripheral.java
compat/simulated/peripherals/AnalogTransmissionPeripheral.java
compat/simulated/peripherals/DirectionalLinkPeripheral.java
compat/simulated/peripherals/DockingConnectorPeripheral.java
compat/simulated/peripherals/GimbalSensorPeripheral.java
compat/simulated/peripherals/LinkedTypewriterPeripheral.java
compat/simulated/peripherals/ModulatingLinkPeripheral.java
compat/simulated/peripherals/NamePlatePeripheral.java
compat/simulated/peripherals/NavTablePeripheral.java
compat/simulated/peripherals/OpticalSensorPeripheral.java
compat/simulated/peripherals/PhysicsAssemblerPeripheral.java
compat/simulated/peripherals/PortableEnginePeripheral.java
compat/simulated/peripherals/SimPeripheral.java
compat/simulated/peripherals/SteeringWheelPeripheral.java
compat/simulated/peripherals/SwivelBearingPeripheral.java
compat/simulated/peripherals/TorsionSpringPeripheral.java
compat/simulated/peripherals/VelocitySensorPeripheral.java
compat/wired/DockingConnectorWiredElement.java
compat/wired/DockingConnectorWiredElementImpl.java
compat/wired/NoopDockingConnectorWiredElement.java
compat/AttachedComputerHandler.java
compat/aeronautics/peripherals/GasProviderPeripheral.java
api/aero/GasProviderData.java
```

Original authors include RyanHCode, Edgar Onghena, TechTastic, and the
Simulated Team.

## Imported source: Create Radar

Create Radar shipped under MIT for 17 months. The upstream maintainers
relicensed to "look but don't fork" terms on 2026-05-04, hours after
closing a Sable-port PR. The MIT License is, by design, irrevocable for
already-distributed code — so the work of those 17 months is permanent
community property regardless of any later opinions about whether it
should have been. This fork picks up where MIT left off.

Create: Avionics incorporates source and assets from [Create Radar][radar]
at commit `03add64a8327420f6097b034c7019e7db2977a26` ("fixed yaw fixed
pitch fixed autotargeting fixed LOS fixed datalink texture bug",
2026-05-04 10:49 UTC-5), which was the last commit on the NeoForge 1.21.1
port branch (`Neoforge-1.21.1-DEV`) before the upstream project relicensed
two and a half hours later. The branch was published to the public
remote during the MIT era; the snapshot at `03add64` was distributed
under the MIT License, and that grant is irrevocable for all recipients
including this project.

The MIT-era master branch (last coherent commit `dbd02ac`) targets Forge
1.20.1; the NEO port branch at `03add64` targets NeoForge 1.21.1 and
matches Avionics' own runtime. Subsequent upstream commits — including
the actual LICENSE-file swap (`de7a6bf` on master, `e999a93` on NEO) —
do not retroactively alter the terms of the imported snapshot.

Imported tree:

```
common/src/main/java/com/happysg/radar/**
common/src/main/resources/assets/create_radar/**
common/src/main/resources/data/create_radar/**
common/src/main/resources/create_radar.mixins.json
```

Primary contributors (by commit count, MIT era): happygill (96 commits,
the original creator), Aycer-8187 (74), KrimZik (39), Ondřej Lenikus (37),
VladisCrafter (17), bmartin127 (11), koiboi-dev (7, primary author of the
ComputerCraft integration), Kertik_Creative (7, translations),
CeoOfGoogle1 (6), and others. Copyright on each imported file remains
with the respective author.

## Apache Commons Math

The `com/happysg/radar/math3/` package, imported as part of the Create
Radar snapshot above, is a vendored copy of Apache Commons Math
(Copyright 2001–2016 The Apache Software Foundation), distributed under
the Apache License, Version 2.0. The original NOTICE file is preserved at
`common/src/main/java/com/happysg/radar/math3/NOTICE.txt`. The Apache
License is compatible with the MIT License under which the rest of this
project is distributed.

## Trademarks

"Create", "Create: Simulated", and "Create: Aeronautics" are projects of their
respective authors. This addon is unaffiliated with The Simulated Team, The
Creators of Aeronautics, or the CC: Tweaked project.

[simulated]: https://github.com/Creators-of-Aeronautics/Simulated-Project
[aeronautics]: https://modrinth.com/mod/create-aeronautics
[radar]: https://github.com/Arsenalists-of-Create/Create-Radar
