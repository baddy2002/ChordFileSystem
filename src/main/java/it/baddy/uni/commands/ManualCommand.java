package it.baddy.uni.commands;

import it.baddy.uni.rmi.interfaces.ChordRemoteInterface;
import it.baddy.uni.chord.ChordNode;
import it.baddy.uni.utils.FileUtils;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ManualCommand {

    private static final int PORT = 1099;
    private static final String allNodesEnv = System.getenv().get("ALL_NODES");

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        List<Integer> allNodes = Arrays.stream(allNodesEnv.split(","))
                .map(name -> name.replace("node", "")) // X
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toList());
        System.out.println("=== CHORD CLI ===");

        while (true) {

            System.out.print("\nEnter node hostname (es. node3) or 'exit': ");
            String host = scanner.nextLine();

            if (host.equalsIgnoreCase("exit"))
                break;

            ChordRemoteInterface rmiNode =
                    (ChordRemoteInterface) Naming.lookup("//" + host + ":" + PORT + "/ChordNode");

            ChordNode chordNode = new ChordNode(rmiNode.getNodeId(), allNodes );
            System.out.println("\nChoose operation:");
            System.out.println("1 - Search file");
            System.out.println("2 - Insert file");
            System.out.println("3 - Update file");
            System.out.println("4 - Delete file");
            System.out.println("5 - Print finger table");
            System.out.println("6 - Controlled leave");
            System.out.println("0 - Back");

            String choice = scanner.nextLine();

            switch (choice) {

                case "1":
                    handleDownload(scanner, rmiNode, chordNode);
                    break;

                case "2":
                    handleUpload(scanner, rmiNode, true);
                    break;
                case "3":
                    handleUpload(scanner, rmiNode, false);
                    break;
                case "4":
                    handleDelete(scanner, rmiNode, chordNode);
                    break;

                case "5":
                    handlePrint(rmiNode);
                    break;

                case "6":
                    handleLeave(rmiNode);
                    break;

                case "0":
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }

        scanner.close();
    }

    public static void handleDownload(Scanner scanner, ChordRemoteInterface rmiNode, ChordNode node){
        System.out.print("File name: ");
        String fileName = scanner.nextLine();
        try {
            byte[] content = rmiNode.getFile(fileName);
            Path output = Path.of(fileName);
            FileUtils.writeFile(output, content);
            System.out.println("File saved in " + output.toAbsolutePath());

        } catch (Exception e) {
            //se è remote di file not found non esiste
            if (node.isFileNotFoundException(e)) {

                System.err.println("File not found in distributed system.");

            } else {
                System.err.println("Generic error: " + e.getMessage());
            }
        }
    }

    public static void handleUpload(Scanner scanner, ChordRemoteInterface rmiNode, boolean to_create){
        try {
            if (to_create)
                System.out.print("Local file path to upload: ");
            else
                System.out.print("Local file path to update: ");
            String localPath = scanner.nextLine();
            Path path = Path.of(localPath);
            byte[] fileBytes = Files.readAllBytes(path);
            if (to_create) {
                rmiNode.uploadFile(path.getFileName().toString(), fileBytes);
                System.out.println("File inserted in distributed system.");
            } else {
                boolean created = rmiNode.updateFile(path.getFileName().toString(), fileBytes);
                if (created)
                    System.out.println("File created because it was not present on distributed system.");
                else
                    System.out.println("File updated in distributed system.");
            }
        } catch (Exception e) {
            System.err.println("Error uploading file: " + e.getMessage());
        }
    }

    public static void handleDelete(Scanner scanner, ChordRemoteInterface rmiNode, ChordNode node){
        try{
            System.out.print("Local file path to delete: ");
            String deleteLocalPath = scanner.nextLine();
            Path deletePath = Path.of(deleteLocalPath);
            rmiNode.deleteFile(deletePath.getFileName().toString());
            System.out.println("File deleted from distributed system.");
        } catch (Exception e) {
            //se è remote di file not found non esiste
            if (node.isFileNotFoundException(e)) {

                System.err.println("File not found in distributed system.");

            } else {
                System.err.println("Generic error: " + e.getMessage());
            }
        }
    }

    public static void handlePrint(ChordRemoteInterface rmiNode) throws RemoteException{
        System.out.println(rmiNode.printFingerTable());
    }

    public static void handleLeave(ChordRemoteInterface rmiNode) throws RemoteException{
        rmiNode.controlledLeave();
        System.out.println("Node leave triggered.");
    }

}
