package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.service.Grafo;
import uade.prog3.tpo.service.Mapa;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** DTO del mapa de la demo, armado en memoria (sin Neo4j). */
class MapaTest {

    @Test
    void dominioCabaTiene31LugaresY51CallesSinRepetir() {
        Mapa mapa = Mapa.desde(GrafosDePrueba.dominioCaba(), List.of());
        assertEquals(31, mapa.lugares().size());
        assertEquals(51, mapa.calles().size(), "cada calle una sola vez, no las 102 entradas");
        double total = 0;
        for (Mapa.Calle c : mapa.calles()) {
            total += c.costo();
        }
        assertEquals(1782.0, total, 1e-9, "suma de los 51 costos del dominio");
    }

    @Test
    void usaNombreYTipoDelNodoYCaeAlIdSiFalta() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 7);
        Mapa mapa = Mapa.desde(g, List.of(new Nodo("A", "Abasto", "REFUGIO", 400)));

        assertEquals(new Mapa.Lugar("A", "Abasto", "REFUGIO", 400), mapa.lugares().get(0));
        assertEquals(new Mapa.Lugar("B", "B", "PUNTO", 0), mapa.lugares().get(1));
        assertEquals(List.of(new Mapa.Calle("A", "B", 7)), mapa.calles());
    }

    @Test
    void lugarAisladoAparecePeroSinCalles() {
        Grafo g = new Grafo(false);
        g.agregarVertice("ZZZ");
        Mapa mapa = Mapa.desde(g, List.of());
        assertEquals(1, mapa.lugares().size());
        assertTrue(mapa.calles().isEmpty());
    }
}
