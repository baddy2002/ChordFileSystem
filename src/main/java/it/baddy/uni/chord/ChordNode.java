package it.baddy.uni.chord;

import it.baddy.uni.rmi.interfaces.ChordRemoteInterface;
import it.baddy.uni.rmi.client.ChordRemoteClient;
import it.baddy.uni.rmi.interfaces.NodeAction;
import it.baddy.uni.utils.FileUtils;

import java.math.BigInteger;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.*;

import static it.baddy.uni.config.AppManagements.FILE_NOT_FOUND_EXCEPTION_STRING;

public class ChordNode {
    private static final int MAX_RETRY = 10;

    private final int nodeId;

    private final List<Integer> allNodes;
    private final FingerTable fingerTable;
    private int predecessor;

    public ChordNode(int nodeId, List<Integer> allNodes) {
        this.nodeId = nodeId;
        this.allNodes = allNodes;
        this.fingerTable = new FingerTable(nodeId, allNodes);
        this.predecessor = computePredecessor();
    }

    //predeccessor per facilitare responsabilità ed eventuali leave
    private int computePredecessor() {
        int idx = allNodes.indexOf(nodeId);
        return idx == 0 ? allNodes.getLast()
                : allNodes.get(idx - 1);
    }

    //capire se il nodo è responsabile per quella chiave
    // (vero se:
    // 1. la chiave è maggiore del precedente ma non di lui
    // 2. nel caso in cui il precedente di lui è maggiore(il nodeId è il primo):
    //      2a. se la chiave è minore di lui
    //      2b. la chiave è maggiore del predecessor(nessuno più grande di lui può diventare responsabile))
    private boolean isResponsible(int keyId) {
        if (predecessor < nodeId) {
            return keyId > predecessor && keyId <= nodeId;
        } else {
            return keyId > predecessor || keyId <= nodeId;
        }
    }

    private int closestPrecedingFinger(int id) {
        int[] fingers = fingerTable.getFingers();
        //parto dalla fine e trovo il primo nodeId tra il mio e la chiave
        for (int i = fingers.length - 1; i >= 0; i--) {
            if (isInInterval(fingers[i], nodeId, id)) {
                return fingers[i];
            }
        }

        return nodeId;
    }

    private boolean isInInterval(int id, int start, int end) {
        // intervallo normale
        if (start < end) {
            //es key=5 start=3 end=7
            return id > start && id <= end;
        } else if (start > end) { // intervallo con wrap
            //es key=7(o 1) start=5 end=2
            return id > start || id <= end;
        } else {
            // start == end, il nodo è l'unico nel ring
            return true;
        }
    }


    public int getPredecessor() {
        return predecessor;
    }

    public void printResponsibleKeys(Path filesDir) {

        Map<BigInteger, Path> index = FileUtils.indexFiles(filesDir);

        System.out.println("Node " + nodeId + " predecessor = " + predecessor);

        index.forEach((key, path) -> {
            int keyId = key.mod(BigInteger.valueOf(fingerTable.getRingSize())).intValue();

            if (isResponsible(keyId)) {
                System.out.println("  responsible for key " + keyId +
                        " (file " + path.getFileName() + ")");
            }
            else {
                System.out.println("NOT responsible for keyId: "+keyId);
            }
        });
    }

    //restituisce i file per cui il NodeID è responsabile del KeyID
    public Map<BigInteger, Path> getResponsibleFiles(Path filesDir) {
        Map<BigInteger, Path> index = FileUtils.indexFiles(filesDir);
        Map<BigInteger, Path> responsibleFiles = new HashMap<>();
        index.forEach((key, path) -> {
            int keyId = key.mod(BigInteger.valueOf(fingerTable.getRingSize())).intValue();

            if (isResponsible(keyId)) {
                responsibleFiles.put(key, path);
            }

        });
        return responsibleFiles;
    }

    public FingerTable getFingerTable() {
        return fingerTable;
    }

    public int lookup(int id) {

        //se appartiene al nodo risponde subito
        if(isResponsible(id))
            return nodeId;

        //anche il successore può essere ritornato
        int successor = fingerTable.getSuccessor();
        if (isInInterval(id, nodeId, successor)) {
            return successor;
        }


        int nextNode = closestPrecedingFinger(id);

        //se non sono stati trovati nodi
        if (nextNode == nodeId) {
            return successor;
        }

        //manda richiesta al nodo più vicino sulla finger table
        try {
            System.out.println("calling lookup on remote node: "+ nextNode);
            ChordRemoteInterface remote = ChordRemoteClient.connect("node" + nextNode, 1099);
            return remote.lookup(id);
        } catch (RemoteException re){
            System.err.println("error during remote lookup call: " + re.detail);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return -1; //error
    }

    public void checkNodesAndRemove(int nodeId){
        int idx = allNodes.indexOf(nodeId);
        if(idx!=-1){
            System.out.println("removing node "+nodeId+" from list");
            allNodes.remove(idx);
        }
        //controlla se devi aggiornare il predecessor
        this.predecessor = computePredecessor();
        System.out.println("new predecessor is: "+predecessor);
        //aggiorna se necessario il successor
        fingerTable.updateFingersChecked(nodeId, allNodes);

    }

    public <T> T routeToResponsible(int keyId, NodeAction<T> action, String actionName, int depth) throws RemoteException {
        if (depth > MAX_RETRY) {
            throw new RuntimeException("Max routing retries exceeded");
        }

        System.out.println("routing to responsible node for keyId: " + keyId + " with action: " + actionName);
        if (isResponsible(keyId)) {
            // sono responsabile, eseguo direttamente
            return action.execute(this);
        }

        int successor = fingerTable.getSuccessor();
        if (isInInterval(keyId, nodeId, successor)) {
            try {
                System.out.println("sending routeToResponsible request to successor: " + successor + " for action: " + actionName);
                ChordRemoteInterface remote = ChordRemoteClient.connect("node" + successor, 1099);
                return remote.routeToResponsible(keyId, action, actionName);
            } catch (RemoteException e) {
                throw new RuntimeException("Routing failed to successor " + successor, e);
            } catch (Exception e){
                throw new RuntimeException("Generic error during execution: ", e);
            }
        }

        int nextNode = closestPrecedingFinger(keyId);
        if (nextNode == nodeId) {
            nextNode = successor; // fallback
        }

        try {
            System.out.println("sending routeToResponsible request to node: " + nextNode + " for action: " + actionName);
            ChordRemoteInterface remote = ChordRemoteClient.connect("node" + nextNode, 1099);
            return remote.routeToResponsible(keyId, action, actionName);
        } catch (RemoteException e) {
            if (isFileNotFoundException(e)) {
                // errore applicativo, NON rimuovere nodo
                throw e;
            }
            //se un nodo non risponde, lo rimuovo dalla finger table
            System.out.println("Node " + nextNode + " unreachable. Removing from finger table.");
            checkNodesAndRemove(nextNode);

            //riprova dopo aver aggiornato la finger table
            return routeToResponsible(keyId, action, actionName,depth + 1);

        }catch (Exception e){
            throw new RuntimeException("Generic error during execution: ", e);
        }
    }

    public boolean isFileNotFoundException(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause instanceof RemoteException &&
                FILE_NOT_FOUND_EXCEPTION_STRING.equals(cause.getMessage());
    }

}
