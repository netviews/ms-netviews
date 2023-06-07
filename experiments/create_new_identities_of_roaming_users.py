#!/usr/bin/env python3
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import re
#this script creates new identites for roaming users (all hosts) of site one of ministanford network that move from site 1 to site 2.

def get_MAC_address(curr_mac):
    mac_address = '{0:012x}'.format(curr_mac)
    mac_address = ':'.join(re.findall(r'\w\w', mac_address))
    return mac_address;

if __name__ == '__main__':
    #main()
    identitites="["
    for i in range(1,66):
            host_num=str(i)
            identitites+="\n{\n\"Device\": {\n\"Name\": \"h"+host_num+"\",\n\"IP\": \"172.20.2."+str(110+i)+"/24\",\n\"Port\": \"h1-eth0:s25-eth"+str(50+i)+"\",\n\"MAC\": \""+get_MAC_address(i)+"\",\n\"Location\": \"site2\",\n\"Type\": \"Host\",\n\"ID\": \"1\",\n\"Description\": \"host\"\n},\n\"User\": \"h"+host_num+"\"\n},"
    identitites=identitites[:-1]
    identitites+=("\n]")
    print(identitites)
    file1 = open("./ministanfordMultipleHostIdentitiesToMoveToSite2.json", "w")
    file1.write(identitites)
    file1.close()