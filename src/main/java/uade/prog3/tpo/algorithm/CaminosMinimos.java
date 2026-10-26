package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * UNIDAD: Grafos II (clase 5) y PD sobre grafos (clase 8)
 * PUNTAJE: forma parte de los 3 puntos de "Dijkstra, Prim, Kruskal"
 *          y del punto de PD (mochila 0/1 + Floyd-Warshall con deteccion de ciclos negativos).
 *
 * En el dominio: ruta mas rapida (en minutos a pie) entre dos refugios de CABA.
 */
@Component
public class CaminosMinimos {

    /** Resultado de una consulta de camino minimo. */
    public record Camino(List<String> vertices, double costoTotal) { }

    /**
     * Comparacion Floyd vs. Dijkstra corrido desde cada vertice (todos los pares).
     * operacionesFloyd: comparaciones del triple bucle (V^3).
     * nodosExpandidosDijkstra / aristasExaminadasDijkstra: sumados sobre las V corridas.
     */
    public record Comparacion(int vertices, long operacionesFloyd,
                              long nodosExpandidosDijkstra, long aristasExaminadasDijkstra) { }

    /** Entrada de la cola de prioridad: un vertice con la distancia con que se encolo. */
    private record EnCola(int vertice, double distancia) { }

    /** Arbol de caminos minimos de una corrida de Dijkstra, con sus contadores. */
    private record ArbolDeCaminos(double[] dist, int[] previo, long nodosExpandidos, long aristasExaminadas) { }

    /**
     * Resultado del triple bucle de Floyd: distancias, matriz siguiente[][] para
     * reconstruir caminos y cantidad de comparaciones que hizo.
     */
    private record DistanciasFloyd(double[][] dist, int[][] siguiente, long operaciones) { }

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

        ArbolDeCaminos arbol = arbolDesde(grafo, origen);
        if (arbol.dist()[destino] == Grafo.INFINITO) {
            throw new IllegalStateException("No hay camino de " + origenId + " a " + destinoId);
        }
        return new Camino(reconstruirCamino(grafo, arbol.previo(), destino), arbol.dist()[destino]);
    }

    /**
     * Nucleo de Dijkstra: arbol de caminos minimos desde el origen, con contadores.
     * nodosExpandidos cuenta las extracciones VALIDAS (no las entradas viejas descartadas);
     * aristasExaminadas, las aristas que se miran al expandir cada nodo.
     */
    private ArbolDeCaminos arbolDesde(Grafo grafo, int origen) {
        int n = grafo.cantidadVertices();
        double[] dist = new double[n];
        int[] previo = new int[n];
        Arrays.fill(dist, Grafo.INFINITO);
        Arrays.fill(previo, -1);
        dist[origen] = 0;
        long nodosExpandidos = 0;
        long aristasExaminadas = 0;

        PriorityQueue<EnCola> cola = new PriorityQueue<>(Comparator.comparingDouble(EnCola::distancia));
        cola.add(new EnCola(origen, 0));
        while (!cola.isEmpty()) {
            EnCola actual = cola.poll();
            int u = actual.vertice();
            if (actual.distancia() > dist[u]) {
                continue; // entrada vieja: u ya se proceso con una distancia menor
            }
            nodosExpandidos++;
            aristasExaminadas += grafo.vecinos(u).size();
            relajarVecinos(grafo, u, dist, previo, cola);
        }
        return new ArbolDeCaminos(dist, previo, nodosExpandidos, aristasExaminadas);
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
     * Floyd-Warshall: costo minimo entre TODOS los pares de vertices (PD sobre grafos).
     *
     * d_k[i][j] = min(d_k-1[i][j], d_k-1[i][k] + d_k-1[k][j]): el mejor camino de i a j
     * usando solo los vertices 0..k como intermedios. Alcanza con una unica matriz
     * porque en la iteracion k la fila y la columna de k no cambian.
     *
     * A diferencia de Dijkstra, ACEPTA aristas negativas siempre que no haya un
     * ciclo de costo negativo. El ciclo se detecta al final: si algun d[i][i] < 0,
     * hay un camino de i a i que cuesta menos que quedarse quieto.
     * En un grafo NO dirigido una arista negativa ya es un ciclo negativo (ida y vuelta).
     *
     * Devuelve "origen->destino" -> costo minimo, en orden de indices. Los pares
     * INALCANZABLES se omiten: si falta la clave, no hay camino.
     *
     * Complejidad: O(V^3) en tiempo (tres bucles de V), O(V^2) en espacio (la matriz).
     *
     * @throws IllegalStateException si hay un ciclo de costo negativo
     */
    public Map<String, Double> floydWarshall(Grafo grafo) {
        double[][] dist = distanciasFloyd(grafo).dist();
        Map<String, Double> resultado = new LinkedHashMap<>();
        for (int i = 0; i < dist.length; i++) {
            for (int j = 0; j < dist.length; j++) {
                if (dist[i][j] != Grafo.INFINITO) {
                    resultado.put(grafo.idDe(i) + "->" + grafo.idDe(j), dist[i][j]);
                }
            }
        }
        return resultado;
    }

    /**
     * Camino minimo de origen a destino reconstruido con la matriz siguiente[][] de Floyd.
     * Mismo resultado que Dijkstra, pero tambien admite aristas negativas sin ciclo negativo.
     *
     * Complejidad: O(V^3) por Floyd + O(V) para reconstruir el camino.
     *
     * @throws IllegalArgumentException si origen o destino no existen
     * @throws IllegalStateException    si el destino es inalcanzable o hay un ciclo de costo negativo
     */
    public Camino caminoFloyd(Grafo grafo, String origenId, String destinoId) {
        int origen = grafo.indiceDe(origenId);
        int destino = grafo.indiceDe(destinoId);
        DistanciasFloyd floyd = distanciasFloyd(grafo);
        if (floyd.dist()[origen][destino] == Grafo.INFINITO) {
            throw new IllegalStateException("No hay camino de " + origenId + " a " + destinoId);
        }
        return new Camino(reconstruirCaminoFloyd(grafo, floyd.siguiente(), origen, destino),
                floyd.dist()[origen][destino]);
    }

    /** Desde el origen se salta a siguiente[u][destino] hasta llegar al destino. */
    private List<String> reconstruirCaminoFloyd(Grafo grafo, int[][] siguiente, int origen, int destino) {
        List<String> camino = new ArrayList<>();
        camino.add(grafo.idDe(origen));
        for (int u = origen; u != destino; ) {
            u = siguiente[u][destino];
            camino.add(grafo.idDe(u));
        }
        return List.copyOf(camino);
    }

    /** Triple bucle de Floyd, con k (el vertice intermedio) SIEMPRE por fuera. */
    private DistanciasFloyd distanciasFloyd(Grafo grafo) {
        double[][] d = grafo.matrizDeAdyacencia(); // ya es una copia nueva
        int n = d.length;
        int[][] siguiente = siguienteInicial(d);
        long operaciones = 0;
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    operaciones++;
                    // relajacion por k. Con double, INFINITO + (negativo) sigue siendo INFINITO,
                    // asi que un par sin camino nunca "mejora" por error.
                    if (d[i][k] + d[k][j] < d[i][j]) {
                        d[i][j] = d[i][k] + d[k][j];
                        siguiente[i][j] = siguiente[i][k]; // para ir a j, primero se va hacia k
                    }
                }
            }
        }
        verificarSinCicloNegativo(d);
        return new DistanciasFloyd(d, siguiente, operaciones);
    }

    /** siguiente[i][j]: j si hay arista, i en la diagonal, -1 si no hay arista. */
    private int[][] siguienteInicial(double[][] d) {
        int n = d.length;
        int[][] siguiente = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    siguiente[i][j] = i;
                } else {
                    siguiente[i][j] = (d[i][j] == Grafo.INFINITO) ? -1 : j;
                }
            }
        }
        return siguiente;
    }

    private void verificarSinCicloNegativo(double[][] d) {
        for (int i = 0; i < d.length; i++) {
            if (d[i][i] < 0) {
                throw new IllegalStateException("Hay un ciclo de costo negativo");
            }
        }
    }

    /**
     * Compara el trabajo de Floyd (una corrida, V^3 comparaciones) contra Dijkstra
     * corrido desde cada uno de los V vertices, que tambien da todos los pares:
     * V * O((V + E) log V). Dijkstra conviene en grafos ralos; Floyd en densos
     * o con aristas negativas.
     *
     * @throws IllegalArgumentException si hay costos negativos (Dijkstra no los admite)
     */
    public Comparacion compararConDijkstra(Grafo grafo) {
        validarCostosNoNegativos(grafo);
        long operacionesFloyd = distanciasFloyd(grafo).operaciones();
        long nodosExpandidos = 0;
        long aristasExaminadas = 0;
        for (int origen = 0; origen < grafo.cantidadVertices(); origen++) {
            ArbolDeCaminos arbol = arbolDesde(grafo, origen);
            nodosExpandidos += arbol.nodosExpandidos();
            aristasExaminadas += arbol.aristasExaminadas();
        }
        return new Comparacion(grafo.cantidadVertices(), operacionesFloyd, nodosExpandidos, aristasExaminadas);
    }

    /**
     * UCS (Uniform Cost Search): igual que Dijkstra pero con parada temprana
     * al expandir el destino.
     *
     * OPCIONAL. Si se implementa, en el coloquio hay que poder mostrar la diferencia de
     * nodos expandidos respecto de Dijkstra sobre la misma consulta.
     */
    public Camino ucs(Grafo grafo, String origenId, String destinoId) {
        throw new PendienteDeImplementar("UCS");
    }
}
