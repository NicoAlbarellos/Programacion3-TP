package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.seed.DominioCaba;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de los datos del dominio (DominioCaba), sin Neo4j.
 * Comprueban que la carga inicial y los tests trabajan sobre datos coherentes.
 */
class DominioCabaTest {

    @Test
    void tiene31Lugares51CallesYOchoSuministros() {
        assertEquals(31, DominioCaba.LUGARES.size());
        assertEquals(51, DominioCaba.CALLES.size());
        assertEquals(8, DominioCaba.SUMINISTROS.size());
    }

    @Test
    void losIdsDeLugaresNoSeRepiten() {
        Set<String> ids = new HashSet<>();
        for (DominioCaba.Lugar l : DominioCaba.LUGARES) {
            assertTrue(ids.add(l.id()), "id de lugar repetido: " + l.id());
        }
    }

    @Test
    void cadaCalleUneDosLugaresQueExisten() {
        Set<String> ids = idsDeLugares();
        for (DominioCaba.Calle c : DominioCaba.CALLES) {
            assertTrue(ids.contains(c.origen()), "origen inexistente: " + c.origen());
            assertTrue(ids.contains(c.destino()), "destino inexistente: " + c.destino());
            assertNotEquals(c.origen(), c.destino(), "una calle no puede volver al mismo lugar");
        }
    }

    @Test
    void todasLasCallesCuestanMinutosPositivos() {
        for (DominioCaba.Calle c : DominioCaba.CALLES) {
            assertTrue(c.minutos() > 0, "costo no positivo en " + c.origen() + "-" + c.destino());
        }
    }

    @Test
    void ningunaCalleEstaCargadaDosVeces() {
        Set<String> pares = new HashSet<>();
        for (DominioCaba.Calle c : DominioCaba.CALLES) {
            assertTrue(pares.add(c.origen() + "-" + c.destino()), "calle repetida");
            assertFalse(pares.contains(c.destino() + "-" + c.origen()), "calle cargada en los dos sentidos");
        }
    }

    @Test
    void cadaSuministroEstaEnUnLugarQueExiste() {
        Set<String> ids = idsDeLugares();
        for (DominioCaba.Suministro s : DominioCaba.SUMINISTROS) {
            assertTrue(ids.contains(s.lugarId()), "lugar inexistente para " + s.id());
        }
    }

    @Test
    void losSuministrosPesanTreintaYTresKgEnTotal() {
        double total = 0;
        for (DominioCaba.Suministro s : DominioCaba.SUMINISTROS) {
            total += s.kg();
        }
        assertEquals(33.0, total);
    }

    private Set<String> idsDeLugares() {
        Set<String> ids = new HashSet<>();
        for (DominioCaba.Lugar l : DominioCaba.LUGARES) {
            ids.add(l.id());
        }
        return ids;
    }
}
