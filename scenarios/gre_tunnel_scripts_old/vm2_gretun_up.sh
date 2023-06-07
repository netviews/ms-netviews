#!/usr/bin/env bash
set -euo pipefail

ip tunnel add gretun-1 mode gre remote 192.168.55.10 ttl 64 dev eth0
ip tunnel add gretun-2 mode gre remote 192.168.55.30 ttl 64 dev eth0

ip add add dev gretun-1 10.0.0.2 peer 10.0.0.1/32
ip add add dev gretun-2 10.0.1.2 peer 10.0.1.3/32

ip link set dev gretun-1 up
ip link set dev gretun-2 up

ip route add 172.20.31.0/24 via 10.0.0.2
ip route add 172.20.33.0/24 via 10.0.1.2
