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
- **Light and the day** - every cell of the world carries two levels of light: what reaches it from
  the sky and what the torches and the furnaces around it give away. A cave is dark because the sky
  cannot reach it, a roof casts a shadow that moves as the sun does, a hole that is dug lets the
  light in again and the light of a torch that is taken away is taken back, see `LightEngine`. The
  clock of the world runs from the sunrise through the noon into the night in twenty minutes:
  `DayCycle` names the hour, brightens and dims the light of the sky, paints the colour a frame is
  cleared with and tells the sun and the moon of `SkyRenderer` where to stand. The two of them are the art
  of `assets/environment` - a glowing body on a black ground - and the pass of the sky reads a texel through
  its own brightness, which is what leaves the black ground out of the picture and keeps the sun warm on a
  blue sky. The moon walks through its eight phases, one to a day.
  The hour travels with a save game and `/time` moves it. A level of light is stored in the vertices
  of a section and not a brightness, so the sun moving across the sky costs the world no mesh at all:
  the shader scales the light of the sky once per frame.
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
  is no faster there than a hand, stone hands its item over only to a pickaxe of the level it
  asks for and a trunk to an axe. **The ladder runs hand, wood, stone, iron, diamond**: a hand
  counts as level 0, wood as 1, stone as 2, iron as 2 and diamond as 3, so a pickaxe of wood
  opens the stone and the coal of a landscape, one of stone opens the ore of iron and only one
  of diamond opens obsidian - a world can be opened with nothing but a tree and a hole, see
  `Items#WOOD_TOOL_LEVEL` and `ToolRecipesTest`. Every block a tool harvests costs it one use,
  the slot shows what is left as a bar under the icon, the tooltip names it, and a tool that is
  used up leaves the hand: wood lasts 59 blocks, stone 131, iron 250 and diamond 1561. Any item
  may declare a life and not only a tool, see `Damageable`.
- **Inventory and hotbar** - nine hotbar slots, a full inventory screen, and
  dropped items that are thrown where the player looks, fall on the ground and are
  picked up by walking over them; any kind of item is drawn, a block as the cube of
  its block and a tool as a board of its picture one pixel thick.
- **Chests** - the first container of the game: a block that holds twenty seven slots of
  what a player puts in, kept in a block entity that travels with its chunk and handed
  over when the block is broken, so nothing that was put into one is ever lost. Using a
  chest with the build button opens its screen: three rows of its own above the three
  storage rows and the hotbar of the player, drawn with the very panel, slots and clicks
  the inventory screen is drawn with, see `ContainerBlockEntity` and `ContainerGui`.
  A container works on nothing - the tick of the world passes it by - which is what tells
  it from a machine.
- **The table of the workshop** - the crafting table makes what its nine cells hold: a pattern
  laid out in them or a bag of ingredients, looked up in the very tables the two by two field
  of the player uses, so a recipe that fits the inventory fits the table as well. The field
  belongs to the block: a pattern is still lying on the table when the player comes back, and
  it travels with the chunk. The result beside it is worked out from the cells and never
  stored, so a stored table can never hand out a product its own cells do not make, and taking
  it gives up the ingredients the recipe was matched with, see `CraftingField`. Every tool of the
  game is made on it - a pickaxe of three planks over two sticks is the first one - together with
  the table itself, a chest, a furnace, a torch and a ladder, see `assets/recipes/crafting_shaped`.
- **The screen of recipes** - `R` on an item asks how it is made and `U` what it is used for: its recipes are
  shown as pages - the ingredients, the arrow, the product and the number of the page - and **a click on a page
  lays the recipe out** into the field the player has open, out of what they really carry: an ingredient nobody
  has is named in the foot of the panel and the cells of a creative player are filled for nothing. A click on a
  cell walks on to that item, the arrows of the head walk through the groups of the game - the blocks, the
  machines and one group per kind of recipe - and the box at the foot of a list searches it by the name of an
  item, the name it reads as and the chemical formula of its material, see `RecipeIndex`, `RecipeArranger` and
  `RecipeBrowserGui`. A page reports what a recipe is worth: how long a craft takes, what steam it is worth
  and, for a recipe that names it, the power it draws a tick and the voltage it asks for, see `RecipeReport`.
  The screen is drawn from the pictures of the interface and needs no art of its own.
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
  fluid, what it holds and what it takes, and, while `SHIFT` is held, the side of the block the tank is
  reached through, which the wheel walks on.
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
  mouth glows while it burns, which is the state of the cell and therefore part of the saved world. **Water
  arrives through one side of its block and the steam leaves through another**: every part of a machine is
  reached through a side of its own - the tank of water through one, the tank of steam through another - and
  the front of the machine carries nothing at all, because the player who built it is looking at that side,
  see `FaceConfig` and `MachineSides`. A line of pipes is built towards the side of the water to fill it and
  towards the side of the steam to take what the boiler boiled, see `FluidNode`.
  **Which side that is is set with the wrench**: a right click turns the machine onto the side a player
  clicked, `SHIFT` with the left button gives a side the job of taking something in - the vent of a machine
  that breathes, the plug the power arrives through - and `SHIFT` with the right button the job of giving
  something out, the plug a machine hands its power over to. The same click takes the job away again, a side
  the machine has no use for is refused, the front takes no job at all, and **giving a side a job takes the
  job that stood there away**, which is how a player moves the mouth of a pipe that was built against the
  wrong side. **The sides of a machine travel with it**: they are words a player reads at the machine - the
  right flank, the back, the ceiling - and not sides of the world, so a machine that is turned shows the pipe
  a player built against it on the same flank of its new front, and the face the machine shows can never be
  covered by one of its own parts.
  **The panel says the same thing**: the box of a tank names the side it is reached through while `SHIFT` is
  held and the wheel walks that side on, stepping over the front and over the sides another part of the very
  machine owns - the plug of the power is not taken away by a wheel that runs over a tank. A machine is built
  with the water on its right flank, the steam on its left one, the vent of a machine that breathes on the
  back and the plugs of the power on the back and on the left flank, and it keeps those sides until a player
  sets another one. A machine is the pump of that exchange: every tick it pours a tank it fills into the pipe
  that stands on the side of that tank, and a pipe pours what it holds into the side of a tank it feeds.
  A boiler is still filled and emptied by clicking its tanks with a cell as well, and a
  boiler that cannot let its steam out is a boiler that ruins itself - a wooden pipe at its hatch bursts
  with the steam of three hundred and seventy three kelvin.
- **The machines of the age of steam** - six machines that run on the steam of the boiler instead of on a
  flame of their own: the **steam furnace** and the **alloy furnace**, which smelt an ore and melt two metals
  together, the **grinder**, which turns ore into dust, the **compressor**, which presses an item into a
  plate, the **extractor**, which takes something apart, and the **forge hammer**, which beats an ingot into
  shape. Each of them holds one tank of sixteen thousand millibuckets, which a line of pipes or a cell fills,
  and each reads a group of recipes from a folder of `assets/recipes` - `smelting`, `alloy_smelting`,
  `grinding`, `compressing`, `extracting` and `forging` - where every file says what a craft takes, what it
  makes, how long it takes and **how much power it draws a tick**. **Steam is the energy of a recipe written
  in millibuckets**: four of them are worth one unit, so a machine of this age pays four times what one craft
  costs and no file names the same number twice - and the very file a furnace of bronze melts an ore with is
  the one a furnace of the high voltage melts it with, see `ProcessingRecipe`. **A machine of the age of steam
  is a machine of the ultra low voltage**, the eight units a tick of the first line, so it reads the groups of
  its age and of no later one, and a machine of the line over-clocks the recipes of this age, see below.
  **Steam is spent while a craft runs**, the way a machine of the electrical age spends its energy: one frame
  pays the share of the craft it is worth, so a machine whose tank is empty waits with its input in the slot
  until the boiler catches up.
  **A machine of the bronze age blows its steam out of one side of its block.** The vent is a side of the
  machine like every other part of it and is set with the wrench - `SHIFT` with the left button, the click
  that gives a side the job of taking something in - and the moment a craft ends the machine looks at that
  side: a solid block standing there is
  reported in the corner of its panel and the machine refuses the next recipe until the way out is open again.
  The craft that was already running is always finished, because the steam of it was paid for. A machine of
  this age is built of bronze casing like the boiler, is taken apart with the wrench, and its front glows
  while it works.
- **The steam turbines** - three machines that turn the steam of a boiler into the power of a line of cables,
  and the first machines of the game that **make** energy. A turbine is a tank of sixteen thousand millibuckets
  of steam, the same tank every machine of its age carries, a buffer of the tier it was built for and one plug
  the power leaves by: every tick it drinks the steam of its tier and fills its own buffer with the energy that
  steam is worth, and the line that stands at the side of its plug draws what it made, see
  `MachineEnergyStorage` and `MachineBlockEntity#updateEnergy`. **One energy is worth two millibuckets of steam**, and the blades of a
  turbine do not get all of it: the low turbine turns eighty five of every hundred, the middle one seventy
  five and the high one sixty six, so a better machine of this family drinks more steam for the same ampère
  and not less. Each of them hands over **one ampère of its own tier** a tick - thirty two units of the low
  voltage, a hundred and twenty eight of the middle one, five hundred and twelve of the high one - and its
  buffer holds a few seconds of that, see `TurbineTier` for the whole table. A turbine of bronze is the
  machine of the first workshop, the two others are the machines a larger one is built around.
  **Nothing is drunk that nobody wants**: a turbine whose buffer is full stands still instead of boiling the
  steam of its tank away. **A generator has no vent**: the steam it drank became the power of its line, so
  there is nothing left to blow out of a side and no wall can ever stop it - the only side of its own is the
  one its plug stands on, see `ExhaustMachine` for the machines that do breathe. **A generator is no machine that works on an item**: the
  buffer of a turbine may be emptied and never filled, so it has one plug - the one the line hangs on - and no
  side that takes power in at all, its screen holds no slot, so there is no cell a stack could be put into,
  and it shows no bar, because there is no craft to fill one towards: the steam it drinks stands on the row a
  bar stands on and what the machine holds is read on the cell of energy at the foot of its panel, which wears
  the picture of the cell of the electricity - the cell a battery is put into in the screens of the original
  game - and names the power in a box while the mouse rests on it. **A generator is read and not fed**: that cell
  is a picture of what the machine makes and no slot, so the cell of a boiler or of a turbine never takes an
  item, see `MachineMenu#hasEnergySlot`. An empty tank is no error of a generator
  either: it stands still until a line of pipes fills it, see `ProgressKind#NONE`. The tiers are read off the
  block - each of them is built of the casing of its age, while the panel of every machine that makes power is
  the grey one of the age of electricity - and a player may build a turbine before the line it feeds exists,
  because a machine that takes power later is the one that makes it worth drinking. The wheel on the front of a
  turbine is one picture of the pack, so it stands still while the machine turns; what a player reads the work
  of a turbine off is the cell of energy of its panel, which fills as the machine makes power.
  The block ids 728 to 730 were appended behind the cables, so no block of the game moved; the items took the
  numbers the materials of the game used to start at, so every item of every material stood three numbers
  higher - which is the version 16, and the sides of a machine became words a player reads at it in version
  17, see `SaveFormat`.
- **The machines of the line** - the age of electricity: six families of machine and three tiers of casing
  each, which makes **eighteen machines** that run on the power of a line of cables instead of on a flame or
  on steam - the **electric furnace**, which smelts what the furnace of coal smelts, the **macerator**, the
  **compressor**, the **extractor**, the **forge hammer** and the **alloy smelter**, each of them reading the
  very group of recipes its twin of bronze reads, see `MachineFamilies` and `ElectricMachine`.
  **A machine of the line asks the line for what it needs.** An idle machine tops its buffer up at **one
  ampere of its tier a tick**; a machine that works asks for the current its recipe draws -
  `floor(power × 2 / voltage) + 1` amperes, never below one and never above the **two** a machine of this kind
  may take. **The machine is the one that reaches for the power and never the cable**: a line carries nothing
  of its own, so the machine that works is the one that draws it, and the block entity of the machine is what
  carries the question to the line standing at its plug, see `EnergyGrid.Line#pull`.
  **The buffer of such a machine holds sixty four ticks of its tier** - two thousand and forty eight units at
  the low voltage, eight thousand one hundred and ninety two at the middle one and thirty two thousand seven
  hundred and sixty eight at the high one - and it may **only be filled**: what one call may add is the current
  of the machine and what one call may take is nothing at all. A machine of the line is therefore never a
  source of the line it stands on, and a workshop of them is a tree of power and never a ring.
  **A machine of a later tier over-clocks the recipes of an earlier one.** Every tier is four times the one
  below it, so one step of over-clock runs a recipe at four times the power for half the time, which is twice
  what the craft costs - and the step is taken as far as the current of the machine allows and no further. A
  recipe that would need more amperes than the machine may take is no recipe for it at all, and a frame that
  cannot be paid for leaves the work and the input where they are, so a machine of the line neither eats an
  input it cannot pay for nor loses one, see `ElectricMachine`.
  What a player reads at a machine of the line is the tier in front of the name of its family - `LV Macerator`,
  `HV Alloy Smelter`. Every one of them is drawn in the grey panel of the age of electricity, holds no tank,
  because the power it runs on arrives over a line and never in a bucket, and lights the front of its block
  while it works. **The side a player gave the plug of the power to is drawn as the casing of the tier of the
  machine with the plug over it** - and never as the bronze of the age of steam - so a machine of the high
  voltage reads as a machine of the high voltage from every side of it, see `MachineCasing`. **The cell of
  energy at the foot of its panel is a slot and no bar**: a player who has no line
  yet puts redstone dust into it, and every frame the machine burns one piece of it in its own buffer, so a
  workshop can be started before the line that feeds it is built - a machine that is fed by hand takes from its
  line only what the dust did not cover, see `Reagents`. **Nothing but dust goes in**, because a machine that
  swallowed whatever a player dropped on the cell of energy would be a machine filled with junk by accident, and
  the box of that cell names what the buffer holds and never the name of the piece in it, see `MachineMenu`.
  The eighteen blocks take the ids 731 to 748 and the items 847 to 864, both right behind the
  turbines, so nothing that a stored world or a stored inventory names has moved and no version is refused.
  Their art is two pictures of the pack drawn together - the casing of the tier under the overlay of the family
  - which is what `tools/verify/import_basicmachines.ps1` writes.
- **The cells of the industry** - the batteries a workshop keeps its power in: fifteen of them, one for every
  chemistry and every tier of the line, from the lead acid cell of a first workshop to the lithium cell a line
  of the high voltage is kept fed with, see `Batteries`. **A battery is an interface and not a kind of item**:
  what a machine or a battery box hands its power to one day is a stack that is also a cell, and the cell is
  asked what it holds, what may be put into it and how much of it is left, see `Battery`.
  **The charge of a battery is the wear of its stack.** A cell is an ordinary item whose life is its capacity,
  so a fresh battery holds everything it was built for, a cell that was drained to the last unit is one whose
  life is spent, the bar under its icon reads as what is left in it and the tooltip of the stack names it as
  `Charge: 250000 / 400000 EU`. **A cell that is spent is not destroyed**: a battery is never swung at a
  block, see `MiningController`, so it stands in its slot until a player fills it again or takes it out.
  **What is inside a cell is what a player reads at it.** The chemistry decides the colour its window is
  poured in, whether the charge may be put back into it at all and how long one ampère of it lasts, see
  `BatteryChemistry`: an acid and a mercury cell are spent for good, a sodium, a cadmium and a lithium one
  take a charge again, and the three tiers of a chemistry carry a charge for the same length of time - twenty
  eight seconds for an acid cell, fifty for a mercury one, seventy eight for a sodium one, a hundred and
  seventeen for a cadmium one and a hundred and fifty six for a lithium one. **A bigger cell of a chemistry is
  a cell that holds more and not one that gives more**, and **a tier is four times the one below it**: the
  cells of the low voltage hold eighteen thousand units as an acid cell, thirty two thousand as a mercury one,
  fifty thousand as a sodium one, seventy five thousand as a cadmium one and a hundred thousand as a lithium
  one, and the same five hold four and sixteen times that at the middle and the high voltage.
  **A cell is drawn from its charge.** The picture of a battery is a strip of frames, the first a full cell and
  the last a spent one, and the window of a pack of plain steel is poured once per chemistry and stacked into
  the frames of a strip by `tools/verify/import_batteries.ps1`, so the icon of a stack follows what is left in
  it, see `BatteryIcon`; `gradlew :core:test` writes `core/build/reports/battery-preview.png`, the fifteen
  cells of the table full and spent and the fill of a window at every amount of charge.
  **A machine takes no battery.** What a machine of the line runs on is the power of its line and, by hand, a
  piece of redstone dust: the cell of energy at the foot of its panel is a slot a player fills, and the dust is
  burned in the buffer of the machine - eight hundred units a piece, one piece a frame and never one that would
  not fit whole. What a charge is put back into is a battery box, which the game does not have yet, see
  `Reagents` and `MachineEnergyStorage`.
  A cell is held and not swung, so it is listed with the materials of the creative inventory. The fifteen
  cells take the item ids 865 to 879, right behind the machines of the line, so nothing that a stored world or
  a stored inventory names has moved and no version is refused, see `Items#BATTERY_FIRST_ID`.
- **Pipes** - the fluid system of the industry, four materials: wood, copper, bronze and steel. A material
  comes in the sizes the table of the industry gives it - copper, bronze and steel as a tiny, small,
  medium, large and huge tube and as a quadruple and a nonuple bundle of tubes, **wood as a small, a
  medium and a large pipe and as no bundle at all** - which is a hundred and forty five pipes of twenty five
  materials: wood, clay, copper, bronze, wrought iron, lead, steel, polyethylene, stainless steel, titanium,
  polytetrafluoroethylene, tungsten steel, polybenzimidazole, niobium titanium, tungsten, the two tantalum
  tungsten alloys, europium, depleted uranium, maraging steel of both grades, Inconel of both grades,
  Hastelloy X and Incoloy 903. Their blocks take the block ids 29 to 173 and their items the ids 100 to 244,
  the window the items of the industry were given, see `SaveFormat`. In the creative inventory the pipes have a
  tab of their own. A pipe decides one thing
  and nothing else: **which of its six sides it joins.** Those six sides are the six properties of its
  block and the number of its state is the mask of the connections, so the game knows every way a pipe can
  run without a line of code per case. **A pipe is joined where a player joined it.** The one connection
  that comes for free is the one a player asks for while building: a pipe that is put down while the eyes
  name a pipe **of the same material** is joined to that very pipe on both sides at once, so a line of one
  metal grows by itself while it is laid. Everything else waits for the wrench: a pipe of another material,
  a line that runs the other way, and a pipe that was put down without aiming at anything. The size is free
  - a line may step from a small pipe into a huge one, and which of the two limits the flow is the question
  of the transport. **Any side may be turned**, whatever it faces: a side that is opened towards nothing
  shows the open ending of the tube with the plate of its size on it, and a side that faces a wall may be
  opened as well, because the wall may be gone a moment later. A side that was joined stays joined when the
  block behind it is taken away. Nothing of the world is read and no mask heals itself, so a player gets
  exactly the lines they built: two lines that meet at a wall stay apart, and a line that runs past a
  machine does not take from it by accident. Two pipes always join, whatever their material and their size;
  a machine joins as well, because it names the tanks behind its sides - each tank through a side of its own,
  set with the wrench, and the front of the machine through none at all - which is what a line is built
  towards, see `FluidNode`. A pipe is not a wall: it is
  walked through like a torch, it is drawn from the eight grey scale pictures of the pack that every metal
  shares (its colour is painted over them, so a new metal needs no art at all), and its geometry is a thin
  tube that ends at the border of its cell in the plate of its own size. That plate is what a joined side
  shows: two tubes of one size put the very same plate against each other and the seam between them
  disappears, so a line reads as one tube, while two tubes of different sizes cover one plate with the other
  - the plate of the wider tube is the collar of a reducer, and it is the only place the art of a pipe is
  seen from its end. A bundle fills its cell and is capped by its own plate, so which picture a side of it
  carries says whether the column of tubes runs on there: the side a tube runs towards shows the plate of
  the bundle with its four or nine tubes on it, and every other side shows the plain flank, which reads as
  the plate a player capped the block with. None of the six is left out, because a side that is left out
  would be a hole in a block that fills its cell. In a slot a pipe is drawn as a straight length of itself,
  the way a hand and the ground show it, and the frame of that icon follows the shape of the pipe: a small
  pipe is a thin bar across the slot, a huge one a fat bar and a bundle the block it is, so a player can
  tell the sizes apart in the inventory, see `BlockIconRenderer`. The window the pipes are given holds their
  items from 100 to 244, and the materials of the game stand behind them, see `Pipes` and `SaveFormat`.
- **What a pipe carries** - a pipe is a tube with a little fluid of its own and no pump, and every tick it
  offers what it holds to the pipes it is joined to. **The tube of a pipe is as large as what the pipe
  moves a second** - the rate of the table of its material - and it offers a twentieth of that a tick, so a
  line moves its rate and no more. **A junction divides what it has the way the rates of the pipes behind it
  stand to each other**: where a small copper pipe of a hundred and twenty millibuckets a second and a huge
  one of sixteen hundred meet, the huge one takes the larger part and not an equal half, and the odd
  millibucket rounding leaves over goes to the last neighbour, so nothing is lost between two ticks.
  **Nothing runs uphill**: a pipe gives only to sides that stand under less pressure than it does itself -
  the share of a pipe that is filled is its pressure - so a line fills up from the end it is fed at and
  stops where the fluid stands as high as the pipe that feeds it. Without that rule two pipes would hand
  the same fluid back and forth for ever, because a pipe that was just given something is as willing to
  give as the one that gave it. A pipe that is filled faster than it can pass the fluid on holds what it
  has, so a line backs up instead of losing fluid.
- **A pipe bursts and a pipe has a valve** - a fluid hotter than the material of a pipe takes bursts it:
  the pipe is replaced by air, the fluid in it is lost with it and no item is dropped, because a tube that
  gave way is a tube a player has to build again. Wood gives way at three hundred and fifty kelvin, so the
  steam of a boiler of 373 K bursts it while it carries the water of 300 K without trouble; copper takes
  1000 K, bronze 2000 K and steel 2500 K, so lava of 1300 K runs in bronze and in steel and bursts a copper
  pipe. The rates run from 60 millibuckets a second for a tiny copper pipe to 19200 for a huge steel one,
  and a bundle moves no more than the single pipe of its cell - nine tubes through one cell are narrow and
  no bargain. Every number of the table is the table of the industry and is checked, see `PipeMaterials` and
  `PipeTableTest`. **The valve of a side is set with the wrench and the modifier key**: a click with the key
  held walks that side through both ways, in only, out only and back again, so a player may make a mouth
  that only takes, an outlet that only gives, or a loop that runs one way round, and a click without the key
  turns the join as it always did. **The wrench is held in one of two hands**: the right button turns a face - the
  join, or the valve with the modifier key held - and the left button takes the block apart, which is why a
  pipe and a machine name the wrench as the tool they are broken with: a player who holds a wrench mines
  them faster than with a bare hand and always gets the item. **The modifier key builds against a machine
  instead of opening it**: a player who aims at a boiler with a pipe in hand and the key held lays the pipe
  instead of seeing the slots of the boiler. **A machine and a line meet in the two directions of
  `FluidNode`**: the pipe pours what it holds into the side a player gave to a tank the machine is filled
  through, the machine pours what it made out of the sides of the tanks it fills, and a machine weighs one at
  a junction, because it names no rate of its own - a boiler empties its kettle at the rate of the widest line
  that stands at it.

- **The cables of the power** - the other half of the industry: a line of cables carries the energy a machine
  makes to the machines that work on it. A cable is a **material** and a **width** in one of **two
  wrappings**: the bare wire a workshop starts with and the insulated cable that wears a skin over it, which
  loses **half** of what the bare one loses, rounded down - a copper line of four units a block loses two
  once it is wrapped, and a line of one unit loses nothing at all - and is drawn from art of its own. The game
  knows forty five materials, from the red alloy of the first line to the superconductor of the last age, in
  the six widths of one, two, four, eight, twelve and sixteen amperes: five hundred and forty cables that take
  the block ids 188 to 727 and the item ids 304 to 843, which is the run of the items the materials of the
  game used to start at, see `SaveFormat`. The widths of the industry are the widths of the pipes, so a line
  of it is read the same way whatever runs through it.
  **A cable joins the sides its state says it joins**, exactly like a pipe: a cable that is placed joins
  nothing at all - it stands as a stub in its cell until a player opens a side of it with the wrench on the
  grid of its faces - and nothing heals itself, so a line a player built is the line they get. Two cables that
  touch without a joined side are two lines, and a machine the line is run past takes nothing from it by
  accident.
  **What a line carries is the worst of its cables**: a line of two materials is a line of the worse one and a
  line of two widths a line of the narrower one, so a copper cable between two superconductors carries no more
  than copper. One tick of a line is its voltage times its current - a single copper cable is a hundred and
  twenty eight units a tick and the sixteen fold bundle four times that - and a line is not slower when it is
  long, it simply costs more: every block of the run takes its loss away from what travels through it, and the
  machine that hands the energy over pays what the machine at the far end receives **plus** that loss.
  **A line that is too strong destroys the machine that reaches for it**: a machine that asks a line of a
  higher tier for power is not fed at all - the machine and every cable of that line go, which is what a player
  finds when a line of a later age is run into a workshop of an earlier one, see `EnergyNet` and
  `EnergyAcceptor`. A machine that asks for nothing never meets the line, so an idle workshop is safe, and the
  tier of a line is settled by the machine that works and never by the one that only makes power.
  **A machine of the power network holds a buffer of a tier.** What one call may add is **one ampere of that
  tier** and no more however wide the line at it is. A machine that makes power may be emptied by the line,
  because that is what it fills its buffer for, while **a machine that works may only be filled**: it spends
  out of its own buffer, so it is never a source of the line it stands on and a workshop of consumers is a tree
  of power and never a ring, see `MachineEnergyStorage`. The plugs of a machine - the side a line arrives on
  and the side it hands power out of - are sides like every other part of it, set with the wrench or with the
  wheel of the panel, and a line hangs on those sides and on no other: a cable that stands at the front of a
  machine or at the side of its tank carries nothing of it. A machine that holds no buffer has no plug at all,
  which is why the boiler and the machines of the age of steam stand along the cables of a workshop and do
  nothing with them.
  **A line is drawn by the machine that works and never pushed by the one that makes.** A cable carries
  nothing of its own, so the machine that wants the power is the one that moves it: every tick the block entity
  of a machine walks the line that stands at the plug it takes power in through and draws out of the buffers
  that may give what its own buffer may take, and the loss of the run is paid by the machine the power comes
  from, so nothing ever travels to be lost on the way, see `EnergyGrid` and
  `MachineBlockEntity#updateEnergy`. A machine that makes power therefore needs no pump of its own: it fills
  its own buffer, and every machine that works draws what it left there - which is what makes the turbines and
  the machines of the line one network and not two.
- **Fixed ticks** - the world advances in twenty steps a second no matter how fast the
  frames come, so a machine does the same work at any frame rate.
- **Faces and tools** - a block is worked on from the face it is looked at, and the faces a player
  cannot reach from there are reached through a grid of nine cells drawn on that face: the middle cell
  is the face itself, the four beside it are the faces around it, and all four corners lead behind the
  block - which is how a pipe is told to let go of the line that runs behind a block, see `FaceGrid`.
  **The cells are cut one to two to one**, so the middle cell of a row is twice as wide as the two beside
  it and the face a player looks at is the largest target of the nine. **Every cell says what its side
  does**: a side that is not joined is crossed out by the two diagonals of its cell, a side that carries
  the valve of a one way line carries the small arrow of it - out of the block or into it, the head of
  the arrow sitting at the far end of the shaft or at the face itself - and a side that runs both ways
  stays plain, see `FaceMark`. **The colour of an arrow says what that side moves**: the yellow of the
  fluid system for a pipe, the green of the power for a plug of a line of cables and the red of what a
  machine of steam spent for its vent - which wears the very same picture as a pipe, so the colour is what
  tells a player that nothing is caught on the other side of it. The grid appears while a tool is held - a
  wrench, a wire cutter, a crowbar
  or a screwdriver - or while
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
  the four sides a player cannot see from where they stand. **A machine answers the grid as well**: its front
  is crossed out, a side that takes something in carries the arrow of what it takes into the block and a side
  that gives something out the arrow out of it, in the green of the power or the yellow of a fluid, see
  `FaceMark`. All four clicks of the wrench have a meaning on a machine
  where a pipe only reads the right button: the right button turns the machine onto the side that was clicked,
  `SHIFT` with the left button gives a side the job of taking something in, `SHIFT` with the right button the
  job of giving something out, and the left button without the key is the click that mines, which is why it
  never reaches the machine at all, see `FaceClick`. The wire cutter, the
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
  `/gamemode`, `/time`), anything else is a chat message. The recent conversation stays visible
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
| mouse wheel on the panel of a machine | walk the side of the block the tank under the mouse is reached through, stepping over the front and over the sides another part of the machine owns; `SHIFT` names the sides of a tank and of the cell of energy in their boxes |
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
| `R` | ask how the item the mouse points at is made, while a container or the inventory is open |
| `U` | ask what the item the mouse points at is used for; a click on the page of a recipe lays it out into the field that is open |
| `ESC` | close the input line first, then the screen of recipes, then the inventory, then open the pause menu |
| `F11` | switch to fullscreen |

## Modules

- `core`: the game itself, shared by every platform.
- `lwjgl3`: desktop launcher.

## Packages of `core`

| Package | Contents |
| --- | --- |
| `fluid` | what a fluid is: the kinds, the temperature it carries, the colour a tank and a cell paint it in, the containers that carry it and the tank a machine offers a pipe on a side (`FluidNode`) |
| `pipe` | the pipes of the industry: the materials and the sizes each of them comes in, what one of them moves a second and how hot a fluid may be in it, the mask of the sides it joins, the valve of every side and the arithmetic that divides a junction between pipes - a side is turned with the wrench of the game, see `blockentity` and `world.interaction` |
| `cable` | the line of the power: the materials and the six widths each of them comes in, the two wrappings of a line and the loss the skin takes away, the mask of the sides a cable joins (turned with the wrench, see `blockentity`) and the tables of the blocks and items of a cable |
| `energy` | the power of the game: one line of cables and what it carries and loses a tick (`EnergyNet`), the net of a world with the machines that hang on the sides their plugs lie on (`EnergyGrid`) and the buffer of a tier a line is measured against (`EnergyAcceptor`) |
| `block` | block types, the six faces of a cube, the id/name lookup table (ids are stable across save games), and the shape of a block: `block.model` reads the models, `block.state` the states |
| `blockentity` | what a block carries beyond its id and its state: the base class, the type registry, the machine behind a block and the container that keeps items |
| `machine` | what a machine is built from: slots and tanks with a role, energy (the buffer of a tier, `MachineEnergyStorage`), the reagent a machine is fed with by hand (`Reagents`), the recipes it runs and the side of the block each of those is reached through (`FaceConfig`, `MachineSides`, `FaceClick`) |
| `recipe` | the files behind `assets/recipes`: what a recipe is, the grid it is offered, the loader that reads a file, and the work field of nine cells a table of the workshop holds |
| `material` | what a material is: the shapes it comes in, the colour and the formula it carries, and the items one line per material turns into |
| `item` | item types, stacks, the inventory, the hotbar selection, the tool an item is for a face of a block (`FaceTool`), the cell of energy an item may carry (`Battery`, `Batteries`), and the sinks broken blocks hand items to |
| `loot` | what a broken block leaves behind: the tables, the files below `assets/loot_tables` and the rule that a block without a table drops itself |
| `world` | chunks, the world, the game mode, the clock of the day (`DayCycle`), the chunk store interface and the generator |
| `world.light` | the light of the world: the sky that fills every column, the light sources that spread and fade, and what is taken away when a source is removed (`LightEngine`) |
| `world.decoration` | the trees and plants planted on a finished chunk |
| `world.interaction` | aiming, breaking and building, and the grid of nine cells a tool works a face of a block with (`FaceGrid`, `FaceOperable`) |
| `world.save` | the save format, the level file, the chunk files and the entity tags |
| `entity` | entities: the base class, the type registry, the manager, the player and dropped items |
| `chat` | the input line, the messages and the commands a line behind a slash is looked up in |
| `gui` | hotbar, inventory, creative inventory, the screen of a container and of the table of the workshop, and chat rendering, layout and widgets |
| `gui.recipe` | the screen of recipes: its groups of items, the page of a recipe and the layout of both |
| `render` | world, sky, entity, selection and font rendering |
| `screen` | the screens and the manager that switches between them |
| `input` | keyboard and mouse state |
| `util` | constants, the box a world of cubes is measured against, and the NBT layer the save format is built on |

## Where the game writes

The game writes into its working directory:

```
run/saves/<id>/level.dat              world fields, the hour of the day, entities, game rules
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

The light of a cell is written into neither file. It is what the sky and the light sources around a
cell make of it, so it is computed again the moment a chunk arrives - whether the generator made it
or the save game brought it back - and the hour of the day travels with the level file, which is what
tells the sky how bright to be. Nothing of that changes the format: a file written before the day
existed carries no hour and is read as the morning.

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
every item of every material moved twenty eight numbers up, and version 11 took four of
those numbers back: not every material is made in every size any more - wood comes as a
small, a medium and a large pipe and as no bundle - so the run of the pipes holds twenty
four of them and the pipes behind wood moved four numbers down, while the items of the
materials kept the numbers they had. The wrench took item id 88
when it arrived, a number of the window that stood free next to the pipes, which costs
no version: no stored inventory can hold it. Version 13 is the one the containers arrived in: a chest is a
block that keeps what a player puts in it, at block id 187 and item id 258, and it names the block entity
`chest` that a build without it would report and skip - a chest would open empty while the items in it were
still written down. Its item took the number the run of the materials used to start at, so every item of
every material stands one number higher than it did, and a stored inventory of version 12 names the wrong
items for every one of them. The machines of the line arrived without a version of their own: they take
block and item numbers behind the ones that were already handed out - 731 to 748 and 847 to 864 - so
nothing that a stored world or a stored inventory names has moved and no file of an older version has to
be refused. The one kind of file that changed is a recipe: it names the power a craft draws a tick and the
voltage it asks for instead of the steam it spends, and a recipe is read while the game starts and never
stored, see `ProcessingRecipe` and `MachineFamilies`.

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

## Tools

The scripts that write the art and the models of the game live in `tools/` and are part of the
repository, so a picture or a model can always be written again from what the pack holds:

```
tools/gen_pipe_models.ps1            models and blockstates of every pipe of the industry
tools/verify/extract_3d_assets.ps1   the faces, the colormaps and the sky an art pack is asked for
tools/verify/extract_creative.ps1    the panels, the tabs and the thumbs of the creative inventory
tools/verify/material_forms.ps1      one grey scale picture per shape a material comes in
tools/verify/import_basicmachines.ps1  the machines of the line: the casing of a tier under the overlay of a family
tools/verify/import_batteries.ps1    the cells: the window of a pack of steel poured once per chemistry
tools/verify/import_gregtech_assets.ps1  the casing, the fronts and the tops of the machines
tools/verify/restore_assets.ps1      fetches back whatever an editor emptied out of assets/
tools/verify/grayscale_fluid.ps1     turns a picture of the pack into a fluid window
tools/verify/smoke.ps1               starts the shipped jar and walks the menus with a real mouse
```

Only the pictures they write are read only: everything a script produces lands in `assets/` or in
`core/build/reports`, while what a smoke run captures goes into `build/verify`. Both build folders
are ignored by git, which is why the scripts themselves live in `tools/` and never in `build/`.

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
the very piece of pipe a player thinks of. **The eye stands at the corner of the north face**, which is the
side a block draws its picture on - the mouth of a furnace, the front of a machine, the door of a boiler -
so a slot shows the top, that front and the flank beside it, and the machines of an age, built of one
casing, are told apart by what stands on their front, see `BlockIconRenderer#DIRECTION`.

**The art of a machine and the gear that turns while it works.** `tools/verify/import_gregtech_assets.ps1` takes the
pictures of the machines of the industry out of the pack: the casing of their age - bronze or steel, plain and
in bricks for the two that hold a fire - and the front and the top that stand on it, one picture for a machine
at rest and one for a machine that works. **A picture that is a strip of frames moves.** The array of the world
gives every frame of such a strip a layer of its own, one below the other, and a corner of a face carries the
layer of the first frame of the run and how many frames follow it, see `MeshData#FRAMES`; the shader of the
world walks that run with the tick the world is in, one frame every four ticks, see `BlockShader#ANIMATION`.
The layer a face names therefore never shifts for the faces around it, which is what turns the gear on the top
of a running grinder, compressor or extractor - the pack draws the gear of a macerator as four frames and the
script writes the tops of the other two as four counter clockwise quarter turns of their picture - while the
casing and the mouth of the same machine stay where they are, because a still picture names one frame and no
run at all. A top that is a strip is counted from its file and never from the model that draws it, so a machine
begins to turn as soon as its art is a strip, see `BlockPictures#frameCountOf`.

**The art of a machine of the line is two pictures of the pack drawn together.** The pack draws a basic
machine as an overlay - one file per face, with a second file for the face it shows while it runs - while the
game draws a machine from one picture per face, because the icon of its item is baked out of the block: so
`tools/verify/import_basicmachines.ps1` draws the casing of the tier under the overlay of the family and
writes what comes out. Four faces and the two of them that move, three tiers and six families make the
hundred and forty-four pictures of `assets/blocks/basicmachines`, the thirty-six models and the eighteen
blockstates beside them. A face the pack draws nothing for - the top of a furnace that has nothing on it, the
floor of every one of them - is left as the bare casing, so every face of a machine is a picture like any
other and its model is a plain cube whose faces are the front, the top, the bottom and the two flanks.
**A side a player gives a job to is drawn by the block entity of the machine and not by its model**, so it
shows the casing of the tier once more - the plug of the line of cables over `machine_lv/machine_lv` on a
machine of the low voltage - and never the bronze of the age of steam: the casing of the plug is the one of
the tier of the machine, which is the casing the generator of that tier is built of as well, see
`MachineCasing` and `MachineBlockEntity#pictureOn`.

**The models of the pipes are written by a script and checked by a test.** A pipe is drawn from the state
of its cell - the mask of the six sides it joins, see `pipe` - and sixty four masks in seven sizes are more
files than a hand writes, so `tools/gen_pipe_models.ps1` writes them: one model per family, size and
canonical mask (a turn of a pipe is the same pipe, so a size needs twenty four models and not sixty four),
one blockstate per pipe block naming the model and the quarter turn of every one of its states, and one
short model per pipe block that points at a straight length of it, which is the shape a pipe is drawn from
where a block is shown as itself. A family is written in every size of the table - the art of a size is the
art of every material of that family - while a material is written in the sizes the table of the materials
gives it: wood, which comes as three tubes and as no bundle, has a blockstate for each of those three and
the four it is not made in are removed by the run, so the files of the project name the pipes the game
really holds. The script reads the art of the pack - the grey scale `pipeSide` and one
plate per size, the same eight files for every metal, see `PipeTexture` - and writes nothing a hand has to
keep in step, because `PipeModelTest` opens every file again and checks the boxes against the very mask
`Pipes` computes, including that no box of a pipe is wider than the tube of its size - a collar that ran
around every block boundary is gone, a joined side shows the end face of the arm and nothing else - and
that no side of a pipe is ever left out: a bundle that fills its cell carries a face on all six of its
sides (the plate of the bundle where the line goes on, the plain flank where its tubes end) and a pipe that
is joined on every side keeps its core as the joint the arms meet in, because a side that is left out is a
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
the item of a shape is drawn from. `tools/verify/material_forms.ps1` copies them out of the art pack
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
`tools/verify/extract_3d_assets.ps1` fetches the missing ones home - the faces of the blocks that are
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
`tools/verify/restore_assets.ps1`, which reads the list of what was removed on purpose from
`tools/verify/removed_assets.txt` and fetches everything else back out of the last commit.
`BlockFace` names the six faces of a cube - with the way each
one lies in space and the light it catches - and `FaceSet` gives every one of them a picture, one
fallback at a time, so a block of six pictures stays three lines.

`gui/inventory_icons.png` holds every panel of the interface. Since the creative
inventory arrived it also carries that screen's art - the two panels, the tab in its two
states and the two thumbs of the scroll bar - which `tools/verify/extract_creative.ps1`
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

The screen of a machine comes from `gui/machine_icons.png`, which carries one panel per age of the industry:
the light grey one at the top, which every machine of the electrical age is drawn with, and the bronze one
right below it for the machines that run on steam, see `MachineStyle`. Either panel is empty in its upper
half, where a machine stands the slots it works with, and carries the slots of the player inventory in its
lower half. Next to the panels the sheet holds a grid of icons - one per kind of item slot, the slots of the
bronze age in a column of their own, a bright and an empty pair of arrows per kind of process, the error
pictures of both ages, one icon per kind of tank, and one heavy track beyond the grid for the tall bar of a
forge hammer - and a machine names the cells it wants through `SlotKind` and `ProgressKind`. A slot a machine
leaves plain is drawn with the slot of its own panel, which is what turns the same screen grey or bronze.

What a machine shows is registered with the machine, see `MachineScreen`: the title of the
upper left corner, the panel of the age it belongs to, the pair of arrows its progress bar is
drawn from, the kind of every slot it works with and the tanks it holds. The layout turns that
into a screen of its own, see `MachineMenu`: a block of one, two, four or six slots takes the
shape its size asks for - one slot alone, two above each other, a square of four and two rows
of three - the inputs to the left of the progress bar and the products to its right, the tanks
at the foot of the panel and the slots of the player inventory on the rows its own panel
carries. The column at the
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
paints them, text included and drawn with the bitmap the game uses, and
`machine-generator-preview.png` for a machine that makes power, whose cell of energy wears the
icon of a battery instead of the plain cell of a slot.

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
