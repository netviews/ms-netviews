#!/usr/bin/env bash
set -euo pipefail

ip tunnel del gretun-1
ip tunnel del gretun-2
