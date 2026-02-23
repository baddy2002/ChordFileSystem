package it.baddy.uni.rmi.client;

import it.baddy.uni.rmi.interfaces.ChordRemoteInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ChordRemoteClient {

    public static ChordRemoteInterface connect(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        return (ChordRemoteInterface) registry.lookup("ChordNode");
    }
}
