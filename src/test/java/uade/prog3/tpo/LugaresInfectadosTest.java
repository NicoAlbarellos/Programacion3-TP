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
    void monumentalInfectadoTomaLaCalleDirecta() {
        // normal: PAL-MON-CUN = 66 + 18 = 84. Sin MON solo queda la calle directa PAL-CUN = 90
        Camino c = dijkstra("PAL", "CUN", "MON");
        assertEquals(List.of("PAL", "CUN"), c.vertices());
        assertEquals(90, c.costoTotal(), 1e-9);
    }

    @Test
    void palermoInfectadoAislaCiudadUniversitaria() {
        // CUN solo conecta con MON y PAL; MON solo con PAL y CUN: sin PAL no se llega
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> dijkstra("RET", "CUN", "PAL"));
        assertEquals("No hay camino de RET a CUN", e.getMessage());
    }

    @Test
    void constitucionInfectadaObligaAUnDesvioMasLargo() {
        // normal: LBO-CON-ABA-HIT = 29 + 36 + 19 = 84
        Camino normal = dijkstra("LBO", "HIT");
        assertEquals(List.of("LBO", "CON", "ABA", "HIT"), normal.vertices());
        assertEquals(84, normal.costoTotal(), 1e-9);
        // sin CON: LBO-PMA-OBE-ABA-HIT = 36 + 20 + 28 + 19 = 103
        Camino desvio = dijkstra("LBO", "HIT", "CON");
        assertEquals(List.of("LBO", "PMA", "OBE", "ABA", "HIT"), desvio.vertices());
        assertEquals(103, desvio.costoTotal(), 1e-9);
    }

    @Test
    void variosInfectados() {
        // sin CON ni ABA: LBO-PMA-RET-PAL-HIT = 36 + 18 + 45 + 21 = 120
        // (LBO-PMA-OBE-REC-PAL-HIT = 36 + 20 + 25 + 30 + 21 = 132)
        Camino c = dijkstra("LBO", "HIT", "CON", "ABA");
        assertEquals(List.of("LBO", "PMA", "RET", "PAL", "HIT"), c.vertices());
        assertEquals(120, c.costoTotal(), 1e-9);
    }

    @Test
    void conjuntoVacioEsDijkstraNormal() {
        Camino normal = caminosMinimos.dijkstra(caba, "RET", "CUN");
        assertEquals(normal, dijkstra("RET", "CUN"));
        assertEquals(List.of("RET", "PAL", "MON", "CUN"), normal.vertices());
        assertEquals(129, normal.costoTotal(), 1e-9);
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
    void rutasSinRecoleta() {
        // de las 4 rutas RET->CUN (150 min, 4 saltos) quedan las 2 que no pasan por REC
        ResultadoRutas r = backtracking.rutasSimples(caba, "RET", "CUN", 150, 4, Set.of("REC"));
        assertEquals(List.of(
                new Ruta(List.of("RET", "PAL", "CUN"), 135),
                new Ruta(List.of("RET", "PAL", "MON", "CUN"), 129)), r.rutas());
        // bloquear un nodo achica el arbol en las dos corridas (sin REC: 118 -> menos, 684 -> menos)
        ResultadoRutas normal = backtracking.rutasSimples(caba, "RET", "CUN", 150, 4);
        assertTrue(r.nodosConPoda() < normal.nodosConPoda());
        assertTrue(r.nodosSinPoda() < normal.nodosSinPoda());
    }

    @Test
    void rutasConDestinoInfectadoFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> backtracking.rutasSimples(caba, "RET", "CUN", 150, 4, Set.of("CUN")));
        assertThrows(IllegalArgumentException.class,
                () -> backtracking.rutasSimples(caba, "RET", "CUN", 150, 4, Set.of("XXX")));
    }
}
