#!/usr/bin/env python3
"""
Example usage:
-f cisco_2_site_netviews_latency_experiment_round/client_* -o ./parsed-results/cisco_2_site_netviews_latency_experiment.csv -r -c 4 -s 8
"""

import argparse
import matplotlib.pyplot as plt
import statistics
import numpy as np
import os
import csv

output = ''
raw = False
NETVIEWS_CLIENTS_PER_SITE = 0
NETVIEWS_SERVERS_PER_SITE = 0


def main():
    parser = argparse.ArgumentParser(
        description='Convert mtr to CSV')
    parser.add_argument('-f', '--file', required=True, nargs="+",
                        help='the result file(s) to parse for graphing')
    parser.add_argument('-o', '--output', type=str, required=True,
                        help='output file path (e.g. "output.csv")')
    parser.add_argument('-r', '--raw', action='store_true',
                        help='mtr result files are in raw format')
    parser.add_argument('-c', '--clients_per_site', type=int, required=True,
                        help='clients per site')
    parser.add_argument('-s', '--servers_per_site', type=int, required=True,
                        help='servers per site')
    args = parser.parse_args()

    files = args.file  # [os.path.join(os.getcwd(), file_path) for file_path in args.file]

    global output
    output = args.output

    global raw
    raw = args.raw

    global NETVIEWS_CLIENTS_PER_SITE
    NETVIEWS_CLIENTS_PER_SITE = args.clients_per_site

    global NETVIEWS_SERVERS_PER_SITE
    NETVIEWS_SERVERS_PER_SITE = args.servers_per_site

    parse_ping(files)

def parse_ping(files):
    csv_values = []

    # with open('outfile', 'wb') as write_file:
    #    csv_wrtier = csv.writer(write_file, delimiter = ',')
    if raw:
        for file_path in files:
            host_number = int(file_path.split("/")[1].split("_")[1][1:])
            server_number = int(
                file_path.split("/")[1].split("_")[2].split(".")[3]) - NETVIEWS_CLIENTS_PER_SITE - 10 + (
                                        NETVIEWS_SERVERS_PER_SITE * (
                                            int(file_path.split("/")[1].split("_")[2].split(".")[2]) - 1))
            print(file_path)
            file = open(file_path, 'r')
            line = file.readline()
            latency_values = []

            while line:
                line = line.split(" ")
                if line[0] == "PING":
                    if latency_values!=[]:
                        csv_values.append(latency_values)
                        latency_values = []
                    latency_values.append(str(host_number) + "_" + str(server_number))
                elif len(line)==8 and "ttl=" in line[5] and "time=" in line[6]:
                    #get rtt
                    rtt=float(line[6].split("=")[1])
                    # convert to us. 
                    if line[7].strip()=='ms':
                        rtt=rtt*1000
                    elif line[7].strip()=='s':
                        rtt=rtt*1000*1000
                    latency_values.append(rtt)
                line = file.readline()

            file.close()
            print(latency_values)
            csv_values.append(latency_values)

    print(csv_values)

    with open(output, 'w') as write_file:
        csv_writer = csv.writer(write_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        # for row in csv_values:
        csv_writer.writerows(csv_values)
    write_file.close()


if __name__ == '__main__':
    main()

