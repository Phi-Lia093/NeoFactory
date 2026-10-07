# 怎么给 NeoFactory 加东西

> 这份文件是「加新东西」的说明书：**流程 + 格式 + 验收**。想懂内核语义（SMILES、守恒、箭头）看
> [`chemistry.md`](chemistry.md)；想懂代码在哪看 [`architecture.md`](architecture.md)。

## 0. 先记住三件事

1. **整个游戏由几张表驱动。** 想加东西，九成是在某张表的**末尾**加一行：
   `Substances`（物质）、`Fluids`（流体）、`Materials`（材料 × 形状）、`MachineFamilies`（机器 family）、
   `assets/recipes/<type>/*.json`（配方）、`PolarReactions`（有机规则）。
2. **id 永远只追加，不许插在中间。** 物品/方块/方块实体的 id 是**存档格式**的一部分；
   插在中间会让老存档读到别的东西。新的东西一律写在表尾。
3. **美术是零成本的（对材料而言）。** 形状的图是**灰阶**的，材料只提供**颜色**；
   渲染时颜色乘在图上。所以加一种金属 = 几行 Java + 一个颜色，**不需要任何图**。

---

## 1. 加一个物质（最常做的事）

物质 = 化学内核认识的一个分子 + 它所在的状态。它在 `chemistry/Substances.java` 的 `starter()` 里一行：

```java
catalog.register("lithium", "[Li]", Phase.SOLID);        // 元素：方括号里写元素符号
catalog.register("brass 前面的铜", "[Cu]", Phase.SOLID);
catalog.register("methanol", "CO", Phase.LIQUID);        // 有机：写 SMILES
catalog.register("uranium-235", "[235U]", Phase.SOLID);  // 同位素写在方括号里
catalog.register("sulfuric acid", "OS(=O)(=O)O", Phase.LIQUID);
```

加完这一行，**你什么都不用再做**：

- 它自动获得一个**物品**：固体 → 一份 **dust**（100 mB），液/气 → 一个 **cell**（1000 mB），见 `ChemicalItems`；
  已经在 `Materials`/`Fluids` 里有同名物品的会**借用**那一个，不会出两个。
- 若是液体或气体，它自动获得一条**流体**（`Fluids` 按物质建，名字一致）。
- 它自动能被反应引擎取用、能被配方文件按名字引用。

**要点**

| 事项 | 规则 |
|---|---|
| 名字 | 唯一；`Substances.register` 对「同一分子同一状态、两个名字」会当场抛异常（这是刻意的：不允许同一物质有两个条目） |
| 分子必须是**一个片段** | `[Na+].[Cl-]` 这种带点的写法会被拒绝——多个片段是**混合物**（`Mixture`/`Alloy`），不是物质 |
| 状态 | `Phase.SOLID / LIQUID / GAS`；水与蒸汽是两个条目（同一分子、不同状态） |
| 公式 | 不用写——它从分子算出来（`Composition.formula()`，Hill 序） |

## 2. 加一条流体

**通常不用手工加**：注册一个液/气物质就自动有了（见上）。只有「名字与物质不同」或「需要自己的温度/颜色」时才手写：

```java
public static Fluid MOLTEN_SALT;      // 在 Fluids 里声明
// registerAll() 里：
MOLTEN_SALT = register("molten_salt", color, temperature, "NaCl", Phase.LIQUID, substance);
```

- `Fluid` 在场上有 `substances()`（一个流体可以承载多个物质——溶液就是这么表示的）。
- 流体表的大小有测试钉着（`FluidTest`、`MachineStorageTest`）：**加了就同步那两个数字**。

## 3. 加一种材料（金属 / 矿物 / 宝石 / 聚合物……）

材料 = 颜色 + 公式 + **一组形状**。在 `material/Materials.java` 的 `declare()` **末尾**加：

```java
register(Material.builder("lithium", "Lithium").color(new Color(0.85f, 0.87f, 0.90f, 1f))
        .formula("Li")
        .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.PLATE)
        .build());
```

- **形状从哪来**：`kind(MaterialKind.METAL)` 会给一整套金属形状（粉/锭/粒/板/箔/棒/齿轮……）；
  `MINERAL`（碎矿/粉/宝石）、`GEM`（宝石/碎矿/粉）、`POLYMER`（条/板/箔/粉/齿轮，**不含锭**）。
  想只要几个形状就用 `onlyForms(...)` 覆盖，或用 `.forms(...)` 追加。
- **颜色就是全部美术**：`MaterialForm` 的图是灰阶的，`color()` 乘在上面。
- **id 固定**：某个形状的物品 id 已经被存档用过时，用 `.item(MaterialForm.INGOT, 19)` 钉住它；其余形状自动取下一个空 id。
- **越界公式要能解析**：`formula()` 会被 `Composition.parse` 读成各元素的比例，再拆成「各元素物质 × 比例」的 `Blend`
  （见 `Materials.blendOf`）——所以青铜 `Cu3Sn` 在锅里就是 Cu 75 mB + Sn 25 mB。**括号不要写**（`CaCO3` 而不是 `Ca(CO3)`）。

## 4. 加一个新形状（form）

1. 把灰阶图放进 `assets/items/`（文件名 `generic_<name>.png`，可再带一层 `<name>_overlay.png`）。
   **两层的约定**：底图会被染上材料色，覆盖层不染色（`MaterialForm#overlayTexture`）。
   ⚠️ 从 GTNH 素材包（`D:\textures\items\materialicons\NONE`）拷图时要注意**它是反的**：
   那边的 `X_OVERLAY.png` 是**全白遮罩＝染色层**，`X.png` 是深色细节＝不染色层
   ⇒ 拷进来要与本项目约定对齐：**把两层文件名对调**（`X_OVERLAY.png` → `generic_x.png`）。
2. `material/MaterialForm.java` 的枚举里加一项：
   ```java
   GEM("gem", "Gem", Item.DEFAULT_MAX_STACK, "generic_gem", "generic_gem_overlay"),
   ```
3. 想要它成为某类材料的默认形状，就把它列进 `MaterialKind` 里对应的那一种。
4. 跑 `.\gradlew.bat generateAssetList` 刷新 `assets/assets.txt`。
5. 跑 `--tests "*TextureAuditTest"`：它会挑出缺图、无 alpha 的图。

## 5. 加一种合金

合金是**混合物**，不是分子（`Cu3Sn` 这种写法在内核里是被拒绝的）：

```java
// Substances.starter() 末尾
catalog.registerAlloy(Alloy.of("bronze", Phase.SOLID,
        Alloy.part(catalog.byName("copper"), 3), Alloy.part(catalog.byName("tin"), 1)));
```

- 成分必须是**目录里已有的物质**（否则当场抛异常）；份额是小整数，`Alloy.blend()` 会按比例展开成 mB。
- 查找键把成分排序后拼接 ⇒「同一合金两种写法」是一个条目；「同名不同合金」会抛异常。
- 材料的物品侧：`Materials` 里同名材料（`bronze`）的公式已经让机器把它的物品读成混合物了，不用额外接线。

---

## 6. 加一条配方

配方都是 `assets/recipes/<type>/<name>.json`，**文件名就是配方名**。写错（不守恒、名字不在目录里）会在**读取时当场被拒**，不会带病运行。

### 6.1 化学路线（`chemical_reacting/`、`electrolysis/`）

```json
{
  "inputs":  { "sodium chloride": 200 },
  "outputs": { "sodium": 200, "chlorine": 100 },
  "primary": "sodium",
  "temperature": [1100, 1400],
  "time": 15.0,
  "power": 40,
  "voltage": 32
}
```

| 字段 | 说明 |
|---|---|
| `inputs` / `outputs` | **物质的英文名** + mB；1 mB ≈ 1 个分子，按**元素守恒**校验（`BlendConservation`） |
| `primary` | 配方浏览器上显示哪个产物；也是机器结算「一锅出什么」用的那个 |
| `temperature` / `pressure` / `catalysts` / `medium` | 可选的**条件**；机器不测的维度**不拦**（小化反没温度计，所以写了也不挡） |
| `time` | 一秒一次的运行秒数 |
| `power` | 一 tick 抽多少 EU（`energy()` = power × time） |
| `voltage` | 允许运行的机器档位 |

- `electrolysis/` 下的即「需要电流」的路线（电解机读这个文件夹）。
- ⚠️ **小化反只有 2 槽 + 2 罐**：固体 ≤2、流体进 ≤2、流体出 ≤2。有测试（`ChemicalMachineTest#everyWrittenRouteFits…`）逐条钉着。
- ⚠️ **必须先有物质**：写「三氧化钨」前先在 `Substances` 里注册它（否则加载器直接拒这条路线）。

### 6.2 合金熔炼（`alloy_smelting/`）

```json
{ "ingredients": ["copper_ingot", "tin_ingot"],
  "result": { "item": "bronze_ingot", "count": 2 },
  "time": 12.0, "power": 2, "voltage": 8 }
```

### 6.3 加工与合成

`smelting/`、`grinding/`、`compressing/`、`extracting/`、`forging/` 是「物品进、物品出」的加工配方，`crafting_shaped/`、`crafting_shapeless/` 是合成表。**照抄同目录里现有文件的字段最稳**，写完跑 `*RecipeLoaderTest`（它会读遍 `assets/recipes` 下每个文件）。

---

## 7. 加一台机器

1. **一行 family**（`MachineFamilies`）：机器是「一个 family × 三个档位（LV/MV/HV）」自动注册成三台。
   在类里声明一个 `Family`（如 `machine/MachineFamilies.java` 的用法），字段是：
   `name, display, recipeTypes, inputs(槽的 SlotKind 列表), outputs, fluidInputs, fluidOutputs, maxAmps, factory`。
2. **一个类**：继承 `ElectricMachine`（带能量、有配方循环的机器）。
   构造函数把 `FAMILY.screenOf(tier)`、`FAMILY.inventory()`、档位、安培、配方类型，以及 0–4 个 `MachineTank.of(容量, Role.INPUT/OUTPUT)` 交给父类。
3. **登记到表的末尾**：`MachineFamilies.ALL`。
4. **GUI 不用写坐标**：`MachineScreen` 按**槽数**自动排版（1 个独占、2 个并排、4 个方阵、6 个两行三列），罐在面板脚下。
5. **美术**：机壳/面板/overlay 贴图 + 模型 + blockstate（仓库里有 `tools/gen_pipe_models.ps1` 这类生成脚本可照做），跑 `generateAssetList`。
6. **验收**：`machine.*`，其中 `ElectricMachineTest` 会遍历 family 表，逐台比对槽数/配方组/安培/标题。

**机器的配方循环**（`RecipeMachine`）值得记住，加机器几乎不用动它：

```
update(delta)
  ├─ 没有正在做的 craft → findRecipe()      ← 想加「自己找活干」的机器，只覆盖这个（见 ChemicalReactorMachine）
  │      → recipe.fits(outputs())? → payForWork()? → recipe.consume(inputs())
  └─ 正在做 → payForWork() → craftSeconds += … → 到点 → recipe.produce(outputs()) → craftFinished(recipe)
```

**输入从哪来**：`MachineInput`（`items()` 是 `RecipeGrid`，`tank(i)` 是 `FluidStorage`）。想把自己的机器接上化学推断，照 `ChemicalReactorMachine`：把槽+罐读成 `Blend` → `ReactionRouter.route(pile, conditions)` → 把结局装扮成 `ChemicalRecipe` 交给同一套付费/交付循环。

---

## 8. 加一条有机规则

在 `chemistry/PolarReactions.java` 的表里加一条，外加一个「怎么写」的函数：

```java
rules.add(new ReactionRule("myRule", "condensation", 3, EnumSet.of(FunctionalGroup.ALDEHYDE),
        Conditions.at(Warmth.HEATED), PolarReactions::myWriting));
```

- `category` 会决定它是不是「简单有机」（`PolarReactions.simple()` 按 family 过滤，小化反只用简单的那批）。
- `selectivity` 越大越优先被 `PolarEngine.best` 选中；同分看表里的先后。
- **电子怎么记账**：`Arrow`（一对电子或一个电子；`homolysis`=键均匀裂成两个自由基、`couple`=两个自由基成键）。
  **箭头不写电荷也不写自由基**，而是 `Δcharge = −(2·Δ孤对 + Δradical + Δ键级和)`，出站时整分子校验——
  **画错只会表现为「这条规则当没发生」**，不会静默出错。
- **造小分子（H₂/H₂O/HX）的套路**（照抄 `dehydrogenate` / `esterify` / `formAmide`）：
  `betweenBonds(给原子的C, 它的H, 它的H, 另一个H)`（用 C–H 的电子对造 H–H/H–X）
  → `toLonePair(那个H, 它原来挂的原子)`（σ 对收成孤对，把电荷收干净）
  → `fromLonePair(那个原子, 那个原子, 要接上来的原子)`（孤对推进新键）。
- **手性 / 双键构型**：`ElementaryStep.substitution(name,Nu,C,LG)` 自带翻转、`addition(name,Nu,C,acceptor,Face)` 按面决定新手性；
  周环两条键用 `PolarReactions.setFaces`；`DoubleBond.set(product, bond, 'E'|'Z')` 写双键构型。
- 验收：`chemistry.*`（`PolarEngineTest` 钉着规则条数，加规则要同步那个数字）。

---

## 9. 加方块 / 物品 / 创造栏

- 物品：`items/Item.builder(id, name).displayName(...).texture("items/xxx").maxStackSize(...)`，登记到 `Items`
  （`id` 追加、不要重排）；贴图放 `assets/items/`。
- 方块：`Blocks` 里登记 + 模型（`assets/models/block/`）+ blockstate（`assets/blockstates/`）+ 方块实体（`blockentity/`）。
- 创造栏：`CreativeRegistry` 按物品类型自动归类（材料类物品按 `MaterialForm` 归到对应页）。
- 贴图都要跑 `generateAssetList`，并过 `*TextureAuditTest`。

## 10. 存档与版本

- `level.dat` 的结构写在 `world/save/LevelData` 的类注释里：
  `DataVersion, WorldName, GameMode, WorldType, Seed, SpawnX, SpawnZ, Created, LastPlayed` +
  `Player（Inventory、GameRules 等）` + `GameRules`。
- **不兼容改动要递增 `DATA_VERSION`**（`SaveFormat`），读老版本要能容忍缺失字段。
- 键名都在 `SaveTags`；**机器自己的状态可以自己写 key**（例：化学釜把「正在推断的那条反应」写进
  `neofactory.inferred_recipe`，`RecipeMachine#unknownCraft` 在重载时把它读回来）。
- 新存档默认：**创造模式 + 空物品栏**（`LevelData#gameMode` / `storedInventory`）。

## 11. 每一次加东西的验收清单

1. 编译：`.\gradlew.bat :core:compileJava :core:compileTestJava --offline`
2. 加物质/流体 → 同步 `FluidTest`、`MachineStorageTest` 的**流体数字**
3. 加材料 → `*MaterialPreviewTest`、`*MachinePreviewTest`、`*TextureAuditTest`
4. 加配方 → `*ChemicalRecipeTest`、`*RecipeLoaderTest`（读取即校验守恒与「名字必须是目录里的物质」）
5. 加规则 → `chemistry.*`（并同步 `PolarEngineTest` 的规则条数）
6. 加机器 → `machine.*`（`ElectricMachineTest` 遍历 family 表逐台比对）
7. 加美术 → `generateAssetList` + `*TextureAuditTest`
8. 提交前：行尾统一 LF、删掉临时文件（`*.log` / `cmsg.tmp`）、`git status --porcelain` 干净、
   用 **JUnit XML** 报 `tests/failures/errors`（不要看日志文件）

---

## 12. 代码地图（想改哪里）

| 包 | 管什么 |
|---|---|
| `chemistry/` | **纯 Java 内核**（不许 import libGDX）：SMILES 解析/写回、芳香化、规范化、精确分数、守恒、无机路线簿、有机规则表与推断、立体化学、`Substances` 物质目录、`Alloy` 合金 |
| `material/` | 材料 × 形状（`Materials` 一张表 → 物品）、`MaterialForm`（形状 + 灰阶贴图 + 每件多少 mB）、`MaterialKind` |
| `item/` | 物品注册、玩家物品栏、容器/单元（`FluidCells`/`CellTransfer`）、`ChemicalItems`（物质 ↔ 物品） |
| `fluid/` | 流体表（每个液/气物质一条）与流体存储 |
| `recipe/` | 配方登记与加载：`RecipeRegistry`、`RecipeType`、`ChemicalRecipe`（化学路线）、`ProcessingRecipe`、`CraftingRecipes`、`RecipeLoader` |
| `machine/` | 机器的形状与循环：`MachineFamilies`（family 表）、`RecipeMachine`（配方循环）、`MachineScreen/Menu`（面板与槽）、`MachineTank`、`SlotKind`，以及每台机器一个类 |
| `energy/`、`cable/` | 能量缓存、电线、二极管、变压器、电池 |
| `block/`、`blockentity/` | 方块与方块实体注册 |
| `gui/`、`screen/`、`render/` | 界面、面板贴图、世界渲染与模型 |
| `world/`、`world/save/` | 世界、区块、存档（`LevelData`/`SaveFormat`/`SaveTags`）、`GameMode` |
| `tools/`、`assets/` | 生成脚本（美术/模型/导入）与资源（贴图/模型/blockstate/配方 JSON/`assets.txt`） |


