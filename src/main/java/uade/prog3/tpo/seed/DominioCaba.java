package uade.prog3.tpo.seed;

import java.util.List;

/**
 * Datos del dominio: apocalipsis zombie en CABA. Unica fuente de verdad,
 * la usan la carga inicial (CargaInicial) y los tests (GrafosDePrueba).
 *
 *   - Lugar:      refugio o punto de interes real de la Ciudad
 *                 (capacidad = personas que puede albergar).
 *   - Calle:      trayecto a pie entre dos lugares vecinos, costo en minutos.
 *                 Grafo NO dirigido: cada calle se guarda UNA sola vez.
 *   - Suministro: peso en kg, utilidad para sobrevivir.
 */
public final class DominioCaba {

    public record Lugar(String id, String nombre, String tipo, double capacidad) { }

    public record Calle(String origen, String destino, double minutos) { }

    public record Suministro(String id, String nombre, double kg, double utilidad, String lugarId) { }

    private DominioCaba() {
    }

    public static final List<Lugar> LUGARES = List.of(
            new Lugar("ABA", "Abasto", "REFUGIO", 400),
            new Lugar("CON", "Constitucion", "ESTACION", 250),
            new Lugar("CUN", "Ciudad Universitaria", "REFUGIO", 500),
            new Lugar("HIT", "Hospital Italiano", "HOSPITAL", 200),
            new Lugar("LBO", "La Boca - Caminito", "PUNTO", 60),
            new Lugar("MON", "Estadio Monumental", "REFUGIO", 1000),
            new Lugar("OBE", "Obelisco", "PUNTO", 80),
            new Lugar("PAL", "Plaza Italia - Palermo", "PUNTO", 100),
            new Lugar("PCE", "Parque Centenario", "REFUGIO", 150),
            new Lugar("PMA", "Puerto Madero", "PUNTO", 120),
            new Lugar("REC", "Recoleta", "PUNTO", 90),
            new Lugar("RET", "Estacion Retiro", "ESTACION", 300)
    );

    /**
     * Escritas YA ordenadas por (origen, destino) y guardadas en ese sentido:
     * es el mismo orden que devuelve la consulta ORDER BY de GrafoService,
     * asi el grafo de los tests tiene las mismas listas de adyacencia que la app.
     */
    public static final List<Calle> CALLES = List.of(
            new Calle("ABA", "HIT", 19),
            new Calle("ABA", "PCE", 30),
            // El arbol de recubrimiento minimo es unico, costo total 287.
            new Calle("ABA", "REC", 29),
            new Calle("CON", "ABA", 36),
            new Calle("CON", "LBO", 29),
            new Calle("HIT", "PAL", 21),
            new Calle("HIT", "PCE", 18),
            new Calle("MON", "CUN", 18),
            new Calle("OBE", "ABA", 28),
            new Calle("OBE", "CON", 31),
            new Calle("OBE", "PMA", 20),
            new Calle("OBE", "REC", 25),
            // El directo PAL-CUN cuesta 90, pero PAL->MON->CUN cuesta 66+18=84.
            // Sirve para verificar que la relajacion de aristas este bien hecha.
            new Calle("PAL", "CUN", 90),
            new Calle("PAL", "MON", 66),
            new Calle("PMA", "CON", 30),
            new Calle("PMA", "LBO", 36),
            new Calle("REC", "PAL", 30),
            new Calle("RET", "OBE", 22),
            new Calle("RET", "PAL", 45),
            new Calle("RET", "PMA", 18),
            new Calle("RET", "REC", 20)
    );

    /**
     * Calibrado a proposito: con capacidad 12, greedy por ratio valor/peso
     * elige {S1, S2, S4, S5} y obtiene 112, mientras que el optimo real es
     * {S1, S3, S5} con 119. Es el contraejemplo para comparar greedy contra
     * programacion dinamica.
     */
    public static final List<Suministro> SUMINISTROS = List.of(
            new Suministro("S1", "Botiquin", 3, 50, "HIT"),
            new Suministro("S2", "Comida enlatada", 4, 40, "ABA"),
            new Suministro("S3", "Agua 7L", 7, 55, "PCE"),
            new Suministro("S4", "Linterna", 1, 8, "OBE"),
            new Suministro("S5", "Radio", 2, 14, "RET"),
            new Suministro("S6", "Bate", 5, 10, "MON"),
            new Suministro("S7", "Bolsa de dormir", 6, 30, "CUN"),
            new Suministro("S8", "Herramientas", 5, 24, "CON")
    );
}
