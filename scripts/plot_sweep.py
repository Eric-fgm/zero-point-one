#!/usr/bin/env python3
"""Render charts from the headless parameter-sweep CSVs.

Reads results/exp{1,2,3}*.csv (produced by `./gradlew :composeApp:sweep`) and
writes one PNG per experiment into results/. Dependency-light: standard-library
csv + matplotlib only (no pandas).

Usage:
    python3 scripts/plot_sweep.py              # English labels (for the report)
    python3 scripts/plot_sweep.py --lang pl    # Polish labels (working docs)

The "persist" metric is the mean last tick at which a trophic level still
existed (higher = the level survived longer). Consumer end-survival is 0 in
every configuration, so duration — not binary survival — is the meaningful axis.
"""
from __future__ import annotations

import argparse
import csv
import os
import sys

import matplotlib

matplotlib.use("Agg")  # headless: no display needed
import matplotlib.pyplot as plt

# Trophic-level colours, matching the GUI palette (views/LevelPalette.kt).
LEVEL_COLOR = {
    1: "#4CAF50",  # producer  - green
    2: "#FFEB3B",  # herbivore - yellow
    3: "#FF9800",  # small carnivore - orange
    4: "#F44336",  # large carnivore - red
    5: "#9C27B0",  # apex predator - purple
}

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RESULTS = os.path.join(ROOT, "results")

LABELS = {
    "en": {
        "level": {1: "L1 Producers", 2: "L2 Herbivores", 3: "L3 Small carniv.",
                  4: "L4 Large carniv.", 5: "L5 Apex"},
        "persist_y": "Persistence (mean last tick alive)",
        "exp1_title": "Level persistence vs. energy-transfer yield",
        "exp1_x": "Energy-transfer yield",
        "exp3_title": "Level persistence vs. metabolism multiplier",
        "exp3_x": "Metabolism multiplier",
        "exp2_title": "Effect of behaviours on persistence",
        "exp2_x": "Behaviour configuration",
        "beh": {"00": "none", "0g": "gradient", "h0": "herding", "hg": "herding+grad"},
    },
    "pl": {
        "level": {1: "L1 Producenci", 2: "L2 Roślinożercy", 3: "L3 Małe drap.",
                  4: "L4 Duże drap.", 5: "L5 Szczyt"},
        "persist_y": "Trwałość (średni ostatni krok życia)",
        "exp1_title": "Trwałość poziomów a wydajność transferu energii",
        "exp1_x": "Wydajność transferu energii",
        "exp3_title": "Trwałość poziomów a mnożnik metabolizmu",
        "exp3_x": "Mnożnik metabolizmu",
        "exp2_title": "Wpływ zachowań na trwałość",
        "exp2_x": "Konfiguracja zachowań",
        "beh": {"00": "brak", "0g": "gradient", "h0": "stadność", "hg": "stadność+grad"},
    },
}


def read_csv(name: str) -> list[dict[str, str]]:
    path = os.path.join(RESULTS, name)
    if not os.path.exists(path):
        sys.exit(f"Missing {path} — run ./gradlew :composeApp:sweep first.")
    with open(path, newline="") as fh:
        return list(csv.DictReader(fh))


def stddev(row: dict[str, str], level: int) -> float:
    """Persistence std dev for a level; 0 if the CSV predates the *_std columns."""
    return float(row.get(f"L{level}_persist_std") or 0.0)


def line_plot(rows, x_col, levels, title, x_label, y_label, out):
    xs = [float(r[x_col]) for r in rows]
    fig, ax = plt.subplots(figsize=(7, 4.2))
    for lvl in levels:
        ys = [float(r[f"L{lvl}_persist"]) for r in rows]
        errs = [stddev(r, lvl) for r in rows]
        ax.errorbar(xs, ys, yerr=errs, marker="o", capsize=3, color=LEVEL_COLOR[lvl],
                    label=LBL["level"][lvl])
    ax.set_title(title)
    ax.set_xlabel(x_label)
    ax.set_ylabel(y_label)
    ax.grid(True, alpha=0.3)
    ax.legend()
    fig.tight_layout()
    fig.savefig(out, dpi=150)
    plt.close(fig)
    print(f"wrote {out}")


def bar_plot(rows, levels, title, x_label, y_label, out):
    cats = [LBL["beh"][("h" if r["herding"] == "true" else "0")
                        + ("g" if r["gradient"] == "true" else "0")] for r in rows]
    x = range(len(rows))
    width = 0.8 / len(levels)
    fig, ax = plt.subplots(figsize=(7, 4.2))
    for i, lvl in enumerate(levels):
        ys = [float(r[f"L{lvl}_persist"]) for r in rows]
        errs = [stddev(r, lvl) for r in rows]
        offs = [xi + (i - (len(levels) - 1) / 2) * width for xi in x]
        ax.bar(offs, ys, width=width, yerr=errs, capsize=2,
               color=LEVEL_COLOR[lvl], label=LBL["level"][lvl])
    ax.set_title(title)
    ax.set_xlabel(x_label)
    ax.set_ylabel(y_label)
    ax.set_xticks(list(x))
    ax.set_xticklabels(cats)
    ax.grid(True, axis="y", alpha=0.3)
    top = max(float(r[f"L{lvl}_persist"]) + stddev(r, lvl) for r in rows for lvl in levels)
    ax.set_ylim(0, top * 1.08)  # small headroom above the error caps
    # Legend outside (right) so the four long labels never overlap the bars.
    ax.legend(loc="center left", bbox_to_anchor=(1.0, 0.5), fontsize=9)
    fig.tight_layout()
    fig.savefig(out, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"wrote {out}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--lang", choices=["en", "pl"], default="en")
    args = ap.parse_args()

    global LBL
    LBL = LABELS[args.lang]
    sfx = args.lang  # language suffix so en/pl outputs coexist (e.g. exp1_yield_en.png)

    exp1 = read_csv("exp1_yield.csv")
    line_plot(exp1, "yield", [1, 2, 3, 4, 5], LBL["exp1_title"], LBL["exp1_x"],
              LBL["persist_y"], os.path.join(RESULTS, f"exp1_yield_{sfx}.png"))

    exp2 = read_csv("exp2_behaviours.csv")
    bar_plot(exp2, [2, 3, 4, 5], LBL["exp2_title"], LBL["exp2_x"],
             LBL["persist_y"], os.path.join(RESULTS, f"exp2_behaviours_{sfx}.png"))

    exp3 = read_csv("exp3_metabolism.csv")
    line_plot(exp3, "metabolism", [2, 3, 4, 5], LBL["exp3_title"], LBL["exp3_x"],
              LBL["persist_y"], os.path.join(RESULTS, f"exp3_metabolism_{sfx}.png"))


if __name__ == "__main__":
    main()
