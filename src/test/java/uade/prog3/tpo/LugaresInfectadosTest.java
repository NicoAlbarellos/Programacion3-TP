package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Backtracking;
import uade.prog3.tpo.algorithm.Backtracking.ResultadoRutas;
import uade.prog3.tpo.algorithm.Backtracking.Ruta;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.CaminosMinimos.Camino;
import uade.prog3.tpo.service.Grafo;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lugares infectados: Dijkstra y backtracking sin pasar por ciertos nodos.
 * Resultados calculados a mano sobre el dominio CABA (GrafosDePrueba). No tocan Neo4j.
 */
class LugaresInfectadosTest {

    private final CaminosMinimos caminosMinimos = new CaminosMinimos();
    private final Backtracking backtracking = new Backtracking();
    private final Grafo caba = GrafosDePrueba.dominioCaba();

    private Camino dijkstra(String origen, String destino, String... infectados) {
        return caminosMinimos.dijkstra(caba, origen, destino, Set.of(infectados));
    }

    // ---------- Dijkstra ----------

    @Test
    void constitucionInfectadaObligaALaCalleDirectaGarrahanLaBoca() {
        // normal: HGA-CON-LBO = 15 + 32 = 47. Sin CON solo queda la calle directa HGA-LBO = 51
        Camino c = dijkstra("HGA", "LBO", "CON");
        assertEquals(List.of("HGA", "LBO"), c.vertices());
        assertEquals(51, c.costoTotal(), 1e-9);
    }

    @Test
    void puertoMaderoInfectadoAislaLaReserva() {
        // RES (Reserva Ecologica) solo conecta con PMA: sin PMA no se llega
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> dijkstra("OBE", "RES", "PMA"));
        assertEquals("No hay camino de OBE a RES", e.getMessage());
    }

    @Test
    void constitucionInfectadaObligaAUnDesvioMasLargo() {
        // normal: LBO-CON-CGR-ABA-HIT = 32 + 33 + 27 + 15 = 107
        Camino normal = dijkstra("LBO", "HIT");
        assertEquals(List.of("LBO", "CON", "CGR", "ABA", "HIT"), normal.vertices());
        assertEquals(107, normal.costoTotal(), 1e-9);
        // sin CON: LBO-HGA-BOE-ABA-HIT = 51 + 36 + 36 + 15 = 138 (usa la calle lenta HGA-LBO)
        Camino desvio = dijkstra("LBO", "HIT", "CON");
        assertEquals(List.of("LBO", "HGA", "BOE", "ABA", "HIT"), desvio.vertices());
        assertEquals(138, desvio.costoTotal(), 1e-9);
    }

    @Test
    void congresoInfectadoDesviaPorRecoleta82() {
        // normal: OBE-CGR-ABA-HIT = 60. Sin CGR: OBE-REC-ABA-HIT = 31 + 36 + 15 = 82
        Camino c = dijkstra("OBE", "HIT", "CGR");
        assertEquals(List.of("OBE", "REC", "ABA", "HIT"), c.vertices());
        assertEquals(82, c.costoTotal(), 1e-9);
    }

    @Test
    void planetarioInfectadoRodeaPorBelgrano199() {
        // normal: RET-REC-PAL-PLA-AEP-CUN = 154. Sin PLA se rodea por Chacarita y Belgrano:
        // RET-REC-PAL-CHA-COL-BEL-MON-CUN = 26 + 40 + 43 + 19 + 27 + 24 + 20 = 199
        Camino c = dijkstra("RET", "CUN", "PLA");
        assertEquals(List.of("RET", "REC", "PAL", "CHA", "COL", "BEL", "MON", "CUN"), c.vertices());
        assertEquals(199, c.costoTotal(), 1e-9);
    }

    @Test
    void variosInfectados() {
        // sin CON ni BOE, HGA queda en un callejon: LBO-SAN-PMA-OBE-CGR-ABA-HIT
        // = 34 + 20 + 28 + 18 + 27 + 15 = 142
        Camino c = dijkstra("LBO", "HIT", "CON", "BOE");
        assertEquals(List.of("LBO", "SAN", "PMA", "OBE", "CGR", "ABA", "HIT"), c.vertices());
        assertEquals(142, c.costoTotal(), 1e-9);
    }

    @Test
    void conjuntoVacioEsDijkstraNormal() {
        Camino normal = caminosMinimos.dijkstra(caba, "RET", "CUN");
        assertEquals(normal, dijkstra("RET", "CUN"));
        assertEquals(List.of("RET", "REC", "PAL", "PLA", "AEP", "CUN"), normal.vertices());
        assertEquals(154, normal.costoTotal(), 1e-9);
        assertEquals(normal, caminosMinimos.dijkstra(caba, "RET", "CUN", null));
    }

    @Test
    void origenODestinoInfectadoFalla() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> dijkstra("PAL", "CUN", "PAL"));
        assertTrue(e.getMessage().contains("origen PAL esta infectado"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> dijkstra("PAL", "CUN", "CUN"));
        assertTrue(e.getMessage().contains("destino CUN esta infectado"), e.getMessage());
    }

    @Test
    void infectadoInexistenteFalla() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> dijkstra("PAL", "CUN", "XXX"));
        assertEquals("No existe el nodo con id: XXX", e.getMessage());
    }

    // ---------- Backtracking ----------

    @Test
    void rutasSinConstitucion() {
        // de las 5 rutas LBO->HIT (140 min, 5 saltos) solo LBO-HGA-BOE-ABA-HIT (138) no pasa por CON
        ResultadoRutas r = backtracking.rutasSimples(caba, "LBO", "HIT", 140, 5, Set.of("CON"));
        assertEquals(List.of(new Ruta(List.of("LBO", "HGA", "BOE", "ABA", "HIT"), 138)), r.rutas());
        // bloquear un nodo achica el arbol en las dos corridas (106 -> 22 y 522.659 -> 71.600)
        assertEquals(22, r.nodosConPoda());
        assertEquals(71_600, r.nodosSinPoda());
        ResultadoRutas normal = backtracking.rutasSimples(caba, "LBO", "HIT", 140, 5);
        assertTrue(r.nodosConPoda() < normal.nodosConPoda());
        assertTrue(r.nodosSinPoda() < normal.nodosSinPoda());
    }

    @Test
    void rutasConDestinoInfectadoFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> backtracking.rutasSimples(caba, "RET", "CUN", 200, 8, Set.of("CUN")));
        assertThrows(IllegalArgumentException.class,
                () -> backtracking.rutasSimples(caba, "RET", "CUN", 200, 8, Set.of("XXX")));
    }
}
