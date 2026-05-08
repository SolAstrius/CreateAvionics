---
module: [kind=guide] mainthread
---

# Yielding peripheral calls and the parallel idiom

Many Create: Avionics peripherals expose **setters** that mutate world state
— `throttle_lever.setSignal`, `propeller_bearing.assemble`,
`gas_provider.setTargetAmount`, and so on. Every one of these is marked in the
docs with the line:

> Yields until the next server tick.

This page explains what that means and how to make scripts fast despite it.

## Why setters yield

CC: Tweaked runs Lua scripts on a separate thread from the one that owns
Minecraft block-entity state. Any code that mutates a block entity, plays a
sound, sends a sync packet to clients, or spawns/removes an entity **must**
run on the server thread, or it would race with vanilla's own writes.

Internally, `@LuaFunction(mainThread = true)` schedules the call to run at the
start of the next server tick (50 ms / 20 Hz) and suspends the Lua coroutine
until the result is ready. Read-only getters (`getSignal`, `getAngle`,
`isAssembled`, ...) don't have to do this and return immediately.

This is structural, not lazy: there is no version of these setters that
doesn't yield, because the underlying writes require the server thread. We
can't safely "fix" it.

## What sequential setter loops cost you

```lua
local fl = peripheral.wrap("create_avionics:analog_transmission_0")
local fr = peripheral.wrap("create_avionics:analog_transmission_1")
local bl = peripheral.wrap("create_avionics:analog_transmission_2")
local br = peripheral.wrap("create_avionics:analog_transmission_3")

while true do
  local roll, pitch, yaw, lift = autopilot()
  fl.setSignal(mix(lift, +pitch, +roll, +yaw))    -- yield 1 tick
  fr.setSignal(mix(lift, +pitch, -roll, -yaw))    -- yield 1 tick
  bl.setSignal(mix(lift, -pitch, +roll, -yaw))    -- yield 1 tick
  br.setSignal(mix(lift, -pitch, -roll, +yaw))    -- yield 1 tick
end
```

Four sequential setters → **4 ticks per autopilot iteration → 5 Hz**, even
though every write could land on the same tick.

## The fix: `parallel.waitForAll`

```lua
while true do
  local roll, pitch, yaw, lift = autopilot()
  parallel.waitForAll(
    function() fl.setSignal(mix(lift, +pitch, +roll, +yaw)) end,
    function() fr.setSignal(mix(lift, +pitch, -roll, -yaw)) end,
    function() bl.setSignal(mix(lift, -pitch, +roll, -yaw)) end,
    function() br.setSignal(mix(lift, -pitch, -roll, +yaw)) end
  )
end
```

All four mainThread tasks are queued before the next tick boundary, the
server thread executes all four in that one tick, and four `task_complete`
events fire back into the Lua VM. The autopilot now runs at **20 Hz**.

`parallel.waitForAll` is a cooperative scheduler in pure Lua (~50 lines in
CC's `parallel` API). It runs the supplied functions as coroutines, switching
between them whenever they yield, and returns when all have finished. There
is no real concurrency — only one coroutine is running at any instant — but
because every yield is "wait for the next event from the server," batching
multiple suspended coroutines lets a single tick fulfill all of them.

## When `parallel` *doesn't* help

- **A single setter call.** One `setSignal` always costs one tick. Wrapping
  it in `parallel` is just overhead.
- **Reads.** Most read-only getters don't yield, so the win is small. The big
  exception is reading from peripherals across a wired network — those can
  yield, and `parallel` does help there.
- **Racing the same write.** Don't fire two `setSignal(0)` and `setSignal(15)`
  in parallel branches: both land on the same tick, and the order isn't
  defined. Use `parallel` for *independent* writes only.

## Other useful patterns

- `parallel.waitForAny(f1, f2)` returns as soon as **any** function finishes
  — pair with `os.startTimer` to put a deadline on a slow `mainThread` call.
- Inside one coroutine the code is still serialized; if you want N writes to
  go in parallel, you need N branches.
- Don't pre-call: `parallel.waitForAll(setSignal(5), ...)` evaluates
  `setSignal(5)` *before* `parallel` ever runs. Always pass functions —
  either references or lambdas.

## Methods that yield

The following methods are all `mainThread` and will yield until the next
tick:

| Peripheral             | Method                                  |
|------------------------|-----------------------------------------|
| `analog_transmission`  | `setSignal`, `releaseSignal`            |
| `gas_provider`         | `setTargetAmount`                       |
| `gyroscopic_propeller_bearing` | `setTilt`, `setStrictTilt`      |
| `laser_pointer`        | `setColor`, `setRainbow`                |
| `name_plate`           | `setName`                               |
| `optical_sensor`       | `setRange`                              |
| `propeller_bearing`    | `setThrustHandedness`, `assemble`, `disassemble` |
| `throttle_lever`       | `setSignal`                             |
| `torsion_spring`       | `setLimit`                              |
