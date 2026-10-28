package uade.prog3.tpo.controller;

import org.springframework.web.bind.annotation.*;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento;
import uade.prog3.tpo.algorithm.Backtracking;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.service.Grafo;
import uade.prog3.tpo.service.GrafoService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Endpoints de los algoritmos sobre grafos.
 *
 * Cada endpoint carga el grafo (no dirigido, ver DIRIGIDO) y delega en la
 * clase de algoritmo correspondiente. UCS devuelve ademas los nodos expandidos contra Dijkstra.
 *
 * Los controllers NO contienen logica algoritmica: toda va en el paquete algorithm.
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
        // LinkedHashMap: el JSON sale siempre con las claves en este orden (Map.of no lo garantiza)
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("vertices", g.cantidadVertices());
        resumen.put("aristas", g.cantidadAristas());
        resumen.put("dirigido", g.esDirigido());
        resumen.put("ids", g.ids());
        return resumen;
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

    /** GET /api/grafo/dijkstra?origen=RET&destino=CUN  (opcional: &infectados=MON,REC) */
    @GetMapping("/dijkstra")
    public CaminosMinimos.Camino dijkstra(@RequestParam String origen, @RequestParam String destino,
                                          @RequestParam(required = false) Set<String> infectados) {
        return caminosMinimos.dijkstra(grafoService.cargar(DIRIGIDO), origen, destino, infectados);
    }

    /** GET /api/grafo/floyd */
    @GetMapping("/floyd")
    public Map<String, Double> floyd() {
        return caminosMinimos.floydWarshall(grafoService.cargar(DIRIGIDO));
    }

    /** GET /api/grafo/floyd/camino?origen=HGA&destino=LBO */
    @GetMapping("/floyd/camino")
    public CaminosMinimos.Camino caminoFloyd(@RequestParam String origen, @RequestParam String destino) {
        return caminosMinimos.caminoFloyd(grafoService.cargar(DIRIGIDO), origen, destino);
    }

    /** GET /api/grafo/floyd/comparacion : trabajo de Floyd contra Dijkstra desde cada vertice. */
    @GetMapping("/floyd/comparacion")
    public CaminosMinimos.Comparacion floydComparacion() {
        return caminosMinimos.compararConDijkstra(grafoService.cargar(DIRIGIDO));
    }

    /** GET /api/grafo/ucs?origen=RET&destino=CUN */
    @GetMapping("/ucs")
    public CaminosMinimos.ResultadoUcs ucs(@RequestParam String origen, @RequestParam String destino) {
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

    /**
     * GET /api/grafo/rutas?origen=RET&destino=CUN&costoMaximo=200&saltosMaximos=8  (opcional: &infectados=PLA)
     * Defaults moderados: con limites muy holgados la cantidad de rutas (y el JSON) crece mucho.
     */
    @GetMapping("/rutas")
    public Backtracking.ResultadoRutas rutas(@RequestParam String origen,
                                             @RequestParam String destino,
                                             @RequestParam(defaultValue = "200") double costoMaximo,
                                             @RequestParam(defaultValue = "8") int saltosMaximos,
                                             @RequestParam(required = false) Set<String> infectados) {
        return backtracking.rutasSimples(grafoService.cargar(DIRIGIDO), origen, destino,
                costoMaximo, saltosMaximos, infectados);
    }
}
