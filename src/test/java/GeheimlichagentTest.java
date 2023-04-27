import geheimlichagent.Geheimlichagent;
import org.junit.jupiter.api.Test;

import java.io.*;

public class GeheimlichagentTest {

    @Test
    public void testAgents() {
        for (int i = 1; i <= 10; i++) {
            try {
                String filePath = ".\\src\\test\\resources\\";
                String filename = "output" + i + ".txt";
                ProcessBuilder pb = new ProcessBuilder(
                        "java", "-jar", ".\\libs\\sge-1.0.4-dq-exe.jar",
                        "match", "libs/HeimlichAndCo-1.0.0.jar",
                        "default_agents/HeimlichAndCoRandomAgent-1.0.jar",
                        "default_agents/HeimlichAndCoDepthSearchAgent-1.0.jar",
                        "-p", "2",  // 2 players
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
    }
}
