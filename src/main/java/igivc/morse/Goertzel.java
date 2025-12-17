package igivc.morse;

import java.util.function.Consumer;

class Goertzel {
    private final int windowWidth, shift;
    private final Consumer<Goertzel> resultConsumer;
    private final double[] ringBuffer; // see https://www.baeldung.com/java-ring-buffer
    private final double[] windowFunction;
    private final double coeff;
    private int head = 0, tail = 0, size = 0;
    private double magnitudeSquared;
    private long signalPosition = 0;

    Goertzel(int k, int windowWidth, int shift, boolean useBlackmanHarrisWindow, Consumer<Goertzel> resultConsumer) {
        this.resultConsumer = resultConsumer;
        this.windowWidth = windowWidth;
        this.shift = shift;
        this.ringBuffer = new double[windowWidth];
        this.windowFunction = new double[windowWidth];
        double omega = 2.0 * Math.PI * k / windowWidth;
        double cosine = Math.cos(omega);
        coeff = 2 * cosine;
        // prepare window, see https://en.wikipedia.org/wiki/Window_function
        for (int i = 0; i < windowWidth; i++) {
            windowFunction[i] = useBlackmanHarrisWindow ? blackmanHarris(i) : 1.0;
        }
    }

    void process(double sample) {
        offer(sample);
        if (size == windowWidth) {
            double q1 = 0, q2 = 0;
            for (int i = 0; i < windowWidth; i++) {
                double d = poll();
                offer(d);
                double w = windowFunction[i];
                double q0 = coeff * q1 - q2 + d * w;
                q2 = q1;
                q1 = q0;
            }
            // shift data and free `shift` elements in a ring buffer
            head = (head + shift) % windowWidth;
            size -= shift;
            // calc magnitude^2
            magnitudeSquared = q1 * q1 + q2 * q2 - q1 * q2 * coeff;
            resultConsumer.accept(this);
            signalPosition += windowWidth;
        }
    }

    void process(short[] samples) {
        for(var sample : samples) {
            process(sample);
        }
    }

    double getMagnitude() {
        return Math.sqrt(magnitudeSquared);
    }

    double getMagnitudeSquared() {
        return magnitudeSquared;
    }

    long getSignalPosition() {
        return signalPosition;
    }

    private double blackmanHarris(int index) {
        final double a0 = 0.35875, a1 = 0.48829, a2 = 0.14128, a3 = 0.01168;
        final double p = 2.0 * Math.PI * index / windowWidth;
        return a0
                - a1 * Math.cos(p)
                + a2 * Math.cos(2 * p)
                - a3 * Math.cos(3 * p);
    }

    private void offer(double sample) {
        if (size == windowWidth) {
            throw new IndexOutOfBoundsException("Overfilled");
        }
        ringBuffer[tail] = sample;
        tail = (tail + 1) % windowWidth;
        size++;
    }

    private double poll() {
        if (size == 0) {
            throw new IndexOutOfBoundsException("Underfilled");
        }
        double v = ringBuffer[head];
        head = (head + 1) % windowWidth;
        size--;
        return v;
    }
}
