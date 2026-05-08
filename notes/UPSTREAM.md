# Upstream provenance and re-derivation notes

This file documents the relationship between this fork's radar subsystem
and the upstream Create Radar project. It is a paper trail for what we
imported, what we did not, and how we handle anything else we want.

## Sources

Two MIT-licensed snapshots of the upstream are relevant:

- **`03add64` on `Neoforge-1.21.1-DEV`** (NEO MIT tip, 2026-05-04). NeoForge
  1.21.1 target. **Used as the primary import source.**
- **`dbd02ac` on `master`** (Forge MIT tip, 2026-05-03). Forge 1.20.1 target.
  **Used as a secondary source for content that was empty-stubbed in `03add64`.**

Anything past these commits on either branch is under the upstream's later
"source-available" license and is not used.

## What's empty at `03add64`

When the NeoForge port branched (`90feed5 "begin neoforge port"`),
Aycer-8187 emptied dozens of files (`git rm`-equivalent into empty blobs)
intending to re-port them. Most were never re-ported under MIT before the
relicense. The full list is:

- Anti-radar suite: `AntiRadarGuidanceBlock(Entity)`, `HomeJamGuidanceBlock(Entity)`, `ShieldJammerBlock(Entity)`
- Variant radars: `SonarBearingBlock(Entity)`, `SonarPanel`, `CannonMountRadomeBlock`/`BlockEntity`
- Track controller: `TrackControllerBlock(Entity)`, `TrackLinkBehavior`
- Guided fuze: `GuidedFuzeItem`
- Other: `MechBearingPitch`, `WeaponNetworkRuntime`, `RadarGuidanceBlock(Entity)Item`, `SmartMountRenderer`/`Visual`, `SirenBlockEntity`, `ShupapiumACContraptionAccessor`, `CBCCompatRegister`, `RadarLinkConfigurationPacket`
- Many block models, item models, recipes, loot tables
- All sounds (`sounds.json`, `spotted.ogg`, `tracked.ogg`)
- Some textures
- Slovak translation `sk_sk.json`

These are present in our import but empty.

## Strategy by category

### Drop entirely (out of scope for this fork)

- CBC integration (`compat/cbc/`, `CBCCompatRegister`, guided fuze, MountedPotatoCannon-derived stuff). We do not depend on Create Big Cannons.
- VS2 integration (`compat/vs2/*`). Replaced with native Sable.
- Shupapium mixin and other third-mod compat we are not adopting.

### Port from `dbd02ac` (Forge MIT) when needed

Anything we want to keep that is empty at `03add64`. The Forge 1.20.1
source has content, MIT-licensed; we port `net.minecraftforge.*` →
`net.neoforged.*` and adapt to 1.21.1 API ourselves. No exposure to
post-relicense code.

### Re-derive independently

For features we want that exist nowhere in MIT-era source, or for fixes
where the upstream cursed-era patch is one obvious correct line and we
would rather write the proper version ourselves.

## What we DID import from `03add64`

The successfully-ported (non-empty) files at `03add64` cover the core
radar bearing, monitor multiblock, pitch/yaw controllers, datalink, fire
controller, plane radar, RWR receiver shell, network filterer, identification
transponder, and the ComputerCraft peripherals (CCCompatRegister and the
seven peripheral classes). These were imported wholesale and form the
basis we build on.

## License

All imported source remains under the MIT License granted at the time of
distribution under that snapshot. See `LICENSE.md` and `NOTICE.md`.
