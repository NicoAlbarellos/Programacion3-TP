package uade.prog3.tpo.controller;

import org.springframework.web.bind.annotation.*;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento;
import uade.prog3.tpo.algorithm.Backtracking;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.service.Grafo;
import uade.prog3.tpo.service.GrafoService;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de los algoritmos sobre grafos.
 *
 * El cableado ya esta hecho: cada endpoint carga el grafo y delega en la
 * clase de algoritmo correspondiente. Mientras el algoritmo no este
 * implementado, la respuesta es 501 con un mensaje claro.
 *
 * Lo unico que hay que tocar para que un endpoint funcione es el metodo
 * del paquete algorithm. Los controllers NO deben contener logica algoritmica.
 */
@RestController
@RequestMapping("/api/grafo")
public class GrafoController {

    /**
     * El dominio (calles de CABA a pie) es NO dirigido: cada calle se guarda
     * una vez y se recorre en ambos sentidos.
     */
    private static final boolean DIRIGIDO = false;

    private final GrafoService grafoService;
    private final Recorridos recorridos;
    private final CaminosMinimos caminosMinimos;
    private final ArbolRecubrimiento arbolRecubrimiento;
    private final Backtracking backtracking;

    public GrafoController(GrafoService grafoService,
                           Recorridos recorridos,
                           CaminosMinimos caminosMinimos,
                           ArbolRecubrimiento arbolRecubrimiento,
                           Backtracking backtracking) {
        this.grafoService = grafoService;
        this.recorridos = recorridos;
        this.caminosMinimos = caminosMinimos;
        this.arbolRecubrimiento = arbolRecubrimiento;
        this.backtracking = backtracking;
    }

    /** Metadatos del grafo cargado. Sirve para verificar la conexion a la base. */
    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        Grafo g = grafoService.cargar(DIRIGIDO);
        return Map.of(
                "vertices", g.cantidadVertices(),
                "aristas", g.cantidadAristas(),
                "dirigido", g.esDirigido(),
                "ids", g.ids());
    }

    /** GET /api/grafo/dfs?origen=RET */
    @GetMapping("/dfs")
    public List<String> dfs(@RequestParam String origen) {
        return recorridos.dfs(grafoService.cargar(DIRIGIDO), origen);
    }

    /** GET /api/grafo/bfs?origen=RET */
    @GetMapping("/bfs")
    public List<String> bfs(@RequestParam String origen) {
        return recorridos.bfs(grafoService.cargar(DIRIGIDO), origen);
    }

    /** GET /api/grafo/dijkstra?origen=RET&destino=CUN */
    @GetMapping("/dijkstra")
    public CaminosMinimos.Camino dijkstra(@RequestParam String origen, @RequestParam String destino) {
        return caminosMinimos.dijkstra(grafoService.cargar(DIRIGIDO), origen, destino);
    }

    /** GET /api/grafo/floyd */
    @GetMapping("/floyd")
    public Map<String, Double> floyd() {
        return caminosMinimos.floydWarshall(grafoService.cargar(DIRIGIDO));
    }

    /** GET /api/grafo/ucs?origen=RET&destino=CUN */
    @GetMapping("/ucs")
    public CaminosMinimos.Camino ucs(@RequestParam String origen, @RequestParam String destino) {
        return caminosMinimos.ucs(grafoService.cargar(DIRIGIDO), origen, destino);
    }

    /**
     * GET /api/grafo/prim?origen=RET
     * El arbol de recubrimiento minimo exige SIEMPRE grafo no dirigido:
     * se carga con false sin importar el valor de DIRIGIDO.
     */
    @GetMapping("/prim")
    public ArbolRecubrimiento.Mst prim(@RequestParam String origen) {
        return arbolRecubrimiento.prim(grafoService.cargar(false), origen);
    }

    /**
     * GET /api/grafo/kruskal
     * Igual que Prim: el MST exige SIEMPRE grafo no dirigido, sin importar DIRIGIDO.
     */
    @GetMapping("/kruskal")
    public ArbolRecubrimiento.Mst kruskal() {
        return arbolRecubrimiento.kruskal(grafoService.cargar(false));
    }

    /** GET /api/grafo/rutas?origen=RET&destino=CUN&costoMaximo=150&saltosMaximos=4 */
    @GetMapping("/rutas")
    public List<Backtracking.Ruta> rutas(@RequestParam String origen,
                                         @RequestParam String destino,
                                         @RequestParam(defaultValue = "1000") double costoMaximo,
                                         @RequestParam(defaultValue = "10") int saltosMaximos) {
        return backtracking.rutasSimples(grafoService.cargar(DIRIGIDO), origen, destino,
                costoMaximo, saltosMaximos);
    }
}
