package igivc.morse;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class HilbertEnvelopeTest {

    private static final double EPSILON = 1e-12;

    @Test
    public void testCtorRejectsDifferentLengths() {
        double[] src = new double[1024];
        double[] dst = new double[512];
        assertThrows(IllegalArgumentException.class, () -> new HilbertEnvelope(src, dst));
    }

    @Test
    public void testCtorRejectsNonPowerOfTwo() {
        double[] src = new double[1000];
        double[] dst = new double[1000];
        assertThrows(IllegalArgumentException.class, () -> new HilbertEnvelope(src, dst));
    }

    @Test
    public void testZeroSignalGivesZeroEnvelope() {
        int n = 1024;
        double[] src = new double[n]; // all zeros
        double[] dst = new double[n];

        HilbertEnvelope he = new HilbertEnvelope(src, dst);
        he.envelope();

        for (int i = 0; i < n; i++) {
            assertEquals(0.0, dst[i], EPSILON);
        }
    }

    @Test
    public void testEnvelopeNonNegativeFinite() {
        int n = 1024;
        double[] src = new double[n];
        double[] dst = new double[n];

        Random rnd = new Random(123);
        for (int i = 0; i < n; i++) {
            src[i] = rnd.nextGaussian();
        }

        HilbertEnvelope he = new HilbertEnvelope(src, dst);
        he.envelope();

        for (int i = 0; i < n; i++) {
            assertTrue(dst[i] >= 0.0, "envelope must be non-negative at " + i);
            assertFalse(Double.isNaN(dst[i]), "NaN at " + i);
            assertFalse(Double.isInfinite(dst[i]), "Inf at " + i);
        }
    }

    @Test
    public void testDoesNotModifySrcAndOverwritesDst() {
        int n = 1024;
        double[] src = new double[n];
        double[] dst = new double[n];

        for (int i = 0; i < n; i++) {
            src[i] = Math.sin(2 * Math.PI * i / n);
            dst[i] = 12345.0; // sentinel
        }
        double[] srcCopy = src.clone();

        HilbertEnvelope he = new HilbertEnvelope(src, dst);
        he.envelope();

        assertArrayEquals(srcCopy, src, 0.0, "src must not be modified");

        // dst должен быть перезаписан: значения не должны остаться sentinel
        boolean anyChanged = false;
        for (double v : dst) {
            if (v != 12345.0) {
                anyChanged = true;
                break;
            }
        }
        assertTrue(anyChanged, "dst must be overwritten");
    }

    @Test
    public void testPureSineHasAlmostConstantEnvelope() {
        int n = 2048;
        double[] src = new double[n];
        double[] dst = new double[n];

        // Важно: частота должна ровно попадать в FFT-бин, чтобы избежать утечек спектра
        // sin(2*pi*k*i/n)
        int k = 17;
        double A = 0.8;

        for (int i = 0; i < n; i++) {
            src[i] = A * Math.sin(2.0 * Math.PI * k * i / n);
        }

        HilbertEnvelope he = new HilbertEnvelope(src, dst);
        he.envelope();

        int start = (int) (0.05 * n);
        int end = (int) (0.95 * n);

        double mean = 0.0;
        for (int i = start; i < end; i++) mean += dst[i];
        mean /= (end - start);

        // Среднее значение огибающей должно быть близко к A
        assertEquals(A, mean, 0.01, "mean envelope should be close to amplitude");

        // Разброс вокруг среднего должен быть небольшим
        double maxDev = 0.0;
        for (int i = start; i < end; i++) {
            maxDev = Math.max(maxDev, Math.abs(dst[i] - mean));
        }
        assertTrue(maxDev < 0.08, "envelope should be fairly flat, maxDev=" + maxDev);
    }

    @Test
    public void testAMSignalEnvelopeMatchesModulation() {
        int n = 4096;
        double[] src = new double[n];
        double[] dst = new double[n];

        // AM: x[i] = A(i) * sin(2*pi*kCarrier*i/n)
        // A(i) = 1 + m * sin(2*pi*kMod*i/n)
        int kCarrier = 123;
        int kMod = 7;
        double m = 0.5;     // глубина модуляции
        double carrierAmp = 1.0;

        double[] expected = new double[n];
        for (int i = 0; i < n; i++) {
            double A = 1.0 + m * Math.sin(2.0 * Math.PI * kMod * i / n);
            src[i] = carrierAmp * A * Math.sin(2.0 * Math.PI * kCarrier * i / n);
            expected[i] = carrierAmp * A;
        }

        HilbertEnvelope he = new HilbertEnvelope(src, dst);
        he.envelope();

        int start = (int) (0.05 * n);
        int end = (int) (0.95 * n);

        double mse = 0.0; //Mean Square Error
        for (int i = start; i < end; i++) {
            double d = dst[i] - expected[i];
            mse += d * d;
        }
        mse /= (end - start);
        double rmse = Math.sqrt(mse); // Root Mean Square Error

        // RMSE должен быть маленьким. Порог можно подстроить под твою FFT реализацию.
        assertTrue(rmse < 0.05, "RMSE too high: " + rmse);
    }

    @Test
    public void testScaleInvariant() {
        int n = 2048;

        double[] src1 = new double[n];
        double[] dst1 = new double[n];

        double[] src2 = new double[n];
        double[] dst2 = new double[n];

        // Возьмём сигнал с богатым спектром (не просто синус)
        // чтобы тест был более "общим"
        int k1 = 23;
        int k2 = 57;
        double a1 = 0.7;
        double a2 = 0.3;

        for (int i = 0; i < n; i++) {
            src1[i] = a1 * Math.sin(2.0 * Math.PI * k1 * i / n) +
                    a2 * Math.cos(2.0 * Math.PI * k2 * i / n);
        }

        double scale = 2.5; // произвольный коэффициент масштаба

        for (int i = 0; i < n; i++) {
            src2[i] = scale * src1[i];
        }

        HilbertEnvelope h1 = new HilbertEnvelope(src1, dst1);
        HilbertEnvelope h2 = new HilbertEnvelope(src2, dst2);

        h1.envelope();
        h2.envelope();

        // Игнорируем края из-за FFT-артефактов
        int start = (int) (0.05 * n);
        int end = (int) (0.95 * n);

        double maxRelError = 0.0;

        for (int i = start; i < end; i++) {
            double expected = Math.abs(scale) * dst1[i];
            double actual = dst2[i];

            if (expected > 1e-9) {
                double relErr = Math.abs(actual - expected) / expected;
                maxRelError = Math.max(maxRelError, relErr);
            }
        }

        // Допуск зависит от FFT и double-арифметики,
        // 2–3% — нормальный и устойчивый порог
        assertTrue(
                maxRelError < 0.03,
                "Scale invariance violated, max relative error = " + maxRelError
        );
    }

}
