#!/usr/bin/env python3
"""
Example usage:
./experiments/mtr_to_csv -f ../cisco-single-docker-results/ifwd_latency/client_latency_*
    -n {1..4}_{1..8} -o ../cisco-single-docker-results/ilat.csv -r
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

    parse_mtr(files)


def get_position_and_seq_num(host_number, server_number):
    if (host_number - 1) // (NETVIEWS_CLIENTS_PER_SITE) == (server_number - 1) // NETVIEWS_SERVERS_PER_SITE:
        return '0', '33000'
    else:
        return '2', '33002'


def parse_mtr(files):
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
            position, seq_num = get_position_and_seq_num(host_number, server_number)
            print(file_path)
            file = open(file_path, 'r')
            line = file.readline()
            latency_values = []

            while line:
                line = line.split()
                # p 0 219 33009
                if line and line[0] == 'p' and line[1] == position:
                    if line[3] == seq_num:
                        if len(latency_values) > 0:
                            csv_values.append(latency_values)
                        latency_values = []
                        latency_values.append(str(host_number) + "_" + str(server_number))
                    latency_values.append(float(line[2]))
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

