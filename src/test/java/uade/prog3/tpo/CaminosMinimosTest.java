package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.CaminosMinimos.Camino;
import uade.prog3.tpo.service.Grafo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Dijkstra con resultados calculados a mano. No tocan Neo4j.
 * Costos del dominio en minutos a pie (ver DominioCaba.CALLES).
 */
class CaminosMinimosTest {

    private final CaminosMinimos caminosMinimos = new CaminosMinimos();

    private void verificar(Grafo g, String origen, String destino, double costo, List<String> vertices) {
        Camino camino = caminosMinimos.dijkstra(g, origen, destino);
        assertEquals(costo, camino.costoTotal(), 1e-9);
        assertEquals(vertices, camino.vertices());
    }

    @Test
    void retiroACiudadUniversitaria() {
        // RET-PAL 45 + PAL-MON 66 + MON-CUN 18 = 129 (por PAL-CUN directo seria 135)
        verificar(GrafosDePrueba.dominioCaba(), "RET", "CUN", 129, List.of("RET", "PAL", "MON", "CUN"));
    }

    @Test
    void relajacionEvitaElDirectoPalermoCiudadUniversitaria() {
        // PAL-MON 66 + MON-CUN 18 = 84 < PAL-CUN directo 90
        verificar(GrafosDePrueba.dominioCaba(), "PAL", "CUN", 84, List.of("PAL", "MON", "CUN"));
    }

    @Test
    void laBocaAHospitalItaliano() {
        // LBO-CON 29 + CON-ABA 36 + ABA-HIT 19 = 84
        verificar(GrafosDePrueba.dominioCaba(), "LBO", "HIT", 84, List.of("LBO", "CON", "ABA", "HIT"));
    }

    @Test
    void retiroAAbasto() {
        // RET-REC 20 + REC-ABA 29 = 49 (por OBE: 22 + 28 = 50)
        verificar(GrafosDePrueba.dominioCaba(), "RET", "ABA", 49, List.of("RET", "REC", "ABA"));
    }

    @Test
    void laBocaACiudadUniversitaria() {
        // LBO-PMA 36 + PMA-RET 18 + RET-PAL 45 + PAL-MON 66 + MON-CUN 18 = 183
        verificar(GrafosDePrueba.dominioCaba(), "LBO", "CUN", 183,
                List.of("LBO", "PMA", "RET", "PAL", "MON", "CUN"));
    }

    @Test
    void semillaCatedraAaH() {
        // A-B 4 + B-C 3 + C-D 2 + D-F 8 + F-H 3 = 20 (verificacion de la clase 5)
        verificar(GrafosDePrueba.semillaCatedra(), "A", "H", 20, List.of("A", "B", "C", "D", "F", "H"));
    }

    @Test
    void semillaCatedraAaC() {
        // A-B 4 + B-C 3 = 7 < A-C directo 9
        verificar(GrafosDePrueba.semillaCatedra(), "A", "C", 7, List.of("A", "B", "C"));
    }

    @Test
    void origenIgualDestino() {
        verificar(GrafosDePrueba.dominioCaba(), "OBE", "OBE", 0, List.of("OBE"));
    }

    @Test
    void costoNegativoFalla() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 2);
        g.agregarArista("B", "C", -1);
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.dijkstra(g, "A", "C"));
    }

    @Test
    void destinoInalcanzableFalla() {
        Grafo g = GrafosDePrueba.dominioCaba();
        g.agregarVertice("ZZZ");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> caminosMinimos.dijkstra(g, "RET", "ZZZ"));
        assertEquals("No hay camino de RET a ZZZ", e.getMessage());
    }

    @Test
    void nodoInexistenteFalla() {
        Grafo g = GrafosDePrueba.dominioCaba();
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.dijkstra(g, "XXX", "CUN"));
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.dijkstra(g, "RET", "XXX"));
    }
}
