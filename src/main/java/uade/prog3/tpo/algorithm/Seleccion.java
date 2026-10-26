package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Greedy (clase 3) y Programacion Dinamica (clase 6)
 * PUNTAJE: 1 punto greedy + 1 punto programacion dinamica
 *
 * Los dos metodos resuelven EL MISMO problema (mochila 0/1) con tecnicas distintas:
 *
 *   FUNCION OBJETIVO: maximizar la utilidad total (suma de valores) de los
 *   suministros elegidos, sujeto a que la suma de sus pesos (kg) no supere
 *   la capacidad de la mochila. Cada item se toma entero o no se toma.
 *
 * Con los suministros del dominio y capacidad 12, greedy da 112 y la PD 119:
 * es el contraejemplo donde greedy NO da el optimo.
 */
@Component
public class Seleccion {

    /** Resultado de una seleccion de items bajo restriccion de capacidad. */
    public record Resultado(List<Item> elegidos, double valorTotal, double pesoTotal, String tecnica) { }

    /** Las dos tecnicas sobre los mismos items y la misma capacidad. */
    public record Comparacion(Resultado greedy, Resultado dinamica) { }

    private final Ordenamiento ordenamiento;

    public Seleccion(Ordenamiento ordenamiento) {
        this.ordenamiento = ordenamiento;
    }

    // ---------------------------------------------------------------- Greedy

    /**
     * GREEDY 0/1 por ratio valor/peso DESCENDENTE ("Tecnica 2" de la clase 6).
     * Ver {@link #greedy(List, double, Comparator)}.
     */
    public Resultado greedy(List<Item> items, double capacidad) {
        return greedy(items, capacidad, Comparator.comparingDouble(Item::ratioValorPeso).reversed());
    }

    /**
     * GREEDY 0/1 con un criterio de orden cualquiera (el primero es el preferido).
     *
     * Ordena con MergeSort (propio y estable: en un empate respeta el orden de
     * entrada) y recorre la lista UNA vez: si el item entra en la capacidad
     * restante se toma; si NO entra se SALTEA y se sigue con el siguiente
     * (no se corta, porque uno mas liviano todavia puede entrar).
     * La eleccion nunca se reconsidera.
     *
     * NO garantiza el optimo en la mochila 0/1 (items indivisibles). Si se
     * permitiera fraccionar el primer item que no entra, seria la mochila
     * fraccional de la clase 3, y ahi el greedy por ratio SI es optimo.
     *
     * Complejidad: O(n log n) por el ordenamiento + O(n) del recorrido = O(n log n).
     * Espacio: O(n) por la lista ordenada.
     *
     * @throws IllegalArgumentException si la capacidad, algun peso o algun valor es negativo
     */
    public Resultado greedy(List<Item> items, double capacidad, Comparator<Item> criterio) {
        validarItems(items, capacidad);
        List<Item> ordenados = ordenamiento.mergeSort(items, criterio);

        List<Item> elegidos = new ArrayList<>();
        double restante = capacidad;
        for (Item item : ordenados) {
            if (item.getPeso() <= restante) { // entra: se toma y no se reconsidera
                elegidos.add(item);
                restante -= item.getPeso();
            }
        }
        return resultado(elegidos, "GREEDY");
    }

    // --------------------------------------------------- Programacion dinamica

    /**
     * PROGRAMACION DINAMICA: mochila 0/1 con tabla dp, bottom-up.
     *
     * dp[i][j] = mejor valor usando los primeros i items con capacidad j.
     *   dp[0][j] = 0 y dp[i][0] = 0
     *   dp[i][j] = dp[i-1][j]                                   si p_i > j
     *   dp[i][j] = max(dp[i-1][j], dp[i-1][j - p_i] + v_i)      si p_i <= j
     *
     * Despues se RECUPERA EL CAMINO recorriendo la tabla hacia atras desde dp[n][W].
     *
     * Complejidad: O(n * W) en tiempo y en espacio (la tabla). Es PSEUDOPOLINOMICA:
     * es polinomica en el VALOR numerico W, pero W ocupa log W bits en la entrada,
     * asi que en funcion del tamano de la entrada crece exponencialmente.
     *
     * @throws IllegalArgumentException si la capacidad, algun peso o algun valor es negativo,
     *                                  o si algun peso no es entero (indexa la tabla)
     */
    public Resultado programacionDinamica(List<Item> items, int capacidad) {
        validarItems(items, capacidad);
        validarPesosEnteros(items);

        // Si la capacidad supera la suma de los pesos, entran todos: las columnas de mas
        // repetirian el mismo valor. Acotar W da el mismo resultado y evita tablas gigantes.
        int w = (int) Math.min(capacidad, sumaDePesos(items));
        double[][] dp = construirTabla(items, w);
        return resultado(recuperarElegidos(items, dp, w), "PROGRAMACION_DINAMICA");
    }

    private double[][] construirTabla(List<Item> items, int w) {
        int n = items.size();
        double[][] dp = new double[n + 1][w + 1]; // fila 0 y columna 0 quedan en 0
        for (int i = 1; i <= n; i++) {
            int peso = (int) items.get(i - 1).getPeso();
            double valor = items.get(i - 1).getValor();
            for (int j = 0; j <= w; j++) {
                dp[i][j] = dp[i - 1][j]; // no tomar el item i
                if (peso <= j && dp[i - 1][j - peso] + valor > dp[i][j]) {
                    dp[i][j] = dp[i - 1][j - peso] + valor; // tomar el item i
                }
            }
        }
        return dp;
    }

    /** Si dp[i][j] != dp[i-1][j], el item i se tomo: se resta su peso y se sigue hacia arriba. */
    private List<Item> recuperarElegidos(List<Item> items, double[][] dp, int w) {
        boolean[] tomado = new boolean[items.size()];
        int j = w;
        for (int i = items.size(); i >= 1; i--) {
            if (dp[i][j] != dp[i - 1][j]) {
                tomado[i - 1] = true;
                j -= (int) items.get(i - 1).getPeso();
            }
        }
        List<Item> elegidos = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (tomado[i]) {
                elegidos.add(items.get(i)); // en el orden de entrada
            }
        }
        return elegidos;
    }

    // ------------------------------------------------------------ comparacion

    /** Greedy por ratio y PD sobre los mismos items y la misma capacidad. */
    public Comparacion comparar(List<Item> items, int capacidad) {
        return new Comparacion(greedy(items, capacidad), programacionDinamica(items, capacidad));
    }

    // ------------------------------------------------------------ auxiliares

    private void validarItems(List<Item> items, double capacidad) {
        if (Double.isNaN(capacidad) || Double.isInfinite(capacidad)) {
            throw new IllegalArgumentException("La capacidad tiene que ser un numero finito: " + capacidad);
        }
        if (capacidad < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa: " + capacidad);
        }
        for (Item item : items) {
            if (item.getPeso() < 0 || item.getValor() < 0) {
                throw new IllegalArgumentException("Peso y valor no pueden ser negativos: " + item.getId());
            }
        }
    }

    private void validarPesosEnteros(List<Item> items) {
        for (Item item : items) {
            if (item.getPeso() != Math.rint(item.getPeso())) {
                throw new IllegalArgumentException("La programacion dinamica exige pesos enteros: "
                        + item.getId() + " pesa " + item.getPeso());
            }
        }
    }

    private long sumaDePesos(List<Item> items) {
        long suma = 0;
        for (Item item : items) {
            suma += (long) item.getPeso();
        }
        return suma;
    }

    private Resultado resultado(List<Item> elegidos, String tecnica) {
        double valor = 0;
        double peso = 0;
        for (Item item : elegidos) {
            valor += item.getValor();
            peso += item.getPeso();
        }
        return new Resultado(List.copyOf(elegidos), valor, peso, tecnica);
    }
}
