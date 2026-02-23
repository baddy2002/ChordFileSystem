package it.baddy.uni.rmi.bootstrap;


import it.baddy.uni.rmi.interfaces.ChordRemoteInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiBootstrap {

    public static void start(int port, String hostname, ChordRemoteInterface remote) throws Exception {

        System.setProperty("java.rmi.server.hostname", hostname);

        Registry registry = LocateRegistry.createRegistry(port);

        registry.rebind("ChordNode", remote);

        System.out.println("RMI registry started on " + hostname + ":" + port);
    }
}