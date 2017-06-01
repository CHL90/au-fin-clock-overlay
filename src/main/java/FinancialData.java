import java.util.ArrayList;
import java.util.List;

public class FinancialData {

    private final long UPDATE_DATA_INTERVAL = 30000;
    private int disposableIncome;
    private List<DataEntry> financialData = new ArrayList<>();
    private long updateDataTimer = System.currentTimeMillis();
    private String SEPARATOR = ",";

    public FinancialData(int disposableIncome, String csvData) {

        this.disposableIncome = disposableIncome;
        loadCSVData(csvData);
    }

    public void loadCSVData(String csvData) {
        String[] lines = csvData.split("\n");
        for (String line : lines) {
            //System.out.println(line);
            String[] entries = line.split(SEPARATOR);
            for (int i = 0; i < entries.length; i++) {
                entries[i] = entries[i].replaceAll("\"", "");
            }
            float amount = Float.parseFloat(entries[3].replaceAll("\\.", "").replace(',', '.'));
            if (amount <= 0.0) {
                String date = entries[0];
                date = date.replaceAll("/", "\\.");
                String text = entries[1];
                System.out.print(date + " ");
                System.out.print(text + " ");
                System.out.println(amount);
                financialData.add(new DataEntry(date, text, amount));
            }
        }
    }

    public void updateCSVData(FinancialProjection projection, String csvName) {
        if (System.currentTimeMillis() - updateDataTimer > UPDATE_DATA_INTERVAL) {
            updateDataTimer = System.currentTimeMillis(); // Reset timer
            String csv = Network.getCSVData(csvName);
            financialData.clear();
            loadCSVData(csv);
            projection.createSpendingGraphic();
        }
    }

    public int getDisposableIncome() {
        return disposableIncome;
    }

    public List<DataEntry> getFinancialDataList() {
        return financialData;
    }

    public int getTotalSpending() {
        int total = 0;
        for (DataEntry entry : financialData) {
            total += entry.getAmount();
        }
        return total;
    }

    public class DataEntry {
        private String date;
        private String text;
        private float amount;

        DataEntry(String date, String text, float amount) {
            this.date = date;
            this.text = text;
            this.amount = amount;
        }

        public String getDate() {
            return date;
        }

        public String getText() {
            return text;
        }

        public float getAmount() {
            return amount;
        }
    }

}
