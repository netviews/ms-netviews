#!/usr/bin/env python3
# File on global site that keeps socket open in order to listen for incoming data coming from local sites
import json
import socket
import os
from json.decoder import JSONDecodeError

conn = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
conn.bind(('', 9999))
conn.listen(5)


while 1:
    connection, address = conn.accept()
    raw = connection.recv(8192).decode().strip()   
    
    data = json.loads(raw)

    #print("Data Received: ")
    #print(data)

    site_num = data["ip"].split("/")[0].split(".")[2]
    site_name = "site" + site_num

    try:
        infile = open(os.environ.get('HOME')+"/central_admin/location_log.json", "r")
    except FileNotFoundError:
        infile = open(os.environ.get('HOME')+"/central_admin/location_log.json", "w")
        infile.close()
        infile = open(os.environ.get('HOME')+"/central_admin/location_log.json", "r")

    try:
        f = json.load(infile)
        
        for site in f:
            for user in f[site]:
                if user["name"] == data["name"]:
                    f[site].remove(user)

        if site_name not in f:
            f[site_name] = []
        f[site_name].append(data)
    except JSONDecodeError:
        d = "{\"" + site_name + "\": [" + raw + "]}"
        f = json.loads(d)
    infile.close()
    infile = open(os.environ.get('HOME')+"/central_admin/location_log.json", "w")
    json.dump(f, infile, indent=4)
    infile.close()
