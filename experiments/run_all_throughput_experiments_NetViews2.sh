#!/usr/bin/env bash 
# start the script like $ source <script-name>
# source experiments/run_all_throughput_experiments_NetViews2.sh 0.5 | tee all_experiments_logs.txt

#set -e -x

pushd ~/netviews-code/ && make netviews && make ifwd && popd  

#lsof -i :8101 |awk '{print $2}'|tail -n +2 | xargs --no-run-if-empty sudo kill -9

#running ministanford two sites 

#: << 'END'

ps -aux | grep '[r]un_multisite_experiment' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9

sudo mn -c && ps -aux | grep '[w]atch' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9 && ps -aux | grep '[s]erver.py' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9

pushd ~/netviews-code/ && make vm-down && popd

pushd ~/netviews-code/topology-json/automation/ && ./generate_related_files.py -s 2 -n 200 -client_ratio 0.65 -server_ratio 0.35 -t ministanford -tc 1000 -et RR -r 50 && popd

cell netviews-cell

pushd ~/netviews-code/ && make vm-up && popd

pushd ~/central_admin/ && ./distribute_policies && popd

pushd ~/netviews-code && ./scenarios/run_multisite_experiment -s 2 -t ./topology-json/automation/ministanford_2_sites/site -e ./experiments/netviews2_experiment_files/ministanford_two_site_throughput_experiment.json -m experiments/mininet_script -d ministanford_2_site_netviews_throughput_experiment_round -a org.onosproject.netviews -M '"-z MULTISITE -s 70 -A 30 -f -r 20 -T gre -I gretun_1 -l '$1'"' && sleep 180 && ./scenarios/run_multisite_experiment -s 2 -t ./topology-json/automation/ministanford_2_sites/site -e ./experiments/netviews2_experiment_files/ministanford_two_site_throughput_experiment.json -m experiments/mininet_script -d ministanford_2_site_ifwd_throughput_experiment_round -a org.onosproject.ifwd -M '"-z MULTISITE -s 70 -A 30  -f -r 20 -T gre -I gretun_1 -l '$1'"' && popd

sleep 180

#END

#running ministanford single site

#: << 'END'

ps -aux | grep '[r]un_multisite_experiment' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9

sudo mn -c && ps -aux | grep '[w]atch' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9 && ps -aux | grep '[s]erver.py' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9

pushd ~/netviews-code/ && make vm-down && popd

pushd ~/netviews-code/topology-json/automation/ && ./generate_related_files.py -s 1 -n 100 -client_ratio 0.65 -server_ratio 0.35 -t ministanford -tc 1000 -et RR -r 50 && popd

cell netviews-cell

pushd ~/netviews-code/ && make vm-up && popd

pushd ~/central_admin/ && ./distribute_policies && popd

pushd ~/netviews-code && ./scenarios/run_multisite_experiment -s 1 -t ./topology-json/automation/ministanford_1_sites/site -e ./experiments/netviews2_experiment_files/ministanford_single_site_throughput_experiment.json -m experiments/mininet_script -d ministanford_1_site_netviews_throughput_experiment_round -a org.onosproject.netviews -M '"-s 70 -A 30 -f -r 20 -T gre -I gretun_1 -l '$1'"' && sleep 180 && ./scenarios/run_multisite_experiment -s 1 -t ./topology-json/automation/ministanford_1_sites/site -e ./experiments/netviews2_experiment_files/ministanford_single_site_throughput_experiment.json -m experiments/mininet_script -d ministanford_1_site_ifwd_throughput_experiment_round -a org.onosproject.ifwd -M '"-s 70 -A 30 -f -r 20 -T gre -I gretun_1 -l '$1'"' && popd

sleep 180

#END

#running cisco two sites

#: << 'END'

ps -aux | grep '[r]un_multisite_experiment' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9

sudo mn -c && ps -aux | grep '[w]atch' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9 && ps -aux | grep '[s]erver.py' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9

pushd ~/netviews-code/ && make vm-down && popd

pushd ~/netviews-code/topology-json/automation/ && ./generate_related_files.py -s 2 -n 24 -client_ratio 0.34 -server_ratio 0.67 -t cisco -tc 1000 -et RR -r 50 && popd

cell netviews-cell

pushd ~/netviews-code/ && make vm-up && popd

pushd ~/central_admin/ && ./distribute_policies && popd

pushd ~/netviews-code && ./scenarios/run_multisite_experiment -s 2 -t ./topology-json/automation/cisco_2_sites/site -e ./experiments/netviews2_experiment_files/cisco_two_site_throughput_experiment.json -m experiments/mininet_script -d cisco_2_site_netviews_throughput_experiment_round -a org.onosproject.netviews -M '"-z MULTISITE -s 70 -A 30 -f -r 20 -T gre -I gretun_1 -l '$1'"' && sleep 180 && ./scenarios/run_multisite_experiment -s 2 -t ./topology-json/automation/cisco_2_sites/site -e ./experiments/netviews2_experiment_files/cisco_two_site_throughput_experiment.json -m experiments/mininet_script -d cisco_2_site_ifwd_throughput_experiment_round -a org.onosproject.ifwd -M '"-z MULTISITE -s 70 -A 30 -f -r 20 -T gre -I gretun_1 -l '$1'"' && popd

sleep 180

#END

#running cisco single site

#: << 'END'

ps -aux | grep '[r]un_multisite_experiment' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9

sudo mn -c && ps -aux | grep '[w]atch' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9 && ps -aux | grep '[s]erver.py' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9

pushd ~/netviews-code/ && make vm-down && popd

pushd ~/netviews-code/topology-json/automation/ && ./generate_related_files.py -s 1 -n 12 -client_ratio 0.34 -server_ratio 0.67 -t cisco -tc 1000 -et RR -r 50 && popd

cell netviews-cell

pushd ~/netviews-code/ && make vm-up && popd

pushd ~/central_admin/ && ./distribute_policies && popd

pushd ~/netviews-code && ./scenarios/run_multisite_experiment -s 1 -t ./topology-json/automation/cisco_1_sites/site -e ./experiments/netviews2_experiment_files/cisco_single_site_throughput_experiment.json -m experiments/mininet_script -d cisco_1_site_netviews_throughput_experiment_round -a org.onosproject.netviews -M '"-s 70 -A 30 -f -r 20 -T gre -I gretun_1 -l '$1'"' && sleep 180 && ./scenarios/run_multisite_experiment -s 1 -t ./topology-json/automation/cisco_1_sites/site -e ./experiments/netviews2_experiment_files/cisco_single_site_throughput_experiment.json -m experiments/mininet_script -d cisco_1_site_ifwd_throughput_experiment_round -a org.onosproject.ifwd -M '"-s 70 -A 30 -f -r 20 -T gre -I gretun_1 -l '$1'"' && popd

sleep 180

#END
