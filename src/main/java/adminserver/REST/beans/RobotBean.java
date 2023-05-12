package adminserver.REST.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RobotBean {
    private int id;
    private String ipAddress;
    private int portNumber;
    public RobotBean() {
    }
    public RobotBean(int id, String ipAddress, int portNumber) {
        if (id <= 0 || ipAddress.equals("") || portNumber <= 0) {
            throw new IllegalArgumentException("Creating a robot with invalid parameters: id=" + id + ", ipAddress=" + ipAddress + ", portNumber=" + portNumber + ".");
        }
        this.id = id;
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
    }
    public int getId() {
        return this.id;
    }
    public String getIpAddress() {
        return this.ipAddress;
    }
    public int getPortNumber() {
        return this.portNumber;
    }
    public void setId(int id) {
        this.id = id;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }
}
