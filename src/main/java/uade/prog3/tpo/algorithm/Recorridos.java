package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * UNIDAD: Grafos I (clase 4)          PUNTAJE: 2 puntos
 *
 * Recorridos sobre grafos. En el dominio: que refugios de CABA son
 * alcanzables a pie desde un punto, y en que orden se los descubre.
 */
@Component
public class Recorridos {

    /**
     * Recorrido en profundidad (DFS) recursivo desde un vertice.
     *
     * Complejidad: O(V + E) con lista de adyacencia. Cada vertice entra una
     * sola vez a la recursion (se marca visitado al entrar) y cada arista se
     * mira una vez por extremo al recorrer las listas de vecinos.
     * Espacio: O(V) por el arreglo de visitados y la pila de llamadas.
     *
     * @param grafo grafo cargado en memoria
     * @param origenId id del vertice de partida
     * @return lista de ids en orden de visita
     * @throws IllegalArgumentException si el origen no existe
     */
    public List<String> dfs(Grafo grafo, String origenId) {
        int origen = grafo.indiceDe(origenId);
        boolean[] visitado = new boolean[grafo.cantidadVertices()];
        List<String> orden = new ArrayList<>();
        dfsRecursivo(grafo, origen, visitado, orden);
        return orden;
    }

    private void dfsRecursivo(Grafo grafo, int vertice, boolean[] visitado, List<String> orden) {
        visitado[vertice] = true;
        orden.add(grafo.idDe(vertice));
        for (Grafo.Arista arista : grafo.vecinos(vertice)) {
            if (!visitado[arista.destino()]) {
                // se profundiza; al volver de la llamada se retrocede al vertice actual
                dfsRecursivo(grafo, arista.destino(), visitado, orden);
            }
        }
    }

    /**
     * Recorrido en anchura (BFS) iterativo con cola FIFO desde un vertice.
     *
     * Complejidad: O(V + E) con lista de adyacencia. Como se marca visitado
     * AL ENCOLAR, cada vertice entra a la cola una sola vez, y cada arista se
     * mira una vez por extremo al desencolar su vertice.
     * Espacio: O(V) por la cola y el arreglo de visitados.
     *
     * Da el camino minimo en CANTIDAD DE ARISTAS, no en minutos: para costos
     * hace falta Dijkstra.
     *
     * @param grafo grafo cargado en memoria
     * @param origenId id del vertice de partida
     * @return lista de ids en orden de visita
     * @throws IllegalArgumentException si el origen no existe
     */
    public List<String> bfs(Grafo grafo, String origenId) {
        int origen = grafo.indiceDe(origenId);
        boolean[] visitado = new boolean[grafo.cantidadVertices()];
        List<String> orden = new ArrayList<>();
        Queue<Integer> cola = new ArrayDeque<>();

        visitado[origen] = true;
        cola.add(origen);
        while (!cola.isEmpty()) {
            int vertice = cola.poll();
            orden.add(grafo.idDe(vertice));
            for (Grafo.Arista arista : grafo.vecinos(vertice)) {
                int vecino = arista.destino();
                if (!visitado[vecino]) {
                    visitado[vecino] = true; // marcar AL ENCOLAR: nadie entra dos veces
                    cola.add(vecino);
                }
            }
        }
        return orden;
    }
}
