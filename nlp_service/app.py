"""
教学评价 ABSA（方面级情感分析）微服务
技术方案：
  1. 使用 uer/roberta-base-finetuned-dianping-chinese（二分类模型）估计句子正面情感概率
  2. 通过正则方面词检测，将文本拆分到各教学维度
  3. 对每个维度相关句子进行 BERT 情感推断（而非关键词匹配打分）
  4. 返回各维度情感分、关键词和整体情感极性
"""

import re
import json
import logging
import os
from flask import Flask, request, jsonify
from flask_cors import CORS
from functools import lru_cache

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)
# ============ 模型加载（懒加载，首次请求时初始化）============
_pipeline = None


def get_pipeline():
    global _pipeline
    if _pipeline is None:
        try:
            from transformers import pipeline as hf_pipeline
            logger.info("正在加载 BERT 情感分析模型...")
            _pipeline = hf_pipeline(
                "text-classification",
                model=os.getenv("NLP_MODEL_PATH", "uer/roberta-base-finetuned-dianping-chinese"),
                tokenizer=os.getenv("NLP_MODEL_PATH", "uer/roberta-base-finetuned-dianping-chinese"),
                top_k=None,
            )
            logger.info("模型加载完成")
        except Exception as e:
            logger.error(f"模型加载失败: {e}")
            _pipeline = None
    return _pipeline


# ============ 方面维度定义 ============
ASPECT_PATTERNS = {
    "教学态度": re.compile(
        r"(态度|认真|负责|热情|耐心|严格|用心|投入|师德|积极|尽责|细心|关心|关注|辅导|敬业)"
    ),
    "课程难度": re.compile(
        r"(难度|简单|容易|复杂|深度|易懂|难懂|浅显|深入|基础|高深|挑战|难学|不难|很难|较难|适中)"
    ),
    "表达清晰度": re.compile(
        r"(清晰|清楚|模糊|讲解|表达|语言|思路|逻辑|通俗|简洁|明了|条理|层次|语速|发音)"
    ),
    "互动性": re.compile(
        r"(互动|提问|交流|参与|活跃|答疑|讨论|课堂氛围|师生|沟通|反馈|回答|启发|引导)"
    ),
    "教学内容": re.compile(
        r"(内容|丰富|实用|知识|案例|理论|实践|课程|教材|举例|联系实际|前沿|应用|课件|教学质量)"
    ),
}

# 兼容五星模型；默认 uer 模型实际输出 negative/positive 两类。
STAR_SCORE_MAP = {
    "star_1": 0,
    "star_2": 25,
    "star_3": 50,
    "star_4": 75,
    "star_5": 100,
}

def _label_to_score(label_probs: list[dict]) -> float:
    """将模型标签概率转换为情感指标；二分类时即正面概率乘100。"""
    score = 0.0
    for item in label_probs:
        label = item["label"].lower()
        prob = item["score"]
        if label.startswith("negative"):
            value = 0
        elif label.startswith("positive"):
            value = 100
        elif label in STAR_SCORE_MAP:
            value = STAR_SCORE_MAP[label]
        else:
            raise ValueError(f"不支持的模型标签: {item['label']}")
        score += prob * value
    return round(score, 1)


def split_sentences(text: str) -> list[str]:
    """按中文标点切分句子"""
    parts = re.split(r"[，。！？,!?；;、\n]", text)
    return [p.strip() for p in parts if p.strip() and len(p.strip()) >= 2]


def _fallback_score(text: str, aspect: str) -> dict:
    """当模型不可用时的降级策略：基于情感词典的打分（仅作 fallback）"""
    positive = ["好", "棒", "优秀", "认真", "详细", "清楚", "耐心", "满意", "推荐", "喜欢", "有趣"]
    negative = ["差", "糟糕", "不好", "失望", "太难", "听不懂", "无聊", "枯燥", "不满"]
    pos = sum(1 for w in positive if w in text)
    neg = sum(1 for w in negative if w in text)
    score = 50 + pos * 8 - neg * 10
    return {"score": max(0, min(100, score)), "method": "fallback", "hasData": True}


def analyze_absa(text: str) -> dict:
    """
    核心 ABSA 分析函数
    对每个方面维度：
      1. 提取与该维度相关的句子
      2. 使用 BERT 对相关句子进行情感分类（非关键词计数）
      3. 聚合得到该维度情感分
    """
    if not text or len(text.strip()) < 3:
        return {asp: {"score": 50.0, "hasData": False} for asp in ASPECT_PATTERNS}

    sentences = split_sentences(text)
    nlp = get_pipeline()
    results = {}

    for aspect, pattern in ASPECT_PATTERNS.items():
        relevant = [s for s in sentences if pattern.search(s)]

        if not relevant:
            # 无相关句子：对全文做整体情感分析，结果向中性收缩
            if nlp:
                try:
                    probs = nlp(text, truncation=True, max_length=512)[0]
                    raw_score = _label_to_score(probs)
                    score = 50 + (raw_score - 50) * 0.4  # 收缩向50
                except Exception:
                    score = 50.0
            else:
                score = _fallback_score(text, aspect)["score"] * 0.5 + 25
            results[aspect] = {
                "score": round(score, 1),
                "hasData": False,
                "sentenceCount": 0,
                "method": "bert" if nlp else "fallback",
            }
        else:
            # 有相关句子：逐句 BERT 推断情感
            scores = []
            if nlp:
                for sent in relevant[:5]:  # 最多取5句避免超时
                    try:
                        # 这是句子分类器，并未学习指令前缀；直接使用原句避免改变其情感预测。
                        probs = nlp(sent, truncation=True, max_length=512)[0]
                        scores.append(_label_to_score(probs))
                    except Exception:
                        scores.append(50.0)
            else:
                for sent in relevant:
                    scores.append(_fallback_score(sent, aspect)["score"])

            avg_score = sum(scores) / len(scores) if scores else 50.0
            results[aspect] = {
                "score": round(avg_score, 1),
                "hasData": True,
                "sentenceCount": len(relevant),
                "relevantSentences": relevant[:3],
                "method": "bert" if nlp else "fallback",
            }

    return results


def extract_keywords(text: str) -> dict:
    """提取各维度中出现的关键词（用于报告摘要展示）"""
    keywords = {}
    for aspect, pattern in ASPECT_PATTERNS.items():
        found = pattern.findall(text)
        keywords[aspect] = list(dict.fromkeys(found))[:5]  # 去重取前5
    return keywords


# ============ API 路由 ============

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model_loaded": _pipeline is not None})


@app.route("/absa", methods=["POST"])
def absa_analyze():
    """
    请求体: {"text": "学生评语内容"}
    返回:
      {
        "dimensions": {
          "教学态度": {"score": 82.5, "hasData": true, "sentenceCount": 2},
          ...
        },
        "overallSentiment": "positive" | "neutral" | "negative",
        "overallScore": 78.3,
        "keywords": {"教学态度": ["认真", ...], ...}
      }
    """
    body = request.get_json(silent=True) or {}
    text = body.get("text", "").strip()

    if not text:
        return jsonify({"error": "text 不能为空"}), 400

    try:
        dimensions = analyze_absa(text)
        keywords = extract_keywords(text)

        # 综合情感分
        scores_with_data = [v["score"] for v in dimensions.values() if v.get("hasData")]
        overall_score = sum(scores_with_data) / len(scores_with_data) if scores_with_data else 50.0
        overall_score = round(overall_score, 1)

        if overall_score >= 70:
            sentiment = "positive"
        elif overall_score >= 40:
            sentiment = "neutral"
        else:
            sentiment = "negative"

        return jsonify({
            "dimensions": dimensions,
            "overallSentiment": sentiment,
            "overallScore": overall_score,
            "keywords": keywords,
            "analysisMethod": "bert" if _pipeline is not None else "fallback",
        })
    except Exception as e:
        logger.error(f"ABSA 分析出错: {e}", exc_info=True)
        return jsonify({"error": str(e)}), 500


@app.route("/batch-absa", methods=["POST"])
def batch_absa():
    """
    批量分析：[{"id": "xxx", "text": "..."}, ...]
    返回各评语的情感分析结果
    """
    body = request.get_json(silent=True) or {}
    items = body.get("items", [])
    if not items:
        return jsonify({"error": "items 不能为空"}), 400

    results = []
    for item in items[:50]:  # 最多50条
        text = item.get("text", "")
        analysis = analyze_absa(text)
        scores = [v["score"] for v in analysis.values() if v.get("hasData")]
        overall = sum(scores) / len(scores) if scores else 50.0
        results.append({
            "id": item.get("id"),
            "dimensions": analysis,
            "overallScore": round(overall, 1),
            "analysisMethod": "bert" if _pipeline is not None else "fallback",
        })

    return jsonify({"results": results})


if __name__ == "__main__":
    logger.info("启动 ABSA 情感分析服务，端口 5001...")
    # 预加载模型
    get_pipeline()
    app.run(host=os.getenv("NLP_HOST", "127.0.0.1"), port=int(os.getenv("NLP_PORT", "5001")), debug=False)
