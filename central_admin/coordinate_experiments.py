#!/usr/bin/env python3
# File on global site that keeps socket open in order to listen for incoming data coming from local sites
from json.decoder import JSONDecodeError
import os
import socket
import argparse
import json

def main():
    parser = argparse.ArgumentParser(
        description='Get the difference between two files')
    parser.add_argument('-s', '--sites', type=int, required=True,
                        help='total sites')
    args = parser.parse_args()
    coordinate(args.sites)

def coordinate(total_sites):
    localIP=""
    localPort = 9997
    bufferSize = 1024
    msgFromServer = "next_round"
    bytesToSend = str.encode(msgFromServer)
    # Create a datagram socket
    UDPServerSocket = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)
    # Bind to address and ip
    UDPServerSocket.bind((localIP, localPort))
    print("UDP server up and listening")
    # Listen for incoming datagrams

    while (True):
        print("staring over")
        messages = []
        addresses = []
        for site_number in range(total_sites):
            bytesAddressPair=UDPServerSocket.recvfrom(bufferSize)
            messages.append(bytesAddressPair[0])
            addresses.append(bytesAddressPair[1])
            print("Message from Client:{}".format(messages[site_number]))
            print("Client IP Address:{}".format(addresses[site_number]))
        # Sending a reply to client
        for site_number in range(total_sites):
            UDPServerSocket.sendto(bytesToSend, addresses[site_number])
            print("message send to a site")

if __name__ == '__main__':
    main()
