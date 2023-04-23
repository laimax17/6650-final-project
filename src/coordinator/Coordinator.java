package coordinator;

import client.CallbackClient;
import common.*;
import server.Server;
import server.ServerInt;
import paxos.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Semaphore;


public class Coordinator extends UnicastRemoteObject implements CoordinatorInt{

    private final static int numsOfReplicas = 9;
    private final Semaphore lock = new Semaphore(1);
    private static Registry registry;

    private List<ServerInt> replicaList;

    private int proposalCnt = 0;

    private Set<CallbackClient> callbackClients = new HashSet<>();

    private Proposer proposer;

    public Coordinator(Proposer proposer,List<ServerInt> replicaList) throws RemoteException {
        super();
        this.proposer = proposer;
        this.replicaList = replicaList;
    }

    private void setupReplicaList() {
        this.replicaList = new ArrayList<>(Collections.nCopies(numsOfReplicas, null));
    }

    @Override
    public boolean registerClient(CallbackClient callbackClient) throws RemoteException {
        if (callbackClient == null) return false;
        this.callbackClients.add(callbackClient);
        System.out.println(callbackClient + " registered");
        return true;
    }

    @Override
    public List<Message> getHistory() throws RemoteException {
        return proposer.getAll();
    }

    @Override
    public List<Message> getLatest() throws RemoteException {
        return proposer.getUpdate();
    }

    @Override
    public Message sendMessage(Message message) throws RemoteException {
        try {

            System.out.println("Received message from " + message.getUserName() + ": " + message.getContent());
            LocalDateTime time = LocalDateTime.now();
            String timeStr = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            message.setTime(timeStr);
            Message returnMessage = null;
            try {
                // TODO: save message
//                returnMessage = proposer.saveMessage(message);
                // TODO: should use proposer.sendProposal(proposalCnt,msg) ?
                //returnMessage = proposer.saveMessage(msg);
                // create a new propoasl
                this.proposalCnt = proposer.sendProposal(proposalCnt, message);
            } catch (Exception e) {
                // TODO: add timeout and elect another proposer
                elect();
                // sendMessage(username, message)
                e.printStackTrace();
            }
            // TODO: sync returnMessage after saveMessage is implemented
            syncClients(message);
            return message;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private void syncClients(Message message) {
        HashSet<CallbackClient> toRemove = new HashSet<>();
        for (CallbackClient destClient : this.callbackClients) {
            try {
                if (!message.getUserName().equals(destClient.getUsername())) {
                    System.out.println("Sending message to " + destClient.getUsername() + " ...");
                    destClient.showNewMessage(message);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("Removing " + destClient + " from callback list");
                toRemove.add(destClient);
            }
        }
        this.callbackClients.removeAll(toRemove);
    }

    private List<ServerInt> getReplicaList() {
        return this.replicaList;
    }

    // TODO: elect algorithm
    private void elect() {
        Random rand = new Random();
        int num = rand.nextInt(numsOfReplicas);
        while (this.proposer.equals((Proposer) replicaList.get(num))) {
            num = rand.nextInt(numsOfReplicas);
        }
        this.proposer = (Proposer) replicaList.get(num);
    }

    public static void main(String[] args) throws RemoteException, UnknownHostException {
        System.out.println("coordinator started");
        InetAddress host = InetAddress.getLocalHost();
        String hostAddress = host.getHostAddress();
        String hostName = host.getHostName();
        System.setProperty("java.rmi.server.hostname", hostName);
        Integer portNumber = Integer.parseInt(args[0]);
        System.out.println("The host address is: " + hostAddress);
        System.out.println("THe host name is: " + hostName);
        System.out.println("The port number is: " + portNumber );
        // set timeout in 5s
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "5000");
        try {
            registry = LocateRegistry.createRegistry(portNumber);
            System.out.println("java RMI registry created. Port number is: " + portNumber);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry();
            System.out.println("java RMI registry already exists.");
        }

        try {
            // create replica servers and bind to rmi registry
            System.out.println("Now creating replica servers.");
            List<ServerInt> replicaList = new ArrayList<>();

            for (int i = 0; i < numsOfReplicas; i+=1) {
                try{
                    replicaList.add(i,new Server(i));
                    registry.rebind("rmi://" + hostName + ":" + portNumber +"/replicaServer"+ i,replicaList.get(i));
                    System.out.println("Replica server "+i+" is created successfully.");
                }catch (RemoteException e) {
                    System.out.println("Failed to rebind service: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            Random rand = new Random();
            int num = rand.nextInt(numsOfReplicas);
            CoordinatorInt coordinator = new Coordinator((Proposer) replicaList.get(num), replicaList);
            registry.rebind("rmi://" + hostName + ":" + portNumber +"/coordinator.CoordinatorInt", coordinator);


        }catch (RemoteException e) {
            System.out.println("coordinator.Coordinator error:" + e.getMessage());
            e.printStackTrace();
        }

    }

}
