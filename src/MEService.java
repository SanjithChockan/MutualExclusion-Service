// Implements cs-enter() and cs-leave()
// which is accessed by the Application module

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class MEService {

    // Terminal text colors
    String RESET = "\u001B[0m";
    String RED = "\u001B[31m";
    String GREEN = "\u001B[32m";
    String YELLOW = "\u001B[33m";
    String ANSI_WHITE = "\u001B[37m";
    String BLUE = "\u001B[34m";
    String PURPLE_BOLD = "\033[1;35m";
    String CYAN_UNDERLINED = "\033[4;36m";

    // from file and main
    private ArrayList<Node> neighbors;
    private Node currentNode;
    private AtomicIntegerArray sharedKeys;
    private AtomicIntegerArray requests;
    private AtomicInteger timeStamp;
    private SCTPClient client;

    // ME Service variables
    private Queue<Integer> deferredNodes;
    private ArrayList<Node> nodes;
    private int nodeSize;
    private int keysNeeded;
    private AtomicInteger PREVIOUS_REQ;
    private Queue<Integer> unfulfilledRequests;
    private ConcurrentHashMap<Integer, Integer> unfulfilledRequestsMap;
    private AtomicInteger numOfKeys;
    private volatile boolean hasRequests;

    // used for testing: consistency, message complexity
    AtomicInteger criticalSectionCount = new AtomicInteger();
    AtomicInteger messagesExchanged = new AtomicInteger();

    // create filewriter to write output
    FileWriter fileWriter;
    BufferedWriter outputWriter;

    // vector clock to check for consistency
    private AtomicIntegerArray vectorClock;

    private AtomicBoolean readyForCritical = new AtomicBoolean(Boolean.FALSE);
    private AtomicBoolean requestsInProgress = new AtomicBoolean(Boolean.FALSE);

    public MEService(ArrayList<Node> nodes, ArrayList<Node> neighbors, Node currentNode, AtomicIntegerArray sharedKeys,
            SCTPClient client, AtomicInteger numOfKeys) {
        this.nodes = nodes;
        this.neighbors = neighbors;
        this.currentNode = currentNode;
        this.sharedKeys = sharedKeys;
        this.requests = new AtomicIntegerArray(neighbors.size() + 1);
        this.client = client;
        this.timeStamp = new AtomicInteger(0);
        this.deferredNodes = new LinkedList<>();
        this.unfulfilledRequests = new LinkedList<>();
        this.unfulfilledRequestsMap = new ConcurrentHashMap<>();
        this.numOfKeys = numOfKeys;
        this.hasRequests = false;
        this.PREVIOUS_REQ = new AtomicInteger();
        this.nodeSize = nodes.size();
        this.vectorClock = new AtomicIntegerArray(nodeSize);
        this.keysNeeded = nodeSize - 1;
        try {
            fileWriter = new FileWriter(
                    "/home/010/s/sx/sxc180101/AdvancedOS/project2/node-" + this.currentNode.nodeID
                            + "-CSTimeStamps.out");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        outputWriter = new BufferedWriter(fileWriter);
    }

    public void startClients() throws Exception {
        client.initiateChannels();
    }

    public void csenter() {
        // check if has all the keys to execute critical section
        // if not: request permission from other processes
        PREVIOUS_REQ.set(timeStamp.get());
        hasRequests = true;

        // messagesExchanged.set(0);
        hasAllKeys();

        readyForCritical.set(numOfKeys.get() == keysNeeded);

        while (!readyForCritical.get())
            ;
        hasRequests = false;
        criticalSectionCount.incrementAndGet();

        // edge case when a node in request progress is deffered later due to timestamp
        // when C.S is happening
        // preserved in the deffered queue before it is sent out after critical section

        // while (requestsInProgress.get());

        System.out.println("Number of Keys in CS => [" + numOfKeys.get() + "]");
        System.out.println(RED + "\nEntering CS" + sharedKeys.toString() + '\n' + RESET);
        // increment timestamp once you enter CS
        timeStamp.incrementAndGet();
        vectorClock.incrementAndGet(currentNode.nodeID);
        System.out.println(criticalSectionCount.get());
        System.out.println(GREEN + "Critical Section Count: " + criticalSectionCount + RESET);
        System.out.println("Current Vector Clock: " + vectorClock.toString());
        try {
            outputWriter.write("" + criticalSectionCount.get() + ":" + vectorClock.toString() + "-");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void csleave() {
        System.out.println(RED + "\nExiting CS" + sharedKeys.toString() + '\n' + RESET);
        // exit out of critical section
        timeStamp.incrementAndGet();
        vectorClock.incrementAndGet(currentNode.nodeID);
        try {
            outputWriter.write("" + vectorClock.toString());
            outputWriter.newLine();
            outputWriter.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        readyForCritical.set(Boolean.FALSE);

        sendDeferredKeys();

    }

    private boolean hasAllKeys() {

        boolean flag = true;
        synchronized (sharedKeys) {
            for (Node neighbor : neighbors) {
                if (sharedKeys.get(neighbor.nodeID) == 0) {
                    flag = false;
                    // send request for key if it already hasn't requested
                    if (requests.get(neighbor.nodeID) == 0) {
                        timeStamp.incrementAndGet();
                        vectorClock.incrementAndGet(currentNode.nodeID);
                        unfulfilledRequests.add(PREVIOUS_REQ.get());
                        unfulfilledRequestsMap.put(neighbor.nodeID, PREVIOUS_REQ.get());
                        sendRequest(neighbor,
                                new Message(MessageType.REQUEST, "I need key", currentNode.nodeID, PREVIOUS_REQ,
                                        vectorClock, criticalSectionCount));
                    }
                }
            }
        }

        return flag;

    }

    public void handleRequest(Message request) {
        timeStamp.set(Math.max(timeStamp.get(), request.timestamp.get()));
        synchronized (vectorClock) {
            for (int i = 0; i < nodeSize; i++) {
                vectorClock.set(i, Math.max(request.vectorClock.get(i), this.vectorClock.get(i)));
            }
        }
        timeStamp.incrementAndGet();
        vectorClock.incrementAndGet(currentNode.nodeID);
        criticalSectionCount.set(Math.max(request.criticalSectionCount.get(), this.criticalSectionCount.get()));
        if (readyForCritical.get()) {
            synchronized (deferredNodes) {
                deferredNodes.add(request.NodeID);
            }
        } else if (!hasRequests) {
            timeStamp.incrementAndGet();
            vectorClock.incrementAndGet(currentNode.nodeID);
            sendReply(nodes.get(request.NodeID),
                    new Message(MessageType.REPLY, "Have my key", currentNode.nodeID, timeStamp, vectorClock,
                            criticalSectionCount));
        }
        // else outstanding requests
        // if current node timestamp < requesting node, defer
        // else send key

        else if (hasRequests) {

            requestsInProgress.set(Boolean.TRUE);

            if (PREVIOUS_REQ.get() < request.timestamp.get()) {

                synchronized (deferredNodes) {
                    deferredNodes.add(request.NodeID);
                }

            }

            else if (PREVIOUS_REQ.get() == request.timestamp.get()) {
                if (request.NodeID > currentNode.nodeID) {
                    timeStamp.incrementAndGet();
                    vectorClock.incrementAndGet(currentNode.nodeID);
                    sendReply(nodes.get(request.NodeID),
                            new Message(MessageType.REPLY, "Have my key", currentNode.nodeID, timeStamp, vectorClock,
                                    criticalSectionCount));
                    // send request right after

                    if (requests.get(request.NodeID) == 0) {
                        unfulfilledRequests.add(PREVIOUS_REQ.get());
                        unfulfilledRequestsMap.put(request.NodeID, PREVIOUS_REQ.get());
                        timeStamp.incrementAndGet();
                        vectorClock.incrementAndGet(currentNode.nodeID);
                        sendRequest(nodes.get(request.NodeID),
                                new Message(MessageType.REQUEST, "Gimme Key", currentNode.nodeID, PREVIOUS_REQ,
                                        vectorClock, criticalSectionCount));
                    }

                } else {
                    synchronized (deferredNodes) {
                        deferredNodes.add(request.NodeID);
                    }
                }
            }

            else {
                synchronized (sharedKeys) {
                    System.out.println(BLUE + "MY REQ TIMESTAMP IS LARGER SO SENDING KEY" + RESET);
                    timeStamp.incrementAndGet();
                    vectorClock.incrementAndGet(currentNode.nodeID);
                    sendReply(nodes.get(request.NodeID),
                            new Message(MessageType.REPLY, "Have my key", currentNode.nodeID, timeStamp, vectorClock,
                                    criticalSectionCount));
                    // incrementTS();
                    unfulfilledRequests.add(PREVIOUS_REQ.get());
                    unfulfilledRequestsMap.put(request.NodeID, PREVIOUS_REQ.get());
                    timeStamp.incrementAndGet();
                    vectorClock.incrementAndGet(currentNode.nodeID);
                    sendRequest(nodes.get(request.NodeID),
                            new Message(MessageType.REQUEST, "Gimme Key", currentNode.nodeID, PREVIOUS_REQ, vectorClock,
                                    criticalSectionCount));
                }
                // timeStamp.incrementAndGet();
                // incrementTS();
            }
            requestsInProgress.set(Boolean.FALSE);
        }

        System.out.println("SHARED KEYS OF MACHINE " + currentNode.nodeID + ": " + sharedKeys);

    }

    public void handleReply(Message reply) {
        messagesExchanged.incrementAndGet();
        System.out.println(RED + "ENTERS HANDLE REPLY FOR " + reply.NodeID + RESET);
        timeStamp.set(Math.max(timeStamp.get(), reply.timestamp.get()));
        synchronized (vectorClock) {
            for (int i = 0; i < nodeSize; i++) {
                vectorClock.set(i, Math.max(reply.vectorClock.get(i), this.vectorClock.get(i)));
            }
        }
        timeStamp.incrementAndGet();
        vectorClock.incrementAndGet(currentNode.nodeID);
        criticalSectionCount.set(Math.max(reply.criticalSectionCount.get(), this.criticalSectionCount.get()));
        int grantedFrom = reply.NodeID;

        int sentTS = unfulfilledRequestsMap.get(grantedFrom);
        unfulfilledRequests.remove(sentTS);
        unfulfilledRequestsMap.remove(grantedFrom);

        sharedKeys.getAndIncrement(grantedFrom);
        requests.getAndDecrement(grantedFrom);

        // edge case of receiving all keys and entering into CS but another request is
        // in progress
        numOfKeys.getAndIncrement();
        if (numOfKeys.get() == keysNeeded) {
            while (requestsInProgress.get())
                ;
        }
        readyForCritical.set(numOfKeys.get() == keysNeeded);

    }

    // client side
    // take care of timestamp increments before method calls to handle concurrent
    // executions
    public void sendRequest(Node destination, Message message) {
        messagesExchanged.incrementAndGet();
        System.out.println("SENDING REQUEST TO " + destination.nodeID);
        System.out.println("requests array BEFORE: " + requests.toString());
        requests.incrementAndGet(destination.nodeID);
        System.out.println("requests array AFTER: " + requests.toString());
        try {
            // incrementTS();
            client.sendMessage(destination, message);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println(RED + "COULD NOT SEND MESSAGE" + RESET);
            e.printStackTrace();
        }
    }

    // take care of timestamp increments before method calls to handle concurrent
    // executions
    public void sendReply(Node destination, Message message) {

        sharedKeys.decrementAndGet(destination.nodeID);
        numOfKeys.getAndDecrement();

        System.out.println("SENDING REPLY TO " + destination.nodeID);
        System.out.println(sharedKeys.toString());
        try {
            client.sendMessage(destination, message);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void sendDeferredKeys() {

        synchronized (deferredNodes) {
            while (!deferredNodes.isEmpty()) {
                int id = deferredNodes.remove();
                // sharedKeys.decrementAndGet(id);
                timeStamp.incrementAndGet();
                vectorClock.incrementAndGet(currentNode.nodeID);
                sendReply(nodes.get(id),
                        new Message(MessageType.REPLY, "sending deffered key", currentNode.nodeID, timeStamp,
                                vectorClock, criticalSectionCount));
            }
        }

    }

    // Collect messages sent for critical section entry for some c
    public void writeMessageComplexity(int csExecutionTime) {
        String filePath = "/home/010/s/sx/sxc180101/AdvancedOS/project2/node-" + currentNode.nodeID
                + "-MessageComplexity-" + csExecutionTime + ".out";
        File file = new File(filePath);
        FileWriter fr = null;
        BufferedWriter br = null;
        try {
            fr = new FileWriter(file, true);
            br = new BufferedWriter(fr);

            // you can use write or append method
            br.write(String.valueOf((double)(messagesExchanged.get())/300));
            br.newLine();
            br.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeFileWriter() {
        try {
            outputWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
