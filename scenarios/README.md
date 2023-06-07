### TO ADD 2 ADDITIONAL CONTAINERS (HAVE 5 SITES TOTAL)
-> vm-mininet.sh changed from using gre tunneling to wireguard (file in: ~/netviews-code/scenarios/vm-mininet.sh)

-> Swap your wg_conf folder with the five_site_wg_conf folder (folders in: ~/netviews-code/scenarios)

-> Swap your netviews-cell file for the five-site-netviews-cell file (file in: ~/netviews-code/scenarios)
  - Note: You also need to replace the netviews-cell file in the onos/tools/test/cells folder and, 
  for the first time using it, refresh your cell running the command `cell netviews-cell`. Run the following commands:

```
cp scenarios/netviews-cell "$ONOS_ROOT/tools/test/cells"
cell netviews-cell
```

-> Swap the docker-compose.yml file with the five_site_docker_compose.yml version (files in: ~/netviews-code)

-> Swap the netviews-setup.xml file with the five_site_netviews_setup.xml file (files in: ~/netviews-code/scenarios)

-> in gre_tunnel_scripts folder, touch vm4_gretun_up.sh and touch vm5_gretun_up.sh (did not implement gre tunnels for five sites, only wireguard) (files in: ~/netviews-code/scenarios)

-> swap the latency_scripts folder with five_site_latency_scripts folder (folder in ~netviews-code/scenarios)

-> line 538 to 563 of netviews app here needs to be modified according to 50 container setting (https://github.com/enck/netviews-code/blob/master/ONOS_Apps/netviews/src/main/java/org/onosproject/netviews/IntentReactiveForwarding.java). 
