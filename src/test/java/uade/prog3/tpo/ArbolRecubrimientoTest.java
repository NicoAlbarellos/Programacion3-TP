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

    /** MST del dominio: unico, 30 aristas, 835 minutos (en el orden en que las acepta Kruskal). */
    private static final Set<String> MST_DOMINIO = Set.of(
            "ABA-HIT 15.0", "CON-HGA 15.0", "PMA-RES 15.0", "CGR-OBE 18.0", "CON-SAN 18.0",
            "CHA-COL 19.0", "CAB-PCE 20.0", "CUN-MON 20.0", "HIT-PCE 20.0", "PMA-SAN 20.0",
            "AEP-PLA 21.0", "OBE-RET 23.0", "PAL-PLA 23.0", "BEL-MON 24.0", "REC-RET 26.0",
            "ABA-CGR 27.0", "BEL-COL 27.0", "BOE-CAB 27.0", "OBE-PMA 28.0", "CAB-PCH 29.0",
            "AGR-VUR 31.0", "CON-LBO 32.0", "SAA-VUR 35.0", "FLO-PCH 36.0", "CHA-PCE 37.0",
            "PAL-REC 40.0", "AGR-DEV 43.0", "MON-SAA 43.0", "MAT-VEL 45.0", "DEV-VEL 58.0");

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
        verificar(arbol.prim(GrafosDePrueba.dominioCaba(), "RET"), 30, 835, MST_DOMINIO);
    }

    @Test
    void primDominioDaElMismoArbolDesdeOtroOrigen() {
        verificar(arbol.prim(GrafosDePrueba.dominioCaba(), "CUN"), 30, 835, MST_DOMINIO);
    }

    @Test
    void kruskalDominio() {
        verificar(arbol.kruskal(GrafosDePrueba.dominioCaba()), 30, 835, MST_DOMINIO);
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
