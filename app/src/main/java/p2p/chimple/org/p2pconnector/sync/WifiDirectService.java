package p2p.chimple.org.p2pconnector.sync;

public class WifiDirectService {
    private String instanceName;
    private String serviceType;
    private String deviceAddress;
    private String deviceName;

    public WifiDirectService(String instance,String type,String address, String name){
        this.instanceName = instance;
        this.serviceType = type;
        this.deviceAddress = address;
        this.deviceName =  name;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }


    public String getInstanceName() {
        return instanceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }
}
