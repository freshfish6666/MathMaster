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
      "question": "2 + 2 = ?",
      "correct_answer": "4",
      "wrong_answers": ["2", "3", "5"]
    }
  ]
}
```

- `replace` 为 `true` 时，加载该文件前会清空同名题库已经收集的题目。
- `questions` 是题目数组。
- 每道题必须提供非空题干、一个正确答案和恰好三个错误答案。
- 四个答案必须互不相同，同一题库内题干也不能重复。
- 无效题目会被跳过，并在服务端日志中说明原因。

若要覆盖内置题库，请在数据包中使用 `mathmaster` 命名空间及相同文件路径。其他命名空间下使用上述文件名时，题目会追加到对应题库。
