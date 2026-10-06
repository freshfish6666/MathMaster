from __future__ import annotations

from pathlib import Path
import json
import time
import urllib.error
import urllib.parse
import urllib.request


ROOT = Path(__file__).resolve().parents[2]
QUESTION_DIRECTORIES = (
    ROOT / "src/main/resources/data/mathmaster/quiz_banks",
    ROOT / "src/main/resources/data/mathmaster/insight_quiz_banks",
)
CACHE_PATH = ROOT / "build/english-question-translation-cache.json"
DELIMITER = "\n<<<MATHMASTER_SPLIT>>>\n"
MAX_BATCH_CHARACTERS = 3_000
ENDPOINT = "https://translate.googleapis.com/translate_a/single"


def translate_request(texts: list[str]) -> list[str]:
    joined = DELIMITER.join(texts)
    request_data = urllib.parse.urlencode({
        "client": "gtx",
        "sl": "zh-CN",
        "tl": "en",
        "dt": "t",
        "q": joined,
    }).encode("utf-8")
    request = urllib.request.Request(
        ENDPOINT,
        data=request_data,
        headers={"User-Agent": "MathMaster question localization tool"},
    )
    with urllib.request.urlopen(request, timeout=45) as response:
        payload = json.loads(response.read().decode("utf-8"))
    translated = "".join(segment[0] for segment in payload[0])
    parts = [part.strip() for part in translated.split(DELIMITER)]
    if len(parts) != len(texts):
        raise ValueError(f"translation delimiter mismatch: expected {len(texts)}, got {len(parts)}")
    if any(not part for part in parts):
        raise ValueError("translation service returned blank text")
    return parts


def translate_batch(texts: list[str]) -> list[str]:
    for attempt in range(5):
        try:
            return translate_request(texts)
        except (OSError, ValueError, json.JSONDecodeError, urllib.error.HTTPError) as exception:
            if len(texts) > 1:
                middle = len(texts) // 2
                return translate_batch(texts[:middle]) + translate_batch(texts[middle:])
            if attempt == 4:
                raise RuntimeError(f"Could not translate {texts[0]!r}") from exception
            time.sleep(1.5 * (attempt + 1))
    raise AssertionError("unreachable")


def make_batches(texts: list[str]) -> list[list[str]]:
    batches: list[list[str]] = []
    current: list[str] = []
    current_length = 0
    for text in texts:
        added = len(text) + (len(DELIMITER) if current else 0)
        if current and current_length + added > MAX_BATCH_CHARACTERS:
            batches.append(current)
            current = []
            current_length = 0
            added = len(text)
        current.append(text)
        current_length += added
    if current:
        batches.append(current)
    return batches


files = [
    path
    for directory in QUESTION_DIRECTORIES
    for path in sorted(directory.glob("*.json"))
]
documents = {path: json.loads(path.read_text(encoding="utf-8")) for path in files}

CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
cache = json.loads(CACHE_PATH.read_text(encoding="utf-8")) if CACHE_PATH.exists() else {}
source_texts: list[str] = []
seen: set[str] = set(cache)
for document in documents.values():
    for question in document["questions"]:
        for text in (question["question"], question["correct_answer"], *question["wrong_answers"]):
            if text not in seen:
                seen.add(text)
                source_texts.append(text)

batches = make_batches(source_texts)
for index, batch in enumerate(batches, start=1):
    translated = translate_batch(batch)
    cache.update(zip(batch, translated, strict=True))
    CACHE_PATH.write_text(
        json.dumps(cache, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Translated batch {index}/{len(batches)} ({len(batch)} strings)", flush=True)
    time.sleep(0.25)

for path, document in documents.items():
    is_insight = path.parent.name == "insight_quiz_banks"
    for index, question in enumerate(document["questions"], start=1):
        if is_insight and "id" not in question:
            question["id"] = f"question_{index:03d}"
        translations = question.setdefault("translations", {})
        translations["en_us"] = {
            "question": cache[question["question"]],
            "correct_answer": cache[question["correct_answer"]],
            "wrong_answers": [cache[answer] for answer in question["wrong_answers"]],
        }
        english = translations["en_us"]
        replacements = {
            "tolerance": "common difference",
            "even coin": "fair coin",
            "Generalized integral": "Improper integral",
            "staggered harmonic series": "alternating harmonic series",
            "staggered series": "alternating series",
            "Koraz": "Collatz",
            "Coraz": "Collatz",
            "Seven Millennium Puzzles": "seven Millennium Prize Problems",
            "Euler function": "Euler's totient function",
            "zero degree": "nullity",
            "three dimensional": "three-dimensional",
        }
        for field in ("question", "correct_answer"):
            for old, new in replacements.items():
                english[field] = english[field].replace(old, new)
        for answer_index, answer in enumerate(english["wrong_answers"]):
            for old, new in replacements.items():
                answer = answer.replace(old, new)
            english["wrong_answers"][answer_index] = answer

        if question["question"] == "在频率分布直方图中，每个小长方形的面积表示？":
            english["correct_answer"] = "Relative frequency"
            english["wrong_answers"][0] = "Frequency count"
        elif question["question"] == (
                "随机变量X的分布列为P(X=1)=1/5，P(X=2)=1/2，P(X=3)=3/10，则E(X)=?"
        ):
            english["question"] = (
                "A random variable X has distribution P(X=1)=1/5, P(X=2)=1/2, "
                "and P(X=3)=3/10. What is E(X)?"
            )
        elif question["question"] == "线性映射的定义域维数为 n，则秩与零度之和是？":
            english["question"] = (
                "If a linear map has an n-dimensional domain, what is the sum of its rank and nullity?"
            )
        elif question["question"] == "方阵的迹等于其所有特征值的什么量？":
            english["correct_answer"] = "Their sum, counted with algebraic multiplicity"
        elif question["question"] == "上三角矩阵的特征值是什么？":
            english["correct_answer"] = "Its diagonal entries"
        elif question["question"] == "BSD 猜想中，椭圆曲线的有理点群秩对应其 L 函数在 s=1 处的什么量？":
            english["correct_answer"] = "The order of vanishing"
        elif question["question"] == "函数 f(x) = arctan x 的导函数 f'(x) = ?":
            english["question"] = "For f(x) = arctan x, what is f'(x)?"
    path.write_text(
        json.dumps(document, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Updated {path.relative_to(ROOT)} ({len(document['questions'])} questions)")
