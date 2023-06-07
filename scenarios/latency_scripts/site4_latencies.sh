#!/usr/bin/env bash
set -euo pipefail

tc qdisc add dev wg0 parent root handle 1:0 htb default 1

# atlanta (site 4) -> washington, dc (site 5)
tc class add dev wg0 parent 1:0 classid 1:4 htb rate 100mbit
tc qdisc add dev wg0 parent 1:4 handle 33: netem  delay 16.15ms 1ms distribution normal
tc filter add dev wg0 protocol ip parent 1:0 prio 3 u32 match ip dst 172.20.35.10/24 flowid 1:4
