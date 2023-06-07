#!/usr/bin/env python

#sample usage:
# -hl ~/netviews-code/topology-json/automation/ministanford_2_sites/hostList.json -participating_clients 42 -participating_servers 22 -c 65 -s 35
# -hl ~/netviews-code/topology-json/automation/cisco_2_sites/hostList.json -participating_clients 4 -participating_servers 8 -c 4 -s 8

import json
import argparse
import re
import sys
import random


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-hl', '--hostsListFile', type = str, required = True,
         help = 'the hostList file of enterprise to create the policy for')
    parser.add_argument('-participating_clients', '--clientsTakingPartInExperiment', type=int, required=True,
                        help='the total number of clients taking part in the experiments')
    parser.add_argument('-participating_servers', '--serversTakingPartInExperiment', type=int, required=True,
                        help='the total number of servers taking part in the experiments')
    parser.add_argument('-c', '--clients_per_site', type=int, required=True,
                        help='total clients in a site')
    parser.add_argument('-s', '--servers_per_site', type=int, required=True,
                        help='the totalservers in a site')

    args = parser.parse_args()

    doit(args.hostsListFile, args.clientsTakingPartInExperiment,args.serversTakingPartInExperiment,args.clients_per_site,args.servers_per_site)

def host_list_sorting(e):
    return int(re.split('(\d.*)', e)[1])

def doit(topology, host_number, total_servers,clients_per_site,servers_per_site):
    topology = open(topology, 'r')
    topology = json.load(topology)

    selected_hosts=[h["name"] for h in topology["hosts"] if h["type"]=="user" and int(re.split('(\d.*)',h['name'])[1])<clients_per_site+1]
    selected_hosts = random.sample(selected_hosts,k=host_number)
    selected_hosts.sort(key=host_list_sorting)

    selected_servers = [h["name"] for h in topology["hosts"] if
                        h["type"] == "resource" and int(re.split('(\d.*)', h['name'])[1]) < servers_per_site + 1]
    selected_servers = random.sample(selected_servers,k=total_servers)
    
    filename=["within_site","across_site"]
    
    for j in range (2):
        info = {"server": [], "client": []}
        curr_port = 81
        if j==1:
            for i in range(len(selected_servers)):
                selected_servers[i]="sv"+str(int(re.split('(\d.*)',selected_servers[i])[1]) + 35)

        for host in topology["hosts"]:

            if host["description"] == "host" and host["name"] in selected_hosts:
                client_commands = get_client_commands(topology, curr_port,selected_servers,host['name'])
                info["client"].append({
                    host["name"]: client_commands})

                curr_port += 1

            elif host["description"] == "server" and host["name"] in selected_servers:
                server_commands = get_server_commands(host_number,host['name'],selected_hosts)
                info["server"].append({
                    host["name"]: server_commands
                        })

        with open("./"+filename[j]+".json", 'w+') as f:
            json.dump(info, f, indent = 4)

def get_server_commands(host_number,server_name,selected_hosts):
    server_commands = []
    port_number=81
    for i in range(81, host_number + 81):
        server_commands.append(
        "iperf3 -s -p " + str(port_number) + " >& /tmp/experiments/server_throughput_" +  str(re.split('(\d.*)',server_name)[1] ) + "_" + str( str(re.split('(\d.*)',selected_hosts[i-81])[1] ) ) + ".log &")
        port_number+=1
    return server_commands

def get_client_commands(topology, curr_port,selected_servers,client_name):
    client_commands = []
    for server in topology["hosts"]:
        if server["description"] == "server" and server["name"] in selected_servers:
            client_commands.append(
                    "iperf3 -c " + server["ip"][:len(server["ip"]) - 3] + " -p " + str(curr_port) + " -P 1 -V -t 60 >> /tmp/experiments/client_throughput_" + str(re.split('(\d.*)',client_name)[1] ) + "_" + str(re.split('(\d.*)',server['name'])[1] )  + ".log &")

    return client_commands

if __name__ == '__main__':
    main()