#!/usr/bin/env python

import argparse
import copy
import subprocess
import os

enterprise_network="172.20.0.0/24"
container_ip="192.168.55.0/32"
tunnel_ip="10.0.0.0/32"

def runCmd(cmd):
    return subprocess.call(cmd, shell=True)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-s', '--numberOfSites', type=int, required=True,
                        help='Number of sites desired')
    parser.add_argument('-d', '--folderName', type=str, required=True,
                        help='name of the folder that has all the files')
    args = parser.parse_args()

    create_scripts(args.numberOfSites,args.folderName)

def IP_to_string(ip):
    return str(ip[0])+"."+str(ip[1])+"."+str(ip[2])+"."+str(ip[3])

def create_script_to_stop_gre_tunnel(siteCount):
    script_name = os.environ.get('HOME')+"/netviews-code/scenarios/gre_tunnel_scripts/" + "gretun_down.sh"
    f = open(script_name, "w")
    f.write("#!/usr/bin/env bash" + "\n")
    f.write("set -euo pipefail" + "\n")

    for tunnel_count in range(1, siteCount):
            tunnel_name = "gretun_" + str(tunnel_count)
            f.write("ip tunnel del "+tunnel_name+"\n")
    f.close()
    runCmd("chmod +x " + script_name)

def create_scripts(sitesCount,folderName):
    global enterprise_network, container_ip, tunnel_ip
    enterprise_network=enterprise_network.split("/")[0].split(".")
    container_ip=container_ip.split("/")[0].split(".")
    tunnel_ip=tunnel_ip.split("/")[0].split(".")
    peer_ip=copy.deepcopy(tunnel_ip)

    runCmd("rm -rf " + os.environ.get('HOME') + "/netviews-code/scenarios/gre_tunnel_scripts/")
    runCmd("mkdir " + os.environ.get('HOME') + "/netviews-code/scenarios/gre_tunnel_scripts/")

    create_script_to_stop_gre_tunnel(sitesCount)

    for site_number in range(1,sitesCount+1):
        script_name=folderName +"/"+ "site" + str(site_number) + "_gretun_up.sh"

        f = open(script_name ,"w")
        f.write("#!/usr/bin/env bash"+"\n")
        f.write("set -euo pipefail"+"\n")

        tunnel_ip[3] = site_number

        tunnels_count=0
        for othersite in [x for x in range(1,sitesCount+1) if x!=site_number]:

            tunnels_count+=1
            tunnel_name="gretun_"+str(tunnels_count)

            peer_ip[3]=othersite
            container_ip[3]=othersite
            enterprise_network[2]=othersite

            f.write("ip tunnel add "+tunnel_name+" mode gre remote "+IP_to_string(container_ip) + " ttl 64 dev eth0" + "\n")
            f.write("ip addr add dev "+tunnel_name+" "+IP_to_string(tunnel_ip)+ " peer "+IP_to_string(peer_ip) + "/32" + "\n")
            f.write("ip link set dev "+tunnel_name+" up" + "\n")
            f.write("ip route add "+ IP_to_string(enterprise_network)+ "/24 via "+IP_to_string(tunnel_ip) + "\n")
        f.close()
        runCmd("chmod +x "+ script_name)

        # copying gre tunnel scripts to scenarios/gre_tunnel_scripts
        runCmd("scp "+ script_name +" "+os.environ.get('HOME')+"/netviews-code/scenarios/gre_tunnel_scripts/")

if __name__ == '__main__':
    main()
