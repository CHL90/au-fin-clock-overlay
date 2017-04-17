import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import processing.core.PApplet;
import processing.core.PShape;

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
    private float CLOCK_RADIUS = 150;
    private float CLOCK_CENTER_RADIUS = 25;
    private float DAY_ROTATION = TWO_PI/30;

    private static FinancialData finData;

    // Text and animation
    private List<String> finDataTextList;
    private int textTransparency = 1;
    private int fadingSpeed = 5; // Fading speed
    private int transactionNumber = 0; // Transaction being inspected
    private int dayOfMonth;
    private boolean displayDayMsg = true;

    // Custom shapes
    private PShape dayLine;
    private PShape spendingLines;

    public static void main(String[] args) {
        if(args.length > 0) {

            int disposableIncome = Integer.parseInt(args[0]);
            String csv = "";

            try {
                csv = Unirest.get("http://198.211.106.128:1337/csv/" + args[1] + ".csv")
                        .header("Accept", "text/csv")
                        .asString().getBody();
            } catch (UnirestException e) {
                e.printStackTrace();
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
        // Create custom line that represents one day
        stroke(128);
        strokeWeight(0.5f);
        dayLine = createShape();
        dayLine.beginShape();
        dayLine.vertex(0, 0);
        dayLine.vertex(0, -CLOCK_RADIUS);
        dayLine.endShape();

        // Spending lines graphic
        fill(255, 255, 255);
        spendingLines = createSpendingGraphic(finData);

        /* This method should be called by the clock to update the text displayed in the center. Default 0 to display
           nothing on start up. */
        setCenterText(11);
    }

    public void draw() {
        background(0);
        drawDayLines(30);
        drawSpendingGraphic(spendingLines);
        drawCenter();
        drawCenterText();
    }

    private void drawCenterText() {

        // If nothing is inspected do not draw anything
        if (dayOfMonth == 0) return;

        // Set text options
        textSize(6 * SCALE);
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
        stroke(128);
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

    private void drawSpendingGraphic(PShape spendingGraphic) {
        pushMatrix();
        translate(centerPositionX, centerPositionY);
        scale(SCALE);
        shape(spendingGraphic);
        popMatrix();
    }

    private PShape createSpendingGraphic(FinancialData financialData) {

        int disposableIncome = financialData.getDisposableIncome();
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        List<FinancialData.DataEntry> finDataList = financialData.getFinancialDataList();

        // Initialize points list with center point
        ArrayList<Point> points = new ArrayList<>();
        points.add(new Point(0, 0));
        // Keep track of spending position. Center radius equals no spending while clock radius equals entire disposable income spent
        float spendingPosition = -CLOCK_CENTER_RADIUS;

        for (int i = 1; i <= dayOfMonth + 1; i++) {
            float dailySpending = 0;

            // Calculate amount spent on a given day
            for (FinancialData.DataEntry entry : finDataList) {
                if (Integer.parseInt(entry.getDate().split("\\.")[0]) == i) {
                    dailySpending += entry.getAmount();
                }
            }

            // Add points in relation to spending. When there is no daily spending just rotate current point.
            // When there is daily spending rotate current point and add another point further out according to spending
            if (dailySpending == 0) {
                Point currentSP = rotate2d(new Point(0, spendingPosition), (i - 1) * DAY_ROTATION);
                points.add(currentSP);
            } else {
                Point currentSP = rotate2d(new Point(0, spendingPosition), (i - 1) * DAY_ROTATION);
                spendingPosition += dailySpending / disposableIncome * (CLOCK_RADIUS - CLOCK_CENTER_RADIUS);
                Point newSP = rotate2d(new Point(0, spendingPosition), (i - 1) * DAY_ROTATION);
                points.add(currentSP);
                points.add(newSP);
            }
        }

        PShape spendingLine = createShape();
        spendingLine.beginShape();
        for (Point point : points) {
            spendingLine.vertex(point.x, point.y);
        }
        spendingLine.endShape();

        return spendingLine;
    }

    // Rotate a point around center (0,0)
    private Point rotate2d(Point p, double rotation) {
        float newX = (float) (Math.cos(rotation) * p.x - Math.sin(rotation) * p.y);
        float newY = (float) (Math.sin(rotation) * p.x + Math.cos(rotation) * p.y);

        newX = (float) (Math.round(newX * 100.0) / 100.0);
        newY = (float) (Math.round(newY * 100.0) / 100.0);

        return new Point(newX, newY);
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

    private class Point {
        public float x;
        public float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
