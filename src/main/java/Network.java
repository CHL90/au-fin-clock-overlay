import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import processing.core.PApplet;
import processing.net.Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class Network extends PApplet {

    private static Client client;
    private static float serverAvailabilityTimer;
    private static float reconnectTimer;

    public static void createTCPConnection(PApplet parent, String address, int port) {
        client = new Client(parent, address, port);
    }

    public static void createTCPConnection(PApplet parent, Socket socket) {
        try {
            client = new Client(parent, socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void initTimers(int millis) {
        serverAvailabilityTimer = millis;
        reconnectTimer = millis;
    }

    public static String getCSVData(String fileName) {
        String csv = "";

        try {
            csv = Unirest.get("http://198.211.106.128:1337/csv/" + fileName + ".csv")
                    .header("Accept", "text/csv")
                    .asString().getBody();
        } catch (UnirestException e) {
            // Potentially retry connection?
        }
        return csv;
    }

    public static void pingServer(int millis) {
        if (!client.active()) return;

        if (millis - serverAvailabilityTimer > 30000) {
            client.write("ping#");
            serverAvailabilityTimer = millis;
            System.out.println("Pinging server");
        }
    }

    public static void checkTCPConnection(int millis, PApplet parent) {
        if (millis - reconnectTimer > 90000) {
            System.out.println("Init new connection");
            client.dispose();
            Socket s = null;
            try {
                SocketAddress sa = new InetSocketAddress("192.168.87.104", 1337);
                s = new Socket();
                s.connect(sa, 1500); // Timeout set to 1500 milliseconds
                createTCPConnection(parent, s);
            } catch (IOException e) {
                e.printStackTrace();
            }
            reconnectTimer = millis;
        }
    }

    public static String getAvailableString() {
        return client.readString();
    }

    public static boolean isClientAvailable() {
        return client.available() > 0;
    }

    public static void resetReconnectTimer(int millis) {
        reconnectTimer = millis;
    }

}
