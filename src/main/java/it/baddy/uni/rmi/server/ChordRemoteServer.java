package it.baddy.uni.rmi.server;

import it.baddy.uni.chord.ChordNode;
import it.baddy.uni.rmi.client.ChordRemoteClient;
import it.baddy.uni.rmi.interfaces.ChordRemoteInterface;
import it.baddy.uni.rmi.interfaces.NodeAction;
import it.baddy.uni.utils.FileUtils;

import java.math.BigInteger;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import static it.baddy.uni.config.AppManagements.FILE_NOT_FOUND_EXCEPTION_STRING;

public class ChordRemoteServer extends UnicastRemoteObject implements ChordRemoteInterface {

    private final ChordNode chordNode;
    public ChordRemoteServer(ChordNode chordNode) throws RemoteException {
        super();
        this.chordNode = chordNode;
    }

    @Override
    public <T> T routeToResponsible(int keyId, NodeAction<T> action, String actionName) throws RemoteException {
        System.out.println("requested routeToResponsible for keyId: " + keyId + " and action: " + actionName + " from external node");
        return chordNode.routeToResponsible(keyId, action, actionName, 0);
    }

    @Override
    public int lookup(int id) throws RemoteException {
        System.out.println("requested lookup for id: " + id + " from external node");
        return chordNode.lookup(id);
    }

    @Override
    public byte[] getFile(String fileName) throws RemoteException {
        System.out.println("requested file: " + fileName + " from external node");
        // 1. Calcolo KeyID del file
        BigInteger keyIdBig = FileUtils.hashFilename(fileName);
        int keyId = keyIdBig.mod(BigInteger.valueOf(chordNode.getFingerTable().getRingSize())).intValue();


        return chordNode.routeToResponsible(keyId, node -> {
                Map<BigInteger, Path> index = FileUtils.indexFiles(Path.of("/data"));
                Path path = index.get(keyIdBig);
                if (path == null)
                    throw new RemoteException(FILE_NOT_FOUND_EXCEPTION_STRING);
                return FileUtils.readFile(path);
            }, "GET_FILE", 0);

    }

    @Override
    public void uploadFile(String fileName, byte[] content) throws RemoteException {
        System.out.println("received file: " + fileName + " from external node");

        BigInteger keyIdBig = FileUtils.hashFilename(fileName);
        int keyId = keyIdBig.mod(BigInteger.valueOf(chordNode.getFingerTable().getRingSize())).intValue();

        chordNode.routeToResponsible(keyId, node->{
                // nodo responsabile: salva il file
                Path fileCompleteName = Path.of("/data").resolve(fileName);
                FileUtils.writeFile(fileCompleteName, content);
                System.out.println("File " + fileName + " saved on node " + node.getFingerTable().getNodeId());
                return null;
            }, "UPLOAD_FILE", 0);
    }

    @Override
    public boolean updateFile(String fileName, byte[] content) throws RemoteException {
        System.out.println("update request for file: " + fileName);
        boolean created = false;
        BigInteger keyIdBig = FileUtils.hashFilename(fileName);
        int keyId = keyIdBig.mod(BigInteger.valueOf(chordNode.getFingerTable().getRingSize())).intValue();

        created = chordNode.routeToResponsible(keyId, node -> {
                    // nodo responsabile: sovrascrive o crea il file
                    Path fileCompleteName = Path.of("/data").resolve(fileName);

                    boolean createdInner = FileUtils.updateFile(fileCompleteName, content);
                    System.out.println("File " + fileName + " updated on node " + node.getFingerTable().getNodeId());
                    return createdInner;
                }, "UPDATE_FILE", 0);
        return created;
    }


    @Override
    public void deleteFile(String fileName) throws RemoteException {
        System.out.println("delete request for file: " + fileName);

        BigInteger keyIdBig = FileUtils.hashFilename(fileName);
        int keyId = keyIdBig.mod(BigInteger.valueOf(chordNode.getFingerTable().getRingSize())).intValue();

        chordNode.routeToResponsible(keyId, node-> {
                // nodo responsabile: cancella il file
                Map<BigInteger, Path> index = FileUtils.indexFiles(Path.of("/data"));
                Path path = index.get(keyIdBig);
                if (path != null && FileUtils.deleteFile(path)) {
                    System.out.println("File " + fileName + " deleted from node " + node.getFingerTable().getNodeId());
                } else {
                    throw new RemoteException(FILE_NOT_FOUND_EXCEPTION_STRING);
                }
                return null;
            }, "DELETE_FILE", 0);
    }


    @Override
    public int getNodeId() throws RemoteException {
        return chordNode.getFingerTable().getNodeId();
    }


    @Override
    public void controlledLeave() throws RemoteException {
        System.out.println("received controlled leave request");

        // ottieni i file dal proprio percorso per cui si è responsabili
        Map<BigInteger, Path> responsibleFiles = chordNode.getResponsibleFiles(Path.of("/data"));

        try{
            //di al proprio predecessore che stai lasciando la DHT
            //hostname="node"+chordNode.getPredecessor();
            //ChordRemoteClient.connect(hostname, 1099).notifyLeave(chordNode.getFingerTable().getNodeId());


            //definisci hostname del successore
            String hostname="node"+chordNode.getFingerTable().getSuccessor();
            //di al proprio successore che stai lasciando la DHT
            int myId = chordNode.getFingerTable().getNodeId();
            ChordRemoteClient.connect(hostname, 1099).notifyLeave(myId, myId);


            //dopo aver notificato il leave al successore lui accetterà le chiavi
            for (Path file : responsibleFiles.values()) {
                //ottieni il contenuto
                byte[] content = FileUtils.readFile(file);

                //inviali al successore
                ChordRemoteClient.connect(hostname, 1099).uploadFile(file.getFileName().toString(), content);

            }


        } catch (RemoteException re){
            System.err.println("error during remote execution, it is impossible leave the network now");
            throw new RemoteException("Failed to contact successor or predessor node ", re);
        } catch (Exception e) {
            System.err.println("generic error during execution, the node cannot leave the network");
            throw new RuntimeException(e);
        }

        System.out.println("Node " + chordNode.getFingerTable().getNodeId() + " left network.");

        //fai davvero terminare il container
        System.exit(0);
    }

    @Override
    public void notifyLeave(int nodeId, int originId) throws RemoteException, Exception {
        System.out.println("received notifyLeave request from node: " + nodeId + " with originId: " + originId);
        boolean to_send = true;
        //se il nodo origine è lo stesso del nodo che sta per lasciare la rete
        // sono il primo a ricevere il messaggio, modifico l'origine col mio id
        //e divento responsabile per smettere di propagare il messaggio
        if(originId == nodeId)
            originId = chordNode.getFingerTable().getNodeId();
        //se l'origine è il mio successore allora posso fermarlo
        else if(originId == chordNode.getFingerTable().getSuccessor())
            to_send = false;

        chordNode.checkNodesAndRemove(nodeId);

        //propaga verso il prossimo nodo
        int successor = chordNode.getFingerTable().getSuccessor();
        if (to_send) {
            ChordRemoteClient
                    .connect("node" + successor, 1099)
                    .notifyLeave(nodeId, originId);
        }
    }

    @Override
    public String printFingerTable() throws RemoteException {
        return chordNode.getFingerTable().printRemote();
    }


}
