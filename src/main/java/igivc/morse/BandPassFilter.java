package igivc.morse;

/**
 * IIR (Infinite Impulse Response) is a digital filter in which:
 * the output depends not only on the current input,
 * but also on previous outputs (feedback).
 * The equation for a second-order IIR filter is:
 * y[n] = b0*x[n] + b1*x[n - 1] + b2*x[n - 2] - a1*y[n - 1] - a2*y[n - 2];
 *
 * bi-quadratic filter:
 *
 * These coefficients are the standard from the
 * RBJ Audio EQ Cookbook (a classic in digital filtering).
 *
 * b0 = alpha
 * b1 = 0
 * b2 = -alpha
 * a0 = 1+alpha
 * a1 = -2*cos(omega)
 * a2 = 1-alpha
 *
 * where:
 * omega = 2*pi*Fc/Fs
 * Fc = central frequency
 * Fs = sample rate
 * Q = quality
 * alpha = sin(omega)/(2*Q)
 *
 * What it does:
 * 1. Cuts low frequencies
 * (below center)
 *
 * 2. Cuts high frequencies
 * (above center)
 *
 * 3. Boosts only a narrow range around a given frequency
 * At high Q, the filter is very narrow -> ideally emphasizes a single tone.
 *
 * BandwidthFreq = Fc/Q, (at -3db level)
 */
class BandPassFilter {
    private double b0, b1, b2, a1, a2;
    private double x1 = 0, x2 = 0;   // previous inputs
    private double y1 = 0, y2 = 0;   // previous outputs

    /**
     * Constructor
     * @see BandPassFilter#setParams(int, double, double)
     */
    public BandPassFilter(int sampleRate, double centerFreq, double Q) {
        setParams(sampleRate, centerFreq, Q);
    }

    /**
     * @param sampleRate sample rate, Hz (for example 22500)
     * @param centerFreq central frequency, Hz (for example 800)
     * @param Q quality factor (for example 5..20, more Q -> less bandwidth),
     *          optimal range Q = 7..15 for Morse tone signal.
     *          BandwidthFreq = centerFreq/Q, (at -3db level)
     */
    public final void setParams(int sampleRate, double centerFreq, double Q) {
        final double omega = 2.0 * Math.PI * centerFreq / sampleRate;
        final double sin = Math.sin(omega);
        final double cos = Math.cos(omega);
        final double alpha = sin / (2.0 * Q);

        // RBJ Audio EQ Cookbook: band-pass (constant skirt gain, peak gain = Q)
        final double b0 =  alpha;
        final double b1 =  0.0;
        final double b2 = -alpha;
        final double a0 =  1.0 + alpha;
        final double a1 = -2.0 * cos;
        final double a2 =  1.0 - alpha;

        // normalize by a0 so that a0 == 1
        this.b0 = b0 / a0;
        this.b1 = b1 / a0;
        this.b2 = b2 / a0;
        this.a1 = a1 / a0;
        this.a2 = a2 / a0;
    }

    /** Process one sample */
    public double processSample(double x0) {
        double y0 = b0 * x0 + b1 * x1 + b2 * x2
                - a1 * y1 - a2 * y2;

        // shift the history
        x2 = x1;
        x1 = x0;
        y2 = y1;
        y1 = y0;

        return y0;
    }

    /** Process samples in-place */
    public void processBuffer(double[] buffer) {
        // локальные копии коэффициентов
        final double b0 = this.b0, b1 = this.b1, b2 = this.b2;
        final double a1 = this.a1, a2 = this.a2;

        // локальные копии состояния
        double x1 = this.x1, x2 = this.x2;
        double y1 = this.y1, y2 = this.y2;

        for (int i = 0; i < buffer.length; i++) {
            double x0 = buffer[i];

            double y0 = b0 * x0 + b1 * x1 + b2 * x2
                    - a1 * y1 - a2 * y2;

            x2 = x1; x1 = x0;
            y2 = y1; y1 = y0;

            buffer[i] = y0;
        }

        // сохранить состояние обратно
        this.x1 = x1; this.x2 = x2;
        this.y1 = y1; this.y2 = y2;
    }

    /** Reset internal state (if you want) */
    public void reset() {
        x1 = x2 = y1 = y2 = 0.0;
    }
}
