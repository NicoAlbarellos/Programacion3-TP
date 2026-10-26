package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.seed.DominioCaba;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de QuickSort y MergeSort con resultados calculados a mano. No tocan Neo4j.
 */
class OrdenamientoTest {

    private final Ordenamiento ordenamiento = new Ordenamiento();

    /** Mayor ratio valor/peso primero: el orden que usa el greedy. */
    private static final Comparator<Item> POR_RATIO_DESCENDENTE =
            Comparator.comparingDouble(Item::ratioValorPeso).reversed();

    private static final Comparator<Item> POR_PESO = Comparator.comparingDouble(Item::getPeso);

    /**
     * Items semilla de la catedra (peso / valor).
     * Ratios: I1 2.0, I2 10.0, I3 5.0, I4 16.67, I5 7.86.
     */
    private static List<Item> itemsSemilla() {
        return List.of(
                new Item("I1", "I1", 5, 10, null),
                new Item("I2", "I2", 4, 40, null),
                new Item("I3", "I3", 6, 30, null),
                new Item("I4", "I4", 3, 50, null),
                new Item("I5", "I5", 7, 55, null));
    }

    /** Suministros del dominio (peso en kg, utilidad como valor). */
    private static List<Item> suministros() {
        List<Item> items = new ArrayList<>();
        for (DominioCaba.Suministro s : DominioCaba.SUMINISTROS) {
            items.add(new Item(s.id(), s.nombre(), s.kg(), s.utilidad(), s.lugarId()));
        }
        return items;
    }

    private static List<String> ids(List<Item> items) {
        List<String> ids = new ArrayList<>();
        for (Item item : items) {
            ids.add(item.getId());
        }
        return ids;
    }

    private static List<Double> pesos(List<Item> items) {
        List<Double> pesos = new ArrayList<>();
        for (Item item : items) {
            pesos.add(item.getPeso());
        }
        return pesos;
    }

    @Test
    void quickSortSemillaPorRatio() {
        // verificacion de la clase 2: I4, I2, I5, I3, I1
        assertEquals(List.of("I4", "I2", "I5", "I3", "I1"),
                ids(ordenamiento.quickSort(itemsSemilla(), POR_RATIO_DESCENDENTE)));
    }

    @Test
    void mergeSortSemillaPorRatio() {
        assertEquals(List.of("I4", "I2", "I5", "I3", "I1"),
                ids(ordenamiento.mergeSort(itemsSemilla(), POR_RATIO_DESCENDENTE)));
    }

    @Test
    void quickSortSuministrosPorPeso() {
        // S4 1, S5 2, S1 3, S2 4, {S6, S8} 5, S7 6, S3 7 (QuickSort no es estable: se miran los pesos)
        assertEquals(List.of(1.0, 2.0, 3.0, 4.0, 5.0, 5.0, 6.0, 7.0),
                pesos(ordenamiento.quickSort(suministros(), POR_PESO)));
    }

    @Test
    void mergeSortSuministrosPorPesoEsEstable() {
        // S6 y S8 pesan 5 kg: MergeSort respeta el orden original (S6 antes que S8)
        assertEquals(List.of("S4", "S5", "S1", "S2", "S6", "S8", "S7", "S3"),
                ids(ordenamiento.mergeSort(suministros(), POR_PESO)));
    }

    @Test
    void mergeSortEstableConMuchosEmpates() {
        List<Item> items = List.of(
                new Item("a", "a", 2, 0, null),
                new Item("b", "b", 1, 0, null),
                new Item("c", "c", 2, 0, null),
                new Item("d", "d", 1, 0, null),
                new Item("e", "e", 2, 0, null));
        assertEquals(List.of("b", "d", "a", "c", "e"), ids(ordenamiento.mergeSort(items, POR_PESO)));
    }

    @Test
    void listaVaciaYDeUnElemento() {
        assertEquals(List.of(), ordenamiento.quickSort(List.<Item>of(), POR_PESO));
        assertEquals(List.of(), ordenamiento.mergeSort(List.<Item>of(), POR_PESO));
        List<Item> uno = List.of(new Item("I1", "I1", 5, 10, null));
        assertEquals(List.of("I1"), ids(ordenamiento.quickSort(uno, POR_PESO)));
        assertEquals(List.of("I1"), ids(ordenamiento.mergeSort(uno, POR_PESO)));
    }

    @Test
    void noModificaLaEntrada() {
        List<Item> entrada = new ArrayList<>(itemsSemilla());
        List<String> antes = ids(entrada);
        List<Item> ordenadaQuick = ordenamiento.quickSort(entrada, POR_RATIO_DESCENDENTE);
        List<Item> ordenadaMerge = ordenamiento.mergeSort(entrada, POR_RATIO_DESCENDENTE);
        assertEquals(antes, ids(entrada));
        assertNotSame(entrada, ordenadaQuick);
        assertNotSame(entrada, ordenadaMerge);
    }

    @Test
    void ordenaListaYaOrdenadaEInvertida() {
        List<Integer> ascendente = List.of(1, 2, 3, 4, 5, 6, 7);
        List<Integer> descendente = List.of(7, 6, 5, 4, 3, 2, 1);
        Comparator<Integer> natural = Comparator.naturalOrder();
        assertEquals(ascendente, ordenamiento.quickSort(descendente, natural));
        assertEquals(ascendente, ordenamiento.quickSort(ascendente, natural));
        assertEquals(ascendente, ordenamiento.mergeSort(descendente, natural));
        assertEquals(ascendente, ordenamiento.mergeSort(ascendente, natural));
    }

    @Test
    void quickSortConTodosLosElementosIguales() {
        // peor caso de Lomuto (el pivote cae siempre en un extremo), pero el resultado es correcto
        List<Item> iguales = List.of(
                new Item("a", "a", 2, 0, null),
                new Item("b", "b", 2, 0, null),
                new Item("c", "c", 2, 0, null),
                new Item("d", "d", 2, 0, null),
                new Item("e", "e", 2, 0, null));
        List<Item> ordenada = ordenamiento.quickSort(iguales, POR_PESO);
        assertEquals(5, ordenada.size());
        assertEquals(List.of(2.0, 2.0, 2.0, 2.0, 2.0), pesos(ordenada));
    }

    @Test
    void mergeSortDescendenteConEmpatesSigueSiendoEstable() {
        // por peso descendente: S3 7, S7 6, {S6, S8} 5 (S6 sigue antes que S8), S2 4, S1 3, S5 2, S4 1
        assertEquals(List.of("S3", "S7", "S6", "S8", "S2", "S1", "S5", "S4"),
                ids(ordenamiento.mergeSort(suministros(), POR_PESO.reversed())));
    }
}
