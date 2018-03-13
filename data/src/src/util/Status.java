package util;

public class Status {
    public static byte OFFLINE = 0,
            HANDSHAKE = 1,
            ONLINE = 2,
            AWAY = 3,
            XA = 4;
    
    //I would replace this class with an enum, but existing codebase relies on byte type. May refactor at later date
    //TODO determine if replacing class with enum is advisable
}
