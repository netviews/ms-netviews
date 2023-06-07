#!/usr/bin/env python3
# libraries & dataset
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import os
import json
import re
from collections import OrderedDict
from statistics import *
import argparse
from matplotlib import rcParams
import numpy as np

#Sample usage:
#-pdir ./multiple_roaming_users/11.2_ms_WAN -o ./ocation_update_time_multiple_user_11ms_WAN.pdf

def main():
        parser = argparse.ArgumentParser(
                description='plot results of location update time')
        parser.add_argument('-pdir', '--parent_dir', type=str, required=True,
                            help='parent directory where result are present (e.g. "output.pdf")(give reletive path)')
        parser.add_argument('-o', '--output', type=str, required=True,
                            help='output file path (e.g. "output.pdf") (give reletive path)')
        args = parser.parse_args()

        plot_single_user_multisite_location_update_time_without_batching()
        plot_multiuser_user_two_site_location_update_time_without_batching(args.parent_dir,args.output)
        plot_location_update_time_vs_number_of_roaming_users_with_batching()
        #plot_location_update_time_vs_batching_frequency()
        

def plot_single_user_multisite_location_update_time_without_batching():
        rcParams['font.size'] = 28
        rcParams["font.family"] = "Arial"
        rcParams['xtick.labelsize'] = 24
        rcParams['ytick.labelsize'] = 24
        rcParams['xtick.alignment'] = 'center'
        rcParams["figure.figsize"] = 8, 5

        sns.set_style("ticks", {'axes.grid': True})

        data = {'Location Update Time (s)': [902/1000, 911/1000, 1068/1000, 1465/1000],
                'Number of Sites': [2, 4, 8, 16]}

        # Create DataFrame
        df = pd.DataFrame(data)

        sns.lineplot(data=df, x="Number of Sites", y="Location Update Time (s)",marker='o')
        #plt.show()
        plt.gca().yaxis.grid(True)
        # plt.yticks([x for x in range(0,1500,1)])
        plt.yticks([round(x/1000,2) for x in range(0, 2200, 200)]) #plt.yticks([round(x/1000,2) for x in range(900, 1600, 100)])
        plt.xticks(data['Number of Sites'])
        plt.savefig('./location_update_time_single_user.pdf', bbox_inches='tight')

def load_data(file,case):
        stats=[]
        reported_times=[]
        data=open(file,"r").read()
        data = str(data).replace("}{", "}\n{")

        for line in data.split("\n"):
                jsonObject=json.loads(line)
                stats.append(jsonObject)

        for host_num in range(1, int(case)+1):
                host_specific_stats=[stat for stat in stats if stat['Name']=='h'+str(host_num)]
                host_specific_times=[int(stat['time']) for stat in host_specific_stats]
                reported_times.append(max(host_specific_times)-min(host_specific_times))

        return reported_times


def plot(x_parameter,y_parameter,x_values,y_values,output_file,to_log_y_axis=False,y_ticks_for_plot=None,add_bar_labels=False,to_round=False):
        rcParams['font.size'] = 28
        rcParams["font.family"] = "Arial"
        rcParams['xtick.labelsize'] = 24
        rcParams['ytick.labelsize'] = 24
        rcParams['xtick.alignment'] = 'center'
        rcParams["figure.figsize"] = 8, 5

        sns.set_style("ticks", {'axes.grid': True})
        data = {y_parameter: y_values ,
                x_parameter: x_values}
        # Create DataFrame
        df = pd.DataFrame(data)

        plt.clf()
        if to_round:
                df[y_parameter] = df[y_parameter].round(1)
        ax=sns.barplot(data=df, x=x_parameter, y=y_parameter,palette='mako',ci=None) #, marker='o'
        if add_bar_labels:
                ax.bar_label(ax.containers[0])
        sns.despine()
        plt.gca().yaxis.grid(True)
        if y_ticks_for_plot:
                plt.yticks(y_ticks_for_plot)
                plt.gca().set_ylim([min(y_ticks_for_plot), max(y_ticks_for_plot)])
        #plt.xticks(data[x_parameter])
        #use either log of limits
        if to_log_y_axis:
                plt.yscale('log')
        #plt.gca().set_ylim([lower_y, upper_y])

        plt.savefig(output_file, bbox_inches='tight')

def plot_multiuser_user_two_site_location_update_time_without_batching(parent_dir,output):
        reported_times_for_all_cases = OrderedDict()
        for name in os.listdir(parent_dir):
                if os.path.isdir(parent_dir+"/"+name):
                        folder = parent_dir + "/" + name
                        case=name.split("_")[3]
                        reported_times_per_host=load_data(folder+"/"+"user_arrival_timestamps.json",case)
                        reported_times_for_all_cases[int(case)]=reported_times_per_host

        reported_times_for_all_cases= {k: v for k, v in sorted(list(reported_times_for_all_cases.items()))}
        x_values=[]
        y_values=[]
        for case in reported_times_for_all_cases:
                # if case==32:
                #         y_values += [x for x in reported_times_for_all_cases[case]]
                #         x_values += [str(case) for x in range(0, int(case))]
                # else:
                y_values+=[round(x,2) for x in reported_times_for_all_cases[case]]
                x_values+=[str(case) for x in range(0,int(case))]

        y_values_in_sec=[round(y/1000,2) for y in y_values] #convert from ms to sec
        
        plot('Number of Roaming Users','Location Update Time (s)',x_values,y_values_in_sec,output,y_ticks_for_plot=[round(x/1000,2) for x in range(0, 1800, 200)])

def generate_location_update_time_for_specific_batch_interval_and_number_of_users(users,user_arrival,batch_interval,per_update_event_time,per_user_processing):
       
        y_values = []
        for user_count in users:
                number_of_batches = 1
                wait = batch_interval
                sum = 0.0
                curr_user = 1
                while curr_user <= user_count:
                        if wait < user_arrival:
                                # start the next batch
                                wait = 1
                                number_of_batches+=1
                        # sum waiting time of all users in the batch
                        wait -= user_arrival
                        sum = sum + (wait + per_user_processing)
                        curr_user += 1
                average_location_update_time_per_user = (sum / user_count) + (per_update_event_time*number_of_batches/user_count)
                y_values.append(round(average_location_update_time_per_user, 2))
        return y_values

def plot_location_update_time_vs_number_of_roaming_users_with_batching():
        x_parameter = "Number of Roaming Users"
        y_parameter = "Avg. Location Update Time (s)"  # time per user
        x_values = ("2", "4", "8", "16", "32")
        users=[2,4,8,16,32]
        #time to update location of one user in 2 site setting (in seconds) in global WAN case
        per_update_event_time_in_11ms_WAN = 0.924
        #time it takes to trigger obligation for 1 user on a site
        per_user_processing=0.083

        # plot for 1 sec batching time ( with new user every 0.1 s)
        # new user roams every 0.1 s
        user_arrival = 0.1
        # 1 sec batching time
        batch_interval = 1
        y_values=generate_location_update_time_for_specific_batch_interval_and_number_of_users(users,user_arrival,batch_interval,per_update_event_time_in_11ms_WAN,per_user_processing)
        plot(x_parameter, y_parameter, x_values, y_values,
             "./location_update_time_with_batching_of_1s_in_105ms_WAN.pdf",y_ticks_for_plot=[x for x in np.arange(0.5, 1.60, 0.20)], add_bar_labels=True)

        # plot for 10 sec batching time ( with new user every 0.1 s)
        # new user roams every 0.1 s
        user_arrival = 0.1
        # 1 sec batching time
        batch_interval = 10
        y_values=generate_location_update_time_for_specific_batch_interval_and_number_of_users(users,user_arrival,batch_interval,per_update_event_time_in_11ms_WAN,per_user_processing)
        plot(x_parameter, y_parameter, x_values, y_values,
             "./location_update_time_with_batching_of_10s_in_105ms_WAN.pdf", y_ticks_for_plot=[x for x in range(7, 12, 1)], add_bar_labels=True,to_round=True)
        


# def plot_location_update_time_vs_batching_frequency():
# 
#         # num=599.5#599.5
#         # sum=0.0
#         # users=1
#         # while num>=0 and users<=32:
#         #         sum+=num
#         #         num-=0.5
#         #         users+=1
# 
#         per_user_update_time_in_11ms_WAN=1.0025 #s
# 
#         #1 user every 0.5 s
#         x_parameter="Batch Interval"
#         x_values=("per sec","per 10 sec", "per min", "per 10 min")
#         y_parameter="Avg. Location Update Time (s)" #time per user
#         y_values=[]
# 
#         #need to fine-tune the numbers since processing for 600 users would not be exactly 1002.5 (as trigger obligation called for more users)
#         #total_users = 600 interval 1 min
#         # y_values.append( ((0.5+0)+per_user_update_time_in_11ms_WAN +(2*0.088))/(2) )
#         # y_values.append( ( (3570) +per_user_update_time_in_11ms_WAN +(120*0.088))/(120) )
#         # y_values.append( ( (269850) +per_user_update_time_in_11ms_WAN +(600*0.088))/(600) )
#         # plot(x_parameter, y_parameter, x_values, y_values,
#         #      "./scalability_plots_location_update_time_vs_batching_frequency_in_11ms_WAN.pdf",to_log_y_axis=True, add_bar_labels=True)
# 
#         #total_users = 32
#         y_values.append( ((0.5+0)+per_user_update_time_in_11ms_WAN +(2*0.088))/(2) ) # 1 sec batch frequency
#         y_values.append ( ((95) + per_user_update_time_in_11ms_WAN + (20 * 0.088)+(81) + per_user_update_time_in_11ms_WAN + (12 * 0.088)) / (32))  # 10 sec batch frequency
#         y_values.append( ( (1656) +per_user_update_time_in_11ms_WAN +(32*0.088))/(32) )  #1 min batch frequency
#         y_values.append( ( (18936) +per_user_update_time_in_11ms_WAN +(32*0.088))/(32) ) #10 min batch frequency
#         plot(x_parameter,y_parameter,x_values,y_values,"./scalability_plots_location_update_time_vs_batching_frequency_in_11ms_WAN.pdf",to_log_y_axis=True,add_bar_labels=True)

if __name__ == '__main__':
        main()



