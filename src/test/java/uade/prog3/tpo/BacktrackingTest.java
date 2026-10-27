package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Backtracking;
import uade.prog3.tpo.algorithm.Backtracking.ResultadoRutas;
import uade.prog3.tpo.algorithm.Backtracking.Ruta;
import uade.prog3.tpo.service.Grafo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del backtracking de rutas simples, calculados a mano siguiendo el
 * orden de las listas de adyacencia (ver RecorridosTest). No tocan Neo4j.
 *
 * Nodo explorado = cada entrada a la recursion, raiz incluida. Sin poda la
 * busqueda solo respeta el camino simple, asi que su cantidad de nodos no
 * depende de los limites (es la cantidad de caminos simples que salen del
 * origen sin pasar por el destino, mas los que terminan en el).
 */
class BacktrackingTest {

    private final Backtracking backtracking = new Backtracking();

    private static Ruta ruta(double costo, String... vertices) {
        return new Ruta(List.of(vertices), costo);
    }

    private ResultadoRutas rutas(Grafo g, String origen, String destino, double costo, int saltos) {
        ResultadoRutas r = backtracking.rutasSimples(g, origen, destino, costo, saltos);
        verificarCaminosSimples(r);
        return r;
    }

    private static void verificarCaminosSimples(ResultadoRutas r) {
        for (Ruta ruta : r.rutas()) {
            assertEquals(ruta.vertices().size(), new HashSet<>(ruta.vertices()).size(),
                    "La ruta repite vertices: " + ruta.vertices());
        }
    }

    private static void verificarNodos(ResultadoRutas r, long conPoda, long sinPoda) {
        assertEquals(conPoda, r.nodosConPoda(), "nodos con poda");
        assertEquals(sinPoda, r.nodosSinPoda(), "nodos sin poda");
    }

    // ---------- semilla de la catedra (A..H) ----------

    private static final List<Ruta> SEIS_RUTAS_A_H = List.of(
            ruta(23, "A", "B", "C", "D", "E", "F", "H"),
            ruta(20, "A", "B", "C", "D", "F", "H"),
            ruta(25, "A", "B", "D", "E", "F", "H"),
            ruta(22, "A", "B", "D", "F", "H"),
            ruta(25, "A", "C", "D", "E", "F", "H"),
            ruta(22, "A", "C", "D", "F", "H"));

    @Test
    void semillaDirigida() {
        ResultadoRutas r = rutas(GrafosDePrueba.semillaCatedra(true), "A", "H", 25, 10);
        assertEquals(SEIS_RUTAS_A_H, r.rutas());
        verificarNodos(r, 26, 38);
    }

    @Test
    void semillaNoDirigida() {
        // no dirigida aparecen mas caminos (por ejemplo A-C-B-D-...), pero todos superan 25
        ResultadoRutas r = rutas(GrafosDePrueba.semillaCatedra(false), "A", "H", 25, 10);
        assertEquals(SEIS_RUTAS_A_H, r.rutas());
        verificarNodos(r, 37, 78);
    }

    @Test
    void semillaNoDirigidaCincoSaltos() {
        // se pierde A-B-C-D-E-F-H (6 saltos)
        ResultadoRutas r = rutas(GrafosDePrueba.semillaCatedra(false), "A", "H", 25, 5);
        assertEquals(SEIS_RUTAS_A_H.subList(1, 6), r.rutas());
        verificarNodos(r, 36, 78);
    }

    @Test
    void semillaNoDirigidaCuatroSaltos() {
        ResultadoRutas r = rutas(GrafosDePrueba.semillaCatedra(false), "A", "H", 25, 4);
        assertEquals(List.of(ruta(22, "A", "B", "D", "F", "H"), ruta(22, "A", "C", "D", "F", "H")),
                r.rutas());
        verificarNodos(r, 30, 78);
    }

    // ---------- dominio CABA ----------

    @Test
    void retiroCiudadUniversitaria150Minutos4Saltos() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "RET", "CUN", 150, 4);
        assertEquals(List.of(
                ruta(135, "RET", "PAL", "CUN"),
                ruta(129, "RET", "PAL", "MON", "CUN"),
                ruta(140, "RET", "REC", "PAL", "CUN"),
                ruta(134, "RET", "REC", "PAL", "MON", "CUN")), r.rutas());
        verificarNodos(r, 118, 684);
    }

    @Test
    void retiroCiudadUniversitaria130Minutos() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "RET", "CUN", 130, 5);
        assertEquals(List.of(ruta(129, "RET", "PAL", "MON", "CUN")), r.rutas());
        verificarNodos(r, 144, 684);
    }

    @Test
    void constitucionParqueCentenario() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "CON", "PCE", 90, 4);
        assertEquals(List.of(
                ruta(73, "CON", "ABA", "HIT", "PCE"),
                ruta(66, "CON", "ABA", "PCE"),
                ruta(89, "CON", "OBE", "ABA", "PCE")), r.rutas());
        verificarNodos(r, 42, 684);
    }

    @Test
    void obeliscoHospitalItaliano() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "OBE", "HIT", 80, 3);
        assertEquals(List.of(
                ruta(47, "OBE", "ABA", "HIT"),
                ruta(76, "OBE", "ABA", "PCE", "HIT"),
                ruta(73, "OBE", "REC", "ABA", "HIT"),
                ruta(76, "OBE", "REC", "PAL", "HIT")), r.rutas());
        verificarNodos(r, 34, 333);
    }

    @Test
    void sinRutasValidas() {
        // la mas rapida (RET-PAL-MON-CUN) tarda 129 > 110
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "RET", "CUN", 110, 3);
        assertTrue(r.rutas().isEmpty());
        verificarNodos(r, 47, 684);
    }

    @Test
    void limitesHolgadosDanTodosLosCaminosSimples() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "RET", "CUN", 1000, 10);
        assertEquals(110, r.rutas().size());
        verificarNodos(r, 682, 684);
    }

    @Test
    void origenIgualADestino() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "RET", "RET", 100, 5);
        assertEquals(List.of(ruta(0, "RET")), r.rutas());
        verificarNodos(r, 1, 1);
    }

    @Test
    void conYSinPodaDevuelvenLasMismasRutas() {
        // con costoMaximo y saltos enormes la poda nunca se activa: es la corrida "sin poda"
        Grafo g = GrafosDePrueba.dominioCaba();
        List<Ruta> todas = rutas(g, "RET", "CUN", 1e9, 1000).rutas();
        Set<List<String>> filtradas = new HashSet<>();
        for (Ruta ruta : todas) {
            if (ruta.costoTotal() <= 150 && ruta.vertices().size() - 1 <= 4) {
                filtradas.add(ruta.vertices());
            }
        }
        Set<List<String>> conPoda = new HashSet<>();
        for (Ruta ruta : rutas(g, "RET", "CUN", 150, 4).rutas()) {
            conPoda.add(ruta.vertices());
        }
        assertEquals(filtradas, conPoda);
    }

    // ---------- errores ----------

    @Test
    void limitesNegativosFallan() {
        Grafo g = GrafosDePrueba.dominioCaba();
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "RET", "CUN", -1, 4));
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "RET", "CUN", 100, -1));
    }

    @Test
    void nodoInexistenteFalla() {
        Grafo g = GrafosDePrueba.dominioCaba();
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "XXX", "CUN", 100, 4));
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "RET", "XXX", 100, 4));
    }
}
