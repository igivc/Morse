package igivc.morse;

/**
 * A simple implementation of a complex FFT (Cooley–Tukey radix-2).
 */
class FFT {
    /**
     * A simple implementation of a complex FFT (Cooley–Tukey radix-2).
     * real[] and imag[] contain the signal at the input and the spectrum at the output (or vice versa).
     * If inverse == true, an inverse FFT is performed (dividing by N).
     * The array length must be a power of two.
     *
     * @param real    Real part of signal, input and output.
     * @param imag    Image part of signal, input and output.
     * @param inverse Perform inverse FFT.
     */
    public static void fft(double[] real, double[] imag, boolean inverse) {
        int n = real.length;
        if (Integer.bitCount(n) != 1) {
            throw new IllegalArgumentException("Length must be power of 2");
        }

        // Битарный разворот индексов
        int j = 0;
        for (int i = 0; i < n; i++) {
            if (i < j) {
                double tr = real[i];
                double ti = imag[i];
                real[i] = real[j];
                imag[i] = imag[j];
                real[j] = tr;
                imag[j] = ti;
            }
            int m = n >>> 1;
            while (j >= m && m >= 2) {
                j -= m;
                m >>>= 1;
            }
            j += m;
        }

        // Основные стадии FFT
        for (int len = 2; len <= n; len <<= 1) {
            double ang = 2 * Math.PI / len * (inverse ? 1 : -1);
            double wlenCos = Math.cos(ang);
            double wlenSin = Math.sin(ang);

            for (int i = 0; i < n; i += len) {
                double wr = 1.0;
                double wi = 0.0;

                for (int k = 0; k < len / 2; k++) {
                    int u = i + k;
                    int v = i + k + len / 2;

                    double ur = real[u];
                    double ui = imag[u];
                    double vr = real[v] * wr - imag[v] * wi;
                    double vi = real[v] * wi + imag[v] * wr;

                    real[u] = ur + vr;
                    imag[u] = ui + vi;
                    real[v] = ur - vr;
                    imag[v] = ui - vi;

                    // w *= wlen
                    double nextWr = wr * wlenCos - wi * wlenSin;
                    double nextWi = wr * wlenSin + wi * wlenCos;
                    wr = nextWr;
                    wi = nextWi;
                }
            }
        }

        // Scaling for reverse FFT
        if (inverse) {
            for (int i = 0; i < n; i++) {
                real[i] /= n;
                imag[i] /= n;
            }
        }
    }
}
