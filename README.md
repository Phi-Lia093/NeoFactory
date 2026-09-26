# NeoFactory

A voxel sandbox game in the spirit of Minecraft, built with
[libGDX](https://libgdx.com/).

The player stands in a world of cubes: a chunk is a column of sections, a block is one unit on
every side, and the view is from inside the body. The eyes pick the cell a break or a build
applies to, so a wall is built against from the side and a bridge grows sideways. The world is
stored on disk so that a session can be continued later.

## What is in the game today

- **Terrain** - noise based biomes (plains, forest, desert, snow and a rocky one)
  with grass, trees and ore clusters, all generated from the
  world seed.
- **View from inside the body** - the world is seen through the eyes of the player: the pointer turns the
  view, `W`, `A`, `S`, `D` walk and `SPACE` jumps. The arm of the body is drawn in the lower right of the
  picture, it sways with every step and it is thrown forward when a block is broken or built, and the
  block the player holds sits in its hand. `CTRL` with the wheel, and `-` and `=` while they are held,
  zoom the view - which is the field of view of the eye, so the arm stays where it is.
- **Digging and building** - the left button breaks the block the eyes meet while it
  is held, the right button builds the held block into the cell behind the face the
  line of sight entered, and both are limited by the reach of the player. A block
  hangs on whatever it is built against, so a wall is built against and a bridge
  grows sideways. A break takes as long as the hardness of the block says, a tool
  that fits the block is faster than a bare hand, and a tool that is too weak still
  breaks the block but leaves nothing behind; the cracks of the break are drawn over
  the block while it runs. What a block leaves behind is its loot table - and a block
  that names no table leaves itself. Creative mode breaks every block at once and
  hands nothing over, see `assets/loot_tables/blocks`.
- **Tools and wear** - a tool names the kind of work it is good for and the level of its
  material, which is exactly what a block asks for: a pickaxe is quick on stone and an axe
  is no faster there than a hand, stone hands its item over only to a pickaxe of level 1 and
  a trunk to an axe. Every block a tool harvests costs it one use, the slot shows what is
  left as a bar under the icon, the tooltip names it, and a tool that is used up leaves the
  hand. Iron lasts 250 blocks and diamond 1561. Any item may declare a life and not only a
  tool, see `Damageable`.
- **Inventory and hotbar** - nine hotbar slots, a full inventory screen, and
  dropped items that are thrown where the player looks, fall on the ground and are
  picked up by walking over them; any kind of item is drawn, a block as the cube of
  its block and a tool as a board of its picture one pixel thick.
- **Save games** - one folder per world, a level file plus one file per chunk the
  player changed, written while playing and when leaving.
- **Chunk streaming** - the terrain around the player is kept in memory and the
  terrain behind is dropped, which keeps memory use flat no matter how far the
  player walks.
- **Entities** - a framework the player and dropped items are built on, with one
  type registry and one save layout they share.
- **World** - a seeded generator builds the terrain from noise fields: regions of grass, sand,
  rock and snow, ore clusters in the stone, and the valleys a river and a lake carved. A river
  follows the contour line of a field of its own, so it winds and is as wide as the slope of the
  field allows; a lake is a lobe of a fractal field and never a circle. The bed of such a valley is
  gravel, clay and sand, and nothing is poured over it: a fluid is a material of the industry and
  never a block of the world. A world is created as one of two lands, see `WorldType` - the
  landscape of its seed, or the flat table of bedrock, soil and grass that every cell of its
  surface shares, which is what a world to build and to try something out in is made of.
- **Machines** - the furnace, the first machine of the game: it is built from an item,
  keeps its slots, its fuel and its work in a block entity that travels with its chunk,
  runs in fixed ticks as long as its chunk is loaded, and hands what it holds back to
  the player when it is broken. Using it opens its screen: the slots it works with above,
  the inventory of the player below, the progress of the craft between them and a line
  that writes how much fuel is left. The contract behind it - slots and tanks with a
  role, energy, fluids and the recipe types a machine reads - is what the larger machines
  will be built from, and a test builds a reactor of that shape out of the same pieces.
  A machine is filled with fluid by hand through its tanks and not through a slot: a player clicks a tank
  with a cell on the mouse, which pours a full cell into a tank the machine takes fluid from or fills an
  empty cell from a tank it makes fluid in, see `CellTransfer`. Nothing is ever lost - a tank is only
  emptied when it holds a whole cell's worth and a cell is only poured into a tank that is empty of
  anything else or holds the very fluid and still has room for all of it - and hovering a tank names the
  fluid, what it holds and what it takes.
  A machine swallows what a recipe needs the moment the work starts and not when it ends, so a craft
  that is interrupted - the block broken, the buffer empty, a flame gone for good - costs the portion
  that was being worked on and hands nothing back. A machine that cannot pay for a frame takes nothing
  at all, which is what keeps a machine without power from eating a stack one piece at a time, and the
  recipe of a craft that runs travels with the state, so a furnace that is saved and opened again
  carries on with the work it had.
- **The bronze boiler** - the second machine and the first one of the industry: a fire under a tank of
  water. It holds one slot each way - the fuel it burns and the slot its ash will land in once the game has
  ash - and its two tanks take no slot at all: the tank of water it boils and the tank of steam it makes,
  drawn at the foot of the panel with the bar above them, are filled and emptied by clicking them with a
  cell on the mouse.
  **A boiler is read by its temperature**: it starts at the standard temperature of the game, 298 K or 25
  degrees Celsius, a flame adds ten kelvin a second while no flame takes five away, and water starts to
  boil at 373 K or 100 degrees Celsius - the number the corner of the panel writes and the one the bar
  fills towards while a piece of fuel burns. Boiling water carries the heat away, so a boiler that boils
  holds the temperature of boiling water and turns twenty units of water into three hundred and twenty
  units of steam a second whatever it burns: the fuel decides how long a boiler stays hot, not how fast it
  boils. A boiler that cannot boil - because its tank of water ran dry, or because its tank of steam is
  full and has nowhere to put more - keeps climbing, and at 473 K or 200 degrees Celsius it is ruined: the
  block goes and the entity that holds its slots and tanks goes with it, so neither what was in it nor the
  boiler itself is handed back. A boiler that boils dry while it is hot is marked as well, and water that
  reaches it while it is still that hot cracks the metal: filling a red hot boiler by hand is as dangerous
  as leaving it alone, while one that had time to cool down below the boiling point takes water again. Its
  mouth glows while it burns, which is the state of the cell and therefore part of the saved world. Water
  is meant to arrive through a pipe and the steam to leave the same way: a machine will take fluid on the
  one face its art shows - the mouth or the hatch of its front - and give it on every other face. Until
  those pipes are in the game a boiler is filled and emptied by clicking its tanks with a cell, and a
  boiler that cannot let its steam out is a boiler that ruins itself.
- **Pipes** - the fluid system of the industry, four materials in seven sizes: wood, copper, bronze and
  steel, each as a tiny, small, medium, large and huge tube and as a quadruple and a nonuple bundle of
  tubes, which is twenty eight pipes. A pipe decides one thing and nothing else: **which of its six sides
  it joins.** Those six sides are the six properties of its block and the number of its state is the mask
  of the connections, so the game knows every way a pipe can run without a line of code per case. **A pipe
  joins nothing by itself**: it is placed with its six sides closed and stays that way, and the wrench of
  the game opens a side and closes it again, see the tools below. **Any side may be turned**, whatever it
  faces: a side that is opened towards nothing shows the open ending of the tube with the plate of its size
  on it, and a side that faces a wall may be opened as well, because the wall may be gone a moment later.
  A side that was joined stays joined when the block behind it is taken away.
  Nothing of the world is read and no mask heals itself, so a
  player gets exactly the lines they built: two lines that meet at a wall stay apart, and a line that runs
  past a machine does not take from it by accident. Two pipes always join, whatever their material and
  their size; a machine will join on the faces that carry a tank of fluid as well - the mouth of its front
  takes fluid in and every other face gives it - which arrives with the transport of fluids. A pipe is not
  a wall: it is walked through like a torch, it is drawn from the eight grey scale pictures of the pack
  that every metal shares (its colour is painted over them, so a new metal needs no art at all), and its
  geometry is a thin tube that ends at the border of its cell in the plate of its own size. That plate is
  what a joined side shows: two tubes of one size put the very same plate against each other and the seam
  between them disappears, so a line reads as one tube, while two tubes of different sizes cover one plate
  with the other - the plate of the wider tube is the collar of a reducer, and it is the only place the art
  of a pipe is seen from its end. A bundle is the other way round: it fills its cell and is read by its
  cross section from wherever a player looks at it, so all six of its sides carry the plate of the bundle -
  the picture with four or nine tubes on it - and none of them is left out, because a side that is left out
  would be a hole in a block that fills its cell. In a slot a pipe is drawn as a straight length of itself,
  the way a hand and the ground show it, and the frame of that icon follows the shape of the pipe: a tiny
  pipe is a thin bar across the slot, a huge one a fat bar and a bundle the block it is, so a player can
  tell the sizes apart in the inventory, see `BlockIconRenderer`. Its twenty eight
  blocks take the block ids 29 to 56 and the item ids 100 to 127, so bronze and steel joined the materials
  as well and every item of every material stands twenty eight numbers higher than before: `DATA_VERSION`
  is 10 and a world of version 9 is refused. The heat a material takes and what it moves are placeholders
  for now - a larger pipe carries more than a smaller one and a better metal takes more heat - and the
  transport, the flow rates, the temperature a pipe bursts at and the fluids it carries are the next step.
  In the creative inventory the pipes have a tab of their own.
- **Fixed ticks** - the world advances in twenty steps a second no matter how fast the
  frames come, so a machine does the same work at any frame rate.
- **Faces and tools** - a block is worked on from the face it is looked at, and the faces a player
  cannot reach from there are reached through a grid of nine cells drawn on that face: the middle cell
  is the face itself, the four beside it are the faces around it, and all four corners lead behind the
  block - which is how a pipe is told to let go of the line that runs behind a block, see `FaceGrid`.
  The grid appears while a tool is held - a wrench, a wire cutter, a crowbar or a screwdriver - or while
  a crouching player holds nothing at all, and a click on a cell does what the tool is good for on the
  face that cell stands for, see `FaceOperable`. **A grid changes nothing about the shape of a block**: it
  is an overlay a player works through, so a pipe is walked through with a wrench in hand exactly as it is
  without one. A grid used to fill the cell of the block it was opened on, which turned the block a player
  stands in into a wall - and a pipe of the large sizes is a cell of its own, so a body inside one could
  not leave it again. **The wrench is the first tool of the
  game** - `Items.WRENCH`, item id 88, listed with the tools, a picture of its own and the kind
  `ToolType.WRENCH`, so it opens no block and is held at a face - and the pipe is the first block that
  answers to a grid: a click on a cell turns the side of the pipe that the cell stands for - either button
  works, and holding the left one does not start to break the block - so the four corners of the grid reach
  the four sides a player cannot see from where they stand. The wire cutter, the
  crowbar and the screwdriver are items the art of the machine mod brings and will follow.
- **Shapes and states** - a block is not one picture but a shape, and the skin of its faces lives in
  `assets/models/block`: a model names the picture of every face of every box, a template is written
  once and inherited, and a second layer is drawn over a face where the colour of a biome belongs
  (the side of the grass is its own picture with the layer of the green above it). What a state of a
  block shows is a file of `assets/blockstates` as well: a furnace looks in one of four directions
  and its model is turned by a quarter turn per direction, a slab fills the lower or the upper half
  of its cell, a ladder hangs on a side of a block and is climbed, a plant is two crossed planes, and both
  the shapes and the states are read once while the game starts. A block without a model file is the
  whole cube of its picture, so an older world still draws every block it holds, and a file that
  cannot be read is reported and skipped like a recipe.
- **The shape of a block is what a body runs into** - the boxes of the model of a state are the
  collision of its cell, so a slab is half a block high for the player as well: a body walks into the
  half a slab fills, steps onto its top, and is stopped by the upper half of one that hangs on a
  ceiling. A block that is not solid - a plant, a torch, a ladder - holds nothing back whatever shape
  it is drawn with. Two boxes that only touch do not overlap, which is what lets a body rest on the
  very top of what it stands on.
- **The player is a body of boxes** - the head, the body, two arms and two legs the skin of
  `assets/entity/steve.png` draws, cut per face and hung on joints: an arm swings from the shoulder,
  a leg from the hip, a hit throws the right arm forward and the head follows the view. A view from
  inside the body shows the arm of that very body with the item it holds in its hand; `F5` steps back
  and shows the whole figure, which is where the walk of the limbs is visible.
- **Chat and commands** - one input line at the lower left for messages and for
  commands: a line behind a slash is a command (`/help`, `/give`, `/tp`, `/seed`,
  `/gamemode`), anything else is a chat message. The recent conversation stays visible
  for a few seconds and comes back while something is typed.
- **Creative mode** - `/gamemode creative` hands out every item of the game: the
  inventory key then opens a grid of everything the game owns, with a tab for each
  group, a scroll bar and a search box, and a stack that was taken is there again right
  away. A slot of the grid shows one piece and carries no amount, because the shelf of the
  game is not a chest: the left button takes one piece, the right button a whole stack, and
  a click with shift held hands over one group of the item and no more - the supply never
  runs out, so a whole session there would be a backpack full of it. A creative player
  builds for nothing as well: a block is placed the very way it is in survival and the
  stack in the hand is not touched, so a line of a hundred blocks may be laid with one
  piece, see `BlockPlacer#place` and `GameModeMining` for the same rule while a block is
  broken. Every tab carries the icon of its group, an empty
  search box lists everything
  and the scroll bar tells a list that fits into the grid from one that goes on. The tabs
  frame the panel - the first row above it and the second one below it, painted with the
  same art turned by half a circle and mirrored back left to right - and every tab shows
  the very same panel, so choosing one never moves or resizes the screen. The last tab
  swaps the grid for the slots of the player, so what the player owns is at hand while the
  other tabs hand out what the game holds. What is put into the grid is thrown away, and a
  stack dragged out of it lands on the ground. `/gamemode survival` goes back, and the mode
  is stored with the world.

- **Fluids** - a fluid is a material of the industry and no longer a block of the world: water, lava and
  the steam of the first machines are held in the tank of a machine and carried by a cell, and a recipe
  asks for them by amount. None of them is ever met in the terrain, so nothing spreads, nothing floods
  and no picture of a fluid is drawn into the world. **What lies in a tank is continuous and what travels
  in a cell is not**: a tank holds whatever amount a machine boiled or a recipe drained, while a cell
  carries whole thousands of it - a thousand millibuckets is one cell - so a hand that meets a tank moves a
  whole cell's worth of fluid or moves nothing at all, see `CellTransfer`. The game has one container: a
  cell carries any fluid
  the industry brings, and a filled cell shows its fluid in the window
  while the steel around it stays grey. The buckets are gone with the fluids that used to stand in the
  world as a block, so the item ids 67, 68 and 69 name nothing and the cells kept theirs - 70, 71 and
  72 are the cell, the water cell and the lava cell, and the steam of the industry took the first free
  number above them, which is why a save game of an older version is refused, see `SaveFormat`. An empty
  cell stacks like any other material, a full one does not: filling one out of a stack of empty cells
  leaves the rest of the stack in the hand and puts the full cell into the inventory, where it takes a
  slot of its own.

- **Materials** - a factory holds hundreds of them, and a metal is not one item but nineteen: a
  dust, a small and a tiny pile of it, an ingot, a nugget, a plate, a foil, a rod, a long rod, a
  bolt, a screw, a ring, a round, a fine wire, a spring, a small spring, a gear, a small gear and a
  rotor. Writing an item and a picture for each of them would be hundreds of lines per material, so
  the game keeps the two apart: `Material` says what a material is - a name, a colour, a chemical
  formula and the shapes it comes in - `MaterialForm` says what a shape is - the picture, the name
  its item carries and how many of them fit in a slot - and `Materials` builds every item of every
  material out of those two. Twelve metals bring two hundred and twenty eight items with them today - the
  last two are bronze and steel, the metals of the bronze age the boiler and the pipes are built of - and
  a metal that arrives later is one line in that file. An item is named after material and shape, so a
  plate of iron is `iron_plate` and reads as "Iron Plate" over "Fe" in its tooltip: the name first,
  the formula of the material under it, and the search box of the creative inventory finds an item
  by that formula as well. The picture of a shape holds brightness only and the colour of the
  material is multiplied over it while the item is drawn - the very trick the cells of the fluids use - so no
  material needs art of its own. The iron ingot therefore keeps the id it always had in
  `Items.IRON_INGOT_ID` although its definition now lives with the material.
## Controls

| Input | Action |
| --- | --- |
| `W`, `A`, `S`, `D` | walk |
| mouse | turn the view, the block the eyes meet is what an action applies to |
| mouse wheel | select a hotbar slot, `CTRL` with it zooms the view instead; a notch rolled forwards walks towards the first slot |
| `-` / `=` | zoom out / in while held (numpad `-` and `+` do the same) |
| left mouse button | break the block the eyes meet while held; a click on the hotbar selects that slot |
| `1` to `9` | select a hotbar slot |
| `Q` | throw the held item where the view points, `SHIFT` with it throws the whole stack |
| `[` / `]` | keep fewer / more chunks around the player |
| `F5` | switch between the view from inside the body and the view of it from behind |
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
| `fluid` | what a fluid is: the kinds, the colour a tank and a cell paint it in, and the containers that carry it |
| `pipe` | the pipes of the industry: the materials and sizes a pipe comes in, the mask of the sides it joins, and the table of the twenty eight blocks - a side is turned with the wrench of the game, see `blockentity` and `world.interaction` |
| `block` | block types, the six faces of a cube, the id/name lookup table (ids are stable across save games), and the shape of a block: `block.model` reads the models, `block.state` the states |
| `blockentity` | what a block carries beyond its id and its state: the base class, the type registry and the machine behind a block |
| `machine` | what a machine is built from: slots and tanks with a role, energy and the recipes it runs |
| `material` | what a material is: the shapes it comes in, the colour and the formula it carries, and the items one line per material turns into |
| `item` | item types, stacks, the inventory, the hotbar selection, the tool an item is for a face of a block (`FaceTool`), and the sinks broken blocks hand items to |
| `loot` | what a broken block leaves behind: the tables, the files below `assets/loot_tables` and the rule that a block without a table drops itself |
| `world` | chunks, the world, the game mode, the chunk store interface and the generator |
| `world.decoration` | the trees and plants planted on a finished chunk |
| `world.interaction` | aiming, breaking and building, and the grid of nine cells a tool works a face of a block with (`FaceGrid`, `FaceOperable`) |
| `world.save` | the save format, the level file, the chunk files and the entity tags |
| `entity` | entities: the base class, the type registry, the manager, the player and dropped items |
| `chat` | the input line, the messages and the commands a line behind a slash is looked up in |
| `gui` | hotbar, inventory, creative inventory and chat rendering, layout and widgets |
| `render` | world, entity, selection and font rendering |
| `screen` | the screens and the manager that switches between them |
| `input` | keyboard and mouse state |
| `util` | constants, the box a world of cubes is measured against, and the NBT layer the save format is built on |

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

A stored chunk is a column of sections, and it holds the cell of every section that
carries something: the block id of a cell and its state travel as full 32 bit numbers,
so the game never runs out of room for the blocks a factory is built from - an ore, a
machine, a pipe and a cable are each just another id. A section that holds nothing but
air is not written at all, which is what keeps the sky above a landscape out of the
file, and the array of a section is stored as a palette plus one packed index per cell,
so a piece of terrain that repeats six blocks spends a few bits per block instead of
four bytes. The state is what a block carries beyond its id - the direction a machine
faces, the shape a pipe is drawn with - and it travels with the chunk, while a cell
whose block changes loses the state it had. Writing a state is a change like writing a
block is: the section is meshed again, because the state is what the shape of the cell is
read from, and the chunk is marked for the save game, or a turned pipe would be drawn and
stored as the pipe it was before the wrench.

What a block carries beyond that - the slots, the buffer and the tanks of a machine - is
stored with the chunk as well, as a list of block entities next to the sections. An
entry whose type the game no longer knows, or one whose block is gone, is reported and
skipped, so a chunk always opens.

The format version travels with every file and is checked while reading. The game is
still being built, so nothing is converted between versions: a world written by another
version is refused with a clear reason instead of being read halfway. Version 2 is the
one that turned the two flat layers of a chunk into the column of sections a world of
cubes needs, so a world of version 1 is refused instead of being read into a shape it
never had, version 3 names the spawn and the player of the level file by X and Z, and
version 10 gave the pipes the item ids 100 to 127 and bronze and steel their items, so
every item of every material moved twenty eight numbers up. The wrench took item id 88
when it arrived, a number of the window that stood free next to the pipes, which costs
no version: no stored inventory can hold it.

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

The world is seen from every side, so a block is drawn from a model: `assets/models/block` names the
picture of every face of every box of a block, `assets/blockstates` says which model a state shows and
how it is turned, and `gradlew :core:test` writes the shape every block is drawn with to
`core/build/reports/model-audit.txt`. The same test fails when a block names a picture that is not
there, and the audit of the art - `core/build/reports/texture-audit.txt` - fails when a block or an
item points at a file nobody has, and lists the spare pictures: the art nothing references yet and
that is kept for the systems to come.

In a slot a block item does not show a tile as a flat square: the game folds it into a small cube at
run time, see `BlockIconFactory`. The two side faces the view meets are derived from the tile itself
- its edge pulled into the depth and darkened with every step - so no side picture is needed and none
is kept. The cube is folded at four times the cell that shows it and drawn with a smooth filter, so
the steps along its slanted edges are four pixels wide instead of sixteen and the graphics card turns
them into soft edges; `BlockIconFactory#downscale` is the same arithmetic for a preview that cannot
lean on the graphics card. Tall grass and leaves keep their flat picture, they do not fill a cell.

**The icon of a block is framed around the cell the block stands in.** `BlockIconRenderer` draws the block
itself off screen, and the frame it is drawn in is the cell: a slot says how large a thing is by the room
the ring around it is, so a tiny pipe is a thin tube in the middle of a slot and a block fills its slot
the way it fills its cell. What a pipe is drawn *as* is the state `Block#itemState` names - a straight
length of itself with its arms, not the bare stub of state zero - so a slot, a hand and the ground show
the very piece of pipe a player thinks of.

**The models of the pipes are written by a script and checked by a test.** A pipe is drawn from the state
of its cell - the mask of the six sides it joins, see `pipe` - and sixty four masks in seven sizes are more
files than a hand writes, so `tools/gen_pipe_models.ps1` writes them: one model per family, size and
canonical mask (a turn of a pipe is the same pipe, so a size needs twenty four models and not sixty four),
one blockstate per pipe block naming the model and the quarter turn of every one of its states, and one
short model per pipe block that points at a straight length of it, which is the shape a pipe is drawn from
where a block is shown as itself. The script reads the art of the pack - the grey scale `pipeSide` and one
plate per size, the same eight files for every metal, see `PipeTexture` - and writes nothing a hand has to
keep in step, because `PipeModelTest` opens every file again and checks the boxes against the very mask
`Pipes` computes, including that no box of a pipe is wider than the tube of its size - a collar that ran
around every block boundary is gone, a joined side shows the end face of the arm and nothing else - and
that no side of a pipe is ever left out: a bundle that fills its cell carries a face on all six of its
sides (the flank where the line goes on, the plate of the bundle where its tubes end) and a pipe that is
joined on every side keeps its core as the joint the arms meet in, because a side that is left out is a
hole a player looks straight through.
`PipePreviewTest` paints every size and five ways of being joined into
`core/build/reports/pipe-preview.png` and the places where two tubes meet into
`core/build/reports/pipe-junction.png` - two of one size, which must show no seam at all, and pairs of
different sizes, which show the plate of the wider tube - so the thickness of a tube and the collar of a
reducer are judged without starting the game. Run the script after a change of the table:

```bash
powershell -File tools/gen_pipe_models.ps1
```

A dropped item is a small body of its own: the cube of the block it stands for, or - when it places no
block, a tool or a material - one upright board of its own picture, see `ItemCubeMeshes`. The board is one
pixel of that picture thick, which is what keeps it from turning into a line seen from the side and into
nothing at all seen from behind, and the four rims that thickness brings with it show the row or the column
of the picture that meets them. The picture stands upright on both large faces, and the face behind shows it
mirrored, the way it comes through the board: that is what makes a full turn of a turning item read as a
full turn instead of a quarter of one that keeps looking the same.
`gradlew :core:test` writes a sheet that shows the result to
`core/build/reports/block-icons-preview.png`, with a few blocks enlarged in `block-icons-detail.png`.

The body of a player is cut out of the skin `assets/entity/steve.png`: `SkinRegions` holds the region
of the skin each face of each bone is drawn from - the arithmetic is the one the original game
unwraps a box with, so a face cannot end up on the wrong side of a head - and `HumanoidModel` hangs
the boxes on their joints. `Bone` meshes a box with the window of its own face, `HumanoidPose` turns
the bones for a step, a hit and a look, and `HumanoidRenderer` draws the whole figure where the body
stands or, in the frame of the eye, the block the hand holds. **A view draws neither a bare arm nor a
tool**: a block is held as its cube, and a hand that holds anything else shows nothing, because a thing of
one picture belongs to the interface, which draws it as its icon. A skin that is packed differently fails
the test that reads those regions back out of the file, see `HumanoidModelTest`.

Water and lava are the fluids of the game, and neither has a picture of its own any more: a fluid is
a name and a colour, see `Fluids`. The colour is what a tank of a machine and the window of a cell
are painted with, which is why the game needs no sheet of water and no sheet of lava. The cell of a
fluid is one grey scale picture instead, and
only its window takes the colour of the fluid: `CellIconFactory` paints the two pixels wide and
ten pixels tall window in the middle of the cell and leaves the steel of the container grey, so a
cell of water and a cell of oil are the same object with something else inside. A new fluid needs
a colour, a line in `Fluids` and an entry in `FluidCells` and nothing else. (`gradlew :core:test` drew
the frames of both fluids next to the cells into `core/build/reports/fluid-preview.png`
while the sheet existed; the preview of the fluids is gone with it.)

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
shape, which is how the colours of twelve metals are reviewed without starting the game.

The flat engine only ever needed the view from above, so the side, the bottom and the front of every
block had been deleted: `assets/blocks` held 271 pictures of the art pack instead of 356, and a test
even failed the build when one of them came back. A world of cubes shows six faces, so
`build/verify/extract_3d_assets.ps1` fetches the missing ones home - the faces of the blocks that are
more than one picture, and the metadata of the sheets whose animation is written outside of them -
together with the three sets a rounder world needs: `colormap` holds the two pictures the grass and
the leaves are painted through, `environment` the sun, the moon and its phases, the clouds, the rain
and the snow, and `misc` the underwater filter, the vignette and the shadow an entity drops. The list
of faces lives in `MultiFaceTextures` and is watched by the audit, so a face that is renamed, lost
while the pack is replaced or mistyped in the script is a failed build and never a hole in the world.
**The art of what this game does not use is not shipped**: the pack brought 356 pictures of blocks and
233 of items, the game holds about twenty blocks and a few dozen items, and everything else was let go
- `assets/blocks` keeps the 103 pictures of the blocks the game knows and the faces they are drawn
from. Every picture that was let go is one call of git away, and the script above fetches it from the
pack again. An editor that empties a file of the art - which happens, the resource root of an
IntelliJ project is not a safe place for a picture - is repaired by
`build/verify/restore_assets.ps1`, which reads the list of what was removed on purpose from
`build/verify/removed_assets.txt` and fetches everything else back out of the last commit.
`BlockFace` names the six faces of a cube - with the way each
one lies in space and the light it catches - and `FaceSet` gives every one of them a picture, one
fallback at a time, so a block of six pictures stays three lines.

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
a material, food, a tool - so a new item lands in its group by itself; a machine
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

What a broken block leaves behind is read from `assets/loot_tables/blocks/<block>.json`, one
file per block that needs one: the file is named after the block, so the folder is the whole
link between a block and its drops, and a block that names no file hands over the item of its
own block. A line names an item, how many of it - one number or a range - and how often it
happens.

```
loot_tables/blocks/stone.json      { "drops": [ { "item": "cobblestone" } ] }
loot_tables/blocks/clay.json       { "drops": [ { "item": "clay_ball", "count": 4 } ] }
loot_tables/blocks/gravel.json     { "drops": [ { "item": "gravel" }, { "item": "flint", "chance": 0.1 } ] }
loot_tables/blocks/glass.json      { "drops": [] }
```

A table with an empty list is not the same as no table at all: glass breaks into nothing,
while a block without a file hands itself over. Whether anything is handed over is decided by
the held tool - a tool that is too weak for a block still breaks it, it only leaves nothing
behind - and creative mode breaks every block at once and hands nothing over.

A tool says two things about itself: the kind of work it is good for, see `ToolType` - a pickaxe,
an axe, a shovel, a hoe, a sword - and the level of its material. A block names the kind it is
worked with as well, so a pickaxe mines stone with its own speed while an axe mines it no faster
than a bare hand, and the same pair decides the other way round what happens under a trunk. A
block that asks for a mining level hands its item over only to the kind it names and only while
the tool reaches that level: stone wants a pickaxe of level 1, so an iron axe takes it apart
slowly and loses the item, and an iron pickaxe is what the item comes back with. What a tool
takes with every block it harvests is its life, and the item declares it - `250` for iron, `1561`
for diamond - while the damage belongs to the stack that dug, see `Damageable` and
`ItemStack#applyDamage(int)`. Nothing about that belongs to a tool: a mortar of a chemist, a
screwdriver of a workshop or a part inside a machine declares its own life the same way and shows
it in the same bar. The slot draws what is left as a bar under the icon, green while the piece is
nearly new and red at its end, the tooltip names it under the name of the item, a copy and a
stored inventory keep it, and a piece whose life is gone is reported to an `ItemWear` sink, which
is what takes it out of the hand. A block that was not harvested costs the tool nothing at all,
so a wrong tool is not worn away by stone. The eight pieces of armour the pack brought are gone
for now: they were icons that no slot held and that no defence stood behind, and their item
numbers stay free, see `Items#ARMOUR_ID_FROM`.

Note that most tasks that are not specific to a single project can be run with a
`name:` prefix, where the `name` is the id of the project, for example `core:test`.
