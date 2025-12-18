package igivc.morse;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

class Receiver {
    private static final int sampleRate = 8000; // samples per second
    private static final double quality = 10;
    private static final double freq = 800; // Hz
    private static final double silenceLevel = 0.005; // 0..1
    private static final double flushDetectionBuffers = 1.5; // flush buffers when silence duration is more than ... seconds
    private static final double minDotDuration = 0.02; // seconds
    private static final double approxWindow = 0.2; // signal window in seconds
    private final SignalState[] savedSignalStates = new SignalState[2];
    private final List<SignalState> statesForDetection = new LinkedList<>();
    private SignalState signalState = null;
    private int savedSignalStatesIndex = -1;
    private int dotDurationInSamples;
    private final DashDotClassifier dashDotClassifier;

    public Receiver(Consumer<String> consumer) {
        dashDotClassifier = new DashDotClassifier(consumer);
    }


    /**
     * Receives Morse symbols
     */
    void receive() {
        signalState = new SignalState();
        savedSignalStates[0] = savedSignalStates[1] = null;
        savedSignalStatesIndex = -1;

        double window = approxWindow; //seconds

        int nWindowSamples = 1; // must be power of 2

        while (nWindowSamples < (int) (sampleRate * window)) {
            nWindowSamples *= 2; // power of 2
        }

        window = (double) nWindowSamples / sampleRate; // re-calculate window in seconds

        BandPassFilter filter = new BandPassFilter(sampleRate, freq, quality);
        SoundRecorder recorder = new SoundRecorder(sampleRate);
        recorder.start();
        RingBuffer ringBuffer = new RingBuffer(sampleRate * 2); // two seconds buffer is more than enough
        double[] samplesToProcess = new double[nWindowSamples];
        double[] processedSamples = new double[nWindowSamples];
        HilbertEnvelope hilbertEnvelope = new HilbertEnvelope(samplesToProcess, processedSamples);
        // overlapping
        final int shift = (int) (nWindowSamples * 0.9);
        final int leftOffset = (nWindowSamples - shift) / 2;
        final int rightOffset = nWindowSamples - leftOffset;
        //

        while (recorder.isRunning()) {
            double[] samples = recorder.get(); // get scaled sound data
            if (samples == null) break; // recorder is finished?
            filter.processBuffer(samples);
            ringBuffer.write(samples);
            while (ringBuffer.getSize() >= nWindowSamples) {
                ringBuffer.copyTo(samplesToProcess, 0, nWindowSamples);
                ringBuffer.discard(shift);
                hilbertEnvelope.envelope();
                processEnvelope(leftOffset, rightOffset, processedSamples);
            }
        }

    }

    private void processEnvelope(int leftOffset, int rightOffset,
                                 double[] processedSamples) {
        for (int i = leftOffset; i < rightOffset; i++) {
            final SignalLevel signalLevel = processedSamples[i] > silenceLevel ? SignalLevel.High : SignalLevel.Low;
            if (signalState.signalLevel != signalLevel) { // signal state changed LOW <--> HIGH
                // debouncing
                if (signalState.durationInSamples / (double) sampleRate < minDotDuration) { // too short?
                    if (savedSignalStatesIndex > -1) { // has previous state?
                        // add duration to the previous detected state
                        savedSignalStates[savedSignalStatesIndex].durationInSamples += signalState.durationInSamples;
                    }
                } else {
                    if (savedSignalStatesIndex > -1 && savedSignalStates[savedSignalStatesIndex].signalLevel == signalState.signalLevel) { // the same level as before?
                        savedSignalStates[savedSignalStatesIndex].durationInSamples += signalState.durationInSamples; // add duration
                    } else { // level changed or/and no saved state
                        savedSignalStates[++savedSignalStatesIndex] = signalState.cloneState(); // save actual state
                    }
                }

                if (savedSignalStatesIndex > 0) { // do we have 2 saved states savedSignalStates[0] and savedSignalStates[1]?
                    final SignalState ss = savedSignalStates[0];
                    savedSignalStates[0] = savedSignalStates[1]; // shift the latest state to the previous position
                    savedSignalStates[1] = null;
                    savedSignalStatesIndex = 0;
                    detectDashDotDurations(ss);
                }

                signalState.durationInSamples = 0;
                signalState.signalLevel = signalLevel;
            } else if (signalState.durationInSamples > flushDetectionBuffers * sampleRate &&
                    savedSignalStatesIndex > -1) {
                detectDashDotDurations(savedSignalStates[0]);
                detectDashDotDurations(signalState);
                savedSignalStates[0] = null;
                savedSignalStatesIndex = -1;
            }
            signalState.durationInSamples++;
        }
    }

    private void detectDashDotDurations(SignalState signalState) {
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
                        dashDotClassifier.process(ss, dotDurationInSamples);
                    }
                }
                while (statesForDetection.size() >= detectorQueueLength) {
                    statesForDetection.removeFirst();
                }
                if (firstUpdate) return;
            }
        }

        if (dotDurationInSamples > 0) {
            dashDotClassifier.process(signalState, dotDurationInSamples);
        }
    }



}
