# MathMaster 公共 API 与联动

本页描述 MathMaster 1.1.1 开始提供的公共 API v1。对接代码应只引用 `com.freshfish.mathmaster.api.MathMasterApi`；`intellect`、`intelligence`、`init`、`menu` 和 `network` 等包均属于内部实现，不承诺跨版本稳定。

## Java API v1

`MathMasterApi.API_VERSION` 当前为 `1`。MathMaster 1.x 会保留 API v1 已公开方法的含义与二进制签名。

```java
OptionalInt base = MathMasterApi.getBaseEntityIntellect(entity.getType());
OptionalInt effective = MathMasterApi.getEffectiveEntityIntellect(entity);
int playerIntellect = MathMasterApi.getEffectivePlayerIntellect(player);

if (player instanceof ServerPlayer serverPlayer) {
    boolean insighted = MathMasterApi.hasSuccessfullyInsighted(
            serverPlayer,
            entity.getType()
    );
}
```

- 基础智识不包含临时增益；玩家有效智识包含心流与学士帽等装备增益，并保持 200 上限。
- 没有智识定义或生物智识总开关关闭时，生物查询返回 `OptionalInt.empty()`。
- 洞悉记录是服务端权威数据，因此查询方法要求 `ServerPlayer`。
- `getSuccessfullyInsightedEntityTypes` 返回不可变快照，调用方不能修改玩家存档。

### 数学书目录与答题

服务端联动可以读取当前确实含有有效题目的数学书，并请求 MathMaster 为玩家打开指定书籍的答题。目录及其中的 `QuizBook` 都是不可变值；书籍使用物品注册表 ID 标识，调用方不需要接触题库、菜单或网络负载。

```java
for (MathMasterApi.QuizBook book : MathMasterApi.getAvailableQuizBooks()) {
    ResourceLocation id = book.id();
    String translationKey = book.translationKey();
    int difficulty = book.difficulty();
}

boolean opened = MathMasterApi.startQuiz(serverPlayer, selectedBookId);
```

`startQuiz` 只接受 `ServerPlayer`，并在服务端重新核对书籍 ID 与当前题库；未知书籍或空题库返回 `false`。调用方只应传达玩家的选择，不能自行抽题、判分或发放奖励。

## 生物智识 JSON

无需编译依赖即可用数据包为其他模组生物设置智识：

```text
data/<命名空间>/entity_intellect/<文件名>.json
```

```json
{
  "entity": "othermod:example_creature",
  "intellect": 80,
  "required_mod": "othermod",
  "enabled": true
}
```

`required_mod` 是可选的模组 ID。MathMaster 内置其他模组的兼容 JSON 时应填写它，以便目标模组未安装时跳过该定义。服务器执行 `/reload` 后重新读取数据；`integrations.external_entity_intellect = false` 会统一禁用所有非 `minecraft` 命名空间的生物定义。

## Jade

Jade 是可选依赖，不会打包进 MathMaster。安装后，MathMaster 对具有智识定义的生物仅追加两行：

- 生物智识
- 洞悉状态

智识与当前查看玩家的洞悉状态都由服务端生成。可在 `mathmaster-server.toml` 中把 `integrations.jade` 改为 `false`，只关闭 MathMaster 的 Jade 信息。

查看几何持有者时，如果 Jade 信息框与正在显示的Boss血条重叠，客户端会通过 Jade 的公开绘制回调把整个信息框移到当前血条组下方，留出6个GUI像素间距。只调整 Jade 信息框的本帧Y坐标，不改变 MathMaster 血条、Jade 保存的布局/缩放设置或其他目标的信息框。按当帧实际绘制的血条范围处理多Boss混排；无该Boss血条、非重叠布局、打开其他界面或隐藏HUD时不调整。此布局避让独立于智识两行的服务端开关，Jade仍为可选依赖；JEI的物品/配方界面保持原样。

## MCphone

MCphone 是可选依赖，不会打包进 MathMaster。两者同时安装时，手机中会出现使用专用学习图标的“学习通”App，其简介为“在手机上选择科目进行答题”；“数学大师”是当前第一个科目，进入后选择服务端下发的五本数学书即可打开现有答题界面。App 实现仅位于客户端兼容包，通过 MCphone 的公开 SPI 发现；选书后只向服务端发送书籍注册表 ID，题目选择、判分与奖励仍由 MathMaster 服务端负责。

## 数学书物品标签

`mathmaster:quiz_books` 是可放入书架设施的公共物品标签。MathMaster 将该标签整体引入原版 `minecraft:bookshelf_books` 和 `minecraft:lectern_books`，因此其他模组或数据包把书加入此标签后，也能将其放进雕纹书架和讲台。加入标签只提供放置能力；要让书实际启动某一科目的答题，仍需由对应模组注册题目与服务端答题能力。

## Touhou Little Maid

Touhou Little Maid 是服务端与客户端均可选的依赖，不会打包进 MathMaster。两者同时安装时，女仆工作模式中新增“学习/答题”：处于工作时段的女仆每 5 秒在水平 12 格、垂直 4 格内寻找放有可用 MathMaster 数学书的讲台，靠近并面向讲台自主答题。

女仆拥有独立持久化 IQ，初始为 40～60，升级所需经验始终为玩家的两倍。答题时间由书籍难度与当前 IQ 共同决定并限制在 2～600 秒；正确率限制在 10%～95%。题目行同时显示当前 IQ，下一行使用“思考中……/答对啦/哎呀，答错了”状态，并通过女仆模组的聊天气泡同步。正确答案使用与玩家相同的重叠奖励表、智力经验来源上限和最高档每日 10 次限制；奖励优先进入女仆背包，余下物品掉在脚边。错误答案使用同一惩罚分布，但女仆不会受到雷击惩罚。可在 `mathmaster-server.toml` 中把 `integrations.touhou_little_maid` 改为 `false`。

## MadnessCore

MadnessCore 1.x 是服务端与客户端均可选的依赖，不会打包进 MathMaster。未安装时使用空实现，答题、智识、洞悉与其他联动保持原行为；安装后，仅隔离兼容类 `compat.madnesscore.MadnessCoreIntegration` 引用 MadnessCore 的 `com.freshfish.madnesscore.api` 公共 API。

玩家每次实际开始一道题时消耗“书籍难度 ÷ 2”的精神值。精神值低于 10 时不能开始或继续答题；若单题消耗高于 10，则还必须拥有足够支付完整消耗的精神值。检查与扣除都发生在服务端权威答题入口，来源为 `mathmaster:quiz_system`，原因为 `mathmaster:quiz_start`。

MathMaster 根据当前服务端题库和玩家正确记录，向 MadnessCore 上报一份 `mathmaster:progression` 位阶贡献：完成启蒙数学手册为位阶 9，完成高等数学手册为位阶 8，完成《对数学诚挚的爱》为位阶 7；多项完成时上报其中最强的一项。该贡献在进度变化、登录与题库重载后重新计算，只会修改 MathMaster 自己的来源。联动不复制 MadnessCore 存档或网络状态，也不需要旧档迁移。
