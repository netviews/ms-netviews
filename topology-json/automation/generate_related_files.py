#!/usr/bin/env python

import argparse
import os
import csv
import re
import subprocess
import logging
import math
from collections import OrderedDict
import json

container_ip="192.168.55.0/32"

#sample usage:
#-s 5 -n 500 -client_ratio 0.65 -server_ratio 0.35 -t ministanford -tc 1000 -r 100 -et RR

def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('-s', '--numberOfSites', type=int, required=True,
						help='Number of sites desired')
	parser.add_argument('-n', '--numberOfNodes', type=int, required=True,
						help='The total number of nodes (host+server) desired')
	parser.add_argument('-client_ratio', '--clientRatio', type=float, required=True,
						help='The ratio of clients')
	parser.add_argument('-server_ratio', '--serverRatio', type=float, required=True,
						help='The ratio of servers')
	parser.add_argument('-t', '--topologyType', type=str, required=True,
						help='Which site topology is desired')
	parser.add_argument('-et', '--experiementType', type=str, required=True,
						help='IA:Iffat side of experiemnt; RR:Ramzah side of expriment')
	parser.add_argument('-hg', '--policyTreeHeight', type=int, required=False, default=1,
						help='this height determines the depth of the policy graph')
	parser.add_argument('-tc', '--totalConnectionsInExperiment', type=int, required=False, default=1000,
						help='the total number of connections taking part in the experiments')
	parser.add_argument('-r', '--rounds', type=int, required=False,default=100,
						help='the number of rounds of experiment')
	args = parser.parse_args()
	if args.experiementType == "IA":
		do_it_IA(args.numberOfSites, args.numberOfNodes, args.clientRatio, args.serverRatio, args.topologyType, args.policyTreeHeight)
	if args.experiementType == "RR":
		do_it_RR(args.numberOfSites, args.numberOfNodes, args.clientRatio, args.serverRatio, args.topologyType,
				 args.totalConnectionsInExperiment,args.rounds)


def runCmd(cmd):
	print("Running Command: "+cmd)
	return subprocess.call(cmd, shell=True)

def merge_identity_mapping_files(folderName,siteCount):
	topo_info = OrderedDict()
	topo_info["Users"]=[]
	topo_info["Objects"]=[]

	for site_num in range(1,siteCount+1):
		identity_mapping_filename="./"+folderName+"/"+"identity_mapping_site"+str(site_num)+".json"
		with open(identity_mapping_filename) as json_file:
			site_identity_mapping=json.load(json_file)

		topo_info["Users"]+=site_identity_mapping["Users"]
		topo_info["Objects"] += site_identity_mapping["Objects"]

	with open("./"+folderName+"/"+"identity_mapping.json", 'w+') as f:
		json.dump(topo_info, f, indent=4)

def create_identity_mapping_file(siteCount,folderName):
	for site_num in range(1, siteCount + 1):
		runCmd("sudo -E " + os.environ.get(
			'HOME') + "/netviews-code/experiments/mininet_script -t ./" + folderName + "/" + "site" + str(
			site_num) + ".json" + " -im " + "./" + folderName + "/" + "identity_mapping_site" + str(
			site_num) + ".json" + " -c generate -d foo -z 0 -a org.onosproject.openflow")
	merge_identity_mapping_files(folderName, siteCount)
	for site_num in range(1, siteCount + 1):
		runCmd("sudo rm ./" + folderName + "/" + "identity_mapping_site" + str(site_num) + ".json")

	# copy to ~/central_admin
	runCmd("scp " + "./"+folderName+"/"+"identity_mapping.json" + " " + os.environ.get('HOME') + "/central_admin/")

def create_prohibition_file(folderName):
	prohibitions = OrderedDict()
	prohibitions["prohibitions"] = []
	prohibition_file_name = "./" + folderName + "/" + "prohibition.json"
	with open(prohibition_file_name, "w") as outfile:
		json.dump(prohibitions, outfile)
	# copy to ~/central admin
	runCmd("scp " + prohibition_file_name + " " + os.environ.get('HOME') + "/central_admin/")

def IP_to_string(ip):
	return str(ip[0])+"."+str(ip[1])+"."+str(ip[2])+"."+str(ip[3])

def create_onos_cell_script(siteCount,folderName):
	global container_ip
	container_ip = container_ip.split("/")[0].split(".")

	cell_file_name=os.environ.get('HOME')+"/netviews-code/scenarios/netviews-cell"
	runCmd("sudo rm "+ cell_file_name)
	f = open(cell_file_name, "w")

	for site_number in range(1,siteCount+1):
		container_ip[3] = site_number
		f.write("export OC"+str(site_number)+"=\""+IP_to_string(container_ip)+"\""+"\n")

	f.write("export ONOS_APPS=\"drivers,openflow,proxyarp,optical,bgprouter\""+"\n")
	f.write("export ONOS_USER=root"+"\n")
	f.write("export ONOS_GROUP=ubuntu"+"\n")
	f.close()

	#copy the file to this experiment's folder as well
	runCmd("scp "+cell_file_name+" "+"./"+folderName+"/")
	#copy to ~/onos/tools/test/cells
	runCmd("sudo scp "+cell_file_name+" "+os.environ.get('HOME')+"/onos/tools/test/cells/")

def do_it_IA(siteCount, nodeCount, clientRatio, serverRatio, topoType, treeHeight):
	hostPerSite = int((nodeCount * clientRatio) / siteCount)
	serverPerSite = int((nodeCount * serverRatio) / siteCount)

	folderName = topoType + "_" + str(siteCount) + "_sites"
	runCmd("rm -fr " + folderName)
	runCmd("mkdir " + folderName)

	# creates topologies
	host = 1
	server = 1
	flag = "DONE"
	for site in range(1, siteCount + 1):
		if (topoType == "ministanford"):
			fileName = "site" + str(site) + ".json"
			runCmd(
				"./create_ministanford_topology -a " + str(host) + " -b " + str(host + hostPerSite - 1) + " -c " + str(
					server) +
				" -d " + str(server + serverPerSite - 1) + " -o " + (folderName + "/" + fileName) + " -s " + str(
					site) + " -r " + (folderName + "/" + "resourcelist.txt"))
			host = host + hostPerSite
			server = server + serverPerSite
		else:
			print("We don't have the script for other site ready yet.")
			flag = "ERROR"

	# creates rest of the files

	if (flag == "DONE"):
		runCmd("./create_enterprise_hosts_file -d ./" + folderName + " -o ./" + (
					folderName + "/" + "hostList.json") + " -s " + str(siteCount))
		runCmd("./create_input_file" + " -d " + folderName + " -s " + str(siteCount))
		runCmd("./create_obligation_files -d " + folderName + " -s " + str(siteCount))

		treeHeight = 1
		runCmd("./create_binaryTree_policy -fn " + folderName + "/" + " -hl " + "hostList.json" + " -ph " + str(
			treeHeight) + " -s " + str(siteCount))

def do_it_RR(siteCount, nodeCount, clientRatio, serverRatio, topoType, totalConnections,experiment_rounds):
	#copy ~/netviews-code/central_admin to ~/central_admin
	runCmd("cp -r ~/netviews-code/central_admin ~/")
	hostPerSite = int((nodeCount * clientRatio)/siteCount)
	serverPerSite = int((nodeCount * serverRatio)/siteCount)

	folderName = topoType + "_"+str(siteCount) + "_sites"
	runCmd("rm -fr "+folderName)
	runCmd("mkdir "+folderName)

	#creates topologies 
	host = 1
	server = 1
	flag = "DONE"
	for site in range(1, siteCount+1):
		if(topoType == "ministanford"):
			fileName = "site" + str(site) + ".json"
			runCmd("./create_ministanford_topology -a "+ str(host) + " -b " + str(host + hostPerSite -1 ) + " -c " + str(server) +
				   " -d " + str(server+serverPerSite-1) + " -o " + (folderName+"/"+fileName) + " -s " + str(site) + " -r "+ (folderName+"/"+"resourcelist.txt"))
			host = host + hostPerSite
			server = server + serverPerSite
		elif (topoType=="cisco"):
			fileName = "site" + str(site) + ".json"
			runCmd(
				"./create_cisco_topology -a " + str(host) + " -b " + str(host + hostPerSite - 1) + " -c " + str(
					server) +
				" -d " + str(server + serverPerSite - 1) + " -o " + (folderName + "/" + fileName) + " -s " + str(
					site) + " -r " + (folderName + "/" + "resourcelist.txt"))
			host = host + hostPerSite
			server = server + serverPerSite

	#creates rest of the files

	commonFactorForNodesTakingPart=math.sqrt(totalConnections/(clientRatio*serverRatio))
	clientsTakingPartInExperiments=min(int(nodeCount * clientRatio) , int(commonFactorForNodesTakingPart*clientRatio))
	serversTakingPartInExperiments=min(int(nodeCount * serverRatio) , int(commonFactorForNodesTakingPart*serverRatio))

	if (flag == "DONE"):
		runCmd("./create_enterprise_hosts_file -d ./"+ folderName + " -o ./"+ (folderName+"/"+"hostList.json") + " -s "+ str(siteCount))

		#script creates inputFiles.txt for central-admin and copies file to /tmp/. Also it creates inputFilesForVM.txt for individual sites and copies it to~/netviews-code/scenarios
		runCmd("./create_input_file"+ " -d " + folderName + " -s " + str(siteCount))

		#script creates obligation files and then we copy those to ~/central-admin
		runCmd("./create_obligation_files -d " + folderName + " -s " +  str(siteCount))
		runCmd("rm " + os.environ.get('HOME') + "/central_admin/locationChangeToSite*")
		for siteNumber in range(1,siteCount+1):
			obligation_file_name="./" + folderName + "/" + "locationChangeToSite" + str(siteNumber) + ".yml"
			runCmd("scp " + obligation_file_name + " " + os.environ.get('HOME') + "/central_admin/")

		#create policy file
		runCmd("./create_policy -hl ./"+ folderName+"/"+"hostList.json" + " -o " +"./"+folderName +"/"+"policy.json"+" -s "+str(siteCount))
		#create experiments files
		runCmd("./create_throughput_experiment -hl ./"+ folderName+"/"+ "hostList.json"+" -o "+"./"+ folderName+"/"+ "throughput_experiment.json"+ " -participating_clients "+ str(clientsTakingPartInExperiments)+" -participating_servers "+ str(serversTakingPartInExperiments) +" -s "+str(siteCount))
		runCmd("./create_latency_experiment -hl ./" + folderName + "/" + "hostList.json" + " -d " + folderName +" -participating_clients "+ str(clientsTakingPartInExperiments)+" -participating_servers "+ str(serversTakingPartInExperiments)+ " -r "+ str(experiment_rounds))

		#call function to create identity mpping files the file which copies it to ~/central-admin a well
		create_identity_mapping_file(siteCount, folderName)

		#script creates gre tunnel scripts and copies to scenarios/gre_tunnel_scripts
		runCmd("./create_gre_tunnel_scripts.py"+ " -s "+str(siteCount)+" -d ./"+ folderName)

		# function creates prohibition file and copies to ~/central_admin
		create_prohibition_file(folderName)

		#create netviews-code/scenarios/netviews-cell file according to the number of sites, copies it local folder of experiments as well as to ~/onos/tools/test/cells/.
		create_onos_cell_script(siteCount,folderName)

		#the script creates docker-compose.yml file and copies it to the ~/netviews-code/.
		runCmd("./create_docker_compose_file.py" + " -s " + str(siteCount) + " -d " + folderName)

if __name__ == '__main__':
	main()