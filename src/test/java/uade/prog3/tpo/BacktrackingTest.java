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
    // Sin poda la corrida enumera TODOS los caminos simples desde el origen
    // (desde RET: 807.257 nodos), con poda solo una fraccion.

    @Test
    void obeliscoHospitalItaliano80Minutos3Saltos() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "OBE", "HIT", 80, 3);
        assertEquals(List.of(ruta(60, "OBE", "CGR", "ABA", "HIT")), r.rutas());
        verificarNodos(r, 27, 367_114);
    }

    @Test
    void laBocaHospitalItaliano140Minutos5Saltos() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "LBO", "HIT", 140, 5);
        assertEquals(List.of(
                ruta(107, "LBO", "CON", "CGR", "ABA", "HIT"),
                ruta(134, "LBO", "CON", "HGA", "BOE", "ABA", "HIT"),
                ruta(132, "LBO", "CON", "OBE", "CGR", "ABA", "HIT"),
                ruta(138, "LBO", "HGA", "BOE", "ABA", "HIT"),
                ruta(127, "LBO", "SAN", "CON", "CGR", "ABA", "HIT")), r.rutas());
        verificarNodos(r, 106, 522_659);
    }

    @Test
    void garrahanLaBocaDevuelveElAtajoYLaCalleDirecta() {
        // HGA-CON-LBO 47 y la calle directa HGA-LBO 51; cualquier otra pasa los 60 minutos
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "HGA", "LBO", 60, 3);
        assertEquals(List.of(ruta(47, "HGA", "CON", "LBO"), ruta(51, "HGA", "LBO")), r.rutas());
        verificarNodos(r, 9, 489_963);
    }

    @Test
    void retiroCiudadUniversitaria180Minutos6Saltos() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "RET", "CUN", 180, 6);
        assertEquals(List.of(ruta(154, "RET", "REC", "PAL", "PLA", "AEP", "CUN")), r.rutas());
        verificarNodos(r, 305, 807_257);
    }

    @Test
    void sinRutasValidas() {
        // la mas rapida (RET-REC-PAL-PLA-AEP-CUN) tarda 154 > 150
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "RET", "CUN", 150, 5);
        assertTrue(r.rutas().isEmpty());
        verificarNodos(r, 153, 807_257);
    }

    @Test
    void limitesPorDefectoDelEndpoint() {
        // mismos limites que los defaults de GrafoController.rutas (costoMaximo=200, saltosMaximos=8)
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "RET", "CUN", 200, 8);
        assertEquals(List.of(
                ruta(182, "RET", "OBE", "REC", "PAL", "PLA", "AEP", "CUN"),
                ruta(199, "RET", "REC", "PAL", "CHA", "COL", "BEL", "MON", "CUN"),
                ruta(154, "RET", "REC", "PAL", "PLA", "AEP", "CUN")), r.rutas());
        verificarNodos(r, 542, 807_257);
    }

    @Test
    void conLimitesHolgadosLaPodaCasiNoAhorra() {
        // RES solo se alcanza por la calle PMA-RES: hay UNA ruta, pero con 1000 min y 30 saltos
        // casi ninguna rama se pasa de los limites y la busqueda igual recorre todo el mapa
        // (ahorro de la poda: 0,5 %). Las podas sirven cuando los limites son ajustados.
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "PMA", "RES", 1000, 30);
        assertEquals(List.of(ruta(15, "PMA", "RES")), r.rutas());
        verificarNodos(r, 1_245_922, 1_252_462);
    }

    @Test
    void limitesHolgadosDevuelvenMuchasRutas() {
        ResultadoRutas r = rutas(GrafosDePrueba.dominioCaba(), "RET", "CUN", 1000, 10);
        assertEquals(145, r.rutas().size());
        verificarNodos(r, 6_018, 807_257);
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
            if (ruta.costoTotal() <= 200 && ruta.vertices().size() - 1 <= 8) {
                filtradas.add(ruta.vertices());
            }
        }
        Set<List<String>> conPoda = new HashSet<>();
        for (Ruta ruta : rutas(g, "RET", "CUN", 200, 8).rutas()) {
            conPoda.add(ruta.vertices());
        }
        assertEquals(3, conPoda.size());
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
