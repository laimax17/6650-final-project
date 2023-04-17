package coordinator;

import common.Accept;
import common.AcceptResponse;
import common.Prepare;
import common.Promise;
import paxos.Acceptor;
import paxos.Learner;
import paxos.Proposer;
import server.Server;
import server.ServerInt;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

    private static List<ServerInt> replicaList = new ArrayList<>(Collections.nCopies(numsOfReplicas, null));

    private int proposalCnt = 0;

    public Coordinator() throws RemoteException {
        super();
    }



    public void sendProposal(String userRequest) throws RemoteException {
        // pick a proposer first
        Random random = new Random();
        int i = random.nextInt(replicaList.size());
        Proposer proposer = (Proposer) replicaList.get(i);
        System.out.println(String.format("Picking replica %d as proposer",i));

        // store acceptors and learners
        List<ServerInt> acceptors = new ArrayList<>();
//        List<Learner> learners = new ArrayList<>();
        for (ServerInt replica : replicaList) {
            if (replica != proposer) {
                acceptors.add(replica);

            }
        }
        // create a proposal number
        int proposalNum = proposalCnt + 1;
        proposalCnt++;
        // phase 1
        // send prepare message obtain promise from acceptors
        System.out.println(String.format("Proposer: replica %d sending prepare messages to acceptors.",i));
        Prepare req = new Prepare(proposalNum);
        List<Promise> promiseList = new ArrayList<>();
        for (ServerInt acceptor : acceptors) {
            Promise p = proposer.sendPrepare(req, (Acceptor) acceptor);
            promiseList.add(p);
        }

        // phase 2
        // check if proposer receive the majority of acceptors success responses
        // promise returns could be (success)[Accepted N,Accepted V] or (fail)[error,error]
        // if so, send accept requests to acceptors
        int count = 0;
        int maxAcceptedProposalNum = proposalNum;
        String newUserRequest = userRequest;
        for (Promise p: promiseList) {
            if (p.getStatus()) {
                count += 1;
                int maxN = p.getMaxN();
                if (maxN > maxAcceptedProposalNum) {
                    maxAcceptedProposalNum = maxN;
                    newUserRequest = p.getV();
                }
            }
        }
        proposalCnt = maxAcceptedProposalNum+1;
        Accept accReq = new Accept(maxAcceptedProposalNum, newUserRequest);
        if (count >= acceptors.size()/2 + 1){
            // counting accepted response from acceptors
            int acceptCnt = 0;
            for (ServerInt acceptor : acceptors) {
                AcceptResponse acceptResponse = proposer.sendAccept(accReq, (Acceptor) acceptor);
                if (acceptResponse.getStatus()) {
                    acceptCnt++;
                }
            }

            // if the majority of acceptors agree, then send to learners
            if (acceptCnt >= acceptors.size()/2 + 1) {
                int j = random.nextInt(acceptors.size());
                Acceptor cur = (Acceptor) acceptors.get(j);
                System.out.println(String.format("Acceptor: replica %d sending learn messages to learners.",j));
                for (ServerInt learner: acceptors) {
                    cur.sendLearn(accReq, (Learner) learner);
                }
            }else{
                // if not, resend prepare request
                System.out.println("Failed, restarting...");

            }

        }else {
            // if not, resend prepare request
            System.out.println("Failed, restarting...");

        }



    }

    public static void main(String[] args) throws RemoteException,UnknownHostException {
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
            Coordinator obj = new Coordinator();
            registry.rebind("rmi://" + hostName + ":" + portNumber +"/coordinator.CoordinatorInt",obj);

        }catch (RemoteException  e) {
            System.out.println("coordinator.Coordinator error:" + e.getMessage());
            e.printStackTrace();
        }

        // create replica servers and bind to rmi registry
        System.out.println("Now creating replica servers.");
        for (int i = 0; i < numsOfReplicas; i+=1) {
            try{
                replicaList.set(i,new Server(i));
                registry.rebind("rmi://" + hostName + ":" + portNumber +"/replicaServer"+ i,replicaList.get(i));
                System.out.println("Replica server "+i+" is created successfully.");
            }catch (RemoteException e) {
                System.out.println("Failed to rebind service: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
