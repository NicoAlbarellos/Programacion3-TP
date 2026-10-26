package uade.prog3.tpo.service;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Carga el grafo desde Neo4j a memoria.
 *
 * Decision de diseno importante para el trabajo: los algoritmos NO consultan
 * la base. Se carga el grafo una vez, se opera en memoria, y se devuelve el
 * resultado. Asi la complejidad temporal que se mide y se justifica en el
 * coloquio es la del algoritmo, no la de la red ni la del motor de base.
 *
 * La carga es DETERMINISTA: findAll() no garantiza orden, y en un grafo no
 * dirigido el orden de las listas de adyacencia depende del orden en que se
 * agregan las aristas. Por eso se piden vertices y aristas ORDENADOS por id
 * (ORDER BY en la base, dos consultas antes de cualquier algoritmo). Asi BFS
 * y DFS devuelven siempre el mismo recorrido.
 *
 * El orden de los vecinos depende del sentido en que se guardo cada calle
 * (orden por origen y destino): otro orden daria otro recorrido, igual de valido.
 *
 * Si una calle no tiene costo se lanza IllegalStateException (respuesta 422
 * con mensaje claro, no un 500).
 */
@Service
public class GrafoService {

    private static final Logger log = LoggerFactory.getLogger(GrafoService.class);

    private static final String CONSULTA_VERTICES =
            "MATCH (n:Nodo) RETURN n.id AS id ORDER BY n.id";

    private static final String CONSULTA_ARISTAS =
            "MATCH (a:Nodo)-[r:CONECTA]->(b:Nodo) "
            + "RETURN a.id AS origen, b.id AS destino, r.costo AS costo "
            + "ORDER BY a.id, b.id";

    /** Fila de la consulta de aristas, tal como esta guardada en la base. */
    private record AristaGuardada(String origen, String destino, double costo) { }

    private final Neo4jClient neo4jClient;

    public GrafoService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /**
     * @param dirigido true si las relaciones CONECTA tienen sentido unico.
     *                 Para el arbol de recubrimiento minimo (Prim y Kruskal)
     *                 el grafo DEBE cargarse como NO dirigido.
     */
    public Grafo cargar(boolean dirigido) {
        Grafo grafo = new Grafo(dirigido);

        // 1) primero todos los vertices, para que queden en el grafo
        //    incluso los que no tienen ninguna arista
        for (String id : idsDeVertices()) {
            grafo.agregarVertice(id);
        }

        // 2) despues las aristas, con su costo
        for (AristaGuardada a : aristasGuardadas()) {
            grafo.agregarArista(a.origen(), a.destino(), a.costo());
        }

        log.info("Grafo cargado: {} vertices, {} aristas, dirigido={}",
                grafo.cantidadVertices(), grafo.cantidadAristas(), dirigido);
        return grafo;
    }

    private Collection<String> idsDeVertices() {
        return neo4jClient.query(CONSULTA_VERTICES)
                .fetchAs(String.class)
                .mappedBy((tipos, fila) -> fila.get("id").asString())
                .all();
    }

    private Collection<AristaGuardada> aristasGuardadas() {
        return neo4jClient.query(CONSULTA_ARISTAS)
                .fetchAs(AristaGuardada.class)
                .mappedBy((tipos, fila) -> aristaDesdeFila(fila))
                .all();
    }

    private AristaGuardada aristaDesdeFila(Record fila) {
        String origen = fila.get("origen").asString();
        String destino = fila.get("destino").asString();
        Value costo = fila.get("costo");
        if (costo.isNull()) {
            throw new IllegalStateException("La calle " + origen + "-" + destino + " no tiene costo");
        }
        return new AristaGuardada(origen, destino, costo.asNumber().doubleValue());
    }
}
