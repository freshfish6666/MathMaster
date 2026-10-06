# 几何圣所

结构与模板ID均为 `mathmaster:trials/geometry_sanctuary`。使用用户提供的 `MathMaster-resource/Sructure/Trials/geometry_sanctuary.nbt`，45×20×40格，项目副本逐字节保留原模板。方块坐标、箱子内容/战利品表、几何祭坛及三个保存的实体均不改写。

## 生成

- 仅主世界的平原、向日葵平原、热带草原、热带高原、沙漠、积雪平原；群系标签 `mathmaster:has_structure/geometry_sanctuary`。
- 独立结构集 `mathmaster:geometry_sanctuaries`：64区块网格、separation 24、75%候选通过、独立salt 184735117。候选通过率由50%提高到75%，补偿避让损失；未考虑避让/群系/地形筛选时，候选密度为48/18/50%的N类试炼的84.375%；不是固定1024格必有一座。
- 使用原版 `exclusion_zone` 避开 `minecraft:villages`：圣所起点区块的XZ各±12区块（约192格）内存在村庄候选起点就跳过。直接按种子预测，不依赖谁先生成，不读取或加载邻区块。沿用原版保守规则，即使村庄候选最终因群系等原因不生成也会排除。仅针对村庄，不增加通用结构扫描或自定义避让代码；其他结构仍可能偶尔重叠。
- 等概率四向旋转，中心位于候选区块中心附近。按旋转后外接矩形的中心/四角/四边中点取九个预测噪声地形柱；九个地表最高与最低相差≤6格，任意采样地表有液体或非实块则放弃。
- 原模板底层放在九点地面实块高度的中位数上，相当于在第一格空气下嵌入一格；原版 `beard_thin` 负责附近地形适配。允许局部悬空/嵌入，不调整模板几何，不扩大基础，不强制加载邻区块。九点采样不保证采样点之间没有细小水坑或山脊；后续地形装饰也可能影响外观。
- 自定义结构类型 `mathmaster:geometry_sanctuary` 与自定义pool element `mathmaster:geometry_sanctuary_pool_element`，使用原版 `PoolElementStructurePiece` 保存/分区块放置。其他结构的频率、战利品与Boss战斗不变。

## 小怪与召唤

模板中DATA名称 `mathmaster:geometry_construct` 的标记位于本地[5,3,35]、[41,3,34]。每个标记在结构放置时生成一只几何构造体，脚部Y与标记相同，XZ居中；随整座旋转。只处理当前放置区块边界内的标记，避免跨区块重复生成。原版忽略结构方块的处理器不会吞掉此标记；最终位置为空气，不留下结构方块。

两只小怪持久化，不因走远普通消失，击杀后不重刷，和平难度沿用敌对生物移除规则；原有属性、激光与固定掉落一个几何核心均不变。生成沿用NeoForge生成事件，其他模组仍可取消。未知DATA名称不会生成实体。

场地不直接生成Boss。玩家使用获得的核心右键模板内原有几何祭坛，沿用既有消耗、取消返还及4秒召唤规则。模板中的四个奖励箱保留 `mathmaster:chests/n_altar_ruins`，四个有固定物品的箱子与其余空箱原样保留，暂不新增圣所专属奖励表。N祭坛血祭奖励现必出一张几何圣所地图，使用 `#mathmaster:on_geometry_sanctuary_maps` 与原版探索地图机制指向本场地，规则见 `docs/n-altar.md`。

## 查找与测试

```mcfunction
/locate structure mathmaster:trials/geometry_sanctuary
/place structure mathmaster:trials/geometry_sanctuary
```

自然生成只影响新区域，已有重叠建筑不会自动修复。`/place structure`执行结构逻辑和数据标记，仍需地形通过筛选，但手动放置不会执行结构集的村庄避让；单纯使用结构方块加载或 `/place template`只放模板，不执行本结构的小怪标记处理。

`GeometrySanctuaryStructureCheck`已纳入 `tools/checks/gateway-check.gradle` 的隔离服务器：动态注册/六群系/64与24/75%筛选、原版村庄12区块避让及正负坐标/placement codec、结构与组件codec、九点地形6/7格边界/水面/建筑高度、四向分区块放置、两小怪唯一生成/位置/持久化、箱子原物品与原奖励表、祭坛、未知/界外标记、真实地形及自然locate和附近无村庄候选验证。使用新的build隔离世界，不打开run用户存档；结束后普通强制编译移除自动测试class，具体命令见 `tools/README.md`。

当前233项检查及原主世界传送门、N类试炼、下界传送门隔离回归全部PASS；JSON引用校验通过。

自然场地的地形融合、三份保存实体的显示、召唤和战斗观感待客户端实机验收。
