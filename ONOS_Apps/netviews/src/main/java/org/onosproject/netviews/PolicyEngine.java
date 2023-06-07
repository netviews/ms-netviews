package org.onosproject.netviews;

import com.google.gson.Gson;
import gov.nist.csd.pm.epp.EPPOptions;
import gov.nist.csd.pm.epp.events.LocationEventContext;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.decider.Decider;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pdp.services.UserContext;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.relationships.Assignment;
import gov.nist.csd.pm.pip.graph.model.relationships.Association;

import gov.nist.csd.pm.epp.EPPOptions;
import gov.nist.csd.pm.epp.events.AssignToEvent;
import gov.nist.csd.pm.epp.events.EventContext;

import gov.nist.csd.pm.pip.obligations.MemObligations;
import gov.nist.csd.pm.pip.obligations.Obligations;
import gov.nist.csd.pm.pip.obligations.evr.EVRParser;
import gov.nist.csd.pm.pip.obligations.model.Obligation;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;
import gov.nist.csd.pm.pip.prohibitions.Prohibitions;
import gov.nist.csd.pm.pip.prohibitions.ProhibitionsSerializer;
import gov.nist.csd.pm.pip.prohibitions.model.Prohibition;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.NotActiveException;
import java.rmi.NoSuchObjectException;
import java.util.NoSuchElementException;

import static gov.nist.csd.pm.operations.Operations.*;
import static gov.nist.csd.pm.operations.Operations.ASSIGN_TO;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class PolicyEngine {

    Random rand = new Random();
    Graph graph = new MemGraph();
    private final Logger log = getLogger(getClass());
    Decider decider;
    Prohibitions prohibitions = new MemProhibitions();

    PDP pdp;
    UserContext userCtx;

    public void createPolicyGraph(String inputFile) throws PMException, IOException {
        FileInputStream fisi = new FileInputStream(inputFile);
        Scanner sc =new Scanner(fisi);
        while (sc.hasNextLine()){
            String line = sc.nextLine();
            if(line.contains("POLICYGRAPH") || line.contains("PROHIBITION")){

                String fileName = line.split(",")[1];
                File file = new File(fileName);
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                String json = new String(data, "UTF-8");
                if(line.contains("POLICYGRAPH")) {
                    System.out.println("Creating Policy Graph");
                    GraphSerializer.fromJson(graph, json);
                }
                else if (line.contains("PROHIBITION")) {
                    System.out.println("Adding Prohibition");
                    ProhibitionsSerializer.fromJson(prohibitions, json);
                }
            }
            if(line.contains("OBLIGATION")){
                System.out.println("\nCreate Admin Tree");
                Node adminua1 = graph.createNode("adminua1", UA, Node.toProperties("k1", "v1"), "location");
                Node admin = graph.createNode("admin", U, Node.toProperties("k", "v"), adminua1.getName());
                graph.associate(adminua1.getName(), "enterprise", new OperationSet(READ, WRITE, ASSIGN, ASSIGN_TO));
                graph.associate(adminua1.getName(), "knownUser", new OperationSet(READ, WRITE, ASSIGN, ASSIGN_TO));

                Obligations obligations = new MemObligations();
                pdp = new PDP(new PAP(graph, prohibitions, obligations), new EPPOptions());
                userCtx = new UserContext(admin.getName(), "");

                String[] obligationfiles = line.split(",");
                for(String name: obligationfiles){
                    if(!name.equals("OBLIGATION")){
                        System.out.println("\nAdd Obligation");
                        InputStream inputStream = new FileInputStream(new File(name));
                        Obligation obligation = EVRParser.parse("admin", inputStream);
                        pdp.getObligationsService(userCtx).add(obligation, true);
                    }
                }
            }
        }
        decider = new PReviewDecider(graph, prohibitions);

    }

    public boolean locationChangeEvent(String userDevicePair, String location, String thisLocation) throws PMException, IOException {

        if(graph.getNode(userDevicePair)==null){
            return false;
        }
        else {
            //System.out.println("Updating location in policy");
            Node userNode = graph.getNode(userDevicePair);
            Node siteUA = graph.getNode(location);
            pdp.getEPP().processEvent(new AssignToEvent(userCtx, siteUA, userNode));

            if (graph.isAssigned(userDevicePair, location)) {
                return true;
            }
        }

        return false;

    }


    public boolean getPermission(String subject, String object, String action) {
        try {
            Set<String> permissions = decider.list(subject, "0" , object);
            return permissions.contains(action);
        } catch (PMException p) {
            return false;
        }
    }
    
    public boolean checkNode(String target) {
        try {
                Node test = graph.getNode(target);
                return true;
        }
        catch (PMException p) {
                return false;
        }
    }
}
