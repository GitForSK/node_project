import java.net.*;
import java.io.*;

public class NetworkClient {
    public static void main(String[] args) throws IOException {
        // parameter storage
        String gateway = null;
        int port = 0;
        String identifier = null;
        String command = null;

        // Parameter scan loop
        for(int i=0; i<args.length; i++) {
            switch (args[i]) {
                case "-ident":
                    identifier = args[++i];
                    break;
                case "-gateway":
                    String[] gatewayArray = args[++i].split(":");
                    gateway = gatewayArray[0];
                    port = Integer.parseInt(gatewayArray[1]);
                    break;
                case "terminate":
                    command = "TERMINATE";
                    break;
                default:
                    if(command == null) command = args[i];
                    else if(! "TERMINATE".equals(command)) command += " " + args[i];
            }
        }



        // communication socket and streams
        Socket netSocket;
        PrintWriter out;
        BufferedReader in;
        try {
            System.out.println("Connecting with: " + gateway + " at port " + port);
            netSocket = new Socket(gateway, port);
            out = new PrintWriter(netSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(netSocket.getInputStream()));
            System.out.println("Connected");

            if(! "TERMINATE".equals(command)) {
                command = identifier + " " + command;
            }
            System.out.println("Sending: " + command);
            out.println(command);
            // Read and print out the response
            String response;
            while ((response = in.readLine()) != null) {
                System.out.println(response);
            }



            // Terminate - close all the streams and the socket
            out.close();
            in.close();
            netSocket.close();
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + gateway + ".");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("No connection with " + gateway + ".");
            System.exit(1);
        }



    }
}