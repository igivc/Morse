package igivc.morse;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class DotDurationMeter implements Consumer<SignalState> {
    private final List<SignalState> statesForDetection = new LinkedList<>();
    private final BiConsumer<SignalState, Integer> dashDotClassifier;
    private int dotDurationInSamples;

    public DotDurationMeter(BiConsumer<SignalState, Integer> dashDotClassifier) {
        this.dashDotClassifier = dashDotClassifier;
    }

    @Override
    public void accept(SignalState signalState) {
        final int detectorQueueLength = 20;
        statesForDetection.addLast(signalState);
        if (statesForDetection.size() >= detectorQueueLength) {
            var sortedHighStates = statesForDetection
                    .stream()
                    .filter(s -> s.signalLevel == SignalLevel.High)
                    .sorted(Comparator.comparing(state -> state.durationInSamples))
                    .map(state -> state.durationInSamples)
                    .toArray();
            int dashIndex = sortedHighStates.length - 2;
            int dotIndex = 1;
            int dashDuration = (int) sortedHighStates[dashIndex];
            int dotDuration = (int) sortedHighStates[dotIndex];
            float dashDotRatio = (float) dashDuration / dotDuration;

            if (dashDotRatio > 2.5 && dashDotRatio < 3.5) { // found dashes and dots?
                boolean firstUpdate = dotDurationInSamples == 0;
                dotDurationInSamples = dotDuration;
                if (firstUpdate) {
                    for (var ss : statesForDetection) {
                        dashDotClassifier.accept(ss, dotDurationInSamples);
                    }
                }
                while (statesForDetection.size() >= detectorQueueLength) {
                    statesForDetection.removeFirst();
                }
                if (firstUpdate) return;
            }
        }

        if (dotDurationInSamples > 0) {
            dashDotClassifier.accept(signalState, dotDurationInSamples);
        }
    }
}
