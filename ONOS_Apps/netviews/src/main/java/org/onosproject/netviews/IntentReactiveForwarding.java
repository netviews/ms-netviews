package org.onosproject.netviews;

/*import org.apache.felix.scr.annotations.Activate;
     import org.apache.felix.scr.annotations.Component;
     import org.apache.felix.scr.annotations.Deactivate;
     import org.apache.felix.scr.annotations.Reference;
     import org.apache.felix.scr.annotations.ReferenceCardinality;
 */
import java.net.*;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.TCP;
import org.onlab.packet.UDP;
import org.onlab.packet.IPacket;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMP6;
import org.onlab.packet.TpPort;
import org.onlab.packet.ARP;
import org.onlab.packet.MacAddress;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import gov.nist.csd.pm.exceptions.PMException;
import java.util.Arrays;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.neighbour.NeighbourMessageContext;
import org.onosproject.net.neighbour.NeighbourMessageHandler;
import org.onosproject.net.neighbour.NeighbourMessageType;
import org.onosproject.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import java.net.InetAddress;
import org.onosproject.netviews.GetIPService;
//import org.onosproject.net.NetviewsService;

import java.util.EnumSet;
import java.util.Scanner;
import java.util.Set;
import java.util.HashMap;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

//import org.onosproject.np.impl.PolicyEngine;
//import org.onosproject.np.impl.IdentityMap;

// For the extension from VPLS.
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// INSTALLED, WITHDRAWN, FAILED, etc.
import static org.onosproject.net.intent.IntentState.*;

import java.util.WeakHashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.FileWriter;

/**
 * Direction for Intent installation.
 */
enum Direction {
	ADD,
	REMOVE
}

/**
 * WORK-IN-PROGRESS: Sample reactive forwarding application using intent framework.
 */

@Component(immediate = true)
@Service
public class IntentReactiveForwarding implements ReloadingService,TriggerObligationService,GetIPService {

	// JESS ADDED (containerIP field):
	// - I needed to be able to check which site the controller is in in order to know if the destinaation of a packet is in its site or not. 
	// - I couldn't find a function that directly gives me the containerIP from the controller, so instead I added an ONOS command that is automatically
	// run when you run the mininet script which sets this containerIP
	public String containerIP = "";

	// A hashmap to cache the intents returning from the server.
	WeakHashMap<String, Boolean> return_key_cache = new WeakHashMap<>();

	private static final long TIMEOUT = 30;

	int PRIORITY = (1 << 16) - 1;

	private final Logger log = getLogger(getClass());

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected CoreService coreService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected TopologyService topologyService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected PacketService packetService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected IntentService intentService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected HostService hostService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected FlowRuleService flowRuleService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected FlowObjectiveService flowObjectiveService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EdgePortService edgeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NeighbourResolutionService neighbourResolutionService;

	private PolicyEngine netViews = new PolicyEngine();

	private ReactivePacketProcessor processor = new ReactivePacketProcessor();
	private ApplicationId appId;

	private IdentityMap identityMap;
	private PolicyEngine policyEngine;
	
	private static final int DROP_RULE_TIMEOUT = 300;

	private static final EnumSet<IntentState> WITHDRAWN_STATES = EnumSet.of(IntentState.WITHDRAWN,
										IntentState.WITHDRAWING,
										IntentState.WITHDRAW_REQ);

	private Exception error;

	private static final int OPERATION_TIMEOUT = 1;

	
    private InternalEdgeListener edgeListener = new InternalEdgeListener();
    private NeighbourMessageHandler netviewsARP = new NetviewsNeighbourMessageHandler();

	// Lifted from org.onosproject.proxyarp
    private void addDefault(ConnectPoint port) {
        neighbourResolutionService.registerNeighbourHandler(port, netviewsARP, appId);
    }

    private void removeDefault(ConnectPoint port) {
        neighbourResolutionService.unregisterNeighbourHandler(port, netviewsARP, appId);
    }

    @Override
    public void reload() throws Exception {
	       setIdentityAndPolicy();
    }

    // JESS ADDED:
    // is called when get-ip command is called
    // this is set automatically by mininet script
    @Override
    public void setIP(String ip) {
	this.containerIP = ip;
    }
  
   // ADDED BY JESS -----------------
	// Edited by IA ------------------

    public void setIdentityAndPolicy() throws Exception {
         
	 identityMap = new IdentityMap();
         policyEngine = new PolicyEngine();
		 FileInputStream fis = new FileInputStream("/tmp/inputFiles.txt");
		 Scanner sc =new Scanner(fis);
		 while (sc.hasNextLine()) {
			 String line = sc.nextLine();
			 if (line.contains("IDENTITYMAPPING")) {
				 String fileName = line.split(",")[1];		
				 identityMap.createMapping(fileName);
			 }
		 }
		 policyEngine.createPolicyGraph("/tmp/inputFiles.txt");
    }

    // -----------------------------------

    private class InternalEdgeListener implements EdgePortListener {
        @Override
        public void event(EdgePortEvent event) {
            switch (event.type()) {
            case EDGE_PORT_ADDED:
                addDefault(event.subject());
                break;
            case EDGE_PORT_REMOVED:
                removeDefault(event.subject());
                break;
            default:
                break;
            }
        }
    }
	@Activate
	public void activate() throws Exception {
		appId = coreService.registerApplication("org.onosproject.netviews");

		packetService.addProcessor(processor, PacketProcessor.director(2));

		// JESS ADDED 
		setIdentityAndPolicy();
    
    
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

		selector.matchEthType(Ethernet.TYPE_IPV4);
        edgeService.addListener(edgeListener);
        edgeService.getEdgePoints().forEach(this::addDefault);

		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
		log.info("$$$$$$$$$$$$$$$$ Started");
	}

	@Deactivate
	public void deactivate() {
		packetService.removeProcessor(processor);
        edgeService.removeListener(edgeListener);
        neighbourResolutionService.unregisterNeighbourHandlers(appId);
		processor = null;
		log.info("Stopped");
	}

	/**
	 * Implements access control for ARP using NetViews policies
	 */
	private class NetviewsNeighbourMessageHandler implements NeighbourMessageHandler  {

		@Override
		public void handleMessage(NeighbourMessageContext context, HostService hostService) {
			log.info("\n\n IN ARP \n\n");
			if (context.type() != NeighbourMessageType.REQUEST) {
				log.info("Not a request, dropping.");
				context.drop();
				return;
			}

			Identity subject = null;
			Identity object = null;
			try {
				subject = identityMap.getHostIdentity(context.sender().toString());
			} catch (PMException e) {
				log.info("No identity found for source!");
				context.drop();
				return;
			} catch (IllegalArgumentException e) {
				log.info("Unable to parse ARP into IP address");
			}

			String expectedMAC = subject.getMac().toLowerCase();
			String actualMAC = context.srcMac().toString().toLowerCase();

			if (!expectedMAC.equals(actualMAC)) {
				log.info("ALERT: MAC address spoofing detected -- expected {} for {}, received {}",
						 expectedMAC, subject.getName(), actualMAC);
				context.drop();
				return;
			}

			try {
				log.info("in handle message");
				log.info(context.target().toString());
				object = identityMap.getHostIdentity(context.target().toString());
			} catch (PMException e) {
				log.info("No identity found for destination!" + "Context target: " + context.target().toString() + " !");
				context.drop();
				return;
			} catch (IllegalArgumentException e) {
				log.info("Unable to parse ARP into IP address");
			}

			log.info("\n @@@@@ ARP Request: " + subject.getName()+" "+object.getName());

			String destMAC = object.getMac();
			log.info("des mac");
			log.info(destMAC);

			// ARP access should be bidirectional
			// Conditions:
			// 1. if the node is not in the policy graph (this is to handle gateway not being in the policy graph, but should still allow arp)
			// 2. if the source node had access to the destination node OR
			// 3. if the destination node has access to the source node
			if (!policyEngine.checkNode(object.getName()) || policyEngine.getPermission(subject.getName(), object.getName(), "arp")
					|| policyEngine.getPermission(object.getName(), subject.getName(), "arp")) {
				log.info("ARP from {} to {} is permitted, replying.", actualMAC, destMAC);
				context.reply(MacAddress.valueOf(destMAC));
			} else {
				log.info("ARP from {} to {} is not permitted, dropping.", actualMAC, destMAC);
				context.drop();
			}
		}

	}

	/**
	 * Packet processor responsible for forwarding packets along their paths.
	 */
	
	private class ReactivePacketProcessor implements PacketProcessor  {

		@Override
		public void process(PacketContext context) {
			// Stop processing if the packet has been handled, since we
			// can't do any more to it.
			if (context.isHandled()) {
				//log.info("\nHandled\n");
				return;
			}
			log.info("\nPacket_in\n");
			//log.info("\nThreadname: " + Thread.currentThread().getName());
			log.info("\nSwitch: " + context.inPacket().receivedFrom());
			long inTime = System.nanoTime();

			InboundPacket pkt = context.inPacket();
			Ethernet ethPkt = pkt.parsed();

			HostId srcId = HostId.hostId(ethPkt.getSourceMAC());
			HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());

			// Do we know who this is for? If not, flood and bail.
			Host dst = hostService.getHost(dstId);
			IPacket ethPayload = ethPkt.getPayload();

			String sourceIP = "";
			String destIP = "";
			String sourceMAC = (ethPkt.getSourceMAC()).toString();
			String destMAC = (ethPkt.getDestinationMAC()).toString();
			String ingressPort = ((pkt.receivedFrom()).port()).toString();
			String egressPort = ""; // CURRENTLY UNUSED

			String protocol = "";
			String sourcePort = "";
			String destPort = "";

			String code = "";
			String type = "";

			TrafficSelector.Builder outSelector = DefaultTrafficSelector.builder()
					//.matchEthSrc(ethPkt.getSourceMAC())
					//.matchEthDst(ethPkt.getDestinationMAC())
					.matchEthType(ethPkt.getEtherType());
			TrafficSelector.Builder inSelector = DefaultTrafficSelector.builder()
					//.matchEthSrc(ethPkt.getDestinationMAC())
					//.matchEthDst(ethPkt.getSourceMAC())
					.matchEthType(ethPkt.getEtherType());


			boolean addIntent = false;
			boolean icmp_reply = false;
			short flags = 0;
			boolean tcp_ack = false;
			IPv4 ipv4Packet=null;
			TCP tcpPacket=null;
			int sourceAddress=0;
			int destinationAddress=0;

			if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
				log.info("\n\n IN IPV4 \n\n");
				ipv4Packet = (IPv4) ethPkt.getPayload();
				byte ipv4Protocol = ipv4Packet.getProtocol();
				sourceAddress = ipv4Packet.getSourceAddress();
				destinationAddress = ipv4Packet.getDestinationAddress();
				sourceIP = IpAddress.valueOf(sourceAddress).toString();
				destIP = IpAddress.valueOf(destinationAddress).toString();


				if (ipv4Protocol == IPv4.PROTOCOL_TCP) {
					log.info("\n\n IN IPV4 TCP \n\n");
					tcpPacket = (TCP) ipv4Packet.getPayload();

					// JESS ADDED: ----------------------------------------
					// - logic: this should check that its a TCP acknowledgement or not, and if it is to skip checking
					// the policy and automaticlly just install the intent
					flags = tcpPacket.getFlags();

					//16 is [ack] packet, 17 is [fin, ack] packet, 18 is [syn, ack] packet, 24 is [psh, ack] packet
					if ( (flags >= 16 && flags <= 18)  || flags==24 ) {
						log.info("flag is "+String.valueOf(flags));
						tcp_ack = true;
						addIntent = true;
					}

					//log.info("\n\n TCP FLAGS: " + String.valueOf(flags));
					//------------------------------------------------------

					protocol = "TCP";
					sourcePort = Integer.toString(tcpPacket.getSourcePort());
					destPort = Integer.toString(tcpPacket.getDestinationPort());

					outSelector = outSelector.matchIPProtocol(ipv4Packet.getProtocol());
					inSelector = inSelector.matchIPProtocol(ipv4Packet.getProtocol());
					//outSelector = outSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
							//.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
					// OPTIMIZED
					
					outSelector = outSelector.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
					inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()));

					outSelector = outSelector.matchIPDst(IpPrefix.valueOf(destinationAddress, 32));
					inSelector = inSelector.matchIPDst(IpPrefix.valueOf(sourceAddress, 32));

					outSelector = outSelector.matchIPSrc(IpPrefix.valueOf(sourceAddress, 32));
					inSelector = inSelector.matchIPSrc(IpPrefix.valueOf(destinationAddress, 32));
					
					// NOT OPTIMIZED
					/*
					outSelector = outSelector.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()))
								 .matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort())); // NOT OPTIMIZED
					inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
								 .matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
					*/
					//inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()));
									//.matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort())); // NOT OPTIMIZED
				}
				if (ipv4Protocol == IPv4.PROTOCOL_UDP) {
					log.info("\n\n IN IPV4 UDP \n\n");
					UDP udpPacket = (UDP) ipv4Packet.getPayload();
					protocol = "UDP";
					//sourcePort = Integer.toString(udpPacket.getSourcePort());
					//destPort = Integer.toString(udpPacket.getDestinationPort());
					//outSelector = outSelector.matchIPProtocol(ipv4Packet.getProtocol());
					//inSelector = inSelector.matchIPProtocol(ipv4Packet.getProtocol());
					//outSelector = outSelector.matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
					//inSelector = inSelector.matchUdpSrc(TpPort.tpPort(udpPacket.getDestinationPort()));
					sourcePort = Integer.toString(udpPacket.getSourcePort());
					destPort = Integer.toString(udpPacket.getDestinationPort());

					outSelector = outSelector.matchIPProtocol(ipv4Packet.getProtocol());
					inSelector = inSelector.matchIPProtocol(ipv4Packet.getProtocol());
					//outSelector = outSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
							//.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
							outSelector = outSelector.matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
					//inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
							//.matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
							inSelector = inSelector.matchUdpSrc(TpPort.tpPort(udpPacket.getDestinationPort()));
				}
				if (ipv4Protocol == IPv4.PROTOCOL_ICMP) {
					log.info("\n\n IN ICMP \n\n");

					outSelector = outSelector.matchIPProtocol(ipv4Packet.getProtocol());
					inSelector = inSelector.matchIPProtocol(ipv4Packet.getProtocol());

					outSelector = outSelector.matchIPDst(IpPrefix.valueOf(destinationAddress, 32));
					inSelector = inSelector.matchIPDst(IpPrefix.valueOf(sourceAddress, 32));

					outSelector = outSelector.matchIPSrc(IpPrefix.valueOf(sourceAddress, 32));
					inSelector = inSelector.matchIPSrc(IpPrefix.valueOf(destinationAddress, 32));


					ICMP icmpPacket = (ICMP) ipv4Packet.getPayload();
					protocol = "ICMP";
					code = String.valueOf(icmpPacket.getIcmpCode());
					type = String.valueOf(icmpPacket.getIcmpType());

					log.info("\n\n\n  TYPE:   " + type + "\n\n\n");
					log.info("\n\n\n SOURCE IP: " + sourceIP + "\n\n");
					log.info("\n\n\n DESTINATION IP: " + destIP + "\n\n");

					// JESS ADDED:
					// - if the ICMP type is 0 (aka an ICMP reply, source: https://www.iana.org/assignments/icmp-parameters/icmp-parameters.xhtml)
					// then we should not check the policy and just add the intent
					// - TODO: might want to change this so that it also checks that the reply is coming from a different site
					if (type.equals("0")) {
						addIntent = true;
						icmp_reply = true; 
					}
					//selector = selector.matchIcmpCode(icmpPacket.getIcmpCode())
					//		.matchIcmpType(icmpPacket.getIcmpType());
				}
			}
			else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV6) {
				log.info("\n\n IN IPV6 \n\n");
				IPv6 ipv6Packet = (IPv6) ethPkt.getPayload();
				byte ipv6Protocol = ipv6Packet.getNextHeader();
				sourceIP = Byte.toString(ipv6Packet.getSourceAddress()[0]);
				destIP = Byte.toString(ipv6Packet.getDestinationAddress()[0]);
				if (ipv6Protocol == IPv6.PROTOCOL_TCP) {
					tcpPacket = (TCP) ipv6Packet.getPayload();
					protocol = "TCP";
					sourcePort = Integer.toString(tcpPacket.getSourcePort());
					destPort = Integer.toString(tcpPacket.getDestinationPort());
					outSelector = outSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
							.matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
					inSelector = inSelector.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
							.matchTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
				}
				if (ipv6Protocol == IPv6.PROTOCOL_UDP) {
					log.info("\n\n IN IPV6 UDP \n\n");
					UDP udpPacket = (UDP) ipv6Packet.getPayload();
					protocol = "UDP";
					sourcePort = Integer.toString(udpPacket.getSourcePort());
					destPort = Integer.toString(udpPacket.getDestinationPort());
					outSelector = outSelector.matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
							.matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
					inSelector = inSelector.matchUdpSrc(TpPort.tpPort(udpPacket.getDestinationPort()))
							.matchUdpDst(TpPort.tpPort(udpPacket.getSourcePort()));
				}
				if (ipv6Protocol == IPv6.PROTOCOL_ICMP6) {
					log.info("\n\n IN IPV6 ICMP6 \n\n");
					ICMP icmpPacket = (ICMP) ipv6Packet.getPayload();
					protocol = "ICMP";
					code = String.valueOf(icmpPacket.getIcmpCode());
					type = String.valueOf(icmpPacket.getIcmpType());
					//selector = selector.matchIcmpCode(icmpPacket.getIcmpCode())
					//		.matchIcmpType(icmpPacket.getIcmpType());
				}
			}

			//log.info("\n###"+sourceIP + " "+ sourceMAC+" "+ ingressPort+" "+ destIP+" "+ destMAC+" "+ egressPort+" "+ protocol+" "+ sourcePort+" "+ destPort+"###\n");
		
			// ADDED BY JESS:
			// - this section checks whether or not the packet is going to a different site. Right now it is hardcoded for five sites,
			// so if you add more sites, if the container IPs change, if the host ips change inside each site, this section would need to be changed
			// to have new logic.	
			// Sends automatically if site is not in current site


			log.info("\n\n\n\n" + "CONTAINER IP: " + containerIP + "\n\n\n");
			log.info("\n\n\n SOURCE IP: " + sourceIP + "\n\n");
                        log.info("\n\n\n DESTINATION IP: " + destIP + "\n\n");
			String[] container_site = containerIP.split("\\.");
			String[] destination_site = destIP.split("\\.");
			String[] source_site = sourceIP.split("\\.");

			log.info("\n\n\n add intent, tcp ack, icmp reply, equality"+"\n\n");
			log.info("\n\n\n"+String.valueOf(addIntent)+"\n\n\n");
			log.info("\n\n\n"+String.valueOf(tcp_ack)+"\n\n\n");
			log.info("\n\n\n"+String.valueOf(icmp_reply)+"\n\n\n");

			//checkif it's a "Destination unreachable (Fragmentation needed)" sent from gateway to the client sending the packet 
            //no need to set return intent or verify return key since such packet would never come. 
            String gatewayIP="172.20."+(String.valueOf(container_site[3]))+".1";
            if (sourceIP.equals(gatewayIP) || destIP.equals(gatewayIP)){
                log.info("packet is to or from gateway. source and destination IPs are"+ sourceIP + " "+ destIP);
                
                TrafficSelector.Builder gatewaySelector = DefaultTrafficSelector.builder()
                                .matchEthType(ethPkt.getEtherType())
                                .matchIPSrc(IpPrefix.valueOf(sourceAddress, 32))
                                .matchIPDst(IpPrefix.valueOf(destinationAddress, 32));

                String gatewayIntent=sourceIP+destIP+"GATEWAYINTENT";
                log.info("gateway_intent_key : " + Key.of(gatewayIntent, appId));

                setUpConnectivity(context, srcId, dstId, gatewaySelector.build(),gatewayIntent, inTime, false);

                if (dst == null) {
                    log.info("unknown destination, flooding packet");
                    flood(context);
                }
                else{
                    forwardPacketToDst(context, dst);
                }
                return;
            }
			
			if (containerIP!="") {
				log.info("\n\n\n" + String.valueOf(container_site[3].equals(destination_site[2])) + "\n\n\n");

				if (!tcp_ack && !icmp_reply && !(container_site[3].equals(destination_site[2]))) {

					HostId gatewayHostId=null;
					try {
						InetAddress localIP = InetAddress.getByName("172.20."+container_site[3]+".1");

						NetworkInterface ni = NetworkInterface.getByInetAddress(localIP);
						byte[] macAddress = ni.getHardwareAddress();

						String[] hexadecimal = new String[macAddress.length];
						for (int i = 0; i < macAddress.length; i++) {
							hexadecimal[i] = String.format("%02X", macAddress[i]);
						}
						String macAddressString = String.join(":", hexadecimal);
						MacAddress macOfGateway= MacAddress.valueOf(macAddressString);
						gatewayHostId = HostId.hostId(macOfGateway);
						log.info("\n\n\n Installing Gateway Intent \n\n\n");

						TrafficSelector.Builder outSelectorGateway = DefaultTrafficSelector.builder()
								.matchEthType(ethPkt.getEtherType())
								.matchIPSrc(IpPrefix.valueOf(sourceAddress, 32))
								.matchIPDst(IpPrefix.valueOf(destinationAddress, 32));
						setUpConnectivity(context, srcId, gatewayHostId, outSelectorGateway.build(),sourceIP+destIP+"OTHERSITE", inTime, false);
						TrafficSelector.Builder inSelectorGateway = DefaultTrafficSelector.builder()
                                                                .matchEthType(ethPkt.getEtherType())
                                                                .matchIPDst(IpPrefix.valueOf(sourceAddress, 32))
                                                                .matchIPSrc(IpPrefix.valueOf(destinationAddress, 32));
                                                setUpConnectivity(context, gatewayHostId, srcId, inSelectorGateway.build(),destIP+sourceIP+"OTHERSITE", inTime, false);

					}
					catch (Exception e){
						log.info(e.toString());
					}

					log.info("\n\n\n SENDING TO A DIFFERENT SITE \n\n\n");
					if (dst == null) {
						log.info("\n\n\n unknown destination, flooding packet \n\n\n");
						flood(context);
					} else {
						// dont check policy, dont install an intent, just send the packet
						forwardPacketToDst(context, dst);
					}
					return;
				}
			}

			// Otherwise forward and be done with it

			// EDITED BY JESS:-----------
			// - initialized this above checking the ether type in order to be able to set it to true if its an 
			// icmp reply or tcp ack
			//boolean addIntent = false;
			//---------------------------
			//
			// ADDED BY JESS:f
			// - only if addIntent has not already been flipping to true...
			if (!addIntent) {
				try {
					log.info("\n Request Intent");
					addIntent = netViewsPolicyPlaceholder(sourceIP, sourceMAC, ingressPort, destIP, destMAC, egressPort, protocol, sourcePort, destPort, context);
					log.info("\nReceived "+addIntent);
				} catch (Exception e) {
					log.info("\nexception:\n");
					log.info(e.getLocalizedMessage());
				}
			}
 			
			String outPseudoKey = "";
			String inPseudoKey = "";

			outPseudoKey += (srcId.toString() + dstId.toString());
			inPseudoKey += (dstId.toString() + srcId.toString());

			//outPseudoKey += (sourceIP + sourceMAC + ingressPort + destIP + destMAC + egressPort + protocol + sourcePort + destPort + code + type);
			//inPseudoKey += (destIP + destMAC + egressPort + sourceIP + sourceMAC + ingressPort + protocol + destPort + sourcePort + code + type);

			// OPTIMIZED
			
			outPseudoKey += (sourceIP + sourceMAC + destIP + destMAC + protocol + destPort);//sourcePort + destPort + code + type);

			// ADDED BY JESS: --------------------------------------
			// - this might need to be changed once every intent isn't bidirectional already
			if (icmp_reply || tcp_ack) {
				inPseudoKey += (sourceIP + sourceMAC + destIP + destMAC + protocol + destPort + "RETURN"); //+ sourcePort + code + type);
			}
			else {
				inPseudoKey += (destIP + destMAC + sourceIP + sourceMAC + protocol + destPort + "RETURN"); //+ sourcePort + code + type);
			}

			// NOT OPTIMIZED
			/*
			outPseudoKey += (sourceIP + sourceMAC + destIP + destMAC + protocol + destPort + sourcePort);//sourcePort + destPort + code + type);
                        inPseudoKey += (destIP + destMAC + sourceIP + sourceMAC + protocol + sourcePort + destPort); //+ sourcePort + code + type);
			*/
			Key key;
			key = Key.of(outPseudoKey, appId);
			log.info("\nChecking Key: " + key.toString() + "\n" + Key.of(inPseudoKey, appId) + "\n");

			if (addIntent && (!icmp_reply) && (!tcp_ack) ) {
				// WAIT UNTIL INSTALL
				/*
				setUpConnectivity(context, srcId, dstId, outSelector.build(), outPseudoKey, inTime, true);
				setUpConnectivity(context, dstId, srcId, inSelector.build(), inPseudoKey, inTime, true);
				*/
                return_key_cache.put(inPseudoKey, true);
				// DO NOT WAIT UNTIL INSTALL

				if (dst == null) {
					log.info("unknown destination, flooding packet");
					flood(context);
				} else {
					// ADDED BY JESS:
					// - the idea is here, but also might need to change once intents aren't bidirectional
					// right now, any intent with the two hosts that you want will be able to send back and forth so hard to test
					// if it was unidirectional, but that would just involve playing around with what information the return intent needs
					// versus the forward intent.
					if (icmp_reply) {
						log.info("icmp reply");
						setUpConnectivity(context, srcId, dstId, outSelector.build(), outPseudoKey+"RETURN", inTime, false);
					}
					else if (tcp_ack) {
						log.info("\n\n\n ADDING INTENT FOR ACK\n\n\n");
						setUpConnectivity(context, srcId, dstId, inSelector.build(), inPseudoKey, inTime, false);
						setUpConnectivity(context, dstId, srcId, outSelector.build(), outPseudoKey, inTime, false);
					}
					else {
						setUpConnectivity(context, srcId, dstId, outSelector.build(), outPseudoKey, inTime, false);
						setUpConnectivity(context, dstId, srcId, inSelector.build(), inPseudoKey, inTime, false);
					}
					forwardPacketToDst(context, dst);
				}
			}
			else if ((!icmp_reply) && (!tcp_ack) && (!addIntent) )
			{
				//if the packet is not a return packet and policy says that intent is not allowed, simply exit without installing intents. 
				return;
			} else {
				if(! String.valueOf(source_site[2]).equals(String.valueOf(destination_site[2]))){
					log.info("\n\n\n inter-site packet \n\n\n");
					//include key checking

					if (dst == null) {
						log.info("\n\n\n unknown destination, flooding packet \n\n\n");
						flood(context);
					} else {
						// dont check policy, dont install an intent, just send the packet
						forwardPacketToDst(context, dst);
					}
				}
				else{
					// This is a fix for dropped return packet ins.
					String lostPseudoKey = "";
					lostPseudoKey += (srcId.toString() + dstId.toString());
					lostPseudoKey += (sourceIP + sourceMAC + destIP + destMAC + protocol + sourcePort + "RETURN");

					if (return_key_cache.containsKey(lostPseudoKey)) {
					log.info("\n LOST PACKET \n");
					forwardPacketToDst(context, dst);
					return;
					} else {
					// The intent should not be installed.
					log.info("\nFalse Policy, dropping: " + outPseudoKey + "\n" + inPseudoKey);
					return;
					}
				}
				
			}
		}
	}

	private void locationChangeEventProcessor(String userIP, String userLocation, String thisSite, String deviceName) throws IOException, PMException{
		// userLocation, is where Alice is right now; thisSite, where the ONOS-controller is running

		//To udnerstand what this fuction is doing assume Alice moves form site 1 to site 2
		// At site 2 a new identity would be generated and Alice's IPMapping and NAMEMapping would point to that
		// At site 1  a new identity would be generated (as alice has a new IP at site 2) and Alice's IPMapping and
		// NAMEMapping would point to that new identity. Old IPMApping for previous IP and identity would still exist
		// but the old IPNAMEMapping would be lost. Alice owuld not be able to access anyting from her old IP at site 1
		// since the policy would not allow it as location association had been updated.

		String[] arrOfStr = userIP.split("/", 0);
		userIP=arrOfStr[0];

		log.info("user ip:");
		log.info(userIP);

		String subjectName=null;
		try{
			subjectName = identityMap.getHostIdentity(userIP).getName();
		}
		catch(Exception e){

			log.info("got exception");

			identityMap.addnewIdentityMapping(userIP,userLocation,deviceName);

			log.info("In location change");
			log.info("getting host identity for ");
			log.info(userIP);
			subjectName = identityMap.getHostIdentity(userIP).getName();
			log.info("got");
			log.info(subjectName);

			Map <String, String>  user_arrival_info = new HashMap();
			user_arrival_info.put("source","MSNetviews App");
			user_arrival_info.put("time", String.valueOf(System.currentTimeMillis()));
			user_arrival_info.put("Name",subjectName);
			user_arrival_info.put("New Location",userLocation);
            //adding timestamp logic for time-to-invalidate calculations

			Gson gson = new Gson();
       	 	Type gsonType = new TypeToken<HashMap>(){}.getType();
        	String gsonString = gson.toJson(user_arrival_info,gsonType);

			FileWriter file = new FileWriter("/tmp/user_arrival_timestamps.json",true);
            file.write(gsonString);//jsonArray.toJSONString()
            file.flush();
            file.close();
			
		}
		finally {

			subjectName = identityMap.getHostIdentity(userIP).getName();

		}
		boolean locationChangeStatus = policyEngine.locationChangeEvent(subjectName, userLocation, thisSite);
		if(locationChangeStatus) {
			log.info("\n\n\n Location changed successfuly \n\n");	
				////////
				// TODO: Update location in location log
				//
				// //EDITED BY JESS:
				// - updates to the location log are done in separate files
				///////
		}
		else{
			log.info("\n\n\n Location did not change \n\n");
		}

	}
	
         // ADDED BY JESS: Allows ONOS command to trigger obligation
         @Override
         public void triggerObligation(String userIP, String userLocation, String thisSite, String deviceName) throws Exception {
               locationChangeEventProcessor(userIP, userLocation, thisSite, deviceName);
         }

	private boolean netViewsPolicyPlaceholder(
			String sourceIP, String sourceMAC, String ingressPort,
			String destIP, String destMAC, String egressPort,
			String protocol, String sourcePort, String destPort, PacketContext context) throws IOException, PMException {

		boolean permission = false;

		log.info("\n Request Permission");
		String subjectName = identityMap.getHostIdentity(sourceIP).getName();
		String objectName = identityMap.getHostIdentity(destIP).getName();
		//String subjectName = identityMap.getHostIdentity(sourceIP);
		//String objectName = identityMap.getHostIdentity(destIP);
		
		String actionName = protocol.toLowerCase()+"/"+destPort;

		log.info("\n @@@@@ Got 	Name: " + subjectName+" "+objectName+" "+actionName + " From: " + context.inPacket().receivedFrom());

		permission = policyEngine.getPermission(subjectName, objectName, actionName);

		//log.info("\n@@@@@ " + permission + " @@@@@@");
		//long outTime = System.nanoTime();
                //log.info("\n\nPolicy Time:" + String.valueOf(outTime - inTime) + "\n\n");
		
		return permission;
		
		//return true;
	}

	// Floods the specified packet if permissible.
	private void flood(PacketContext context) {
		if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
										context.inPacket().receivedFrom())) {
			packetOut(context, PortNumber.FLOOD);
		} else {
			context.block();
		}
	}

	// Sends a packet out the specified port.
	private void packetOut(PacketContext context, PortNumber portNumber) {
		context.treatmentBuilder().setOutput(portNumber);
		context.send();
	}

	private void forwardPacketToDst(PacketContext context, Host dst) {
		TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(dst.location().port()).build();
		OutboundPacket packet = new DefaultOutboundPacket(dst.location().deviceId(),
										treatment, context.inPacket().unparsed());
		packetService.emit(packet);
		log.info("sending packet: {}", packet);
	}

	// Install a rule forwarding the packet to the specified port.
	private void setUpConnectivity(PacketContext context, HostId srcId, HostId dstId, TrafficSelector selector, String pseudoKey, Long inTime, boolean waitInstalled) {
		//TrafficSelector selector = DefaultTrafficSelector.emptySelector();
		TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

		Key key;
	  /*if (srcId.toString().compareTo(dstId.toString()) < 0) {
	    key = Key.of(srcId.toString() + dstId.toString(), appId);
	  } else {
	    key = Key.of(dstId.toString() + srcId.toString(), appId);
	  }*/
		key = Key.of(pseudoKey, appId);

		// UNCOMMENT FOR JUST THE TIME TO INSTALL AN INTENT, WITHOUT NETVIEWS OVERHEAD.
		//inTime = System.nanoTime();intent

		HostToHostIntent intent = (HostToHostIntent) intentService.getIntent(key);
		// TODO handle the FAILED state
		if (intent != null) {
			//log.info("\nNon-null intent state: " + intentService.getIntentState(key));
			if (WITHDRAWN_STATES.contains(intentService.getIntentState(key))) {
				HostToHostIntent hostIntent = HostToHostIntent.builder()
						.appId(appId)
						.key(key)
						.one(srcId)
						.two(dstId)
						.selector(selector)
						.treatment(treatment)
						.priority(PRIORITY)
						.build();

				if (waitInstalled) {
					submitIntent(hostIntent, intentService);
				} else {
					intentService.submit(hostIntent);
				}
					//applyIntentSync(hostIntent, Direction.ADD);
			} else if (intentService.getIntentState(key) == IntentState.FAILED) {

				TrafficSelector objectiveSelector = DefaultTrafficSelector.builder()
						.matchEthSrc(srcId.mac()).matchEthDst(dstId.mac()).build();

				TrafficTreatment dropTreatment = DefaultTrafficTreatment.builder()
						.drop().build();

				ForwardingObjective objective = DefaultForwardingObjective.builder()
						.withSelector(objectiveSelector)
						.withTreatment(dropTreatment)
						.fromApp(appId)
						.withPriority(intent.priority() - 1)
						.makeTemporary(DROP_RULE_TIMEOUT)
						.withFlag(ForwardingObjective.Flag.VERSATILE)
						.add();

				flowObjectiveService.forward(context.outPacket().sendThrough(), objective);
			}

		} else if (intent == null) {
			HostToHostIntent hostIntent = HostToHostIntent.builder()
					.appId(appId)
					.key(key)
					.one(srcId)
					.two(dstId)
					.selector(selector)
					.treatment(treatment)
					.priority(PRIORITY)
					.build();

			if (waitInstalled) {
                                submitIntent(hostIntent, intentService);
                        } else {
                                intentService.submit(hostIntent);
                        }
			//intentService.submit(hostIntent);
			//submitIntent(hostIntent, intentService);
			//applyIntentSync(hostIntent, Direction.ADD);
		}

		PRIORITY -= 1;

		//ComputeTime computeTime = new ComputeTime(inTime, key);
		//Thread t = new Thread(computeTime);
		//t.start();
	}

	private boolean submitIntent(HostToHostIntent intent, 
				     IntentService intentService) {
		Key key = intent.key();
		intentService.submit(intent);
		IntentState state = intentService.getIntentState(key);
		log.info("Intent State: {}\n", state);

		while (state != INSTALLED && state != INSTALLING) {

			try {
                                Thread.sleep(2);
                        } catch (Exception e) {
                                System.out.println(e);
                        }
			
			state = intentService.getIntentState(key);
		
			try {
                                Thread.sleep(2);
                        } catch (Exception e) {
                                System.out.println(e);
                        }

			log.info("Intent State: {}\n", state);

		}
		//log.info("\n\nIntent State: {}\n\n", listener.getState());
		return false;
	}
	
	private final class InternalIntentListener implements IntentListener {
		private final HostToHostIntent intent;
		private final IntentService intentService;
		private final CountDownLatch latch;
		private IntentState state;

		private InternalIntentListener(HostToHostIntent intent, 
					       IntentService intentService,
					       CountDownLatch latch) {
			this.intent = intent;
			this.intentService = intentService;
			this.latch = latch;
		}

		@Override
		public void event(IntentEvent event) {
			if (event.subject().equals(intent)) {
				state = intentService.getIntentState(intent.key());
				if (state == INSTALLED || state == INSTALL_REQ) {
					latch.countDown();
					intentService.removeListener(this);
				}
			}
		}

		public IntentState getState() {
			return state;
		}
	}

	public void waitInstall(Key key) {
                while(intentService.getIntentState(key) != IntentState.INSTALLED) {
 	        	//do nothing
                        try {
                                Thread.sleep(1);
                        } catch (Exception e) {
                                System.out.println(e);
                        }
       
                }
        }


	public class ComputeTime implements Runnable {
		Long inTime;
		Key key;
		public ComputeTime(Long inTime, Key key) {
			this.inTime = inTime;
			this.key = key;
		}

		public void run() {
			while(intentService.getIntentState(key) != IntentState.INSTALLED) {
				//do nothing
				try {
                                        Thread.sleep(1);
                                } catch (Exception e) {
                                        System.out.println(e);
                                }
			}
			long outTime = System.nanoTime();
			log.info("\nIntent " + (intentService.getIntent(key).id()).toString() + ": " + key.toString() + " Installed Time: " + String.valueOf(outTime - inTime) + "\n\n");
		}
	}
}
