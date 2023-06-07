#!/usr/bin/env bash 
#1 ms WAN
../box_plot_throughput -f ./cisco_2_sites_1ms_wan.csv -t "Cisco" -o ./aggregate_throughput_cisco_2_sites_1ms_wan -ly 947 -uy 965
../box_plot_throughput -f ./ministanford_2_sites_1ms_wan.csv -t "Ministanford" -o ./aggregate_throughput_ministanford_2_sites_1ms_wan -ly 2400 -uy 3200
#105 ms WAN
../box_plot_throughput -f ./cisco_2_sites_105ms_wan.csv -t "Cisco" -o ./aggregate_throughput_cisco_2_sites_105ms_wan -ly 880 -uy 1000
../box_plot_throughput -f ./ministanford_2_sites_105ms_wan.csv -t "Ministanford" -o ./aggregate_throughput_ministanford_2_sites_105ms_wan -ly 300 -uy 500

#combined
../box_plot_throughput -f ./combined.csv -t "" -o ./aggregate_throughput_combined -ly 300 -uy 3200