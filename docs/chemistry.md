# 化学内核的规则（速查）

> 想加化学内容看 [`modding.md`](modding.md) 第 1、2、5、6、8 节；这份文件只讲**语义**——内核怎么想事情。

## 1. 三种东西：纯物质、混合物、反应

| 概念 | 含义 | 代码 |
|---|---|---|
| **物质** | 一个分子 + 状态。水是一个化合物，青铜不是 | `Substance`、`Chemical`（`ElementChemical`/`CompoundChemical`） |
| **混合物** | 多个物质并排（合金、溶液、盐的两离子） | `Mixture`、`Blend`（按 mB / 分子计数）、`Alloy`（有名有份额的混合物） |
| **反应** | 「这一步动了哪些电子」+ 记账 | `Arrow`、`ElementaryStep`、`Mechanism`、`Reaction` |

**单片段不变量**：`Chemical.parse("[Na+].[Cl-]")` 会被拒绝——带点的写法是混合物，不是物质。合金、盐水、溶液一律走混合物那条路。

## 2. 守恒裁判：箭头不写电荷

- 一根箭头 = 一对（或一个）电子从「孤对 / 键 / 自由基」移到「键 / 孤对 / 自由基」。
- 电荷不是写出来的，是**算出来的**：`Δcharge = −(2·Δ孤对 + Δradical + Δ键级和)`，出站时整分子校验。
- **画错的表现**：`Mechanism.apply` 的守恒失败 ⇒ 这条规则「当作没发生」，不会静默产出错的分子。
- 鱼钩箭头：`homolysis`（键均匀裂成两个自由基）、`couple`（两个自由基成键）、`fromRadical`/`toRadical`。

## 3. 两条路：无机查表、有机推断

| | 无机 | 有机 |
|---|---|---|
| 来源 | `assets/recipes/chemical_reacting` + `electrolysis` 的 JSON（人写的） | `PolarReactions` 的规则表（引擎按结构推断） |
| 语义 | **不命中 = 不反应**，绝不外推 | 允许自由重写（模板/规则合法即反应） |
| 断言 | 读取时校验**元素守恒**与「名字必须是目录里的物质」 | 出站校验守恒 |
| 入口 | `InorganicRecipeBook` | `PolarEngine`（`best(system)` 按 selectivity 选） |
| 合流 | `ReactionRouter.route(blend, conditions)`：**先查表、再推断、都不中就什么都不做** | |

`ReactionRouter` 返回 `Outcome`（`consumed`/`produced`/`electrons`/`medium`），机器把它当配方结算（见 `ChemicalReactorMachine`）。

## 4. 物质目录是**封闭世界**

- 求解器只在目录里找产物：不认识的组合**不反应**，而不是凭空造一个化合物。
- 目录按「分子 + 状态」查找（不是按名字）：苯的芳香写法和 Kekulé 写法是一个条目；水和蒸汽是两个。
- 要加物质 = 在 `Substances.starter()` 加一行；**同名不同分子**会当场抛异常。
- 同位素是独立条目（`[7Li]` 与 `[Li]` 是两行）。

## 5. 立体化学

- 手性：`Atom.chirality()`（`@`/`@@`）、`Cip.configuration`（R/S）、`Stereocentre.set/invert/apply`。
- 双键：`Cip.descriptor`（E/Z，标记写在双键**旁边**的单键上）、`DoubleBond.set(product, bond, 'E'|'Z')`。
- 「按底物预测面」：`Steric.faceFor`（用 `Conformer` 建出的真实形状算两侧拥挤度 ⇒ Cram 一族都在里面）；
  周环用 `PolarReactions.setFaces`（DA / 电环化 / σ迁移 / [2+2] 的两个新中心按同一面写入）。
- `StereoKey.of(m)` = `canonicalKey()` + 构型（无立体时完全退化 ⇒ 老存档/老物质不受影响）。

## 6. 条件（温度 / 压力 / 催化剂 / 光 / 电流 / 介质）

- `Conditions` 是**可要求、也可不要求**的：一条规则/路线只在自己写的维度上被拦。
- **机器不测的维度不拦**：小化反没有温度计，所以写了温度的路线照样能跑；但它只声明 `Warmth.HEATED`，
  于是需要催化剂/光照/压力的**有机规则**在小化反里永不触发（这正是「只能做简单有机」的一半原因）。
- 光与电流是「**要求的**」维度（不点灯就是黑的，见 `ConditionsTest`）。

## 7. 分数的算术

- 量一律用精确分数 `Fraction`（`of/plus/times/dividedBy/compareTo`），**不用 double**
  ⇒ 配方按元素守恒能**精确**校验（`BlendConservation`），不会因浮点误差误判。
- 1 mB ≈ 一个分子（`Blend` 的记账单位），固体一件 = 100 mB（dust/ingot）、粒 = 10、单元 = 1000。
