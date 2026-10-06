from pathlib import Path
import json
import re


ROOT = Path(__file__).resolve().parents[2]
BANKS = (
    ROOT / "src/main/resources/data/mathmaster/quiz_banks",
    ROOT / "src/main/resources/data/mathmaster/insight_quiz_banks",
)
CJK = re.compile(r"[\u3400-\u9fff]")

total = 0
for directory in BANKS:
    directory_total = 0
    for path in sorted(directory.glob("*.json")):
        document = json.loads(path.read_text(encoding="utf-8"))
        ids: set[str] = set()
        for index, question in enumerate(document["questions"], start=1):
            label = f"{path.relative_to(ROOT)} question {index}"
            question_id = question.get("id", "").strip()
            assert question_id, f"{label}: missing id"
            assert question_id not in ids, f"{label}: duplicate id {question_id}"
            ids.add(question_id)

            english = question.get("translations", {}).get("en_us")
            assert english is not None, f"{label}: missing en_us translation"
            assert english["question"].strip(), f"{label}: blank English question"
            assert len(english["wrong_answers"]) == 3, f"{label}: wrong English answer count"
            answers = [english["correct_answer"], *english["wrong_answers"]]
            assert all(answer.strip() for answer in answers), f"{label}: blank English answer"
            assert len(set(answers)) == 4, f"{label}: duplicate English answers"
            assert not CJK.search(english["question"]), f"{label}: Chinese remains in question"
            assert not any(CJK.search(answer) for answer in answers), f"{label}: Chinese remains in answers"
            assert "MATHMASTER_SPLIT" not in json.dumps(english), f"{label}: delimiter leaked"
        directory_total += len(document["questions"])
    total += directory_total
    print(f"{directory.name}: {directory_total} bilingual questions")

assert total == 1228, f"expected 1228 built-in questions, found {total}"
print(f"Validated {total} bilingual questions")
