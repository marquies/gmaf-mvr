# Retrieval experiment

Checks changes to the Graph Code metric, ranking and spatial relationships against the evaluation
of Wagenpfeil et al., *Graph Codes - 2D Projections of Multimedia Feature Graphs for Fast and
Effective Retrieval* (ICIVR 2021). The workflow is `.github/workflows/retrieval-experiment.yml`.

This is a regression experiment derived from that paper. It is not a replication, and its numbers
can't be compared with the paper's values.

## Running it locally

From this repository, with Java 21, Maven and Python 3 available:

```sh
bash experiments/run-experiment.sh                # k = 5,10,20,50,100
bash experiments/run-experiment.sh 1,5,10,25      # choose your own k
```

The first run downloads about 460 MB and verifies the checksum, later runs reuse
`~/.cache/gmaf-voc` (override with `VOC_HOME`). Results are printed and stored in
`target/voc-report.json`. In this workspace, prefix the command with `mise exec --`.

## Protocol

- **Corpus:** the 5011 PASCAL VOC2007 trainval annotations, pinned by SHA-256. Objects marked
  `difficult` are excluded.
- **Features:** gold annotations replace object detection, so results don't depend on a detector.
  Spatial edges come from `SpacialFeatureFusion`, Graph Codes from `GraphCodeGenerator`.
- **Term queries:** the paper's ten classes, one term each. Gold annotations make these 1.0 by
  construction, so they only detect breakage, and they can't detect a change to `M_FR` or `M_RT`.
- **Relation queries:** class pairs with `above`, `under` or `next_to`, with at least 10 relevant
  images. Relevance comes from the bounding boxes alone, independently of the code under test.
- **Measures:** results are the images sharing a query term. Per query, with `R` relevant images:
  - `P@k` is the relevant results in the top k, divided by k. Missing results count as misses.
  - `R@k` is the relevant results in the top k, divided by `R`.
  - `AP@k` sums the precision at each relevant position up to k, divided by `min(R, k)`.
  - `mAP@k` is the mean of `AP@k` over all queries, and `mAP` the same with no cutoff.

  Runtime by collection size is reported, not gated.
- **Rankings:** `production` runs `GraphCodeMetric` through `DefaultCollectionProcessor`. The two
  `paper_mrt_*` rankings run `PaperMetric`, the formulas as printed, sorted by M_F, M_FR, M_RT.
  Ties keep file name order.
- **Control:** `correctedLabels` repeats everything with `ABOVE` and `UNDER` swapped, because
  `SpacialFeatureFusion` labels the upper node `UNDER`.
- **Gate:** a pull request fails if `production` mAP on the production fusion drops by more than
  0.01 against its base revision, for either query group. A reviewed, intended change is accepted
  with the `experiment-accepted` label.

## Differences from the paper

| Paper | Here | Reason |
|---|---|---|
| GMAF object detection | Gold annotations | No detector is configured, and CI needs stable features |
| Ten classes, likely not VOC2007 | VOC2007 | The paper doesn't name the year. Its `rel` counts differ from VOC2007, and the official host wasn't reachable when this was set up |
| Single-noun queries | Single-term and relation queries | Single-term queries have no edges |
| Flickr30k, DIV2K, Neo4j runtimes | Not included | Hardware dependent, and Neo4j isn't part of this repository |

## Open questions in the paper's definition

- `M_RT` is described as a similarity, but the worked example counts edges that differ, so
  identical Graph Codes score 0. Both sort directions are reported.
- The printed `M_RT` sums absolute differences of type codes. That makes the result depend on the
  arbitrary numeric encoding of relationship types, and it can exceed 1.
- The printed `M_FR` uses only the second Graph Code's non-zero fields, ignoring which edges the
  query asks for.
