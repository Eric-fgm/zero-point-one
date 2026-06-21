# Project Progress — Food Chain with the 10% Energy Rule

Agent-Based Model (ABM) simulating a 5-level trophic food chain. Each predator gains
only **10%** of its prey's energy; the goal is to observe population fluctuations
(Lotka–Volterra style) and the survival probability of the 5th trophic level (apex predator).

Team: Eryk Hadała, Hieronim Koc, Kacper Drożdż
Tech: Kotlin Multiplatform + Compose Desktop (JVM), Coroutines, StateFlow.

See also **`MODEL.md`** — what the model is, the research question, what we vary/measure, the
state-of-the-art comparison, and the agent-behaviour decision.
See **`DOKUMENTACJA.md`** (Polish) — full description of our agent approach, implementation,
the simulator/GUI, a parameter how-to, and how to read the data. Feeds report sections d/e/f.

---

## Course steps

| Step | What | Status |
|------|------|--------|
| 1–2  | Topic + State of the Art | done |
| 3    | Agent Definition & Interaction Modelling (`Agent Definition and Interaction Modelling.pdf`) | done |
| 4    | Prototype / Proof of Concept | done |
| 5    | **Full implementation** | in progress |
| 6    | Final Report + presentation + source delivery | pending (done last) |

Instructor: pieta@agh.edu.pl — weekly progress updates are **mandatory** (MS Teams / e-mail / consultations).
Final report must be in English. Presentation 5–7 min (a live app demo is allowed instead of slides).

---

## How to run

Requires a JDK (17+; the repo is tested with Java 21). Gradle is provided via the wrapper.

```bash
./gradlew :composeApp:run
```

A desktop window opens. In the left sidebar enter **Width** and **Height** (e.g. 800 × 600),
optionally set **Speed**, then press **Run**. **Reset** clears the simulation.

> Note: the sidebar size fields start empty — pressing Run with empty fields crashes.
> Enter numbers first.

---

## Architecture (after M1)

The simulation logic is pure Kotlin and fully decoupled from the UI.

```
engine/
  Agent.kt            sealed Agent: Producer (L1) + Consumer (L2..L5). Holds energy + per-level update().
  SimulationConfig.kt SpeciesParams per trophic level + initial populations, carrying capacity, seed, speed.
  Engine.kt           single tick-based simulation loop (coroutine). Owns the agent list. Publishes StateFlow snapshots.
  Terrain.kt          world geometry (pixel size + hex cells for rendering) + per-tick spatial index builder.
  SpatialIndex.kt     neighbour lookups within a perception radius (hex spatial hash).
  StepContext.kt      per-tick context passed to each agent: spatial index, config, births/deaths queues, populations.
  Snapshot.kt         immutable SimulationSnapshot (agent render data + per-level counts) consumed by the UI.
enums/ConsumerLevel.kt  Primary..Quaternary mapped to trophic levels 2..5.
utils/                  Position (+ vector helpers), Hexagon (axial hex math).
views/                  Compose UI: App, Sidebar, HexagonGrid, AgentsView, ScrollableArea.
```

### Why a central tick loop (changed from the PoC)
The PoC ran one coroutine per agent. Once agents interact (shared grid reads/writes,
eating each other) that causes data races. M1 replaced it with a single discrete-time
scheduler: every tick the engine builds a fresh spatial index, lets every agent act on a
shuffled order, then applies queued births/deaths atomically. This is the standard ABM
approach (NetLogo / Mesa style), is deterministic given a seed, and makes speed control trivial.

### The 10% rule
`Agent.ENERGY_TRANSFER_YIELD = 0.10`. A consumer that eats prey gains exactly 10% of the
prey's *current* energy (capped at its own max). Prey are removed on consumption.
Producers (L1) photosynthesise (regrow energy each tick) up to a carrying capacity.

---

## Milestones

- **M1 — Core simulation engine** *(in progress)*: energy state on all agents, 5 trophic
  levels, hunt/flee movement, consumption with 10% transfer, starvation, reproduction,
  producer regrowth + carrying capacity, central tick loop, StateFlow snapshots, render by level.
- **M2 — Visualisation & controls** *(DONE)*: per-level colours/sizes, live population counter
  overlay (per level + total + tick), **live** speed slider (adjusts tick rate while running),
  Pause/Resume, Restart/Reset, and sidebar inputs for world size, seed, per-level initial
  populations, grass carrying capacity, and a global metabolism multiplier. Inputs are
  prefilled with sensible defaults so Run works immediately (no more empty-field crash).
- **M3 — Metrics & export** *(DONE)*: in-app log-scale population-vs-time chart (bottom panel,
  per-level lines — shows Lotka–Volterra cycles and the top-down collapse), CSV export button
  (writes `foodchain_populations_tick<N>.csv` to the working dir), and the **10% yield is now a
  config parameter** with a sidebar slider (5–40%) — enabling the flagship efficiency experiment.
- **M4 — Experiments** *(in progress — chose Option B)*:
  - *Behaviours added (done):* steering-vector movement with **herding** (cohesion + separation)
    and **gradient foraging**, both toggleable (`BehaviorConfig` + sidebar switches). Closes the
    Step-3 emergent-behaviour gap. See `MODEL.md` for the finding.
  - *Sweep runner (done):* headless experiment runner `experiments/Sweep.kt` + Gradle task
    `./gradlew :composeApp:sweep` (no GUI; uses `Engine.initialize()/advance()`). Runs three
    experiments — survival/persistence vs. energy-transfer yield, behaviours (herding × gradient),
    and metabolism — for all 5 levels, reporting **mean ± SD over seeds**, writing `results/exp{1,2,3}*.csv`.
    Charts: `python3 scripts/plot_sweep.py` (± 1 SD error bars; `--lang pl` for Polish) → `results/*.png`.
    Key results (in `ANALIZA.md` §2, 30 seeds / 1500 ticks): L5 persistence rises monotonically with
    yield (614→853 ticks for 5%→30%) and most strongly with lower metabolism (×0.6→1136, ×1.4→459).
    **Behaviours are NOT a robust lever** — at 30 seeds the four configs differ by < ±1 SD (the earlier
    "herding +28% on L4" was a small-sample artefact); they reshape spatial structure, not survival timing.
    Under a non-regenerating producer base, long-run survival of all consumer levels → 0 (collapse
    inevitable) — only its *timing* changes.
  - *Optional later:* see `ANALIZA.md` §3 (background grass spawn, correlated wander/dispersal).
- **M5 — Final Report + presentation**: reuse Step 3 PDF for the agents section; add problem
  description, State of the Art, GUI description + screenshots, results analysis (M4),
  conclusions / further research (ML, ethics). Build the 5–7 min presentation / live demo.

---

## Observed behaviour with the default parameters (seed 42 / 7, 800×600)
A headless 1000-tick run of the M1 defaults shows the expected trophic cascade:

- L1 producers and L2 herbivores oscillate (predator–prey style) for several hundred ticks.
- Upper levels go extinct in strict **top-down order** — L4 first (~tick 250), then L3.
- Losing the mid-level predator (L3) lets herbivores boom and **overgraze** the producers to
  zero (~tick 525), which then collapses the whole chain to total extinction (~tick 825).
- L5 never sustains a hunting population — it slowly starves from its initial energy because
  its prey (L4) collapses almost immediately.

This is scientifically faithful and *is* the project thesis: the higher the trophic level, the
lower its survival probability. It is rich material for the report's results section (M3/M4).

## Open tuning notes
Default `SpeciesParams` (in `SimulationConfig.kt`) are a reasonable starting point, not finely
tuned. Whether 5 levels can ever coexist, and for how long, under the hard 10% rule is exactly
the question to explore in the **M4** parameter sweeps — it is a result to measure, not a bug to fix.
