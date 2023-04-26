package client;

import common.Message;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Scanner;

import coordinator.CoordinatorInt;

public class Client extends UnicastRemoteObject implements CallbackClient, Serializable {
    String username;

    protected Client(String username) throws RemoteException {
        super();
        this.username = username;
    }

    @Override
    public void showNewMessage(Message message) {
        System.out.println(message);
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public void kickOut() throws RemoteException {
        System.out.println(new Message("System", "You have logged in on another device, you will be logged out in this device."));
        System.exit(1);
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
            System.out.println("finding registry with :"+ host +" "+ port);
            Registry registry = LocateRegistry.getRegistry(host, port);
            CoordinatorInt coordinator = (CoordinatorInt) registry.lookup("rmi://" + host + ":" + port + "/coordinator.CoordinatorInt");
            // register client to coordinator
            Client client = new Client(username);
            coordinator.registerClient(client);
            // Read commands from commands.txt and process them
            Scanner scanner = new Scanner(System.in);
            String input;
            System.out.println("History Chat:\n");
            List<Message> history = coordinator.getHistory();
            if (history != null) {
                for (Message msg : history) {
                    System.out.println(msg);
                }
            }
            while (true) {
                input = scanner.nextLine();
                // send message
                Message message = new Message(username, input);
                Message returnValue = coordinator.sendMessage(message);
                System.out.print("\033[F\r" + returnValue + "\n");

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
