# NeoFactory — 文档

这是本项目的文档目录。**游戏怎么玩**看根目录的 `README.md`；**怎么给这个项目加东西**看这里。

| 文件 | 讲什么 |
|---|---|
| [`architecture.md`](architecture.md) | 代码地图：每个包做什么、关键 API、一个 tick 里发生了什么 |
| [`modding.md`](modding.md) | **怎么加入新东西**：物质 / 流体 / 材料 / 形状 / 机器 / 配方 / 有机规则 / 美术，逐条给出格式与步骤 |
| [`chemistry.md`](chemistry.md) | 化学内核的规则：SMILES 语义、守恒裁判、无机路线格式、有机规则表与推断器、立体化学 |

---

## 1. 项目概况

**NeoFactory** —— 从零实现的 GTNH（GregTech: New Horizons）风格工厂游戏。

- 技术栈：Java 17 + libGDX；`core` = 游戏逻辑（**纯 Java，化学内核不许 import libGDX**），`lwjgl3` = 桌面启动器；构建用 Gradle（`gradlew.bat`，常配 `--offline`）。
- 代码根：`core/src/main/java/com/philia093/neofactory/…`，测试 `core/src/test/java/com/philia093/neofactory/…`，资源 `assets/`。
- 单位：`EU` 能量（1 EU = 一 tick 满功率的工作量）、`euPerTick` 电压、`mB` 流体、1 tick 固定 20/s（`TickClock.TICK_SECONDS`）。
- 电压阶梯每级 ×4：`ULV 8 / LV 32 / MV 128 / HV 512 / EV 2048 / IV 8192 / LuV 32768 / ZPM 131072 / UV …`（`cable.Voltage`；游戏里的线只到 HV）。

**新存档默认是创造模式**（`world/save/LevelData#gameMode`），物品栏是空的：这个游戏是用来搭工业的，第一天就该拿到目录里的东西，而不是先去手挖铁。

## 2. 命令速查

```powershell
cd D:/NeoFactory
.\gradlew.bat :core:compileJava :core:compileTestJava --console=plain --offline   # 只编译
.\gradlew.bat :core:test --tests "*MachineStyleTest" --console=plain --offline    # 只跑一个类
.\gradlew.bat generateAssetList                                                   # 刷新 assets/assets.txt
.\gradlew.bat lwjgl3:run                                                          # 跑游戏（要窗口）
git --no-pager log --oneline -6 ; git --no-pager status --porcelain
```

**判断测试结果一律读 JUnit XML，不要靠日志文件**（Gradle 会把日志缓冲到结束才 flush，`> x.txt` 常常是空的）：

```powershell
$t=0;$f=0;$e=0
foreach ($p in Get-ChildItem core/build/test-results/test/*.xml) {
  $c=[xml](Get-Content $p.FullName); $t+=[int]$c.testsuite.tests
  $f+=[int]$c.testsuite.failures; $e+=[int]$c.testsuite.errors
}
"tests=$t failures=$f errors=$e"
```

结果目录在测试任务**开始时**被清空，所以「目录里还有上一次的 XML」说明本次还没跑到测试阶段（不是失败）。长跑用 `Start-Process cmd /c "..." -WindowStyle Hidden` 起后台再轮询。

## 3. 写代码/提交的硬性约定

1. **长篇 javadoc 用叙事英语讲「为什么」**，不是罗列参数；注释讲规则与理由，不写「是什么」。命名用完整英文单词（`MachineEnergyStorage`、`FaceConfig`），不缩写（`euPerTick` 这类业界通用词除外）。
2. **偏好「一张表 + 数据驱动」**，厌恶样板代码（18 台机器由 `MachineFamilies` 一张表驱动注册、86+ 物质由 `Substances` 一张表、材料由 `Materials` 一张表）。
3. **不要随手动全量测试**（整跑约 3–4 分钟）：只跑受影响的测试类；带 `--tests` 过滤时 Gradle 仍会编译整个测试源集，等于顺带做了编译检查。
4. **提交信息**：`git commit -m "<area>: <小写短句>"`（area 例：`machine`/`energy`/`material`/`chemistry`/`gui`/`line`），正文多段叙事英语（每段 3–6 行），无 emoji、无署名。
5. **提交前**把改动的文本文件行尾统一成 LF（仓库是 `text=auto eol=lf`），删掉临时文件（`*.log`、`cmsg.tmp`），`git status --porcelain` 必须只剩未跟踪的文档文件。
6. **美术/资源用程序验证，不靠肉眼**：用 .NET `System.Drawing` 打开 PNG 看尺寸/alpha/是否灰阶；新增美术后必须 `.\gradlew.bat generateAssetList` 刷新 `assets/assets.txt`。
7. **小步提交**：一批改动完成后立刻提交，提交前只跑相关测试类。
8. **id 永远只追加，不插入中间**：物品 / 方块 / 方块的 id 都属于**存档格式**，一个材料、一个 family、一条路线都要写在表的**末尾**，否则老存档会读错（见 `modding.md`）。
9. **不许凭空发明**：数值、容量、配方取舍按需求方给的来；没给的先问，不要自己编。
