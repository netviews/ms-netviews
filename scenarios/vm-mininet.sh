#!/usr/bin/env bash

echo "=== Copying files ==="
scp experiments/mininet_script "$ONOS_USER"@"$1":
scp "$2" "$ONOS_USER"@"$1":/tmp/topology
scp "$3" "$ONOS_USER"@"$1":/tmp/identity_mapping.json
scp "$4" "$ONOS_USER"@"$1":/tmp/prohibition.json
scp "$5" "$ONOS_USER"@"$1":/tmp/locationChangeToSite1.yml
scp "$6" "$ONOS_USER"@"$1":/tmp/locationChangeToSite2.yml
scp "$7" "$ONOS_USER"@"$1":/tmp/locationChangeToSite3.yml
echo "=== Launching mininet on $1 ==="
exec ssh -t "$ONOS_USER"@"$1" ./mininet_script -t /tmp/topology -c cli -d nil -a nil -z True --tunnel wireguard
