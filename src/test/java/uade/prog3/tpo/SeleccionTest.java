package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.algorithm.Seleccion;
import uade.prog3.tpo.algorithm.Seleccion.Resultado;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.seed.DominioCaba;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de greedy 0/1 por ratio y mochila 0/1 con PD, calculados a mano. No tocan Neo4j.
 *
 * Suministros del dominio (kg / utilidad / ratio):
 *   S1 3/50 16,67 - S2 4/40 10 - S4 1/8 8 - S3 7/55 7,86 - S5 2/14 7
 *   S7 6/30 5 - S8 5/24 4,8 - S6 5/10 2   (en orden de ratio descendente)
 * Los elegidos se comparan como CONJUNTO de ids.
 */
class SeleccionTest {

    private final Seleccion seleccion = new Seleccion(new Ordenamiento());

    private static List<Item> suministros() {
        List<Item> items = new ArrayList<>();
        for (DominioCaba.Suministro s : DominioCaba.SUMINISTROS) {
            items.add(new Item(s.id(), s.nombre(), s.kg(), s.utilidad(), s.lugarId()));
        }
        return items;
    }

    private static Item item(String id, double peso, double valor) {
        return new Item(id, id, peso, valor, null);
    }

    private static Set<String> ids(Resultado r) {
        Set<String> ids = new HashSet<>();
        for (Item item : r.elegidos()) {
            ids.add(item.getId());
        }
        return ids;
    }

    private void verificar(Resultado r, double valor, Set<String> elegidos) {
        assertEquals(valor, r.valorTotal(), 1e-9);
        assertEquals(elegidos, ids(r));
    }

    private void greedy(int capacidad, double valor, Set<String> elegidos) {
        Resultado r = seleccion.greedy(suministros(), capacidad);
        verificar(r, valor, elegidos);
        assertEquals("GREEDY", r.tecnica());
    }

    private void dinamica(int capacidad, double valor, Set<String> elegidos) {
        Resultado r = seleccion.programacionDinamica(suministros(), capacidad);
        verificar(r, valor, elegidos);
        assertEquals("PROGRAMACION_DINAMICA", r.tecnica());
    }

    @Test
    void capacidadCero() {
        greedy(0, 0, Set.of());
        dinamica(0, 0, Set.of());
    }

    @Test
    void capacidadDos() {
        // greedy: S1 (3) no entra, S2 no, S4 (1) entra, S3 no, S5 (2) ya no entra -> 8
        greedy(2, 8, Set.of("S4"));
        dinamica(2, 14, Set.of("S5"));
    }

    @Test
    void capacidadCinco() {
        // greedy: S1 (3), S2 no, S4 (1) -> 4 kg 58; S5 ya no entra. Optimo: S1 + S5 = 64
        greedy(5, 58, Set.of("S1", "S4"));
        dinamica(5, 64, Set.of("S1", "S5"));
    }

    @Test
    void capacidadOchoCoinciden() {
        greedy(8, 98, Set.of("S1", "S2", "S4"));
        dinamica(8, 98, Set.of("S1", "S2", "S4"));
    }

    @Test
    void capacidadDiezCoinciden() {
        greedy(10, 112, Set.of("S1", "S2", "S4", "S5"));
        dinamica(10, 112, Set.of("S1", "S2", "S4", "S5"));
    }

    @Test
    void capacidadDoceGreedyNoEsOptimo() {
        // Contraejemplo principal. Greedy: S1 3 + S2 4 + S4 1 = 8 kg; S3 (7) NO entra y se
        // SALTEA; S5 (2) entra -> 10 kg, 112. Si el greedy cortara en S3 daria 98.
        Resultado g = seleccion.greedy(suministros(), 12);
        verificar(g, 112, Set.of("S1", "S2", "S4", "S5"));
        assertEquals(10, g.pesoTotal(), 1e-9);
        // Optimo: S1 50 + S3 55 + S5 14 = 119 con 12 kg
        Resultado d = seleccion.programacionDinamica(suministros(), 12);
        verificar(d, 119, Set.of("S1", "S3", "S5"));
        assertEquals(12, d.pesoTotal(), 1e-9);
    }

    @Test
    void capacidadQuinceCoinciden() {
        greedy(15, 153, Set.of("S1", "S2", "S3", "S4"));
        dinamica(15, 153, Set.of("S1", "S2", "S3", "S4"));
    }

    @Test
    void capacidadVeinte() {
        // greedy: S1 S2 S4 S3 S5 = 17 kg 167; S7 (6), S8 (5), S6 (5) no entran en 3 kg
        greedy(20, 167, Set.of("S1", "S2", "S3", "S4", "S5"));
        // optimo: cambiar S5 (2 kg, 14) por S8 (5 kg, 24) -> 20 kg 177
        dinamica(20, 177, Set.of("S1", "S2", "S3", "S4", "S8"));
    }

    @Test
    void capacidadIgualOMayorALaSumaTomaTodo() {
        Set<String> todos = Set.of("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8");
        // suma de pesos 33, suma de utilidades 231
        greedy(33, 231, todos);
        dinamica(33, 231, todos);
        // 40 > 33: la PD acota W a la suma de pesos y da lo mismo
        greedy(40, 231, todos);
        dinamica(40, 231, todos);
    }

    @Test
    void fichajesClaseSeisCasoDondeAcierta() {
        // ratios J3 70/4 = 17,5 > J2 16,7 > J1 12. Greedy: J3 (4) + J2 (3) = 7; J1 (5) no entra en 1
        List<Item> jugadores = List.of(item("J1", 5, 60), item("J2", 3, 50), item("J3", 4, 70));
        verificar(seleccion.greedy(jugadores, 8), 120, Set.of("J2", "J3"));
        verificar(seleccion.programacionDinamica(jugadores, 8), 120, Set.of("J2", "J3"));
    }

    @Test
    void fichajesClaseSeisCasoDondeFallaSegunLaDiapositiva() {
        // La diapositiva 7 ("Greedy por ratio") toma primero J3 y da 90, pero J3 tiene ratio 15
        // y J2 16,7: en realidad esta ordenando por VALOR (J3 90 > J1 60 > J2 50).
        // Por valor: J3 (6), J1 (5) no entra en 2, J2 (3) no entra -> 90.
        List<Item> jugadores = List.of(item("J1", 5, 60), item("J2", 3, 50), item("J3", 6, 90));
        Comparator<Item> porValor = Comparator.comparingDouble(Item::getValor).reversed();
        verificar(seleccion.greedy(jugadores, 8, porValor), 90, Set.of("J3"));
        // Por ratio bien hecho: J2 (3), J3 (6) no entra en 5 y se saltea, J1 (5) entra -> 110,
        // que coincide con el optimo de la PD.
        verificar(seleccion.greedy(jugadores, 8), 110, Set.of("J1", "J2"));
        verificar(seleccion.programacionDinamica(jugadores, 8), 110, Set.of("J1", "J2"));
    }

    @Test
    void compararDevuelveAmbasTecnicas() {
        Seleccion.Comparacion c = seleccion.comparar(suministros(), 12);
        verificar(c.greedy(), 112, Set.of("S1", "S2", "S4", "S5"));
        verificar(c.dinamica(), 119, Set.of("S1", "S3", "S5"));
    }

    @Test
    void criterioPorValorDaOtraSeleccion() {
        // por valor: S3 55 (7 kg) entra, S1 (3) no, S2 no, S7 no, S8 no, S5 (2) no, S6 no, S4 (1) entra
        Comparator<Item> porValor = Comparator.comparingDouble(Item::getValor).reversed();
        verificar(seleccion.greedy(suministros(), 8, porValor), 63, Set.of("S3", "S4"));
        // por ratio con la misma capacidad: 98
        verificar(seleccion.greedy(suministros(), 8), 98, Set.of("S1", "S2", "S4"));
    }

    @Test
    void dinamicaDevuelveEnOrdenDeEntrada() {
        List<String> orden = new ArrayList<>();
        for (Item item : seleccion.programacionDinamica(suministros(), 12).elegidos()) {
            orden.add(item.getId());
        }
        assertEquals(List.of("S1", "S3", "S5"), orden);
    }

    @Test
    void capacidadNegativaFalla() {
        assertThrows(IllegalArgumentException.class, () -> seleccion.greedy(suministros(), -1));
        assertThrows(IllegalArgumentException.class, () -> seleccion.programacionDinamica(suministros(), -1));
    }

    @Test
    void capacidadNoFinitaFalla() {
        assertThrows(IllegalArgumentException.class, () -> seleccion.greedy(suministros(), Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> seleccion.greedy(suministros(), Double.POSITIVE_INFINITY));
    }

    @Test
    void pesoOValorNegativoFalla() {
        List<Item> pesoNegativo = List.of(item("X", -1, 5));
        List<Item> valorNegativo = List.of(item("X", 1, -5));
        assertThrows(IllegalArgumentException.class, () -> seleccion.greedy(pesoNegativo, 10));
        assertThrows(IllegalArgumentException.class, () -> seleccion.programacionDinamica(valorNegativo, 10));
    }

    @Test
    void pesoNoEnteroFallaEnDinamica() {
        List<Item> items = List.of(item("X", 1.5, 5));
        assertThrows(IllegalArgumentException.class, () -> seleccion.programacionDinamica(items, 10));
        // greedy no indexa una tabla: acepta pesos reales
        verificar(seleccion.greedy(items, 10), 5, Set.of("X"));
    }
}
