import java.util.ArrayList;
import java.util.List;

public class FinancialData {

    private int disposableIncome;
    private List<DataEntry> financialData = new ArrayList<>();

    public FinancialData(int disposableIncome, String csvData) {

        this.disposableIncome = disposableIncome;
        loadCSVData(csvData);
    }

    public void loadCSVData(String csvData) {
        String[] lines = csvData.split("\n");
        for (String line : lines) {
            String[] entries = line.split(",\"");
            for (int i = 0; i < entries.length; i++) {
                entries[i] = entries[i].replaceAll("\"", "");
            }
            float amount = Float.parseFloat(entries[2].replaceAll("\\.", "").replace(',', '.'));
            if (amount <= 0.0) {
                String date = entries[0];
                String text = entries[1];
                financialData.add(new DataEntry(date, text, amount));
            }
        }
    }

    public int getDisposableIncome() {
        return disposableIncome;
    }

    public List<DataEntry> getFinancialDataList() {
        return financialData;
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
