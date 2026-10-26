package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.CaminosMinimos.Camino;
import uade.prog3.tpo.algorithm.CaminosMinimos.Comparacion;
import uade.prog3.tpo.service.Grafo;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Floyd-Warshall con resultados calculados a mano. No tocan Neo4j.
 * Costos del dominio en minutos a pie (ver DominioCaba.CALLES).
 */
class FloydWarshallTest {

    private final CaminosMinimos caminosMinimos = new CaminosMinimos();

    private Map<String, Double> floydDominio() {
        return caminosMinimos.floydWarshall(GrafosDePrueba.dominioCaba());
    }

    @Test
    void dominioTieneTodosLosParesYDiagonalCero() {
        Map<String, Double> d = floydDominio();
        assertEquals(144, d.size()); // 12 x 12: el dominio es conexo
        for (String id : GrafosDePrueba.dominioCaba().ids()) {
            assertEquals(0.0, d.get(id + "->" + id));
        }
    }

    @Test
    void dominioCoincideConLosCalculosAMano() {
        Map<String, Double> d = floydDominio();
        assertEquals(129.0, d.get("RET->CUN")); // RET-PAL-MON-CUN 45+66+18
        assertEquals(84.0, d.get("PAL->CUN"));  // PAL-MON-CUN 66+18 < directo 90
        assertEquals(183.0, d.get("LBO->CUN")); // LBO-PMA-RET-PAL-MON-CUN
        assertEquals(165.0, d.get("LBO->MON"));
        assertEquals(165.0, d.get("MON->LBO")); // no dirigido: simetrico
        assertEquals(66.0, d.get("CON->PCE"));  // CON-ABA-PCE 36+30
        assertEquals(78.0, d.get("PCE->PMA"));  // PCE-ABA-OBE-PMA 30+28+20
    }

    @Test
    void sumaDeTodasLasDistancias() {
        double suma = 0;
        for (double costo : floydDominio().values()) {
            suma += costo;
        }
        assertEquals(9206.0, suma, 1e-9);
    }

    @Test
    void coincideConDijkstraEnTodosLosPares() {
        Grafo g = GrafosDePrueba.dominioCaba();
        Map<String, Double> d = caminosMinimos.floydWarshall(g);
        for (String origen : g.ids()) {
            for (String destino : g.ids()) {
                assertEquals(caminosMinimos.dijkstra(g, origen, destino).costoTotal(),
                        d.get(origen + "->" + destino), 1e-9, origen + "->" + destino);
            }
        }
    }

    @Test
    void semillaCatedra() {
        Map<String, Double> d = caminosMinimos.floydWarshall(GrafosDePrueba.semillaCatedra());
        assertEquals(20.0, d.get("A->H")); // A-B-C-D-F-H 4+3+2+8+3
        assertEquals(7.0, d.get("A->C"));  // A-B-C 4+3 < directo 9
    }

    @Test
    void cicloNegativoDirigidoFalla() {
        // A->B->C->A cuesta 1 - 3 + 1 = -1
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", -3);
        g.agregarArista("C", "A", 1);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> caminosMinimos.floydWarshall(g));
        assertEquals("Hay un ciclo de costo negativo", e.getMessage());
    }

    @Test
    void aristaNegativaNoDirigidaEsCicloNegativo() {
        // B-C-B (ida y vuelta) cuesta -2
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 2);
        g.agregarArista("B", "C", -1);
        assertThrows(IllegalStateException.class, () -> caminosMinimos.floydWarshall(g));
    }

    @Test
    void aristaNegativaSinCicloSeAcepta() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", -2);
        g.agregarArista("A", "C", 3);
        Map<String, Double> d = caminosMinimos.floydWarshall(g);
        assertEquals(-1.0, d.get("A->C")); // A->B->C 1 - 2 < directo 3
        assertEquals(1.0, d.get("A->B"));
        assertFalse(d.containsKey("B->A")); // inalcanzable: la clave no esta
    }

    // ------------------------------------------------- reconstruccion del camino

    private void verificarCamino(Grafo g, String origen, String destino, double costo, List<String> vertices) {
        Camino camino = caminosMinimos.caminoFloyd(g, origen, destino);
        assertEquals(costo, camino.costoTotal(), 1e-9);
        assertEquals(vertices, camino.vertices());
    }

    @Test
    void caminoPalermoCiudadUniversitaria() {
        verificarCamino(GrafosDePrueba.dominioCaba(), "PAL", "CUN", 84, List.of("PAL", "MON", "CUN"));
    }

    @Test
    void caminoRetiroCiudadUniversitaria() {
        verificarCamino(GrafosDePrueba.dominioCaba(), "RET", "CUN", 129, List.of("RET", "PAL", "MON", "CUN"));
    }

    @Test
    void caminoLaBocaCiudadUniversitariaIgualADijkstra() {
        Grafo g = GrafosDePrueba.dominioCaba();
        Camino dijkstra = caminosMinimos.dijkstra(g, "LBO", "CUN");
        verificarCamino(g, "LBO", "CUN", dijkstra.costoTotal(), dijkstra.vertices());
    }

    @Test
    void caminoFloydIgualADijkstraEnTodosLosPares() {
        // en el dominio no hay empates de caminos minimos: tambien coinciden los vertices
        Grafo g = GrafosDePrueba.dominioCaba();
        for (String origen : g.ids()) {
            for (String destino : g.ids()) {
                Camino dijkstra = caminosMinimos.dijkstra(g, origen, destino);
                Camino floyd = caminosMinimos.caminoFloyd(g, origen, destino);
                assertEquals(dijkstra.costoTotal(), floyd.costoTotal(), 1e-9, origen + "->" + destino);
                assertEquals(dijkstra.vertices(), floyd.vertices(), origen + "->" + destino);
            }
        }
    }

    @Test
    void caminoSemillaCatedra() {
        verificarCamino(GrafosDePrueba.semillaCatedra(), "A", "H", 20, List.of("A", "B", "C", "D", "F", "H"));
    }

    @Test
    void caminoInalcanzableFalla() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 1);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> caminosMinimos.caminoFloyd(g, "B", "A"));
        assertEquals("No hay camino de B a A", e.getMessage());
    }

    @Test
    void caminoConCicloNegativoFalla() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "A", -3);
        assertThrows(IllegalStateException.class, () -> caminosMinimos.caminoFloyd(g, "A", "B"));
    }

    @Test
    void caminoNodoInexistenteFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> caminosMinimos.caminoFloyd(GrafosDePrueba.dominioCaba(), "XXX", "CUN"));
    }

    @Test
    void comparacionConDijkstraEnElDominio() {
        Comparacion c = caminosMinimos.compararConDijkstra(GrafosDePrueba.dominioCaba());
        assertEquals(12, c.vertices());
        assertEquals(1728, c.operacionesFloyd());          // 12^3
        assertEquals(144, c.nodosExpandidosDijkstra());    // 12 corridas x 12 nodos
        assertEquals(504, c.aristasExaminadasDijkstra());  // 12 corridas x 2*21 entradas de adyacencia
    }
}
