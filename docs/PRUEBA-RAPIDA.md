# Prueba rápida

Prueba de humo para comprobar en un par de minutos que el sistema funciona. Con la aplicación
levantada (ver «Cómo levantar el proyecto» en el [README](../README.md)), abrir cada URL en el
navegador y comparar con el resultado esperado.

| # | URL | Resultado esperado |
|---|---|---|
| 1 | http://localhost:8080/api/grafo/resumen | `"vertices":31,"aristas":51,"dirigido":false` |
| 2 | http://localhost:8080/api/grafo/dijkstra?origen=HGA&destino=LBO | `{"vertices":["HGA","CON","LBO"],"costoTotal":47.0}` |
| 3 | http://localhost:8080/api/grafo/ucs?origen=RET&destino=PMA | `{"vertices":["RET","PMA"],"costoTotal":36.0,"nodosExpandidosUcs":4,"nodosExpandidosDijkstra":31}` |
| 4 | http://localhost:8080/api/grafo/dijkstra?origen=HGA&destino=LBO&infectados=CON | `{"vertices":["HGA","LBO"],"costoTotal":51.0}` |
| 5 | http://localhost:8080/api/grafo/floyd/camino?origen=HGA&destino=LBO | `{"vertices":["HGA","CON","LBO"],"costoTotal":47.0}` |
| 6 | http://localhost:8080/api/grafo/kruskal | 30 aristas y `"costoTotal":835.0` |
| 7 | http://localhost:8080/api/seleccion/comparar?capacidad=12 | greedy con `"valorTotal":112.0` y dinámica con `"valorTotal":119.0` |
| 8 | http://localhost:8080/api/grafo/rutas?origen=RET&destino=CUN | 3 rutas y `"nodosConPoda":542,"nodosSinPoda":807257` |
| 9 | http://localhost:8080/api/seleccion/repartir?contenedores=3 | `"cargaMaxima":11.0,"cotaInicial":11.0,"nodosExplorados":61,"nodosPodados":87` |
| 10 | http://localhost:8080/api/grafo/bfs?origen=XXX | error 400 con `{"estado":"ERROR","detalle":"No existe el nodo con id: XXX"}` |

Qué comprueba cada una:

1. Los datos se cargaron bien en Neo4j: 31 lugares y 51 calles, grafo no dirigido. Si da 12
   vértices, la base tiene el mapa viejo: borrarla con `docker compose down -v` y volver a
   levantarla (ver el README).
2. Dijkstra relaja bien las aristas: la calle directa HGA – LBO cuesta 51, pero pasando por
   Constitución cuesta 15 + 32 = 47.
3. UCS da el mismo costo que Dijkstra pero corta apenas el destino sale de la cola: de Retiro a
   Puerto Madero expande 4 lugares (RET, OBE, REC, PMA) contra los 31 de Dijkstra.
4. Con Constitución infectada el atajo no se puede usar, y la ruta más rápida pasa a ser la calle
   directa HGA – LBO, de 51 minutos.
5. Floyd-Warshall reconstruye el camino con su matriz `siguiente` y coincide con Dijkstra.
6. Kruskal arma el árbol de recubrimiento mínimo: 30 aristas (31 lugares − 1) y 835 minutos.
   `/api/grafo/prim?origen=RET` tiene que dar las mismas calles y el mismo costo.
7. Con una mochila de 12 kg el greedy por ratio utilidad/peso no llega al óptimo (112), y la
   mochila 0/1 con programación dinámica sí (119).
8. Con los límites por defecto (200 minutos, 8 tramos) el backtracking encuentra 3 rutas de escape
   de Retiro a Ciudad Universitaria, y la poda por viabilidad baja la búsqueda de 807.257 a 542
   nodos.
9. Branch & Bound reparte los 33 kg entre 3 sobrevivientes con 11 kg como máximo, que es igual a
   la cota (33 / 3): explora 61 nodos y poda 87.
10. Los errores vuelven como JSON, nunca como stack trace.

Los resultados esperados están calculados a mano en [`VERIFICACIONES.md`](VERIFICACIONES.md), y
los mismos requests están en [`api.http`](api.http) para probarlos desde el IDE.
