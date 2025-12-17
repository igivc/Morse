package igivc.morse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

class DecoderTest {
    @Test
    void test1() {
        final int sampleRate = 22050;
        final int windowWidthMilliseconds = 80;
        final int freq = 800;
        final int k = windowWidthMilliseconds * freq / 1000;
        final int windowWidthSamples = sampleRate * windowWidthMilliseconds / 1000;
        final int shift = windowWidthSamples /16;
        try (java.io.PrintWriter writer = new java.io.PrintWriter("output.txt")) {
            Goertzel goertzel = new Goertzel(k, windowWidthSamples, shift, true, (Goertzel g)->{
                double magnitude = g.getMagnitude();
                long pos = g.getSignalPosition();
                try {
                    writer.println(pos + "\t" + Math.round(1000000*magnitude));
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            Path filePath = Paths.get("signal.txt"); // Replace with your file path
            try (Stream<String> lines = Files.lines(filePath)) {
                lines.forEach(line->goertzel.process(Double.parseDouble(line)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
