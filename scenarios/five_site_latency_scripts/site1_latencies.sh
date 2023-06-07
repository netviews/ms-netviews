#!/usr/bin/env bash
set -euo pipefail

tc qdisc add dev wg0 parent root handle 1:0 htb default 1

# san francisco (site 1) -> san diego (site 2)
tc class add dev wg0 parent 1:0 classid 1:1 htb rate 100mbit
tc qdisc add dev wg0 parent 1:1 handle 30: netem  delay 16.68ms 1ms distribution normal
tc filter add dev wg0 protocol ip parent 1:0 prio 3 u32 match ip dst 172.20.32.10/24 flowid 1:1

# san francisco (site 1) -> chicago (site 3)
tc class add dev wg0 parent 1:0 classid 1:2 htb rate 100mbit
tc qdisc add dev wg0 parent 1:2 handle 31: netem  delay 52.90ms 1ms distribution normal
tc filter add dev wg0 protocol ip parent 1:0 prio 3 u32 match ip dst 172.20.33.10/24 flowid 1:2

# san francisco (site 1) -> atlanta (site 4)
tc class add dev wg0 parent 1:0 classid 1:3 htb rate 100mbit
tc qdisc add dev wg0 parent 1:3 handle 32: netem  delay 68.37ms 1ms distribution normal
tc filter add dev wg0 protocol ip parent 1:0 prio 3 u32 match ip dst 172.20.34.10/24 flowid 1:3

# san francisco (site 1) -> washington, dc (site 5)
tc class add dev wg0 parent 1:0 classid 1:4 htb rate 100mbit
tc qdisc add dev wg0 parent 1:4 handle 33: netem  delay 72.0ms 1ms distribution normal
tc filter add dev wg0 protocol ip parent 1:0 prio 3 u32 match ip dst 172.20.35.10/24 flowid 1:4
