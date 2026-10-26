package uade.prog3.tpo;

import uade.prog3.tpo.seed.DominioCaba;
import uade.prog3.tpo.service.Grafo;

/**
 * Grafos armados en memoria para los tests de algoritmos (sin Neo4j).
 *
 * El dominio se arma desde las MISMAS listas que usa la carga inicial
 * (DominioCaba), que ya estan en el orden de la consulta ORDER BY de
 * GrafoService. Asi el orden de las listas de adyacencia es el mismo que en la app.
 */
public final class GrafosDePrueba {

    private GrafosDePrueba() {
    }

    /** Dominio CABA ({@link DominioCaba}): costo en minutos a pie. No dirigido. */
    public static Grafo dominioCaba() {
        Grafo g = new Grafo(false);
        for (DominioCaba.Lugar l : DominioCaba.LUGARES) {
            g.agregarVertice(l.id());
        }
        for (DominioCaba.Calle c : DominioCaba.CALLES) {
            g.agregarArista(c.origen(), c.destino(), c.minutos());
        }
        return g;
    }

    /** Semilla original de la catedra (A..H, 12 aristas). Dirigido. */
    public static Grafo semillaCatedra() {
        return semillaCatedra(true);
    }

    /**
     * Semilla de la catedra, dirigida o no. La no dirigida es la que pide el MST
     * (7 aristas, costo 27 segun la clase 5).
     */
    public static Grafo semillaCatedra(boolean dirigido) {
        Grafo g = new Grafo(dirigido);
        g.agregarArista("A", "B", 4);
        g.agregarArista("A", "C", 9);
        g.agregarArista("B", "C", 3);
        g.agregarArista("B", "D", 7);
        g.agregarArista("C", "D", 2);
        g.agregarArista("C", "E", 11);
        g.agregarArista("D", "E", 5);
        g.agregarArista("D", "F", 8);
        g.agregarArista("E", "F", 6);
        g.agregarArista("E", "G", 10);
        g.agregarArista("F", "H", 3);
        g.agregarArista("G", "H", 4);
        return g;
    }
}
