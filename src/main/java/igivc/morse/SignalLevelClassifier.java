package igivc.morse;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

class SignalLevelClassifier implements Consumer<Double> {

    private final int nSamples;
    private final Consumer<SignalLevel> signalLevelConsumer;
    private final Double[] arrWindow;
    private final List<Double> window = new LinkedList<>();
    private double toneOn, toneOff, tone, noise;
    private SignalLevel currentLevel = SignalLevel.Low;

    public SignalLevelClassifier(Consumer<SignalLevel> signalLevelConsumer, int sampleRate, double windowInterval) {
        this.signalLevelConsumer = signalLevelConsumer;
        nSamples = (int) (sampleRate * windowInterval);
        arrWindow = new Double[nSamples];
    }

    public SignalLevelClassifier(Consumer<SignalLevel> signalLevelConsumer, int sampleRate) {
        this(signalLevelConsumer, sampleRate, 0.1 /* seconds */);
    }

    @Override
    public void accept(Double x) {
        window.addLast(x);
        if (window.size() == nSamples) {
            window.toArray(arrWindow);
            Arrays.sort(arrWindow);
            noise = percentile(0.1); // P10
            tone = percentile(0.9); // P90
            if (tone / noise > 4) {
                boolean firstUpdate = toneOn == 0;
                // initialize thresholds
                calculateThresholds();
                if (firstUpdate) {
                    // send detected levels
                    for (Double sample : window) {
                        acceptSignalLevel(sample);
                    }
                    window.removeFirst();
                    return;
                }
            }
            acceptSignalLevel(x);
            window.removeFirst();
        }
    }

    private void calculateThresholds() {
        toneOn = noise + 0.80 * (tone - noise);
        toneOff = noise + 0.30 * (tone - noise);
    }

    private double percentile(double p) {
        int k = (int) Math.floor(p * (arrWindow.length - 1));
        return arrWindow[k];
    }

    private void acceptSignalLevel(Double sample) {
        if (sample > toneOn) {
            currentLevel = SignalLevel.High;
        } else if (sample < toneOff) {
            currentLevel = SignalLevel.Low;
        }
        signalLevelConsumer.accept(currentLevel);

    }
}
