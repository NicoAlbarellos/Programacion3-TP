package uade.prog3.tpo.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.ItemRepository;
import uade.prog3.tpo.repository.NodoRepository;

import java.util.ArrayList;
import java.util.HashMap;
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

    private final NodoRepository nodoRepository;
    private final ItemRepository itemRepository;

    public CargaInicial(NodoRepository nodoRepository, ItemRepository itemRepository) {
        this.nodoRepository = nodoRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (nodoRepository.count() > 0) {
            log.info("La base ya tiene datos. No se ejecuta la carga inicial.");
            return;
        }
        cargarLugaresYCalles();
        cargarSuministros();
        log.info("Carga inicial completada: {} lugares de CABA, {} calles, {} suministros.",
                DominioCaba.LUGARES.size(), DominioCaba.CALLES.size(), DominioCaba.SUMINISTROS.size());
        log.info("Para desactivarla: app.seed.enabled=false");
    }

    private void cargarLugaresYCalles() {
        Map<String, Nodo> nodoPorId = new HashMap<>();
        for (DominioCaba.Lugar l : DominioCaba.LUGARES) {
            nodoPorId.put(l.id(), new Nodo(l.id(), l.nombre(), l.tipo(), l.capacidad()));
        }
        // cada calle se guarda una sola vez, en el sentido origen -> destino
        for (DominioCaba.Calle c : DominioCaba.CALLES) {
            nodoPorId.get(c.origen()).conectar(nodoPorId.get(c.destino()), c.minutos());
        }
        nodoRepository.saveAll(nodoPorId.values());
    }

    private void cargarSuministros() {
        List<Item> items = new ArrayList<>();
        for (DominioCaba.Suministro s : DominioCaba.SUMINISTROS) {
            items.add(new Item(s.id(), s.nombre(), s.kg(), s.utilidad(), s.lugarId()));
        }
        itemRepository.saveAll(items);
    }
}
