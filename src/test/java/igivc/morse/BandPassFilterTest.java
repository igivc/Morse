package igivc.morse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BandPassFilterTest {

    private static final double EPS = 1e-2;

    @Test
    public void testCenterFrequencyPasses() {
        int fs = 8000;
        double fc = 700.0;
        double Q = 10.0;

        BandPassFilter bp = new BandPassFilter(fs, fc, Q);

        int n = fs; // 1 second
        double[] x = new double[n];

        for (int i = 0; i < n; i++) {
            double t = i / (double) fs;
            x[i] = Math.sin(2 * Math.PI * fc * t);
        }

        bp.processBuffer(x);

        // Пропускаем начальный переходный процесс
        int start = fs / 10;

        double rms = 0.0;
        int count = 0;
        for (int i = start; i < n; i++) {
            rms += x[i] * x[i];
            count++;
        }
        rms = Math.sqrt(rms / count);

        // RMS должен быть заметно больше нуля
        assertTrue(rms > 0.3, "Center frequency should pass through band-pass");
    }

    @Test
    public void testLowFrequencyRejected() {
        int fs = 8000;
        double fc = 700.0;
        double Q = 10.0;

        BandPassFilter bp = new BandPassFilter(fs, fc, Q);

        double lowFreq = 100.0;
        int n = fs;
        double[] x = new double[n];

        for (int i = 0; i < n; i++) {
            double t = i / (double) fs;
            x[i] = Math.sin(2 * Math.PI * lowFreq * t);
        }

        bp.processBuffer(x);

        int start = fs / 10;
        double rms = 0.0;
        int count = 0;

        for (int i = start; i < n; i++) {
            rms += x[i] * x[i];
            count++;
        }
        rms = Math.sqrt(rms / count);

        assertTrue(rms < 0.1, "Low frequency should be attenuated");
    }

    @Test
    public void testHighFrequencyRejected() {
        int fs = 8000;
        double fc = 700.0;
        double Q = 10.0;

        BandPassFilter bp = new BandPassFilter(fs, fc, Q);

        double highFreq = 3000.0;
        int n = fs;
        double[] x = new double[n];

        for (int i = 0; i < n; i++) {
            double t = i / (double) fs;
            x[i] = Math.sin(2 * Math.PI * highFreq * t);
        }

        bp.processBuffer(x);

        int start = fs / 10;
        double rms = 0.0;
        int count = 0;

        for (int i = start; i < n; i++) {
            rms += x[i] * x[i];
            count++;
        }
        rms = Math.sqrt(rms / count);

        assertTrue(rms < 0.1, "High frequency should be attenuated");
    }

    @Test
    public void testZeroSignal() {
        int fs = 8000;
        double fc = 700.0;
        double Q = 10.0;

        BandPassFilter bp = new BandPassFilter(fs, fc, Q);

        double[] x = new double[1000];
        bp.processBuffer(x);

        for (double v : x) {
            assertEquals(0.0, v, 1e-12);
        }
    }
}
