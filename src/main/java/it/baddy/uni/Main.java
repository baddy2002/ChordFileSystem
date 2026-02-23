package it.baddy.uni;

import it.baddy.uni.chord.ChordNode;
import it.baddy.uni.rmi.bootstrap.RmiBootstrap;
import it.baddy.uni.rmi.server.ChordRemoteServer;

import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {

        String nodeIdEnv = System.getenv("NODE_ID");
        //per restrizione di chord deve essere conosciuto altrimenti non posso calcolare m=log(n)
        String allNodesEnv = System.getenv("ALL_NODES");
        String nodePortEnv = System.getenv("NODE_PORT");
        String hostname = System.getenv("NODE_HOSTNAME");
        checkEnvVariable(nodeIdEnv, allNodesEnv, nodePortEnv, hostname);

        int nodeId = Integer.parseInt(nodeIdEnv);

        // Estrae solo gli ID numerici da nodeX
        List<Integer> allNodes = Arrays.stream(allNodesEnv.split(","))
                .map(name -> name.replace("node", "")) // X
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toList());

        System.out.println("=================================");
        System.out.println("Starting Chord node " + nodeId);
        System.out.println("All nodes: " + allNodes);
        System.out.println("=================================");

        ChordNode chordNode = new ChordNode(nodeId, allNodes);
        //stampa finger table del nodo
        chordNode.getFingerTable().print();
        //cartella separata per ogni nodo
        chordNode.printResponsibleKeys(Path.of("/data"));

        int port = Integer.parseInt(nodePortEnv);
        try{
            ChordRemoteServer remoteServer = new ChordRemoteServer(chordNode);
            RmiBootstrap.start(port, hostname, remoteServer);
        } catch (RemoteException re){
            System.err.println("error during remote execution");
        } catch (Exception e){
            System.err.println("generic error occurred trying to start rmi service");
        }

    }

    private static void checkEnvVariable(String nodeId, String allNodesEnv, String nodePortEnv, String hostNameEnv){
        if (nodeId == null) {
            System.err.println("NODE_ID not set");
            System.exit(1);
        }
        if (allNodesEnv == null) {
            System.err.println("ALL_NODES not set");
            System.exit(1);
        }
        if(nodePortEnv == null){
            System.err.println("NODE_PORT not set");
            System.exit(1);
        }
        if(hostNameEnv == null){
            System.err.println("HOSTNAME not set");
            System.exit(1);
        }
    }
}
