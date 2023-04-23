package client;

import common.Message;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Scanner;

import coordinator.Coordinator;
import coordinator.CoordinatorInt;

public class Client extends UnicastRemoteObject implements CallbackClient {

    public Client() throws RemoteException {
        super();
    }

    @Override
    public void showNewMessage(Message message) {
        System.out.println("New message from server: " + message);
    }

    public static void main(String[] args) {
        int len = args.length;
        if (len != 3) {
          System.out.println("invalid number of arguments");
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String username = args[2];

        try {
            System.out.println("finding registry with :"+host+" "+ port);
            Registry registry = LocateRegistry.getRegistry(host, port);
            CoordinatorInt coordinator = (CoordinatorInt) registry.lookup("rmi://" + host + ":" + port + "/coordinator.CoordinatorInt");
            // register client to coordinator
            coordinator.registerClient(new Client());
            // Read commands from commands.txt and process them
            Scanner scanner = new Scanner(System.in);
            String input;
            System.out.println("History Chat:\n");
            List<Message> history = coordinator.getHistory();
            for (Message msg : history) {
                System.out.println(msg);
            }
            System.out.println("Let's chat");
            while (true) {
                input = scanner.nextLine();

                // send message
                Message returnValue = coordinator.sendMessage(input);
                System.out.println(returnValue);

                // check for exit condition
                if (input.equals("exit")) {
                    break;
                }
            }

            scanner.close();

        } catch (NotBoundException | IOException e) {
            e.printStackTrace();
        }
    }
}
