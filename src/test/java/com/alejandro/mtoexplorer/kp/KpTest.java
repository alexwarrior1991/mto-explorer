package com.alejandro.mtoexplorer.kp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KpTest {

    @Test
    void formatMetersAsPlanKp() {
        assertEquals("17+858", Kp.format(17858d));
        assertEquals("17+858.5", Kp.format(17858.5));
        assertEquals("17+000", Kp.format(17000d));
        assertEquals("0+012.3", Kp.format(12.3));
        assertEquals("-0+250", Kp.format(-250d));
        assertNull(Kp.format(null));
    }

    @Test
    void parsesPlanKpAndPlainMeters() {
        assertEquals(12345d, Kp.parse("12+345"));
        assertEquals(12345.5, Kp.parse("12+345.5"));
        assertEquals(12345.5, Kp.parse("12+345,5"));
        assertEquals(12345d, Kp.parse("12345"));
        assertEquals(-250d, Kp.parse("-0+250"));
    }

    @Test
    void rejectsWhatIsNotAKp() {
        assertThrows(IllegalArgumentException.class, () -> Kp.parse("doce"));
        assertThrows(IllegalArgumentException.class, () -> Kp.parse(" "));
    }
}
