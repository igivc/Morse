package igivc.morse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class GoertzelTest {

    @Test
    void testSquareWindow() {
        ArrayList<Double> results = new ArrayList<>();
        Goertzel goertzel = new Goertzel(1, 8, 1, false, (Goertzel g)->{
            results.add(g.getMagnitudeSquared());
        });
        // See https://ru.dsplib.org/content/goertzel/goertzel.html
        // Пример использования алгоритма Гёрцеля
        goertzel.process(3);
        goertzel.process(2);
        goertzel.process(1);
        goertzel.process(-1);
        goertzel.process(1);
        goertzel.process(-2);
        goertzel.process(-3);
        goertzel.process(-2);
        assertEquals(1, results.size());
        // |(4.1213 - 7.5355j)| = 73.769...
        assertEquals(737696, Math.round(results.getFirst() * 10000));
    }

    @Test
    void testBlackmanHarrisWindow() {
        ArrayList<Double> results = new ArrayList<>();
        Goertzel goertzel = new Goertzel(1, 8, 1, true, (Goertzel g)->{
            results.add(g.getMagnitudeSquared());
        });
        goertzel.process(3);
        goertzel.process(2);
        goertzel.process(1);
        goertzel.process(-1);
        goertzel.process(1);
        goertzel.process(-2);
        goertzel.process(-3);
        goertzel.process(-2);
        assertEquals(1, results.size());
        assertEquals(22526, Math.round(results.getFirst() * 10000));
    }

}
