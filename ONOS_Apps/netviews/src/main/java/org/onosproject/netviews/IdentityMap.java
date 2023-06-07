package org.onosproject.netviews;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import gov.nist.csd.pm.exceptions.PMException;
//import com.google.gson.JsonParser

public class IdentityMap {
    HashMap <String, Identity> IPMapping = new HashMap<String, Identity>();
    HashMap <String, Identity> USERMapping = new HashMap<String, Identity>();
    HashMap <String, Identity> NAMEMapping = new HashMap<String, Identity>();

    public void addnewIdentityMapping(String userIP,String userLocation,String deviceName){
        //JsonObject node = (JsonObject) JsonParser.parseString(identity_json_string);
        //USER node1 = jsonNode.getUsers();

        //not sure yet if we need to set correct MAC.
        String MAC="";
        String port="";
        Identity newNode = new Identity(deviceName, "host", "Host", "1", MAC,userLocation, userIP, port);

        //I might need to remove old ip for identity mapping of old site of alice. On second thoughts I can keep old identity since the location of user Alice would
        //have changed in policy graph and access from Alice's old IP would not be granted. Keeping the old identity is also useful since when Alice moves to site 1 again we
        //will already have the old identity

//        IPMapping.put(node.Device.get("IP"), newNode);
//        USERMapping.put(node.User, newNode);
//        NAMEMapping.put(node.Device.get("Name"), newNode);

        IPMapping.put(userIP, newNode);
       //USERMapping.put(node.User, newNode);
        NAMEMapping.put(deviceName, newNode);

    }
    public void createMapping(String fileName) throws IOException {

        Reader reader = new FileReader(fileName);
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        String jsonFile = new String(data, "UTF-8");

        JsonNode jsonNode = new Gson().fromJson(jsonFile, JsonNode.class);

        Collection<USER> nodes = jsonNode.getUsers();

        for (USER node : nodes) {

            Identity newNode = new Identity(node.Device.get("Name"), node.Device.get("Description"), node.Device.get("Type"), node.Device.get("ID"), node.Device.get("MAC"),node.Device.get("Location"), node.Device.get("IP"), node.Device.get("Port"));

            IPMapping.put(node.Device.get("IP"), newNode);
            USERMapping.put(node.User, newNode);
            NAMEMapping.put(node.Device.get("Name"), newNode);

        }

        Collection<OBJECT> objects = jsonNode.getObjects();

        for (OBJECT node : objects) {

            Identity newNode = new Identity(node.Name, node.Description, node.Type, node.ID, node.MAC, node.Location, node.IP, node.Port);
            IPMapping.put(node.IP, newNode);
            NAMEMapping.put(node.Name, newNode);
        }

    }

    public Identity getHostIdentity(String ip) throws PMException {
        if(!IPMapping.containsKey(ip)) {
            throw new PMException("Identity does not exist for IP " + ip);
        }
        return IPMapping.get(ip);

    }

    private class JsonNode {
        Collection<USER> Users;
        Collection<OBJECT> Objects;

        public Collection<USER> getUsers() {
            return Users;
        }

        public Collection<OBJECT> getObjects() {
            return Objects;
        }

        JsonNode(Collection<USER> Users, Collection<OBJECT> Objects) {
            this.Users = Users;
            this.Objects = Objects;
        }

    }

}

class USER {
    String User;
    Map<String, String> Device;

    USER(String user, Map<String, String> devices){
        this.User = user;
        this.Device = devices;
    }

}

class OBJECT {
    String Name;
    String Description;
    String Type;
    String ID;
    String MAC;
    String Location;
    String IP;
    String Port;

    public OBJECT(String name, String description, String type, String ID, String MAC, String location, String IP, String port) {
        this.Name = name;
        this.Description = description;
        this.Type = type;
        this.ID = ID;
        this.MAC = MAC;
        this.Location = location;
        this.IP = IP;
        this.Port = port;
    }

}

