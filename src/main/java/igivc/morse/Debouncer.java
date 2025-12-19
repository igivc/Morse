package igivc.morse;

import sun.misc.Signal;

import java.util.function.Consumer;

class Debouncer implements Consumer<SignalLevel> {
    private final SignalState[] savedSignalStates = new SignalState[2];
    private final SignalState signalState = new SignalState();
    private final Consumer<SignalState> dotDurationMeter;
    private final int sampleRate;
    private int savedSignalStatesIndex = -1;
    private static final double minDotDuration = 0.02; // seconds
    private static final double flushDetectionBuffers = 1.5; // flush buffers when silence duration is more than ... seconds

    public Debouncer(Consumer<SignalState> dotDurationMeter, int sampleRate) {
        this.dotDurationMeter = dotDurationMeter;
        this.sampleRate = sampleRate;
    }

    public void accept(SignalLevel signalLevel) {
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
                dotDurationMeter.accept(ss);
            }

            signalState.durationInSamples = 0;
            signalState.signalLevel = signalLevel;
        } else if (signalState.durationInSamples > flushDetectionBuffers * sampleRate &&
                savedSignalStatesIndex > -1) {
            dotDurationMeter.accept(savedSignalStates[0]);
            dotDurationMeter.accept(signalState);
            savedSignalStates[0] = null;
            savedSignalStatesIndex = -1;
        }
        signalState.durationInSamples++;
    }
}
