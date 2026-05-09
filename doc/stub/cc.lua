--[[-
Stubs for the CC: Tweaked globals our docs reference. Lets illuaminate
tokenize and highlight Lua snippets in guide pages. Not shipped at runtime.

@module _G
]]

--- Pause execution for `time` seconds.
function sleep(time) end

--- Find and control peripherals attached to this computer.
peripheral = {}

--- Find a peripheral of a given type.
function peripheral.find(type) end

--- Wrap a peripheral by name.
function peripheral.wrap(name) end

--- The os API.
os = {}

--- Pull an event from the queue, optionally filtered by name.
function os.pullEvent(filter) end

--- Pull an event from the queue, ignoring `terminate` filtering.
function os.pullEventRaw(filter) end

--- Start a timer; returns the timer id.
function os.startTimer(seconds) end

--- A simple way to run several functions at once.
parallel = {}

--- Run all functions until each finishes.
function parallel.waitForAll(...) end

--- Run all functions until any finishes.
function parallel.waitForAny(...) end
