package util;

import core.Buddy;

import java.util.HashMap;

public class StatusUtils {
    private static HashMap<Byte, String> statusNameMap;
    
    
    static {
        statusNameMap = new HashMap<>();
        statusNameMap.put(Status.OFFLINE,"Offline");
        statusNameMap.put(Status.AWAY,"Away");
        statusNameMap.put(Status.HANDSHAKE,"Handshake");
        statusNameMap.put(Status.ONLINE,"Online");
        statusNameMap.put(Status.XA,"Extended Away");
    }
    
    public static String getStatusName(byte b) {
        return statusNameMap.getOrDefault(b, "Idk.");
        //NOTE that default return value has not been changed from orginal in case it breaks something.
        //FIXME default return value
    }
}
