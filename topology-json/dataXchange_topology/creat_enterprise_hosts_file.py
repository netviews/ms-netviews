import argparse
import json
import os
from collections import OrderedDict

# sample usage: -t ./five_site/topologies -o ./five_site/topologies/enterprise_hosts.json -sites 5

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-t', '--topology_directory', type=str, required=True,
                        help='The directory location that has all site topologies. Site topology names are expected in format "siteX.json" where x is the number of the site.')
    parser.add_argument('-o', '--output', type = str, required = False,
         help = 'the name of the output file')
    parser.add_argument('-sites', '--total_sites', type=int, required=True,
                        help='total sites')
    args = parser.parse_args()

    createEnterpriseHostsFile(args.topology_directory,args.output,args.total_sites)
def createEnterpriseHostsFile(topology_directory,output_file,total_sites):
    hosts = OrderedDict()
    hosts["hosts"] = []

    for site_number in range(total_sites):
        site_topology = open(topology_directory+"/site"+str(site_number+1)+".json", 'r')
        site_topology = json.load(site_topology)
        hosts["hosts"].extend(site_topology["hosts"])

    with open(output_file, 'w+') as f:
        json.dump(hosts, f, indent = 4)
if __name__ == '__main__':
    main()