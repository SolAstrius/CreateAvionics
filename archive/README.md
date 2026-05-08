# Archive

Code that's been moved out of the active source tree but preserved in
git for later revival. These files don't compile and aren't part of the
build — that's intentional. They can be moved back into a source root
when we're ready to integrate them.

## Contents

### `controller/`

The radar controller subsystem from Create Radar @ `dbd02ac` — auto
yaw, auto pitch, fire control, network filterer, IFF transponder, and
shared physics-bearing helpers. Forge 1.20.1 originals; would need
NeoForge porting plus rework against Sable instead of VS2 before
re-integration.

Originally at `common/src/main/java/com/happysg/radar/block/controller/`.

To revive (e.g.):

```
git mv archive/controller common/src/main/java/com/happysg/radar/block/controller
```

…then port imports and physics calls.
