package paxos;

import common.*;
import server.ServerInt;

import java.rmi.RemoteException;

public interface Proposer extends ServerInt {


    Promise sendPrepare(Prepare req, Acceptor acceptor) throws RemoteException;

    AcceptResponse sendAccept(Accept req, Acceptor acceptor) throws RemoteException;

    int sendProposal(int proposalNum, Message message) throws RemoteException;


}
