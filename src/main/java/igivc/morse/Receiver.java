package igivc.morse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.function.Consumer;
import java.util.function.Function;

class Receiver {
    private static final int sampleRate = 8000; // samples per second
    private static final double quality = 15;
    private static final double freq = 800; // Hz
    private static final double silenceLevel = 0.122; // 0..1
    private static final double flushDetectionBuffers = 1.5; // flush buffers when silence duration is more than ... seconds
    private static final double minDotDuration = 0.02; // seconds
    private static final double approxWindow = 0.2; // signal window in seconds
    private final SignalState[] savedSignalStates = new SignalState[2];
    private final Consumer<String> consumer;
    private SignalState signalState = null;
    private int savedSignalStatesIndex = -1;

    public Receiver(Consumer<String> consumer) {
        this.consumer = consumer;
    }

    /**
     * Receives Morse symbols
     */
    void receive() throws Exception {
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
        DashDotClassifier dashDotClassifier = new DashDotClassifier(consumer);
        DotDurationMeter dotDurationMeter = new DotDurationMeter(dashDotClassifier);
        final Debouncer debouncer = new Debouncer(dotDurationMeter, sampleRate);
        AttackReleaseSmoother smoother = new AttackReleaseSmoother(sampleRate);
        SignalLevelClassifier classifier = new SignalLevelClassifier(debouncer, sampleRate);
//        Consumer<Double> classifier = (Double sample) -> {
//            if (sample > silenceLevel) debouncer.accept(SignalLevel.High); else debouncer.accept(SignalLevel.Low);
//        };
        //
        try (var writer = new PrintWriter("smoothed.csv")) {
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
                    processEnvelope(classifier, smoother, leftOffset, rightOffset, processedSamples, writer);
                }
            }
            //

        }

    }

    private void processEnvelope(Consumer<Double> classifier, Function<Double, Double> smoother, int leftOffset, int rightOffset,
                                 double[] processedSamples, PrintWriter writer) throws IOException {
        for (int i = leftOffset; i < rightOffset; i++) {
            double x = processedSamples[i];
            double y = smoother.apply(x);
            writer.println(Integer.toString ((int)(y * 32768)));
            writer.flush();
            classifier.accept(y);
        }
    }
}
