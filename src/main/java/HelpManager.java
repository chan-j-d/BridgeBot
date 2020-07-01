import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HelpManager {

    private HashMap<String, String> textMap;
    private HashMap<String, String> descriptions;
    private String defaultHelpText;
    private String processedString;

    private HelpManager() {
        textMap = new HashMap<>();
        descriptions = new HashMap<>();
        processedString = "Not processed!";
    }

    private void setText(String commandName, String text) {
        textMap.put(commandName, text);
    }

    private void setDescription(String commandName, String description) {
        descriptions.put(commandName, description);
    }

    private void setHelpText(String text) {
        this.defaultHelpText = text;
    }

    public static HelpManager init() {
        String rootDirectory = System.getProperty("user.dir");
        File file = new File(rootDirectory + "\\help_texts");
        File[] files = file.listFiles();
        String[] fileNames = file.list();

        HelpManager helpManager = new HelpManager();
        String commandName;
        String text;
        for (int i = 0; i < files.length; i++) {
            commandName = fileNames[i].split("\\.")[0];

            try {
                BufferedReader reader = new BufferedReader(new FileReader(files[i]));

                if (commandName.equals("help")) {
                    StringBuilder helpText = new StringBuilder();
                    String line = reader.readLine();
                    while (line != null) {
                        helpText.append(line);
                        line = reader.readLine();
                    }
                    helpManager.setHelpText(helpText.toString());
                } else {
                    String description = reader.readLine();
                    StringBuilder remainingText = new StringBuilder();
                    String line = reader.readLine();
                    while (line != null) {
                        remainingText.append("\n" + line);
                        line = reader.readLine();
                    }

                    helpManager.setDescription(commandName, description);
                    helpManager.setText(commandName, remainingText.toString());

                }

            } catch (IOException e) {
                System.err.println(e);
            }
        }
        helpManager.processHelpString();

        return helpManager;
    }

    private void processHelpString() {
        StringBuilder tempString = new StringBuilder();
        tempString.append(defaultHelpText);
        tempString.append("\n");
        for (Map.Entry<String, String> entries : descriptions.entrySet()) {
            tempString.append("\n/" + entries.getKey() + " - " + entries.getValue());
        }
        processedString = tempString.toString();

    }

    public boolean validHelpCommmand(String command) {
        return descriptions.containsKey(command);
    }

    public String getHelpText(String command) {
        return textMap.get(command);
    }

    public String getDefaultText() {
        return this.toString();
    }

    @Override
    public String toString() {
        return processedString;
    }

    public static void main(String[] args) {
        init();
    }

}
