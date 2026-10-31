"""Regression checks for the real model label contract; no model download required."""
import unittest
from unittest.mock import patch

import app


class SentimentContractTests(unittest.TestCase):
    def test_actual_binary_model_labels_do_not_collapse_to_neutral(self):
        positive = [
            {"label": "negative (stars 1, 2 and 3)", "score": 0.1},
            {"label": "positive (stars 4 and 5)", "score": 0.9},
        ]
        negative = [{**item, "score": 1 - item["score"]} for item in positive]
        self.assertEqual(app._label_to_score(positive), 90.0)
        self.assertEqual(app._label_to_score(negative), 10.0)

    def test_unknown_labels_fail_instead_of_silently_returning_50(self):
        with self.assertRaises(ValueError):
            app._label_to_score([{"label": "unexpected", "score": 1}])

    def test_fallback_response_identifies_its_method(self):
        with patch.object(app, "get_pipeline", return_value=None):
            response = app.app.test_client().post("/absa", json={"text": "老师认真负责，讲解清晰。"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json["analysisMethod"], "fallback")
        self.assertEqual(response.json["dimensions"]["教学态度"]["method"], "fallback")

    def test_empty_comment_is_rejected(self):
        response = app.app.test_client().post("/absa", json={"text": ""})
        self.assertEqual(response.status_code, 400)

    def test_classifier_receives_the_original_sentence(self):
        from unittest.mock import Mock
        classifier = Mock(return_value=[[{"label": "positive (stars 4 and 5)", "score": 1}]])
        with patch.object(app, "get_pipeline", return_value=classifier):
            result = app.analyze_absa("老师认真负责。")
        self.assertEqual(classifier.call_args_list[0].args[0], "老师认真负责")
        self.assertEqual(result["教学态度"]["score"], 100)


if __name__ == "__main__":
    unittest.main()
