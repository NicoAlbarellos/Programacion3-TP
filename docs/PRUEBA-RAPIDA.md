# Prueba rápida
Prueba de humo para comprobar en un par de minutos que el sistema funciona. Con la aplicación
levantada (ver «Cómo levantar el proyecto» en el [README](../README.md)), abrir cada URL en el
navegador y comparar con el resultado esperado.
| # | URL | Resultado esperado |
|---|---|---|
| 1 | http://localhost:8080/api/grafo/resumen | `"vertices":12,"aristas":21,"dirigido":false` |
| 2 | http://localhost:8080/api/grafo/dijkstra?origen=PAL&destino=CUN | `{"vertices":["PAL","MON","CUN"],"costoTotal":84.0}` |
| 3 | http://localhost:8080/api/grafo/ucs?origen=PAL&destino=CUN | `"costoTotal":84.0,"nodosExpandidosUcs":11,"nodosExpandidosDijkstra":12` |
| 4 | http://localhost:8080/api/grafo/dijkstra?origen=PAL&destino=CUN&infectados=MON | `{"vertices":["PAL","CUN"],"costoTotal":90.0}` |
| 5 | http://localhost:8080/api/grafo/floyd/camino?origen=LBO&destino=CUN | `{"vertices":["LBO","PMA","RET","PAL","MON","CUN"],"costoTotal":183.0}` |
| 6 | http://localhost:8080/api/grafo/kruskal | 11 aristas y `"costoTotal":287.0` |
| 7 | http://localhost:8080/api/seleccion/comparar?capacidad=12 | greedy con `"valorTotal":112.0` y dinámica con `"valorTotal":119.0` |
| 8 | http://localhost:8080/api/grafo/rutas?origen=RET&destino=CUN&costoMaximo=150&saltosMaximos=4 | 4 rutas y `"nodosConPoda":118,"nodosSinPoda":684` |
| 9 | http://localhost:8080/api/seleccion/repartir?contenedores=3 | `"cargaMaxima":11.0,"cotaInicial":11.0,"nodosExplorados":61,"nodosPodados":87` |
| 10 | http://localhost:8080/api/grafo/bfs?origen=XXX | error 400 con `{"estado":"ERROR","detalle":"No existe el nodo con id: XXX"}` |

Qué comprueba cada una:
1. Los datos se cargaron bien en Neo4j: 12 lugares y 21 calles, grafo no dirigido.
2. Dijkstra relaja bien las aristas: el camino directo PAL – CUN cuesta 90, pero pasando por MON
   cuesta 66 + 18 = 84.
3. UCS da el mismo costo que Dijkstra (84) pero corta apenas el destino sale de la cola: expande
   11 lugares contra los 12 de Dijkstra (La Boca, el más lejano de Palermo, queda sin expandir).
4. Con Monumental infectado el desvío por MON no se puede usar, y la ruta más rápida pasa a ser
   la calle directa PAL – CUN, de 90 minutos.
5. Floyd-Warshall reconstruye el camino con su matriz `siguiente`: de La Boca a Ciudad
   Universitaria son 183 minutos a pie.
6. Kruskal arma el árbol de recubrimiento mínimo: 11 aristas (12 lugares − 1) y 287 minutos.
   `/api/grafo/prim?origen=RET` tiene que dar el mismo costo.
7. Con una mochila de 12 kg el greedy por ratio utilidad/peso no llega al óptimo (112), y la
   mochila 0/1 con programación dinámica sí (119).
8. El backtracking encuentra las 4 rutas de escape de Retiro a Ciudad Universitaria en 150 minutos
   y 4 tramos como máximo, y la poda por viabilidad baja la búsqueda de 684 a 118 nodos.
9. Branch & Bound reparte los 33 kg entre 3 sobrevivientes con 11 kg como máximo, que es igual a
   la cota (33 / 3): explora 61 nodos y poda 87.
10. Los errores vuelven como JSON, nunca como stack trace.
Los resultados esperados están calculados a mano en [`VERIFICACIONES.md`](VERIFICACIONES.md), y
los mismos requests están en [`api.http`](api.http) para probarlos desde el IDE.
