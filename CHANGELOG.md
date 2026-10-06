# MathMaster 更新日志

## 1.3.0 — 2026-10-06

### 中文

本次更新补全了从数学学习、遗迹探索到首个Boss挑战的主世界冒险流程。

#### 探索与试炼

- 新增N类试炼和几何圣所，扩充主世界未完成传送门，并加入下界未完成传送门。
- 各类遗迹使用独立战利品表，提供材料、装备、笔记和探索地图。
- 主世界未完成传送门的箱子有概率出现N类试炼地图；完成N祭坛血祭后，必定获得一张几何圣所地图和2～3个灵虚锭，另有随机奖励。

#### N祭坛

- 新增血祭与正常献祭两种模式。血祭默认需要20点贡献，附近玩家的平均有效IQ越高，怪物获得的增益越少，并逐步转为减益。
- 本人有效IQ大于50，且使用智识之镜成功洞悉至少5种生物后，永久解锁正常献祭。
- 求知之物：献上铁锭、金锭、绿宝石或钻石，有概率获得低级神谕。
- 合意之物：献上考拉兹果实获得智识经验；献上素数核心可指引附近未完成主世界传送门。
- 亵渎之物：献上特定物品会受到雷击、失明等随机惩罚。
- 加入专属血祭音乐，以及范围渐变、完整循环和仪式结束尾声。

#### 几何持有者

- 新增首个Boss“几何持有者”，具有200点生命、20点护甲，生命低于40%时进入第二阶段。
- 拥有光环震荡、画地为牢、蓄力光线、炸弹抛掷、十字波和天地为牢六种技能。近战、远程与大招独立释放，第二阶段增强部分技能并提高释放频率。
- 加入发光模型、阶段颜色渐变、专属Boss血条、技能音效和两阶段战斗音乐，支持转阶段与击败后的音乐平滑过渡。
- 新增使用激光攻击的几何构造体，击杀必定掉落一个几何核心。
- 使用几何核心右键几何祭坛可开启召唤；再次右键取消并返还材料。祭坛支持重复挑战，生存模式不可破坏。
- 击败几何持有者掉落2～3个素锭。

#### 公理与材料

- 新增“测地线”：2～5级几何类主动公理，提供短距离瞬移和沿视线飞行。
- 新增“回归”：2～5级逻辑类主动公理，记录维度与坐标锚点，蓄力后返回，可在同一维度使用。
- 新增“素数连击”：2～5级数论类主动公理，消耗素数核心，按素数序列递增近战额外伤害。
- 新增“香农熵”：1～5级混沌类被动公理，使攻击与承伤产生随机倍率。
- 新增素锭、素粒、素块及相互合成配方。
- 新增红、紫、蓝三种考拉兹树及木材系列、果实。树苗附有红色种植安全提示。
- 新增数字生物5的4×4画作，可在创造模式获取，不参与随机画作放置。

#### 调整与优化

- 新增数字污染水及可按维度配置的环境污染等级；穿戴完整灵虚套装可降低一档有效污染效果等级。
- 数字生物6～9的攻击有概率增加玩家污染。
- 灵虚五种工具的基础攻击力调整为对应下界合金工具加2。
- 皮亚诺公理补充击杀数字7后生成数字8的效果。
- 改善几何持有者的悬浮移动、转向和空档追近，修复高度受阻时停止攻击的问题。
- 修复使用回归跨维度时，满污染未正常致死的问题。
- 优化洞悉界面的状态更新，简化公理物品说明和JEI描述；加入Jade信息框对Boss血条的避让。
- 更新玩法文档与主世界流程说明。

Curios仍为客户端和服务端必需前置；Jade、JEI、MadnessCore、MCphone和Touhou Little Maid仍为可选联动。保持旧存档兼容。新增自然生成结构需要探索新区域。

### English

- Completed the Overworld exploration route with N Trials and Geometry Sanctuaries, expanded unfinished Overworld gateways, and added unfinished Nether gateways. Structures have separate loot tables. Gateway loot can include N Trial maps; N altar blood-sacrifice rewards guarantee a Geometry Sanctuary map and 2–3 Lingxu Ingots.
- Implemented N altar blood sacrifices and normal offerings. Blood sacrifices require 20 contributions by default, with nearby players' average effective IQ determining creature buffs. Effective IQ above 50 and successful insight into at least five creature types permanently unlock normal offerings. Knowledge offerings draw from an extensible low-tier oracle pool; favored offerings grant Intellect experience or gateway guidance, while profane offerings trigger random punishments.
- Added the first boss, Geometry Holder, with 200 health, 20 armor, and a second phase below 40% health. Independently scheduled melee, ranged, and ultimate skills include Halo Shock, Geometric Prison, Charged Beam, Cube Bombs, Cross Waves, and Heaven-and-Earth Prison. The encounter includes an emissive model, gradual phase coloration, a custom boss bar, dedicated sounds, and two-phase music. Defeating it drops 2–3 Prime Ingots.
- Added Geometry Constructs, Geometry Cores, and the Geometry Altar. Constructs attack with lasers and always drop one core. Right-clicking the altar consumes a core to summon the boss; another right-click cancels the ritual and refunds it. Repeat challenges are supported, and the altar cannot be broken in Survival.
- Added Geodesic (Geometry, active, levels 2–5), Return (Logic, active, levels 2–5), Prime Combo (Number Theory, active, levels 2–5), and Shannon Entropy (Chaos, passive, levels 1–5), providing teleportation and flight, dimension-coordinate anchors, prime-sequence melee bonuses, and randomized damage multipliers.
- Added Prime Ingots, Nuggets, Blocks, and conversion recipes; red, purple, and blue Collatz trees with complete wood families, fruit, and sapling warnings. Added a 4×4 vanilla Number 5 painting, available in Creative and excluded from random painting placement.
- Added Digitally Polluted Water, configurable dimension-based environmental pollution tiers, and a full Lingxu armor set bonus that lowers environmental pollution tiers. Numbers 6–9 can inflict pollution on attack. Increased each Lingxu tool's base attack damage to its Netherite counterpart plus two and extended Peano's number conversion from 7 to 8.
- Fixed boss attacks stalling when vertical movement is blocked, improved pursuit and movement between casts, and fixed lethal full pollution being lost during Return's dimension travel. Improved state-based insight UI updates, shortened axiom and JEI descriptions, and revised the 1.3.0 gameplay documentation.

Curios remains required on both sides. Jade, JEI, MadnessCore, MCphone, and Touhou Little Maid remain optional. Existing registry IDs, network encoding, and save compatibility are preserved.

发布产物 / Release artifact: `build/libs/mathmaster-1.3.0.jar`
SHA-256: `0C13DE43F772A25363CF51B3A0365C8346DB90F978DC8169E87A0BF9D79BDA24`

## 1.2.2 — 2026-10-01

### 中文

- 新增“被数字污染的方块”：具有幽匿块风格的属性，站在其上的玩家和有智识生物会获得短暂数字污染效果，离开即解除；挖掘时有1%概率额外掉落灵虚粒，时运可提高概率。
- 有智识的非玩家生物开始自然净化污染，连续无污染效果的时间越长，净化速度越快；数字污染效果生效或污染增加时重新计时。
- 新增逻辑类5级主动公理“对合”：与智识更低的非Boss生物交换外观，并获得其原版旁观视野；双方控制、属性和AI保持原状，可正常使用物品、方块和其他技能。该技能保留实验警告。
- 实装2～4级逻辑类被动公理“数学归纳法”：把其他同匣逻辑公理的有效等级提升至至少自身等级+1，不降低更高等级、不提升自身，多份只取最高等级。当前固定5级的对合不受提升。
- 新增仅创造模式获取的“数理之戒”，佩戴于Curios戒指槽位后使用公理无冷却。
- 新增巨大数字生物5的实验原型：固定位置的血肉平台，可承托生物并浮于水面；5刷怪蛋标注“此物品仍在研制中”，暂不自然生成。
- 重绘灵虚套、公理匣、神谕匣、真理匣和数字化污染计量仪的物品栏纹理，统一为更接近原版的16×16像素风；灵虚护甲穿戴纹理保持不变。
- 创造栏中的数字生物刷怪蛋按5、6、7、8、9排列。
- 修复玩家清醒时污染每分钟意外降低10点：主世界日常自然净化恢复为每分钟1点，成功睡过夜跳过的时间仍按每分钟10点净化。

Curios仍为双端必需前置；Jade、JEI、MadnessCore、MCphone和Touhou Little Maid仍为可选联动。

### English

- Added the Digitally Corrupted Block with sculk-like properties. Players and intelligent creatures standing on it receive a brief Digital Pollution effect, which ends when they leave. Mining has a 1% chance to drop an extra Lingxu Nugget, increased by Fortune.
- Intelligent non-player creatures now naturally purify pollution at an increasing rate while free of the Digital Pollution effect. Receiving pollution or having the effect resets the purification timer.
- Added Involution, a level-5 active Logic axiom: exchange appearances with a non-boss creature of lower Intellect and gain its vanilla spectator vision. Both retain their controls, attributes, and AI, with normal item, block, and skill use. The experimental warning remains.
- Implemented Mathematical Induction, a level-2–4 passive Logic axiom. Other Logic axioms in the same case have an effective level of at least its level + 1. Higher levels are retained; it cannot boost itself, and only the highest copy applies. The current level-5 Involution receives no boost.
- Added the creative-only Ring of Mathematical Principles. Wearing it in a Curios ring slot removes axiom cooldowns.
- Added an experimental giant Number 5: a stationary flesh platform that supports creatures and floats on water. Its spawn egg is marked as still in development; it does not spawn naturally.
- Redrew the inventory icons for Lingxu armor, the three axiom cases, and the Digital Pollution Meter in a vanilla-style 16×16 pixel palette. Worn armor textures are unchanged.
- Ordered number-creature spawn eggs as 5, 6, 7, 8, 9 in the creative tab.
- Fixed pollution unexpectedly dropping by 10 every minute while awake. Normal Overworld purification is restored to 1 point per minute; time skipped by successful sleep still purifies 10 points per minute.

Curios remains required on both sides. Jade, JEI, MadnessCore, MCphone, and Touhou Little Maid remain optional.

发布产物 / Release artifact: `build/libs/mathmaster-1.2.2.jar`
SHA-256: `FAAEA3DA9F334739F817C0829937047067F6FD6032743867C488B0DB92856CCE`

## 1.2.1 — 2026-09-21

### 中文

#### 新内容

- 新增数字生物 7 和 6，以及各自的模型、纹理、声音和刷怪蛋。7 穿戴钻石风格护甲；6 是体型更大的血肉化失败产物，攻击可暂时禁用盾牌，死亡必定掉落受抢夺影响的灵虚锭。
- 新增几何类主动技能“普莱费尔公理”：指定有智识生物作为基点，沿选定轴线行动时可阻止其接近和伤害玩家；淡石英色粒子提示可行动方向与边界。
- 新增可装备在 Curios `trinkets` 槽位的“数字化污染计量仪”及配方，屏幕右下角实时显示污染值。
- 新增会切换图像的“集合”与独立的“空集”物品。
- 全部 1228 道内置数学书和洞悉题目提供英文版本；答题界面可即时切换中英文，同一道题共用题目 ID、答案与完成进度。
- 数学手册加入简短的“研读之后”引导。
- 数字污染较高时出现像素风扭曲符文、乱码文本和多语种低语；曾制作的血肉插图资源保留，但污染效果不会显示它。

#### 修复与调整

- 修复数字 7 被攻击击退后可能陷入沙块等方块的问题，并将其刷怪蛋名称统一为“7刷怪蛋”。
- 主世界每累计 1 分钟降低 10 点数字污染；成功睡过夜时，跳过的时间也计入净化。
- 清理未使用的构建模板配置与少量冗余代码，保留现有注册 ID 和存档数据格式。

Curios 仍是客户端和服务端必需前置；Jade、JEI、MadnessCore、MCphone 和 Touhou Little Maid 仍是可选联动。

#### 发布产物

- 文件：`build/libs/mathmaster-1.2.1.jar`
- SHA-256：`1D76C38A3FE2DC6D039A8A781EF816B8E88C715169552BC44DD594C24D6902D5`

### English

#### New content

- Added Number 7 and Number 6, each with its own model, textures, sounds, and spawn egg. Number 7 has diamond-themed armor; Number 6 is a larger, flesh-like failed transformation that can temporarily disable shields and always drops a Lingxu Ingot on death, with the amount affected by Looting.
- Added Playfair's Axiom, an active geometry skill. Designate a creature with intellect as the base point, then move along a chosen axis to keep it from approaching or harming you. Pale quartz particles show the available directions and boundaries.
- Added the Digital Pollution Meter and its crafting recipe. Equip it in the Curios `trinkets` slot to see your pollution level in a pixel-style HUD.
- Added Set, an item with a changing image, and Empty Set as a separate item.
- Added English translations for all 1,228 built-in math-book and insight questions. You can switch between Chinese and English in the quiz screens; both languages use the same question IDs, answers, and completion progress.
- Added a short “After Studying” introduction to the MathMaster guide.
- High digital pollution now brings distorted pixel-style symbols, garbled text, and multilingual whispers. The experimental flesh-overlay image remains in the resources but is not shown by pollution effects.

#### Fixes and adjustments

- Fixed Number 7 sometimes being knocked into sand or other blocks. Its Chinese spawn egg name is now consistently “7刷怪蛋”.
- Digital pollution decreases by 10 points for each accumulated minute in the Overworld. Time skipped by a successful night's sleep also counts toward this reduction.
- Removed unused build-template configuration and minor redundant code while retaining existing registry IDs and save-data formats.

Curios remains required on both client and server. Jade, JEI, MadnessCore, MCphone, and Touhou Little Maid remain optional integrations.

#### Release artifact

- File: `build/libs/mathmaster-1.2.1.jar`
- SHA-256: `1D76C38A3FE2DC6D039A8A781EF816B8E88C715169552BC44DD594C24D6902D5`

## 1.2.0 — 2026-09-13

本版本将 MathMaster 从数学问答模组扩展为以数学公理、智识与数字化污染为核心的冒险玩法模组。Curios 自本版本起成为客户端与服务端均必需的前置。

### 公理与笔记

- 完成五本数学书后，可在手册中领取对应的 1～5 级研读笔记；领取冷却时间等于书籍难度秒数。
- 新增“公理演绎台”。使用笔记与红石、铁锭、下界石英、末影珍珠或灵虚锭，可以演绎算术、数论、逻辑、几何和混沌五类公理。
- 新增公理匣、神谕匣和真理之匣，分别容纳 3、6、9 份公理笔记，并通过 Curios 的唯一“公理匣”槽位生效。
- 新增固定五扇区的主动技能轮盘。默认按住 X 选择主动公理，按 C 使用；同一匣子中每个类别最多装备一个主动技能。
- 加入公理等级、分类颜色、演绎台滚动列表、笔记标签和具体技能数值说明；新增独立“数学笔记”创造模式标签页。
- 新增 JEI 可选联动“公理演绎”类别，可查询各公理的笔记等级、材料、技能说明和具体数值。

### 正式公理技能

- 皮亚诺公理：允许数字 8 与 9 在主世界自然生成，概率随公理等级和玩家数字污染值提高。
- 加法单位元：使数字污染值最低为 1，并周期性生成能够抵消污染增加的单位元护盾。
- 加法交换律：依次选择两个符合条件的有智识生物，交换双方的当前生命值与最大生命值。
- 加法逆元：永久连接两个同类生物；非友好生物会互相攻击，任意一方死亡时另一方也会死亡。
- 欧几里得素数无穷定理：为范围内有智识生物施加素数标记；玩家击杀后生成带下一素数标记的同类，到期时受到标记值伤害。
- 算术基本定理：玩家击杀未被素数标记的有智识生物时，按等级概率获得“素数核心”，数量由生物智识的素因子数量决定。

### 灵虚、数字生物与污染

- 新增灵虚头盔、胸甲、护腿和靴子，耐久、护甲与韧性约为对应下界合金装备的 1.5 倍；装备防火，穿戴可提供心流，完整套装额外提供 1 级心流。
- 新增“精神暴走”：每 5 级心流获得 1 级；每级提高 20% 攻击力与 10% 攻击速度，同时降低 10% 移动速度。
- 新增数字生物“8”，拥有铁甲风格模型、25 点生命、15 点护甲和随难度变化的攻击力；AI 与数字 9 保持一致。
- 为数字 8 与 9 加入闲逛、激怒、攻击、受伤和死亡音效，共 22 个声音文件。
- 新增每位玩家和有智识生物独立保存的“数字化污染”。污染达到 100 时受到 kill 伤害；玩家使用随机专用死亡信息，非玩家生物死亡后生成数字 9。
- 新增“数字污染”状态效果及药水。效果按智识持续积累污染；灵虚粒可将粗制药水酿成数字化污染药水。
- 新增“素数核心”，以 2～29 的十个素数和短暂乱码帧循环播放动画。

### 界面与联动

- 智识之镜可以对空气或方块持续使用 2 秒进行自我洞悉，查看玩家名称、UUID、当前智识、升级经验和数字污染值。
- 新增 MadnessCore 可选联动：安装后，开始答题会消耗书籍难度一半的精神值；精神值不足时禁止继续答题。完成指定数学书可获得对应位阶贡献。
- Jade 生物信息可显示素数标记；不安装 Jade、JEI、MadnessCore、MCphone 或 Touhou Little Maid 时，不会加载对应可选联动。

### 修复与改进

- 修复自我洞悉界面和公理技能轮盘错误启用背景模糊的问题。
- 修复公理演绎台界面物品栏背景过暗、物品悬停提示缺失及候选公理空间不足的问题。
- 调整皮亚诺公理的数字生物刷新权重与主世界群系覆盖范围。

发布产物：`build/libs/mathmaster-1.2.0.jar`
SHA-256：`E619025292A6A96D1AE76F7AD990FE1A896897A9331025D3BEFFB49369A1B8E6`

## 1.1.1 — 2026-09-06

本版本完善生物智识与跨模组扩展能力，加入讲台答题、可选联动、新生物“9”和学士帽。所有洞悉状态、玩家数据、答题判定及奖励仍以服务端为权威。

### 主要功能

- 新增公共 API v1 `com.freshfish.mathmaster.api.MathMasterApi`，提供生物与玩家智识查询、服务端洞悉状态、不可变数学书目录，以及受控的服务端答题启动入口。
- 新增数学书公共标签 `mathmaster:quiz_books`。五本数学书可放入雕纹书架和讲台；其他模组或数据包也可通过该标签声明兼容书籍。
- 新增生物“9”：拥有 20 点生命、7 点护甲、独立模型与窄碰撞箱；平时四处观察并偶尔凝视玩家，被玩家攻击后会号召 32 格内同类反击。其伤害随难度变化，死亡时有受抢夺影响的概率掉落灵虚粒。
- 新增装备“学士帽”：可戴入头盔栏，提供 2 点护甲和 365 点耐久；佩戴时增加 10 点临时有效智识，并保持 200 上限。

### 联动与扩展

- 新增 Jade 可选联动，仅额外显示“生物智识”和“洞悉状态”两项，显示数据由服务端生成。
- 新增 MCphone 可选联动“学习通”。玩家可在手机上选择“数学大师”科目和数学书开始答题；书目、抽题、判分与奖励均由服务端处理。
- 新增 Touhou Little Maid 可选联动“学习/答题”工作模式。女仆会寻找放有数学书的讲台自主答题，拥有独立持久化 IQ，并按书籍难度和 IQ 决定答题速度与正确率；奖励优先放入背包，错误惩罚沿用玩家规则但不会触发雷击。
- 生物智识 JSON 新增可选 `required_mod` 字段，并可通过 `external_entity_intellect` 配置统一控制非原版命名空间定义；缺少目标模组时仅跳过当前定义，不会删除此前已加载的有效定义。
- 新增 `docs/integration-contract.json`、`docs/integrations.md` 和精简的跨模组契约发现规则，联动方无需阅读 MathMaster 内部实现。

### 改进与修复

- 智识之镜在准星离开目标后不再立即清空进度，而是按相同速度回退；重新指向同种生物可继续积累，切换生物类型则重新开始。
- 修复数学书放上讲台后玩家无法正常打开的问题；潜行空手右击可取书，潜行手持另一本文数学书可替换，背包已满时旧书会掉在脚边。
- 女仆答题气泡保留旋转书籍图标，并根据讲台上的实际数学书显示对应封面；寻路停止距离缩短至约一格。
- 学士帽、MCphone 和生物“9”的客户端模型或界面代码均保持客户端隔离；Jade、MCphone 与 Touhou Little Maid 只作为可选依赖，不打包进 MathMaster。

发布产物：`build/libs/mathmaster-1.1.1.jar`
SHA-256：`B6266137A43FD70ED992868C1E0E60548E0106EDA7A40C8D95AF29A6474E03B6`

## 1.1.0 — 2026-09-03

本版本扩展数学书、智识与灵虚系列玩法，并保持客户端与服务端均需安装 MathMaster。

### 主要功能

- 新增统一的“数学大师手册”。手持任意数学书右击即可查看教程、当前书完成度及题目掌握情况；答对过的题目显示题干和正确答案，未掌握题目显示为 `???`。
- 新增智识之镜洞悉玩法。长按右击配置过智识的生物可进入多题挑战，题量和难度由生物智识决定；全部答对才会记录为成功洞悉，并可获得受目标智识上限约束的智力经验。
- 新增生物智识成就、蝙蝠隐藏挑战，以及僵尸、村民、末影龙、凋零等原版生物的默认智识定义。
- 新增智识领先近战增伤。领先区间提供 5%～40% 的减伤前乘算加成，可通过服务端配置调整或关闭。
- 新增“心流”效果：每级提供 5 点临时智识。主手、副手持灵虚工具分别提供一级；五件工具同步提高耐久和攻击速度，并补充灵虚块、灵虚粒及相关配方。
- 扩充并校正五本数学书与五个洞悉难度池的题库；正式题库总数为 48、81、196、250、36。

### 兼容与扩展

- 生物智识、洞悉门槛和题库选择改为服务端权威判定；整合包可用 `data/<命名空间>/entity_intellect/*.json` 为其他模组生物设置智识。
- 智识近战增伤兼容 `minecraft:is_player_attack` 伤害标签，并提供 `mathmaster:no_intellect_damage_bonus` 排除标签。
- 代码可通过 `EntityIntellectManager.get(entity)` 查询生物有效智识，通过 `IntelligenceManager.getEffectiveIq(player)` 查询玩家有效 IQ。

### 修复

- 修复智识差超过 30 时仍显示洞悉进度、末影龙部位无法触发洞悉，以及若干题目的等值干扰项、重复语义和错误答案。

发布产物：`build/libs/mathmaster-1.1.0.jar`
SHA-256：`FF49ACAF2CD84AF64D2DE478C55027999DC9576CC2A2032D12DA8DEFD60745FB`

## 1.0.2 — 2026-09-02

优化答题流程，并加入题目掌握记录、灵虚系列物品及全新的数学书纹理。

- 答题后不再自动退出界面；正确或错误选项会显示对勾或红叉，并可选择“下一题”或“退出”。
- 增加答题音效反馈，并会在玩家远离或破坏触发答题的书架后自动关闭界面。
- 为题目加入唯一 ID、答对记录、近期题目排除和掌握度权重，降低重复抽到已熟悉题目的概率。
- 数学书名称后会显示玩家已答对过的题目数与题库总数，并为完成每本书的全部题目加入进度成就。
- 调整数学书名称、物品介绍和配方解锁条件；启蒙与最高阶手册默认解锁，其余手册随 IQ 提升解锁。
- 新增“灵虚之镜”，持有时会在动作栏显示当前 IQ 与经验进度。
- 新增灵虚锭，以及灵虚剑、灵虚镐、灵虚斧、灵虚锹和灵虚锄；灵虚锭已加入最高级答题奖励。
- 重新绘制五本数学书、灵虚之镜和灵虚工具的纹理，使其更贴近 Minecraft 原版像素风格并保留各自特色。
- 将模组版本更新为 1.0.2。

发布产物：`build/libs/mathmaster-1.0.2.jar`
SHA-256：`E5D9A27343A0E950AE49804EE8E5C1EC57176DAB63EA93C9D420019A969438CC`

## 1.0.1 — 2026-09-01

本版本记录了从项目交由协作修改开始，到 MathMaster 1.0.1 构建完成为止的全部主要更新。

### 新增

- 新增 JSON 数据化题库。五类数学书的题目已从 Java 源码迁移至 `data/<命名空间>/quiz_banks/`，支持通过数据包追加或覆盖题目，无需修改源码或重新编译模组。
- 新增题库热重载支持。安装或修改数据包后可通过 `/reload` 重新加载题库。
- 新增题库数据校验。自动跳过题干为空、答案数量错误、答案重复或题干重复的无效题目，并在服务端日志中记录原因。
- 新增题库扩展文档，说明文件路径、JSON 格式、覆盖规则和校验规则。
- 新增答题界面的长文本自动换行。题目和答案会根据可用宽度分行显示，过长内容会以省略号收尾。
- 新增答案悬停提示。将鼠标停留在答案按钮上时，会显示未经截断的完整答案。
- 新增答题窗口缩放适配。界面会根据游戏窗口尺寸动态调整面板、题目区域、答案按钮和经验条布局。
- 新增数学符号字体兼容检查。答题界面会检测当前字体能否显示题目和答案中的数学符号；发现缺失字形时会在界面中提示，并写入日志。
- 新增最高奖励档每日领取限制。每位玩家每个 Minecraft 游戏日最多获得 10 次最高档物品奖励。
- 达到最高奖励档的每日上限后，玩家仍可继续答题并获得智力经验，但不再获得物品奖励，同时会在聊天框收到提醒。
- 新增最高奖励领取次数的玩家数据存储，并支持死亡后保留及跨存档读写。
- 在“小学二年级数学”的物品提示中新增答题书架线索，引导玩家将普通书架紧挨雕纹书架摆放并右击普通书架。

### 改进

- 重新设计除“高等数学习题册”外四本数学书的 16×16 纹理，使其更贴近 Minecraft 原版像素风格，并通过不同图案表达各自的难度与用途。
- 改进 IQ 成长模型，改为根据累计智力经验通过公式直接计算 IQ，避免逐级循环计算。
- 累计智力经验改用 `long` 存储，提高可表示范围并降低长期游玩时发生整数溢出的风险。
- 为 IQ 设置合理上限：初始 IQ 为 30～50，最高 IQ 为 200；累计经验和管理员命令也会遵守该范围。
- 增加旧版智力数据迁移逻辑，将原有的 IQ 与当前等级经验转换为新的累计经验格式，尽量保持已有存档进度。
- 改进答题界面的 IQ 显示，加入当前等级经验进度条，并处理达到最高 IQ 时的显示状态。
- 完善项目 README，补充模组玩法、Minecraft/NeoForge/Java 版本、答题书架搭建方法、管理员命令、配置项、题库扩展方式、源码构建方法和许可证说明。
- 将模组版本更新为 1.0.1，并生成经过完整 Gradle 构建验证的发布 JAR。

### 修复

- 修复较长题目或答案超出答题窗口、相互遮挡或难以阅读的问题。
- 修复窗口尺寸较小时固定布局可能超出屏幕的问题。
- 修复自定义字体缺少数学符号时无法及时发现、题目可能显示为缺字符号的问题。
- 修复累计经验使用较小整数类型时，长期积累可能溢出并导致 IQ 数据异常的问题。
- 修复最高奖励档可以在同一游戏日无限领取的问题，同时保留超过上限后继续答题的能力。

### 技术与兼容性

- 题库加载改为 NeoForge 服务端资源重载流程，兼容世界数据包和不同命名空间的扩展题库。
- 新的智力数据读取逻辑兼容旧存档字段，并对异常或超出范围的数值进行限制。
- 数学符号检查使用 Minecraft 当前字体的实际字形集进行判断，不依赖操作系统字体名称。
- 当前目标环境为 Minecraft 1.21.1、NeoForge 21.1.248、Java 21。

### 发布产物

- 文件：`build/libs/mathmaster-1.0.1.jar`
- SHA-256：`5A3765A00097EC450732027C47CF12C2CA705D07FA94789FB083386931FA7C7A`
- `libs/` 目录中的依赖 JAR 被视为只读文件，不属于版本更新或修改范围。
