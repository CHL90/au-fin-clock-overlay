import processing.core.PApplet;
import processing.core.PShape;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Graphics extends PApplet {

    private static float CLOCK_RADIUS = FinancialProjection.CLOCK_RADIUS;
    private static float CLOCK_CENTER_RADIUS = FinancialProjection.CLOCK_CENTER_DIAMETER/2;
    private static float DAY_ROTATION = FinancialProjection.DAY_ROTATION;

    public static PShape createSpendingGraphic(FinancialData financialData, PShape graphics) {
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

        graphics.beginShape();
        for (Point point : points) {
            graphics.vertex(point.x, point.y);
        }
        graphics.endShape();

        return graphics;
    }

    public static PShape createDayLine(PShape dayLine) {
        dayLine.beginShape();
        dayLine.vertex(0, 0);
        dayLine.vertex(0, -CLOCK_RADIUS);
        dayLine.endShape();
        return dayLine;
    }

    public static int[] getSpendingGraphicColor(int amountSpent, int disposableIncome) {
        double idealSpending = ( (double) disposableIncome / 30) * LocalDateTime.now().getDayOfMonth();
        double veryLow = idealSpending * 0.75;
        double low = idealSpending * 0.90;
        double high = idealSpending * 1.1;
        double veryHigh = idealSpending * 1.25;

        int[] rgbArray = new int[]{255, 255, 255};

        if (amountSpent <= veryLow) rgbArray = new int[]{0, 255, 0};
        if (amountSpent < low && amountSpent > veryLow) rgbArray = new int[]{128, 255, 0};
        if (amountSpent >= low && amountSpent <= high) rgbArray = new int[]{255, 255, 0};
        if (amountSpent > high && amountSpent < veryHigh) rgbArray = new int[]{255, 128, 0};
        if (amountSpent >= veryHigh) rgbArray = new int[]{255, 0, 0};

        return rgbArray;
    }

    // Rotate a point around center (0,0)
    private static Point rotate2d(Point p, double rotation) {
        float newX = (float) (Math.cos(rotation) * p.x - Math.sin(rotation) * p.y);
        float newY = (float) (Math.sin(rotation) * p.x + Math.cos(rotation) * p.y);

        newX = (float) (Math.round(newX * 100.0) / 100.0);
        newY = (float) (Math.round(newY * 100.0) / 100.0);

        return new Point(newX, newY);
    }

    private static class Point {
        public float x;
        public float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

}
