## Getting Started

For setting up the VNC server, include `-vnc :0` in the &ldquo;Boot up the
VM&rdquo; section (only necessary when using a Linux workstation). Then from
your workstation, perform the following: 

Forward ports, replace `zion05` with the machine you will run the VM on

``` shell
ssh -L 5900:localhost:5900 -L 5556:localhost:5556 -L 8181:localhost:8181 -N zion05
```

If this is the first time, create a VM image on the host machine

``` shell
qemu-img create -f qcow2 netviews_vm_1.qcow 100G
```

Boot up the VM on the host and keep it running in the background

``` shell
nohup qemu-system-x86_64 \                                                # Use qemu as a hypervisor to emulate x86_64
    -cdrom ubuntu-20.04.2-live-server-amd64.iso \                         # Use Ubuntu 20.04.2
    -cpu host \                                                           # KVM processor, use all features supported by host
    -enable-kvm \                                                         # KVM virtualization
    -m 32768 \                                                            # Set RAM in MiB
    -smp 16 \                                                             # Number of cores the guest can use
    -drive file=netviews_vm_1.qcow,format=qcow2 \                         # The name and format of the VM image
    -device e1000,netdev=net0 \                                           # Add device driver, in this case: the e1000 network driver
    -netdev user,id=net0,hostfwd=tcp::5556-:22,hostfwd=tcp::8181-:8181 \  # Configure network device, including port fowarding: 5556 -> 22, 8181 -> 8181
    -vnc :0 &                                                             # Open port 5900 (default) for a VNC viewer to connect to
```

Connect using a VNC client from the host

``` shell
vinagre ::5900
```
In the VNC session, follow the install instructions. Make sure to check the box
to install `OpenSSH` when prompted. Remember the user password you choose. You
can also import SSH keys at this step--it will ask you if you want to import them
from GitHub.

SSH into the VM from local host. Password is `msnetviews` (or whatever you chose
during install).

``` shell
ssh -X ben@localhost -p 5556
```

(Optional) Manually setup SSH public key authentication, if you did not do so
during this install

``` shell
echo "<YOUR PUBLIC KEY HERE>" >> $HOME/.ssh/authorized_keys
```

Source `bashrc` on every login
``` shell
echo -e "if [ -f ~/.bashrc ]; then\n  source ~/.bashrc\nfi" >> ~/.bash_profile
```

Enable X11 forwarding, useful for Wireshark

``` shell
echo "export XAUTHORITY=$HOME/.Xauthority" >> $HOME/.bashrc
```

Export HOME_DIRECTORY variable to ~/.bashrc file [^1]

``` shell
echo "export HOME_DIRECTORY=~" >> ~/.bashrc && source ~/.bashrc
```

Install dependencies (will need to enter user password, be alert for the prompt)

``` shell
curl https://bazel.build/bazel-release.pub.gpg | sudo apt-key add -
    
echo "deb [arch=amd64] https://storage.googleapis.com/bazel-apt stable jdk1.8" | sudo tee /etc/apt/sources.list.d/bazel.list
    
sudo apt -y update && sudo apt -y install bazel
sudo apt -y update && sudo apt -y full-upgrade
sudo apt -y install bazel-3.7.2 openjdk-11-jdk net-tools zip maven docker docker-compose sshpass python python3-pip
pip3 install mininet
```

Build and install mininet

``` shell
git clone https://github.com/mininet/mininet && pushd mininet && git checkout -b 2.3.0d6 && ./util/install.sh -fnv && popd
```

Build and install ONOS

``` shell
git clone https://gerrit.onosproject.org/onos ~/onos && pushd ~/onos && bazel build onos && popd
```

Setup ONOS environment variables

``` shell
echo -e "export ONOS_ROOT=~/onos" >> $HOME/.bashrc && source $HOME/.bashrc
echo -e "source $ONOS_ROOT/tools/dev/bash_profile" >> $HOME/.bashrc && source $HOME/.bashrc
```

Generate an SSH key if necessary
``` shell
ssh-keygen -t rsa -C "your_email@example.com"
```

[upload your VM's RSA (e.g. ~/.ssh/id_rsa.pub) SSH key to GitHub](https://github.com/settings/keys).

Clone the MSNetViews repo
``` shell
git clone git@github.com/netviews/ms-netviews ~/ms-netviews
```

Create an ONOS app
``` shell
pushd ~/ms-netviews/ONOS_Apps/netviews && onos-create-app cli org.onosproject netviews 1.0-SNAPSHOT org.onosproject.netviews && popd
```

If the above returns a build error, you may need to delete a file (as below) and
try again

``` shell
rm ~/ms-netviews/ONOS_Apps/netviews/src/main/java/org/onosproject/netviews/AppCommand.java
```

Run ONOS, if not running already, wait until debug log stops

``` shell
cd ~/onos && bazel run onos-local -- clean debug
```

Start a new shell. Use `generate_related_files.py` to generate all related files for the experiment (topology, identity mapping, policy, obligation files, prohibition files, etc for all the sites). The script above generates related files for two site settings (set via the -s parameter). Please refer to the arguments of `generate_related_files.py` for more details. 

``` shell
pushd ~/ms-netviews/topology-json/automation/ && ./generate_related_files.py -s 2 -n 200 -client_ratio 0.65 -server_ratio 0.35 -t ministanford -tc 1000 -et RR -r 50 && popd
```
  
Make sure the terminal has up-to-date environment variables for installing and running containers by running the following command. Put this command in `~/.bashrc` too, so any new terminals opened afterwards would automatically load the configurations.

``` shell
echo "cell netviews-cell" >> ~/.bashrc && source ~/.bashrc
```

## Preparing to run demos and experiments

These commands need to be run anytime the NetViews code changes.

First, build the NetViews app; `make vm-up` creates the containers for all sites and copies some of the related files to the containers. It also installs the NetViews app in all containers.


``` shell
pushd ~/netviews-code/ && make vm-build && make netviews && make ifwd && make vm-down && make vm-up && popd
```

`distribute_policies` takes an enterprise policy generated above (by `generate_related_files.py`) and then slices that policy into individual site specific policies. It also copies Policy Identity mapping, Obligation files, and Prohibition files to each site

``` shell
pushd ~/central_admin/ && ./distribute_policies && popd
```

### Experiments

To run experiments (please refer to the arguments of `run_multisite_experiment` for more details). The follwoing script is for running latency experiements. 

``` shell
pushd ~/netviews-code && ./scenarios/run_multisite_experiment -s 2 -t ./topology-json/automation/ministanford_2_sites/site -e ./topology-json/automation/ministanford_2_sites/latency_experiments/latency_experiment_round1.json -m experiments/mininet_script -d ministanford_2_site_netviews_latency_experiment_round1 -a org.onosproject.netviews -M '"-z MULTISITE -s 120 -A 10 -f -r 20 -w 0.1 -T gre -I gretun_1 -l 0.5"' && popd
```

### Results

The results folder specificed by `-d` argument in the [experiment](#experiments) would be available in the `~/netviews-code` directory where the experiments had been run.  

### Plots

Run the following caommands to plot the results. The resultant plots would be availabe in `~/parsed-results` folder specificed as the output folder in the commands below. The follwoing script is for running latency experiment plots. 

``` shell
~/netviews-code/experiments/multisite_ping_to_csv.py -f ministanford_2_site_netviews_latency_experiment_round/client_* -o ~/parsed-results/ministanford_2_site_netviews_latency_experiment.csv -r -c 65 -s 35
~/netviews-code/experiments/clean_up_mtr -f ~/parsed-results/ministanford_2_site_netviews_latency_experiment.csv -n "NetViews" -o ~/parsed-results/ministanford_two_site_nth_packet_latency_results.csv -t "Ministanford Two Site nth Packet Latency" -rfr True -runs 20
~/netviews-code/experiments/clean_up_mtr -f ~/parsed-results/ministanford_2_site_netviews_latency_experiment.csv -n "NetViews" -o ~/parsed-results/ministanford_two_site_first_packet_latency_results.csv -t "Ministanford Two Site first Packet Latency" -F -rfr True -runs 20

```

### Demos

To have topology, mininet_script, and experiment files copied to individual
sites, run `run_multisite_experiment` once (see [above](#experiments)) while keeping the last action, i.e., `Run-Experiment-${#}`, commented.

Next, `ssh` into each site (i.e. `ssh root@$OC#`) and run the following

``` shell
sudo ./mininet_script -t /tmp/topology -c run -e /tmp/experiment -d /tmp/experiment_results -a org.onosproject.netviews -z MULTISITE -s 120 -A 10 -f -r 1 -w 0.1 -T gre -I gretun_1 -l 0.5
```

## Debugging
For issues running `stc` include `stcDumpLogs=true` in front of the stc command.
Also, file the xml file corresponding to that scenario and enable log output for
the relevant commands.

Ex.

``` shell
stcDumpLogs=true stc distribute-policies
```

## Footnotes
[^1]: I realized there’s a \$HOME environment already present in linux, so I’ll update my scripts to load that info from \$HOME rather than \$HOME_DIRECTORY and then will remove this step.

