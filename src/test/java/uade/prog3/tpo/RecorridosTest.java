package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.service.Grafo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de BFS y DFS con resultados calculados a mano. No tocan Neo4j.
 *
 * Listas de adyacencia del dominio (orden de carga):
 *   ABA: HIT PCE REC CON OBE      OBE: ABA CON PMA REC RET
 *   CON: ABA LBO OBE PMA          PAL: HIT CUN MON REC RET
 *   CUN: MON PAL                  PCE: ABA HIT
 *   HIT: ABA PAL PCE              PMA: OBE CON LBO RET
 *   LBO: CON PMA                  REC: ABA OBE PAL RET
 *   MON: CUN PAL                  RET: OBE PAL PMA REC
 */
class RecorridosTest {

    private final Recorridos recorridos = new Recorridos();

    @Test
    void bfsSemillaCatedra() {
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G", "H"),
                recorridos.bfs(GrafosDePrueba.semillaCatedra(), "A"));
    }

    @Test
    void dfsSemillaCatedra() {
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "H", "G"),
                recorridos.dfs(GrafosDePrueba.semillaCatedra(), "A"));
    }

    @Test
    void bfsDominioDesdeRetiro() {
        // nivel 0: RET | nivel 1: OBE PAL PMA REC | nivel 2: ABA CON (de OBE),
        // HIT CUN MON (de PAL), LBO (de PMA) | nivel 3: PCE (de ABA)
        assertEquals(List.of("RET", "OBE", "PAL", "PMA", "REC",
                        "ABA", "CON", "HIT", "CUN", "MON", "LBO", "PCE"),
                recorridos.bfs(GrafosDePrueba.dominioCaba(), "RET"));
    }

    @Test
    void dfsDominioDesdeRetiro() {
        // RET > OBE > ABA > HIT > PAL > CUN > MON, retrocede a PAL > REC,
        // retrocede a HIT > PCE, retrocede a ABA > CON > LBO > PMA
        assertEquals(List.of("RET", "OBE", "ABA", "HIT", "PAL", "CUN",
                        "MON", "REC", "PCE", "CON", "LBO", "PMA"),
                recorridos.dfs(GrafosDePrueba.dominioCaba(), "RET"));
    }

    @Test
    void nodoAisladoDevuelveSoloElOrigen() {
        Grafo g = GrafosDePrueba.dominioCaba();
        g.agregarVertice("ZZZ");
        assertEquals(List.of("ZZZ"), recorridos.bfs(g, "ZZZ"));
        assertEquals(List.of("ZZZ"), recorridos.dfs(g, "ZZZ"));
    }

    @Test
    void noVisitaOtraComponente() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 1);
        g.agregarArista("X", "Y", 1);
        assertEquals(List.of("A", "B", "C"), recorridos.bfs(g, "A"));
        assertEquals(List.of("A", "B", "C"), recorridos.dfs(g, "A"));
        assertEquals(List.of("X", "Y"), recorridos.bfs(g, "X"));
    }

    @Test
    void origenInexistenteFalla() {
        Grafo g = GrafosDePrueba.dominioCaba();
        assertThrows(IllegalArgumentException.class, () -> recorridos.bfs(g, "XXX"));
        assertThrows(IllegalArgumentException.class, () -> recorridos.dfs(g, "XXX"));
    }

    @Test
    void dominioTiene12VerticesY21Aristas() {
        Grafo g = GrafosDePrueba.dominioCaba();
        assertEquals(12, g.cantidadVertices());
        assertEquals(21, g.cantidadAristas());
    }
}
