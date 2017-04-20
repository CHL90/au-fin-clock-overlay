import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import processing.core.PApplet;
import processing.core.PShape;
import processing.net.Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FinancialProjection extends PApplet {

    // Setup and calibration
    private final int width = 600;
    private final int height = 400;
    private int centerPositionX = width/2;
    private int centerPositionY = height/2;
    private final int CALIBRATION_SPEED_HIGH = 20;
    private final int CALIBRATION_SPEED_LOW = 2;
    private int calibrationSpeed = 1;
    private float SCALE = 1.0f;
    public static float CLOCK_RADIUS = 150;
    public static float CLOCK_CENTER_RADIUS = 50;
    public static float DAY_ROTATION = TWO_PI/30;

    private static FinancialData finData;

    // Network communication
    private Client client;
    private float serverAvailabilityTimer = millis();
    private float reconnectTimer = millis();

    // Text and animation
    private List<String> finDataTextList;
    private int textTransparency = 1;
    private int fadingSpeed = 3; // Fading speed
    private int transactionNumber = 0; // Transaction being inspected
    private int dayOfMonth;
    private boolean displayDayMsg = true;

    // Custom shapes
    private PShape dayLine;
    private PShape spendingLines;

    public static void main(String[] args) {
        if(args.length > 0) {

            // TODO: Create separate method in order to request data continuously

            int disposableIncome = Integer.parseInt(args[0]);
            String csv = "";

            try {
                csv = Unirest.get("http://198.211.106.128:1337/csv/" + args[1] + ".csv")
                        .header("Accept", "text/csv")
                        .asString().getBody();
            } catch (UnirestException e) {
                // Potentially retry connection?
            }

            finData = new FinancialData(disposableIncome, csv);

        }
        PApplet.main("FinancialProjection", args);
    }

    public void settings() {
        fullScreen();
        //size(width, height);
    }

    public void setup() {
        // Init network communication
        //System.out.println("Init tcp client");
        //client = new Client(this, "192.168.43.60", 1337);

        // Create custom line that represents one day
        stroke(255);
        strokeWeight(1f);
        dayLine = createShape();
        dayLine = Graphics.createDayLine(dayLine);

        // Draw graphics into spending lines shape
        spendingLines = createShape();
        spendingLines = Graphics.createSpendingGraphic(finData, spendingLines);

        /* This method should be called by the clock to update the text displayed in the center. Default 0 to display
           nothing on start up. */
        setCenterText(11);
    }

    public void draw() {
        /*if (client.available() > 0) {
            String response = client.readString();
            System.out.print(response);

            if (response.contains("ping")) {
                // Reset reconnect timer
                System.out.println("Reset reconnect timer");
                reconnectTimer = millis();
            }
        }

        if (millis() - reconnectTimer > 15000) {
            System.out.println("Init new connection");
            client.dispose();
            Socket s = null;
            try {
                SocketAddress sa = new InetSocketAddress("192.168.43.60", 1337);
                s = new Socket();
                s.connect(sa, 1500);
                client = new Client(this, s);
            } catch (IOException e) {
                e.printStackTrace();
            }
            reconnectTimer = millis();
        }

        pingServer();*/

        background(0);
        drawDayLines(30);
        drawSpendingGraphic();
        drawCenter();
        drawCenterText();
    }

    // Ping server every x milliseconds
    private void pingServer() {
        if (!client.active()) return;

        if (millis() - serverAvailabilityTimer > 5000) {
            client.write("ping");
            serverAvailabilityTimer = millis();
            System.out.println("Pinging server");
        }
    }

    private void drawCenterText() {

        // If nothing is inspected do not draw anything
        if (dayOfMonth == 0) return;

        // Set text options
        textSize(10 * SCALE);
        textAlign(CENTER);

        if (displayDayMsg) {
            // Fade in/out effect
            if (textTransparency >= 255) {
                fadingSpeed = -fadingSpeed;
            }
            textTransparency += fadingSpeed;

            if (textTransparency <= 0) {
                displayDayMsg = false;
            }

            fill(0, textTransparency);
            text("Day " + dayOfMonth, centerPositionX, centerPositionY);
            return;
        }

        if (finDataTextList.isEmpty()) {
            fill(0);
            text("No Transactions", centerPositionX, centerPositionY);
        } else if (finDataTextList.size() == 1) {
            fill(0);
            text(finDataTextList.get(0), centerPositionX, centerPositionY);
        } else {
            // Fade in/out effect
            if (textTransparency <= 0 || textTransparency >= 255) {
                fadingSpeed = -fadingSpeed;
            }
            textTransparency += fadingSpeed;

            if (textTransparency <= 0) {
                transactionNumber++;
                transactionNumber = transactionNumber % finDataTextList.size();
            }

            fill(0, textTransparency);
            text(finDataTextList.get(transactionNumber), centerPositionX, centerPositionY);
        }
    }

    private void setCenterText(int dayOfMonth) {
        // Store day of month. Used when drawing center text
        this.dayOfMonth = dayOfMonth;

        // Loop through financial data and store text and amount
        List<FinancialData.DataEntry> finDataList = finData.getFinancialDataList();
        finDataTextList = new ArrayList<>();

        for (FinancialData.DataEntry entry : finDataList) {
            if (Integer.parseInt(entry.getDate().split("\\.")[0]) == dayOfMonth) {
                String text = StringUtils.substring(entry.getText(), 0, 10);
                finDataTextList.add(text + "\n" + entry.getAmount());
            }
        }

        // Reset displayDayMsg and textTransparency when text in center is updated. Allows quick updating without bugs.
        displayDayMsg = true;
        textTransparency = 1;
    }

    private void drawCenter() {
        stroke(0);
        fill(255, 255, 255);
        pushMatrix();
        translate(centerPositionX, centerPositionY);
        scale(SCALE);
        ellipse(0, 0, CLOCK_CENTER_RADIUS * 2, CLOCK_CENTER_RADIUS * 2);
        popMatrix();
    }

    private void drawDayLines(int dayOfMonth) {
        for (int i = 0; i <= dayOfMonth; i++) {
            pushMatrix();
            translate(centerPositionX, centerPositionY);
            rotate(DAY_ROTATION * i);
            scale(SCALE);
            shape(dayLine);
            popMatrix();
        }
    }

    private void drawSpendingGraphic() {
        fill(255, 255, 255);
        pushMatrix();
        translate(centerPositionX, centerPositionY);
        scale(SCALE);
        shape(spendingLines);
        popMatrix();
    }

    public void keyPressed() {
        if (keyCode == SHIFT) calibrationSpeed = CALIBRATION_SPEED_HIGH;

        // 1: scale up 2: scale down 3: reset scale 4: reset position
        switch (key) {
            case '1': SCALE += 0.1f;
                break;
            case '2': SCALE -= 0.1f;
                break;
            case '3': SCALE = 1.0f;
                break;
            case '4':
                centerPositionX = width/2;
                centerPositionY = height/2;
                break;
        }

        // Calibrate position of graphics with arrow keys
        switch (keyCode) {
            case UP:
                centerPositionY -= calibrationSpeed;
                break;
            case DOWN:
                centerPositionY += calibrationSpeed;
                break;
            case LEFT:
                centerPositionX -= calibrationSpeed;
                break;
            case RIGHT:
                centerPositionX += calibrationSpeed;
                break;
        }
    }

    public void keyReleased() {
        if (keyCode == SHIFT) calibrationSpeed = CALIBRATION_SPEED_LOW;
    }

}