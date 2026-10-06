# 开发工具

以下工具不参与普通编译或发布打包。命令均从项目根目录执行；运行生成器可能覆盖对应源码、纹理或声音，请先保存现有成果。

| 目录 | 用途 | 主要输出 |
|---|---|---|
| `models/` | Blender 数字生物与原生方块模型导出 | `dev-assets/models/`、对应 Java 模型/实体纹理、`build/*-export/` |
| `textures/` | 原创物品及方块像素纹理 | `src/main/resources/assets/mathmaster/textures/`、`build/` 预览 |
| `audio/` | 数字生物声音及污染低语 | 对应声音资源、`build/` 临时音源 |
| `quiz/` | 英文题目写入与翻译校验 | 写入脚本修改题库；校验脚本只读 |
| `checks/` | 显式启用的隔离服务器回归检查 | `build/seven-collision-check/`、`build/gateway-check/` |

## 模型与纹理

几何构造体生成器 `models/create_geometry_construct.py` 输出新小怪的128×128纹理/青色全亮层、11组件Mesh、可编辑 `dev-assets/geometry-construct/geometry_construct.bbmodel` 和模型JSON源，以及 `build/geometry-construct-preview.png` 软件预览。依赖Pillow/NumPy，命令使用上述本机Python路径；只写新Mesh几何，不覆盖手写动画Model、不修改Boss或概念图。`GeometryConstructCheck`已接入collision隔离检查，覆盖激光计时/躲避/墙体/普通伤害、移动和真实死亡固定掉落核心，结束后仍须普通强制编译移除自动检查class。规则与刷怪蛋指令见 `docs/geometry-construct.md`。

几何祭坛与核心使用 `models/create_geometry_altar.py`：输出新祭坛空槽/仪式状态JSON模型、三维核心物品模型和16×16纹理，保留批准概念图；只覆盖这两个新资产，不改N祭坛。模型源在 `dev-assets/geometry-holder/altar/geometry-altar-model-source.json`，软件预览为 `build/geometry-altar-preview.png`、`geometry-altar-summoning-preview.png`、`geometry-core-preview.png`。使用本机带Pillow/NumPy的Python运行；这是生成器，不是只读检查。

`GeometryAltarCheck`已加入下述collision隔离服务器检查，覆盖消耗/取消/组件返还、满背包、存档计时、重复召唤、空间/和平拒绝、取消生成、生存不可挖掘/空闲与仪式中真实爆炸保护、创造移除及暂存核心返还，当前42项断言通过。运行后仍须普通强制编译移除自动测试订阅器。玩法与领取指令见 `docs/geometry-altar.md`。

可编辑 Blender 文件集中在 `dev-assets/models/`。模型生成器会重新保存这些文件；既有 `.blend1` 是备份文件，保留在同一目录。

```powershell
blender --background --python tools/models/create_eight_blender.py
blender --background --python tools/models/create_seven_blender.py
blender --background --factory-startup --python tools/models/create_six_blender.py
blender --background --python tools/models/create_five_blender.py
blender --background --factory-startup --python tools/models/create_geometry_holder.py
python tools/textures/create_prime_core_texture.py
python tools/textures/create_prime_ingot_texture.py
python tools/textures/create_prime_material_textures.py
python tools/textures/create_polluted_water_textures.py
python tools/textures/create_collatz_tree_textures.py
python tools/textures/create_collatz_fruit_texture.py
python tools/textures/create_collatz_wood_textures.py
python tools/textures/create_collatz_color_variants.py
python tools/models/create_n_altar.py
```

素锭纹理生成器依赖Pillow，按用户选定A银白淡紫方案绘制原生16×16透明像素纹理与build/prime-ingot-preview.png；批准配色对照原图保存在dev-assets/prime-ingot，不裁切为生产纹理。素粒/块由create_prime_material_textures.py生成同色系纹理与build/prime-material-preview.png，不修改已接受素锭纹理。普通检查不重新生成。

污染水纹理生成器依赖Pillow，确定性生成静止/流动16帧动画和16×16桶图标，直接写入assets/mathmaster/textures；桶图标只改原版桶口液体，参考纹理位于dev-assets/textures/polluted-water；资源模型与双语名称由源码维护。普通检查不需要重新生成。

考拉兹树纹理生成器依赖Pillow，确定性生成红色款七张16×16（含树苗）原生像素纹理与build/collatz-red-texture-preview.png。批准的配色概念图保留在dev-assets/collatz-tree，不被裁剪成生产纹理。普通检查不重新执行生成器。果实生成器create_collatz_fruit_texture.py确定性绘制琥珀三瓣款16×16物品图标与build/collatz-fruit-preview.png，批准的概念原图保存在dev-assets/collatz-tree，生成器不修改概念图。

木材生成器create_collatz_wood_textures.py依赖Pillow，确定性绘制五张16×16红色木板/门/活板门相关纹理及build/collatz-wood-texture-preview.png，不修改概念原图。CollatzWoodFamilyCheck通过真实RecipeManager检查自有与原版木材配方、木炭烧制、燃料、双台阶/门掉落、原版栅栏连接及燃烧。

N祭坛生成器create_n_altar.py依赖Pillow和NumPy，生成28组件完整模型源、上下半与物品模型、六张16×16原生纹理和build/n-altar-preview.png、build/n-altar-blood-preview.png软件渲染，血祭下半模型附加透明符文满亮度层（neoforge_data）。原始概念保存在dev-assets/n-altar，不修改AI原图；预览不代替客户端显示验收。NAltarCheck检查四朝向双格放置、阻挡、无需支撑、生存/创造移除、唯一掉落与活塞限制，以及灵虚底座实时模式切换、两半同步、发光与状态存档；通过实际BlockDropsEvent计数。NAltarRitualCheck验证低IQ档真实速度Ⅰ/力量Ⅰ效果和范围边界、数字生物、死亡确认/取消/去重/重叠选择、存档恢复/旧无实体雕像补齐、脉冲衰减、20次底座转箱及独立N类表真实开箱。当前规则见docs/n-altar.md；遗迹试炼刷怪笼领取/放置指令见docs/n-altar-trial-spawners.md，NAltarTrialSpawnerCheck核验原版指令解析、普通/不祥配置、真实生成及数字自然/试炼门槛。

部分模型生成器依赖本机 Minecraft 原版资源路径；查看脚本顶部的输入路径后再运行。`export_nine.py` 读取当前打开的数字9模型场景，不会创建新的源模型文件。

## 音频与题库

几何持有者方向试听已获用户接受。`create_geometry_holder_cinematic_draft.py`依赖NumPy+FFmpeg，仅写三段方向试听到dev-assets/geometry-holder/audio/cinematic-draft，保留WAV/MP3/时间信息；普通检查不重复生成或覆盖已接受试听。

整套生成器`create_geometry_holder_audio.py`依赖NumPy+FFmpeg并复用方向工具，读取已接受的三段WAV，写19段专属mono48kHz Vorbis到sounds/entity/geometry_holder；新母版、manifest及listening_reel.wav/.mp3保留在dev-assets/geometry-holder/audio/cinematic。旧稿、旧OGG和旧生成器保留original-tone-backup，不清理。17个事件ID/双语字幕不变，不读取其他生物或BGM音源。普通检查不重新生成。本机可运行：

```powershell
& 'C:/Users/31375/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/audio/create_geometry_holder_audio.py
```

GeometryHolderSoundCheck捕获服务端声音事件，验证注册、五招客户端蓄力映射/服务器不重发或重复脉冲、释放一次/剩余tick不重播、短持续细节/恢复停止、异变一次/重载静默、受伤/无敌时间/死亡及HOSTILE类别。客户端GeometryHolderChargeSounds的4tick取消淡出/位置/离开清理与整体混音待实机确认，不把服务器回归当作客户端试听。

```powershell
blender --background --factory-startup --python tools/audio/create_seven_audio.py
blender --background --factory-startup --python tools/audio/create_six_audio.py
powershell -NoProfile -File tools/audio/create_pollution_voice_sources.ps1
blender --background --factory-startup --python tools/audio/create_pollution_whispers.py
python tools/quiz/validate_question_translations.py
```

N祭坛原创30秒战斗试听由 `python tools/audio/create_n_altar_battle_draft.py` 生成，依赖NumPy和FFmpeg；输出MIDI、音符事件、WAV/MP3/OGG到dev-assets/n-altar/music/draft-01，不写入正式声音资源。该试听保留；正式BGM已采用用户提供的N_Song_1.wav。

`prepare_n_altar_music.py` 依赖Python标准库和FFmpeg，保留原WAV逐字节副本于dev-assets/n-altar/music/approved，把完整144.08秒歌曲与128秒起尾声转换为两个流式Vorbis OGG。不会写回输入文件；已保存同名原音源若内容不同则拒绝覆盖。改变尾声切点需同步NAltarMusicSequence.OUTRO_START_SECONDS。此工具会写入正式声音资源，普通检查不重复执行转换。

```powershell
python tools/audio/prepare_n_altar_music.py "C:\software\Minecraft\ModMake\MathMaster-resource\Music\N_Song_1.wav"
```

播放状态检查无需客户端、音频设备或Gradle，使用Java21与可控音频后端覆盖渐变、完整循环、自然尾声/交叉淡化、异步解码等待、播放失败回退、重入和清理。

```powershell
javac -encoding UTF-8 -d build/n-altar-music-check src/main/java/com/freshfish/mathmaster/ritual/NAltarMusicSequence.java tools/checks/n-altar-music/NAltarMusicSequenceCheck.java
java -cp build/n-altar-music-check NAltarMusicSequenceCheck
```

污染低语先生成 Windows 人声音源，再运行混音脚本。`add_english_question_translations.py` 是题库写入工具，不要把它当作校验命令重复执行。

## 隔离回归检查

几何持有者BGM转换工具保留用户原WAV，导出两个战斗段与两个自然尾段，源哈希及切点记录于dev-assets/geometry-holder/music/approved/audio-report.json。预览工具依赖NumPy+FFmpeg，导出27秒阶段/击败过渡示意，不是游戏录音；普通检查不重复转换音源。

```powershell
& 'C:/Users/31375/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/audio/prepare_geometry_holder_music.py
& 'C:/Users/31375/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/audio/preview_geometry_holder_music.py
javac -encoding UTF-8 -d build/geometry-holder-music-check src/main/java/com/freshfish/mathmaster/ritual/GeometryHolderMusicSequence.java tools/checks/geometry-holder-music/GeometryHolderMusicCheck.java
java -cp build/geometry-holder-music-check GeometryHolderMusicCheck
```

播放状态检查覆盖3759项增益/启动/解码等待、两阶段2秒淡化、循环提前接回、阶段中击败、两种尾声只播一次、脱离/卸载退出、失败回退/重试及尾声失败退出上限；N祭坛原有338项检查保持PASS。客户端响度和音乐焦点共存仍需实机确认。

素数连击PrimeComboSkillCheck同属collision隔离检查，当前178项断言通过：真实Player.attack/Mixin主目标范围与同刻切槽、首击/素数递增/切换目标/动态生命上限、冷却缩放/香农熵/护甲/横扫、双层费用/核心不足/取消/零伤害/无敌帧/盾牌/吸收、未命中边界/无总持续上限/受伤/反伤/卸下/跨维度/离线/满污染致死清理、旧存档/共享冷却/戒指及本人状态包。规则docs/prime-combo.md；屏幕边缘和实战另需客户端验收，结束后普通强制编译移除自动测试class。

回归的ReturnSkillCheck包含于collision隔离检查，79项覆盖新公理/旧编号、五槽持久保存、数学归纳法隐藏恢复、点按管理/非法与空槽操作、四等级2秒边界/冷却/+2x、精确落点不避障、取消与缺维度免费、主世界↔下界真实传送、99污染Ⅴ级跨维度/同维度回归致死（等待原版确认、满值存档、唯一死亡与来源）、戒指及重复回归。客户端锚点界面与旋转粒子另需试玩，完整说明docs/return.md；结束后仍须普通强制编译移除自动测试class。

GeometryHolderCheck、GeometryHolderCombatCheck、GeometryHolderAttackCheck与GeometryHolderRangedCheck包含在collision检查中，验证实际刷怪蛋交互、注册尺寸/属性、无重力/推动、游走与存档恢复；另外覆盖单一身体受击箱、普通伤害/取消/无敌帧/玩家归属/单次死亡、抢夺III真实击杀仅掉落2～3个素锭且死亡后不重复发放、每tick追踪、半秒转身及40tick连续位移、32格发现/16格速度分界/48格脱战边界、战斗升降/长期无高度漂移、近远距离空档追近/普通恢复移动及计时不变/施法与画地僵直锁位/面向/墙体阻挡/摔伤、旧预览迁移与模板搬放锚点平移；近距离技能检查覆盖4格边界/近战施法、16/40tick蓄力、3格球与7格立方范围、光环击退/单次结算、画地为牢30tick持续伤害/无敌帧3次命中/离开及重入/取消、锁位、30tick僵直、目标丢失与重载清理；远程检查覆盖2秒光线蓄力/2.5秒持续伤害/锁向/侧移/墙体裁剪，炸弹追踪/限速/玩家接触爆炸归属/撞墙保留方块/6秒超时/目标失效/活动状态重载，十字四向夹角/预警/4秒旋转/结束清理、偏高先下降对齐及站立玩家命中；二阶段覆盖2秒渐变起点/中点/终点/单调性/帧间插值/重载不重播、40%严格边界/颜色参数/回血保持/旧存档与阶段重载/8格范围裁剪和实际伤害/4格光环命中与击退；二阶段远程另验证加粗光线实际命中/十字宽度与4秒不变/三弹数量、分散、限速、追踪、裁剪包络、独立引爆与无敌帧、全部到期/目标失效/重载清理；GeometryHolderUltimateCheck另验证两阶段12/20枚、独立远近调度/冷却、0.5格体积/16格分布/裁剪、64次固定种子面积分布抽样与第20枚独立引爆、4秒到期/单枚接触/唯一爆裂、1格伤害边界/墙体/地形保留、归属/无敌帧与取消/死亡/重载清理；GeometryHolderSkillPoolCheck验证两万次固定种子加权抽样/重复与偏向未用/独立历史/重载重置、实际200生命/20护甲减伤/80点阶段边界及保存恢复；GeometryHolderBossBarCheck验证14项多人追踪/48格边界/返回/生命比例/改名/阶段色/死亡/卸载/重载清理与音乐天空雾保持；当前397项几何持有者检查通过。伤害分档普通难度减免前为光环12/画地6/光线4/炸弹13/十字6/大招10，两个阶段相同；近战检查另覆盖两阶段简单/普通/困难及0/20护甲真实扣血。技能几何检查用受控单技能池隔离随机调度，不依赖固定轮换顺序。全亮贴图另检查蓝色区域不透明、其余透明黑。游戏内视觉与命中手感需重启开发客户端后在生存模式实测，创造/旁观不被发现。

需要 Java 21、项目 Gradle Wrapper，以及已安装的开发依赖。以下命令会写入 `build/` 测试目录，不使用 `run/` 用户存档。

```powershell
.\gradlew.bat -I tools/checks/collision-check.gradle runServer --offline --no-daemon --no-configuration-cache
.\gradlew.bat -I tools/checks/gateway-check.gradle runServer --offline --no-daemon --no-configuration-cache
.\gradlew.bat -I tools/checks/n-altar-offerings-check.gradle runServer --offline --no-daemon --no-configuration-cache
```

结果分别查看 `build/seven-collision-check/collision-result.txt` 和 `build/gateway-check/gateway-result.txt`；不能只凭 Gradle 的退出码判断断言通过。collision检查包含PollutedWaterCheck：三维度桶收放、有限源与熔岩式扩散、身体/浅流接触、环境重叠单次积累、套装和外部效果保留，以及真实LivingEntity.travel横向减速和上浮出水。CollatzTreeBlocksCheck检查树苗和五种基础方块实际放置/掉落、三个轴向、手放树叶持久化/含水、剪刀与精准采集回收及木棍/果实真实掉落概率、时运数量与回收优先级；CollatzTreeGrowthCheck使用隔离存档的受控时钟验证初始5～10完整数列、每段真实树干长度、2秒/0.2秒时序、受阻转向与恢复、破坏永久停止、存档往返及未加载区块不推进/不强制加载，并检查分叉概率、50端点共享额度、独立枝段受阻恢复、16,384树干共享上限和旧单枝存档迁移。

NAltarIntellectCheck同属collision隔离检查，当前204项断言通过：验证IQ四档边界、12格球形玩家平均/小数/创造旁观死亡排除、心流和学士帽、10格生物范围与外部效果保留、永久解锁/旧洞悉记录/死亡复制、锁定时四类祭品无费用/污染/随机及双手空手提示。原OracleOfferingCheck和NAltarOfferingsCheck的常规玩法夹具先标记永久解锁，锁定与边界另由新检查负责；隔离检查后仍须普通强制编译移除测试class。

OracleOfferingCheck同属collision隔离检查：实际上下半/主副手交互逐一枚举四种祭品的全部100个概率区间，验证成功/失败各消耗一个及聊天回应、未知物品/旁观/血祭排除；同时验证低中高级池独立、合集追加/去重/非法文件隔离、条目删除重载与空池不消耗。

NAltarOfferingsCheck验证果实经验/智识上限/污染、单位元护盾及污染致死、四种亵渎物全部100个随机区间、10秒状态及保留外部效果、无火焰/重复伤害的雷击、跨祭坛冷却、上下半/主副手与血祭排除。collision世界关闭结构生成，验证指引未找到不消费；n-altar-offerings-check.gradle另用build/n-altar-offerings-check普通世界验证普通/巨型候选距离排序、真实结构起点、最近坐标、不改引用次数、成功扣物/60秒冷却、重复请求/切手取消及非主世界拒绝。首次运行需在该目录提供eula=true和启用结构的正常世界server.properties，结果查看offerings-result.txt；不打开箱子，可复用该隔离世界。测试订阅器NAltarOfferingsServerCheck同样须由后续普通强制编译移除。

测试源码只在使用对应 `-I` 参数时加入源集。隔离检查结束后，执行一次普通强制编译，避免增量编译的UP-TO-DATE保留带自动事件订阅的检查class，影响后续正常开发启动；不执行clean，不清空run/build，也不构建发布JAR。

```powershell
.\gradlew.bat compileJava --rerun processResources --offline --no-daemon --no-configuration-cache
```

确认 `build/classes/java/main/com/freshfish/mathmaster/check/SevenCollisionCheck.class` 不存在后，再正常启动客户端/服务端。

主世界传送门战利品检查覆盖1024次真实抽取、21项物品、6～8次抽奖、数量边界、六件随机附魔装备，并核对N类试炼地图的保存数据、翻译名称、红叉标记及真实结构目标。权重与静态表见docs/structures.md。

NAltarRewardLootCheck同属gateway检查，当前5252项断言通过：读取血祭表中已解码的random_rewards池单独抽样1024次，避免重复进行1024次地图搜索；验证13项/3～5次/数量边界与三件装备附魔。完整表另验证地图1张和灵虚锭2～3个保底不占随机次数、真实圣所目标/地图名称/红叉/原版组件保存/结构引用保持、真实箱子分堆保底/重开不重掷及下界空地图回退。collision中的NAltarRitualCheck同时验证20贡献实际转箱后的两项保底；规则与测试指令见docs/n-altar.md。

结构检查会打开自然生成的箱子。重新运行前，在 `build/gateway-check/server.properties` 使用新的 `level-name` 创建隔离测试世界，保留旧世界；已打开箱子的战利品表已抽取，不能继续用于首次开箱断言。不使用 `run/` 用户存档，也不删除旧测试目录。启动回调集中执行大量结构/地图检查时可能超过原版60秒看门狗限制；检查期间可仅把该build隔离服务器的max-tick-time临时设为-1，结束后恢复原值，不修改run配置。

## 几何持有者血条样式预览

预览使用实际HUD的GeometryHolderBarStyle绘制代码，不启动run世界；SVG/PNG输出到build根目录，字体预览与游戏实际Minecraft字体有区别。PNG导出需要Pillow和本机Windows微软雅黑。

```powershell
javac -encoding UTF-8 -d build/geometry-holder-bar-preview src/main/java/com/freshfish/mathmaster/client/GeometryHolderBarStyle.java tools/checks/geometry-holder-bar/GeometryHolderBarPreview.java
java -cp build/geometry-holder-bar-preview GeometryHolderBarPreview
& 'C:/Users/31375/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' tools/checks/geometry-holder-bar/render_preview.py
```

Jade信息框避让检查独立运行，不启动客户端或存档。覆盖普通/混合/多Boss血条、徽记底部、非重叠用户布局、GUI尺寸变化及逐帧清理；实际Jade绘制回调与GUI缩放仍需游戏内确认。

```powershell
javac -encoding UTF-8 -d build/geometry-holder-tooltip-check src/main/java/com/freshfish/mathmaster/compat/jade/BossTooltipLayout.java tools/checks/geometry-holder-bar/BossTooltipLayoutCheck.java
java -cp build/geometry-holder-tooltip-check BossTooltipLayoutCheck
```

并行技能调度回归 `GeometryHolderConcurrentCheck` 已接入上述隔离服务器命令：覆盖≤4格近远并行、>4格仅远程、16tick起手间隔、两组独立计时、画地僵直与既有远程继续、固定牢笼、天地为牢三组并行与独立结束、阶段冷却及死亡/重载清理；另覆盖两阶段近远距离的完整底座、半砖、天花板阻挡升降后仍可开招/实际伤害/不穿墙，原无障碍先平滑对齐高度检查保留。旧几何检查通过禁用无关组的冷却隔离单招，避免并行伤害干扰范围断言；单招数值和无敌帧断言保留。

## 原版数字5画作

variant资源data/mathmaster/painting_variant/digital_five.json，4×4格；生产纹理assets/mathmaster/textures/painting/digital_five.png逐字节保留批准像素图。开发原图/概念图在dev-assets/paintings/digital-five，不重复生成。ModPaintings.createDigitalFive提供带原版ENTITY_DATA的minecraft:painting，创造方块页可取，未加入minecraft:placeable随机标签。PaintingCheck随collision隔离检查验证动态注册、创造/搜索入口、真实原版物品放置、随机池排除、空间不足不消耗和实体存档恢复；真实客户端纹理显示另行试玩。

## 几何圣所结构

GeometrySanctuaryStructureCheck随gateway-check以233项断言检查45×20×40原始模板、六群系、64/24与75%筛选、原版村庄12区块避让及正负坐标/placement codec、九点预测地形6格高差/水面/高度边界、四向逐区块放置及两个DATA标记唯一生成/持久化、原箱子内容/奖励表/祭坛/保存实体、组件codec、真实地形和自然locate。使用新的level-name保留旧检查世界，结束后普通强制编译移除自动检查class；规则与指令见docs/geometry-sanctuary.md。

## N类试炼结构

下界未完成传送门的 `NetherGatewayStructureCheck` 同属上述gateway隔离检查，验证五个原始NBT/20种旋转和下界专用表实际开箱、五种群系、40/15与50%筛选、32～100高度/支撑与可进入空间/岩浆及边界拒绝、按列缓存、原版组件存档，以及真实下界地形与自然locate。修改模板清单时同步该检查的尺寸/箱子预期；运行前使用新的level-name，结束后普通强制编译移除自动测试class。规则见 `docs/nether-gateway.md`。

NTrialStructureCheck随gateway-check检查动态结构/模板池/17种试炼与21种传送门群系、48/18与50%筛选、34×18×33原始模板四向放置、灵虚底座/0贡献新血祭轮次/双半模式同步、四个试炼刷怪笼NBT与24次补给箱真实开箱，以及自然locate/生成起点；补给表另抽样1024次，覆盖全部24项、4～6次抽取、所有数量上下限和七件附魔装备。检查前server.properties使用新level-name，保留旧检查世界；结果仍为gateway-result.txt。用户模板/生成规则见docs/n-trial.md。

紫/蓝考拉兹生成器create_collatz_color_variants.py复用现有树木/建筑像素绘制源码，仅在绘制前替换配色及输出名称，不覆盖原红色PNG；输出24张新纹理和两色树木/木材预览。三色木材配方/燃料/火焰检查和紫蓝扎根/同色成长/原格式重载/停长检查包含在collision隔离检查。
