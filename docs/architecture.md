# 架构与关键语义

> 代码地图见 [`modding.md`](modding.md) 第 12 节；这份文件讲**系统怎么一起工作**，以及踩过的坑。

## 1. 能量：**拉模型**

- 机器**不推电**，是**拉**：机器每 tick 需要一个电流（`requestAmps`），电线按「谁先要谁先拿」分配。
- 机器有自己的缓存（`EnergyStorage`）：一 tick 只吃**自己这一 tick 需要的那一份**，多出来的留在线上；
  64×V（满功率×64 tick）是缓存的量级。
- **电压 ≠ 消耗**：一个档位是「一条线每 tick 能携带多少」。机器只有**欠电**（拿不够）时按比例推进，
  不会因为「电多」而变快——**超频**是配方/机器自己的事。
- **过压与熔断**：线上电压高于机器/电线的额定 ⇒ 过压（可能熔断）。电缆按材质 × 尺寸分档。
- 二极管是**带缓存的单向机器**：15 种；变压器 6 种（LV/MV × 1/4/16 A，软锤换向）。
- 电池箱 12 种 + 电芯 15 种、能量终端按档着色。

## 2. 机器的配方循环

`RecipeMachine#update(delta)`：**找不到 craft → `findRecipe()`**（可覆盖：`ChemicalReactorMachine` 就在这一步
「先查表、再推断」）→ `fits(outputs())` → `payForWork()` → `consume(inputs())`（**开工即吞料**）
→ 每 tick 按 `craftSeconds/craftTotal` 付钱推进 → 到点 `produce(outputs())` → `craftFinished(recipe)`。

- **欠电不丢进度**：付不起的一帧不算，机器保持原样等着（和炉子保住火一样）。
- **存档恢复**：`saveState` 记配方**名字**；重载时 `RecipeMachine#restoredCraft` 按名字找回来，
  找不到就调 `unknownCraft(name)`（机器自己的配方——如化学釜推断出来的那条——从这里回来）。

## 3. 蒸汽时代

- 蒸汽机的能量就是**蒸汽本身**：`MachineRecipe#steam()` = `4 × energy()` mB（4 mB 蒸汽 = 1 EU）。
- 锅炉烧燃料出蒸汽；蒸汽机工作时**把用掉的蒸汽排到指定的那一面**（`ExhaustMachine`）。
- 蒸汽时代有青铜/钢两代压力（`MachinePressure`），机器标题带压力前缀。

## 4. 面板与 GUI

- `MachineScreen` 决定「这台机器的面板长什么样」：槽的 **kind 列表 + 数量**（摆位由数量自动决定）、
  罐的数量（0–2 进、0–2 出）、进度条样式、是否留一个配置槽、是否整面板就是一片网格。
- `MachineMenu` 把面板 + 玩家物品栏拼成可点击的布局；**罐不是槽**：拿单元点它即倒/取一整个单元（`CellTransfer`）。
- 面板贴图按「样式」分（青铜/钢/电气灰），槽的图标取自 `gui/machine_icons.png` 的格子（`SlotKind`）。

## 5. 渲染与模型

- 机器是**方块模型 + 每面贴图**：`_front/_side/_top/_bottom` + 工作时换 `_active`。
- 帧条带动画（`tools/gen_pipe_models.ps1` 之类脚本生成模型/blockstate）。
- 物品/方块贴图是**灰阶 + 颜色**：材料的 `color()` 乘在形状的图上，**加一种材料零美术**（见 `modding.md` §3/§4）。

## 6. 存档与 id 空间

- `level.dat`（`LevelData`）：`DataVersion / WorldName / GameMode / WorldType / Seed / Spawn / Created / LastPlayed` + `Player` + `GameRules`。
- 区块是 `ChunkCodec` 打包的 `section`（`ChunkStorage`）。
- **id 是永久编号**：物品、方块、方块实体各有一张表，新条目**只追加**；
  某个物品曾经用过的编号可以用 `Material.Builder#item(form, id)` 钉住（铁锭就是这么保住它老编号的）。
- **不兼容的存档结构改动要抬 `DATA_VERSION`**，并且读旧版要能容忍缺字段（例：老存档没有 `GameMode` ⇒ 当 `SURVIVAL` 读）。

## 7. 踩过的坑（排查时先看这里）

1. **日志文件不可靠**：Gradle 缓冲到结束才 flush，`> x.txt` 常为空/残缺 ⇒ **一律读 JUnit XML**。
2. **结果目录在测试任务开始时被清空** ⇒ 「目录里还有上一次的 XML」说明本次还没跑到测试。
3. **「写好了但从没被调用」**：`MachineGui.drawEnergy(...)` 曾写好却没人调；查 GUI 问题先查「有没有被调用 / 有没有图」。
4. **调色板 PNG（无 alpha）会被 `TextureAuditTest` 挑出来** ⇒ 新增贴图必须先程序化验证尺寸/alpha/是否灰阶。
5. **`Sites` 的取氢函数取一次给你一个氢**，取两个不同的氢要用专门的方法（水有两个氢，混了就错）。
6. **容器只写 `Warmth.HEATED` 时，需要催化剂的规则也会跑**（催化剂是「条件」，不是消耗品）；
   反过来，机器没测的维度不拦——要挡就得在规则里要求。
7. **同一物质两种写法要是一个条目**：目录按分子查（不是按名字）；苯的芳香/Kekulé 写法、水/蒸汽的差别都靠这个。
8. **`Chemical.parse` 拒绝带点的 SMILES**（多片段 = 混合物）⇒ 合金/盐水/溶液走 `Mixture`/`Alloy`，别硬写成分子。
9. **大改动别动 `MachineFamilies.ALL` / `Materials.declare()` 的中间**：family 顺序与材料声明顺序就是 id 顺序。
10. **在 `chemistry/` 里不许 import libGDX**（内核要能单独跑测试）。
