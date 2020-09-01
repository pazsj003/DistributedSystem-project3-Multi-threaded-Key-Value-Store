package server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Server extends Thread implements ServerInterface {
  static Helper serverHelp = new Helper();
  private int[] otherServers = new int[4];
  private int myPort;
  private Map<UUID, Value> kvCache = Collections.synchronizedMap(new HashMap<UUID, Value>());
  private Map<UUID, Map<Integer, Ack>> onePcAcksStore = Collections
      .synchronizedMap(new HashMap<UUID, Map<Integer, Ack>>());
  private Map<UUID, Map<Integer, Ack>> twoPcAcksStore = Collections
      .synchronizedMap(new HashMap<UUID, Map<Integer, Ack>>());
  LockOperation rwl = new LockOperation();
  private boolean stopRunning = false;

  public void stopRunning() {
    this.stopRunning = true;
    serverHelp.log("Server Stopped Running at port " + myPort);
  }

  public String KvStoreCURD(String functionality, String key, String value) {
    String message = "";
    String fileName = "keyValueStore_" + myPort + ".txt";
    KvStore k1 = new KvStore(fileName);
    try {
      if (functionality.equalsIgnoreCase("GET")) {
        // serverHelp.log("Looking for key: " + key + " - from client: ");
        rwl.lockRead();
        String res = k1.isInStore(key);
        String status = res != null ? "SUCESS " : "FAIL";
        message += "GET -> key " + key + " value:" + res + " Status: " + status;
        serverHelp
            .log("Server at port " + myPort + " Process: " + "GET  key:" + key + " value:" + res + " Status:" + status);
        rwl.unlockRead();
      } else if (functionality.equalsIgnoreCase("PUT")) {

        rwl.lockWrite();
        boolean res = k1.putInStore(key, value);
        String status = res ? "SUCESS " : "FAIL";
        message += "PUT -> key " + key + " value:" + value + " status : " + status;
        serverHelp.log(
            "Writing the key: " + key + " and value: " + value + " at server port: " + myPort + " status:" + status);
        rwl.unlockWrite();
      } else {
        // serverHelp.log("Deleting the key: " + key + " at server port: " + myPort);
        rwl.lockWrite();
        String res = k1.deleteKeyValue(key);
        String status = !res.equals("") ? "SUCESS " : "FAIL";
        message += "DELETE -> key " + key + " status : " + status;
        // serverHelp.log("Server at port " + myPort + " Process: " + "DEL key:" + key +
        // " Status:" + status);
        serverHelp.log("Deleting the key: " + key + " at server port: " + myPort + " status:" + status);
        rwl.unlockWrite();
      }
    } catch (Exception e) {
      serverHelp.log(e.getMessage());
    }
    return (message);
  }

  /*
   * 2pc CURD operation
   * 
   */
  public String twoPC(UUID messageId, String functionality, String key, String value) throws RemoteException {
    if (functionality.equalsIgnoreCase("GET")) {
      serverHelp.log("Server at port " + myPort + " Received Request: " + functionality + " Key:" + key);
      return KvStoreCURD(functionality, key, value);
    }
    serverHelp
        .log("Server at port " + myPort + " Received Request: " + functionality + " Key:" + key + " Value:" + value);
    addToTempStorage(messageId, functionality, key, value);
    serverHelp.log("Server at port " + myPort + " Send Request to Node Server Ack");
    tellToPrepare(messageId, functionality, key, value);
    boolean prepareSucc = waitAck_1pc(messageId, functionality, key, value);
    if (!prepareSucc) {
      return functionality + " FAIL ";
    }

    serverHelp.log("Server at port " + myPort + " Send Request to Node Server Commit");
    tellToGo(messageId);

    boolean goSucc = waitToAckGo(messageId);
    if (!goSucc) {
      return functionality + " FAIL ";
    }

    Value v = this.kvCache.get(messageId);

    if (v == null) {
      throw new IllegalArgumentException("The message is not in the storage");
    }
    // System.out.println("before put + key " + v.key + " value " + v.value);
    String message = this.KvStoreCURD(v.function, v.key, v.value);
    this.kvCache.remove(messageId);
    serverHelp.log("Server at port " + myPort + " Process: " + message);
    return message;
  }

  private boolean waitToAckGo(UUID messageId) {

    int areAllAck = 0;
    int retry = 0;

    while (retry != 3) {
      try {
        Thread.sleep(100);
      } catch (Exception ex) {
        serverHelp.log("wait fail.");
      }

      areAllAck = 0;
      retry++;
      Map<Integer, Ack> map = this.twoPcAcksStore.get(messageId);
      serverHelp.log("Wait Ack To Commit Try " + retry);
      for (int server : this.otherServers) {
        if (map.get(server).isAcked) {
          areAllAck++;
        } else {
          callCommitRequest(messageId, server);
        }
      }
      if (areAllAck == 4) {
        serverHelp.log("Wait Ack To Commit Scuess");
        return true;
      }
    }
    serverHelp.log("Wait Ack To Commit Fail, Abort operation");
    return false;
  }

  private boolean waitAck_1pc(UUID messageId, String functionality, String key, String value) {

    int areAllAck = 0;
    int retry = 0;

    while (retry != 3) {
      try {
        Thread.sleep(100);
      } catch (Exception ex) {
        serverHelp.log("wait fail.");
      }
      areAllAck = 0;
      retry++;
      serverHelp.log("Wait Ack Try " + retry);
      Map<Integer, Ack> map = this.onePcAcksStore.get(messageId);
      for (int server : this.otherServers) {
        if (map.get(server).isAcked) {
          areAllAck++;
        } else {
          callAckRequest(messageId, functionality, key, value, server);
        }
      }

      if (areAllAck == 4) {
        serverHelp.log("Wait Ack Scuess");
        return true;
      }
    }
    serverHelp.log("Wait Ack Fail, Abort operation");
    return false;
  }

  private void tellToPrepare(UUID messageId, String functionality, String key, String value) {

    this.onePcAcksStore.put(messageId, Collections.synchronizedMap(new HashMap<Integer, Ack>()));

    for (int server : this.otherServers) {
      callAckRequest(messageId, functionality, key, value, server);
    }

  }

  private void tellToGo(UUID mesUuid) {
    this.twoPcAcksStore.put(mesUuid, Collections.synchronizedMap(new HashMap<Integer, Ack>()));

    for (int server : this.otherServers) {
      callCommitRequest(mesUuid, server);
    }
  }

  private void callCommitRequest(UUID messageId, int server) {
    try {
      Ack a = new Ack();
      a.isAcked = false;
      this.twoPcAcksStore.get(messageId).put(server, a);
      Registry registry = LocateRegistry.getRegistry(server);
      ServerInterface stub = (ServerInterface) registry.lookup("compute.ServerInterface");
      stub.go(messageId, myPort);
    } catch (Exception ex) {
      serverHelp.log("Something went wrong in sending go, removing data from temporary storage");
    }

    serverHelp.log("call commit for worked. server port: " + server);
  }

  private void callAckRequest(UUID messageId, String functionality, String key, String value, int server) {
    try {
      Ack a = new Ack();
      a.isAcked = false;
      this.onePcAcksStore.get(messageId).put(server, a);
      Registry registry = LocateRegistry.getRegistry(server);
      ServerInterface stub = (ServerInterface) registry.lookup("compute.ServerInterface");
      // System.out.println("callAckRequest " + myPort);
      stub.prepareKeyValue(messageId, functionality, key, value, myPort);
    } catch (Exception ex) {
      serverHelp.log("Something went wrong in sending Ack, removing data from temporary storage");
    }

    serverHelp.log("call request for worked. server port: " + server);
  }

  public void ackMe(UUID messageId, int yourPort, AckType type, boolean stopRunning) throws RemoteException {
    // System.out.println("ackMe" + yourPort);
    // System.out.println("port " + yourPort + "stoprunning status" + stopRunning);
    if (!stopRunning) {
      if (type == AckType.ackGo) {
        this.twoPcAcksStore.get(messageId).get(yourPort).isAcked = true;
      } else if (type == AckType.AkcPrepare) {
        this.onePcAcksStore.get(messageId).get(yourPort).isAcked = true;
      }
      serverHelp.log("Ack received from: " + yourPort);
      return;
    }
    serverHelp.log("Ack received from: " + yourPort);
  }

  public void go(UUID messageId, int callBackServer) throws RemoteException {

    Value v = this.kvCache.get(messageId);

    if (v == null) {
      throw new IllegalArgumentException("The message is not in the storage");
    }

    this.KvStoreCURD(v.function, v.key, v.value);
    this.kvCache.remove(messageId);
    this.sendAck(messageId, callBackServer, AckType.ackGo);
  }

  public void prepareKeyValue(UUID messageId, String functionality, String key, String value, int callBackServer)
      throws RemoteException {
    // System.out.println("prepareKeyValue" + callBackServer);
    // System.out.println("prepareKeyValue my port" + myPort);
    if (this.kvCache.containsKey(messageId)) {

      sendAck(messageId, callBackServer, AckType.AkcPrepare);
    }

    this.addToTempStorage(messageId, functionality, key, value);
    sendAck(messageId, callBackServer, AckType.AkcPrepare);
  }

  public void setServersInfo(int[] otherServersPorts, int yourPort) throws RemoteException {

    this.otherServers = otherServersPorts;
    this.myPort = yourPort;
  }

  public int getPort() throws RemoteException {

    return this.myPort;
  }

  private void sendAck(UUID messageId, int destination, AckType type) {
    try {
      Registry registry = LocateRegistry.getRegistry(destination);
      ServerInterface stub = (ServerInterface) registry.lookup("compute.ServerInterface");
      // System.out.println("sendAck" + myPort);
      stub.ackMe(messageId, myPort, type, stopRunning);

    } catch (Exception ex) {
      serverHelp.log("Something went wrong in sending Ack, removing data from temporary storage");
      this.kvCache.remove(messageId);
    }
  }

  private void addToTempStorage(UUID messageId, String functionality, String key, String value) {
    Value v = new Value();
    v.function = functionality;
    v.key = key;
    v.value = value;

    this.kvCache.put(messageId, v);
  }

  public void exit(UUID messageId) {

  }
}

class Value {
  String function;
  String key;
  String value;
}

class Ack {
  public boolean isAcked;
}
