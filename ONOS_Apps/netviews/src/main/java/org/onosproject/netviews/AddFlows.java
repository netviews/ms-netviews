/*
 * Copyright 2021-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.netviews;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.*;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.*;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.ModIPInstruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onlab.packet.IpAddress;
import org.onosproject.net.PortNumber;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.karaf.shell.api.action.Argument;

/**
 * Preinstall flows
 */
@Service
@Command(scope = "onos", name = "add-flows",
         description = "Adds the necessary flows for cross site pings")
public class AddFlows extends AbstractShellCommand {

    @Argument(index = 0, name = "numSwitches", description = "Number of switches in this topology",
            required = true, multiValued = false)
    private int numSwitches = 0;

    @Argument(index = 1, name = "gatewaySwitchNum", description = "Number of gateway switch",
            required = true, multiValued = false)
    private int gatewaySwitchNum = 0;

    @Override
    protected void doExecute() throws Exception {
	
	// This ONOS command adds the flows that need to be preinstalled onto switches in order to
	// allow for cross site pinging with an obfuscated policy. 
	// - Note, in a single site or if every site has the global policy, then these flows are not necessary
	// because the controller would be able to check the NetViews policy immediatly and install flows as needed.
	// However, with an obfuscated policy, the NetViews policy would always return false for a cross site ping, because
	// the destination would not be in the policy every time. It would not make it to the other side to be enforced

        FlowRuleService service = get(FlowRuleService.class);

	InetAddress ip;
        Ip4Address src_ip;
        Ip4Address dst_ip;

	ip = InetAddress.getLocalHost();
	
        if (ip.getHostAddress().equals("192.168.55.20")) {
        	src_ip = Ip4Address.valueOf("172.20.32.10");
        	dst_ip = Ip4Address.valueOf("172.20.31.11");
        }
        else {
                dst_ip = Ip4Address.valueOf("172.20.32.10");
                src_ip = Ip4Address.valueOf("172.20.31.11");
        }
	

	// This sets how traffic that matches the flow rule will be treated.
	// In this case, we want normal L2/L3 processing, and therefore just output
	// to port "Normal" which achieves that for us
	// helpful sources: 
	// http://api.onosproject.org/1.8.6/org/onosproject/net/flow/DefaultTrafficTreatment.Builder.html
	// http://trainer.edu.mirantis.com/SDN50/ovs.html
	TrafficTreatment tt = DefaultTrafficTreatment.builder()
        	.setOutput(PortNumber.NORMAL)
                .build();

        TrafficTreatment tt_controller = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.CONTROLLER)
                .build();

	IpPrefix src_ip_prefix = IpPrefix.valueOf(src_ip, 24);
        IpPrefix dst_ip_prefix = IpPrefix.valueOf(dst_ip, 24);
	Ip4Address gateway = Ip4Address.valueOf("172.20.32.1");

	TrafficSelector ts = DefaultTrafficSelector.builder()
                .matchEthType((short)0x800)
                .matchIPSrc(src_ip_prefix)
                .matchIPDst(dst_ip_prefix)
                .build();

	TrafficSelector ts_ping = DefaultTrafficSelector.builder()
                .matchEthType((short)0x800)
		.matchIPProtocol((byte)1)
		.matchIcmpType((byte)0)
                .build();

	TrafficSelector ts_opposite = DefaultTrafficSelector.builder()
                .matchEthType((short)0x800)
                .matchIPSrc(dst_ip_prefix)
                .matchIPDst(src_ip_prefix)
                .build();

	for (int i=1; i<=numSwitches; i++) {
		DeviceId device;
		ArrayList<String> alphabet = new ArrayList<String>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
		if (i < 10) {
			device = DeviceId.deviceId("of:000000000000000" + Integer.toString(i));	
		}
		else {
			device = DeviceId.deviceId("of:000000000000000" + alphabet.get(i - 10));
		}

                FlowRule.Builder builder = DefaultFlowRule.builder()
                                .forDevice(device)
                                .makePermanent()
                                .withCookie(1234567)
                                .withPriority(65535)
                                .withSelector(ts)
                                .withTreatment(tt);
                FlowRule fr = builder.build();


                FlowRule.Builder builder_ping = DefaultFlowRule.builder()
                                .forDevice(device)
                                .makePermanent()
                                .withCookie(1234567)
                                .withPriority(65535)
                                .withSelector(ts_ping)
                                .withTreatment(tt);
                FlowRule fr_ping = builder_ping.build();

		service.applyFlowRules(fr, fr_ping);
	}


        print("Flow Rules Applied!");
    }

}
