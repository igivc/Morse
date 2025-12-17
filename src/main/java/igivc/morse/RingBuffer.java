package igivc.morse;

/**
 * Ring buffer
 */
final class RingBuffer {
    private final double[] buf;
    private int head = 0, tail = 0, size = 0;

    public RingBuffer(int capacity) {
        buf = new double[capacity];
    }

    public void write(double sample) {
        if (isFull()) {
            throw new IndexOutOfBoundsException("Overfilled");
        }
        buf[tail] = sample;
        tail = (tail + 1) % buf.length;
        size++;
    }

    public void write(double[] samples) {
        if (isFull()) {
            throw new IndexOutOfBoundsException("Overfilled");
        }
        final int srcLength = samples.length;
        if (srcLength == 0) return;

        if (srcLength > getFreeSpace()) {
            throw new IndexOutOfBoundsException("Not enough space to write");
        }

        int first = Math.min(srcLength, buf.length - tail);
        System.arraycopy(samples, 0, buf, tail, first);

        int rem = srcLength - first;
        if (rem > 0) {
            System.arraycopy(samples, first, buf, 0, rem);
        }

//        tail += srcLength;
//        if (tail >= buf.length) tail -= buf.length;
        tail = (tail + srcLength) % buf.length;
        size += srcLength;
    }

    public double read() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("Underfilled");
        }
        double v = buf[head];
        head = (head + 1) % buf.length;
        size--;
        return v;
    }

    public int getSize() {
        return size;
    }

    public int getFreeSpace() {
        return buf.length - size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == buf.length;
    }

    public void clear() {
        head = tail = size = 0;
    }

    public void discard(int len) {
        if (len < 0) throw new IndexOutOfBoundsException("len cannot be negative");
        if (len > size) throw new IndexOutOfBoundsException("Underfilled");
        if (len == 0) return;
        head = (head + len) % buf.length;
        size -= len;
        if (size == 0) { // необязательно, но удобно
            head = tail = 0;
        }
    }

    public double[] toArray() {
        double[] out = new double[size];

        if (size == 0) {
            return out;
        }

        copyTo(out, 0, size);

        return out;
    }

    public void copyTo(double[] dst, int offset, int len) {
        if (len < 0) {
            throw new IndexOutOfBoundsException("len < 0");
        }
        if (len > size) {
            throw new IndexOutOfBoundsException("Not enough data");
        }
        if (offset < 0 || offset + len > dst.length) {
            throw new IndexOutOfBoundsException("dst range");
        }
        if (len == 0) return;

        int cap = buf.length;

        int first = Math.min(len, cap - head);
        System.arraycopy(buf, head, dst, offset, first);

        int rem = len - first;
        if (rem > 0) {
            System.arraycopy(buf, 0, dst, offset + first, rem);
        }
    }


}
