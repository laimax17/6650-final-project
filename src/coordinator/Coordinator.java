package coordinator;

import client.CallbackClient;
import common.*;

import server.Server;
import server.ServerInt;
import paxos.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Coordinator extends UnicastRemoteObject implements CoordinatorInt{

    private final static int numsOfReplicas = 9;
    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static Registry registry;

    private List<ServerInt> replicaList;

    private int proposalCnt = 0;

    private Set<CallbackClient> callbackClients = new HashSet<>();

    private Proposer proposer;

    public Coordinator(Proposer proposer,List<ServerInt> replicaList) throws RemoteException {
        super();
        this.proposer = proposer;
        this.replicaList = replicaList;
        for (ServerInt replica : replicaList) {
            replica.setReplicaList(replicaList);
        }
    }

    @Override
    public boolean registerClient(CallbackClient callbackClient) throws RemoteException {
        if (callbackClient == null) return false;
        for (CallbackClient client : this.callbackClients) {
            if (client.getUsername().equals(callbackClient.getUsername())) {
                return false;
            }
        }
        this.callbackClients.add(callbackClient);

        System.out.println(callbackClient + " registered");
        return true;
    }

    @Override
    public List<Message> getHistory() throws RemoteException {
        return proposer.getAll();
    }

    @Override
    public Message sendMessage(Message message) throws RemoteException {
        System.out.println("Received message from " + message.getUserName() + ": " + message.getContent());
        LocalDateTime time = LocalDateTime.now();
        String timeStr = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        message.setTime(timeStr);
        Callable<Status> task = () -> {
            this.proposalCnt = proposer.sendProposal(proposalCnt, message);
            System.out.println("coordinator send message task");
            System.out.println(message);
            syncClients(message);
            return Status.SUCCESS;
        };
        try {
            Future<Status> future = executorService.submit(task);
            future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(e);
            System.out.println(proposer.getId());
        } catch (TimeoutException e) {
            elect();
            System.out.println("Request timed out.");
        }
        return message;
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


    private void elect() {
        Random rand = new Random();
        int num = rand.nextInt(numsOfReplicas);
        while (this.proposer.equals((Proposer) replicaList.get(num))) {
            num = rand.nextInt(numsOfReplicas);
        }
        this.proposer = (Proposer) replicaList.get(num);
        System.out.println("Elected Replica " + num + " as proposer");
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
                    registry.rebind("rmi://" + hostAddress + ":" + portNumber +"/replicaServer"+ i,replicaList.get(i));
                    System.out.println("Replica server "+i+" is created successfully.");
                }catch (RemoteException e) {
                    System.out.println("Failed to rebind service: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            // choose a random proposer
            Random rand = new Random();
            int num = rand.nextInt(numsOfReplicas);
            CoordinatorInt coordinator = new Coordinator((Proposer) replicaList.get(num), replicaList);
            // use hostname
            registry.rebind("rmi://" + hostName + ":" + portNumber +"/coordinator.CoordinatorInt", coordinator);

        }catch (RemoteException e) {
            System.out.println("coordinator.Coordinator error:" + e.getMessage());
            e.printStackTrace();
        }

    }

}
