package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UNIDAD: Branch and Bound (clase 12)       PUNTAJE: 1 punto
 *
 * Es backtracking mas una cota. La diferencia con la clase Backtracking
 * es el criterio de poda: alli se podaba por VIABILIDAD, aca se poda por
 * OPTIMALIDAD.
 *
 * En el dominio: repartir los suministros entre k sobrevivientes de modo
 * que el que mas carga lleve la menor cantidad de kg posible.
 */
@Component
public class RamificacionYPoda {

    private static final String PREFIJO_CONTENEDOR = "Sobreviviente ";

    /**
     * Mejor reparto encontrado.
     *
     * @param porContenedor   "Sobreviviente 1".."Sobreviviente m" con sus items (m = min(k, n))
     * @param cargaMaxima     kg del sobreviviente mas cargado (el makespan optimo)
     * @param cotaInicial     cota optimista en la raiz: peso total / m
     * @param nodosExplorados entradas a la recursion, raiz incluida
     * @param nodosPodados    hijos que no se abrieron porque su cota no mejoraba la mejor solucion
     */
    public record Asignacion(Map<String, List<Item>> porContenedor, double cargaMaxima,
                             double cotaInicial, long nodosExplorados, long nodosPodados) { }

    /**
     * Reparte items entre k contenedores identicos minimizando la carga del
     * contenedor mas cargado (makespan). DFS recursivo, esquema de la clase 12.
     *
     * Se usan m = min(k, n) contenedores: con pesos no negativos un reparto
     * optimo nunca necesita mas contenedores que items. Por eso un k enorme
     * no reserva memoria de mas, y el mapa de salida tiene m entradas (vacio
     * si no hay items).
     *
     * Ramificar: el item i va a alguno de los m contenedores. Ruptura de
     * simetria: los contenedores son identicos, asi que un contenedor vacio
     * solo se prueba si es el PRIMERO vacio (contenedores 0..usados). Si no,
     * cada reparto aparece m! veces con los nombres permutados.
     * OJO: la ruptura de simetria y el recorte a min(k, n) valen SOLO con
     * contenedores identicos. Si se agregan capacidades distintas por
     * sobreviviente, hay que sacar ambas cosas.
     *
     * Acotar: cota inferior OPTIMISTA del makespan alcanzable desde el hijo
     *     max( carga maxima tras ubicar el item , peso total / m )
     * Es optimista porque las cargas solo crecen (el maximo actual se
     * mantiene o sube) y en cualquier reparto completo el contenedor mas
     * cargado lleva al menos el promedio total / m (si k &gt;= n, total / n no
     * supera al item mas pesado, que es una cota del optimo). Nunca
     * sobreestima, asi que no descarta el optimo.
     *
     * Podar: si la cota es &gt;= la mejor solucion conocida, el hijo no se
     * abre. Sin solucion inicial: la mejor arranca en +infinito.
     *
     * Los items se procesan en el orden recibido (el controller los pasa por id).
     *
     * Complejidad: O(m^n) nodos en el peor caso; a lo sumo
     * (m^(n+1) - 1) / (m - 1) si m &gt; 1, y n + 1 si m = 1. La poda y la
     * ruptura de simetria lo reducen mucho en la practica. Cada nodo cuesta
     * O(1) (la carga maxima viaja como parametro) y cada mejora copia la
     * asignacion en O(n). Espacio: O(n), porque m &lt;= n.
     *
     * @throws IllegalArgumentException si k &lt; 1, items es null o algun peso es negativo o no finito
     * @throws IllegalStateException    si ningun reparto es factible (hoy no pasa: queda
     *                                  cubierto por si se agregan restricciones)
     */
    public Asignacion repartir(List<Item> items, int cantidadContenedores) {
        validar(items, cantidadContenedores);
        int contenedores = Math.min(cantidadContenedores, items.size());
        double cotaInicial = contenedores == 0 ? 0 : pesoTotal(items) / contenedores;

        Busqueda busqueda = new Busqueda(items, contenedores, cotaInicial);
        busqueda.ramificar(0, 0, 0);
        if (busqueda.mejorAsignacion == null) {
            throw new IllegalStateException("No hay un reparto factible");
        }

        return new Asignacion(nombrar(agrupar(items, busqueda.mejorAsignacion, contenedores)),
                busqueda.mejor, cotaInicial, busqueda.nodosExplorados, busqueda.nodosPodados);
    }

    private void validar(List<Item> items, int cantidadContenedores) {
        if (cantidadContenedores < 1) {
            throw new IllegalArgumentException(
                    "La cantidad de contenedores debe ser al menos 1: " + cantidadContenedores);
        }
        if (items == null) {
            throw new IllegalArgumentException("La lista de items no puede ser null");
        }
        for (Item item : items) {
            if (item == null || !Double.isFinite(item.getPeso()) || item.getPeso() < 0) {
                throw new IllegalArgumentException("Peso invalido (negativo o no numerico) en: " + item);
            }
        }
    }

    private double pesoTotal(List<Item> items) {
        double total = 0;
        for (Item item : items) {
            total += item.getPeso();
        }
        return total;
    }

    /** Items de cada contenedor, segun asignacion[i] = contenedor del item i. */
    private List<List<Item>> agrupar(List<Item> items, int[] asignacion, int contenedores) {
        List<List<Item>> grupos = new ArrayList<>();
        for (int c = 0; c < contenedores; c++) {
            grupos.add(new ArrayList<>());
        }
        for (int i = 0; i < items.size(); i++) {
            grupos.get(asignacion[i]).add(items.get(i));
        }
        return grupos;
    }

    private Map<String, List<Item>> nombrar(List<List<Item>> grupos) {
        Map<String, List<Item>> porContenedor = new LinkedHashMap<>();
        for (int c = 0; c < grupos.size(); c++) {
            porContenedor.put(PREFIJO_CONTENEDOR + (c + 1), List.copyOf(grupos.get(c)));
        }
        return Collections.unmodifiableMap(porContenedor);
    }

    /**
     * Estado de UNA busqueda. Va en un objeto por llamada (y no en campos de
     * RamificacionYPoda) porque el @Component es singleton.
     */
    private static final class Busqueda {
        private final double[] pesos;
        private final double[] cargas;
        private final double promedio;

        private final int[] asignacion;
        private int[] mejorAsignacion = null; // null mientras no haya reparto completo
        private double mejor = Double.POSITIVE_INFINITY;
        private long nodosExplorados = 0;
        private long nodosPodados = 0;

        Busqueda(List<Item> items, int contenedores, double promedio) {
            this.pesos = new double[items.size()];
            for (int i = 0; i < pesos.length; i++) {
                pesos[i] = items.get(i).getPeso();
            }
            this.cargas = new double[contenedores];
            this.promedio = promedio;
            this.asignacion = new int[pesos.length];
        }

        /**
         * @param i           item a ubicar
         * @param cargaMaxima carga del contenedor mas cargado hasta ahora
         * @param usados      contenedores no vacios (los primeros 'usados')
         */
        void ramificar(int i, double cargaMaxima, int usados) {
            nodosExplorados++;
            if (i == pesos.length) {
                registrarSiMejora(cargaMaxima);
                return;
            }
            int limite = Math.min(cargas.length, usados + 1); // ruptura de simetria
            for (int c = 0; c < limite; c++) {
                double nuevaCargaMaxima = Math.max(cargaMaxima, cargas[c] + pesos[i]);
                // cota optimista: si ni en el mejor caso mejora, no se abre el hijo
                if (Math.max(nuevaCargaMaxima, promedio) >= mejor) {
                    nodosPodados++;
                    continue;
                }
                cargas[c] += pesos[i];
                asignacion[i] = c;
                ramificar(i + 1, nuevaCargaMaxima, Math.max(usados, c + 1));
                cargas[c] -= pesos[i]; // retroceso
            }
        }

        private void registrarSiMejora(double cargaMaxima) {
            if (cargaMaxima < mejor) {
                mejor = cargaMaxima;
                mejorAsignacion = asignacion.clone();
            }
        }
    }
}
