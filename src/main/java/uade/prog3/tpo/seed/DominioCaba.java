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

    /**
     * 31 lugares, ordenados por id (el mismo orden que la consulta ORDER BY de GrafoService).
     */
    public static final List<Lugar> LUGARES = List.of(
            new Lugar("ABA", "Abasto", "REFUGIO", 400),
            new Lugar("AEP", "Aeroparque", "ESTACION", 350),
            new Lugar("AGR", "Facultad de Agronomia", "REFUGIO", 600),
            new Lugar("BEL", "Barrancas de Belgrano", "PUNTO", 80),
            new Lugar("BOE", "Boedo", "PUNTO", 70),
            new Lugar("CAB", "Parque Rivadavia - Caballito", "PUNTO", 90),
            new Lugar("CGR", "Congreso", "PUNTO", 100),
            new Lugar("CHA", "Cementerio de la Chacarita", "PUNTO", 50),
            new Lugar("COL", "Plaza Mafalda - Colegiales", "PUNTO", 60),
            new Lugar("CON", "Constitucion", "ESTACION", 250),
            new Lugar("CUN", "Ciudad Universitaria", "REFUGIO", 500),
            new Lugar("DEV", "Plaza Arenales - Villa Devoto", "PUNTO", 70),
            new Lugar("FLO", "Plaza Flores", "PUNTO", 90),
            new Lugar("HGA", "Hospital Garrahan", "HOSPITAL", 300),
            new Lugar("HIT", "Hospital Italiano", "HOSPITAL", 200),
            new Lugar("LBO", "La Boca - Caminito", "PUNTO", 60),
            new Lugar("MAT", "Feria de Mataderos", "PUNTO", 80),
            new Lugar("MON", "Estadio Monumental", "REFUGIO", 1000),
            new Lugar("OBE", "Obelisco", "PUNTO", 80),
            new Lugar("PAL", "Plaza Italia - Palermo", "PUNTO", 100),
            new Lugar("PCE", "Parque Centenario", "REFUGIO", 150),
            new Lugar("PCH", "Parque Chacabuco", "REFUGIO", 200),
            new Lugar("PLA", "Planetario", "PUNTO", 70),
            new Lugar("PMA", "Puerto Madero", "PUNTO", 120),
            new Lugar("REC", "Recoleta", "PUNTO", 90),
            new Lugar("RES", "Reserva Ecologica Costanera Sur", "REFUGIO", 300),
            new Lugar("RET", "Estacion Retiro", "ESTACION", 300),
            new Lugar("SAA", "Parque Saavedra", "REFUGIO", 250),
            new Lugar("SAN", "Plaza Dorrego - San Telmo", "PUNTO", 70),
            new Lugar("VEL", "Estadio Velez Sarsfield", "REFUGIO", 800),
            new Lugar("VUR", "Villa Urquiza", "PUNTO", 80)
    );

    /**
     * 51 calles entre lugares vecinos (ninguna se cruza con otra en el mapa).
     *
     * Costo: minutos = round(distancia en linea recta (haversine, km) x factor x 12 min/km).
     * Factor 1,25 (la calle real es mas larga que la linea recta), salvo dos tramos
     * con rodeo real: AEP-BEL 1,6 y HGA-LBO 1,5.
     *
     * Propiedades que usan los tests (verificadas con script):
     *   - el arbol de recubrimiento minimo es UNICO: 30 aristas, costo total 835;
     *   - ningun par de lugares tiene dos caminos minimos empatados, asi que Dijkstra,
     *     UCS y Floyd devuelven siempre el mismo camino, sin depender del desempate.
     *
     * Escritas YA ordenadas por (origen, destino) y guardadas en ese sentido:
     * es el mismo orden que devuelve la consulta ORDER BY de GrafoService,
     * asi el grafo de los tests tiene las mismas listas de adyacencia que la app.
     */
    public static final List<Calle> CALLES = List.of(
            new Calle("ABA", "BOE", 36),
            new Calle("ABA", "CGR", 27),
            new Calle("ABA", "HIT", 15),
            new Calle("ABA", "REC", 36),
            // Factor 1,6: rodeo por las vias del Mitre, Av. Lugones y el cerco de Aeroparque.
            new Calle("AEP", "BEL", 59),
            new Calle("AEP", "CUN", 44),
            new Calle("AEP", "PLA", 21),
            new Calle("AGR", "CHA", 45),
            new Calle("AGR", "DEV", 43),
            new Calle("AGR", "VUR", 31),
            new Calle("BEL", "COL", 27),
            new Calle("BEL", "MON", 24),
            new Calle("BEL", "SAA", 44),
            new Calle("BOE", "CAB", 27),
            new Calle("BOE", "HGA", 36),
            new Calle("BOE", "PCH", 35),
            new Calle("CAB", "FLO", 43),
            new Calle("CAB", "PCE", 20),
            new Calle("CAB", "PCH", 29),
            new Calle("CGR", "CON", 33),
            new Calle("CGR", "OBE", 18),
            new Calle("CHA", "COL", 19),
            new Calle("CHA", "PAL", 43),
            new Calle("CHA", "PCE", 37),
            new Calle("COL", "PLA", 50),
            new Calle("COL", "VUR", 56),
            new Calle("CON", "HGA", 15),
            new Calle("CON", "LBO", 32),
            new Calle("CON", "OBE", 40),
            new Calle("CON", "SAN", 18),
            new Calle("CUN", "MON", 20),
            new Calle("DEV", "VEL", 58),
            new Calle("FLO", "MAT", 73),
            new Calle("FLO", "PCH", 36),
            new Calle("FLO", "VEL", 79),
            // Relajacion: el directo HGA-LBO cuesta 51 (factor 1,5: hay pocos pasos sobre
            // las vias del Roca y la autopista), pero HGA->CON->LBO cuesta 15+32=47.
            new Calle("HGA", "LBO", 51),
            new Calle("HIT", "PAL", 43),
            new Calle("HIT", "PCE", 20),
            new Calle("LBO", "SAN", 34),
            new Calle("MAT", "VEL", 45),
            new Calle("MON", "SAA", 43),
            new Calle("OBE", "PMA", 28),
            new Calle("OBE", "REC", 31),
            new Calle("OBE", "RET", 23),
            new Calle("PAL", "PLA", 23),
            new Calle("PAL", "REC", 40),
            new Calle("PMA", "RES", 15),
            new Calle("PMA", "RET", 36),
            new Calle("PMA", "SAN", 20),
            new Calle("REC", "RET", 26),
            new Calle("SAA", "VUR", 35)
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
