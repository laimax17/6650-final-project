package server;

import common.Accept;
import common.AcceptResponse;
import common.Prepare;
import common.Promise;
import paxos.Acceptor;
import paxos.Learner;
import paxos.Proposer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends UnicastRemoteObject implements ServerInt, Proposer, Learner, Acceptor {

    private int replicaNo;

    // proposer param
    private int proposalNum = 0;

    // acceptor param
    private int maxProposalNumRec = 0;
    private String maxProposalValueRec = "";

    public Server(int i) throws RemoteException {
        super();
        this.replicaNo = i;
    }


    /**
     * proposer sends prepare message to acceptor
     * @param req
     * @param acceptor
     * @return
     * @throws RemoteException
     */
    @Override
    public Promise sendPrepare(Prepare req, Acceptor acceptor) throws RemoteException {
        return acceptor.handlePrepare(req);
    }

    /**
     * proposer sends accept message to acceptor
     * @param req
     * @param acceptor
     * @return
     * @throws RemoteException
     */
    @Override
    public AcceptResponse sendAccept(Accept req, Acceptor acceptor) throws RemoteException {
        return acceptor.handleAccept(req);
    }

    /**
     * acceptor sends learn message to learner
     * @param req
     * @param learner
     * @return
     * @throws RemoteException
     */
    @Override
    public AcceptResponse sendLearn(Accept req, Learner learner) throws RemoteException {
        return learner.handleLearn(req);
    }


    /**
     * acceptor handles prepare message
     * @param req
     * @return
     * @throws RemoteException
     */
    @Override
    public Promise handlePrepare(Prepare req) throws RemoteException{
        Random random = new Random();
        if (random.nextInt(10) == 1) {
            System.out.println(String.format("Acceptor: Replica %d failed to respond to prepare message. Retry later.",this.replicaNo));
            return new Promise(false,0,"");
        }

        if (req.getProposalNum() >= this.maxProposalNumRec) {
            int lastK = this.maxProposalNumRec;
            String lastValue = this.maxProposalValueRec;
            System.out.println(String.format("Acceptor: Replica %d approves the prepare message.",this.replicaNo));
            return new Promise(true, lastK, lastValue);
        }else{
            System.out.println(String.format("Acceptor: Replica %d rejects the prepare message.",this.replicaNo));
            return new Promise(false,0,"");
        }
    }

    /**
     * acceptor handles accept message
     * @param req
     * @return
     * @throws RemoteException
     */
    @Override
    public AcceptResponse handleAccept(Accept req) throws RemoteException {
        Random random = new Random();
        if (random.nextInt(10) == 1) {
            System.out.println(String.format("Acceptor: Replica %s failed to respond to accept message. Retry later.",this.replicaNo));
            return new AcceptResponse(false,0,"");
        }

        if (req.getN() >= this.maxProposalNumRec) {
            this.maxProposalNumRec = req.getN();
            this.maxProposalValueRec = req.getValue();

            System.out.println(String.format("Acceptor: Replica %d approves the accept message.",this.replicaNo));
            return new AcceptResponse(true, req.getN(), req.getValue());
        }else {
            System.out.println(String.format("Acceptor: Replica %d rejects the prepare message.",this.replicaNo));
            return new AcceptResponse(false,0,"");
        }
    }

    /**
     * learner handles learn message
     * @param req
     * @return
     * @throws RemoteException
     */
    @Override
    public AcceptResponse handleLearn(Accept req) throws RemoteException {
        // learner always success
        if (this.maxProposalNumRec == req.getN()) {
            return new AcceptResponse(true, 0,"");
        }
        this.maxProposalNumRec = req.getN();
        this.maxProposalValueRec = req.getValue();

        System.out.println(String.format("Learner: Replica %d learns the result.",this.replicaNo));
        return new AcceptResponse(true, 0,"");
    }
}


    public void sendProposal(String userRequest) throws RemoteException {
        // pick a proposer first
        Random random = new Random();
        int i = random.nextInt(replicaList.size());
        Proposer proposer = (Proposer) replicaList.get(i);
        System.out.println(String.format("Picking replica %d as proposer",i));

        // store acceptors and learners
        List<ServerInt> acceptors = new ArrayList<>();

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