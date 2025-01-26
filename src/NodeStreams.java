import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import static java.lang.System.exit;

public class NodeStreams {

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    public boolean isClosed = true;

    public NodeStreams(String ipAdres, int port) {
        try {
            this.socket = new Socket(ipAdres,port);
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isClosed = false;
        } catch (UnknownHostException hostex) {

            try {
                this.socket = new Socket("localhost",port);
                this.writer = new PrintWriter(socket.getOutputStream(), true);
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to connect");
            exit(1);
        }

    }

    public NodeStreams(Socket socket) {
        try {
            this.socket = socket;
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isClosed = false;
        } catch (IOException e) {
            System.out.println("Failed to connect");
            exit(1);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void sendMessage(String message){
        writer.println(message);
    }

    public void sendMessage(int message){
        writer.println(message);
    }

    public String readMessage() throws IOException {
        return reader.readLine();
    }

    public void closeConnection(){
        try {
            isClosed = true;
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Socket port: " + socket.getLocalPort();
    }

}
