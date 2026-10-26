package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * UNIDAD: Grafos II (clase 5)
 * PUNTAJE: forma parte de los 3 puntos de "Dijkstra, Prim, Kruskal"
 *
 * En el dominio: el minimo de calles a despejar (en minutos a pie) para que
 * todos los refugios de CABA queden conectados.
 *
 * El arbol de recubrimiento minimo solo tiene sentido sobre un grafo NO
 * DIRIGIDO y CONEXO: si no lo es, se falla con un mensaje claro.
 */
@Component
public class ArbolRecubrimiento {

    /** Una arista del arbol resultante, con ids de negocio. */
    public record AristaMst(String origen, String destino, double costo) { }

    /** Arbol de recubrimiento minimo completo. */
    public record Mst(List<AristaMst> aristas, double costoTotal) { }

    /** Entrada de la cola de prioridad de Prim: un vertice con la key con que se encolo. */
    private record EnCola(int vertice, double key) { }

    /** Arista del grafo por indices, para ordenar en Kruskal. */
    private record AristaIndexada(int u, int v, double costo) { }

    private final Ordenamiento ordenamiento;

    public ArbolRecubrimiento(Ordenamiento ordenamiento) {
        this.ordenamiento = ordenamiento;
    }

    // ------------------------------------------------------------------ Prim

    /**
     * Prim: hace crecer un unico arbol desde el origen, agregando en cada paso
     * la arista de menor costo que cruza de dentro del arbol hacia afuera.
     *
     * Arreglos de la diapositiva: key[] (costo minimo para sumar cada vertice),
     * parent[] (guarda el arbol) e inMST[]. El minimo se extrae con una
     * PriorityQueue con eliminacion perezosa (se reencola en vez de decreaseKey).
     *
     * Prim y Dijkstra son el mismo greedy, cambia la prioridad: Dijkstra ordena
     * por dist[u] + costo (distancia acumulada desde el origen) y Prim por el
     * costo de la arista sola (costo < key[v]).
     *
     * Complejidad de ESTA implementacion (con heap): O(E log V). La cola tiene
     * a lo sumo E entradas y log E <= 2 log V.
     * La variante con matriz y busqueda lineal del minimo, O(V^2), NO esta
     * implementada: solo se menciona porque conviene en grafos densos (E ~ V^2).
     *
     * @throws IllegalArgumentException si el grafo es dirigido o el origen no existe
     * @throws IllegalStateException    si el grafo no es conexo
     */
    public Mst prim(Grafo grafo, String origenId) {
        validarNoDirigido(grafo);
        int origen = grafo.indiceDe(origenId);
        int n = grafo.cantidadVertices();
        double[] key = new double[n];
        int[] parent = new int[n];
        boolean[] inMST = new boolean[n];
        Arrays.fill(key, Grafo.INFINITO);
        Arrays.fill(parent, -1);
        key[origen] = 0;

        List<AristaMst> arbol = new ArrayList<>();
        PriorityQueue<EnCola> cola = new PriorityQueue<>(Comparator.comparingDouble(EnCola::key));
        cola.add(new EnCola(origen, 0));
        while (!cola.isEmpty()) {
            int u = cola.poll().vertice();
            if (inMST[u]) {
                continue; // entrada vieja: u ya entro al arbol con una key menor
            }
            inMST[u] = true;
            if (parent[u] != -1) {
                arbol.add(new AristaMst(grafo.idDe(parent[u]), grafo.idDe(u), key[u]));
            }
            actualizarKeys(grafo, u, key, parent, inMST, cola);
        }

        validarConexo(arbol, n);
        return new Mst(arbol, costoTotal(arbol));
    }

    /** Para cada vecino v fuera del arbol: si la arista (u,v) es mas barata que key[v], se actualiza. */
    private void actualizarKeys(Grafo grafo, int u, double[] key, int[] parent, boolean[] inMST,
                                PriorityQueue<EnCola> cola) {
        for (Grafo.Arista arista : grafo.vecinos(u)) {
            int v = arista.destino();
            if (!inMST[v] && arista.costo() < key[v]) {
                key[v] = arista.costo();
                parent[v] = u;
                cola.add(new EnCola(v, arista.costo()));
            }
        }
    }

    // --------------------------------------------------------------- Kruskal

    /**
     * Kruskal: ordena TODAS las aristas por costo creciente (MergeSort propio,
     * estable: con empates el resultado es determinista) y las agrega una a una,
     * salteando las que formarian ciclo. Termina al juntar V-1 aristas.
     *
     * Deteccion de ciclo con Union-Find: si find(u) == find(v), u y v ya estan
     * conectados y agregar (u,v) cerraria un ciclo.
     *
     * Complejidad: O(E log E) por el ordenamiento, que es lo que domina
     * (tambien se escribe O(E log V)). Cada find/union es casi O(1) amortizado
     * (inversa de Ackermann) gracias a la compresion de caminos y la union por rango.
     *
     * @throws IllegalArgumentException si el grafo es dirigido
     * @throws IllegalStateException    si el grafo no es conexo
     */
    public Mst kruskal(Grafo grafo) {
        validarNoDirigido(grafo);
        int n = grafo.cantidadVertices();
        List<AristaIndexada> ordenadas = ordenamiento.mergeSort(aristasSinRepetir(grafo),
                Comparator.comparingDouble(AristaIndexada::costo));

        UnionFind conjuntos = new UnionFind(n);
        List<AristaMst> arbol = new ArrayList<>();
        for (AristaIndexada a : ordenadas) {
            if (arbol.size() == n - 1) {
                break; // un arbol de V vertices tiene exactamente V-1 aristas
            }
            if (conjuntos.find(a.u()) != conjuntos.find(a.v())) {
                conjuntos.union(a.u(), a.v());
                arbol.add(new AristaMst(grafo.idDe(a.u()), grafo.idDe(a.v()), a.costo()));
            } // si no, formaria ciclo: se descarta
        }

        validarConexo(arbol, n);
        return new Mst(arbol, costoTotal(arbol));
    }

    /** En un grafo no dirigido cada arista aparece en las dos listas: se toma solo con u < v. */
    private List<AristaIndexada> aristasSinRepetir(Grafo grafo) {
        List<AristaIndexada> aristas = new ArrayList<>();
        for (int u = 0; u < grafo.cantidadVertices(); u++) {
            for (Grafo.Arista arista : grafo.vecinos(u)) {
                if (u < arista.destino()) {
                    aristas.add(new AristaIndexada(u, arista.destino(), arista.costo()));
                }
            }
        }
        return aristas;
    }

    /**
     * Union-Find (Disjoint Set) a mano.
     *   find(x): representante del conjunto de x, con compresion de caminos.
     *   union(x, y): fusiona los conjuntos, colgando el de menor rango del de mayor.
     */
    private static final class UnionFind {
        private final int[] padre;
        private final int[] rango;

        UnionFind(int n) {
            padre = new int[n];
            rango = new int[n];
            for (int i = 0; i < n; i++) {
                padre[i] = i; // cada vertice arranca en su propio conjunto
            }
        }

        int find(int x) {
            if (padre[x] != x) {
                padre[x] = find(padre[x]); // compresion de caminos: apunta directo a la raiz
            }
            return padre[x];
        }

        void union(int x, int y) {
            int raizX = find(x);
            int raizY = find(y);
            if (raizX == raizY) {
                return;
            }
            // union por rango: el arbol mas bajo se cuelga del mas alto
            if (rango[raizX] < rango[raizY]) {
                padre[raizX] = raizY;
            } else if (rango[raizX] > rango[raizY]) {
                padre[raizY] = raizX;
            } else {
                padre[raizY] = raizX;
                rango[raizX]++;
            }
        }
    }

    // ------------------------------------------------------------- comunes

    private void validarNoDirigido(Grafo grafo) {
        if (grafo.esDirigido()) {
            throw new IllegalArgumentException("El MST requiere grafo no dirigido");
        }
    }

    private void validarConexo(List<AristaMst> arbol, int cantidadVertices) {
        if (cantidadVertices > 0 && arbol.size() < cantidadVertices - 1) {
            throw new IllegalStateException("El grafo no es conexo");
        }
    }

    private double costoTotal(List<AristaMst> arbol) {
        double total = 0;
        for (AristaMst arista : arbol) {
            total += arista.costo();
        }
        return total;
    }
}
