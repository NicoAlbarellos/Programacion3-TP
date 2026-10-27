package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.RamificacionYPoda;
import uade.prog3.tpo.algorithm.RamificacionYPoda.Asignacion;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.seed.DominioCaba;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del Branch and Bound de reparto (makespan). No tocan Neo4j.
 *
 * Suministros en orden de id (kg): S1 3, S2 4, S3 7, S4 1, S5 2, S6 5, S7 6, S8 5. Total 33.
 * El optimo se compara contra fuerza bruta (k^n repartos) y los contadores de nodos
 * contra una simulacion del mismo recorrido (raiz incluida, cota max(carga, 33/m), m = min(k, n)).
 */
class RamificacionYPodaTest {

    private final RamificacionYPoda bb = new RamificacionYPoda();

    private static List<Item> suministros() {
        List<Item> items = new ArrayList<>();
        for (DominioCaba.Suministro s : DominioCaba.SUMINISTROS) {
            items.add(new Item(s.id(), s.nombre(), s.kg(), s.utilidad(), s.lugarId()));
        }
        return items;
    }

    private static List<Set<String>> grupos(Asignacion a) {
        List<Set<String>> grupos = new ArrayList<>();
        for (List<Item> contenedor : a.porContenedor().values()) {
            Set<String> ids = new HashSet<>();
            for (Item item : contenedor) {
                ids.add(item.getId());
            }
            grupos.add(ids);
        }
        return grupos;
    }

    /** Cada item aparece exactamente una vez y la carga mas alta coincide con cargaMaxima. */
    private static void verificarReparto(Asignacion a, int contenedoresEsperados) {
        assertEquals(contenedoresEsperados, a.porContenedor().size());
        List<String> vistos = new ArrayList<>();
        double maxima = 0;
        for (List<Item> contenedor : a.porContenedor().values()) {
            double carga = 0;
            for (Item item : contenedor) {
                vistos.add(item.getId());
                carga += item.getPeso();
            }
            maxima = Math.max(maxima, carga);
        }
        assertEquals(8, vistos.size());
        assertEquals(8, new HashSet<>(vistos).size());
        assertEquals(maxima, a.cargaMaxima(), 1e-9);
    }

    /** Fuerza bruta: prueba los k^n repartos posibles. Solo para verificar. */
    private static double fuerzaBruta(double[] pesos, int k) {
        return fuerzaBruta(pesos, 0, new double[k]);
    }

    private static double fuerzaBruta(double[] pesos, int i, double[] cargas) {
        if (i == pesos.length) {
            double maxima = 0;
            for (double c : cargas) {
                maxima = Math.max(maxima, c);
            }
            return maxima;
        }
        double mejor = Double.POSITIVE_INFINITY;
        for (int c = 0; c < cargas.length; c++) {
            cargas[c] += pesos[i];
            mejor = Math.min(mejor, fuerzaBruta(pesos, i + 1, cargas));
            cargas[c] -= pesos[i];
        }
        return mejor;
    }

    private static void verificarContadores(Asignacion a, double carga, double cota, long explorados, long podados) {
        assertEquals(carga, a.cargaMaxima(), 1e-9);
        assertEquals(cota, a.cotaInicial(), 1e-9);
        assertEquals(explorados, a.nodosExplorados(), "nodos explorados");
        assertEquals(podados, a.nodosPodados(), "nodos podados");
    }

    @Test
    void unContenedorLlevaTodo() {
        // un solo camino de 8 niveles: 9 nodos, nada que podar
        Asignacion a = bb.repartir(suministros(), 1);
        verificarContadores(a, 33, 33, 9, 0);
        verificarReparto(a, 1);
    }

    @Test
    void dosContenedores() {
        // 33 / 2 = 16,5 -> con pesos enteros el optimo es 17
        Asignacion a = bb.repartir(suministros(), 2);
        verificarContadores(a, 17, 16.5, 84, 74);
        verificarReparto(a, 2);
        assertEquals(List.of(Set.of("S1", "S2", "S3", "S4", "S5"), Set.of("S6", "S7", "S8")), grupos(a));
    }

    @Test
    void tresContenedoresAlcanzaLaCota() {
        // 33 / 3 = 11 y hay un reparto perfecto: 3+7+1, 4+2+5, 6+5
        Asignacion a = bb.repartir(suministros(), 3);
        verificarContadores(a, 11, 11, 61, 87);
        verificarReparto(a, 3);
        assertEquals(List.of(Set.of("S1", "S3", "S4"), Set.of("S2", "S5", "S6"), Set.of("S7", "S8")), grupos(a));
    }

    @Test
    void cuatroContenedores() {
        // 33 / 4 = 8,25 pero el optimo es 9: S3 (7) necesita compania de 2 kg y no alcanza para todos
        Asignacion a = bb.repartir(suministros(), 4);
        verificarContadores(a, 9, 8.25, 123, 291);
        verificarReparto(a, 4);
        assertEquals(List.of(Set.of("S1", "S4", "S6"), Set.of("S2", "S8"), Set.of("S3", "S5"), Set.of("S7")),
                grupos(a));
    }

    @Test
    void optimoCoincideConFuerzaBruta() {
        double[] pesos = {3, 4, 7, 1, 2, 5, 6, 5};
        for (int k = 2; k <= 4; k++) {
            assertEquals(fuerzaBruta(pesos, k), bb.repartir(suministros(), k).cargaMaxima(), 1e-9, "k = " + k);
        }
    }

    @Test
    void masContenedoresQueItems() {
        // con 8 o mas contenedores cada item va solo: el optimo es el item mas pesado (S3, 7 kg)
        Asignacion ocho = bb.repartir(suministros(), 8);
        verificarContadores(ocho, 7, 33.0 / 8, 50, 72);
        verificarReparto(ocho, 8);

        Asignacion nueve = bb.repartir(suministros(), 9);
        verificarContadores(nueve, 7, 33.0 / 8, 50, 72); // m = min(9, 8) = 8
        verificarReparto(nueve, 8); // solo min(k, n) = 8 contenedores
    }

    @Test
    void kEnormeNoReservaMemoria() {
        Asignacion a = bb.repartir(suministros(), 1_000_000);
        assertEquals(7, a.cargaMaxima(), 1e-9);
        assertEquals(4.125, a.cotaInicial(), 1e-9); // 33 / min(k, n) = 33 / 8
        verificarReparto(a, 8);
    }

    @Test
    void nombresDeLosContenedores() {
        Map<String, List<Item>> mapa = bb.repartir(suministros(), 3).porContenedor();
        assertEquals(List.of("Sobreviviente 1", "Sobreviviente 2", "Sobreviviente 3"), new ArrayList<>(mapa.keySet()));
    }

    @Test
    void resultadoEsInmodificable() {
        Asignacion a = bb.repartir(suministros(), 2);
        assertThrows(UnsupportedOperationException.class, () -> a.porContenedor().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> a.porContenedor().get("Sobreviviente 1").clear());
    }

    @Test
    void listaVaciaDaCargaCero() {
        Asignacion a = bb.repartir(List.of(), 3);
        assertEquals(0, a.cargaMaxima(), 1e-9);
        assertEquals(0, a.cotaInicial(), 1e-9);
        assertTrue(a.porContenedor().isEmpty());
        assertEquals(1, a.nodosExplorados());
    }

    @Test
    void entradasInvalidasFallan() {
        assertThrows(IllegalArgumentException.class, () -> bb.repartir(suministros(), 0));
        assertThrows(IllegalArgumentException.class, () -> bb.repartir(null, 2));
        List<Item> pesoNegativo = List.of(new Item("X", "X", -1, 5, null));
        assertThrows(IllegalArgumentException.class, () -> bb.repartir(pesoNegativo, 2));
    }
}
