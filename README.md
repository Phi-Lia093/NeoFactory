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
- **Chat and commands** - one input line at the lower left for messages and for
  commands: a line behind a slash is a command (`/help`, `/give`, `/tp`, `/seed`,
  `/gamemode`), anything else is a chat message. The recent conversation stays visible
  for a few seconds and comes back while something is typed.
- **Creative mode** - `/gamemode creative` hands out every item of the game: the
  inventory key then opens a grid of everything the game owns, with a tab for each
  group, a scroll bar and a search box, and a stack that was taken is there again right
  away. Every tab carries the icon of its group, an empty search box lists everything
  and the scroll bar tells a list that fits into the grid from one that goes on. The last
  tab hands the panel over to the inventory of the player, so the crafting field comes
  with it. What is put into the grid is thrown away, and a stack dragged out of it lands
  on the ground. `/gamemode survival` goes back, and the mode is stored with the world.

## Controls

| Input | Action |
| --- | --- |
| `W`, `A`, `S`, `D` | walk |
| mouse | aim, the player looks at the cursor |
| mouse wheel | select a hotbar slot |
| `CTRL` + mouse wheel | zoom the camera instead |
| left mouse button | break the aimed block while held; a click on the hotbar selects that slot |
| right mouse button | build the held block into the aimed cell |
| `1` to `9` | select a hotbar slot |
| `-` / `=` | zoom out / in while held (numpad `-` and `+` do the same) |
| `[` / `]` | keep fewer / more chunks around the player |
| `SHIFT` | address the layer below the feet instead of the layer the player stands in |
| `Q` | drop one item, `SHIFT` + `Q` the whole stack: the hotbar slot during play, the slot under the mouse while the inventory is open |
| mouse outside the panel | throw the carried stack into the world while a container is open; a stack put into the creative grid disappears instead |
| `E` | open and close the inventory; while the world is played in creative mode it opens the creative inventory instead |
| typing in the search box | filter the creative inventory by name, `BACKSPACE` deletes a character |
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
| `block` | block types and the id/name lookup table, ids are stable across save games |
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

The format version is written into every file. A world written by format 1, which
kept all its chunks inside the level file, is converted while it is opened: its
chunks move into chunk files and the level file is rewritten without them.

## Build and run

```bash
./gradlew lwjgl3:run         # start the game
./gradlew :core:test         # run the tests
./gradlew :lwjgl3:jar        # build the runnable jar into lwjgl3/build/libs
./gradlew build              # compile and test everything
```

The tests cover what does not need a window: chunk generation and modification
flags, the chunk streamer, the save format including the conversion of an older
world, the entity layer, and the pictures of the project. Everything that draws is
checked by running the game.

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
with every step - so no side picture is needed and none is kept. Tall grass and
leaves keep their flat picture, they do not fill a cell. `gradlew :core:test` writes
a sheet that shows the result to `core/build/reports/block-icons-preview.png`, with a
few blocks enlarged in `block-icons-detail.png`.

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
tests that need no window. Its grid is an ordinary `Inventory` of result slots, so
`ContainerMenu` shows it and hands a stack out on a click; the supply behind a slot is
endless, see `CreativeInventory#sourceAt(int)`, which is why a stack that was taken is
there again right away. The tabs are recognised by what an item *is* - a block, a
material, food, a tool, armour - so a new item lands in its group by itself.
`CreativeLayout` owns the geometry of the panel, and the drawing code and the hit
testing both read it, so a click always lands on the slot the player sees. Putting
something into a grid slot is the one action that has no place - the slot fills itself
again - so that stack is destroyed instead of finding no room, see
`ContainerMenu#setVoidsOverflow(boolean)`; a stack dragged out of the panel lands on the
ground like anywhere else. The last tab is not a list at all: it hands the panel over to
the inventory of the player, so a creative player keeps the crafting field and both
screens share the items in it.

A machine describes its slots by their role - input, fuel, output - and `MachineMenu`
turns those roles into a layout, so a new machine gets its screen without a line of
drawing code. `Machine`, `EnergyStorage` and `FluidStorage` are the contracts a machine is
built on. Nothing in the world ticks a machine or fills a tank yet, but the numbers of a
furnace, of a buffer and of a tank already work and are covered by tests.

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
