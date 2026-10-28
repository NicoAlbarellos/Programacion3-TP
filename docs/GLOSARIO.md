# Glosario para la defensa

Términos que usamos en el código y en la documentación, y dónde aparecen en nuestro TP.

| Término | Qué significa | Dónde aparece en el TP |
|---|---|---|
| Grafo no dirigido | Cada arista se puede recorrer en los dos sentidos. | Cada calle se guarda una sola vez en Neo4j y al cargarla en memoria se agrega en ambos sentidos. |
| Lista de adyacencia | Para cada vértice, la lista de sus vecinos con el costo de la arista. Ocupa O(V + E). | `service/Grafo`, sobre la que trabajan todos los algoritmos de grafos. |
| Relajación | Mejorar la distancia conocida a un vértice cuando pasando por otro se llega más rápido. | Dijkstra: HGA – LBO directo cuesta 51, pero por CON cuesta 15 + 32 = 47. |
| Cola de prioridad | Estructura que entrega siempre el elemento de menor clave, en O(log n) por operación. | `PriorityQueue` en Dijkstra y UCS (`CaminosMinimos`) y en Prim (`ArbolRecubrimiento`). |
| Eliminación perezosa | En vez de bajar la clave de un elemento que ya está en la cola, se lo encola de nuevo y la entrada vieja se descarta cuando sale. | Dijkstra, UCS y Prim, porque `java.util.PriorityQueue` no tiene la operación decreaseKey. |
| UCS (búsqueda de costo uniforme) | Dijkstra que sabe cuándo parar: la misma cola por costo acumulado, pero con corte temprano cuando el destino **sale** de la cola (no cuando se descubre, porque todavía puede haber un camino más barato en la cola). Da el mismo costo que Dijkstra. | `/api/grafo/ucs`: HGA → LBO cuesta 47, igual que Dijkstra. |
| Nodo expandido | Vértice que sale de la cola con su distancia definitiva y del que se miran los vecinos. Las entradas viejas que se descartan no cuentan. | `/api/grafo/ucs` informa `nodosExpandidosUcs` y `nodosExpandidosDijkstra`: RET → PMA expande 4 contra 31; OBE → HIT, 11 contra 31. |
| Lugar infectado (nodo bloqueado) | Vértice que una ruta no puede pisar, ni como paso intermedio. Si el origen o el destino están infectados, es un error. | Parámetro `infectados` de `/dijkstra` y `/rutas` (`LugaresInfectados`): Dijkstra nunca relaja hacia un bloqueado y el backtracking lo marca como visitado desde el principio. HGA → LBO con `infectados=CON` cuesta 51; con `infectados=PMA` no hay camino a la Reserva (RES). |
| Árbol de recubrimiento mínimo | Conjunto de aristas que conecta todos los vértices, sin ciclos y con costo total mínimo. Tiene V − 1 aristas. | Prim y Kruskal: 30 calles a despejar, 835 minutos en total. |
| Union-Find | Estructura de conjuntos disjuntos: `find` devuelve el representante de un conjunto y `union` fusiona dos. Kruskal la usa para saber si una arista cerraría un ciclo. | Clase interna `UnionFind` de `ArbolRecubrimiento`, con compresión de caminos y unión por rango. |
| Matriz `siguiente` | En Floyd-Warshall, `siguiente[i][j]` es el primer vértice al que hay que ir desde i para llegar a j por el camino mínimo. | `/api/grafo/floyd/camino` reconstruye la ruta con esa matriz. |
| Ciclo negativo | Ciclo cuya suma de costos es menor que 0: los caminos mínimos dejan de estar definidos. Floyd-Warshall lo detecta si al final algún `d[i][i]` es negativo. | `CaminosMinimos` lanza `IllegalStateException` y la API responde 422 con un JSON de error. |
| Greedy (voraz) | Elige en cada paso la opción que parece mejor y nunca la reconsidera. No siempre llega al óptimo. | Mochila por ratio utilidad/peso: con 12 kg da 112, y el óptimo es 119. |
| Programación dinámica | Resuelve cada subproblema una sola vez, guarda el resultado en una tabla y arma la solución a partir de ella. | Mochila 0/1: tabla `dp[n+1][W+1]`, O(n · W) en tiempo y en espacio. |
| Pseudopolinómico | Polinómico en el valor numérico de un dato de la entrada (la capacidad W), no en la cantidad de bits que ocupa. | Complejidad de la mochila 0/1 con programación dinámica. |
| Divide y conquista | Partir el problema en subproblemas más chicos, resolverlos recursivamente y combinar los resultados. | QuickSort y MergeSort, en `Ordenamiento`. |
| Backtracking | Búsqueda en profundidad que arma la solución paso a paso: prueba una opción, sigue, y al volver la deshace (retroceso) para probar la siguiente. | `/api/grafo/rutas`: marca y desmarca cada lugar visitado para encontrar todas las rutas simples. |
| Poda por viabilidad | Cortar una rama porque ya no puede dar ninguna solución válida. | Backtracking: si el tiempo acumulado o los tramos ya superan el máximo, no se sigue. RET → CUN con 200 min y 8 tramos: 542 nodos con poda contra 807.257 sin poda, y las mismas 3 rutas. |
| Branch & Bound | Backtracking más una cota: se ramifica, se calcula la cota de cada rama y se descartan las que no pueden mejorar la mejor solución conocida. | `/api/seleccion/repartir` (`RamificacionYPoda`). |
| Cota optimista | Valor que ninguna solución de la rama puede mejorar. Como nunca sobreestima, podar con ella no pierde el óptimo. | B&B: cota = máx(carga máxima actual, peso total / m). Con k = 3: 33 / 3 = 11, y el óptimo es 11. |
| Poda por optimalidad | Cortar una rama que podría dar soluciones válidas, pero ninguna mejor que la mejor conocida. | B&B: si la cota es ≥ la mejor carga máxima encontrada, el hijo no se abre (`nodosPodados`). Con k = 3: 61 nodos explorados y 87 podados. |
| Ruptura de simetría | Evitar explorar soluciones equivalentes. Como los sobrevivientes son idénticos, el mismo reparto con los nombres permutados aparecería k! veces. | B&B: un suministro solo puede ir a un sobreviviente vacío si es el primero vacío. Sin cota, eso solo baja el árbol de k = 3 de 9841 a 1645 nodos. |
| Makespan | La carga del contenedor más cargado; es lo que se quiere minimizar al repartir. | `cargaMaxima` en la respuesta de `/api/seleccion/repartir`. |
