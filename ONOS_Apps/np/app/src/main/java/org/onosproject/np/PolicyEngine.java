package org.onosproject.np.impl;

import com.google.gson.Gson;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.decider.Decider;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.relationships.Assignment;
import gov.nist.csd.pm.pip.graph.model.relationships.Association;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.NotActiveException;
import java.rmi.NoSuchObjectException;
import java.util.NoSuchElementException;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import org.osgi.service.component.annotations.Component;
import org.onosproject.np.NetviewsService;

@Component(immediate = true,
           service = {NetviewsService.class})
public class PolicyEngine implements NetviewsService{

    Random rand = new Random();
    Graph graph = new MemGraph();
    private final Logger log = getLogger(getClass());
    Decider decider;

    @Override
    public void createPolicyGraph(String filePath) throws FileNotFoundException,IOException, PMException{
        log.info("\n&&& createPolicyGraph &&&\n");
        try
        {
            //File file = new File("/home/ianjum/GitNetViews/netviews-code/netviews-policy-machine/src/policyInput/policySample01.json");
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String json = new String(data, "UTF-8");


            GraphSerializer.fromJson(graph, json);
            
            decider = new PReviewDecider(graph, null);

        } catch (FileNotFoundException f){
            log.info(f.getLocalizedMessage());
        }catch (IOException e) {
            log.info(e.getLocalizedMessage());
        }
        catch (PMException p){
            log.info("PM"+p.getMessage());
        }

    }

    @Override
    public boolean getPermission(String subject, String object, String action) throws IOException, PMException{        
        Set<String> permissions = decider.list(subject, "0" , object);
        return permissions.contains(action);
	/*if (subject.equals("h10") && object.equals("h13") && action.equals("tcp/80")){
		return true;
	} else {
		return false;
	}*/
    }
}
