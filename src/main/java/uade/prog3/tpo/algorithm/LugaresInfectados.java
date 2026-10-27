package uade.prog3.tpo.algorithm;

import uade.prog3.tpo.service.Grafo;

import java.util.Set;

/**
 * Lugares infectados: nodos que una ruta NO puede pisar (ni como paso intermedio).
 *
 * Traduce los ids a un arreglo bloqueado[] indexado como el grafo, para que el
 * algoritmo lo consulte en O(1) dentro de su bucle. Lo usan Dijkstra y el backtracking.
 */
final class LugaresInfectados {

    private LugaresInfectados() {
    }

    /**
     * bloqueado[v] = true si v esta infectado. Un conjunto null o vacio no bloquea nada.
     * Complejidad: O(V + I), con I la cantidad de infectados.
     *
     * @throws IllegalArgumentException si un infectado no existe, o si el origen o el
     *                                  destino estan infectados
     */
    static boolean[] marcar(Grafo grafo, Set<String> infectados, String origenId, String destinoId) {
        boolean[] bloqueado = new boolean[grafo.cantidadVertices()];
        if (infectados == null) {
            return bloqueado;
        }
        for (String id : infectados) {
            bloqueado[grafo.indiceDe(id)] = true; // indiceDe valida que exista
        }
        verificarNoInfectado(infectados, origenId, "origen");
        verificarNoInfectado(infectados, destinoId, "destino");
        return bloqueado;
    }

    private static void verificarNoInfectado(Set<String> infectados, String id, String rol) {
        if (infectados.contains(id)) {
            throw new IllegalArgumentException("El " + rol + " " + id + " esta infectado: no se puede usar");
        }
    }
}
