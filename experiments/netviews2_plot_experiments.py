#!/usr/bin/env python3
import time
import os
import argparse
import subprocess
import csv
import argparse
LATENCY="latency"

#sample usage: ./experiments/netviews2_plot_experiments.py -exp <thrpt/latency> -runs <number of runs of eatch experiement>

def runCmd(cmd):
	print("Running Command: "+cmd)
	return subprocess.call(cmd, shell=True)

def run_experiments(app_name ,site_count ,experiment_type,topology):
    global LATENCY

    TOTAL_RUNS = 50
    for run_number in range(1,TOTAL_RUNS+1):
        if experiment_type==LATENCY:
            outputResultsDir = topology.split(".")[2] + "_" + str(
                site_count) + "_site_" + app_name + "_latency_experiment_round" + str(run_number)
            runCmd(
                "scenarios/run_multisite_experiment -s "+str(site_count)+" -t ~/netviews-code/topology-json/automation/"+topology+"_"
                +str(site_count)+"_sites/site -e ~/netviews-code/topology-json/automation/"+topology+"_"+str(site_count)
                +"_sites/latency_experiments/latency_experiment_round"+str(run_number)+".json -m ~/netviews-code/experiments/mininet_script -d "+outputResultsDir+" -a "+app_name+" -M '\"-z MULTISITE -s 70 -A 20 -f -r 1 -w 0.1 -T gre -I gretun_1 -l 0.5\"'")
            runCmd("~/netviews-code/experiments/mtr_to_csv -f ~/netviews-code/"+outputResultsDir+"/client_* -n {1..65}_{1..35} -o ~/parsed-results/netviews.csv -r")

def plot_throughput_experiments():
    script_dir = '~/netviews-code/experiments'

    #ministanford_2_site

    runCmd(script_dir+'/iperf3_to_csv -n ministanford_2_site_netviews -f ministanford_2_site_netviews_throughput_experiment_round/client_* -o ~/parsed-results/ministanford_2_site_netviews_throughput_experiment.csv')
    runCmd(script_dir+'/iperf3_to_csv -n ministanford_2_site_ifwd -f ministanford_2_site_ifwd_throughput_experiment_round/client_* -o ~/parsed-results/ministanford_2_site_ifwd_throughput_experiment.csv')

#ministanford_1_site

    runCmd(script_dir+'/iperf3_to_csv -n ministanford_1_site_netviews -f ministanford_1_site_netviews_throughput_experiment_round/client_* -o ~/parsed-results/ministanford_1_site_netviews_throughput_experiment.csv')
    runCmd(script_dir+'/iperf3_to_csv -n ministanford_1_site_ifwd -f ministanford_1_site_ifwd_throughput_experiment_round/client_* -o ~/parsed-results/ministanford_1_site_ifwd_throughput_experiment.csv')

# cisco_2_site

    runCmd(script_dir+'/iperf3_to_csv -n cisco_2_site_netviews -f cisco_2_site_netviews_throughput_experiment_round/client_* -o ~/parsed-results/cisco_2_site_netviews_throughput_experiment.csv')
    runCmd(script_dir+'/iperf3_to_csv -n cisco_2_site_ifwd -f cisco_2_site_ifwd_throughput_experiment_round/client_* -o ~/parsed-results/cisco_2_site_ifwd_throughput_experiment.csv')

# cisco_1_site

    runCmd(script_dir+'/iperf3_to_csv -n cisco_1_site_netviews -f cisco_1_site_netviews_throughput_experiment_round/client_* -o ~/parsed-results/cisco_1_site_netviews_throughput_experiment.csv')
    runCmd(script_dir+'/iperf3_to_csv -n cisco_1_site_ifwd -f cisco_1_site_ifwd_throughput_experiment_round/client_* -o ~/parsed-results/cisco_1_site_ifwd_throughput_experiment.csv')

def plot_latency_experiments(total_runs):

    script_dir='~/netviews-code/experiments'

    #ministanford_2_site

    runCmd(script_dir+'/multisite_ping_to_csv.py -f ministanford_2_site_netviews_latency_experiment_round/client_* -o ~/parsed-results/ministanford_2_site_netviews_latency_experiment.csv -r -c 65 -s 35')
    runCmd(script_dir+'/multisite_ping_to_csv.py -f ministanford_2_site_ifwd_latency_experiment_round/client_* -o ~/parsed-results/ministanford_2_site_ifwd_latency_experiment.csv -r -c 65 -s 35')
    
    runCmd(script_dir+'/clean_up_mtr -f ~/parsed-results/ministanford_2_site_netviews_latency_experiment.csv ~/parsed-results/ministanford_2_site_ifwd_latency_experiment.csv -n "NetViews" "ifwd" -o ~/parsed-results/ministanford_two_site_nth_packet_latency_results.csv -t "Ministanford Two Site nth Packet Latency" -rfr True'+"  -runs "+str(total_runs))
    runCmd(script_dir+'/clean_up_mtr  -f ~/parsed-results/ministanford_2_site_netviews_latency_experiment.csv ~/parsed-results/ministanford_2_site_ifwd_latency_experiment.csv -n "NetViews" "ifwd" -o ~/parsed-results/ministanford_two_site_first_packet_latency_results.csv -t "Ministanford Two Site First Packet Latency" -F -rfr True'+" -runs "+str(total_runs))
    
    #runCmd('~/netviews-code/experiments/bar_plot_latency -f ~/parsed-results/ministanford_two_site_first_packet_latency_results.csv -o ~/parsed-results/ministanford_two_site_first_packet_latency -t "Initial Packet Latency"')
    #runCmd('~/netviews-code/experiments/bar_plot_latency -f ~/parsed-results/ministanford_two_site_nth_packet_latency_results.csv -o ~/parsed-results/ministanford_two_site_nth_packet_latency -t "nth Packet Latency"')
    
    #ministanford_1_site

    runCmd(script_dir+'/multisite_ping_to_csv.py -f ministanford_1_site_netviews_latency_experiment_round/client_* -o ~/parsed-results/ministanford_1_site_netviews_latency_experiment.csv -r -c 65 -s 35')
    runCmd(script_dir+'/multisite_ping_to_csv.py -f ministanford_1_site_ifwd_latency_experiment_round/client_* -o ~/parsed-results/ministanford_1_site_ifwd_latency_experiment.csv -r -c 65 -s 35')
    
    runCmd(script_dir+'/clean_up_mtr -f ~/parsed-results/ministanford_1_site_netviews_latency_experiment.csv ~/parsed-results/ministanford_1_site_ifwd_latency_experiment.csv -n "NetViews" "ifwd" -o ~/parsed-results/ministanford_single_site_nth_packet_latency_results.csv -t "Ministanford Single Site nth Packet Latency" -rfr True'+" -runs "+str(total_runs))
    runCmd(script_dir+'/clean_up_mtr  -f ~/parsed-results/ministanford_1_site_netviews_latency_experiment.csv ~/parsed-results/ministanford_1_site_ifwd_latency_experiment.csv -n "NetViews" "ifwd" -o ~/parsed-results/ministanford_single_site_first_packet_latency_results.csv -t "Ministanford Single Site First Packet Latency" -F -rfr True'+" -runs "+str(total_runs))
    
    #runCmd('./experiments/bar_plot_latency -f ~/parsed-results/ministanford_single_site_first_packet_latency_results.csv -o ~/parsed-results/ministanford_single_site_first_packet_latency -t "Initial Packet Latency"')
    #runCmd('./experiments/bar_plot_latency -f ~/parsed-results/ministanford_single_site_nth_packet_latency_results.csv -o ~/parsed-results/ministanford_single_site_nth_packet_latency -t "nth Packet Latency"')

    #cisco_2_site

    runCmd(
        script_dir+'/multisite_ping_to_csv.py -f cisco_2_site_netviews_latency_experiment_round/client_* -o ~/parsed-results/cisco_2_site_netviews_latency_experiment.csv -r -c 4 -s 8')
    runCmd(
        script_dir+'/multisite_ping_to_csv.py -f cisco_2_site_ifwd_latency_experiment_round/client_* -o ~/parsed-results/cisco_2_site_ifwd_latency_experiment.csv -r -c 4 -s 8')

    runCmd(
        script_dir+'/clean_up_mtr -f ~/parsed-results/cisco_2_site_netviews_latency_experiment.csv ~/parsed-results/cisco_2_site_ifwd_latency_experiment.csv -n "NetViews" "ifwd" -o ~/parsed-results/cisco_two_site_nth_packet_latency_results.csv -t "Cisco Two Site nth Packet Latency" -rfr True'+" -runs "+str(total_runs))
    runCmd(
        script_dir+'/clean_up_mtr  -f ~/parsed-results/cisco_2_site_netviews_latency_experiment.csv ~/parsed-results/cisco_2_site_ifwd_latency_experiment.csv -n "NetViews" "ifwd" -o ~/parsed-results/cisco_two_site_first_packet_latency_results.csv -t "Cisco Two Site First Packet Latency" -F -rfr True'+" -runs "+str(total_runs))

    # runCmd('./experiments/bar_plot_latency -f ~/parsed-results/cisco_two_site_first_packet_latency_results.csv -o ~/parsed-results/cisco_two_site_first_packet_latency -t "Initial Packet Latency"')
    # runCmd('./experiments/bar_plot_latency -f ~/parsed-results/cisco_two_site_nth_packet_latency_results.csv -o ~/parsed-results/cisco_two_site_nth_packet_latency -t "nth Packet Latency"')

    # cisco_1_site

    runCmd(
        script_dir+'/multisite_ping_to_csv.py -f cisco_1_site_netviews_latency_experiment_round/client_* -o ~/parsed-results/cisco_1_site_netviews_latency_experiment.csv -r -c 4 -s 8')
    runCmd(
        script_dir+'/multisite_ping_to_csv.py -f cisco_1_site_ifwd_latency_experiment_round/client_* -o ~/parsed-results/cisco_1_site_ifwd_latency_experiment.csv -r -c 4 -s 8')

    runCmd(
        script_dir+'/clean_up_mtr -f ~/parsed-results/cisco_1_site_netviews_latency_experiment.csv ~/parsed-results/cisco_1_site_ifwd_latency_experiment.csv -n "NetViews" "ifwd" -o ~/parsed-results/cisco_single_site_nth_packet_latency_results.csv -t "Cisco Single Site nth Packet Latency" -rfr True'+" -runs "+str(total_runs))
    runCmd(
        script_dir+'/clean_up_mtr  -f ~/parsed-results/cisco_1_site_netviews_latency_experiment.csv ~/parsed-results/cisco_1_site_ifwd_latency_experiment.csv -n "NetViews" "ifwd" -o ~/parsed-results/cisco_single_site_first_packet_latency_results.csv -t "Cisco Single Site First Packet Latency" -F -rfr True'+" -runs "+str(total_runs))

    # runCmd('./experiments/bar_plot_latency -f ~/parsed-results/cisco_single_site_first_packet_latency_results.csv -o ~/parsed-results/cisco_single_site_first_packet_latency -t "Initial Packet Latency"')
    # runCmd('./experiments/bar_plot_latency -f ~/parsed-results/cisco_single_site_nth_packet_latency_results.csv -o ~/parsed-results/cisco_single_site_nth_packet_latency -t "nth Packet Latency"')

    #group 1 and 2 site results into one.
    files_names_of_aggregated_results=["first_packet_latency_results.csv","nth_packet_latency_results.csv"]
    site_wise_results_file=[ "cisco_single_site_first_packet_latency_results.csv",
                            "cisco_two_site_first_packet_latency_results.csv",
                            "ministanford_single_site_first_packet_latency_results.csv",
                            "ministanford_two_site_first_packet_latency_results.csv",
                             "cisco_single_site_nth_packet_latency_results.csv",
                             "cisco_two_site_nth_packet_latency_results.csv",
                            "ministanford_single_site_nth_packet_latency_results.csv",
                            "ministanford_two_site_nth_packet_latency_results.csv"]

    index_of_site_wise_result_files=0
    folder=os.environ.get('HOME')+"/parsed-results/"
    for aggregated_file in files_names_of_aggregated_results:
        with open(folder+aggregated_file, "w", newline="") as f1:
            writer = csv.writer(f1)
            writer.writerow(["application", "topology", "latency_ms"])
            latencies = []
            for i in range(4):
                with open(folder+site_wise_results_file[index_of_site_wise_result_files], "r", newline="") as f2:
                    next(f2)  # skip headers
                    f2_reader = csv.reader(f2)
                    for line in f2_reader:
                        if line[1].split(" ")[1]=="Single":
                            line[1]=line[1].split(" ")[0]+ " 1 " +"Site"
                        if line[1].split(" ")[1]=="Two":
                            line[1]=line[1].split(" ")[0]+ " 2 " +"Sites"
                        writer.writerow(line)
                        latencies.append(round((float(line[2])),2)) 
                index_of_site_wise_result_files+=1

        #~/netviews-code/experiments/
        runCmd(script_dir+'/bar_plot_latency -f '+folder+aggregated_file+" -o "+folder+aggregated_file.split(".")[0]+"_plot"+" -t "+ "\""+ aggregated_file.split(".")[0].split("_")[0]+ " Packet Latency\"" + " -y "+str(round(max(latencies)*1.05,2)))
def main():
    #run_experiments( app_name = "org.onosproject.netviews",site_count = 2,experiment_type="latency",topology="ministanford")
    #run_experiments( app_name = "org.onosproject.ifwd",site_count = 2,experiment_type="latency",topology="ministanford")

    parser = argparse.ArgumentParser()
    parser.add_argument('-exp', '--experiment-type', type=str, required=True,
                        help='\'thrpt\' or \'latency\' experiments.')
    parser.add_argument('-runs', '--total-runs', type=int, required=True,
                        help='Total runs.')

    args = parser.parse_args()

    if args.experiment_type=='thrpt':
        plot_throughput_experiments()
    elif args.experiment_type=='latency':
        plot_latency_experiments(int(args.total_runs))

if __name__ == '__main__':
    main()
