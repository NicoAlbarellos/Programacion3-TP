package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.service.Grafo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de BFS y DFS con resultados calculados a mano. No tocan Neo4j.
 *
 * Listas de adyacencia del dominio (orden de carga, por id):
 *   ABA: BOE CGR HIT REC       FLO: CAB MAT PCH VEL       PCH: BOE CAB FLO
 *   AEP: BEL CUN PLA           HGA: BOE CON LBO           PLA: AEP COL PAL
 *   AGR: CHA DEV VUR           HIT: ABA PAL PCE           PMA: OBE RES RET SAN
 *   BEL: AEP COL MON SAA       LBO: CON HGA SAN           REC: ABA OBE PAL RET
 *   BOE: ABA CAB HGA PCH       MAT: FLO VEL               RES: PMA
 *   CAB: BOE FLO PCE PCH       MON: BEL CUN SAA           RET: OBE PMA REC
 *   CGR: ABA CON OBE           OBE: CGR CON PMA REC RET   SAA: BEL MON VUR
 *   CHA: AGR COL PAL PCE       PAL: CHA HIT PLA REC       SAN: CON LBO PMA
 *   COL: BEL CHA PLA VUR       PCE: CAB CHA HIT           VEL: DEV FLO MAT
 *   CON: CGR HGA LBO OBE SAN                              VUR: AGR COL SAA
 *   CUN: AEP MON
 *   DEV: AGR VEL
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
        // nivel 0: RET | 1: OBE PMA REC | 2: CGR CON (de OBE), RES SAN (de PMA), ABA PAL (de REC)
        // 3: HGA LBO (de CON), BOE HIT (de ABA), CHA PLA (de PAL)
        // 4: CAB PCH (de BOE), PCE (de HIT), AGR COL (de CHA), AEP (de PLA)
        // 5: FLO (de CAB), DEV VUR (de AGR), BEL (de COL), CUN (de AEP)
        // 6: MAT VEL (de FLO), SAA (de VUR), MON (de BEL)
        assertEquals(List.of("RET", "OBE", "PMA", "REC",
                        "CGR", "CON", "RES", "SAN", "ABA", "PAL",
                        "HGA", "LBO", "BOE", "HIT", "CHA", "PLA",
                        "CAB", "PCH", "PCE", "AGR", "COL", "AEP",
                        "FLO", "DEV", "VUR", "BEL", "CUN",
                        "MAT", "VEL", "SAA", "MON"),
                recorridos.bfs(GrafosDePrueba.dominioCaba(), "RET"));
    }

    @Test
    void dfsDominioDesdeRetiro() {
        // RET > OBE > CGR > ABA > BOE > CAB > FLO > MAT > VEL > DEV > AGR > CHA > COL > BEL
        // > AEP > CUN > MON > SAA > VUR, retrocede a COL > PLA > PAL > HIT > PCE,
        // retrocede a PAL > REC, retrocede a CAB > PCH, retrocede a BOE > HGA > CON > LBO > SAN
        // > PMA > RES
        assertEquals(List.of("RET", "OBE", "CGR", "ABA", "BOE", "CAB", "FLO", "MAT",
                        "VEL", "DEV", "AGR", "CHA", "COL", "BEL", "AEP", "CUN",
                        "MON", "SAA", "VUR", "PLA", "PAL", "HIT", "PCE", "REC",
                        "PCH", "HGA", "CON", "LBO", "SAN", "PMA", "RES"),
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
    void dominioTiene31VerticesY51Aristas() {
        Grafo g = GrafosDePrueba.dominioCaba();
        assertEquals(31, g.cantidadVertices());
        assertEquals(51, g.cantidadAristas());
    }
}
