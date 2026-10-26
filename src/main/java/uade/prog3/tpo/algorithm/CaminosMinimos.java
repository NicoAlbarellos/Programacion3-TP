package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * UNIDAD: Grafos II (clase 5) y PD sobre grafos (clase 8)
 * PUNTAJE: forma parte de los 3 puntos de "Dijkstra, Prim, Kruskal"
 *          y de 1 punto de programacion dinamica si implementan Floyd.
 *
 * En el dominio: ruta mas rapida (en minutos a pie) entre dos refugios de CABA.
 */
@Component
public class CaminosMinimos {

    /** Resultado de una consulta de camino minimo. */
    public record Camino(List<String> vertices, double costoTotal) { }

    /** Entrada de la cola de prioridad: un vertice con la distancia con que se encolo. */
    private record EnCola(int vertice, double distancia) { }

    /**
     * Dijkstra: camino de costo minimo desde un origen hasta un destino.
     * Greedy: en cada paso se extrae el vertice no procesado con menor dist[]
     * y esa eleccion no se reconsidera. Por eso exige costos NO negativos.
     *
     * Cola de prioridad con eliminacion perezosa: java.util.PriorityQueue no
     * tiene decreaseKey (el Q.decreaseKey de la diapositiva). En su lugar, al
     * mejorar dist[v] se REINSERTA v con la nueva distancia, y al extraer una
     * entrada vieja (distancia > dist[u]) se la descarta.
     *
     * Se calcula el arbol de caminos minimos completo: NO corta al extraer el
     * destino. Cortar ahi es justamente lo que agrega UCS.
     *
     * Complejidad: O((V + E) log V). Cada relajacion exitosa encola una entrada,
     * asi que la cola tiene a lo sumo E + 1 entradas (el origen mas una por
     * relajacion): cada offer/poll cuesta O(log E), y log E <= log V^2 = 2 log V.
     * Validar costos recorre todas las listas, O(V + E), y
     * reconstruir el camino O(V). Espacio: O(V + E).
     *
     * @throws IllegalArgumentException si origen o destino no existen, o hay costos negativos
     * @throws IllegalStateException    si el destino no es alcanzable desde el origen
     */
    public Camino dijkstra(Grafo grafo, String origenId, String destinoId) {
        int origen = grafo.indiceDe(origenId);
        int destino = grafo.indiceDe(destinoId);
        validarCostosNoNegativos(grafo);

        int n = grafo.cantidadVertices();
        double[] dist = new double[n];
        int[] previo = new int[n];
        Arrays.fill(dist, Grafo.INFINITO);
        Arrays.fill(previo, -1);
        dist[origen] = 0;

        PriorityQueue<EnCola> cola = new PriorityQueue<>(Comparator.comparingDouble(EnCola::distancia));
        cola.add(new EnCola(origen, 0));
        while (!cola.isEmpty()) {
            EnCola actual = cola.poll();
            int u = actual.vertice();
            if (actual.distancia() > dist[u]) {
                continue; // entrada vieja: u ya se proceso con una distancia menor
            }
            relajarVecinos(grafo, u, dist, previo, cola);
        }

        if (dist[destino] == Grafo.INFINITO) {
            throw new IllegalStateException("No hay camino de " + origenId + " a " + destinoId);
        }
        return new Camino(reconstruirCamino(grafo, previo, destino), dist[destino]);
    }

    /** Relajacion: si pasar por u mejora la distancia a v, se actualiza y se reencola v. */
    private void relajarVecinos(Grafo grafo, int u, double[] dist, int[] previo, PriorityQueue<EnCola> cola) {
        for (Grafo.Arista arista : grafo.vecinos(u)) {
            int v = arista.destino();
            double alternativa = dist[u] + arista.costo();
            if (alternativa < dist[v]) {
                dist[v] = alternativa;
                previo[v] = u;
                cola.add(new EnCola(v, alternativa));
            }
        }
    }

    /** Recorre previo[] desde el destino hacia atras; addFirst deja el camino en orden. */
    private List<String> reconstruirCamino(Grafo grafo, int[] previo, int destino) {
        Deque<String> camino = new ArrayDeque<>();
        for (int v = destino; v != -1; v = previo[v]) {
            camino.addFirst(grafo.idDe(v));
        }
        return List.copyOf(camino);
    }

    private void validarCostosNoNegativos(Grafo grafo) {
        for (int u = 0; u < grafo.cantidadVertices(); u++) {
            for (Grafo.Arista arista : grafo.vecinos(u)) {
                if (arista.costo() < 0) {
                    throw new IllegalArgumentException("Dijkstra no admite costos negativos: "
                            + grafo.idDe(u) + "-" + grafo.idDe(arista.destino()) + " cuesta " + arista.costo());
                }
            }
        }
    }

    /**
     * Floyd-Warshall: costo minimo entre TODOS los pares de vertices.
     *
     * Requisitos:
     *   - trabajar sobre la matriz de adyacencia (grafo.matrizDeAdyacencia())
     *   - el bucle del vertice intermedio k va SIEMPRE por fuera de los de i y j
     *   - detectar ciclos de costo negativo revisando la diagonal al terminar
     *
     * Complejidad esperada: O(V^3) en tiempo, O(V^2) en espacio.
     *
     * Devuelve un mapa "origen->destino" con el costo minimo de cada par.
     */
    public Map<String, Double> floydWarshall(Grafo grafo) {
        throw new PendienteDeImplementar("Floyd-Warshall");
    }

    /**
     * UCS (Uniform Cost Search): igual que Dijkstra pero con parada temprana
     * al expandir el destino.
     *
     * OPCIONAL. Solo suma si ya estan implementados Dijkstra y Floyd.
     * Si lo hacen, en el coloquio hay que poder mostrar la diferencia de
     * nodos expandidos respecto de Dijkstra sobre la misma consulta.
     */
    public Camino ucs(Grafo grafo, String origenId, String destinoId) {
        throw new PendienteDeImplementar("UCS");
    }
}
