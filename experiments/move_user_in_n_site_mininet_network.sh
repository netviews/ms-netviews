#!/usr/bin/env bash 
#please chnage the exec command in Run-Experiment-${#} step os scenarios/run_experiment.xml to -c cli from -c run. 

# start the script like $ source <script-name>
# source experiments/move_user_in_n_site_mininet_network.sh 0.5 2 | tee all_experiments_logs.txt

#source experiments/move_user_in_n_site_mininet_network.sh 0.5 2 && sleep 30 && source experiments/move_user_in_n_site_mininet_network.sh 0.5 4 && sleep 30 && source experiments/move_user_in_n_site_mininet_network.sh 0.5 8 && sleep 30 && source experiments/move_user_in_n_site_mininet_network.sh 0.5 16

pushd ~/netviews-code/ && make netviews && make ifwd && popd

total_nodes_per_site=100
total_nodes=$(($2 * $total_nodes_per_site))

ps -aux | grep '[r]un_multisite_experiment' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9


sudo mn -c && ps -aux | grep '[w]atch' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9 && ps -aux | grep '[s]erver.py' | awk '{print $2}'| xargs --no-run-if-empty sudo kill -9

pushd ~/netviews-code/ && make vm-down && popd

pushd ~/netviews-code/topology-json/automation/ && ./generate_related_files.py -s "$2" -n "$total_nodes" -client_ratio 0.65 -server_ratio 0.35 -t ministanford -tc 1000 -et RR -r 50 && popd

cell netviews-cell

pushd ~/netviews-code/ && make vm-up && popd

pushd ~/central_admin/ && ./distribute_policies && popd

./scenarios/run_multisite_experiment -s "$2" -t ./topology-json/automation/ministanford_"$2"_sites/site -e ./experiments/netviews2_experiment_files/ministanford_single_site_throughput_experiment.json -m experiments/mininet_script -d experiments/ministanford_"$2"_sites_location_update_time -a org.onosproject.netviews -M '"-z MULTISITE -s 70 -A 30 -r 20 -T gre -I gretun_1 -l '$1'"'

#sleep 6000
#pushd ~/central_admin/ && ./move_users.py -i ministanfordh1HostIdentityToMoveToSite2.json -s "$2" -d experiments/ministanford_"$2"_sites_location_update_time && popd

