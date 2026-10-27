# Complejidades

Resumen de la complejidad de cada algoritmo implementado (lo pide el hito 10). La justificación
completa de cada fila, con el ejemplo real, está en la ficha del endpoint en
[`ENDPOINTS.md`](ENDPOINTS.md).

Notación: **V** = lugares (vértices), **E** = calles (aristas); en nuestro grafo V = 12 y E = 21.
**n** = cantidad de elementos a ordenar o de suministros; **W** = capacidad de la mochila en kg.
**b** = grado máximo de un lugar; **R** = rutas encontradas; **k** = contenedores pedidos y
**m** = min(k, n) contenedores efectivos.

## Método para resolver recurrencias (clase 1)

**Por sustracción:** T(n) = a · T(n − b) + O(nᵏ)

| Caso | Resultado |
|---|---|
| a < 1 | O(nᵏ) |
| a = 1 | O(nᵏ⁺¹) |
| a > 1 | O(a^(n div b)) |

**Por división:** T(n) = a · T(n / b) + O(nᵏ)

| Caso | Resultado |
|---|---|
| a < bᵏ | O(nᵏ) |
| a = bᵏ | O(nᵏ log n) |
| a > bᵏ | O(n^(log_b a)) |

a = cantidad de llamadas recursivas, b = cuánto se reduce el tamaño, k = grado del costo de lo que
se hace fuera de las llamadas (partir y combinar).

## Tabla

| Algoritmo | Endpoint | Recurrencia (y su resolución) | Temporal | Espacial | Estructura que la sostiene |
|---|---|---|---|---|---|
| BFS | `/api/grafo/bfs` | No aplica: iterativo. Se cuenta: cada vértice entra a la cola una vez (V) y cada lista de adyacencia se recorre una vez (2E). | O(V + E) | O(V) | Cola FIFO (`ArrayDeque`, O(1) por operación), `boolean[] visitado`, lista de adyacencia |
| DFS | `/api/grafo/dfs` | No es divide y conquista: una llamada por vértice (V) y cada lista recorrida una vez (2E). | O(V + E) | O(V) (pila de llamadas, profundidad ≤ V) | Pila implícita (recursión), `boolean[] visitado`, lista de adyacencia |
| Dijkstra | `/api/grafo/dijkstra` | No aplica. A lo sumo E + 1 entradas en la cola, cada `add`/`poll` O(log V). | O((V + E) log V) | O(V + E) | Heap binario (`PriorityQueue`) con eliminación perezosa; arreglos `dist[]`, `previo[]` |
| Floyd-Warshall | `/api/grafo/floyd` | Recurrencia de PD (no de tiempo): d_k[i][j] = min(d_(k−1)[i][j], d_(k−1)[i][k] + d_(k−1)[k][j]). V capas × V² celdas, O(1) cada una. | O(V³) | O(V²) | Matriz de adyacencia `double[][]` (acceso O(1)) |
| Floyd con camino | `/api/grafo/floyd/camino` | Igual que Floyd; al relajar, siguiente[i][j] = siguiente[i][k]. Reconstrucción: a lo sumo V saltos. | O(V³) + O(V) = O(V³) | O(V²) | Matriz de distancias + matriz `int[][] siguiente` |
| Floyd vs. Dijkstra V veces | `/api/grafo/floyd/comparacion` | No aplica: corre los dos. | O(V³ + V (V + E) log V) | O(V² + E) | Las de Floyd y Dijkstra, más contadores |
| Prim | `/api/grafo/prim` | No aplica. A lo sumo E + 1 entradas en la cola, O(log V) cada una; grafo conexo (E ≥ V − 1). | O(E log V) | O(V + E) | Heap binario (`PriorityQueue`) con eliminación perezosa; `key[]`, `parent[]`, `inMST[]` |
| Kruskal | `/api/grafo/kruskal` | La domina el MergeSort de las E calles: T(E) = 2T(E/2) + O(E) → O(E log E). | O(E log E) = O(E log V) | O(V + E) | MergeSort propio + Union-Find |
| Union-Find (dentro de Kruskal) | — | No aplica. Unión por rango: altura ≤ log V; con compresión de caminos, amortizado α(V) (inversa de Ackermann, ≤ 4 en la práctica). | O(α(V)) amortizado por `find`/`union`; O(E α(V)) en Kruskal | O(V) | Arreglos `padre[]` y `rango[]` |
| QuickSort | `/api/seleccion/quicksort` | Mejor/promedio: T(n) = 2T(n/2) + O(n); división con a = 2, b = 2, k = 1, a = bᵏ → O(n log n). Peor: T(n) = T(n − 1) + O(n); sustracción con a = 1, k = 1 → O(n²). | O(n log n) promedio, O(n²) peor | O(n) (copia) + pila O(log n) promedio, O(n) peor | `ArrayList` (acceso e intercambio O(1) por índice); partición de Lomuto in-place, pivote del medio |
| MergeSort | `/api/seleccion/mergesort` | T(n) = 2T(n/2) + O(n); división con a = 2, b = 2, k = 1, a = bᵏ → O(n log n) en todos los casos. | O(n log n) | O(n) + pila O(log n) | `ArrayList` para mitades y mezcla (`add` O(1) amortizado); estable |
| Greedy (mochila 0/1 por ratio) | `/api/seleccion/greedy` | No es recursivo. Lo domina el MergeSort: T(n) = 2T(n/2) + O(n) → O(n log n); después un recorrido O(n). | O(n log n) | O(n) | MergeSort propio (estable) + `ArrayList` |
| Mochila 0/1 con PD | `/api/seleccion/dinamica` | dp[i][j] = max(dp[i−1][j], dp[i−1][j − pᵢ] + vᵢ) (si pᵢ ≤ j). Sin tabla, la recursión es T(n) = 2T(n − 1) + O(1): sustracción con a = 2 > 1 → O(2ⁿ). Con tabla, cada uno de los (n + 1)(W + 1) subproblemas se calcula una vez en O(1). | O(n · W), **pseudopolinómica**; W = min(capacidad, Σ pesos) | O(n · W) | Matriz `double[n+1][W+1]` (tabla dp) + `boolean[] tomado` para la recuperación |
| Greedy vs. PD | `/api/seleccion/comparar` | No aplica: corre los dos. | O(n log n + n · W) | O(n · W) | Las de cada técnica |
| Backtracking (rutas simples) | `/api/grafo/rutas` | T(L) = b · T(L − 1) + O(b), con b = grado máximo y L = min(saltosMaximos, V − 1); sustracción con a = b > 1 → O(b^L). Peor caso (grafo completo, sin límites efectivos): ≤ e · (V − 1)! caminos simples → O(V!). | Con poda O(b^L) + O(R · V) por copiar las R rutas; la corrida sin poda (que el endpoint también hace para contar) recorre todos los caminos simples: O(b^(V − 1)), peor O(V!), y domina el endpoint | O(V) + salida O(R · V) (pila ≤ V + 1, camino ≤ V) | `boolean[] visitado` (marcar/desmarcar O(1)), `ArrayList` como pila del camino, recursión, lista de adyacencia |
| Branch & Bound (reparto en k contenedores) | `/api/seleccion/repartir` | T(n) = m · T(n − 1) + O(m), con m = min(k, n); sustracción con a = m > 1 → O(m^n). Árbol completo: (m^(n+1) − 1) / (m − 1) nodos (k = 1: n + 1). | O(m^n) en el peor caso, + O(n) por cada mejora; la cota y la ruptura de simetría lo reducen mucho (k = 3: 61 de 9841 nodos) | O(n) (pila n + 1, `cargas` m ≤ n) + salida O(n + m) | Arreglo `double[] cargas` (sumar/restar O(1)), `int[] asignacion` y `mejorAsignacion`, recursión DFS, `LinkedHashMap` de salida |

## Notas para el oral

- **Carga del grafo:** todos los endpoints de `/api/grafo` cargan el grafo una vez por request en
  O(V + E) antes de correr el algoritmo; no cambia el orden de BFS/DFS y es menor que el del resto.
- **Por qué la estructura importa:** Dijkstra y Prim con búsqueda lineal del mínimo serían O(V²);
  Kruskal sin Union-Find tendría que hacer un BFS/DFS por calle para detectar ciclos,
  O(E (V + E)); BFS/DFS con matriz de adyacencia serían O(V²).
- **Pseudopolinómica:** O(n · W) es polinómica en el **valor** de W, pero W ocupa log W bits en la
  entrada; en función del tamaño de la entrada crece exponencialmente.
- **Floyd vs. Dijkstra V veces:** O(V³) contra O(V (V + E) log V). En grafos dispersos (E ≈ V,
  como un mapa de calles) gana Dijkstra, O(V² log V); en densos (E ≈ V²) gana Floyd. Floyd además
  acepta aristas negativas y detecta ciclos negativos.
- **Backtracking y Branch & Bound son exponenciales en el peor caso.** La poda no cambia el orden
  del peor caso, cambia cuánto del árbol se recorre en la práctica: en `/rutas` (RET → CUN, 150 min,
  4 tramos) 118 nodos contra 684, y en `/repartir` (k = 3) 61 contra 9841. Backtracking poda por
  **viabilidad** (la rama ya no puede dar una ruta válida); Branch & Bound poda por **optimalidad**
  (la cota optimista dice que la rama no puede mejorar la mejor solución conocida).
