import org.junit.jupiter.api.Test;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GeheimlichagentTest {

    private static final int TEST_COUNT = 10;
    private DateFormat dateFormat = new SimpleDateFormat("dd_MM_yy_HH_mm");

    @Test
    public void testAgents() throws IOException {
        Date date = new Date();
        String filePath = ".\\src\\test\\resources\\";
        for (int i = 1; i <= TEST_COUNT; i++) {
            try {
                String filename = "output" + i + ".txt";
                ProcessBuilder pb = new ProcessBuilder(
                        "java", "-jar", ".\\libs\\sge-1.0.4-dq-exe.jar",
                        "match", "libs/HeimlichAndCo-1.0.0.jar",
                        "build/libs/Geheimlichagent-1.0.jar",
                        "default_agents/HeimlichAndCoRandomAgent-1.0.jar",
                        "default_agents/HeimlichAndCoMCTSAgent-1.0.jar",
                        "-p", "3",  // 2 players
                        "-b", "0",  // use default board
                        "-c", "30", // 30 seconds timeout
                        "-dq"       // timeout disqualifying enabled
                );
                pb.redirectOutput(new File(filePath + filename));
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Print results of every play-through in one file
        String resultFileName = "result-" + dateFormat.format(date) + ".txt";
        File resultFile = new File(filePath + resultFileName);
        resultFile.createNewFile();
        try (var fw = new FileWriter(resultFile, true)) {
            fw.write("Results of " + TEST_COUNT + " play-throughs:\n");
            for (int i = 1; i <= TEST_COUNT; i++) {
                String filename = "output" + i + ".txt";
                StringBuilder writeBlock = new StringBuilder();
                try (var fr = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(new File(filePath + filename))))) {
                    int linesToPrint = i == 1 ? 7 : 4;
                    for (int j = 0; j < linesToPrint; j++) {
                        String line = fr.readLine();
                        if (j > 1) {
                            writeBlock.insert(0, line + "\n");
                        }
                    }
                } catch (FileNotFoundException e) {
                    //skip
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                fw.write(writeBlock.toString() + "\n");
                new File(filePath + filename).delete();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
