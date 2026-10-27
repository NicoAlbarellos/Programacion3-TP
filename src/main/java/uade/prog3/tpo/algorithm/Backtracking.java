package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * UNIDAD: Backtracking (clases 9 y 10)      PUNTAJE: 1 punto
 *
 * En el dominio: rutas de escape a pie entre dos refugios de CABA que no
 * superen un tiempo maximo (minutos) ni una cantidad maxima de tramos.
 */
@Component
public class Backtracking {

    /** Un camino simple encontrado, con su costo acumulado (minutos). */
    public record Ruta(List<String> vertices, double costoTotal) { }

    /**
     * Rutas validas (las de la corrida con poda) y nodos explorados por la
     * misma busqueda con y sin poda, para medir cuanto ahorra la poda.
     */
    public record ResultadoRutas(List<Ruta> rutas, long nodosConPoda, long nodosSinPoda) { }

    /**
     * Enumera TODOS los caminos simples de origen a destino con costo total
     * &lt;= costoMaximo y a lo sumo saltosMaximos aristas.
     *
     * Backtracking de TIPO 2 (como N reinas o Sudoku de la clase 9): cada
     * llamada recorre en un bucle las opciones (los vecinos del vertice
     * actual) y hace una llamada recursiva por cada opcion viable. La
     * solucion parcial es el camino; al volver de la llamada se deshace la
     * ultima decision (se desmarca el vertice y se lo saca del camino).
     *
     * La busqueda se corre dos veces sobre el mismo grafo:
     *   - con poda: no baja a un vecino si el costo o los saltos ya se pasan
     *     (criterio de viabilidad, se poda sin completar el camino)
     *   - sin poda: solo respeta el camino simple y filtra recien al llegar
     *     al destino. Devuelve las mismas rutas, pero explora mas nodos.
     * Nodo explorado = cada entrada a la funcion recursiva, raiz incluida.
     *
     * Los costos de las aristas deben ser no negativos (minutos a pie): la
     * poda por costo supone que el costo acumulado nunca baja.
     *
     * Complejidad, con b el grado maximo:
     *   - corrida con poda: O(b^L) nodos, L = min(saltosMaximos, V - 1)
     *   - corrida sin poda: no usa los limites, recorre todos los caminos
     *     simples desde el origen: O(b^(V - 1)) nodos, peor caso (grafo
     *     completo) O(V!)
     * El endpoint cuesta lo que la corrida sin poda. Copiar cada ruta
     * encontrada cuesta O(V). Espacio: O(V) por visitado[], el camino parcial
     * y la pila de recursion (mas la salida).
     *
     * @throws IllegalArgumentException si origen o destino no existen, o si
     *                                  costoMaximo o saltosMaximos son negativos
     */
    public ResultadoRutas rutasSimples(Grafo grafo, String origenId, String destinoId,
                                       double costoMaximo, int saltosMaximos) {
        return rutasSimples(grafo, origenId, destinoId, costoMaximo, saltosMaximos, Set.of());
    }

    /**
     * Igual, pero sin pasar por lugares infectados: se marcan como visitados ANTES de
     * empezar (en las dos corridas), asi el chequeo de camino simple ya los descarta.
     * La complejidad no cambia.
     *
     * @throws IllegalArgumentException ademas, si un infectado no existe o si el
     *                                  origen o el destino estan infectados
     */
    public ResultadoRutas rutasSimples(Grafo grafo, String origenId, String destinoId,
                                       double costoMaximo, int saltosMaximos, Set<String> infectados) {
        int origen = grafo.indiceDe(origenId);
        int destino = grafo.indiceDe(destinoId);
        boolean[] bloqueado = LugaresInfectados.marcar(grafo, infectados, origenId, destinoId);
        validarLimites(costoMaximo, saltosMaximos);

        Busqueda conPoda = new Busqueda(grafo, destino, costoMaximo, saltosMaximos, true, bloqueado);
        conPoda.explorarDesde(origen);
        Busqueda sinPoda = new Busqueda(grafo, destino, costoMaximo, saltosMaximos, false, bloqueado);
        sinPoda.explorarDesde(origen);

        return new ResultadoRutas(List.copyOf(conPoda.rutas), conPoda.nodosExplorados,
                sinPoda.nodosExplorados);
    }

    private void validarLimites(double costoMaximo, int saltosMaximos) {
        if (Double.isNaN(costoMaximo)) {
            throw new IllegalArgumentException("El costo maximo tiene que ser un numero: " + costoMaximo);
        }
        if (costoMaximo < 0) {
            throw new IllegalArgumentException("El costo maximo no puede ser negativo: " + costoMaximo);
        }
        if (saltosMaximos < 0) {
            throw new IllegalArgumentException("Los saltos maximos no pueden ser negativos: " + saltosMaximos);
        }
    }

    /**
     * Estado de UNA busqueda. Va en un objeto por llamada (y no en campos de
     * Backtracking) porque el @Component es singleton y puede atender
     * pedidos concurrentes.
     */
    private static final class Busqueda {
        private final Grafo grafo;
        private final int destino;
        private final double costoMaximo;
        private final int saltosMaximos;
        private final boolean conPoda;

        private final boolean[] visitado;
        private final List<Integer> camino = new ArrayList<>();
        private final List<Ruta> rutas = new ArrayList<>();
        private long nodosExplorados = 0;

        Busqueda(Grafo grafo, int destino, double costoMaximo, int saltosMaximos, boolean conPoda,
                 boolean[] bloqueado) {
            this.grafo = grafo;
            this.destino = destino;
            this.costoMaximo = costoMaximo;
            this.saltosMaximos = saltosMaximos;
            this.conPoda = conPoda;
            this.visitado = bloqueado.clone(); // infectados = ya visitados: nunca se entra
        }

        void explorarDesde(int origen) {
            visitado[origen] = true;
            camino.add(origen);
            explorar(origen, 0, 0);
        }

        private void explorar(int v, double costo, int saltos) {
            nodosExplorados++;
            if (v == destino) {
                registrarSiCumple(costo, saltos);
                return;
            }
            for (Grafo.Arista arista : grafo.vecinos(v)) {
                int w = arista.destino();
                if (visitado[w]) {
                    continue; // camino simple: no repetir vertices
                }
                // poda por viabilidad: esta rama ya no puede dar una ruta valida
                if (conPoda && (costo + arista.costo() > costoMaximo || saltos + 1 > saltosMaximos)) {
                    continue;
                }
                visitado[w] = true;
                camino.add(w);
                explorar(w, costo + arista.costo(), saltos + 1);
                // retroceso: se deshace la ultima decision
                camino.remove(camino.size() - 1);
                visitado[w] = false;
            }
        }

        /** Con poda siempre cumple; sin poda el filtro se aplica recien aca. */
        private void registrarSiCumple(double costo, int saltos) {
            if (costo <= costoMaximo && saltos <= saltosMaximos) {
                rutas.add(new Ruta(copiaDelCamino(), costo));
            }
        }

        private List<String> copiaDelCamino() {
            List<String> ids = new ArrayList<>(camino.size());
            for (int indice : camino) {
                ids.add(grafo.idDe(indice));
            }
            return List.copyOf(ids);
        }
    }
}
