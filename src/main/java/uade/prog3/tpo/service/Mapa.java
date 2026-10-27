package uade.prog3.tpo.service;

import uade.prog3.tpo.model.Nodo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Foto del grafo para dibujarlo en la demo (GET /api/mapa).
 *
 * No es un algoritmo del TP: solo traduce el Grafo en memoria y los datos de
 * cada Nodo (nombre, tipo, valor) a un formato comodo para el front.
 * Cada calle aparece UNA sola vez aunque el grafo no dirigido la guarde en
 * las dos listas de adyacencia. Costo O(V + E).
 */
public record Mapa(List<Lugar> lugares, List<Calle> calles) {

    /** Lugar del mapa. valor = capacidad del refugio (personas). */
    public record Lugar(String id, String nombre, String tipo, double valor) { }

    /** Calle del mapa, con su costo en minutos a pie. */
    public record Calle(String origen, String destino, double costo) { }

    /**
     * @param grafo grafo ya cargado (los ids salen en el orden del grafo)
     * @param nodos nodos de la base, para nombre/tipo/valor; si falta alguno
     *              el lugar igual aparece con su id como nombre
     */
    public static Mapa desde(Grafo grafo, List<Nodo> nodos) {
        return new Mapa(lugares(grafo, nodos), calles(grafo));
    }

    private static List<Lugar> lugares(Grafo grafo, List<Nodo> nodos) {
        Map<String, Nodo> porId = new HashMap<>();
        for (Nodo n : nodos) {
            porId.put(n.getId(), n);
        }
        List<Lugar> lugares = new ArrayList<>();
        for (String id : grafo.ids()) {
            Nodo n = porId.get(id);
            lugares.add(n == null
                    ? new Lugar(id, id, "PUNTO", 0)
                    : new Lugar(id, n.getNombre(), n.getTipo(), n.getValor()));
        }
        return lugares;
    }

    private static List<Calle> calles(Grafo grafo) {
        List<Calle> calles = new ArrayList<>();
        for (int u = 0; u < grafo.cantidadVertices(); u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                // en no dirigido cada calle esta en las dos listas: se emite desde el extremo menor
                if (grafo.esDirigido() || u < a.destino()) {
                    calles.add(new Calle(grafo.idDe(u), grafo.idDe(a.destino()), a.costo()));
                }
            }
        }
        return calles;
    }
}
