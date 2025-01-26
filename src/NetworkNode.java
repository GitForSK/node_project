import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.lang.System.exit;

public class NetworkNode implements Runnable {

    private int nodeID = 0;
    private int nodePort = 0;
    private String adres = "";
    private String ipFromServer = "";
    private int portToConnect = 0;

    LinkedHashSet<Resource> currentResources;
    LinkedHashSet<Resource> reservedResources = new LinkedHashSet<>();
    HashMap<Integer, LinkedHashSet<Resource>> clientsResources = new HashMap<>();
    LinkedHashSet<String> reports = new LinkedHashSet<>();
    HashMap<Integer, NodeStreams> connectedNodes = new HashMap<>();
    boolean turned = false;
    LinkedHashSet<Thread> alloactor = new LinkedHashSet<>();

/////////////  Flag handler

    public static void main(String[] args) {

        int id = 0;
        int nPort = 0;
        String ipAdres = "";
        int anotherPort = 0;
        LinkedHashSet<Resource> givenRes = new LinkedHashSet<Resource>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]){
                case "-ident":
                    id = Integer.parseInt(args[++i]);
                    System.out.println("okay ident: " + id);
                    break;
                case "-tcpport":
                    nPort = Integer.parseInt(args[++i]);
                    System.out.println("okay port: " + nPort);
                    break;
                case "-gateway":
                    String[] tmp = args[++i].split(":");
                    ipAdres = tmp[0];
                    anotherPort = Integer.parseInt(tmp[1]);
                    System.out.println("okay gate: " + args[i]);
                    break;
                default:
                    if (args[i].matches("\\p{Alpha}:\\d")){
                        String[] split = args[i].split(":");
                        int number = Integer.parseInt(split[1]);
                        Resource rescToPush = new Resource(split[0].charAt(0), number);
                        givenRes.add(rescToPush);
                        System.out.println("Resource pushed: " + args[i]);
                    } else {
                        System.out.println("Wrong flags.");
                        System.out.println(args.length);
                        exit(1);
                    }
            }

        }

        new Thread(new NetworkNode(id, nPort, ipAdres, anotherPort, givenRes)).start();

    }

    NetworkNode(int id, int port, String ipAdres, int anotherPort, LinkedHashSet<Resource> resc){
        this.nodeID = id;
        this.nodePort = port;
        this.adres = ipAdres;
        this.portToConnect = anotherPort;
        this.currentResources = resc;
        for (Resource r: resc) {
            reservedResources.add(new Resource(r.getNameOfResource(), 0));
        }
    }

    @Override
    public void run() {

//////////////////// Create node serv

        try (
                ServerSocket nodeSocket = new ServerSocket(nodePort)
        )
        {
            turned = true;                  // node created

            /////////////////////////////   Connection to prev node

            ipFromServer = nodeSocket.getInetAddress().toString();

            if (!adres.isEmpty()){
                System.out.println("Connecting to another node.");
                connectToAntoherNode(nodeSocket.getLocalSocketAddress().toString());
                System.out.println("Connected");
            }

            ///////////////////////////////     Listening

            nodeSocket.setSoTimeout(1500);

            while (turned) {

                System.out.println("Listening");

                try {
                    NodeStreams listeningSoc = new NodeStreams(nodeSocket.accept());
                    if (listeningSoc.getSocket().isConnected()) {
                        new Thread(clientHandler(listeningSoc)).start();
                        System.out.println("Connection catched");
                    }
                }catch (SocketTimeoutException timex) {

                }

            }

        } catch (IOException e) {
            System.out.println("Failed to create node.");
            exit(1);
        }

        exit(0);
    }

    private void connectToAntoherNode(String serverSocketAdress){

        try {
            NodeStreams anotherNode = new NodeStreams(adres, portToConnect);

            anotherNode.sendMessage("HELLO");

            String readLine = anotherNode.readMessage();

            if (readLine.equals("HELLO")){
                anotherNode.sendMessage(nodeID);
                readLine = anotherNode.readMessage();
                if (readLine.equals("Error")){
                    anotherNode.closeConnection();
                    System.out.println("Failed to connect " + adres + ':' + portToConnect + ". This node already exist.");
                    exit(1);
                }
                connectedNodes.put(Integer.parseInt(readLine), anotherNode);
                anotherNode.sendMessage(serverSocketAdress);
                System.out.println("Connected to another node");
            } else {
                anotherNode.closeConnection();
                System.out.println("Failed to connect " + adres + ':' + portToConnect);
                exit(1);
            }

            readLine = anotherNode.readMessage();

            if (readLine.equals("ERROR")) {
                anotherNode.closeConnection();
                System.out.println("Failed to connect " + adres + ':' + portToConnect);
                exit(1);
            }

        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + adres + ":" + portToConnect);
            System.exit(1);
        }catch (IOException e) {
            System.err.println("No connection with " + adres + ":" + portToConnect);
            System.exit(1);
        }

    }


    private Runnable clientHandler(NodeStreams client) {
        return new Runnable() {

            @Override
            public void run() {

                try {

                    String readLine;

                    while ((readLine = client.readMessage()) != null && !client.isClosed){
                        String[] toSplit = {};
                        int clientId = 0;
                        int deliveredNodeID = 0;
                        if (readLine.contains(" ")){
                            toSplit = readLine.split(" ");
                        }

                        if (toSplit.length == 0){

                        } else if (toSplit.length == 2 && !toSplit[1].matches("\\p{Alpha}:\\d")) {
                            deliveredNodeID = Integer.parseInt(toSplit[0]);
                            readLine = toSplit[1];
                        } else if (toSplit[toSplit.length -1] == "TERMINATE"){
                            readLine = toSplit[toSplit.length -1];

                        } else if (connectedNodes.toString().contains(toSplit[1])) {
                            clientId = Integer.parseInt(toSplit[0]);
                            deliveredNodeID = Integer.parseInt(toSplit[1]);
                            toSplit = Arrays.copyOfRange(toSplit, 2, toSplit.length);
                            readLine = toSplit[0].matches("\\p{Alpha}:\\d") ? "CHECK RESOURCE" : "FAILED";

                        } else {
                            clientId = Integer.parseInt(toSplit[0]);
                            toSplit = Arrays.copyOfRange(toSplit, 1, toSplit.length);
                            readLine = Arrays.stream(toSplit).allMatch(
                                    (resource) -> {
                                        return resource.matches("\\p{Alpha}:\\d");
                                    }
                            ) ? "RESOURCE" : "FAILED";

                        }

                        switch (readLine) {
                            case "HELLO" -> {
                                client.sendMessage("HELLO");
                                readLine = client.readMessage();
                                int identify = Integer.parseInt(readLine);

                                if (checkID(identify, identify) || identify == nodeID){
                                    client.sendMessage("Error");
                                    break;
                                }

                                client.sendMessage(nodeID);

                                readLine = client.readMessage();
                                String[] getSocketInfo = readLine.split(":");
                                int newSocektPort = Integer.parseInt(getSocketInfo[1]);
                                connectedNodes.put(identify, new NodeStreams(getSocketInfo[0], newSocektPort));

                                client.sendMessage("OK");
                            }
                            case "TERMINATE" -> {
                                if (!connectedNodes.containsValue(client)) {
                                    System.out.println("Terminated actived");
                                    client.sendMessage("Terminated");
                                }
                                terminateNet();
                            }
                            case "TERMINATE_ALL" -> {
                                System.out.println("Terminated actived");
                                terminateNet();
                            }
                            case "RESOURCE" -> {
                                if (distributeResources(toSplit, clientId, deliveredNodeID)) {

                                    for (Thread t: alloactor) {
                                        t.start();
                                    }
                                    alloactor.clear();

                                    activateAllocator(deliveredNodeID);

                                    client.sendMessage("BOOKED");

                                    for (String rep: reports) {
                                        if (!rep.isEmpty()) client.sendMessage(rep);
                                    }
                                    reports.clear();
                                    client.closeConnection();

                                } else {
                                    clearAllocator(deliveredNodeID);
                                    client.sendMessage("FAILED");
                                    client.closeConnection();
                                }

                            }
                            case "CHECK RESOURCE" -> {

                                if (distributeResources(toSplit, clientId, deliveredNodeID)) {

                                    System.out.println("Resources checked.");
                                    client.sendMessage("BOOKED");

                                    client.sendMessage(reports.size());
                                    for (String rep: reports) {
                                        client.sendMessage(rep);
                                    }
                                    reports.clear();

                                } else {
                                    client.sendMessage("FAILED");
                                }

                            }
                            case "ACTIVATE_ALLOCATOR" -> {
                                activateAllocator(deliveredNodeID);
                                if (!alloactor.isEmpty()){
                                    for (Thread t: alloactor) {
                                        t.start();
                                    }

                                    alloactor.clear();

                                }


                            }
                            case "CLEAR_ALLOCATOR" -> {
                                clearAllocator(deliveredNodeID);
                                alloactor.clear();
                            }
                            case "FAILED" -> {
                                client.sendMessage("Failed. Wrong resources.");
                            }
                            case "CHECK_ID" ->{
                                int idToCheck = Integer.parseInt(client.readMessage());
                                int callerId = Integer.parseInt(client.readMessage());
                                if (checkID(idToCheck, callerId)){
                                    client.sendMessage("ID_MATCHED");
                                } else {
                                    client.sendMessage("NOT_MATCHED");
                                }
                            }

                        }

                    }


                } catch (SocketException socEx) {

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    private boolean distributeResources(String[] rescToDist, int clientIdenf, int nodeKey) {
        LinkedHashSet<Resource> toBook = new LinkedHashSet<>();
        LinkedHashSet<Resource> canBeBook = new LinkedHashSet<>();
        boolean toReturn = true;

        for (String r : rescToDist) {
            char rescName = r.charAt(0);
            int rescNumb = Integer.parseInt(r.substring(2));
            toBook.add(new Resource(rescName, rescNumb));
        }

        boolean ifToSearch = false;

        for (Resource r : toBook) {
            if (
                    currentResources.stream().anyMatch(
                            (currRes) -> {
                                return currRes.getNameOfResource() == r.getNameOfResource() &&
                                        currRes.getNumberOfResource() >= r.getNumberOfResource();
                            }
                    )
            ){
                canBeBook.add(r);
            } else if (
                    currentResources.stream().anyMatch(
                            (currRes) -> {
                                if (currRes.getNameOfResource() == r.getNameOfResource() &&
                                        currRes.getNumberOfResource() < r.getNumberOfResource() &&
                                        currRes.getNumberOfResource() > 0) {
                                    canBeBook.add(new Resource(r.getNameOfResource(), currRes.getNumberOfResource()));
                                    int num = r.getNumberOfResource() - currRes.getNumberOfResource();
                                    r.setNumberOfResource(num);
                                    return true;
                                } else return false;
                            }
                    )
            ) {
                ifToSearch = true;
            } else {
                ifToSearch = true;
            }

        }

        LinkedHashSet<Resource> formClientMap = new LinkedHashSet<>();
        StringBuilder report = new StringBuilder();

        for (Resource r : canBeBook) {
            if (canBeBook.size() > 1) {
                report.append(r);
                report.append(":").append(ipFromServer.equals("0.0.0.0/0.0.0.0") ? "localhost" : ipFromServer).append(":").append(nodePort).append('\n');
            } else {
                report.append(r);
            }
            toBook.remove(r);
            formClientMap.add(r);
        }

        if (!report.isEmpty() && canBeBook.size() == 1) {
            report.append(":").append(ipFromServer.equals("0.0.0.0/0.0.0.0") ? "localhost" : ipFromServer).append(":").append(nodePort);
        }
        reports.add(report.toString());

        if (formClientMap.size() != 0) {
            clientsResources.put(
                    clientsResources.containsKey(clientIdenf) ? clientIdenf + 10 : clientIdenf
                    , formClientMap);
        }

        if (!ifToSearch) {
            alloactor.add(new Thread(allocate(canBeBook)));
            return true;
        }

        for (Resource r: toBook) {
            if(!searchForResources(r, clientIdenf, nodeKey)){
                toReturn = false;
            }
            if (!toReturn) {
                break;
            }
        }

        if (!canBeBook.isEmpty()) {
            alloactor.add(new Thread(allocate(canBeBook)));
        }

        return toReturn;

    }

    private void bookResources(LinkedHashSet<Resource> canBeBook) {
        if (canBeBook.isEmpty()){
            return;
        }


        for (Resource r: canBeBook) {
            reservedResources.stream().anyMatch(
                    (resRes) -> {
                        if (resRes.getNameOfResource() == r.getNameOfResource()){
                            int currPlus = resRes.getNumberOfResource() + r.getNumberOfResource();
                            resRes.setNumberOfResource(currPlus);
                            return true;
                        } else return false;
                    }
            );
        }

        for (Resource res : canBeBook) {

            currentResources.stream().anyMatch(
                    (currRes) -> {
                        if (currRes.getNameOfResource() == res.getNameOfResource()) {
                            int a = currRes.getNumberOfResource() - res.getNumberOfResource();
                            currRes.setNumberOfResource(a);
                            return true;
                        } else {
                            return false;
                        }
                    }
            );
        }

        canBeBook.clear();

    }

    private boolean searchForResources(Resource resc, int clientIdenf, int nodeKey){
        System.out.println("Searching");

        String concat = resc.toString();
        String finalConcat = concat;
        AtomicBoolean done = new AtomicBoolean(false);
        connectedNodes.forEach(
                (key, nStream) -> {
                    if (!done.get() && key != nodeKey) {
                        nStream.sendMessage(clientIdenf + " " + nodeID + " " + finalConcat);
                        try {
                            if ((nStream.readMessage()).equals("BOOKED")) {

                                int lengthOfRep = Integer.parseInt(nStream.readMessage());
                                for (int i = 0; i < lengthOfRep; i++) {
                                    reports.add(nStream.readMessage());
                                }

                                done.set(true);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
        );

        System.out.println("Done searching");

        return done.get();
    }

    private Runnable allocate(LinkedHashSet<Resource> canBeBook) {
        return new Runnable() {
            @Override
            public void run() {
                bookResources(canBeBook);

                System.out.println("Resources Booked.");

                System.out.print("Current resources: ");
                for (Resource r: currentResources) {
                    System.out.print(r + " ");
                }
                System.out.println();

                System.out.print("Reserved resources: ");
                for (Resource r: reservedResources) {
                    System.out.print(r + " ");
                }
                System.out.println();

                System.out.print("Reserved resources Map: ");
                System.out.println(clientsResources.toString());
            }
        };
    }

    private void activateAllocator(int nodeKey){
        this.connectedNodes.forEach(
                (key, nStream) -> {
                    if (key != nodeKey) {
                        nStream.sendMessage(nodeID + " ACTIVATE_ALLOCATOR");
                    }
                }
        );
    }

    private void clearAllocator(int nodeKey){
        this.connectedNodes.forEach(
                (key, nStream) -> {
                    if (key != nodeKey) {
                        nStream.sendMessage(nodeID + " CLEAR_ALLOCATOR");
                    }
                }
        );
    }

    private void terminateNet(){
        this.connectedNodes.forEach(
                (key, nStream) -> {
                    if (!nStream.getSocket().isClosed()){
                        nStream.sendMessage("TERMINATE_ALL");
                    }
                    nStream.closeConnection();
                }
        );

        turned = false;

    }

    private boolean checkID(int nodeIdToCheck, int callerId) {
        if (connectedNodes.containsKey(nodeIdToCheck)){
            return true;
        }
        boolean ifnotOkay = false;

        for (NodeStreams ns : connectedNodes.values()) {
            if (connectedNodes.containsKey(callerId)){
                continue;
            }
            ns.sendMessage("CHECK_ID");
            ns.sendMessage(nodeIdToCheck);
            ns.sendMessage(nodeID);
            try {
                if (ns.readMessage().equals("ID_MATCHED")){
                    ifnotOkay = true;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("id checked");

        return ifnotOkay;
    }


}
