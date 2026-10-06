# 未竟之门·主世界（开发测试）

普通结构 ID：`mathmaster:ruins/unfinished_overworld_gateway`。

巨型结构 ID：`mathmaster:ruins/unfinished_overworld_giant_gateway`。

采用原版数据驱动结构生成，无需增加 Java 注册。普通门的十一个模板（1、11、2、3、4、5、6、7、8、9、10）在同一模板池中等权随机选择，整座随机旋转，生成在21种草地/森林类主世界群系（完整列表见下文）。全部保留原始尺寸与方块坐标，沿用模板1已验证的起始高度偏移−1和原版 `beard_thin` 地形适配；不做高度归一化。模板1的自然生成已由用户实测通过，其余模板的地形观感仍需客户端检查。

| 普通模板后缀 | 原始尺寸 | 箱子本地坐标 |
|---|---|---|
| 1 | 10×9×10 | [7,2,4] |
| 11 | 8×8×10 | [5,2,7] |
| 2 | 9×9×9 | [3,1,2] |
| 3 | 10×9×10 | [6,2,5] |
| 4 | 10×11×10 | [5,1,6] |
| 5 | 10×9×10 | [2,0,6] |
| 6 | 9×9×9 | [7,2,3] |
| 7 | 10×9×10 | [4,1,6] |
| 8 | 10×9×10 | [6,2,7] |
| 9 | 10×9×10 | [4,1,5] |
| 10 | 10×9×10 | [2,1,7] |

2、4、7、9、10号箱子的本地y为1，5号为0，其余普通模板为2；原样保留，不为贴地检查而移动箱子或模板方块。

巨型模板 `unfinished_overworld_giant_gateway_1` 的原始尺寸为16×16×11，同样保留全部原始坐标、尺寸和高度，沿用模板1已验证的地表投影配置。它有三个箱子，其中本地[6,4,2]处的箱子下方原本为空气；此布局原样保留。只替换三个箱子的战利品表引用。

## 生成机制

- 原版1.21.1的 `RuinedPortalStructure` 先以5%概率选择巨型，再在3个 `giant_portal` 模板内等概率选形；其余95%在10个 `portal` 模板内等概率选形。巨型不是额外独立刷一座。
- 本模组采用相同的大小型占比，并为了分别定位，将普通和巨型设为两个结构ID，放在同一个 `mathmaster:unfinished_overworld_gateways` 结构集中，权重分别为19和1。每个候选位置最多生成其中一种，合计频率不增加。原版的加权结构集在某种结构生成失败时会尝试剩余类型；两种门使用相同群系和有效的地表配置，正常条件下按95%/5%选择。
- 分布沿用原版废弃传送门的 `random_spread`、`spacing: 40`、`separation: 15`，单位为区块。每个40×40区块区域（640×640格）有一个随机候选起点，候选坐标在区域内每轴0～24区块之间取偏移；`separation`约束候选分布，不是建筑之间保证的球形距离。
- 按用户此前要求保留 `frequency: 0.5`，约一半候选点通过筛选。因此在不考虑群系等限制时，每个区域普通门概率47.5%、巨型门概率2.5%、跳过概率50%；在已成功生成的门中，巨型约占5%，不是每20座必定出现一座。
- 类型确定后，普通池的1、11、2、3、4、5、6、7、8、9、10各占1/11（约9.09%），巨型池目前只有一个模板。后续向对应池增加模板不会改变19:1的大小型权重。
- 生成在21种草地/森林类主世界群系（完整列表见下文），四种水平旋转随机选择；共用原有地面高度与 `beard_thin` 地形适配。没有采用原版下界岩蔓延、随机破坏模板、地下掩埋或海底生成逻辑。实际数量还受本模组的群系范围影响，不能保证是原版实际生成数量的一半。
- 使用本模组独立salt，候选点不会刻意与原版废弃传送门重合。下界另用独立结构集及洞窟地面搜索，说明见[未完成下界传送门](nether-gateway.md)；末地尚未接入。

## 模板与战利品

- 模板目录：`data/mathmaster/structure/ruins/`；普通模板为`unfinished_overworld_gateway_1.nbt`、`unfinished_overworld_gateway_11.nbt`、`unfinished_overworld_gateway_2.nbt`、`unfinished_overworld_gateway_3.nbt`、`unfinished_overworld_gateway_4.nbt`、`unfinished_overworld_gateway_5.nbt`、`unfinished_overworld_gateway_6.nbt`、`unfinished_overworld_gateway_7.nbt`、`unfinished_overworld_gateway_8.nbt`、`unfinished_overworld_gateway_9.nbt`、`unfinished_overworld_gateway_10.nbt`，巨型模板为`unfinished_overworld_giant_gateway_1.nbt`。
- 箱子战利品表：`mathmaster:chests/unfinished_overworld_gateway`。
- 普通门每座一箱，巨型门每座三箱，使用相同战利品表，每箱独立抽取6～8次，允许重复抽中。已取消早期固定一个灵虚锭的测试奖励。
- 用户导出的原始 NBT 不修改；原八个项目副本仅替换箱子战利品表引用。新增7/8/9/10的箱子已正确使用上述专用表，直接逐字节复制，SHA256与来源一致。全部保留原始几何、本地方块坐标与实体数据。
- 后续用户提供的模板均按模板1相同的高度形式制作，直接沿用该接入方式；不得自行移动方块坐标、修改保存尺寸或进行高度归一化。

总权重249，常见/一般/稀有/超级稀有的**每件物品权重**分别为30/10/3/1，不是先抽稀有度。共21项，单池抽取6～8次，允许重复，无保底。下表数量为单次抽中的数量，重复抽取或箱子分堆会使实际总数量、占用槽数不同。

| 分类 | 物品 | 单次数量 | 每件权重 |
|---|---|---:|---:|
| 常见 | 石砖 | 4～12 | 30 |
| 常见 | 腐肉 | 3～8 | 30 |
| 常见 | 被数字污染的方块 | 1～3 | 30 |
| 常见 | 蜘蛛网 | 1～3 | 30 |
| 一般 | 灵虚粒 | 3～9 | 10 |
| 一般 | 铁粒 | 6～18 | 10 |
| 一般 | 金粒 | 4～12 | 10 |
| 一般 | 金苹果 | 1 | 10 |
| 一般 | 附魔灵虚锄、附魔灵虚锹、附魔灵虚靴子、附魔学士帽 | 每件1 | 各10 |
| 一般 | 素数核心 | 1～2 | 10 |
| 一般 | 启蒙数学手册、空集 | 每件1 | 各10 |
| 一般 | N类试炼地图 | 1 | 10 |
| 稀有 | 附魔金苹果 | 1 | 3 |
| 稀有 | 灵虚锭 | 1～2 | 3 |
| 超级稀有 | 附魔灵虚胸甲、附魔灵虚护腿、素粒 | 每件1 | 各1 |

六件装备沿用原版 `enchant_randomly` 与 `#minecraft:on_random_loot`，从适用附魔中随机选择一种及其等级，可能出现诅咒。灵虚头盔已从本表移除。每箱至少出现某项或某组奖励的概率为 `1 - Σ(n=6..8)(1-w/249)^n / 3`，其中w是该项或该组权重之和：N类试炼地图约24.90%、任意装备约72.25%、任意稀有约15.68%、任意超级稀有约8.13%、指定一种超级稀有约2.78%。

地图使用原版空地图加 `minecraft:exploration_map`，目标标签 `#mathmaster:on_n_trial_maps` 仅包含 `mathmaster:trials/n_trial`；红叉标记、缩放等级2（512×512格地图）、原版搜索参数50。该参数沿用原版结构搜索单位，不表示50格的固定距离。`skip_existing_chunks: false` 允许指向已探索/已被其他地图指引的试炼，不增加结构引用次数；地图可能重复指向同一座。原版同步定位与地形预览在实际抽中地图时执行，首次可能增加开箱耗时。无法找到目标时沿用原版回退为空地图（仍带地图名称），不会凭空制造试炼；关闭结构生成或不含试炼的维度/数据包尤其如此。中英文显示名为N类试炼地图/N-Class Trial Map，无新增物品注册或自定义存档格式。

战利品表更新可用`/reload`，首次加入结构标签后建议重启世界。已开过的箱子库存不重新抽取；未开且仍保留该LootTable引用的箱子会按新表生成。N类遗迹补给表与N祭坛血祭奖励表本轮不改。

## 主世界与下界的独立箱子

主世界箱子：

```mcfunction
/setblock ~ ~ ~ minecraft:chest{LootTable:"mathmaster:chests/unfinished_overworld_gateway"}
```

下界箱子：

```mcfunction
/setblock ~ ~ ~ minecraft:chest{LootTable:"mathmaster:chests/unfinished_nether_gateway"}
```

两张表独立维护，下界表保留此前11项、总权重134、每箱3～6次的奖励版本，不跟随本次主世界改动，使用独立的随机序列 `mathmaster:chests/unfinished_nether_gateway`。后续可单独调整下界奖励。更新开发资源并重载数据后，可使用下界表；打开过的箱子不会重新抽奖。保存结构前保持奖励箱未打开，保留 `LootTable` 引用。

下界模板1～5已接入，保存名为 `mathmaster:ruins/unfinished_nether_gateway_1` 至 `_5`，项目目录为 `data/mathmaster/structure/ruins/`。自然结构 `mathmaster:ruins/unfinished_nether_gateway` 可在下界用 `/place structure` 和 `/locate structure`；采用五种下界群系、40/15与50%候选通过，专门寻找Y=32～100的洞窟地面。模板尺寸、支撑/空间/岩浆筛选与测试指令见[未完成下界传送门](nether-gateway.md)。

## 游戏内测试

进入安装了开发资源的新世界，先在空旷地面直接放置：

```mcfunction
/place structure mathmaster:ruins/unfinished_overworld_gateway
/place structure mathmaster:ruins/unfinished_overworld_giant_gateway
```

检查建筑高度、朝向、污染方块与箱子；首次打开箱子应按上表生成随机战利品。抽取次数不等于箱子占用槽数，原版会对可堆叠物品随机分堆。直接放置命令绕过自然分布限制，用于检查结构本身。

再定位自然生成的遗迹：

```mcfunction
/locate structure mathmaster:ruins/unfinished_overworld_gateway
/locate structure mathmaster:ruins/unfinished_overworld_giant_gateway
```

点击返回的坐标前往，检查地形衔接和箱子战利品。自然生成应在新世界或尚未生成的区块测试；已有区块不会因为资源更新而补生成结构。世界生成注册数据更新后应重启世界，单独 `/reload` 不足以完成测试。

## 当前群系范围

未完成主世界传送门普通/巨型共用以下21种群系，40/15、50%候选通过与19:1权重不变：

`minecraft:plains`, `minecraft:sunflower_plains`, `minecraft:snowy_plains`, `minecraft:savanna`, `minecraft:savanna_plateau`, `minecraft:forest`, `minecraft:flower_forest`, `minecraft:birch_forest`, `minecraft:old_growth_birch_forest`, `minecraft:dark_forest`, `minecraft:cherry_grove`, `minecraft:taiga`, `minecraft:snowy_taiga`, `minecraft:old_growth_pine_taiga`, `minecraft:old_growth_spruce_taiga`, `minecraft:meadow`, `minecraft:jungle`, `minecraft:sparse_jungle`, `minecraft:bamboo_jungle`, `minecraft:swamp`, `minecraft:mangrove_swamp`。

新增N类试炼独立使用trials分类与48/18、50%候选通过，说明见[n类试炼](n-trial.md)。
