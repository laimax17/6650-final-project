package coordinator;

import client.CallbackClient;
import common.*;
import paxos.Acceptor;
import paxos.Learner;
import paxos.Proposer;
import server.Server;
import server.ServerInt;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;


public class Coordinator extends UnicastRemoteObject implements CoordinatorInt{

    private final static int numsOfReplicas = 9;
    private final Semaphore lock = new Semaphore(1);
    private static Registry registry;

    private List<ServerInt> replicaList;

    private int proposalCnt = 0;

    private List<CallbackClient> callbackClients = new ArrayList<>();

    private ServerInt proposer;

    public Coordinator(ServerInt proposer,List<ServerInt> replicaList) throws RemoteException {
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
    public Message sendMessage(String username, String message) {
        long currentTimestamp = System.currentTimeMillis();
        Message msg = new Message(String.valueOf(currentTimestamp), username, message);
        Message returnMessage = null;
        try {
            returnMessage = proposer.saveMessage(msg);
        } catch (Exception e) {
            // TODO: add timeout and elect another proposer
            elect();
            // sendMessage(username, message)
            e.printStackTrace();
        }
        return returnMessage;
    }

    // TODO use this method to sync all clients when a client sends a new message
    private void syncClients(Message message) {
        for (CallbackClient callbackClient : this.callbackClients) {
            callbackClient.showNewMessage(message);
        }
    }

    private List<ServerInt> getReplicaList() {
        return this.replicaList;
    }

    // TODO: how to elect, do we need to modify hashcode of servers?
    private static void elect() {

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
                    replicaList.add(new Server(i));
                    Naming.rebind("rmi://" + hostName + ":" + portNumber +"/replicaServer"+ i,replicaList.get(i));
                    System.out.println("Replica server " + i + " is created successfully.");
                }catch (RemoteException e) {
                    System.out.println("Failed to rebind service: " + e.getMessage());
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            Random rand = new Random();
            int num = rand.nextInt(numsOfReplicas);
            Coordinator coordinator = new Coordinator(replicaList.get(num), replicaList);
            Naming.rebind("rmi://" + hostName + ":" + portNumber +"/coordinator.CoordinatorInt", coordinator);


        }catch (RemoteException | MalformedURLException e) {
            System.out.println("coordinator.Coordinator error:" + e.getMessage());
            e.printStackTrace();
        }

    }

}
