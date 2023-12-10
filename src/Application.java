
/*
 * Responsible for generating CS Requests
 * Executing CS on receving permission from ME service
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Application {

    int numOfNodes;
    int interRequestDelay;
    int csExecutionTime;
    int numOfRequests;
    ArrayList<Node> nodes;

    // lambdas for the exponential distribution
    double lambdaInterRequest;
    double lambdaCsExecution;

    MEService mutex;
    Node currentNode;

    // collect data variables
    long responseTime = 0;
    long systemThroughPut = 0;

    public Application(int numOfNodes, int interRequestDelay, int csExecutionTime, int numOfRequests,
            ArrayList<Node> nodes, MEService mutex, Node currentNode) {
        this.numOfNodes = numOfNodes;
        this.interRequestDelay = interRequestDelay;
        this.csExecutionTime = csExecutionTime;
        this.numOfRequests = numOfRequests;
        this.nodes = nodes;
        this.lambdaCsExecution = 1.0 / this.csExecutionTime;
        this.lambdaInterRequest = 1.0 / this.interRequestDelay;
        this.mutex = mutex;
        this.currentNode = currentNode;

    }

    public void generateRequests() {

        Random rand = new Random();
        long currentResponseTime = 0;

        long systemThroughPutStart = System.currentTimeMillis();

        for (int i = 0; i < numOfRequests; i++) {

            // capture time b/w csenter and executecs
            // invoke cs enter
            long responseTimeStart = System.currentTimeMillis();
            mutex.csenter();
            // execute CS; takes care of cs execution time

            Double csExecutionTime = -Math.log(1 - rand.nextDouble()) / lambdaCsExecution;
            int intCS = csExecutionTime.intValue();
            System.out.println("REQUEST NUMBER: " + (i + 1));
            executecs(intCS);
            long responseTimeEnd = System.currentTimeMillis();

            currentResponseTime = (responseTimeEnd - responseTimeStart);
            responseTime += currentResponseTime;
            System.out.println("Response Time: " + (currentResponseTime) + "ms");

            // invoke cs leave
            mutex.csleave();

            // need to get random exponential probability for delay before next call
            Double interRequestDelay = -Math.log(1 - rand.nextDouble()) / lambdaInterRequest;
            int intIRD = interRequestDelay.intValue();
            delay(intIRD);
        }
        long systemThroughPutEnd = System.currentTimeMillis();
        System.out.println("...................DONE GENERATING REQUESTS.......................");

        systemThroughPut = (systemThroughPutEnd - systemThroughPutStart);
        System.out.println("System Throughput: " + systemThroughPut);
        mutex.writeMessageComplexity(csExecutionTime);
        writeResponseTime();
        writeSystemThroughput();
        mutex.closeFileWriter();
    }

    public void executecs(int executionTime) {
        System.out.println("Executing Critical Section");
        try {
            Thread.sleep(executionTime);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void delay(int IRD) {
        System.out.println("Delay before next CS Call");
        try {
            Thread.sleep(IRD);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeResponseTime() {
        String filePath = "/home/010/s/sx/sxc180101/AdvancedOS/project2/node-" + currentNode.nodeID
                + "-ResponseTime-" + csExecutionTime + ".out";
        File file = new File(filePath);
        FileWriter fr = null;
        BufferedWriter br = null;
        try {
            fr = new FileWriter(file, true);
            br = new BufferedWriter(fr);
        } catch (IOException e) {

        }

        try {
            double avgResponseTime = ((double)responseTime / numOfRequests);
            br.write(String.valueOf(avgResponseTime));
            br.newLine();
            br.flush();
        } catch (IOException e) {

        }
    }

    public void writeSystemThroughput() {
        String filePath = "/home/010/s/sx/sxc180101/AdvancedOS/project2/node-" + currentNode.nodeID
                + "-SystemThroughput-" + csExecutionTime + ".out";
        File file = new File(filePath);
        FileWriter fr = null;
        BufferedWriter br = null;
        try {
            fr = new FileWriter(file, true);
            br = new BufferedWriter(fr);
        } catch (IOException e) {

        }

        try {
            double systhru = ((double)numOfRequests/systemThroughPut);
            br.write(String.valueOf(systhru));
            br.newLine();
            br.flush();
        } catch (IOException e) {

        }
    }

}
