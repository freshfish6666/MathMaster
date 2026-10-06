# N类试炼

结构ID、模板ID与模板池ID均为 `mathmaster:trials/n_trial`，结构集为 `mathmaster:n_trials`。用户原始文件为 `MathMaster-resource/Sructure/Trials/n_trial.nbt`，模板34×18×33格，直接采用用户重新保存实体并恢复灵虚底座后的最新版NBT，逐字节复制，保留盔甲架、画与其他实体数据，不再修改项目副本的底座或NBT，不做高度归一化。接入方式沿用未完成主世界传送门：原版jigsaw、rigid单模板、地表WORLD_SURFACE_WG投影、start_height=-1、beard_thin。随机四向旋转，无额外平坦度检查，接受局部悬空/埋入。

## 分布与群系

48×48区块（768×768格）每区一个随机候选点，spacing=48、separation=18、frequency=0.5，独立salt=184735071。与未完成主世界传送门独立生成，不绑定、不增加强制避让；群系或地形不满足时不生成。separation不是建筑之间保证的格数距离。原版数据生成仅作用于新生成区块，已探索区域不补生成、不定时重建。

17种适用群系：

`minecraft:plains`, `minecraft:sunflower_plains`, `minecraft:desert`, `minecraft:snowy_plains`, `minecraft:savanna`, `minecraft:savanna_plateau`, `minecraft:forest`, `minecraft:flower_forest`, `minecraft:birch_forest`, `minecraft:old_growth_birch_forest`, `minecraft:dark_forest`, `minecraft:cherry_grove`, `minecraft:taiga`, `minecraft:snowy_taiga`, `minecraft:old_growth_pine_taiga`, `minecraft:old_growth_spruce_taiga`, `minecraft:meadow`。

## 战斗与奖励

四个试炼刷怪笼分别为僵尸、骷髅、数字9、数字8，各5只、同时1只，多人不增怪，无完成奖励。保持模板原有激活状态和NBT；生成参数缺省值沿用原版（40 tick生成间隔、14格检测、30分钟冷却）。祭坛血祭门槛为20，允许公理额外怪物贡献，血祭BGM不变。

模板有6个独立N类遗迹补给箱（mathmaster:chests/n_altar_ruins，每箱4～6次、24项奖励、总权重239，完整数量与附魔规则见[n祭坛说明](n-altar.md#n类遗迹补给)），另有3个原样保留的空箱。祭坛底座已恢复为灵虚块，放置/自然生成后自动进入新的血祭轮次，初始贡献0；其余箱子保留。已有存档配置若仍为required_offerings=15，需要退出世界后改成20。

## 试玩

重启开发客户端/世界后执行：

```mcfunction
/place structure mathmaster:trials/n_trial
/locate structure mathmaster:trials/n_trial
```

测试请使用 `/place structure`；`/place template` 使用不同的邻接更新参数，可能移除双格祭坛，不作为完整试炼的放置方式。普通与巨型传送门原locate ID不变。结构外观/地形落点、四个刷怪笼视线及怪物生成空间、血祭范围需用户实机确认。自然生成检查使用build/gateway-check隔离世界，不操作run中的建造存档。

当前模板SHA256：`CA38AFBA8880979D6C12CBFA8D4E45C9470DEB5316940E7C5DCBAC5AAD4FCF47`。本次仅替换模板并核对复制一致，按用户要求未运行编译或服务器检查；此前结构检查PASS对应旧模板，最新版实体显示与结构放置待实机确认。
