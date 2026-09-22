# NeoFactory

A 2D top-down sandbox game in the spirit of Minecraft, built with
[libGDX](https://libgdx.com/).

The camera looks straight down on the world, which is a plane of square blocks. The
player walks over the terrain, digs blocks out of it and builds new ones, and the
world is stored on disk so that a session can be continued later.

## What is in the game today

- **Terrain** - noise based biomes (plains, forest, desert, snow and a rocky one)
  with grass, trees, ore clusters and decorative plants, all generated from the
  world seed.
- **Two block layers** - the floor the player walks on and the object layer the
  player stands in, which holds trees, plants and everything the player places.
- **Digging and building** - the left button breaks the aimed block while it is
  held, the right button builds the held block, both limited by the reach of the
  player and both able to address the layer below the feet.
- **Inventory and hotbar** - nine hotbar slots, a full inventory screen, and
  dropped items that fall on the ground and are picked up by walking over them.
- **Save games** - one folder per world, a level file plus one file per chunk the
  player changed, written while playing and when leaving.
- **Chunk streaming** - the terrain around the player is kept in memory and the
  terrain behind is dropped, which keeps memory use flat no matter how far the
  player walks.
- **Entities** - a framework the player and dropped items are built on, with one
  type registry and one save layout they share.
- **World** - a seeded generator builds the terrain from noise fields: regions of grass, sand,
  rock and snow, ore clusters in the stone, and the water of the landscape. A river follows the
  contour line of a field of its own, so it winds and is as wide as the slope of the field allows;
  a lake is a lobe of a fractal field and never a circle; a pool of lava is dropped by a
  decoration instead of a biome, the way the original drops it into a hollow. The bed of a river is
  gravel, clay and sand, the water stands above it in the layer the player walks in, and every cell
  of it is a source. What holds a body of water is the ring of sand, gravel and stone around it -
  dig one of those cells away and the water runs into the hole.
- **Machines** - the furnace, the first machine of the game: it is built from an item,
  keeps its slots, its fuel and its work in a block entity that travels with its chunk,
  runs in fixed ticks as long as its chunk is loaded, and hands what it holds back to
  the player when it is broken. Using it opens its screen: the slots it works with above,
  the inventory of the player below, the progress of the craft between them and a line
  that writes how much fuel is left. The contract behind it - slots and tanks with a
  role, energy, fluids and the recipe types a machine reads - is what the larger machines
  will be built from, and a test builds a reactor of that shape out of the same pieces.
- **Fixed ticks** - the world advances in twenty steps a second no matter how fast the
  frames come, so a machine does the same work at any frame rate.
- **Chat and commands** - one input line at the lower left for messages and for
  commands: a line behind a slash is a command (`/help`, `/give`, `/tp`, `/seed`,
  `/gamemode`), anything else is a chat message. The recent conversation stays visible
  for a few seconds and comes back while something is typed.
- **Creative mode** - `/gamemode creative` hands out every item of the game: the
  inventory key then opens a grid of everything the game owns, with a tab for each
  group, a scroll bar and a search box, and a stack that was taken is there again right
  away. A slot of the grid shows one piece and carries no amount, because the shelf of the
  game is not a chest: the left button takes one piece, the right button a whole stack, and
  the slot fills itself again either way. Every tab carries the icon of its group, an empty
  search box lists everything
  and the scroll bar tells a list that fits into the grid from one that goes on. The tabs
  frame the panel - the first row above it and the second one below it, painted with the
  same art turned by half a circle and mirrored back left to right - and every tab shows
  the very same panel, so choosing one never moves or resizes the screen. The last tab
  swaps the grid for the slots of the player, so what the player owns is at hand while the
  other tabs hand out what the game holds. What is put into the grid is thrown away, and a
  stack dragged out of it lands on the ground. `/gamemode survival` goes back, and the mode
  is stored with the world.

- **Fluids** - water and lava stand in either layer and the two never mix: poured on the grass
  they lie in the layer the player stands in, poured into a hole they fill the ground layer, and
  a spill of one of them reaches through neither. While they are poured they follow the rules of
  a block - nothing over a hole, nothing into a taken cell, nothing that skips a layer - and then
  they spread on their own, one ring every few ticks, until they have reached as far as their
  fluid allows: seven cells for water, three for lava. A wall holds a spill back, a plant is
  flooded, a lake of water never covers lava, and a source that a bucket takes away drains
  everything that lived from it ring by ring. A bucket carries water and lava and nothing else, a
  cell carries any fluid the industry will bring: every fluid shares one grey scale picture and is
  painted while it is drawn, and a filled cell shows its fluid in the window and keeps the steel
  around it grey. An empty container stacks like any other material, a full one does not: filling
  a bucket out of a stack of empty ones leaves the rest of the stack in the hand and puts the full
  bucket into the inventory, where it takes a slot of its own.

- **Materials** - a factory holds hundreds of them, and a metal is not one item but nineteen: a
  dust, a small and a tiny pile of it, an ingot, a nugget, a plate, a foil, a rod, a long rod, a
  bolt, a screw, a ring, a round, a fine wire, a spring, a small spring, a gear, a small gear and a
  rotor. Writing an item and a picture for each of them would be hundreds of lines per material, so
  the game keeps the two apart: `Material` says what a material is - a name, a colour, a chemical
  formula and the shapes it comes in - `MaterialForm` says what a shape is - the picture, the name
  its item carries and how many of them fit in a slot - and `Materials` builds every item of every
  material out of those two. Ten metals bring about two hundred items with them today, and a metal
  that arrives later is one line in that file. An item is named after material and shape, so a
  plate of iron is `iron_plate` and reads as "Iron Plate" over "Fe" in its tooltip: the name first,
  the formula of the material under it, and the search box of the creative inventory finds an item
  by that formula as well. The picture of a shape holds brightness only and the colour of the
  material is multiplied over it while the item is drawn - the very trick the fluids use - so no
  material needs art of its own. The iron ingot therefore keeps the id it always had in
  `Items.IRON_INGOT_ID` although its definition now lives with the material.
## Controls

| Input | Action |
| --- | --- |
| `W`, `A`, `S`, `D` | walk |
| mouse | aim, the player looks at the cursor |
| mouse wheel | select a hotbar slot |
| `CTRL` + mouse wheel | zoom the camera instead |
| left mouse button | break the aimed block while held; a click on the hotbar selects that slot |
| right mouse button | build the held block into the aimed cell; a bucket pours its fluid into an empty cell and a bucket over a source of water or lava fills itself |
| `1` to `9` | select a hotbar slot |
| `-` / `=` | zoom out / in while held (numpad `-` and `+` do the same) |
| `[` / `]` | keep fewer / more chunks around the player |
| `SHIFT` | address the layer below the feet instead of the layer the player stands in |
| `Q` | drop one item, `SHIFT` + `Q` the whole stack: the hotbar slot during play, the slot under the mouse while the inventory is open |
| mouse outside the panel | throw the carried stack into the world while a container is open; a stack put into the creative grid disappears instead |
| `E` | open and close the inventory; while the world is played in creative mode it opens the creative inventory instead |
| typing in the search box | filter the creative inventory by name, `BACKSPACE` deletes a character; while the box has the keyboard a letter belongs into it, so `E` and `T` type instead of opening a screen, and only `ESCAPE` and the function keys still reach the game |
| `/` | open the input line with the command slash, `T` opens it for a message |
| `ENTER` | send the line: a command when it starts with a slash, a chat message otherwise |
| `UP` / `DOWN` | walk through the lines that were sent before while the input line is open |
| `ESC` | close the input line first, then the inventory, then open the pause menu |
| `F11` | switch to fullscreen |

## Modules

- `core`: the game itself, shared by every platform.
- `lwjgl3`: desktop launcher.

## Packages of `core`

| Package | Contents |
| --- | --- |
| `fluid` | what a fluid is and where it stands: the kinds, their sheet and colour, the state of a cell, the spread of a spill and the containers that carry it |
| `block` | block types and the id/name lookup table, ids are stable across save games |
| `blockentity` | what a block carries beyond its id and its state: the base class, the type registry and the machine behind a block |
| `machine` | what a machine is built from: slots and tanks with a role, energy and the recipes it runs |
| `material` | what a material is: the shapes it comes in, the colour and the formula it carries, and the items one line per material turns into |
| `item` | item types, stacks, the inventory, the hotbar selection and the sinks broken blocks hand items to |
| `world` | chunks, the world, the game mode, the chunk store interface and the generator |
| `world.decoration` | the trees and plants planted on a finished chunk |
| `world.interaction` | aiming, breaking and building |
| `world.save` | the save format, the level file, the chunk files and the entity tags |
| `entity` | entities: the base class, the type registry, the manager, the player and dropped items |
| `chat` | the input line, the messages and the commands a line behind a slash is looked up in |
| `gui` | hotbar, inventory, creative inventory and chat rendering, layout and widgets |
| `render` | world, entity, selection and font rendering |
| `screen` | the screens and the manager that switches between them |
| `input` | keyboard and mouse state |
| `util` | constants and the NBT layer the save format is built on |

## Where the game writes

The game writes into its working directory:

```
run/saves/<id>/level.dat              world fields, entities, game rules
run/saves/<id>/chunks/c.<x>.<y>.dat   one file per chunk the player changed
run/logs/neofactory.log               the log of the running game
```

`gradlew lwjgl3:run` starts the game inside `run/`, a folder that is created on
demand and ignored by git. A shipped jar writes into the directory it was started
from, so a player keeps their worlds next to the game instead of inside it: the
assets stay read only and are packed into the jar without any save game.

The level file is small and written on every save. Chunks are written into their own
files, and only chunks the player changed are stored at all: everything else is
generated from the seed again, exactly as it was, because every cell and every
decoration depends on nothing but the seed and its coordinates. That split is what
makes a save cost what was built instead of what is loaded, and it is what allows a
distant chunk to be written and dropped from memory in one step.

A stored chunk holds the id and the state of a cell as full 32 bit numbers, so the
game never runs out of room for the blocks a factory is built from: an ore, a machine,
a pipe and a cable are each just another id. The state is what a block carries beyond
its id - the direction a machine faces, the shape a pipe is drawn with - and it travels
with the chunk, while a cell whose block changes loses the state it had.

What a block carries beyond that - the slots, the buffer and the tanks of a machine - is
stored with the chunk as well, as a list of block entities next to the two arrays. An
entry whose type the game no longer knows, or one whose block is gone, is reported and
skipped, so a chunk always opens.

The format version travels with every file and is checked while reading. The game is
still being built, so nothing is converted between versions: a world written by another
version is refused with a clear reason instead of being read halfway.

## Build and run

```bash
./gradlew lwjgl3:run         # start the game
./gradlew :core:test         # run the tests
./gradlew :lwjgl3:jar        # build the runnable jar into lwjgl3/build/libs
./gradlew build              # compile and test everything
```

The tests cover what does not need a window: chunk generation and modification
flags, the chunk streamer, the save format with the ids, the states and the block
entities of a chunk, the machines and the clock that ticks them, the layout of the
screens of a machine and of the player, the entity layer, and the pictures of the
project. Everything that draws is checked by running the game, and every screen also
writes a picture of itself into `core/build/reports`.

## Art

The camera looks straight down, so a block only needs its top face: side, bottom
and front pictures of the art pack were removed from `assets/blocks`, which keeps
the folder to tiles the game can actually show. `gradlew :core:test` writes the list
of spare pictures - art nothing references yet, kept for later systems - to
`core/build/reports/texture-audit.txt`. The same test fails when a block or an item
points at a picture that is not there, and when a side or bottom face sneaks back
into the block folder.

In a slot a block item does not show that tile as a flat square: the game folds it
into a small cube at run time, see `BlockIconFactory`. The two side faces the view
meets are derived from the tile itself - its edge pulled into the depth and darkened
with every step - so no side picture is needed and none is kept. The cube is folded at
four times the cell that shows it and drawn with a smooth filter, so the steps along
its slanted edges are four pixels wide instead of sixteen and the graphics card turns
them into soft edges; `BlockIconFactory#downscale` is the same arithmetic for a preview
that cannot lean on the graphics card. Tall grass and
leaves keep their flat picture, they do not fill a cell. `gradlew :core:test` writes
a sheet that shows the result to `core/build/reports/block-icons-preview.png`, with a
few blocks enlarged in `block-icons-detail.png`.

Water and lava are the fluids of the game, and they share one picture:
`assets/blocks/generic_fluid.png` holds brightness only, and the colour of the fluid is multiplied
with it while the world draws it, see `Fluids`. The sheet is built from the still water of the art
pack by `build/verify/grayscale_fluid.ps1`, which keeps the brightest channel of every pixel, so
the frames of the animation and the transparency travel with it; the script takes a `-Source` and a
`-Target`, so any picture of the pack can become the sheet of the game. Water and lava therefore
differ in their colour alone, and a fluid of the industry that arrives later is one more colour of
the same picture. The sheet is played where the block stands: `Block.Animation` says how many cells
a picture holds and how long one is shown, `BlockAnimation` picks the frame that belongs to the
running tick of the world, and the renderer knows nothing but those two. The cell of a fluid is one
grey scale picture as well, but
only its window takes the colour of the fluid: `CellIconFactory` paints the two pixels wide and
ten pixels tall window in the middle of the cell and leaves the steel of the container grey, so a
cell of water and a cell of oil are the same object with something else inside. A new fluid needs
a colour, a line in `Fluids` and an entry in `Buckets` and nothing else. `gradlew :core:test`
paints the frames of both fluids next to the buckets and the cells into
`core/build/reports/fluid-preview.png`.

Every shape a material comes in is drawn from one grey scale picture as well, and the colour of the
material reaches it the same way: `assets/items/generic_ingot.png`, `generic_plate.png`,
`generic_gear.png` and the seventeen others hold brightness only, and `MaterialForm` names the one
the item of a shape is drawn from. `build/verify/material_forms.ps1` copies them out of the art pack
the shapes were cut from, so the names the game asks for and the files that exist are written down
in exactly one script and a shape that is renamed or lost fails the tests instead of showing up as a
missing icon. One shape covers something: a fine wire is drawn from two layers, the bright metal
around the darker core inside it, and `Item#overlayTexture` is that second layer, drawn in its own
colours while the shape below takes the tint. `gradlew :core:test` paints every shape of every
material into `core/build/reports/material-preview.png`, one row per material and one column per
shape, which is how the colours of ten metals are reviewed without starting the game.

`gui/inventory_icons.png` holds every panel of the interface. Since the creative
inventory arrived it also carries that screen's art - the two panels, the tab in its two
states and the two thumbs of the scroll bar - which `build/verify/extract_creative.ps1`
copies out of the art pack next to it. The thumbs are the pair the original game keeps
for a list that fits into the panel and for one that goes on, and the tabs are the empty
shapes it draws: the icon on a tab is the item of its group, drawn at run time the same
way a slot draws it. The sheet was grown from 256 to 512 square pixels for all of this,
and the older pictures in its upper left corner still start at the same coordinates, so
nothing that already read them had to change.

## Interface

A container screen - the inventory today, the screen of a machine tomorrow - is built
from two halves. `ContainerMenu` holds the slots, the stack the mouse carries and what a
click does with it; it knows nothing about drawing, so every gesture is covered by a test
that needs no window. `ContainerView` draws that menu: `PanelTextures` stretches the
panel picture of `gui/inventory_icons.png` in nine cells to whatever size the slots ask
for, which is why one picture serves the small inventory and a tall machine screen. The
same sheet holds the slot bevel, the crafting arrow and the flame of a furnace.

The creative inventory reuses those two halves instead of adding a third one.
`CreativeInventory` holds the whole logic - which tab is chosen, which items the group
holds, how far the list is scrolled and what the search box keeps - and is covered by
tests that need no window. The box takes the keyboard while it has the focus: the keys
that write a character belong into it and never reach a hotkey, which is what keeps a
typed `E` from closing the screen, see `CreativeInventory#isSearchKey(int)`. Its grid
is an ordinary `Inventory` of result slots, so
`ContainerMenu` shows it and hands a stack out on a click; the supply behind a slot is
endless, see `CreativeInventory#sourceAt(int)`, which is why a stack that was taken is
there again right away. The tabs are recognised by what an item *is* - a block, a machine,
a material, food, a tool, armour - so a new item lands in its group by itself; a machine
is a block that asks for a block entity, which keeps it out of the group of the plain
blocks without a line of its own.
`CreativeLayout` owns the geometry of the panel, and the drawing code and the hit
testing both read it, so a click always lands on the slot the player sees. The tabs fill
the row above the panel and, once a screen holds more of them than that row takes, the
row below it: the second one is painted with the very same pictures turned by half a
circle and mirrored back left to right, so the two rows read as one frame and a chosen
tab lights up on the same side in both of them. Putting
something into a grid slot is the one action that has no place - the slot fills itself
again - so that stack is destroyed instead of finding no room, see
`ContainerMenu#setVoidsOverflow(boolean)`; a stack dragged out of the panel lands on the
ground like anywhere else. The last tab is not a list at all: it swaps the grid of items
for the slots of the player, which lie on the very same coordinates, so the panel keeps
its size and its place and the screen never jumps between two tabs - a stack the mouse
carries stays on the mouse while the layout behind it changes, see
`ContainerMenu#setLayout(ContainerLayout)`.

A machine describes its slots and its tanks by their role - input, fuel, output, upgrade -
and `MachineMenu` turns those roles into a layout, so a new machine gets its screen
without a line of drawing code. `Machine` is what every machine is built from: an
inventory whose slots carry a role, a buffer of energy, tanks of fluid and the recipe
types it reads, and `inputs()` and `outputs()` collect both sides into the view a recipe
is offered. `MachineRecipe` is the contract a recipe follows, so one machine serves a
furnace that turns one item into another and a reactor that takes two items and a fluid in
and hands two items and a fluid out. `RecipeMachine` holds the loop they all share - find
a recipe, check that the products fit, pay for the energy, count the time, hand the
products out - which is why a machine that large only declares its slots.

A machine lives in the world as a block entity: the cell stores which block is there and
what state it has, everything else - the slots, the buffer, the tanks and the work done so
far - sits in the entity behind it, see `BlockEntity`. The game ticks them in fixed steps
of twenty per second, see `TickClock`, so a machine does the same work at any frame rate,
and only the loaded chunks are ticked: a chunk that is dropped from memory is written
first, machines and all. Breaking a machine hands what it holds to the player before the
block goes, so nothing that was put into it is lost.

The screen of a machine comes from `gui/machine_icons.png`: the panel is empty in its upper
half, where a machine stands the slots it works with, and carries the slots of the player
inventory in its lower half. Next to the panel the sheet holds a grid of icons - one per kind
of item slot, a bright and an empty pair of arrows per kind of process, the error pictures
and one icon per kind of tank - and a machine names the cells it wants through `SlotKind` and
`ProgressKind`.

What a machine shows is registered with the machine, see `MachineScreen`: the title of the
upper left corner, the pair of arrows its progress bar is drawn from, the kind of every slot
it works with and the tanks it holds. The layout turns that into a screen of its own, see
`MachineMenu`: a block of one, two, four or six slots takes the shape its size asks for - one
slot alone, two above each other, a square of four and two rows of three - the inputs to the
left of the progress bar and the products to its right, the tanks at the foot of the panel
and the slots of the player inventory on the rows its own panel carries. The column at the
right edge belongs to the upgrade slots, of which a machine may hold four: they fill it from
the lower corner upwards, and the status line of the upper right corner stops beside them.
The configure slot a machine asks for stands at the right of the foot. Every cell of the
panel is placed by rule, so a machine that holds everything the layout can carry - six slots
in, six out, four upgrades, two tanks each way and the configure slot - has no two cells on
top of each other, which a test checks cell by cell and the second preview picture shows.

A machine draws no fire: what is left of its fuel is written in the upper right corner as a
number, see `FuelMachine`, and a machine that waits for energy shows the error icon of the
sheet beside it, see `MachineError`. Everything else - the bevel of a slot, the items, the
tooltips - is the very code the inventory screen uses, see `ContainerAppearance` and
`ContainerView`: a screen only names the pictures it is drawn from. `gradlew :core:test`
writes `core/build/reports/machine-preview.png` and `machine-full-preview.png`, the screen of
the furnace and of a machine that holds everything the layout can carry, the way the game
paints them, text included and drawn with the bitmap the game uses.

Crafting reads its recipes from `assets/recipes/<type>/<name>.json`: the folder names the
type, so a new recipe is a file and not a line of code.

```
recipes/crafting_shapeless/oak_planks.json   { "ingredients": ["log_oak"], "result": { "item": "planks_oak", "count": 4 } }
recipes/crafting_shaped/stick.json           { "pattern": ["P", "P"], "key": { "P": "planks_oak" }, "result": { "item": "stick", "count": 4 } }
recipes/smelting/iron_ingot.json             { "ingredient": "iron_ore", "result": { "item": "iron_ingot" }, "time": 10.0 }
```

An ingredient may list alternatives, `"P": ["planks_oak", "planks_birch"]`, and blocks the
pattern does not cover have to stay empty. A file that cannot be read is logged and
skipped, so one typo never keeps the game from starting. `gradlew :core:test` writes
`core/build/reports/inventory-preview.png`, a picture of the inventory screen as the game
paints it, next to `panel-preview.png` for the panel itself.

Note that most tasks that are not specific to a single project can be run with a
`name:` prefix, where the `name` is the id of the project, for example `core:test`.
