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
        // RET-REC 26 + REC-PAL 40 + PAL-PLA 23 + PLA-AEP 21 + AEP-CUN 44 = 154
        // (por PAL-CHA-COL-BEL-MON-CUN seria 199)
        verificar(GrafosDePrueba.dominioCaba(), "RET", "CUN", 154,
                List.of("RET", "REC", "PAL", "PLA", "AEP", "CUN"));
    }

    @Test
    void relajacionEvitaElDirectoGarrahanLaBoca() {
        // HGA-CON 15 + CON-LBO 32 = 47 < HGA-LBO directo 51
        verificar(GrafosDePrueba.dominioCaba(), "HGA", "LBO", 47, List.of("HGA", "CON", "LBO"));
    }

    @Test
    void laBocaAHospitalItaliano() {
        // LBO-CON 32 + CON-CGR 33 + CGR-ABA 27 + ABA-HIT 15 = 107
        verificar(GrafosDePrueba.dominioCaba(), "LBO", "HIT", 107, List.of("LBO", "CON", "CGR", "ABA", "HIT"));
    }

    @Test
    void retiroAAbasto() {
        // RET-REC 26 + REC-ABA 36 = 62 (por OBE-CGR: 23 + 18 + 27 = 68)
        verificar(GrafosDePrueba.dominioCaba(), "RET", "ABA", 62, List.of("RET", "REC", "ABA"));
    }

    @Test
    void laBocaACiudadUniversitaria() {
        // LBO-CON 32 + CON-OBE 40 + OBE-REC 31 + REC-PAL 40 + PAL-PLA 23 + PLA-AEP 21 + AEP-CUN 44 = 231
        verificar(GrafosDePrueba.dominioCaba(), "LBO", "CUN", 231,
                List.of("LBO", "CON", "OBE", "REC", "PAL", "PLA", "AEP", "CUN"));
    }

    @Test
    void mataderosACiudadUniversitariaEsElParMasLejano() {
        // MAT-FLO 73 + FLO-CAB 43 + CAB-PCE 20 + PCE-CHA 37 + CHA-COL 19 + COL-BEL 27
        // + BEL-MON 24 + MON-CUN 20 = 263 (diametro del mapa)
        verificar(GrafosDePrueba.dominioCaba(), "MAT", "CUN", 263,
                List.of("MAT", "FLO", "CAB", "PCE", "CHA", "COL", "BEL", "MON", "CUN"));
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
