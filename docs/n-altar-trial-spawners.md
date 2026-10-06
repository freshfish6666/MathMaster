# N祭坛遗迹试炼刷怪笼（Minecraft 1.21.1）

四个刷怪笼分别为僵尸、骷髅、数字9、数字8，每个总共5只、同时最多1只；多人不增加数量。生成半径3格、生成间隔40 tick、玩家检测半径14格、完成后冷却36000 tick（30分钟）。完成奖励列表为空，怪物自身掉落保留。普通配置会被1.21.1方块实体继承为不祥配置，两种模式都保持上述数量与无完成奖励；不祥转换仍可能重置本轮，这是原版机制。

## 领取并手动放置

创造模式并拥有管理员权限，先聊天执行 `/give @s minecraft:command_block`。放一个脉冲命令方块，将下面一条 `give @p ...` 原样粘入（命令方块里不加开头斜杠），用按钮启动一次领取。四条分别执行，然后把领取的四个物品放到遗迹选定位置；长命令超过聊天输入上限，因此使用命令方块。命令方块只用于领取，领完可拆除，不需要保存进遗迹。

不要用普通创造栏刷怪笼替代这些带配置的物品，也不要在配置后用刷怪蛋改变类型。

## 僵尸

```mcfunction
give @p minecraft:trial_spawner[block_entity_data={id:"minecraft:trial_spawner",normal_config:{spawn_range:3,total_mobs:5.0f,simultaneous_mobs:1.0f,total_mobs_added_per_player:0.0f,simultaneous_mobs_added_per_player:0.0f,ticks_between_spawn:40,spawn_potentials:[{weight:1,data:{entity:{id:"minecraft:zombie"}}}],loot_tables_to_eject:[]},required_player_range:14,target_cooldown_length:36000}] 1
```

## 骷髅

```mcfunction
give @p minecraft:trial_spawner[block_entity_data={id:"minecraft:trial_spawner",normal_config:{spawn_range:3,total_mobs:5.0f,simultaneous_mobs:1.0f,total_mobs_added_per_player:0.0f,simultaneous_mobs_added_per_player:0.0f,ticks_between_spawn:40,spawn_potentials:[{weight:1,data:{entity:{id:"minecraft:skeleton"}}}],loot_tables_to_eject:[]},required_player_range:14,target_cooldown_length:36000}] 1
```

## 数字9

```mcfunction
give @p minecraft:trial_spawner[block_entity_data={id:"minecraft:trial_spawner",normal_config:{spawn_range:3,total_mobs:5.0f,simultaneous_mobs:1.0f,total_mobs_added_per_player:0.0f,simultaneous_mobs_added_per_player:0.0f,ticks_between_spawn:40,spawn_potentials:[{weight:1,data:{entity:{id:"mathmaster:nine"}}}],loot_tables_to_eject:[]},required_player_range:14,target_cooldown_length:36000}] 1
```

## 数字8

```mcfunction
give @p minecraft:trial_spawner[block_entity_data={id:"minecraft:trial_spawner",normal_config:{spawn_range:3,total_mobs:5.0f,simultaneous_mobs:1.0f,total_mobs_added_per_player:0.0f,simultaneous_mobs_added_per_player:0.0f,ticks_between_spawn:40,spawn_potentials:[{weight:1,data:{entity:{id:"mathmaster:eight"}}}],loot_tables_to_eject:[]},required_player_range:14,target_cooldown_length:36000}] 1
```

## 放置、保存与试玩

- 四个检测范围共同覆盖入口；检测玩家还需视线，别把刷怪笼封在实心墙后。生成点需要空间且能从刷怪笼看到，四组生成区域尽量不重叠。
- 数字8至少预留其完整身体所需空间。试炼生成数字8/9不要求皮亚诺公理和低光照；主世界与非和平难度限制仍保留，自然生成条件不变。
- N祭坛下方放灵虚块才能计数，先准备底座再进入战斗。场地和怪物死亡位置须处于祭坛下半中心的10格球形范围内。皮亚诺等额外生成的合格怪物死亡同样计入20次。
- 建造及保存模板用创造模式，试炼不检测创造/旁观玩家。在首次生存试玩之前，用结构方块保存包含四个刷怪笼的干净模板；方块实体配置会随结构保存，刷怪笼已完成/冷却的状态也会保存，所以不要把试玩后的刷怪笼作为初始模板。
- 生存试玩需非和平难度且 `doMobSpawning=true`。失败或重新测试时，重新放置四个配置物品（或加载干净模板），并重新放置祭坛灵虚底座重置血祭；刷怪笼结束与祭坛重开互不联动。
- 修改已有世界实际使用的 `mathmaster-server.toml`，在 `[n_altar]` 下设置 `required_offerings = 20`；旧配置15不会随代码默认值自动迁移。开发环境通常为 `run/config/mathmaster-server.toml`，若世界有专属 `serverconfig/mathmaster-server.toml` 则检查实际加载文件。退出世界后修改，重新进入生效，不删除配置或存档。

四个笼子固定生成20只不代表祭坛只接受这20只；额外怪物贡献可让祭坛提前完成，这是用户接受的规则。刷怪笼完成奖励关闭不影响N祭坛的奖励箱或怪物自身掉落。
