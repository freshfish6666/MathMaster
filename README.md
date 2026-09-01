# MathMaster（数学大师）

![MathMaster 图标](src/main/resources/mathmaster.png)

MathMaster 是一个面向 Minecraft 生存玩法的数学问答模组。玩家可以制作不同难度的数学书，将它们放入雕纹书架，并通过相邻的普通书架开始四选一答题。

## 运行环境

| 项目 | 版本 |
| --- | --- |
| Minecraft Java Edition | 1.21.1 |
| NeoForge | 21.1.248 |
| Java | 21 |
| MathMaster | 1.0.1 |
| 模组 ID | `mathmaster` |

客户端和服务端都需要安装本模组。使用其他 Minecraft 或 NeoForge 版本可能无法启动。

## 主要玩法

模组目前包含五种数学书：

- 小学二年级数学，难度 1
- 初中数学，难度 5
- 高中数学，难度 10
- 高等数学习题册，难度 20
- 数学十大难题集锦，难度 30

书的难度决定答对后获得的智力经验。玩家初始 IQ 随机为 30～50，最高为 200；答对题目可以提升智力经验和 IQ。

答对后会按照题目难度与当前 IQ 从多个奖励区间中抽取物品，奖励区间可以重叠。达到最高奖励档时，每位玩家每个 Minecraft 游戏日最多领取 10 次完整物品奖励；超过次数后仍可继续答题并获得智力经验，但不会获得物品奖励。

答错可能没有额外影响，也可能受到伤害、失明、雷击或获得三猫奶粉。部分食物也会少量改变智力经验。

## 如何搭建答题书架

1. 制作或从“数学大师”创造模式物品栏中取得一本数学书。
2. 放置一个雕纹书架，并将数学书放入其中。
3. 在雕纹书架任意相邻方向放置一个普通书架。两个书架必须直接相邻，中间不能留空格。
4. 不要按住潜行键，右击普通书架。
5. 游戏会从相邻雕纹书架中的数学书里随机选择题库，并打开四选一答题界面。

一个普通书架周围可以连接多个雕纹书架，也可以放入多本数学书。此时题库会从所有检测到的数学书中随机选择。

## 管理员命令

所有命令都需要权限等级 2。

| 命令 | 作用 |
| --- | --- |
| `/mathmaster query <玩家> points` | 查询玩家当前 IQ 等级内的智力经验 |
| `/mathmaster query <玩家> levels` | 查询玩家当前 IQ |
| `/mathmaster add <玩家> <数量> points` | 增加智力经验 |
| `/mathmaster add <玩家> <数量> levels` | 增加 IQ |
| `/mathmaster set <玩家> <数量> points` | 设置当前 IQ 等级内的智力经验 |
| `/mathmaster set <玩家> <数量> levels` | 设置 IQ |

`add` 和 `set` 省略最后的 `points` 或 `levels` 时，默认操作智力经验。`<玩家>` 支持 Minecraft 目标选择器，例如 `@a`、`@p` 或玩家名。

IQ 会被限制在 30～200。`set points` 的值必须小于当前 IQ 升到下一级所需的经验值。

## 配置

当前版本暂未提供独立的 NeoForge 配置文件。下列参数由源码常量控制，修改后需要重新构建模组：

| 参数 | 当前值 | 代码位置 |
| --- | ---: | --- |
| IQ 范围 | 30～200 | `IntelligenceData` |
| 初始 IQ 范围 | 30～50 | `IntelligenceData` |
| 最高奖励每日次数 | 10 | `HighestTierRewardLimit` |
| 奖励区间与奖池 | 多个可重叠区间 | `RewardManager` |
| 连续答题提醒窗口 | 10 分钟 | `AntiAddictionManager` |

题目内容不需要修改源码，可通过 JSON 数据包扩展或覆盖。

## 题库扩展

题库文件位于数据包的：

```text
data/<命名空间>/quiz_banks/<题库名>.json
```

可用题库名如下：

| 文件名 | 对应数学书 |
| --- | --- |
| `grade_2.json` | 小学二年级数学 |
| `junior_high.json` | 初中数学 |
| `senior_high.json` | 高中数学 |
| `advanced.json` | 高等数学习题册 |
| `millennium_problems.json` | 数学十大难题集锦 |

示例：

```json
{
  "replace": false,
  "questions": [
    {
      "question": "2 + 2 = ?",
      "correct_answer": "4",
      "wrong_answers": ["2", "3", "5"]
    }
  ]
}
```

规则：

- `replace: false` 表示将题目追加到对应题库。
- `replace: true` 表示加载该文件时先清空对应题库。
- 每道题必须有非空题干、一个正确答案和恰好三个错误答案。
- 四个答案必须互不相同，同一题库内不能出现重复题干。
- 无效题目会被跳过，具体原因会写入服务端日志。
- 使用 `mathmaster` 命名空间和相同文件路径可以覆盖内置文件。
- 使用其他命名空间及上述文件名，可以向对应题库追加题目。

将数据包放入世界的 `datapacks` 目录后，执行 `/reload` 即可重新加载题库，无需重新编译模组。更完整的格式说明见 [`docs/quiz-banks.md`](docs/quiz-banks.md)。

## 从源码构建

需要使用 Java 21 和项目自带的 Gradle Wrapper：

```powershell
.\gradlew.bat build
```

构建产物位于：

```text
build/libs/mathmaster-1.0.1.jar
```

## 许可证

MathMaster 基于 [MIT License](LICENSE) 开源。你可以使用、复制、修改和分发本项目，但必须保留原始版权声明和许可证文本。

仓库中的 [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt) 是 NeoForged MDK 原始模板附带的许可证说明；MathMaster 项目本身的许可条款以根目录的 [`LICENSE`](LICENSE) 为准。
