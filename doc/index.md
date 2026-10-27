# Create: Avionics

Create: Avionics adds [CC: Tweaked][cc-tweaked] peripherals for the
[Create: Simulated][simulated] and [Create: Aeronautics][aeronautics]
ecosystems. It's a drop-in addon — no fork, no patches.

## Peripherals

The sidebar lists every peripheral exposed to ComputerCraft. Each page documents
the methods callable from Lua — argument types, return values, and any
mainThread / yield notes.

Wrap a peripheral the usual way:

```lua
local engine = peripheral.find("portable_engine")
print(engine.getRpm())
```

## Blocks

Avionics also adds blocks of its own: weather instruments, the avionics
controller that bridges a ship's peripherals to its computers, and a radar. Start
with the guides to the [ship bridge](guide/ship_bridge.html) and the
[instruments](guide/instruments.html).

## Community

Questions, scripts and ships: [Discord](https://discord.gg/yZxktK59ux), or [Telegram](https://t.me/+c3qBAZD2bUw2YjQy) in Russian.
Bugs go to [GitHub issues](https://github.com/SolAstrius/CreateAvionics/issues).

## Source

Source: <https://github.com/SolAstrius/CreateAvionics> · License: MIT.

[cc-tweaked]: https://tweaked.cc/
[simulated]: https://modrinth.com/mod/create-aeronautics
[aeronautics]: https://modrinth.com/mod/create-aeronautics
