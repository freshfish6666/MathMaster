# MathMaster 题库数据格式

题库位于数据包的 `data/<命名空间>/quiz_banks/` 目录。MathMaster 内置题库使用以下文件名：

- `advanced.json`
- `grade_2.json`
- `junior_high.json`
- `senior_high.json`
- `millennium_problems.json`

修改数据包后执行 `/reload` 即可重新加载题库，无需重新编译模组。

## 文件结构

```json
{
  "replace": false,
  "questions": [
    {
      "id": "addition_001",
      "question": "2 + 2 = ?",
      "correct_answer": "4",
      "wrong_answers": ["2", "3", "5"],
      "translations": {
        "en_us": {
          "question": "What is 2 + 2?",
          "correct_answer": "4",
          "wrong_answers": ["2", "3", "5"]
        }
      }
    }
  ]
}
```

- `replace` 为 `true` 时，加载该文件前会清空同名题库已经收集的题目。
- `questions` 是题目数组。
- `id` 是可选的稳定题目标识。建议始终填写；同一题库内不得重复。
- 每道题必须提供非空题干、一个正确答案和恰好三个错误答案。
- 四个答案必须互不相同，同一题库内题干也不能重复。
- `translations` 可为同一题目提供其他语言文本；当前内置界面支持 `en_us`，其题干、正确答案和三个错误答案必须完整填写。
- 翻译和中文原文共用同一个 `id`，语言切换不会重新抽题或建立另一份学习进度。缺少 `en_us` 时英文模式自动回退中文。
- 无效题目会被跳过，并在服务端日志中说明原因。

未填写 `id` 时，MathMaster 会根据题干和答案生成稳定的回退 ID，并在日志中提示。显式 ID 会与数据包命名空间和题库名组合，例如 `example_pack:grade_2/addition_001`。发布题目后不要因为调整顺序或修正错字而更改 ID；若题目含义或正确答案发生实质变化，应使用一个新 ID。

题目 ID 用于记录每位玩家至少答对过一次的题目、最近答对的题目和掌握度。最近答对的 10 道题会优先从抽题候选中排除；未答对、答对一次和答对两次以上的抽题权重依次为 `1`、`0.5` 和 `0.1`。玩家至少答对当前已加载题库中的每道题一次后，会获得对应数学书的完成成就。

若要覆盖内置题库，请在数据包中使用 `mathmaster` 命名空间及相同文件路径。其他命名空间下使用上述文件名时，题目会追加到对应题库。
