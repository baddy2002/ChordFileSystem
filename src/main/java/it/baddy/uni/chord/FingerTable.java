package it.baddy.uni.chord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FingerTable {

    private final int nodeId;
    private final int m;
    private final int ringSize=40;
    private final int[] fingers;

    //crea la finger table per m=3 si ha max ringsize=8
    //0: nodeId+2^0 mod 8= 1
    //1: nodeId+2^1 mod 8= 2
    //2: nodeId+2^2 mod 8= 4
    //0: nodeId+2^0 mod 8= 0
    //1: nodeId+2^1 mod 8= 1
    //2: nodeId+2^2 mod 8= 3
    public FingerTable(int nodeId, List<Integer> allNodeIds) {
        this.nodeId = nodeId;
        //numero di bit necessari
        this.m = (int) Math.ceil(Math.log(ringSize) / Math.log(2));
        System.out.println("m is equal to:" + m);
        this.fingers = new int[m];

        for (int i = 0; i < m; i++) {
            fingers[i] = successor((nodeId + (1 << i)) % ringSize, allNodeIds);
        }
    }

    //dato il valore di un entry della finger trova il primo nodo con id maggiore nel sistema
    private int successor(int id, List<Integer> nodes) {
        return nodes.stream()
                .filter(n -> n >= id)
                .findFirst()
                .orElse(nodes.getFirst());
    }

    public int getSuccessor() {
        return fingers[0];
    }

    public int[] getFingers() {
        return fingers;
    }


    public int getNodeId() {
        return nodeId;
    }

    public int getM() {
        return m;
    }

    public int getRingSize() {
        return ringSize;
    }

    public void print() {
        System.out.println("Finger table for node " + nodeId +" with m: " + m);
        System.out.println("-------------------------------------------");

        for (int i = 0; i < m; i++) {
            System.out.println(
                    "Entry " + i +
                            " | related node = " + fingers[i]
            );
        }

        System.out.println();
    }

    public String printRemote() {
        StringBuilder sb = new StringBuilder();

        sb.append("Finger table for node ")
                .append(nodeId)
                .append(" with m: ")
                .append(m)
                .append("\n");

        sb.append("-------------------------------------------\n");

        for (int i = 0; i < m; i++) {
            sb.append("Entry ")
                    .append(i)
                    .append(" | related node = ")
                    .append(fingers[i])
                    .append("\n");
        }

        sb.append("\n");

        return sb.toString();
    }

    //aggiorna il successor della finger table se necessario
    public void updateFingersChecked(int nodeId, List<Integer> allNodeIds) {
        System.out.println("allNodeIds is: " + allNodeIds);
        int[] indexes = getEntries(nodeId);
        for (int index : indexes) {
            //ricalcola l'entry della finger table
            fingers[index] = successor((this.nodeId +  (1 << index)) % ringSize, allNodeIds);
            System.out.println("Updated finger table entry " + index + " to " + fingers[index]);
        }

    }

    //ritorna gli indici delle entry della finger table che devono essere aggiornate
    private int[] getEntries(int nodeId) {
        //init a -1
        int[] temp = new int[m];
        Arrays.fill(temp, -1);
        int count = 0;
        //trova gli indici delle entry che devono essere aggiornate
        for (int i = 0; i < m; i++) {
            if (nodeId == this.fingers[i]) {
                temp[count++] = i;
            }
        }
        //ritorna solo quelli da aggiornare
        return Arrays.copyOf(temp, count);
    }
}
