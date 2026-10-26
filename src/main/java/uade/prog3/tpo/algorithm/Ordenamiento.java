package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Divide y Conquista (clase 2)     PUNTAJE: 1 punto
 *
 * Ordenamientos propios, sin Collections.sort, List.sort ni Arrays.sort.
 * Son genericos: sirven para los suministros (Item) y para las aristas de Kruskal.
 * Ninguno modifica la lista de entrada: ambos devuelven una lista nueva.
 */
@Component
public class Ordenamiento {

    /**
     * QuickSort: elige un pivote, deja los menores a la izquierda y los mayores
     * a la derecha (el pivote queda en su posicion definitiva) y repite sobre
     * cada sublista. Ordena in-place sobre una COPIA de la entrada.
     *
     * Particion de Lomuto. Pivote: el elemento del MEDIO de la sublista. Con
     * el primer elemento como pivote, una lista ya ordenada es el peor caso;
     * con el del medio, esa entrada parte justo a la mitad.
     *
     * Recurrencias:
     *   mejor / promedio: T(n) = 2T(n/2) + O(n)  -> division, a=2 b=2 k=1, a = b^k -> O(n log n)
     *   peor caso:        T(n) = T(n-1) + O(n)   -> sustraccion, a=1 -> O(n^2)
     * El peor caso sigue existiendo: cuando el pivote cae siempre en un extremo
     * (por ejemplo, todos los elementos iguales, porque Lomuto los manda a un lado).
     * Espacio: O(n) por la copia, O(log n) de pila en promedio. No es estable.
     *
     * @param lista    elementos a ordenar (no se modifica)
     * @param criterio comparador que define el orden
     * @return lista nueva ordenada segun el criterio
     */
    public <T> List<T> quickSort(List<T> lista, Comparator<? super T> criterio) {
        List<T> copia = new ArrayList<>(lista);
        quickSort(copia, 0, copia.size() - 1, criterio);
        return copia;
    }

    private <T> void quickSort(List<T> lista, int inicio, int fin, Comparator<? super T> criterio) {
        if (inicio >= fin) {
            return; // caso base: 0 o 1 elemento
        }
        int posicionPivote = particionar(lista, inicio, fin, criterio);
        quickSort(lista, inicio, posicionPivote - 1, criterio);
        quickSort(lista, posicionPivote + 1, fin, criterio);
    }

    /** Lomuto: el pivote (el del medio) se lleva al final y se recorre una vez la sublista. */
    private <T> int particionar(List<T> lista, int inicio, int fin, Comparator<? super T> criterio) {
        intercambiar(lista, inicio + (fin - inicio) / 2, fin);
        T pivote = lista.get(fin);
        int ultimoMenor = inicio - 1;
        for (int j = inicio; j < fin; j++) {
            if (criterio.compare(lista.get(j), pivote) < 0) {
                ultimoMenor++;
                intercambiar(lista, ultimoMenor, j);
            }
        }
        intercambiar(lista, ultimoMenor + 1, fin); // el pivote queda en su posicion definitiva
        return ultimoMenor + 1;
    }

    private <T> void intercambiar(List<T> lista, int i, int j) {
        T aux = lista.get(i);
        lista.set(i, lista.get(j));
        lista.set(j, aux);
    }

    /**
     * MergeSort: divide la lista en dos mitades, ordena cada una
     * recursivamente y mezcla las dos mitades ordenadas en una sola.
     *
     * Recurrencia: T(n) = 2T(n/2) + O(n). Metodo de division: a=2 (dos llamadas),
     * b=2 (mitades), k=1 (merge lineal); a = b^k -> O(n^k log n) = O(n log n),
     * en TODOS los casos, sin importar la entrada.
     * Espacio: O(n) auxiliar para la mezcla. Es ESTABLE.
     *
     * @param lista    elementos a ordenar (no se modifica)
     * @param criterio comparador que define el orden
     * @return lista nueva ordenada segun el criterio
     */
    public <T> List<T> mergeSort(List<T> lista, Comparator<? super T> criterio) {
        if (lista.size() <= 1) {
            return new ArrayList<>(lista); // caso base: ya esta ordenada
        }
        int medio = lista.size() / 2;
        List<T> izquierda = mergeSort(lista.subList(0, medio), criterio);
        List<T> derecha = mergeSort(lista.subList(medio, lista.size()), criterio);
        return mezclar(izquierda, derecha, criterio);
    }

    /** Merge lineal de dos listas ordenadas. */
    private <T> List<T> mezclar(List<T> izquierda, List<T> derecha, Comparator<? super T> criterio) {
        List<T> resultado = new ArrayList<>(izquierda.size() + derecha.size());
        int i = 0;
        int j = 0;
        while (i < izquierda.size() && j < derecha.size()) {
            // con <= en un empate se toma el de la IZQUIERDA: eso hace estable al algoritmo
            if (criterio.compare(izquierda.get(i), derecha.get(j)) <= 0) {
                resultado.add(izquierda.get(i++));
            } else {
                resultado.add(derecha.get(j++));
            }
        }
        while (i < izquierda.size()) {
            resultado.add(izquierda.get(i++));
        }
        while (j < derecha.size()) {
            resultado.add(derecha.get(j++));
        }
        return resultado;
    }
}
