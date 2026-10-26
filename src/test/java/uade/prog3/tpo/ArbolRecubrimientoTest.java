package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento.AristaMst;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento.Mst;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.service.Grafo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Prim y Kruskal con el MST calculado a mano (Kruskal en papel:
 * aristas por costo creciente, salteando las que cierran ciclo). No tocan Neo4j.
 *
 * Las aristas se comparan como CONJUNTO con clave normalizada "min-max costo" (costo como double, sin truncar),
 * porque Prim y Kruskal las agregan en distinto orden y sentido.
 */
class ArbolRecubrimientoTest {

    private final ArbolRecubrimiento arbol = new ArbolRecubrimiento(new Ordenamiento());

    /** MST del dominio: unico, 11 aristas, 287 minutos. */
    private static final Set<String> MST_DOMINIO = Set.of(
            "HIT-PCE 18.0", "CUN-MON 18.0", "PMA-RET 18.0", "ABA-HIT 19.0", "OBE-PMA 20.0", "REC-RET 20.0",
            "HIT-PAL 21.0", "ABA-OBE 28.0", "CON-LBO 29.0", "CON-PMA 30.0", "MON-PAL 66.0");

    /** MST de la semilla no dirigida: 7 aristas, costo 27 (verificacion de la clase 5). */
    private static final Set<String> MST_SEMILLA = Set.of(
            "C-D 2.0", "B-C 3.0", "F-H 3.0", "A-B 4.0", "G-H 4.0", "D-E 5.0", "E-F 6.0");

    private static Set<String> claves(Mst mst) {
        Set<String> claves = new HashSet<>();
        for (AristaMst a : mst.aristas()) {
            boolean ordenada = a.origen().compareTo(a.destino()) <= 0;
            String min = ordenada ? a.origen() : a.destino();
            String max = ordenada ? a.destino() : a.origen();
            claves.add(min + "-" + max + " " + a.costo());
        }
        return claves;
    }

    private static void verificar(Mst mst, int aristas, double costo, Set<String> esperado) {
        assertEquals(aristas, mst.aristas().size());
        assertEquals(costo, mst.costoTotal(), 1e-9);
        assertEquals(esperado, claves(mst));
    }

    @Test
    void primDominioDesdeRetiro() {
        verificar(arbol.prim(GrafosDePrueba.dominioCaba(), "RET"), 11, 287, MST_DOMINIO);
    }

    @Test
    void primDominioDaElMismoArbolDesdeOtroOrigen() {
        verificar(arbol.prim(GrafosDePrueba.dominioCaba(), "CUN"), 11, 287, MST_DOMINIO);
    }

    @Test
    void kruskalDominio() {
        verificar(arbol.kruskal(GrafosDePrueba.dominioCaba()), 11, 287, MST_DOMINIO);
    }

    @Test
    void primSemillaNoDirigida() {
        verificar(arbol.prim(GrafosDePrueba.semillaCatedra(false), "A"), 7, 27, MST_SEMILLA);
    }

    @Test
    void kruskalSemillaNoDirigida() {
        verificar(arbol.kruskal(GrafosDePrueba.semillaCatedra(false)), 7, 27, MST_SEMILLA);
    }

    @Test
    void kruskalAgregaEnOrdenCrecienteDeCosto() {
        List<AristaMst> aristas = arbol.kruskal(GrafosDePrueba.semillaCatedra(false)).aristas();
        for (int i = 1; i < aristas.size(); i++) {
            assertTrue(aristas.get(i - 1).costo() <= aristas.get(i).costo());
        }
    }

    @Test
    void grafoNoConexoFalla() {
        Grafo g = GrafosDePrueba.dominioCaba();
        g.agregarVertice("ZZZ");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> arbol.kruskal(g));
        assertEquals("El grafo no es conexo", e.getMessage());
        assertThrows(IllegalStateException.class, () -> arbol.prim(g, "RET"));
    }

    @Test
    void grafoDirigidoFalla() {
        Grafo g = GrafosDePrueba.semillaCatedra(true);
        assertThrows(IllegalArgumentException.class, () -> arbol.prim(g, "A"));
        assertThrows(IllegalArgumentException.class, () -> arbol.kruskal(g));
    }

    @Test
    void primOrigenInexistenteFalla() {
        assertThrows(IllegalArgumentException.class, () -> arbol.prim(GrafosDePrueba.dominioCaba(), "XXX"));
    }
}
