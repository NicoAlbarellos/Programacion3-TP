package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.CaminosMinimos.Camino;
import uade.prog3.tpo.algorithm.CaminosMinimos.ResultadoUcs;
import uade.prog3.tpo.service.Grafo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de UCS con resultados calculados a mano. No tocan Neo4j.
 * UCS tiene que dar el mismo camino y costo que Dijkstra, expandiendo igual o menos nodos.
 * El dominio tiene 12 lugares, todos alcanzables: Dijkstra expande siempre 12.
 */
class UcsTest {

    private final CaminosMinimos caminosMinimos = new CaminosMinimos();

    private ResultadoUcs verificar(Grafo g, String origen, String destino, double costo, List<String> vertices) {
        ResultadoUcs ucs = caminosMinimos.ucs(g, origen, destino);
        Camino dijkstra = caminosMinimos.dijkstra(g, origen, destino);
        assertEquals(costo, ucs.costoTotal(), 1e-9);
        assertEquals(vertices, ucs.vertices());
        assertEquals(dijkstra.costoTotal(), ucs.costoTotal(), 1e-9);
        assertEquals(dijkstra.vertices(), ucs.vertices());
        assertTrue(ucs.nodosExpandidosUcs() <= ucs.nodosExpandidosDijkstra());
        return ucs;
    }

    @Test
    void palermoACiudadUniversitaria() {
        // PAL-MON 66 + MON-CUN 18 = 84. CUN se descubre primero por el directo (90),
        // pero recien sale de la cola con 84. Queda sin expandir LBO (el mas lejano de PAL).
        ResultadoUcs r = verificar(GrafosDePrueba.dominioCaba(), "PAL", "CUN", 84, List.of("PAL", "MON", "CUN"));
        assertEquals(11, r.nodosExpandidosUcs());
        assertEquals(12, r.nodosExpandidosDijkstra());
    }

    @Test
    void retiroACiudadUniversitariaExpandeTodo() {
        // RET-PAL 45 + PAL-MON 66 + MON-CUN 18 = 129. CUN es el mas lejano de RET:
        // sale ultimo de la cola, asi que UCS no ahorra nada (peor caso = Dijkstra).
        ResultadoUcs r = verificar(GrafosDePrueba.dominioCaba(), "RET", "CUN", 129,
                List.of("RET", "PAL", "MON", "CUN"));
        assertEquals(12, r.nodosExpandidosUcs());
        assertEquals(12, r.nodosExpandidosDijkstra());
    }

    @Test
    void laBocaAHospitalItalianoCortaTemprano() {
        // LBO-CON 29 + CON-ABA 36 + ABA-HIT 19 = 84.
        // Orden de salida: LBO 0, CON 29, PMA 36, RET 54, OBE 56, ABA 65, REC 74, HIT 84 -> fin.
        // Quedan sin expandir PCE 95, PAL 99, MON y CUN: 8 contra 12.
        ResultadoUcs r = verificar(GrafosDePrueba.dominioCaba(), "LBO", "HIT", 84,
                List.of("LBO", "CON", "ABA", "HIT"));
        assertEquals(8, r.nodosExpandidosUcs());
        assertEquals(12, r.nodosExpandidosDijkstra());
    }

    @Test
    void retiroAPuertoMaderoVecinoMasCercano() {
        // RET-PMA 18 es la arista mas barata de RET: sale RET 0, luego PMA 18 -> fin.
        ResultadoUcs r = verificar(GrafosDePrueba.dominioCaba(), "RET", "PMA", 18, List.of("RET", "PMA"));
        assertEquals(2, r.nodosExpandidosUcs());
    }

    @Test
    void semillaCatedraComoEnLaClase13() {
        // Clase 13: salen A0, B4, C7, D9, E14, F17, H20 -> fin. G queda en la cola con 24.
        ResultadoUcs r = verificar(GrafosDePrueba.semillaCatedra(), "A", "H", 20,
                List.of("A", "B", "C", "D", "F", "H"));
        assertEquals(7, r.nodosExpandidosUcs());
        assertEquals(8, r.nodosExpandidosDijkstra());
    }

    @Test
    void origenIgualDestinoExpandeSoloUno() {
        ResultadoUcs r = caminosMinimos.ucs(GrafosDePrueba.dominioCaba(), "RET", "RET");
        assertEquals(List.of("RET"), r.vertices());
        assertEquals(0, r.costoTotal(), 1e-9);
        assertEquals(1, r.nodosExpandidosUcs());
    }

    @Test
    void costoNegativoLanzaIllegalArgument() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 2);
        g.agregarArista("B", "C", -1);
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.ucs(g, "A", "C"));
    }

    @Test
    void destinoInalcanzableLanzaIllegalState() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 2);
        g.agregarVertice("Z");
        assertThrows(IllegalStateException.class, () -> caminosMinimos.ucs(g, "A", "Z"));
    }

    @Test
    void nodoInexistenteLanzaIllegalArgument() {
        Grafo g = GrafosDePrueba.dominioCaba();
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.ucs(g, "XXX", "CUN"));
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.ucs(g, "RET", "XXX"));
    }
}
