package server;

import common.*;
import paxos.Acceptor;
import paxos.Learner;
import paxos.Proposer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Server extends UnicastRemoteObject implements Proposer, Learner, Acceptor {

    private int replicaNo;

    private List<ServerInt> replicaList;

    private List<Message> history;

    private Status serverStatus = Status.NORMAL;

    private int proposalInt = 0;

    // acceptor param
    private int maxProposalNumRec = 0;
    private Message maxProposalValueRec;

    public Server(int i) throws RemoteException {
        super();
        this.replicaNo = i;
        history = new ArrayList<>();
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
        return acceptor.handlePrepare(req, this);
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
    public Promise handlePrepare(Prepare req, Proposer proposer) throws RemoteException{
        Random random = new Random();
        if (random.nextInt(10) == 1) {
            System.out.println(String.format("Acceptor: Replica %d failed to respond to prepare message. Retry later.",this.replicaNo));
            return new Promise(false,0);
        }

        // check if the acceptor is behind the proposer and recover
        if (this.history.size() < req.getCurrentPaxosRound()) {
            this.serverStatus = Status.RECOVERING;
            List<Message> messages = proposer.recoverAcceptor(this.history.size(), req.getCurrentPaxosRound());
            this.history.addAll(messages);
            this.serverStatus = Status.NORMAL;
        }

        if (req.getProposalNum() >= this.maxProposalNumRec) {
            int lastK = this.maxProposalNumRec;
            Message lastValue = this.maxProposalValueRec;
            System.out.println(String.format("Acceptor: Replica %d approves the prepare message.",this.replicaNo));
            return new Promise(true, lastK, lastValue);
        }else{
            System.out.println(String.format("Acceptor: Replica %d rejects the prepare message.",this.replicaNo));
            return new Promise(false,0);
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
            return new AcceptResponse(false,0);
        }
        // if the server is recovering, reject the request
        if (this.serverStatus == Status.RECOVERING) return new AcceptResponse(false,0);

        if (req.getN() >= this.maxProposalNumRec) {
            this.maxProposalNumRec = req.getN();
            System.out.println(String.format("Acceptor: Replica %d approves the accept message.",this.replicaNo));
            return new AcceptResponse(true, req.getN(), req.getValue());
        }else {
            System.out.println(String.format("Acceptor: Replica %d rejects the accept message.",this.replicaNo));
            return new AcceptResponse(false,0);
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
        if (this.serverStatus == Status.RECOVERING) return new AcceptResponse(false,0);
        this.maxProposalNumRec = req.getN();

        // save message into history
        saveMessage(req.getValue());
        System.out.println(String.format("Learner: Replica %d learns the result.",this.replicaNo));
        return new AcceptResponse(true, 0, req.getValue());
    }

    @Override
    public int sendProposal(int num, Message message) throws RemoteException {
        Proposer proposer = this;

        // store acceptors and learners
        List<ServerInt> acceptors = new ArrayList<>();

        for (ServerInt replica : replicaList) {
            if (replica != proposer) {
                acceptors.add(replica);
            }
        }
        // update proposer's proposal number
        int proposalNum = num;
        this.proposalInt = proposalNum;
        int currentPaxosRound = this.history.size();
        // phase 1
        // send prepare message obtain promise from acceptors
        System.out.println(String.format("Proposer: sending prepare messages to acceptors."));
        Prepare req = new Prepare(proposalNum, currentPaxosRound);
        List<Promise> promiseList = new ArrayList<>();
        for (ServerInt acceptor : acceptors) {
            Promise p = proposer.sendPrepare(req, (Acceptor) acceptor);
            promiseList.add(p);
        }

        // phase 2
        // check if proposer receive the majority of acceptors success responses
        // promise returns could be [true,x,x] or [false,x,x]
        // if so, send accept requests to acceptors
        int count = 0;
        for (Promise p : promiseList) {
            if (p.getStatus()) {
                count += 1;
                int maxN = p.getMaxN();
                if (maxN > proposalNum) {
                    this.proposalInt = maxN + 1;
                }
            }
        }
        proposalNum = this.proposalInt;
        Accept accReq = new Accept(proposalNum, message);
        if (count >= acceptors.size() / 2 + 1) {
            // counting accepted response from acceptors
            int acceptCnt = 0;
            for (ServerInt acceptor : acceptors) {
                AcceptResponse acceptResponse = proposer.sendAccept(accReq, (Acceptor) acceptor);
                if (acceptResponse.getStatus()) {
                    acceptCnt++;
                }
            }
//            Random random = new Random();
            // if the majority of acceptors agree, then send to learners
            if (acceptCnt >= acceptors.size() / 2 + 1) {
//                int j = random.nextInt(acceptors.size());
//                Acceptor cur = (Acceptor) acceptors.get(j);
                System.out.println(String.format("Proposer: sending learn messages to learners."));
                System.out.println(accReq.getValue());
                for (ServerInt learner : acceptors) {
                    sendLearn(accReq, (Learner) learner);
                }
            } else {
                // if not, resend prepare request
                System.out.println("Failed, restarting...");
                return sendProposal(this.proposalInt+1, message);
            }

        } else {
            // if not, resend prepare request
            System.out.println("Failed, restarting...");
            return sendProposal(this.proposalInt+1, message);
        }
        // save message into history
        saveMessage(message);
        return this.proposalInt;

    }

    @Override
    public List<Message> recoverAcceptor(int acceptorRound, int currentPaxosRound) throws RemoteException {
        return this.history.subList(acceptorRound, currentPaxosRound);
    }

    @Override
    public int getId() {
        return replicaNo;
    }

    @Override
    public List<Message> getAll() {
        return new ArrayList<>(history);
    }

    @Override
    public Message saveMessage(Message message) {
        this.history.add(message);
        return null;
    }

    @Override
    public void setReplicaList(List<ServerInt> replicaList) {
        this.replicaList = replicaList;
    }
}

