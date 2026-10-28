package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.seed.DominioCaba;
import uade.prog3.tpo.service.Grafo;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la matriz de adyacencia del mapa de CABA (la que usa Floyd-Warshall).
 */
class MatrizDominioTest {

    private final Grafo g = GrafosDePrueba.dominioCaba();
    private final double[][] m = g.matrizDeAdyacencia();

    private double costo(String origen, String destino) {
        return m[g.indiceDe(origen)][g.indiceDe(destino)];
    }

    @Test
    void laDiagonalValeCero() {
        for (int i = 0; i < m.length; i++) {
            assertEquals(0.0, m[i][i]);
        }
    }

    @Test
    void esSimetricaPorqueElGrafoNoEsDirigido() {
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m.length; j++) {
                assertEquals(m[i][j], m[j][i], "asimetria entre " + g.idDe(i) + " y " + g.idDe(j));
            }
        }
    }

    @Test
    void retiroObeliscoCuesta23EnLosDosSentidos() {
        assertEquals(23.0, costo("RET", "OBE"));
        assertEquals(23.0, costo("OBE", "RET"));
    }

    @Test
    void guardaElCostoDirectoAunqueNoSeaElMinimo() {
        // HGA-LBO directo cuesta 51; el camino minimo (47, por CON) lo calcula Floyd, no la matriz
        assertEquals(51.0, costo("HGA", "LBO"));
        assertEquals(51.0, costo("LBO", "HGA"));
    }

    @Test
    void sinCalleDirectaEsInfinito() {
        assertEquals(Grafo.INFINITO, costo("RET", "CUN"));
        assertEquals(Grafo.INFINITO, costo("PAL", "CUN"));
    }

    @Test
    void cadaCalleApareceEnLosVecinosDeSusDosExtremos() {
        int entradas = 0;
        for (int i = 0; i < g.cantidadVertices(); i++) {
            entradas += g.vecinos(i).size();
        }
        assertEquals(2 * DominioCaba.CALLES.size(), entradas);
    }
}
