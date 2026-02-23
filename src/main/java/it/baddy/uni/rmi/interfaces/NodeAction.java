package it.baddy.uni.rmi.interfaces;

import it.baddy.uni.chord.ChordNode;

import java.io.Serializable;
import java.rmi.RemoteException;

@FunctionalInterface
public interface NodeAction<T> extends Serializable { //rmi può inviare solo oggetti serializzabili
    T execute(ChordNode node) throws RemoteException;
}

