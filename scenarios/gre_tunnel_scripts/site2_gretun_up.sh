#!/usr/bin/env bash
set -euo pipefail
ip tunnel add gretun_1 mode gre remote 192.168.55.1 ttl 64 dev eth0
ip addr add dev gretun_1 10.0.0.2 peer 10.0.0.1/32
ip link set dev gretun_1 up
ip route add 172.20.1.0/24 via 10.0.0.2
