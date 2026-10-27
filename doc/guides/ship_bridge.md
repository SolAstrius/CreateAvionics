---
module: [kind=guide] ship_bridge
---

# The avionics controller and the ship bridge

An **avionics controller** on a ship connects every peripheral on that ship to the
wired network it sits on. Put a controller anywhere on the hull, connect it to a
computer with a cable or a wired modem, and the computer sees the ship's sensors,
propellers and engines as if each one had its own modem.

```lua
local alt = peripheral.wrap("ship_altitude_sensor_0")
local fwd = peripheral.wrap("ship_velocity_sensor_forward_0")
print(alt.getHeight(), fwd.getVelocity())
```

On the ground a controller bridges nothing; it is then only a peripheral of its
own (see [Transponder](#transponder)).

## What is bridged

Everything a wired modem could share is bridged: Avionics' own blocks, every
Simulated and Aeronautics peripheral, and CC's generic peripherals such as chests,
tanks, clutches and gearshifts. These are left out:

- **Computers, turtles and modems.** They network themselves; bridging them would
  loop the network or show a computer to itself.
- **Other avionics controllers.** Each is already on the network it serves, and the
  transponder is ship-wide, so one is enough.
- **Blocks tagged `create_avionics:not_bridged`.** Shafts, cogwheels, gearboxes,
  encased shafts, chain drives, belts and chain conveyors: pure transmission, with
  nothing useful to ask and hundreds of them on a large ship. Packs can add to the
  tag.

The bridge rescans the ship every two seconds, and at once when a cable or modem
next to a controller changes. Calls go straight
to the block's own peripheral, so methods, units and `mainThread` behaviour are
exactly what the peripheral's own page documents.

## Names

Every bridged peripheral is named `ship_<type>_<n>`. The `ship_` prefix keeps the
names apart from the ones CC gives modem-shared peripherals, so a
`thermometer_0` on a modem and a `ship_thermometer_0` on the bridge can coexist.

A block keeps its number for as long as it stays on the ship. Removing
`ship_thermometer_0` does not renumber `ship_thermometer_1`; the next thermometer
placed takes the lowest free number. Names are saved with the controllers and
survive restarts.

### Names relative to the ship

Peripherals that measure in a direction are named by that direction in ship terms
(see [The bow](#the-bow)):

| Peripheral | Name | Example |
|---|---|---|
| Velocity sensor | by the axis it measures: `forward`, `vertical` or `lateral` | `ship_velocity_sensor_forward_0` |
| Optical sensor, laser pointer | where the beam points: `fore`, `aft`, `port`, `starboard`, `up`, `down` | `ship_optical_sensor_down_0` |
| Docking connector | the side it faces | `ship_docking_connector_up_0` |
| Radar | the side it faces | `ship_radar_fore_0` |

### Names by position

Propeller bearings, gyroscopic propeller bearings, adjustable burners, steam vents
and portable engines (the `create_avionics:named_by_position` tag) are named by
where they sit on the hull: `_fore` or `_aft` outside the middle third of the
ship's length, then `_port` or `_starboard` off the centreline. A bearing on the
centreline mid-ship has no suffix.

```
ship_propeller_bearing_aft_port_0
ship_propeller_bearing_aft_starboard_0
ship_propeller_bearing_0
```

A block keeps its position name when the hull grows around it, even if it would
now fall in another zone; blocks placed later are zoned against the hull as it is
then. When the bow changes, position names are worked out again, because fore,
aft, port and starboard now mean other places.

## The bow

A ship has one bow, shared by all its controllers. The first controller placed on a
ship sets it to the way the controller faces; the arrow on top of every controller
shows where the bow is.

To move the bow, hit a controller's side with a wrench. It turns a quarter and its
new facing becomes the ship's bow. Names relative to the ship and position names
follow straight away, so a script written for the old bow fails to find its
peripherals instead of quietly steering the wrong way.

## Several controllers

A ship can carry any number of controllers, for example one per computer room.
They share one set of names: whichever computer you ask, `ship_thermometer_0` is
the same block. One controller (the one at the lowest position) publishes; the
others stand by. If it is destroyed, the next takes over with the same names.

## Transponder

The controller is also the ship's transponder. While it is on, a
[radar](../peripheral/radar.html) that hits the ship reports its name; while it is
off, the ship shows up as an unidentified contact. The name is the ship's own
name, or a six-letter code from its id when it has none.

Hit the controller's top (the antenna) with a wrench to switch the transponder, or
use the controller's peripheral:

```lua
local controller = peripheral.find("avionics_controller")
controller.setTransponder(false) -- radio silence
```

The switch is ship-wide: every controller on the ship shows the same state.

## Radar Mk.1

The radar casts one narrow beam straight out of its face, twice a second, up to
256 blocks. It reports the first thing the beam meets: another ship, terrain, or
`obstructed` when the radar's own ship is in the way. Contacts carry the distance
and the closing speed (positive when approaching), and the name of a ship whose
transponder is on. A comparator reads 15 right in front of the radar, falling to 1
at full range, and 0 for nothing or an obstructed beam.

There is no sweep: point the radar where you want to look, or put it on a bearing.
