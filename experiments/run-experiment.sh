#!/usr/bin/env bash
# Runs the retrieval experiment locally, see experiments/retrieval-experiment.md
# usage: run-experiment.sh [comma separated k values]
set -euo pipefail

cd "$(dirname "$0")/.."

k="${1:-5,10,20,50,100}"
voc_home="${VOC_HOME:-$HOME/.cache/gmaf-voc}"
voc_url="${VOC_URL:-https://pjreddie.com/media/files/VOCtrainval_06-Nov-2007.tar}"
voc_sha256="7d8cd951101b0957ddfd7a530bdc8a94f06121cfc1e511bb5937e973020c7508"
annotations="$voc_home/VOCdevkit/VOC2007/Annotations"
report="$PWD/target/voc-report.json"

if [[ ! "$k" =~ ^[1-9][0-9]*(,[1-9][0-9]*)*$ ]]; then
  echo "k must be a comma separated list of positive numbers, got: $k" >&2
  exit 2
fi

if [ ! -d "$annotations" ]; then
  mkdir -p "$voc_home"
  echo "Downloading VOC2007 annotations to $voc_home" >&2
  curl -fsSL --retry 3 "$voc_url" -o "$voc_home/voc.tar"
  if ! echo "$voc_sha256  $voc_home/voc.tar" | shasum -a 256 -c - >/dev/null; then
    rm -f "$voc_home/voc.tar"
    echo "Checksum mismatch, the download was deleted" >&2
    exit 1
  fi
  tar xf "$voc_home/voc.tar" -C "$voc_home" VOCdevkit/VOC2007/Annotations
  rm "$voc_home/voc.tar"
fi

mvn -B -q test -Dtest=VocRetrievalExperimentTest -Dvoc.annotations="$annotations" \
  -Dvoc.report="$report" -Dvoc.k="$k"
python3 .github/scripts/compare_experiment.py "$report"
