package uade.prog3.tpo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uade.prog3.tpo.repository.NodoRepository;
import uade.prog3.tpo.service.GrafoService;
import uade.prog3.tpo.service.Mapa;

/**
 * Datos para dibujar el mapa en la demo visual (static/index.html).
 * Sin logica algoritmica: carga el grafo y los nodos y arma el DTO.
 */
@RestController
public class MapaController {

    /** El mapa se dibuja no dirigido: cada calle de CABA se camina en los dos sentidos. */
    private static final boolean DIRIGIDO = false;

    private final GrafoService grafoService;
    private final NodoRepository nodoRepository;

    public MapaController(GrafoService grafoService, NodoRepository nodoRepository) {
        this.grafoService = grafoService;
        this.nodoRepository = nodoRepository;
    }

    /** GET /api/mapa : lugares (id, nombre, tipo, valor) y calles (una vez cada una). */
    @GetMapping("/api/mapa")
    public Mapa mapa() {
        return Mapa.desde(grafoService.cargar(DIRIGIDO), nodoRepository.findAll());
    }
}
