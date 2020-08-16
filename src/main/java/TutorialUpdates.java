import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

public class TutorialUpdates {

    private final static String[][] phaseMessages;

    static {
        String rootDirectory = System.getProperty("user.dir");
        File file = new File(rootDirectory + "/tutorial_texts");
        File[] files = file.listFiles();
        String[] fileNames = file.list();

        int phaseCount = Arrays.stream(fileNames).reduce(
                0,
                (x, y) -> x > Character.getNumericValue(y.charAt(0)) ? x : Character.getNumericValue(y.charAt(0)),
                (x, y) -> x > y ? x : y);

        phaseMessages = new String[phaseCount][];

        for (int phase = 1; phase <= phaseCount; phase++) {
            int count = 0;
            for (String fileName : fileNames) {
                if (Character.getNumericValue(fileName.charAt(0)) == phase) {
                    count++;
                }
            }
            phaseMessages[phase - 1] = new String[count];
        }

        for (int i = 0; i < fileNames.length; i++) {
            String fileName = fileNames[i];
            int phaseNumber = Character.getNumericValue(fileName.charAt(0));
            int messageNumber = Character.getNumericValue(fileName.charAt(1));
            try {
                BufferedReader reader = new BufferedReader(new FileReader(files[i]));
                phaseMessages[phaseNumber - 1][messageNumber - 1] = reader.lines()
                        .reduce((x, y) -> x + "\n" + y)
                        .get();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        //Arrays.stream(phaseMessages).forEach(x -> System.out.println(Arrays.toString(x)));

    }

    //Phase starts at 1, messageNumber starts at 0
    protected static String getPhaseMessage(int phase, int messageNumber) {
        return phaseMessages[phase - 1][messageNumber];
    }

    protected static IndexUpdate createTutorialUpdate(int player, int phase, int message) {
        return new IndexUpdate(player, getPhaseMessage(phase, message), UpdateType.TUTORIAL);
    }

    protected static IndexUpdate createSkipTutorialUpdate(int player) {
        return new IndexUpdate(player, "Skipped!", UpdateType.TUTORIAL);
    }



    public static void main(String[] args) {

    }






}
