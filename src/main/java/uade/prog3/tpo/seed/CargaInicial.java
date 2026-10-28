package uade.prog3.tpo.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.ItemRepository;
import uade.prog3.tpo.repository.NodoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Guarda en Neo4j el dominio definido en {@link DominioCaba}
 * (lugares, calles y suministros). Solo se ejecuta si la base esta vacia.
 *
 * Es SmartInitializingSingleton y no CommandLineRunner: corre al terminar de
 * crear los beans, ANTES de que Tomcat acepte requests. Asi el primer pedido
 * ya ve el grafo completo.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class CargaInicial implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    /** Cada calle se guarda UNA sola vez, en el sentido origen -> destino. */
    private static final String CREAR_CALLES =
            "UNWIND $calles AS c "
            + "MATCH (a:Nodo {id: c.origen}), (b:Nodo {id: c.destino}) "
            + "CREATE (a)-[:CONECTA {costo: c.costo}]->(b) "
            + "RETURN count(*) AS creadas";

    private final NodoRepository nodoRepository;
    private final ItemRepository itemRepository;
    private final Neo4jClient neo4jClient;

    public CargaInicial(NodoRepository nodoRepository, ItemRepository itemRepository, Neo4jClient neo4jClient) {
        this.nodoRepository = nodoRepository;
        this.itemRepository = itemRepository;
        this.neo4jClient = neo4jClient;
    }

    @Override
    public void afterSingletonsInstantiated() {
        long lugaresEnBase = nodoRepository.count();
        if (lugaresEnBase > 0) {
            log.info("La base ya tiene datos. No se ejecuta la carga inicial.");
            if (lugaresEnBase != DominioCaba.LUGARES.size()) {
                // Pasa si la base quedo con una version anterior del mapa (p. ej. la de 12 lugares).
                log.warn("La base tiene {} lugares y el dominio actual tiene {}. Para recargarla: "
                        + "docker compose down -v && docker compose up -d",
                        lugaresEnBase, DominioCaba.LUGARES.size());
            }
            return;
        }
        cargarLugares();
        cargarCalles();
        cargarSuministros();
        log.info("Carga inicial completada: {} lugares de CABA, {} calles, {} suministros.",
                DominioCaba.LUGARES.size(), DominioCaba.CALLES.size(), DominioCaba.SUMINISTROS.size());
        log.info("Para desactivarla: app.seed.enabled=false");
    }

    private void cargarLugares() {
        List<Nodo> nodos = new ArrayList<>();
        for (DominioCaba.Lugar l : DominioCaba.LUGARES) {
            nodos.add(new Nodo(l.id(), l.nombre(), l.tipo(), l.capacidad()));
        }
        nodoRepository.saveAll(nodos);
    }

    /**
     * Las calles se crean con Cypher y no guardando los Nodo con sus conexiones:
     * con muchos ciclos el guardado en cascada de Spring Data Neo4j puede omitir
     * relaciones (con 31 lugares se perdian AEP-PLA y COL-PLA).
     * Una sola consulta, una transaccion; los algoritmos nunca tocan la base.
     *
     * @throws IllegalStateException si no se crearon todas las calles
     *                               (por ejemplo, una calle con un lugar inexistente)
     */
    private void cargarCalles() {
        List<Map<String, Object>> calles = new ArrayList<>();
        for (DominioCaba.Calle c : DominioCaba.CALLES) {
            calles.add(Map.of("origen", c.origen(), "destino", c.destino(), "costo", c.minutos()));
        }
        long creadas = neo4jClient.query(CREAR_CALLES)
                .bind(calles).to("calles")
                .fetchAs(Long.class).one()
                .orElse(0L);
        if (creadas != calles.size()) {
            throw new IllegalStateException("Carga inicial incompleta: se crearon " + creadas
                    + " calles de " + calles.size() + ". Revisar que cada calle una dos lugares existentes.");
        }
    }

    private void cargarSuministros() {
        List<Item> items = new ArrayList<>();
        for (DominioCaba.Suministro s : DominioCaba.SUMINISTROS) {
            items.add(new Item(s.id(), s.nombre(), s.kg(), s.utilidad(), s.lugarId()));
        }
        itemRepository.saveAll(items);
    }
}
