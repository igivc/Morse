package igivc.morse;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class SoundRecorder {
    public final int sampleRate; // samples per second

    private static final Logger logger = Logger.getLogger(SoundRecorder.class.getSimpleName());
    private final Queue<double[]> audioQueue = new LinkedList<>();

    private Thread thrReadAudio;
    private TargetDataLine targetDataLine;
    private final AtomicBoolean canContinue = new AtomicBoolean(true);

    public SoundRecorder(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    void start() {
        try {
            AudioFormat format = new AudioFormat(
                    sampleRate,
                    16,
                    1,
                    true,
                    true);

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);

            thrReadAudio = new Thread(() -> {
                canContinue.set(true);
                targetDataLine.start();
                byte[] data = new byte[targetDataLine.getBufferSize()];
                while (canContinue.get() && !Thread.interrupted()) {
                    int nBytesRead = targetDataLine.read(data, 0, data.length);
                    if (nBytesRead > 0) {
                        double[] readData = bytesToDoublesBigEndian(data, nBytesRead);
                        synchronized (audioQueue) {
                            audioQueue.add(readData);
                            audioQueue.notify();
                        }
                    }
                }
            });

            thrReadAudio.start();

        } catch (LineUnavailableException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static double[] bytesToDoublesBigEndian(byte[] data, int nBytesRead) {
        final int n = nBytesRead / 2;
        final double[] out = new double[n];

        int j = 0;
        for (int i = 0; i < n; i++) {
            final int hi = data[j++] & 0xFF; // старший байт
            final int lo = data[j++] & 0xFF; // младший байт

            final short sample = (short) ((hi << 8) | lo);
            out[i] = sample / 32768.0; // normalization
        }
        return out;
    }


    void stop() {
        try {
            if (targetDataLine != null) {
                canContinue.set(false);
                thrReadAudio.join();
                targetDataLine.close();
                targetDataLine = null;
            }
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    public double[] get() {
        synchronized (audioQueue) {
            while(canContinue.get()) {
                var readData = audioQueue.poll();
                if (readData != null) {
                    return readData;
                }
                try {
                    audioQueue.wait();
                } catch (InterruptedException e) {
                    canContinue.set(false);
                    logger.log(Level.WARNING, e.toString());
                    break;
                }
            }
            return new double[0]; // indicate that recorder is finished/interrupted
        }
    }

    public boolean isRunning() {
        return canContinue.get();
    }

    Stream<double[]> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<double[]>() {

                            @Override
                            public boolean hasNext() {
                                return canContinue.get();
                            }

                            @Override
                            public double[] next() {
                                return get();
                            }
                        },
                        Spliterator.IMMUTABLE),
                false);
    }
}
