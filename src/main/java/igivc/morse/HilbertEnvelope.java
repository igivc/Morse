package igivc.morse;

/**
 * The Hilbert transform is a mathematical operation that allows one to obtain
 * an analytical signal from a real audio signal.
 * In the context of Morse tone decoding, it is used to calculate the envelope -
 * a smooth line indicating when the tone is on and off.
 */
class HilbertEnvelope {

    private final double[] real;
    private final double[] imag;
    private final double[] src;
    private final double[] dst;
    private final double[] hilbertMultiplier;

    public HilbertEnvelope(double[] src, double[] dst) {
        if (src.length != dst.length) throw new IllegalArgumentException("src.length != dst.length");
        if (Integer.bitCount(src.length) != 1) throw new IllegalArgumentException("src length must be a power of 2");
        this.src = src;
        this.dst = dst;
        this.real = new double[src.length];
        this.imag = new double[src.length];
        hilbertMultiplier = buildHilbertMultiplier(src.length);
    }

    /**
     * Calculates the signal envelope using the formula: abs(hilbert(signal)).
     */
    public void envelope() {
        final int n = src.length;
        // 1) copy to real part
        System.arraycopy(src, 0, real, 0, n);
        for (int i = 0; i < n; i++) imag[i] = 0.0;

        // 2) FFT
        FFT.fft(real, imag, false);

        // 3) apply Hilbert multiplier
        for (int i = 0; i < n; i++) {
            real[i] *= hilbertMultiplier[i];
            imag[i] *= hilbertMultiplier[i];
        }

        // 4) IFFT
        FFT.fft(real, imag, true);

        // 5) abs -> dst
        for (int i = 0; i < n; i++) {
            dst[i] = Math.hypot(real[i], imag[i]);
        }
    }

    private static double[] buildHilbertMultiplier(int n) {
        double[] H = new double[n];
        if ((n & 1) == 0) {
            H[0] = 1.0;
            H[n / 2] = 1.0;
            for (int k = 1; k < n / 2; k++) H[k] = 2.0;
        } else {
            H[0] = 1.0;
            for (int k = 1; k <= (n - 1) / 2; k++) H[k] = 2.0;
        }
        return H;
    }
}
