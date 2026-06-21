# The Model — what we simulate and what we measure

This document explains *what* the simulation is, *what question* it answers, and *what we vary*
to produce results. It is written to feed directly into the Final Report (problem description,
state of the art, agent definition, results, further research).

---

## 1. What the model is

A **spatial, energy-explicit, agent-based model (ABM)** of a 5-level food chain on a 2D grid.
Every organism is an autonomous agent with its own energy budget. The single mechanism that
drives the whole system is the **ten-percent law of energy transfer** (Lindeman, 1942): a
consumer captures only ~10% of the energy stored in the prey it eats; the other ~90% is lost
as heat / metabolism / waste and never reaches the next level up.

From that one rule, plus simple per-tick energy bookkeeping, the macro-behaviour emerges.

**Agents**
- **L1 Producers (autotrophs / grass):** gain energy by "photosynthesis" each tick, spread up
  to a carrying capacity, do not move or hunt. The sole energy source for L2.
- **L2–L5 Consumers:** each tick they (1) pay a metabolic energy cost, then act in priority
  order — **flee** the nearest predator (level n+1), else **hunt and eat** the nearest prey
  (level n−1), else **wander**. Eating transfers exactly the configured yield (default 10%)
  of the prey's current energy; the prey dies. Zero energy ⇒ starvation death. Surplus energy
  above a threshold ⇒ reproduction (a child is spawned and the parent pays an energy cost).

**Environment:** a hexagonal spatial index makes "who is near me" queries cheap. Interactions
are strictly **local** — a predator can only eat prey within a short distance, and only
perceives others within its perception radius.

This is a discrete-time model: a central scheduler advances every agent once per **tick**.

---

## 2. What we are checking (the research question)

> **Under the harsh 10% energy law, can a 5-level food chain persist — and what determines the
> survival probability of the fifth trophic level (the apex predator)?**

This is exactly the project's stated aim: *illustrate population fluctuations from the 10% law
and present the survival probability of the fifth trophic level.*

### Dependent variables (what we measure / the outputs)
- **Population of each trophic level over time** — and its qualitative regime: coexistence,
  oscillation (Lotka–Volterra-like cycles), top-down collapse, or total extinction.
- **Survival / persistence of the upper levels**, especially L5: how long it lasts, or whether
  it can sustain a hunting population at all.

### Independent variables (what we vary to get results)
- **Energy transfer yield** (the 10% itself — now a parameter): the flagship axis.
- Metabolic rates (via a global multiplier), reproduction thresholds/costs.
- Producer carrying capacity / regrowth (the size of the energy base).
- Initial populations per level.
- Perception radius and move speed (foraging efficiency).
- World size / agent density, and the random seed (for repeated runs → *probabilities*).

---

## 3. How this differs from the state of the art

The novelty is **structural**, not in exotic behaviours:

| Baseline | What it is | What our model adds |
|---|---|---|
| **Lindeman's 10% law** (1942) | A static energy-pyramid principle | Turns it into *dynamics over time* and *space* |
| **Lotka–Volterra** | Mean-field ODE predator–prey (2 species, no space) | Individual-based and *spatially explicit* |
| **NetLogo Wolf–Sheep–Grass** | Canonical ABM, 2–3 levels | **Five** levels with *explicit energy accounting* and the 10% rule enforced at every step |

So we do **not** need extra behaviours to be distinct from prior work — the spatial, 5-level,
energy-explicit formulation is the contribution. Additional behaviours (below) are about
*richness of dynamics*, not validity.

---

## 4. Expected emergent behaviours (claimed in Step 3)

Our Step-3 design document promised three emergent phenomena. Status in the current model:

1. **Lotka–Volterra cycles** — *present* (L1↔L2 oscillate before upper levels fail).
2. **Spatial clustering / herding** — *implemented* (Option B). Consumers steer toward the
   centroid of same-level peers (cohesion) with a separation term to avoid collapsing to a point.
   Toggleable via `BehaviorConfig.herding` and the sidebar switch.
3. **Optimised foraging paths** — *implemented* (Option B). With `gradientForaging` on, predators
   steer toward inverse-distance-weighted prey *density* rather than the single nearest individual.

Movement is now a blended **steering-vector** model (flee + hunt + cohesion + separation),
which is what makes these behaviours composable.

### Finding (confirmed by the M4 sweep — 30 seeds, 1500 ticks)
- An early small-sample hint that herding extends **L4** persistence does **not** survive a larger
  sample: across 30 seeds the four behaviour configurations differ by less than one standard
  deviation (L4 ≈ 372–412 ticks, SD ≈ 77–117). The behaviours reshape the **spatial** structure
  (clustering / shoaling) but do **not** change survival timing under the hard energy rule.
- **L5 is unaffected** (~651–661 ticks, SD ~21) and tightly so: its prey (L4) collapses early, after
  which L5 merely starves on its initial reserves. The apex's fate is set by the level below it, not
  by its own behaviour — a direct illustration of the 10% bottleneck. See `ANALIZA.md` §2.2.

---

## 5. Do we need more behaviours? — decision

**No, not to be valid.** The 10% rule + energy bookkeeping + local spatial foraging already
produce the target phenomena (cascade, apex fragility, lower-level oscillation), and they
support a real parameter study — which is what the report is graded on.

The choice for the analysis phase (M4) is about *ambition*:

- **Option A — Tuning only.** Use parameter sweeps to find coexistence / oscillation regimes,
  then measure how outcomes shift with each parameter. Sufficient for a strong report.
- **Option B — Add 1–2 behaviours.** Prey **herding** (anti-predator safety in numbers) and/or
  **gradient foraging** (move toward prey density, not just the nearest individual). These
  stabilise dynamics, enable longer coexistence, close the Step-3 gap, and add new questions
  (e.g. "does herding let more trophic levels survive?"). ~Half a session of work.

> Guard-rail: the model's purpose is the **energy bottleneck**. Keep that central; resist
> turning it into a generic ecosystem sim.

### The one change worth making regardless
Make the **energy transfer yield a parameter** (done in M3). That unlocks the single most
compelling experiment — *L5 survival probability vs. transfer efficiency (5% / 10% / 20% / 30%)* —
which tests the 10% law directly instead of merely assuming it. This graph is the heart of the
report's results section.

**Open decision:** A or B for M4. Charts + CSV export (M3) and the yield parameter are being
built first because both options need them.
