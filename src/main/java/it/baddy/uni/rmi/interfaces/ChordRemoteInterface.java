package it.baddy.uni.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChordRemoteInterface extends Remote {

    <T> T routeToResponsible(int keyId, NodeAction<T> action, String actionName) throws RemoteException;

    int lookup(int id) throws RemoteException;

    byte[] getFile(String fileName) throws RemoteException;

    void uploadFile(String fileName, byte[] content) throws RemoteException;

    boolean updateFile(String fileName, byte[] content) throws RemoteException;

    void deleteFile(String fileName) throws RemoteException;

    int getNodeId() throws RemoteException;

    void controlledLeave() throws RemoteException;
    String printFingerTable() throws RemoteException;
    // metodo per aggiornare puntatori
    void notifyLeave(int nodeId, int originId) throws RemoteException, Exception;
}
