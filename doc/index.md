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

## Source

Source: <https://github.com/SolAstrius/CreateAvionics> · License: MIT.

[cc-tweaked]: https://tweaked.cc/
[simulated]: https://modrinth.com/mod/create-simulated
[aeronautics]: https://modrinth.com/mod/create-aeronautics
