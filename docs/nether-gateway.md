# 未完成下界传送门

自然结构 ID：`mathmaster:ruins/unfinished_nether_gateway`。

## 模板与箱子

每个候选先在五个模板中等概率选取，每款20%，整座随机水平旋转；实际成功生成的款式占比还受模板高度、箱子位置与地形筛选影响。原始 NBT 直接复制，来源为用户的 `MathMaster-resource/Sructure/Ruins/`，保持尺寸、方块坐标、底层地形及所有其他数据。箱子已正确使用下界专用表，不额外修改 NBT。

| 模板后缀 | 原始尺寸 | 箱子本地坐标 |
|---|---|---|
| 1 | 10×8×10 | [4,2,7] |
| 2 | 10×7×10 | [5,1,9] |
| 3 | 10×6×10 | [4,1,2] |
| 4 | 10×8×10 | [3,1,8] |
| 5 | 10×8×10 | [7,4,6] |

模板目录为 `data/mathmaster/structure/ruins/`，命名 `unfinished_nether_gateway_1.nbt` 至 `_5.nbt`。自然结构JSON的 `templates` 列表控制等概率选择，追加模板无需修改 Java；这座结构不使用主世界的 jigsaw 地表高度投影。

独立箱子表为 `mathmaster:chests/unfinished_nether_gateway`，保留此前11项、总权重134、每箱3～6次的奖励版本，使用独立随机序列；主世界已另行改为21项、6～8次，下界不跟随改动。放箱指令：

```mcfunction
/setblock ~ ~ ~ minecraft:chest{LootTable:"mathmaster:chests/unfinished_nether_gateway"}
```

保存前保持箱子未打开，保留 `LootTable` 引用；已打开的箱子不会随表更新重新抽奖。

## 生成规则

- 五种原版下界群系均允许：下界荒地、灵魂沙峡谷、绯红森林、诡异森林、玄武岩三角洲。群系标签为 `#mathmaster:has_structure/unfinished_nether_gateway`。
- 独立结构集 `mathmaster:unfinished_nether_gateways` 使用 `random_spread`，40区块分布、15区块分离、50%候选通过，salt为184735093；不改变主世界或N类试炼的参数，也不刻意与原版结构重合。
- 每个640×640格区域有一个随机候选，约一半通过频率筛选，之后还可能因地形不合适跳过。参数描述候选分布，不保证实际数量或相邻遗迹的距离。
- 基础层原点在Y=32～100内寻找，底层直接嵌入所选地面高度，上层保持原始相对坐标。以原版的随机起始高度和向下寻找支撑为基础；若下方无合适落点，再检查本范围内剩余的上方高度，不越过高度范围。
- 在底座四角、四边中点和中心共9处采样，至少7处为非液体、非基岩的遮蔽性实块。底座上方的9列采样空间至少75%可替换且无液体，中心另保留3格高的空间；允许轻微山壁嵌入与边缘悬空，不要求完全平坦。
- 箱子位置及其上方可替换、至少一侧留有2格高的可进入空间。主体底层以上的整个包围体再逐列排除液体，防止细岩浆柱落在采样点之间；底层边缘和遗迹附近可存在岩浆。
- 使用生成器的预测噪声列，不在生成期间请求或加载相邻区块；列数据在单次查找内缓存，没有跨世界或跨生成的地形缓存。检查发生在后续地表装饰、其他结构与液体流动之前，不承诺最终环境永不改变。
- 不使用原版传送门的随机破坏、黑石替换、蔓延下界岩或空气口袋加工，也不修改模板几何。采用原版 `PoolElementStructurePiece` 放置和存档，`terrain_adaptation: none`，模板中的空气正常放置，保持用户模板内容。

## 测试指令

重启安装了当前开发资源的世界，在下界执行：

```mcfunction
/locate structure mathmaster:ruins/unfinished_nether_gateway
/place structure mathmaster:ruins/unfinished_nether_gateway
```

`/place structure` 同样先做地形检查，不合适的地点会放置失败。仅查看某个原始模板时，可在空旷位置使用：

```mcfunction
/place template mathmaster:ruins/unfinished_nether_gateway_1
```

后缀可换为2～5。模板命令绕过自然生成的频率、群系和地形筛选。自然测试在新区块或新世界进行；已生成区块不会补生建筑。

核心入口为 `worldgen/UnfinishedNetherGatewayStructure.java`、`NetherGatewayTerrain.java` 与 `init/ModStructures.java`。`NetherGatewayStructureCheck` 接入现有隔离gateway检查，验证受控地形边界、完整模板四向放置/真实开箱、原版组件存档、真实下界地形和自然定位；真人地形观感仍需客户端测试。
