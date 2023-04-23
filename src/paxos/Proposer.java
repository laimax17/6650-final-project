package paxos;

import common.*;
import server.ServerInt;

import java.rmi.RemoteException;
import java.util.List;

public interface Proposer extends ServerInt {


    Promise sendPrepare(Prepare req, Acceptor acceptor) throws RemoteException;

    AcceptResponse sendAccept(Accept req, Acceptor acceptor) throws RemoteException;

    int sendProposal(int num, Message message) throws RemoteException;


}
