package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

import server.ServerInterface;

public class Client {
  Helper client;
  ServerInterface[] stubs;
  Registry[] registries;
  int connectport;
  int id;

  public Client(String[] args, int port, int clientId) throws Exception {
    client = new Helper();
    client.ClientParseArgs(args);
    stubs = new ServerInterface[1];
    registries = new Registry[1];
    connectport = port;
    // for (int i = 0; i < client.serverPorts.length; i++) {
    registries[0] = LocateRegistry.getRegistry("localhost", client.serverPorts[port]);
    stubs[0] = (ServerInterface) registries[0].lookup("compute.ServerInterface");
    // }
    id = clientId;

  }

  public static void ScenarioOne(String[] args) throws Exception {
    System.out.println("Scenario One: ");
    int clientId = 1;
    Client client1 = new Client(args, 0, clientId++);
    Client client2 = new Client(args, 1, clientId++);
    clientOperation(client1, "PUT", "s1", "1");
    clientOperation(client2, "get", "s1", "");
    clientOperation(client1, "PUT", "s1", "2");
    clientOperation(client2, "get", "s1", "");
    clientOperation(client2, "DEL", "s1", "");
    clientOperation(client2, "DEL", "s3", "");

  }

  public static void ScenarioTwo(String[] args) throws Exception {
    System.out.println("Scenario two: ");
    int clientId = 1;
    Client client1 = new Client(args, 0, clientId++);

    clientOperation(client1, "PUT", "s1", "1");
  }

  public static void clientOperation(Client client, String operation, String key, String value) throws IOException {
    if (operation.toUpperCase().equals("PUT")) {
      System.out.println(client.client.getCurrentTimeStamp() + ": " + "Client " + client.id + " Request: " + operation
          + " KEY-" + key + " VALUE-" + value);
      String res = client.stubs[0].twoPC(UUID.randomUUID(), operation.toUpperCase(), key, value);
      System.out.println(
          client.client.getCurrentTimeStamp() + ": " + "Client " + client.id + " Received from Server: " + res);

    } else if (operation.toUpperCase().equals("GET")) {
      System.out.println(client.client.getCurrentTimeStamp() + ": " + "Client " + client.id + " Request: " + operation
          + " KEY-" + key);
      String res = client.stubs[0].twoPC(UUID.randomUUID(), operation.toUpperCase(), key, value);
      System.out.println(
          client.client.getCurrentTimeStamp() + ": " + "Client " + client.id + " Received from Server: " + res);
    } else if (operation.toUpperCase().equals("DEL")) {
      System.out.println(client.client.getCurrentTimeStamp() + ": " + "Client " + client.id + " Request: " + operation
          + " KEY-" + key);
      String res = client.stubs[0].twoPC(UUID.randomUUID(), operation.toUpperCase(), key, value);
      System.out.println(
          client.client.getCurrentTimeStamp() + ": " + "Client " + client.id + " Received from Server: " + res);

    }

  }

  public static void input(String[] args) throws Exception {
    System.out.print("Please enter machine number, function and values:");
    System.out.print("We have 5 machines, you should choose between 0-4" + "\n");
    while (true) {

      System.out.print("If it is a PUT, the input format is: SERVER PUT KEY VALUE" + "\n");
      System.out.print("If it is a GET or DEL, the format is: SERVER GET/DEL KEY" + "\n");
      System.out.print("Example 4 PUT 101 188 or 1 DEL 120" + "\n");
      System.out.print("Type EXIT to quit" + "\n");
      String input = GetStringFromTerminal();

      String[] formattedInput = input.split(" ");
      if (formattedInput.length == 3) {

        int arg1 = Integer.parseInt(formattedInput[0]);
        String arg2 = formattedInput[1];
        String arg3 = formattedInput[2];
        Client client1 = new Client(args, arg1, 0);
        clientOperation(client1, arg2, arg3, "");

      } else if (formattedInput.length == 4) {
        int arg1 = Integer.parseInt(formattedInput[0]);
        String arg2 = formattedInput[1];
        String arg3 = formattedInput[2];
        String arg4 = formattedInput[3];
        Client client2 = new Client(args, arg1, 0);
        clientOperation(client2, arg2, arg3, arg4);
      } else if (formattedInput.length == 1 && formattedInput[0].toUpperCase().equals("EXIT")) {
        System.exit(0);
      } else {
        System.out.println("Invalid input length");
      }
    }
  }

  public static void normalTest(String[] args) throws Exception {

    Client client1 = new Client(args, 0, 0);
    Client client2 = new Client(args, 1, 1);
    Client client3 = new Client(args, 2, 2);
    Client client4 = new Client(args, 3, 3);
    Client client5 = new Client(args, 4, 4);

    // put
    clientOperation(client1, "PUT", "s1", "1");
    clientOperation(client2, "PUT", "s2", "2");
    clientOperation(client3, "PUT", "s3", "3");
    clientOperation(client4, "PUT", "s4", "4");
    clientOperation(client5, "PUT", "s5", "5");
    // get
    clientOperation(client1, "PUT", "s3", "");
    clientOperation(client2, "PUT", "s1", "");
    clientOperation(client3, "PUT", "s4", "");
    clientOperation(client4, "PUT", "s5", "");
    clientOperation(client5, "PUT", "s2", "");

    // delete

    clientOperation(client1, "DEL", "s3", "");
    clientOperation(client2, "DEL", "s1", "");
    clientOperation(client3, "DEL", "s4", "");
    clientOperation(client4, "DEL", "s5", "");
    clientOperation(client5, "DEL", "s2", "");

    // put
    clientOperation(client1, "PUT", "s2", "1");
    clientOperation(client2, "PUT", "s4", "3");
    clientOperation(client3, "PUT", "s1", "4");
    clientOperation(client4, "PUT", "s5", "5");
    clientOperation(client5, "PUT", "s3", "2");

  }

  public static void main(String[] args) throws Exception {

    try {
      // ScenarioOne(args);
      // ScenarioTwo(args);
      normalTest(args);
      input(args);

    } catch (Exception e) {

      e.getStackTrace();
    }
  }

  public static String GetStringFromTerminal() throws IOException {
    BufferedReader stringIn = new BufferedReader(new InputStreamReader(System.in));
    return stringIn.readLine();
  }
}
