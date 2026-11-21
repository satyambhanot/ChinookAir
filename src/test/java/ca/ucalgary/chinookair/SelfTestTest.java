package ca.ucalgary.chinookair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SelfTestTest {

    @Test
    void passesOnTheDemoDataAndSaysSo() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        boolean ok = SelfTest.run(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        assertTrue(ok, bytes.toString(StandardCharsets.UTF_8));
        assertEquals("SELFTEST OK", bytes.toString(StandardCharsets.UTF_8).trim());
    }

    @Test
    void theArgumentIsTheOneTheReportNames() {
        assertEquals("--selftest", SelfTest.ARGUMENT);
    }
}
