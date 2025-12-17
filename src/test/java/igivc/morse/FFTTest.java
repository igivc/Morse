package igivc.morse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FFTTest {
    private static final double EPS = 1e-9;

    /**
     * 1. Test "inverse FFT restores the original signal."
     * FFT -> IFFT should yield almost the same real/imag.
     */
    @Test
    public void testFftInverseIdentity() {
        int n = 1024; // степень двойки
        double[] real = new double[n];
        double[] imag = new double[n];

        // fill with a random signal
        java.util.Random rnd = new java.util.Random(12345);
        for (int i = 0; i < n; i++) {
            real[i] = rnd.nextDouble() * 2 - 1; // [-1, 1]
            imag[i] = rnd.nextDouble() * 2 - 1;
        }

        // make copies
        double[] realOrig = real.clone();
        double[] imagOrig = imag.clone();

        // forward + inverse FFT
        FFT.fft(real, imag, false); // forward
        FFT.fft(real, imag, true);  // inverse

        // check against the source
        for (int i = 0; i < n; i++) {
            assertEquals(realOrig[i], real[i], EPS, "real[" + i + "]");
            assertEquals(imagOrig[i], imag[i], EPS, "imag[" + i + "]");
        }
    }

    /**
     * 2. Test: Delta pulse -> flat spectrum (all values are approximately the same).
     */
    @Test
    public void testImpulseResponse() {
        int n = 128;
        double[] real = new double[n];
        double[] imag = new double[n];

        // дельта: x[0] = 1, остальные 0
        real[0] = 1.0;

        FFT.fft(real, imag, false);

        for (int k = 0; k < n; k++) {
            // для идеального FFT(x) импульса real[k] = 1, imag[k] = 0
            assertEquals(1.0, real[k], EPS, "real[" + k + "]");
            assertEquals(0.0, imag[k], EPS, "imag[" + k + "]");
        }
    }

    /**
     * 3. Test: sine -> maximum energy in one frequency bin (+ its reflected value).
     */
    @Test
    public void testSineFrequencyBin() {
        int n = 256;
        double sampleRate = 8000.0;
        int targetBin = 10; // bin number
        double freq = targetBin * sampleRate / n; // frequency that falls exactly into the bin

        double[] real = new double[n];
        double[] imag = new double[n];

        for (int i = 0; i < n; i++) {
            double t = i / sampleRate;
            real[i] = Math.sin(2 * Math.PI * freq * t);
        }

        FFT.fft(real, imag, false);

        // evaluate the spectrum by modulus
        double[] mag = new double[n];
        for (int k = 0; k < n; k++) {
            mag[k] = Math.hypot(real[k], imag[k]);
        }

        double signalMag = mag[targetBin] + mag[n - targetBin]; // sum of mag and its "reflected" value
        double totalMag = 0;
        // sum the magnitudes
        for (int k = 1; k < n; k++) {
            totalMag += mag[k];
        }

        assertEquals(n, signalMag, EPS, "(1) peak bin must match target frequency");
        assertEquals(0, totalMag - signalMag, EPS, "(2) peak bin must match target frequency");
    }
}
