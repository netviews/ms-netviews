#!/usr/bin/env bash
set -euo pipefail

# Copy scenarios and cells to ONOS
cp scenarios/*.xml "$ONOS_ROOT/tools/test/scenarios/"

# Start the docker containers
sudo docker-compose up & 

sleep 300
stc netviews_setup
stc setup
stc install_netviews
