# Archived for v2 — Sable-aware bearing control

These files implement *sublevel-aware bearing control* using VS2
Clockwork's `PhysBearingBlockEntity` and the corresponding
ship-frame cannon-targeting math. They are archived (preserved in git,
out of the compile path) because Sable doesn't currently expose a
direct equivalent — Simulated's `SwivelBearingBlockEntity` is
sublevel-aware but its target angle is computed internally and not
externally settable.

## Path forward (v2)

The intended revival is **option (a)** from the radar-fork scope
discussion: add a Mixin into Sable's `SwivelBearingBlockEntity` that
exposes a public `setTargetAngleDegrees(double)` (and the corresponding
"externally controlled" flag pattern we use elsewhere — see
`AnalogTransmissionBlockEntityMixin` for the model). Once that's in
place:

1. Move `PhysBearingPitch.java` / `PhysBearingYaw.java` back to
   `block/controller/pitch/` / `yaw/` and rebind from
   `PhysBearingBlockEntity` (VS2 Clockwork) to
   `SwivelBearingBlockEntity` (Simulated).
2. Move `PhysBearingCommon.java` back to `block/controller/utils/`,
   replacing `getShipIfPresent` with a Sable-Companion lookup
   (`SableCompanion.INSTANCE.getContaining(level, pos)`) and
   `toShipSpace` with `subLevel.logicalPose().transformPositionInverse(...)`.
3. Restore the PHYS mount-kind code paths in
   `AutoYawControllerBlockEntity` and `AutoPitchControllerBlockEntity`,
   renaming PHYS → SUBLEVEL.
4. Move `VS2CannonTargeting.java` / `VS2TargetingSolver.java` back to
   `compat/cbc/` and rebind ship-frame transforms to sublevel-frame.
   The targeting math (cannon ballistics + lead) is identical; only
   the source of "where am I in body coords" changes.

## What v1 keeps

The CBC mount path (`CannonMountPitch.java`, `CannonMountYaw.java`)
stays active and drives Create Big Cannons cannon mounts directly,
without sublevel awareness. This covers the most common case: a
turret built on the static world.

The CC peripheral surface (`yaw_controller.setAngle / .stopAuto`,
`pitch_controller.setAngle / .stopAuto`, `fire_controller.*`) keeps
working with this CBC-only mount kind in v1.
