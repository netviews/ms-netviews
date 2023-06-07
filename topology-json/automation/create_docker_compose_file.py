#!/usr/bin/env python
import argparse
import os
import subprocess

container_gateway = "192.168.55.250"
subnet = "192.168.55.0/24"

def runCmd(cmd):
    print("Running Command: " + cmd)
    return subprocess.call(cmd, shell=True)

def IP_to_string(ip):
    return str(ip[0])+"."+str(ip[1])+"."+str(ip[2])+"."+str(ip[3])

def create_docker_compose_file(folder_name,sites_count):
    global subnet, container_gateway

    fileName = "./" + folder_name + "/" + "docker-compose.yml"
    f = open(fileName, "w")
    f.write("networks:"+"\n")
    f.write("  mynet:"+"\n")
    f.write("    driver: bridge"+"\n")
    f.write("    ipam:"+"\n")
    f.write("      driver: default"+"\n")
    f.write("      config:"+"\n")
    f.write("        - subnet: "+subnet+"\n")
    f.write("          gateway: "+container_gateway+"\n")
    f.write("\n")
    f.write("version: \"2.4\""+"\n")
    f.write("services:"+"\n")

    container_ip=subnet.split("/")[0].split(".")
    for site_number in range(1,sites_count+1):

        container_ip[3]=site_number

        f.write("  vm"+str(site_number)+":"+"\n")
        f.write("    build: ."+"\n")
        f.write("    extra_hosts:"+"\n")
        f.write("        - \"host.docker.internal:"+container_gateway+"\""+"\n")
        f.write("    networks:"+"\n")
        f.write("      mynet:"+"\n")
        f.write("        ipv4_address: "+IP_to_string(container_ip)+"\n")
        f.write("    privileged: true"+"\n")
    
    f.close()

    # copying file to ~/netviews-code/
    runCmd("scp " + fileName + " " + os.environ.get('HOME') + "/netviews-code/")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-d', '--folderName', type=str, required=True,
						help='name of the folder that has all the files')
    parser.add_argument('-s', '--numberOfSites', type=int, required=True,
                        help='Number of sites')
    args = parser.parse_args()
    create_docker_compose_file(folder_name=args.folderName,sites_count=args.numberOfSites)

if __name__ == '__main__':
    main()
