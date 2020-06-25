import java.io.*;
import java.util.Iterator;

public class LogsManagement {

    private String logDirectory;
    private static final String LOG_COUNT_FILENAME = "log_count.txt";
    private int currentLogCount;

    private LogsManagement(int logCount, String logDirectory) {
        currentLogCount = logCount;
        this.logDirectory = logDirectory;
    }

    public static LogsManagement init(String logDirectory) {
        try {
            return new LogsManagement(Integer.parseInt(new BufferedReader(
                    new FileReader(logDirectory + LOG_COUNT_FILENAME))
                        .readLine()),
                    logDirectory);
        } catch (IOException e) {
            try {
                BufferedWriter writer = new BufferedWriter(
                        new FileWriter(logDirectory + LOG_COUNT_FILENAME, true));
                writer.append("1");
                writer.close();
                return new LogsManagement(1, logDirectory);

            } catch (IOException e2) {
                System.err.println(e2);
            }
            System.err.println(e);
        }

        return null;

    }

    private void updateCount() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    logDirectory + LOG_COUNT_FILENAME));
            writer.write(++currentLogCount + "");

            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }


    public void updateLogs(GameLogger logger) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    logDirectory + currentLogCount + ".txt",
                    true));
            writer.append(logger.toString());

            writer.append("\n\nUpdates: ");
            Iterator<GameUpdate> updates = logger.getUpdateHistory();
            while (updates.hasNext()) {
                writer.append("\n" + updates.next().toString());
            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }
        updateCount();
        return;
    }


}
