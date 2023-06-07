package org.onosproject.netviews;

public class Identity {
    private String Name;
    private String Description;
    private String Type;
    private String ID;
    private String MAC;
    private String Location;
    private String IP;
    private String Port;

    Identity(String name, String description, String type, String ID, String MAC, String location, String IP, String port) {
        this.Name = name;
        this.Description = description;
        this.Type = type;
        this.ID = ID;
        this.MAC = MAC;
        this.Location = location;
        this.IP = IP;
        this.Port = port;
    }


    public String getName() {
        return Name;
    }

    public String getDescription() {
        return Description;
    }

    public String getType() {
        return Type;
    }

    public String getID() {
        return ID;
    }

    public String getMac() {
        return MAC;
    }

    public String getLocation() {
        return Location;
    }

    public String getIP() {
        return IP;
    }

    public String getPort() {
        return Port;
    }
}
