#!/usr/bin/env python3
"""Compares two retrieval experiment reports and fails when effectiveness drops."""
import json
import sys

GATED = ("terms", "relations")
VARIANTS = ("production", "paper_mrt_descending", "paper_mrt_ascending")
SETS = (("productionFusion", "Production fusion"), ("correctedLabels", "Corrected ABOVE/UNDER labels (control)"))


def table(head, base):
    lines = ["| Corpus | Ranking | Queries | mAP | Base mAP |", "|---|---|---|---|---|"]
    for key, title in SETS:
        for variant in VARIANTS:
            for group in GATED:
                current = head[key][variant][group]
                before = f"{base[key][variant][group]['mAP']:.4f}" if base else "n/a"
                lines.append(f"| {title} | {variant} / {group} | {current['queries']} | {current['mAP']:.4f} | {before} |")
    return lines


def cutoff_table(head):
    cutoffs = list(head["productionFusion"]["production"]["relations"]["atK"])
    columns = [f"{name}@{k}" for k in cutoffs for name in ("P", "R", "mAP")]
    lines = ["| Corpus | Ranking | " + " | ".join(columns) + " |", "|---|---|" + "---|" * len(columns)]
    for key, title in SETS:
        for variant in VARIANTS:
            for group in GATED:
                at_k = head[key][variant][group]["atK"]
                values = [f"{at_k[k][name]:.4f}" for k in cutoffs for name in ("P", "R", "mAP")]
                lines.append(f"| {title} | {variant} / {group} | " + " | ".join(values) + " |")
    return lines


def largest_changes(head, base, limit=5):
    current = head["productionFusion"]["production"]["relations"]["averagePrecision"]
    before = base["productionFusion"]["production"]["relations"]["averagePrecision"]
    changes = [(current[q] - before[q], q) for q in current if q in before and abs(current[q] - before[q]) > 1e-6]
    changes.sort(key=lambda change: abs(change[0]), reverse=True)
    return [f"- `{q}`: {delta:+.4f}" for delta, q in changes[:limit]]


def main():
    head = json.load(open(sys.argv[1]))
    base = json.load(open(sys.argv[2])) if len(sys.argv) > 2 and sys.argv[2] else None
    threshold = float(sys.argv[3]) if len(sys.argv) > 3 else 0.01

    print("## Retrieval experiment\n")
    print(f"{head['images']} PASCAL VOC images. Protocol: `experiments/retrieval-experiment.md`.\n")
    print("\n".join(table(head, base)))
    print("\n### Measures at k\n")
    print("\n".join(cutoff_table(head)))
    print(f"\nRuntime in ms by collection size (not gated): `{head['runtimeMillisByCollectionSize']}`")
    if not base:
        print("\nNo base report, nothing was gated.")
        return 0

    failed = []
    for group in GATED:
        before = base["productionFusion"]["production"][group]["mAP"]
        current = head["productionFusion"]["production"][group]["mAP"]
        if before - current > threshold:
            failed.append(f"{group}: mAP {before:.4f} -> {current:.4f}")

    changes = largest_changes(head, base)
    if changes:
        print("\n### Largest changes in average precision\n")
        print("\n".join(changes))
    if failed:
        print(f"\n**Failed:** mAP dropped by more than {threshold}: " + "; ".join(failed))
        return 1
    print(f"\n**Passed:** no mAP drop larger than {threshold}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
