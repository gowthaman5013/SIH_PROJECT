import os
import sys
import json

# Add project root to sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.inference import TextMLInferencePipeline

def run_all_test_cases():
    test_cases_path = os.path.join(os.path.dirname(__file__), "test_cases.json")
    with open(test_cases_path, "r") as f:
        cases = json.load(f)
        
    print(f"==================================================")
    print(f"RUNNING TEXT ML CONTEXT & NEGATION TEST SUITE ({len(cases)} Cases)")
    print(f"==================================================\n")

    pipeline = TextMLInferencePipeline()
    passed_count = 0

    for case in cases:
        cid = case["id"]
        cname = case["name"]
        text = case["text"]
        expected_high = case.get("expected_high", [])
        expected_low = case.get("expected_low", [])

        result = pipeline.predict(text)
        indicators = result["indicators"]
        text_score = result["text_score"]
        flags = result["critical_flags"]

        case_passed = True
        failed_reasons = []

        # Check expected high probabilities (>= 0.5)
        for label in expected_high:
            prob = indicators.get(label, 0.0)
            if prob < 0.5:
                case_passed = False
                failed_reasons.append(f"Expected HIGH for '{label}' (>=0.5), got {prob:.2f}")

        # Check expected low probabilities (< 0.5)
        for label in expected_low:
            prob = indicators.get(label, 0.0)
            if prob >= 0.5:
                case_passed = False
                failed_reasons.append(f"Expected LOW for '{label}' (<0.5), got {prob:.2f}")

        status = "PASSED [OK]" if case_passed else "FAILED [X]"
        if case_passed:
            passed_count += 1

        print(f"[{cid}] {cname}: {status}")
        print(f"  Input: \"{text}\"")
        print(f"  Text Score: {text_score:.1f} / 100")
        print(f"  Active Indicators (>=0.5): {[k for k, v in indicators.items() if v >= 0.5]}")
        if flags:
            print(f"  Critical Safety Flags: {[f['indicator'] for f in flags]}")
        if failed_reasons:
            print(f"  Failures: {failed_reasons}")
        print("-" * 50)

    print(f"\nTEST SUITE SUMMARY: {passed_count} / {len(cases)} PASSED ({(passed_count/len(cases))*100:.1f}%)")
    print(f"==================================================")
    return passed_count == len(cases)

if __name__ == "__main__":
    run_all_test_cases()
