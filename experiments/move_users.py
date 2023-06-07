#!/usr/bin/env python3


from ast import arg
import os
import socket
import argparse
import json
import time
import subprocess
import sys

def main():
    parser = argparse.ArgumentParser(
        description='Get the difference between two files')
    parser.add_argument('-i', '--identities_file', type=str, required=True,
                        help='The new identities of the users that have moved')
    parser.add_argument('-s', '--total_sites', type=int, required=True,
                        help='Total Sites')
    parser.add_argument('-d', '--destination_folder', type=str, required=True,
                        help='destination folder for results (give absolute_path)')
    parser.add_argument('-l', '--users_to_move', type=int, default=sys.maxsize, required=False,
    help='limit on the number of users to move')

    args = parser.parse_args()

    inform_relevant_sites(args.identities_file,args.total_sites,args.users_to_move)
    collect_results(args.total_sites,args.destination_folder)

def runCmd(cmd):
    #print("Running command: {}".format(cmd))
    return subprocess.call(cmd, shell=True)

def send_json(container_num, js):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    home = os.environ.get('HOME_DIRECTORY')
    
    #s.connect(('localhost', 9998))
    s.connect((os.environ[f"OC{container_num}"], 9998))
    
    s.sendall(json.dumps(js).encode())

def inform_relevant_sites(identities_file,sites,users_to_move):
    
    for site_number in range(1, sites+1):
        runCmd("ssh "+os.environ.get('ONOS_USER')+"@"+os.environ.get('OC'+str(site_number))+" 'rm -f /tmp/user_arrival_timestamps.json'")
    
    
    with open(identities_file) as file:
        unique_identities = json.load(file)

    #print(unique_identities)
    print("sending identities of roaming users to the new site")
    time.sleep(5)

    for i in range(0, min(users_to_move,len(unique_identities))):
        identity = unique_identities[i]

        container_num = identity['Device']['IP'].split("/")[0].split(".")[2]
        switch_name=identity['Device']['Port'].split(":")[1].split("-")[0]

        send_json(container_num, {"type": "create", "identity": identity,
                                  "switch_name": switch_name})
        #we are not gonna delete the user. we will jsut chnage user's identity mapping and location at old site
        # else:
        #     send_json(container_num, {"type": "delete", "host": identity["Name"]})

def collect_results(sites, destination_folder):
    print("waiting for a few seconds before collecting results")
    time.sleep(15)
    #time.sleep(60)
    runCmd("sudo rm -r "+destination_folder)
    runCmd("mkdir -p "+destination_folder)
    for site_number in range(1, sites+1):
        runCmd("scp -r "+os.environ.get('ONOS_USER')+"@"+os.environ.get('OC'+str(site_number))+":/tmp/user_arrival_timestamps.json "+destination_folder+"/user_arrival_timestamps_site_"+str(site_number)+".json")
    runCmd("cat "+ destination_folder+"/* > "+destination_folder+"/user_arrival_timestamps.json")

if __name__ == '__main__':
    main()


