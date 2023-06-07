#!/usr/bin/env bash 
#RQ1 (research question 1)
../bar_plot_latency -f RQ1_first_packet.csv -o RQ1_first_packet -t "title" -xp "Location of Sites" -yp "Latency (ms)" -y 120 -hue "Application" -col "Topology"
../bar_plot_latency  -f RQ1_nth_packet.csv -o RQ1_nth_packet -t "title" -xp "Location of Sites" -yp "Latency (ms)" -y 120 -hue "Application" -col "Topology"

#RQ2: (research question 2)
../bar_plot_latency -f RQ2_first_packet.csv -o RQ2_first_packet -t "title" -xp "Topology" -yp "Latency (ms)" -y 120 -hue "Traffic"
../bar_plot_latency -f RQ2_nth_packet.csv -o RQ2_nth_packet -t "title" -xp "Topology" -yp "Latency (ms)" -y 120 -hue "Traffic"

../bar_plot_latency -f RQ2.csv -o RQ2 -t "title" -xp "Topology" -yp "Latency (ms)" -y 120 -hue "Traffic" -col "Metric"
