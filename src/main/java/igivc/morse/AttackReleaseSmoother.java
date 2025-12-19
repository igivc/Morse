package igivc.morse;

import java.util.function.Function;

/**
 * One-pole IIR low-pass (EMA, Exponential Moving Average) filters:
 * y[n]=y[n−1]+alphaAttack(x[n]−y[n−1]) if x>y
 * y[n]=y[n−1]+alphaRelease(x[n]−y[n−1]) if x<=y
 */
class AttackReleaseSmoother implements Function<Double, Double> {
    private final double alphaAttack;
    private final double alphaRelease;
    private double y = 0.0;
    private boolean initialized = false;

    public AttackReleaseSmoother(int sampleRate, double attackSeconds, double releaseSeconds) {
        if (sampleRate <= 0) throw new IllegalArgumentException("fs");
        if (attackSeconds <= 0 || releaseSeconds <= 0) throw new IllegalArgumentException();

        this.alphaAttack = 1.0 - Math.exp(-1.0 / (sampleRate * attackSeconds));
        this.alphaRelease = 1.0 - Math.exp(-1.0 / (sampleRate * releaseSeconds));
    }

    public AttackReleaseSmoother(int sampleRate) {
        this(sampleRate, 0.01, 0.02);
    }

    public void reset() {
        y = 0.0;
        initialized = false;
    }

    @Override
    public Double apply(Double x) {
        if (!initialized) {
            y = x;
            initialized = true;
            return y;
        }
        double a = (x > y) ? alphaAttack : alphaRelease;
        y += a * (x - y);
        return y;
    }
}
