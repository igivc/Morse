package igivc.morse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class DashDotClassifier {
    private enum Duration {Short, Middle, Long, Large}
    private final Consumer<String> consumer;

    private final Map<SignalLevel, Map<Duration, String>> symbols;

    public DashDotClassifier(Consumer<String> consumer) {
        this.consumer = consumer;
        symbols = new HashMap<>();
        symbols.put(SignalLevel.High, new HashMap<>());
        symbols.get(SignalLevel.High).put(Duration.Short, ".");
        symbols.get(SignalLevel.High).put(Duration.Middle, "-");
        symbols.get(SignalLevel.High).put(Duration.Long, "?");
        symbols.get(SignalLevel.High).put(Duration.Large, "?");
        symbols.put(SignalLevel.Low, new HashMap<>());
        symbols.get(SignalLevel.Low).put(Duration.Short, "");
        symbols.get(SignalLevel.Low).put(Duration.Middle, "|");
        symbols.get(SignalLevel.Low).put(Duration.Long, " ");
        symbols.get(SignalLevel.Low).put(Duration.Large, "#");
    }

    public void process(SignalState ss, int dotDurationInSamples) {
        float ratio = (float) ss.durationInSamples / dotDurationInSamples;
        if (ratio > 0.5 && ratio < 1.5) {
            accept(ss.signalLevel, Duration.Short);
        } else if (ratio > 2.5 && ratio < 3.5) {
            accept(ss.signalLevel, Duration.Middle);
        } else if (ratio > 5 && ratio < 10) {
            accept(ss.signalLevel, Duration.Long);
        } else {
            accept(ss.signalLevel, Duration.Large);
        }
    }

    private void accept(SignalLevel signalLevel, Duration duration) {
        consumer.accept(symbols.get(signalLevel).get(duration));
    }
}
