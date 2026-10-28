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
 * El dominio tiene 31 lugares, todos alcanzables: Dijkstra expande siempre 31.
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
    void retiroAPuertoMadero() {
        // RET-PMA directo 36. Orden de salida: RET 0, OBE 23, REC 26, PMA 36 -> fin.
        ResultadoUcs r = verificar(GrafosDePrueba.dominioCaba(), "RET", "PMA", 36, List.of("RET", "PMA"));
        assertEquals(4, r.nodosExpandidosUcs());
        assertEquals(31, r.nodosExpandidosDijkstra());
    }

    @Test
    void garrahanALaBocaRelajaAntesDeSacarlo() {
        // LBO entra a la cola con 51 (directo) y baja a 47 al expandir CON.
        // Orden de salida: HGA 0, CON 15, SAN 33, BOE 36, LBO 47 -> fin.
        ResultadoUcs r = verificar(GrafosDePrueba.dominioCaba(), "HGA", "LBO", 47, List.of("HGA", "CON", "LBO"));
        assertEquals(5, r.nodosExpandidosUcs());
        assertEquals(31, r.nodosExpandidosDijkstra());
    }

    @Test
    void obeliscoAHospitalItalianoCortaTemprano() {
        // OBE-CGR 18 + CGR-ABA 27 + ABA-HIT 15 = 60. Salen los 10 lugares a menos de 60
        // (OBE 0, CGR 18, RET 23, PMA 28, REC 31, CON 40, RES 43, ABA 45, SAN 48, HGA 55)
        // y despues HIT 60: 11 contra 31.
        ResultadoUcs r = verificar(GrafosDePrueba.dominioCaba(), "OBE", "HIT", 60,
                List.of("OBE", "CGR", "ABA", "HIT"));
        assertEquals(11, r.nodosExpandidosUcs());
        assertEquals(31, r.nodosExpandidosDijkstra());
    }

    @Test
    void mataderosACiudadUniversitariaExpandeTodo() {
        // CUN es el lugar mas lejano de MAT (263): sale ultimo de la cola,
        // asi que UCS no ahorra nada (peor caso = Dijkstra).
        ResultadoUcs r = verificar(GrafosDePrueba.dominioCaba(), "MAT", "CUN", 263,
                List.of("MAT", "FLO", "CAB", "PCE", "CHA", "COL", "BEL", "MON", "CUN"));
        assertEquals(31, r.nodosExpandidosUcs());
        assertEquals(31, r.nodosExpandidosDijkstra());
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
