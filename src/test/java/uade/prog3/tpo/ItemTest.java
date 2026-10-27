package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.model.Item;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del ratio utilidad/peso de los suministros, el criterio del greedy.
 */
class ItemTest {

    @Test
    void ratioDelBotiquinEsCincuentaSobreTres() {
        Item botiquin = new Item("S1", "Botiquin", 3, 50, "HIT");
        assertEquals(50.0 / 3.0, botiquin.ratioValorPeso(), 1e-9);
    }

    @Test
    void ratioDeLaComidaEsDiez() {
        Item comida = new Item("S2", "Comida enlatada", 4, 40, "ABA");
        assertEquals(10.0, comida.ratioValorPeso());
    }

    @Test
    void laLinternaTieneMejorRatioQueElAgua() {
        Item linterna = new Item("S4", "Linterna", 1, 8, "OBE");
        Item agua = new Item("S3", "Agua 7L", 7, 55, "PCE");
        // 8 > 55/7 = 7,857...: por eso el greedy elige la linterna antes que el agua
        assertTrue(linterna.ratioValorPeso() > agua.ratioValorPeso());
    }

    @Test
    void pesoCeroDaElRatioMaximo() {
        Item sinPeso = new Item("X", "Sin peso", 0, 5, null);
        assertEquals(Double.MAX_VALUE, sinPeso.ratioValorPeso());
    }

    @Test
    void utilidadCeroDaRatioCero() {
        Item inutil = new Item("Y", "Inutil", 2, 0, null);
        assertEquals(0.0, inutil.ratioValorPeso());
    }
}
