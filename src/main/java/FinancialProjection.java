import org.apache.commons.lang3.StringUtils;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PShape;
import java.util.ArrayList;
import java.util.List;

public class FinancialProjection extends PApplet {

    // Setup and calibration
    private int centerPositionX;
    private int centerPositionY;
    private final int CALIBRATION_SPEED_HIGH = 20;
    private final int CALIBRATION_SPEED_LOW = 2;
    private int calibrationSpeed = 1;
    private float SCALE = 2.5f;
    public static float CLOCK_RADIUS = 150;
    public static float CLOCK_CENTER_DIAMETER = 100;
    public static float DAY_ROTATION = TWO_PI/30;
    private static int FADING_SPEED;

    private static FinancialData finData;

    // Network communication
    private String currentCommand;
    private boolean watchActive = true;
    private static boolean ONLINE_MODE;

    // Text and animation
    private List<String> finDataTextList = new ArrayList<>();
    private int textTransparency = 1;
    private int fade = 3; // Fading speed
    private int transactionNumber = 0; // Transaction being inspected
    private int dayOfMonth;
    private boolean displayDayMsg = true;

    // Custom shapes
    private PShape dayLine;
    private PShape spendingLines;

    public static void main(String[] args) {
        if(args.length > 0) {

            int disposableIncome = Integer.parseInt(args[0]);
            String csv = Network.getCSVData(args[1]);
            finData = new FinancialData(disposableIncome, csv);
            FADING_SPEED = Integer.parseInt(args[2]);
            ONLINE_MODE = Boolean.parseBoolean(args[3]);

        }
        PApplet.main("FinancialProjection", args);
    }

    public void settings() {
        //fullScreen();
        size(800, 600, P3D);
        centerPositionX = displayWidth/2;
        centerPositionY = displayHeight/2;
    }

    public void setup() {
        if (ONLINE_MODE) {
            // Init tcp communication
            Network.createTCPConnection(this, "192.168.43.60", 1337);
            Network.initTimers(millis());
        }
        PFont merriWeather = createFont("MerriweatherSans-Bold.ttf", 32);
        textFont(merriWeather);

        // Create custom line that represents one day
        stroke(255);
        strokeWeight(1f);
        dayLine = createShape();
        dayLine = Graphics.createDayLine(dayLine);

        // Create spending graphics
        createSpendingGraphic();

        /* This method should be called by the clock to update the text displayed in the center. Default 0 to display
           nothing on start up. */
        setCenterText(11);
    }

    public void draw() {
        background(0);
        finData.updateCSVData(this); // Check for new csv data
        delay(watchActive ? 0 : 500);

        if (ONLINE_MODE) {
            // Read commands from the clock
            if (Network.isClientAvailable()) {
                String response = Network.getAvailableString();
                System.out.println(response);
                char[] commands = response.toCharArray();

                for (int i = 0; i < commands.length; i++) {
                    if (commands[i] == '#') {
                        char firstChar = currentCommand.charAt(0);
                        switch (firstChar) {
                            case 'P':
                                System.out.println("Reset reconnect timer");
                                Network.resetReconnectTimer(millis());
                                break;
                            case 'D':
                                watchActive = true;
                                int day = Integer.parseInt(currentCommand.substring(1));
                                setCenterText(day);
                                break;
                            case 'H':
                                watchActive = false;

                        }
                        currentCommand = "";
                        continue;
                    }
                    currentCommand += commands[i];
                }
            }

            Network.checkTCPConnection(millis(), this);
            Network.pingServer(millis());
        }

        if (watchActive) {
            drawDayLines(30);
            drawSpendingGraphic();
            drawCenter();
            drawCenterText();
        }
    }

    public void createSpendingGraphic() {
        int totalSpending = finData.getTotalSpending();
        int[] graphicColor = Graphics.getSpendingGraphicColor(Math.abs(totalSpending), finData.getDisposableIncome());
        fill(graphicColor[0], graphicColor[1], graphicColor[2], 128);
        stroke(255, 255, 255);
        strokeWeight(1.5f);
        spendingLines = createShape();
        spendingLines = Graphics.createSpendingGraphic(finData, spendingLines);
    }

    private void drawCenterText() {

        // If nothing is inspected do not draw anything
        if (dayOfMonth == 0) return;

        // Set text options
        textSize(12 * SCALE);
        fill(255, 255, 255);
        textAlign(CENTER);

        if (displayDayMsg) {
            // Fade out effect
            textTransparency += fade;

            if (textTransparency <= 0) {
                displayDayMsg = false;
                fade = FADING_SPEED;
            }

            fill(255, textTransparency);
            text("Day " + dayOfMonth, centerPositionX, centerPositionY);
            return;
        }

        if (finDataTextList.isEmpty()) {
            fadeInText("No\nTransactions");
        } else if (finDataTextList.size() == 1) {
            fadeInText(finDataTextList.get(0));
        } else {
            // Fade in/out effect
            textTransparency += fade;

            if (textTransparency <= 0 || textTransparency >= 255) {
                fade = -fade;
            }

            if (textTransparency <= 0) {
                transactionNumber++;
                transactionNumber = transactionNumber % finDataTextList.size();
            }

            fill(255, textTransparency);
            text(finDataTextList.get(transactionNumber), centerPositionX, centerPositionY);
        }
    }

    private void fadeInText(String text) {
        textTransparency += fade;
        fill(255, textTransparency);
        text(text, centerPositionX, centerPositionY);
    }

    private void setCenterText(int dayOfMonth) {
        // Store day of month. Used when drawing center text
        this.dayOfMonth = dayOfMonth;

        // Loop through financial data and store text and amount
        List<FinancialData.DataEntry> finDataList = finData.getFinancialDataList();
        finDataTextList.clear();

        for (FinancialData.DataEntry entry : finDataList) {
            if (Integer.parseInt(entry.getDate().split("\\.")[0]) == dayOfMonth) {
                String text = StringUtils.substring(entry.getText(), 0, 10);
                finDataTextList.add(text + "\n" + entry.getAmount());
            }
        }

        // Reset displayDayMsg and textTransparency when text in center is updated. Allows quick updating without bugs.
        displayDayMsg = true;
        fade = -FADING_SPEED;
        textTransparency = 255;
        transactionNumber = 0;
    }

    private void drawCenter() {
        stroke(255);
        strokeWeight(2f);
        fill(0, 0, 0);
        pushMatrix();
        translate(centerPositionX, centerPositionY);
        scale(SCALE);
        ellipse(0, 0, CLOCK_CENTER_DIAMETER, CLOCK_CENTER_DIAMETER);
        popMatrix();
    }

    private void drawDayLines(int dayOfMonth) {
        for (int i = 0; i < dayOfMonth; i++) {
            pushMatrix();
            translate(centerPositionX, centerPositionY);
            rotate(DAY_ROTATION * i);
            scale(SCALE);
            shape(dayLine);
            popMatrix();
        }
    }

    private void drawSpendingGraphic() {
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