package igivc.morse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RingBufferTest {

    public static final double EPSILON = 1e-12;

    @Test
    public void testInitiallyEmpty() {
        RingBuffer rb = new RingBuffer(4);

        assertTrue(rb.isEmpty());
        assertFalse(rb.isFull());
        assertEquals(0, rb.getSize());
        assertEquals(4, rb.getFreeSpace());
    }

    @Test
    public void testWriteAndReadSingleElement() {
        RingBuffer rb = new RingBuffer(3);

        rb.write(1.23);

        assertFalse(rb.isEmpty());
        assertEquals(1, rb.getSize());
        assertEquals(2, rb.getFreeSpace());

        double v = rb.read();
        assertEquals(1.23, v, EPSILON);

        assertTrue(rb.isEmpty());
        assertEquals(0, rb.getSize());
    }

    @Test
    public void testFifoOrder() {
        RingBuffer rb = new RingBuffer(5);

        rb.write(1.0);
        rb.write(2.0);
        rb.write(3.0);

        assertEquals(1.0, rb.read(), EPSILON);
        assertEquals(2.0, rb.read(), EPSILON);
        assertEquals(3.0, rb.read(), EPSILON);

        assertTrue(rb.isEmpty());
    }

    @Test
    public void testWrapAroundBehavior() {
        RingBuffer rb = new RingBuffer(3);

        rb.write(1.0);
        rb.write(2.0);
        rb.write(3.0);
        assertTrue(rb.isFull());

        // Освобождаем одно место
        assertEquals(1.0, rb.read(), EPSILON);

        // Добавляем элемент, должен попасть в начало массива
        rb.write(4.0);

        assertTrue(rb.isFull());

        assertEquals(2.0, rb.read(), EPSILON);
        assertEquals(3.0, rb.read(), EPSILON);
        assertEquals(4.0, rb.read(), EPSILON);

        assertTrue(rb.isEmpty());
    }

    @Test
    public void testToArrayPreservesOrder() {
        RingBuffer rb = new RingBuffer(4);

        rb.write(1.0);
        rb.write(2.0);
        rb.write(3.0);

        double[] arr = rb.toArray();

        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, arr, EPSILON);
    }

    @Test
    public void testToArrayAfterWrapAround() {
        RingBuffer rb = new RingBuffer(3);

        rb.write(1.0);
        rb.write(2.0);
        rb.write(3.0);
        rb.read();       // удаляем 1.0
        rb.write(4.0);   // wrap-around

        double[] arr = rb.toArray();

        assertArrayEquals(new double[]{2.0, 3.0, 4.0}, arr, EPSILON);
    }

    @Test
    public void testClearResetsState() {
        RingBuffer rb = new RingBuffer(3);

        rb.write(1.0);
        rb.write(2.0);

        rb.clear();

        assertTrue(rb.isEmpty());
        assertFalse(rb.isFull());
        assertEquals(0, rb.getSize());
        assertEquals(3, rb.getFreeSpace());

        // после clear можно снова использовать
        rb.write(5.0);
        assertEquals(5.0, rb.read(), EPSILON);
    }

    @Test
    public void testOverfillThrowsException() {
        RingBuffer rb = new RingBuffer(2);

        rb.write(1.0);
        rb.write(2.0);

        assertTrue(rb.isFull());

        assertThrows(IndexOutOfBoundsException.class, () -> rb.write(3.0));
    }

    @Test
    public void testUnderflowThrowsException() {
        RingBuffer rb = new RingBuffer(2);

        assertTrue(rb.isEmpty());

        assertThrows(IndexOutOfBoundsException.class, rb::read);
    }

    @Test
    public void testWriteArray() {
        RingBuffer rb = new RingBuffer(5);

        rb.write(new double[]{1, 2, 3});
        assertEquals(3, rb.getSize());

        double[] tmp = new double[3];
        rb.copyTo(tmp, 0, 3);
        assertArrayEquals(new double[]{1, 2, 3}, tmp, EPSILON);
    }

    @Test
    public void testWriteArrayWrapAround() {
        RingBuffer rb = new RingBuffer(5);

        rb.write(new double[]{1, 2, 3});
        rb.discard(2);                 // остаётся [3]
        rb.write(new double[]{4, 5, 6});

        double[] tmp = new double[4];
        rb.copyTo(tmp, 0, 4);
        assertArrayEquals(new double[]{3, 4, 5, 6}, tmp, EPSILON);
    }

    @Test
    public void testWriteArrayWrapAround1() {
        RingBuffer rb = new RingBuffer(5);

        rb.write(new double[]{1, 2, 3, 4, 5});
        rb.discard(3);                 // остаётся [4, 5]
        rb.write(6);
        rb.write(new  double[]{7, 8});

        double[] tmp = new double[5];
        rb.copyTo(tmp, 0, 5);
        assertArrayEquals(new double[]{4, 5, 6, 7, 8}, tmp, EPSILON);
    }

    @Test
    public void testWriteArrayOverflow() {
        RingBuffer rb = new RingBuffer(4);
        rb.write(new double[]{1, 2, 3});

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> rb.write(new double[]{4, 5})
        );
    }

}
