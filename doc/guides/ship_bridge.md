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

On the ground a controller bridges nothing.

## What is bridged

Everything a wired modem could share is bridged: Avionics' own blocks, every
Simulated and Aeronautics peripheral, and CC's generic peripherals such as chests,
tanks, clutches and gearshifts. These are left out:

- **Computers, turtles and modems.** They network themselves; bridging them would
  loop the network or show a computer to itself.
- **The controllers themselves,** except once: the ship's controller appears as
  `ship_avionics_controller`, without a number, whichever controller publishes.
  It is the ship's [radio](#radio-ads-b), and that is ship-wide.
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

## Radio (ADS-B)

The controller is the ship's radio. It works like ADS-B: a ship with its
transponder on broadcasts who it is and what its instruments read a couple of
times a second, and
every controller in range hears it, on another ship or on the ground. A
controller on the ground is a ground station: it never transmits, but it hears
everything around it.

### Out: what the ship sends

While the transponder is on, the ship broadcasts a report with:

- its `address`: six hex digits derived from the ship itself, like an
  aircraft's ICAO address. It never changes, so it tells apart ships that share
  a callsign (`radio.getAddress()` reads your own);
- its `callsign`: the ship's name until you set one, or its address when it has
  no name;
- its `squawk`: four octal digits, `"1200"` until you set one;
- what its instruments measure, and only that:

| Field | Sent when the ship carries |
|---|---|
| `altitude` (blocks) | an altitude sensor; with only a barometer, to the nearest 10 |
| `verticalRate` (blocks/s) | an altitude sensor |
| `heading` (0 north, clockwise, the way the [bow](#the-bow) points) | a navigation table holding a compass |
| `speed` over the ground (blocks/s) | a velocity sensor on the ship's forward axis |

A field the ship can't measure is nil. No positions are sent: the radio says who
is out there and what they report, not where they are.

Only one controller per ship sends, so a ship is one transmitter however many
controllers it carries. Hit the controller's top (the antenna) with a wrench to
switch the transponder, or use `ship_avionics_controller` from any computer on
the ship:

```lua
local radio = peripheral.find("avionics_controller")
radio.setCallsign("HERON-2")   -- 1 to 12 letters, digits or -
radio.setSquawk("7000")        -- or 7000; four digits 0 to 7
radio.setTransponderPower(20)  -- dBm, 0 to 40; default 30
radio.setTransponder(false)    -- radio silence; the receiver still listens
```

Callsign, squawk, power and the switch are the ship's: setting them on any
controller sets every controller on that ship, and they are saved.

Transmit power sets how far the ship is heard. At the default 30 dBm it carries
about 3000 blocks in clear weather; every 20 dB less divides that by ten (10 dBm:
about 300 blocks, 0 dBm: about 100). Rain shortens it a little, snow half as much.

### In: what the controller hears

The receiver is always on, whatever the transponder switch says. It keeps a
**track** for each ship it hears (never its own ship): the ship's latest report,
how strong it came in (`signal`, in dBm) and how many seconds ago (`age`).

```lua
local function num(v, fmt) return v and fmt:format(v) or "--" end
for _, t in ipairs(radio.getTraffic()) do
  print(("%-12s %s %s  alt %s  hdg %s  %4.0f dBm  %.1fs ago"):format(t.callsign, t.address, t.squawk,
    num(t.altitude, "%d"), num(t.heading, "%03d"), t.signal, t.age))
end
```

A message is heard while its signal is at or above the noise floor,
`radio.getNoiseFloor()` (-100 dBm).

For every message rather than the latest per ship, `getMessages(since)` reads
the last messages received, each with a `sequence` number. Pass back the
`sequence` it returned to get only what came in since:

```lua
local seq = 0
while true do
  local batch = radio.getMessages(seq)
  seq = batch.sequence
  for _, m in ipairs(batch.messages) do print(m.sequence, m.callsign, m.signal) end
  sleep(1)
end
```

### Events

Every computer attached to the controller, directly or through the bridge, gets:

- `traffic_new` (peripheral name, callsign, address) when a ship is first heard;
- `traffic_lost` (peripheral name, callsign, address) when its track is dropped.

```lua
while true do
  local event, side, callsign, address = os.pullEvent()
  if event == "traffic_new" then print("new contact", callsign)
  elseif event == "traffic_lost" then print("lost", callsign) end
end
```

### Limits

A controller keeps at most 64 tracks. When the table is full, a new ship takes
the place of the stalest track, then the weakest; a newcomer weaker than every
ship heard in the same moment is left out. A track is dropped 30 seconds after
its last message. The message buffer holds the last 256 messages.

### Server config

`create_avionics-server.toml` (per world, under `serverconfig/`), section
`[radio]`:

| Key | Default | Meaning |
|---|---|---|
| `broadcastIntervalTicks` | 10 | Ticks between a ship's broadcasts |
| `maxTracks` | 64 | Tracks per controller |
| `trackExpirySeconds` | 30 | Seconds without a message before a track is dropped |
| `messageBufferSize` | 256 | Messages kept for `getMessages` |

## Radar Mk.1

The radar casts one narrow beam straight out of its face, twice a second. It
reports the first thing the beam meets: another ship, terrain, or `obstructed`
when the radar's own ship is in the way. A contact carries the `distance`, the
`closingSpeed` (positive when approaching) and the echo's `signal` in dBm. It
does not say who the target is: this is a primary radar, which only hears its
own echo.

The echo falls with the fourth power of distance and grows with the target's
cross-section, the area its bounding box shows the beam. Rain and snow weaken
it, by how hard they fall: the rate the rain gauge would read halfway along the
beam. A target is reported while its echo clears the noise floor (-100 dBm), so
range depends on the target: a ship showing a 10x10 face is seen out to about
250 blocks (`getRange()`), a bigger one further, up to 512 blocks. Terrain counts
as a 20x20 face. A comparator reads 15 right in front of the radar, falling to 1
as far out as the target could be seen, and 0 for nothing or an obstructed beam.

There is no sweep: point the radar where you want to look, or put it on a bearing.

### Identifying a contact

The radar says something is there; the radio says who is out there. Neither
says both, so a script matches them up. A level radar sees ships at about its
own height, so a good candidate is a track whose reported altitude is close to
yours; a contact with no candidate is a ship with its transponder off, or one
without an altitude instrument.

```lua
local radar = peripheral.wrap("ship_radar_fore_0")
local radio = peripheral.find("avionics_controller")
local altimeter = peripheral.wrap("ship_altitude_sensor_0")

local c = radar.getContact()
if c and c.kind == "ship" then
  local mine, found = altimeter.getHeight(), {}
  for _, t in ipairs(radio.getTraffic()) do
    if t.altitude and math.abs(t.altitude - mine) < 15 then found[#found + 1] = t.callsign end
  end
  print(("%d m ahead: %s"):format(c.distance, #found > 0 and table.concat(found, " or ") or "unidentified"))
end
```

## Weather radar

The weather radar hears rain, snow and cloud instead of ships. Its dish turns
once every 12 seconds. Each turn is a sweep at one elevation angle: 64 radials
(5.6° each) of 64 range bins, 32 blocks each, out to 2048 blocks. Radial 1 is
the block's front (the way you faced when placing it), and the radials go
clockwise as seen from above. On a ship the beam stays level with the horizon
and turns with the hull, so add the ship's heading for a north-up picture.

Each bin carries the **reflectivity** in dBZ and the **radial velocity** of
what is in it, in blocks per second, positive away from the radar.

- Precipitation follows the usual Z-R relations: rain Z = 200 R^1.6 (1 mm/h is
  23 dBZ, 10 mm/h is 39, 25 mm/h is 45) and snow Ze = 400 R². Above the
  freezing level, rain shows as ice, 7 dB weaker and thinning with height.
  Nothing returns from above the cloud top.
- The echo falls with the square of distance. The weakest one the radar hears is
  -20 dBZ at 100 blocks, -6 at 512 and +6 at 2048. Cloud without precipitation
  (-25 to -10 dBZ) shows only close in.
- What the radar reports is apparent reflectivity. Heavy rain weakens the beam
  (25 mm/h costs about 0.07 dB per block, each way), so a storm core shadows
  what is behind it.
- Terrain in the beam returns strong ground clutter (55 dBZ, 25 off water), and
  nothing is heard behind it. Site the radar high, or tilt the beam up with
  `setElevations`. The radar's own hull blocks the beam as well.
- Clutter moves only with the radar. On the ground, `setClutterFilter(true)`
  removes it, along with any weather that happens to move across the beam.
- Velocity needs a weather mod that reports wind. Without one the velocity bins
  read "no data".

Where the weather comes from: a weather mod that sends a weather field (see
[For mod authors](instruments.md#for-mod-authors-weather-sources)) gives the
radar its whole range, cloud heights, the freezing level and wind. Without one,
the radar sees what the rain gauge would read at the surface: vanilla's rain
(the same everywhere it falls) reaching up to the cloud layer at y 192, and only
over loaded chunks. Beyond them it reads "no data".

`getSweep()` returns the latest sweep, with each radial as a binary string of
one byte per bin. `0` is no echo, `1` beam blocked, `2` no data. A
reflectivity byte `b` of 3 and up is `(b - 3) / 2 - 32` dBZ. A velocity byte
is `(b - 129) / 4` blocks per second. Computers get a `weather_radar_sweep`
event each time a turn completes. The weather is sampled on a shared grid under
a per-tick budget (`[weather_radar] columnsPerTick` in the server config, default
32). Many radars therefore see older weather, but they never cost more.

This draws the sweep on a monitor, with the dish's front at the top:

```lua
local radar = peripheral.find("weather_radar")
local mon = peripheral.find("monitor")
mon.setTextScale(0.5)
-- Reflectivity colours, 5 dBZ steps from 5 dBZ, in palette slots 1..14
local scale = {0x04e9e7, 0x019ff4, 0x0300f4, 0x02fd02, 0x01c501, 0x008e00, 0xfdf802,
               0xe5bc00, 0xfd9500, 0xfd0000, 0xd40000, 0xbc0000, 0xf800fd, 0x9854c6}
for i, rgb in ipairs(scale) do mon.setPaletteColour(2 ^ i, rgb) end
mon.setPaletteColour(2 ^ 0, 0x404040)   -- "0": blocked or no data
mon.setPaletteColour(2 ^ 15, 0x000000)  -- "f": nothing
local W, H = mon.getSize()
local cx, cy = (W + 1) / 2, (H + 1) / 2
local R = math.min(cx, cy * 1.5)        -- a character is about 1.5 times taller than wide

local function draw(s)
  for y = 1, H do
    local row = {}
    for x = 1, W do
      local dx, dy = x - cx, (y - cy) * 1.5
      local r = math.sqrt(dx * dx + dy * dy) / R * s.bins
      local c = "f"
      if r < s.bins then
        local az = math.atan2(dx, -dy) % (2 * math.pi)
        local b = s.reflectivity[math.floor(az / (2 * math.pi) * s.radials) + 1]:byte(math.floor(r) + 1)
        if b == 1 or b == 2 then
          c = "0"
        elseif b >= 3 then
          local level = math.floor(((b - 3) / 2 - 32 - 5) / 5) + 1
          if level >= 1 then c = ("%x"):format(math.min(level, 14)) end
        end
      end
      row[x] = c
    end
    local line = table.concat(row)
    mon.setCursorPos(1, y)
    mon.blit((" "):rep(W), line, line)
  end
end

local s = radar.getSweep()
if s then draw(s) end
while true do
  os.pullEvent("weather_radar_sweep")
  draw(radar.getSweep())
end
```

For a volume scan, set several elevations: `radar.setElevations({0.5, 2, 5, 10})`.
The radar takes one per turn, and `getSweep(i)` returns the latest sweep at
elevation `i`. A bin's height above the dish is its distance times the sine of
the elevation. The highest elevation where a bin still reads 18 dBZ or more
gives the echo top over it.
