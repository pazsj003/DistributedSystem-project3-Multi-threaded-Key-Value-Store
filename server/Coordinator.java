package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Coordinator extends Thread {

  static Helper hl = new Helper();
  static Server[] servers = new Server[5];

  public static void main(String args[]) throws Exception {
    hl.ServerParseArgs(args);
    Registry registry = null;
    Thread[] theadList = new Thread[5];
    for (int i = 0; i < hl.servers.length; i++) {
      try {
        servers[i] = new Server();

        servers[i].start();
        ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(servers[i], hl.servers[i]);
        registry = LocateRegistry.createRegistry(hl.servers[i]);

        registry.bind("compute.ServerInterface", stub);

        giveNewServerInfo(hl.servers, hl.servers[i]);
        hl.log(String.format("Server %s is running at port %s",
            new String[] { Integer.toString(i), Integer.toString(hl.servers[i]) }));

      } catch (Exception e) {
        System.err.println("Server exception: " + e.toString());
      }

    }

    servers[1].stopRunning();

  }

  private static void giveNewServerInfo(int[] servers, int port) {
    try {
      Registry registry = LocateRegistry.getRegistry(port);

      ServerInterface stub = (ServerInterface) registry.lookup("compute.ServerInterface");

      int j = 0;
      int[] other = new int[servers.length - 1];
      for (int i = 0; i < servers.length; i++) {
        if (servers[i] != port) {
          other[j] = servers[i];
          j++;
        }
      }

      stub.setServersInfo(other, port);
    } catch (Exception ex) {
      hl.log("Cannot connect to server!" + port);
      hl.log(ex.getMessage());
    }
  }
}
