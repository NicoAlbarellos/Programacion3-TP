# Documentación de endpoints

Una ficha por endpoint: qué responde en el dominio (apocalipsis zombie en CABA), parámetros,
ejemplo **real** de request y respuesta, complejidad justificada y estructura de datos usada.

Notación: **V** = cantidad de lugares (vértices), **E** = cantidad de calles (aristas). En nuestro
grafo V = 12 y E = 21. El costo de una arista son **minutos a pie**.

Los ejemplos se obtuvieron corriendo la aplicación contra Neo4j con la carga inicial
(`seed/CargaInicial.java`), salvo donde la ficha indica otra cosa. Los cálculos a mano que
verifican Dijkstra, Prim, Kruskal, greedy vs. PD y Floyd están en
[`VERIFICACIONES.md`](VERIFICACIONES.md). La tabla resumen de complejidades está en
[`COMPLEJIDADES.md`](COMPLEJIDADES.md).

## Índice

- [GET /api/grafo/resumen](#get-apigraforesumen)
- [GET /api/grafo/bfs](#get-apigrafobfs) — BFS
- [GET /api/grafo/dfs](#get-apigrafodfs) — DFS
- [GET /api/grafo/dijkstra](#get-apigrafodijkstra) — Dijkstra
- [GET /api/grafo/floyd](#get-apigrafofloyd) — Floyd-Warshall
- [GET /api/grafo/floyd/camino](#get-apigrafofloydcamino) — Floyd con reconstrucción del camino
- [GET /api/grafo/floyd/comparacion](#get-apigrafofloydcomparacion) — Floyd vs. Dijkstra V veces
- [GET /api/grafo/prim](#get-apigrafoprim) — Prim
- [GET /api/grafo/kruskal](#get-apigrafokruskal) — Kruskal (Union-Find)
- [GET /api/seleccion/quicksort](#get-apiseleccionquicksort) — QuickSort
- [GET /api/seleccion/mergesort](#get-apiseleccionmergesort) — MergeSort
- [GET /api/seleccion/greedy](#get-apiselecciongreedy) — Greedy (mochila 0/1 por ratio)
- [GET /api/seleccion/dinamica](#get-apiselecciondinamica) — Mochila 0/1 con programación dinámica
- [GET /api/seleccion/comparar](#get-apiseleccioncomparar) — Greedy vs. PD sobre la misma mochila
- [Errores](#errores)
- [Pendientes](#pendientes)

---

## Carga del grafo (común a todos los endpoints de `/api/grafo`)

El controller no tiene lógica algorítmica: llama a `GrafoService.cargar(...)` y le pasa el `Grafo`
resultante a la clase del paquete `algorithm`. La base se consulta **dos veces por request, antes
del algoritmo** (vértices y aristas), nunca dentro de su bucle.

- **Representación:** `Grafo` es una **lista de adyacencia** (`List<List<Arista>>`, índices 0..V−1)
  más un `HashMap<String, Integer>` para traducir id → índice en O(1) promedio.
- **No dirigido:** cada calle está guardada una sola vez en Neo4j; al cargarla se agrega en las dos
  listas de adyacencia (u → v y v → u). Por eso hay 2E entradas en total.
- **Orden determinista:** las consultas Cypher piden los vértices `ORDER BY n.id` y las aristas
  `ORDER BY a.id, b.id` (con el sentido en que se guardaron). El orden de cada lista de adyacencia
  es el orden en que se agregaron las aristas, así que es siempre el mismo. Con la carga actual
  quedan así:

  | Lugar | Vecinos (en orden) | Lugar | Vecinos (en orden) |
  |---|---|---|---|
  | ABA | HIT PCE REC CON OBE | OBE | ABA CON PMA REC RET |
  | CON | ABA LBO OBE PMA | PAL | HIT CUN MON REC RET |
  | CUN | MON PAL | PCE | ABA HIT |
  | HIT | ABA PAL PCE | PMA | OBE CON LBO RET |
  | LBO | CON PMA | REC | ABA OBE PAL RET |
  | MON | CUN PAL | RET | OBE PAL PMA REC |

  Ojo: no es orden alfabético de vecinos. Por ejemplo, en ABA aparecen primero HIT, PCE y REC
  (aristas guardadas como ABA → …) y después CON y OBE (guardadas como CON → ABA y OBE → ABA, que
  vienen más tarde en el `ORDER BY`).
- **Costo de la carga:** O(V + E) en tiempo y espacio (un recorrido por cada fila de las consultas,
  inserción O(1) amortizada en `ArrayList`). Es del mismo orden que BFS y DFS, así que no cambia la
  complejidad de esos endpoints.

---

## GET /api/grafo/resumen

Metadatos del grafo cargado. No es un algoritmo del TP: sirve para verificar que la conexión a
Neo4j funciona y que la carga inicial está completa.

**Parámetros:** ninguno. El grafo se carga con la constante `DIRIGIDO = false` del controller
(el dominio es no dirigido).

**Ejemplo**

```
GET /api/grafo/resumen
```

```json
{"vertices":12,"aristas":21,"dirigido":false,"ids":["ABA","CON","CUN","HIT","LBO","MON","OBE","PAL","PCE","PMA","REC","RET"]}
```

12 lugares y 21 calles; los ids salen ordenados porque la carga hace `ORDER BY n.id`.

**Complejidad:** O(V + E) en tiempo y espacio, dominada por la carga del grafo. `cantidadAristas()`
suma los tamaños de las V listas (O(V)) y divide por 2 en el no dirigido, porque cada calle aparece
en las dos listas.

**Estructura usada:** la lista de adyacencia de `Grafo`.

---

## GET /api/grafo/bfs

**Recorrido en anchura (BFS)** desde un lugar. En el dominio: qué lugares son alcanzables a pie
desde un punto, descubiertos **por niveles** (primero los que están a 1 tramo, después a 2, etc.).

Implementación: `Recorridos.bfs` (`algorithm/Recorridos.java`).

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `origen` | String | sí | Id del lugar de partida (ej. `RET`). |

**Respuesta:** lista de ids en el orden en que se visitan.

**Ejemplo**

```
GET /api/grafo/bfs?origen=RET
```

```json
["RET","OBE","PAL","PMA","REC","ABA","CON","HIT","CUN","MON","LBO","PCE"]
```

Lectura por niveles:

| Nivel (tramos desde RET) | Lugares | Descubiertos desde |
|---|---|---|
| 0 | RET | — |
| 1 | OBE, PAL, PMA, REC | RET |
| 2 | ABA, CON | OBE |
| 2 | HIT, CUN, MON | PAL |
| 2 | LBO | PMA |
| 3 | PCE | ABA |

REC no agrega a nadie: todos sus vecinos (ABA, OBE, PAL, RET) ya estaban marcados.

BFS da el camino mínimo en **cantidad de tramos**, no en minutos: CUN aparece en el nivel 2
(RET → PAL → CUN), aunque esa calle de 90 minutos no sea la ruta más rápida. Para minutos hace falta
Dijkstra.

**Complejidad temporal: O(V + E).**
- Cada vértice se marca como visitado **al encolarlo**, así que entra a la cola una sola vez:
  V operaciones de `add`/`poll`, cada una O(1).
- Cada vértice se desencola una vez y se recorre su lista de vecinos completa. La suma de los
  largos de todas las listas es 2E (no dirigido), así que en total se miran 2E aristas, cada una con
  un chequeo O(1) en el arreglo `visitado`.
- Total: O(V + 2E) = O(V + E).

**Complejidad espacial: O(V)** adicional al grafo: arreglo `visitado` (V), cola (a lo sumo V
elementos) y lista de resultado (V).

**Estructura usada:**
- **Cola FIFO** (`ArrayDeque`): garantiza que se procesa todo un nivel antes del siguiente, con
  `add` y `poll` en O(1).
- **Arreglo `boolean[] visitado`**: consulta y marca en O(1) por índice.
- **Lista de adyacencia**: recorrer los vecinos de u cuesta O(grado(u)); con matriz de adyacencia
  costaría O(V) por vértice y el total pasaría a O(V²).

---

## GET /api/grafo/dfs

**Recorrido en profundidad (DFS)** desde un lugar. En el dominio: qué lugares son alcanzables a pie
desde un punto, avanzando siempre lo más lejos posible por un camino y **retrocediendo** cuando no
quedan vecinos sin visitar.

Implementación: `Recorridos.dfs` (`algorithm/Recorridos.java`), recursiva.

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `origen` | String | sí | Id del lugar de partida (ej. `RET`). |

**Respuesta:** lista de ids en el orden en que se visitan (preorden).

**Ejemplo**

```
GET /api/grafo/dfs?origen=RET
```

```json
["RET","OBE","ABA","HIT","PAL","CUN","MON","REC","PCE","CON","LBO","PMA"]
```

Traza (siguiendo la tabla de vecinos de arriba):

1. RET → OBE (primer vecino de RET) → ABA (primer vecino de OBE) → HIT (primer vecino de ABA)
   → PAL (ABA ya visitado) → CUN (HIT ya visitado) → MON.
2. MON no tiene vecinos nuevos; retrocede a CUN (nada nuevo) y a PAL → REC.
3. REC no tiene vecinos nuevos; retrocede a PAL (MON y RET ya visitados) y a HIT → PCE.
4. Retrocede a ABA (PCE y REC ya visitados) → CON → LBO → PMA.
5. Todo visitado: la recursión vuelve hasta RET y termina.

**Complejidad temporal: O(V + E).**
- Cada vértice se marca como visitado al entrar a `dfsRecursivo` y solo se llama sobre vértices no
  visitados, así que hay exactamente una llamada por vértice alcanzable: V llamadas.
- Cada llamada recorre la lista de vecinos de su vértice una vez. En total se miran 2E aristas
  (cada calle aparece en dos listas), cada una con un chequeo O(1).
- Total: O(V + E). Para un DFS recursivo no se plantea recurrencia tipo divide y conquista: el costo
  se cuenta por vértices y aristas visitados.

**Complejidad espacial: O(V)** adicional al grafo: arreglo `visitado` (V), lista de resultado (V) y
la **pila de llamadas**, que en el peor caso (un camino que pasa por todos los vértices) tiene
profundidad V. En el ejemplo la profundidad máxima es 7 (RET-OBE-ABA-HIT-PAL-CUN-MON).

**Estructura usada:**
- **Pila implícita** (la pila de llamadas de la recursión): el último vértice descubierto es el
  primero que se sigue explorando, y al volver de la llamada se retrocede al vértice anterior.
- **Arreglo `boolean[] visitado`**: O(1) por consulta.
- **Lista de adyacencia**: por el mismo motivo que en BFS, sostiene el O(V + E).

---

### Por qué el orden de BFS y DFS es determinista

Los dos recorridos visitan los vecinos **en el orden de la lista de adyacencia**. Como
`GrafoService` carga vértices y aristas con `ORDER BY`, esas listas quedan siempre iguales (ver la
tabla de la sección de carga) y los endpoints devuelven siempre el mismo resultado.

Si los vecinos estuvieran en otro orden (por ejemplo, si se cargara con `findAll()`, que no garantiza
orden, o si se agregara una calle nueva), el recorrido podría ser **otro, igualmente válido**: BFS
seguiría respetando los niveles y DFS seguiría siendo un recorrido en profundidad. Lo que no cambia
es el **conjunto** de lugares alcanzados ni, en BFS, el nivel de cada uno.

Los tests (`RecorridosTest`) arman el grafo en memoria con `GrafosDePrueba.dominioCaba()`, que
agrega las aristas en el mismo orden que la carga real, y comparan contra estos recorridos
calculados a mano.

---

## GET /api/grafo/dijkstra

**Camino mínimo con Dijkstra.** En el dominio: la **ruta más rápida a pie** (en minutos) entre dos
lugares, y por qué lugares pasa.

Implementación: `CaminosMinimos.dijkstra` (`algorithm/CaminosMinimos.java`).

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `origen` | String | sí | Id del lugar de partida (ej. `PAL`). |
| `destino` | String | sí | Id del lugar de llegada (ej. `CUN`). |

**Respuesta:** `vertices` = lugares del camino en orden (incluye origen y destino);
`costoTotal` = minutos a pie del camino.

**Ejemplos**

```
GET /api/grafo/dijkstra?origen=PAL&destino=CUN
```

```json
{"vertices":["PAL","MON","CUN"],"costoTotal":84.0}
```

Existe la calle directa PAL – CUN de 90 minutos, pero PAL → MON → CUN cuesta 66 + 18 = **84**. Al
principio dist[CUN] = 90 (por la calle directa); cuando se extrae MON (dist 66) la **relajación** de
MON – CUN encuentra 66 + 18 = 84 < 90 y actualiza dist[CUN] y previo[CUN] = MON. Si el endpoint
devolviera 90, la relajación estaría mal. La traza completa, paso a paso, está en
[`VERIFICACIONES.md`](VERIFICACIONES.md#1-dijkstra-desde-pal).

```
GET /api/grafo/dijkstra?origen=RET&destino=CUN
```

```json
{"vertices":["RET","PAL","MON","CUN"],"costoTotal":129.0}
```

45 + 66 + 18 = 129. Por la calle directa PAL – CUN serían 45 + 90 = 135.

**Errores**

```
GET /api/grafo/dijkstra?origen=PAL&destino=XXX
```

```
400 Bad Request
{"estado":"ERROR","detalle":"No existe el nodo con id: XXX"}
```

| Situación | Código | `detalle` |
|---|---|---|
| `origen` o `destino` inexistente | 400 | `No existe el nodo con id: XXX` |
| Alguna calle con costo negativo | 400 | `Dijkstra no admite costos negativos: ...` |
| El destino no es alcanzable desde el origen | 422 | `No hay camino de X a Y` |

El caso inalcanzable no se puede provocar con los datos actuales (el grafo es conexo): está
cubierto por `CaminosMinimosTest.destinoInalcanzableFalla`, que agrega un lugar aislado `ZZZ` y
verifica el mensaje `No hay camino de RET a ZZZ`.

**Por qué es greedy.** En cada paso se extrae de la cola el lugar **no procesado con menor
dist[]** y esa elección **no se reconsidera**: su distancia queda definitiva. Es correcto porque
los costos son no negativos: cualquier otro camino hacia ese lugar tendría que salir por algún
lugar todavía en la cola, que ya está a una distancia mayor o igual, y sumarle aristas ≥ 0 no
puede achicarla.

**Por qué los costos negativos lo rompen (y se validan).** Con una arista negativa, un camino que
pasa por un vértice "más lejano" puede terminar siendo más corto que una distancia que el greedy ya
dio por definitiva. Ejemplo dirigido: A → B = 1, A → C = 2, C → B = −2. Dijkstra extrae B con
dist 1 y la congela, pero A → C → B cuesta 0. En un grafo no dirigido es peor: una calle negativa
u – v ya forma el ciclo negativo u → v → u y el camino mínimo deja de estar definido. Por eso,
antes de empezar, `dijkstra` recorre todas las aristas en O(V + E) y, si alguna es negativa, falla
con 400 en lugar de devolver un resultado incorrecto. (Para costos negativos está Floyd-Warshall,
que además detecta ciclos negativos.) En el dominio no pasa: los costos son minutos a pie.

**Detalle de comportamiento:** el algoritmo calcula el árbol de caminos mínimos completo desde el
origen y **no corta** cuando extrae el destino. Cortar al expandir el destino es justamente la
diferencia de UCS.

**Complejidad temporal: O((V + E) log V).**
- **Eliminación perezosa en lugar de decreaseKey.** `java.util.PriorityQueue` no tiene
  `decreaseKey` (el `Q.decreaseKey` del pseudocódigo de la clase). Cuando una relajación mejora
  dist[v], se **inserta otra entrada** (v, nueva distancia) en la cola. Al extraer una entrada cuya
  distancia es mayor que dist[u] actual, es una entrada vieja y se descarta en O(1) (en el ejemplo
  PAL → CUN se descarta la entrada (CUN, 90) después de procesar CUN con 84).
- **Tamaño de la cola.** Cada lugar se procesa una sola vez (las extracciones repetidas se
  descartan), así que cada lista de adyacencia se recorre una vez: 2E aristas miradas en total.
  Una entrada nueva solo se crea con una relajación exitosa, y una calle u – v solo puede relajar
  desde el extremo que se procesa primero (cuando se procesa el segundo, el primero ya tiene su
  distancia definitiva). Entonces hay a lo sumo E + 1 entradas en la cola: O(E).
- **Costo de cada operación.** Cada `add`/`poll` en el heap binario cuesta O(log E), y como
  E ≤ V², log E ≤ 2 log V: O(log V).
- **Total.** V extracciones válidas + O(E) inserciones y extracciones, cada una O(log V):
  O((V + E) log V). Validar costos es O(V + E) y reconstruir el camino O(V), no cambian el orden.
  En el ejemplo PAL → CUN hubo 13 inserciones y 13 extracciones (12 lugares procesados + 1 entrada
  vieja descartada).

**Complejidad espacial: O(V + E)**: arreglos `dist[]` y `previo[]` (V), cola de prioridad (hasta
E + 1 entradas) y el camino reconstruido (≤ V).

**Estructura usada:**
- **Cola de prioridad (heap binario, `PriorityQueue`)** ordenada por distancia: da el mínimo en
  O(log V). Con un arreglo y búsqueda lineal del mínimo cada extracción costaría O(V) y el total
  sería O(V²).
- **Arreglos `dist[]` y `previo[]`** indexados por vértice: lectura y actualización en O(1).
  `previo[]` guarda el árbol de caminos mínimos; el camino se reconstruye recorriéndolo desde el
  destino hacia atrás (insertando al principio de un `ArrayDeque`, O(1) por paso).
- **Lista de adyacencia**: relajar los vecinos de u cuesta O(grado(u)).

---

## GET /api/grafo/floyd

**Caminos mínimos entre todos los pares con Floyd-Warshall** (programación dinámica sobre grafos).
En el dominio: la **tabla de tiempos mínimos a pie entre todos los lugares**, la que un grupo de
sobrevivientes pegaría en la pared del refugio para no recalcular cada ruta.

Implementación: `CaminosMinimos.floydWarshall` (`algorithm/CaminosMinimos.java`).

**Parámetros:** ninguno. El grafo se carga con `DIRIGIDO = false`.

**Respuesta:** un objeto `"origen->destino": minutos` con un par por clave, en orden de índices
(origen por id y, dentro de cada origen, destino por id). Los pares **inalcanzables se omiten**: si
una clave no está, no hay camino. La diagonal (`X->X`) vale 0.

**Ejemplo** (extracto: la respuesta tiene 144 claves = 12 × 12, porque el grafo es conexo)

```
GET /api/grafo/floyd
```

```json
{"ABA->ABA":0.0,"ABA->CON":36.0,"ABA->CUN":124.0, ... ,"PAL->CUN":84.0, ... ,"LBO->CUN":183.0, ... ,"RET->CUN":129.0, ...}
```

- `PAL->CUN` = 84: igual que Dijkstra, pasa por MON (66 + 18) y no usa la calle directa de 90.
- `RET->CUN` = 129 (RET – PAL – MON – CUN) y `LBO->CUN` = 183 (LBO – PMA – RET – PAL – MON – CUN),
  el par más lejano del grafo.
- La suma de los 144 valores es 9206 (lo verifica `FloydWarshallTest.sumaDeTodasLasDistancias`).
  La matriz completa está en [`VERIFICACIONES.md`](VERIFICACIONES.md#5-floyd-warshall).

**Errores**

| Situación | Código | `detalle` |
|---|---|---|
| Hay un ciclo de costo negativo | 422 | `Hay un ciclo de costo negativo` |

Con los datos del dominio no ocurre (todos los costos son minutos a pie, positivos); está cubierto
por `FloydWarshallTest.cicloNegativoDirigidoFalla` y `aristaNegativaNoDirigidaEsCicloNegativo`.

**Cómo funciona.** Se parte de la **matriz de adyacencia**: d[i][i] = 0, d[i][j] = costo de la calle
i – j si existe, ∞ si no. Después, para cada vértice k (el "intermedio permitido"), se recorre toda
la matriz y se **relaja por k**:

```
d[i][j] = min(d[i][j], d[i][k] + d[k][j])
```

**Subproblema de PD.** d_k[i][j] = costo mínimo de i a j usando como intermedios solo los vértices
0..k. Hay dos opciones: el mejor camino no pasa por k (vale d_(k−1)[i][j]) o pasa por k una vez
(d_(k−1)[i][k] + d_(k−1)[k][j]). Al terminar con k = V − 1 están permitidos todos los vértices y
d[i][j] es el camino mínimo.

**¿Por qué k va en el bucle de afuera?** Porque es la dimensión de la PD: para calcular la "capa" k
hace falta tener **completa** la capa k − 1 para todos los pares. Si k fuera el bucle de adentro,
d[i][j] se terminaría de calcular (al cerrar su iteración de i, j) mirando intermedios k cuyas
distancias d[i][k] y d[k][j] todavía no eran definitivas, y nunca se volvería a corregir: el
resultado podría no ser mínimo.

**Una sola matriz alcanza.** En la iteración k la fila y la columna de k no cambian:
d[i][k] = min(d[i][k], d[i][k] + d[k][k]) y d[k][k] = 0 (sin ciclos negativos). Entonces sobreescribir
en el lugar no "contamina" los valores que se usan en esa misma capa.

**Aristas negativas y detección de ciclos negativos.**
- A diferencia de Dijkstra (que no reconsidera un vértice ya extraído y por eso exige costos ≥ 0),
  Floyd **acepta aristas negativas** mientras no haya un ciclo negativo: no toma decisiones
  definitivas, revisa todos los pares en cada capa. Ejemplo dirigido del test
  `aristaNegativaSinCicloSeAcepta`: A → B = 1, B → C = −2, A → C = 3 da `A->C` = −1.
- **Detección:** al terminar se mira la **diagonal**. Si algún d[i][i] < 0, existe un camino de i a i
  que cuesta menos que quedarse quieto, o sea un ciclo negativo, y se responde 422. Con un ciclo
  negativo el camino mínimo no está definido (se puede dar vueltas y bajar el costo sin límite).
- En un grafo **no dirigido** una sola calle negativa u – v ya es un ciclo negativo (u → v → u).
- `∞` es `Double.POSITIVE_INFINITY`: ∞ + (negativo) sigue siendo ∞, así que un par sin camino nunca
  "mejora" por error.

**Complejidad temporal: O(V³).**
- Tres bucles anidados de V iteraciones cada uno, con trabajo O(1) adentro (una suma y una
  comparación sobre la matriz): exactamente V³ relajaciones, **sin importar cuántas aristas haya**.
  En nuestro grafo, 12³ = 1728 (lo cuenta `/floyd/comparacion`).
- Armar la matriz cuesta O(V² + E), verificar la diagonal O(V) y armar la respuesta O(V²): no
  cambian el orden.

**Complejidad espacial: O(V²)**: la matriz de distancias (V × V) y el mapa de respuesta (hasta V²
pares).

**Estructura usada:**
- **Matriz de adyacencia** (`double[][]`, la devuelve `Grafo.matrizDeAdyacencia()` como copia
  nueva): leer y escribir d[i][k], d[k][j] y d[i][j] es O(1) por índice, que es lo que hace que cada
  paso del triple bucle sea O(1). Con lista de adyacencia, averiguar el costo i – j costaría
  O(grado(i)). Si hubiera calles paralelas, la matriz conserva la más barata.
- **`LinkedHashMap`** para la respuesta: mantiene el orden de inserción (por índices), así la salida
  es siempre la misma.
- Además se arma la matriz **`siguiente[][]`** (ver `/floyd/camino`); `/floyd` no la usa, pero el
  triple bucle es el mismo para los tres endpoints de Floyd.

---

## GET /api/grafo/floyd/camino

**Ruta más rápida entre dos lugares calculada con Floyd-Warshall**, reconstruyendo por qué lugares
pasa. En el dominio responde lo mismo que `/dijkstra`, pero con un algoritmo que también funcionaría
con costos negativos (sin ciclos negativos).

Implementación: `CaminosMinimos.caminoFloyd` (`algorithm/CaminosMinimos.java`).

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `origen` | String | sí | Id del lugar de partida (ej. `PAL`). |
| `destino` | String | sí | Id del lugar de llegada (ej. `CUN`). |

**Respuesta:** el mismo formato que `/dijkstra`: `vertices` (camino en orden) y `costoTotal`
(minutos).

**Ejemplos**

```
GET /api/grafo/floyd/camino?origen=PAL&destino=CUN
```

```json
{"vertices":["PAL","MON","CUN"],"costoTotal":84.0}
```

```
GET /api/grafo/floyd/camino?origen=LBO&destino=CUN
```

```json
{"vertices":["LBO","PMA","RET","PAL","MON","CUN"],"costoTotal":183.0}
```

36 + 18 + 45 + 66 + 18 = 183, el par más lejano del grafo. Los dos coinciden con `/dijkstra`; el
test `caminoFloydIgualADijkstraEnTodosLosPares` lo comprueba para los 144 pares (costo y vértices:
en el dominio no hay dos caminos mínimos empatados).

**Errores**

| Situación | Código | `detalle` |
|---|---|---|
| `origen` o `destino` inexistente | 400 | `No existe el nodo con id: XXX` |
| El destino no es alcanzable desde el origen | 422 | `No hay camino de X a Y` |
| Hay un ciclo de costo negativo | 422 | `Hay un ciclo de costo negativo` |

Los dos 422 no se pueden provocar con los datos del dominio (conexo, costos positivos): los cubren
`FloydWarshallTest.caminoInalcanzableFalla` y `caminoConCicloNegativoFalla`.

**Cómo se reconstruye el camino: matriz `siguiente[i][j]`** = primer vértice al que hay que ir desde
i para llegar a j por el camino mínimo.
- **Inicialización:** `siguiente[i][j] = j` si hay calle i – j (se va directo), `i` en la diagonal y
  `−1` si no hay calle (todavía no se conoce camino).
- **Al relajar por k** (cuando d[i][k] + d[k][j] < d[i][j]): `siguiente[i][j] = siguiente[i][k]`. El
  nuevo camino de i a j pasa por k, así que el primer paso es el mismo que para ir de i a k.
- **Reconstrucción:** desde u = origen se salta a `siguiente[u][destino]` hasta llegar al destino.
  Ejemplo PAL → CUN: siguiente[PAL][CUN] = MON (se actualizó al relajar por k = MON, porque
  66 + 18 < 90), y siguiente[MON][CUN] = CUN (calle directa).

Es la contracara de `previo[]` en Dijkstra: Dijkstra guarda el **anterior** desde un único origen
(un vector) y reconstruye desde el destino hacia atrás; Floyd guarda el **siguiente** para cada par
(una matriz) y reconstruye hacia adelante, para cualquier origen.

**Complejidad temporal: O(V³)** del triple bucle (igual que `/floyd`; actualizar `siguiente` es
O(1) más por relajación) + O(V²) de inicializar `siguiente` + **O(V)** de reconstruir el camino (un
camino simple tiene a lo sumo V vértices). Total **O(V³)**.

**Complejidad espacial: O(V²)**: la matriz de distancias y la matriz `siguiente`.

**Estructura usada:** **matriz de adyacencia** `double[][]` para las distancias y **matriz
`int[][] siguiente`**: las dos con acceso O(1) por índice, que es lo que mantiene O(1) cada paso del
triple bucle y cada salto de la reconstrucción.

**¿Floyd o Dijkstra para una sola consulta?** Para un solo par conviene Dijkstra: O((V + E) log V)
contra O(V³). Floyd conviene si se van a responder **muchas** consultas (la matriz se calcula una
vez y después cada camino cuesta O(V)) o si hay aristas negativas. En nuestra API cada request
recalcula la matriz porque el grafo se carga de nuevo en cada request.

---

## GET /api/grafo/floyd/comparacion

**Floyd contra Dijkstra corrido desde cada vértice.** Los dos resuelven "todos los pares": Floyd en
una corrida, Dijkstra repitiéndolo V veces (una por origen). El endpoint mide el trabajo de cada uno
sobre el grafo del dominio.

Implementación: `CaminosMinimos.compararConDijkstra` (`algorithm/CaminosMinimos.java`). Usa el mismo
triple bucle de `floydWarshall` y el mismo núcleo de `dijkstra` (`arbolDesde`), con contadores.

**Parámetros:** ninguno.

**Respuesta**

| Campo | Qué cuenta |
|---|---|
| `vertices` | V. |
| `operacionesFloyd` | Iteraciones del triple bucle de Floyd: cada una es una relajación (suma + comparación) d[i][k] + d[k][j] < d[i][j]. Siempre V³. |
| `nodosExpandidosDijkstra` | Extracciones **válidas** de la cola de prioridad, sumadas sobre las V corridas. No cuenta las entradas viejas que se descartan (eliminación perezosa). En un grafo conexo cada corrida expande los V vértices: V². |
| `aristasExaminadasDijkstra` | Aristas miradas al expandir cada nodo (el tamaño de su lista de adyacencia), sumadas sobre las V corridas. Cada corrida mira las 2E entradas de la lista de adyacencia: V · 2E. |

**Ejemplo**

```
GET /api/grafo/floyd/comparacion
```

```json
{"vertices":12,"operacionesFloyd":1728,"nodosExpandidosDijkstra":144,"aristasExaminadasDijkstra":504}
```

- 1728 = 12³.
- 144 = 12 corridas × 12 nodos expandidos (el grafo es conexo).
- 504 = 12 corridas × 42 (2E = 2 × 21 entradas de adyacencia, porque cada calle está en las dos
  listas).

Lo verifica `FloydWarshallTest.comparacionConDijkstraEnElDominio`. Que los dos métodos den **los
mismos 144 valores** lo verifica `coincideConDijkstraEnTodosLosPares`.

**Errores:** si hay alguna calle con costo negativo → 400 `Dijkstra no admite costos negativos: ...`
(la comparación necesita correr Dijkstra, así que valida los costos antes de empezar). Un ciclo
negativo implica alguna arista negativa, así que también cae en este 400.

**Análisis asintótico**
- **Floyd:** Θ(V³), independiente de E.
- **Dijkstra V veces:** V · O((V + E) log V) = O(V (V + E) log V).
  - Grafo **disperso** (E ≈ V, como un mapa de calles: cada lugar solo se conecta con sus vecinos):
    O(V² log V), **menor** que V³.
  - Grafo **denso** (E ≈ V²): O(V³ log V), **mayor** que V³: ahí conviene Floyd.
- Ejemplo de escala con la proporción de nuestro grafo (E ≈ 2V): con V = 1000, Floyd hace 10⁹
  relajaciones; Dijkstra V veces ronda 1000 · (1000 + 2 · 2000) · log₂ 1000 ≈ 5 · 10⁷, unas 20
  veces menos. Con V = 1000 y grafo denso (E ≈ 5 · 10⁵), Dijkstra V veces ronda 10¹⁰ y pierde.

**Lectura honesta del ejemplo.** Las métricas no miden lo mismo con el mismo costo: cada
relajación de Floyd es O(1), mientras que cada arista examinada por Dijkstra puede generar una
operación de heap O(log V). Con V = 12, 504 · log₂ 12 ≈ 1800 es del mismo orden que 1728: en un
grafo tan chico la diferencia es menor; lo que importa es cómo crece cada uno con V.

**Cuándo conviene cada uno**

| | Floyd-Warshall | Dijkstra V veces |
|---|---|---|
| Complejidad | O(V³) | O(V (V + E) log V) |
| Grafo disperso | Peor | **Mejor** |
| Grafo denso | **Mejor** (y más simple) | Peor |
| Aristas negativas (sin ciclo negativo) | **Sí** | No |
| Detecta ciclos negativos | **Sí** (diagonal) | No |
| Espacio | O(V²) (la matriz) | O(V + E) por corrida, O(V²) si se guardan todos los resultados |

**Complejidad del endpoint:** O(V³ + V (V + E) log V), porque corre los dos. Espacio O(V² + E).

**Estructura usada:** la matriz de adyacencia para Floyd y, para Dijkstra, la lista de adyacencia
con la `PriorityQueue` (las mismas de cada ficha). Los contadores son variables `long`.

---

## GET /api/grafo/prim

**Árbol de recubrimiento mínimo (MST) con Prim.** En el dominio: el **mínimo de minutos de calles a
despejar** de zombies para que todos los lugares queden conectados entre sí.

Implementación: `ArbolRecubrimiento.prim` (`algorithm/ArbolRecubrimiento.java`).

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `origen` | String | sí | Lugar desde el que empieza a crecer el árbol (ej. `RET`). El costo total no depende del origen; el orden en que se agregan las aristas sí. |

El MST se calcula siempre sobre el grafo **no dirigido** (el controller carga con `false`).

**Respuesta:** `aristas` = calles del árbol **en el orden en que se agregan**, cada una como
`origen` (vértice que ya estaba en el árbol) → `destino` (vértice que entra) con su `costo`;
`costoTotal` = suma de los costos.

**Ejemplo**

```
GET /api/grafo/prim?origen=RET
```

```json
{"aristas":[{"origen":"RET","destino":"PMA","costo":18.0},{"origen":"RET","destino":"REC","costo":20.0},{"origen":"PMA","destino":"OBE","costo":20.0},{"origen":"OBE","destino":"ABA","costo":28.0},{"origen":"ABA","destino":"HIT","costo":19.0},{"origen":"HIT","destino":"PCE","costo":18.0},{"origen":"HIT","destino":"PAL","costo":21.0},{"origen":"PMA","destino":"CON","costo":30.0},{"origen":"CON","destino":"LBO","costo":29.0},{"origen":"PAL","destino":"MON","costo":66.0},{"origen":"MON","destino":"CUN","costo":18.0}],"costoTotal":287.0}
```

11 aristas (V − 1) y 287 minutos. Son **exactamente las mismas** 11 calles que devuelve Kruskal,
porque el MST de nuestro grafo es único (ver README). El paso a paso está en
[`VERIFICACIONES.md`](VERIFICACIONES.md#3-prim-desde-ret).

**Errores**

| Situación | Código | `detalle` |
|---|---|---|
| `origen` inexistente | 400 | `No existe el nodo con id: XXX` |
| Grafo dirigido (no aplica desde el endpoint, cubierto por test) | 400 | `El MST requiere grafo no dirigido` |
| Grafo no conexo (cubierto por test) | 422 | `El grafo no es conexo` |

**Cómo funciona.** Hace crecer **un único árbol** desde el origen. `key[v]` es el costo de la
calle más barata que conecta v con el árbol; en cada paso entra el vértice de fuera del árbol con
menor `key` (la arista más barata que cruza el corte árbol / resto), y se actualizan las `key` de
sus vecinos. Es greedy: la arista elegida no se reconsidera, y la propiedad del corte garantiza que
pertenece a algún MST.

**Prim y Dijkstra: mismo esqueleto, cambia la prioridad.** Los dos usan una cola de prioridad,
extraen el mínimo, lo marcan como definitivo y actualizan a los vecinos. La diferencia es qué se
compara:
- Dijkstra: `dist[u] + costo(u, v) < dist[v]` → distancia **acumulada desde el origen**.
- Prim: `costo(u, v) < key[v]` → costo de **la arista sola**, para conectar v al árbol.

Por eso Dijkstra da caminos mínimos desde un origen y Prim da el árbol más barato que conecta a
todos; en general no son el mismo árbol. Ejemplo: desde RET, Dijkstra llega a PAL por la calle
directa RET – PAL (45 min, el camino más rápido), mientras que el MST no usa esa calle y conecta
PAL por HIT – PAL (21 min, la calle más barata para sumarlo al árbol).

**Complejidad temporal: O(E log V).**
- Misma técnica de **eliminación perezosa** que Dijkstra: al bajar `key[v]` se reinserta v en la
  cola; al extraer un vértice que ya está en el árbol (`inMST`), la entrada se descarta.
- Cada vértice entra al árbol una vez y su lista de adyacencia se recorre una vez (2E aristas
  miradas). Una calle u – v solo puede generar una inserción desde el extremo que entró primero,
  así que la cola tiene a lo sumo E + 1 entradas, cada `add`/`poll` en O(log E) = O(log V).
- Total O((V + E) log V), y como el grafo es conexo (E ≥ V − 1) queda **O(E log V)**. En el ejemplo
  hubo 19 inserciones: 12 vértices que entran al árbol y 7 entradas viejas descartadas.

**¿Por qué heap y no la versión O(V²) con arreglo?** La versión con matriz/arreglo busca el mínimo
recorriendo las V `key` en cada paso: O(V²), que conviene en grafos **densos** (E ≈ V², donde el
heap daría O(V² log V)). Nuestro grafo es **disperso**: V = 12, E = 21 (lejos de las
V(V − 1)/2 = 66 calles posibles), así que E log V (≈ 21 · 3,6 ≈ 75) es menor que V² (144), y el
mapa de una ciudad a pie siempre es disperso: cada lugar se conecta solo con sus vecinos.

**Complejidad espacial: O(V + E)**: arreglos `key[]`, `parent[]` e `inMST[]` (V), cola (hasta
E + 1 entradas) y la lista de aristas del árbol (V − 1).

**Estructura usada:**
- **Cola de prioridad (`PriorityQueue`, heap binario)** ordenada por `key`: mínimo en O(log V).
- **Arreglos `key[]`, `parent[]`, `inMST[]`** (los de la diapositiva): O(1) por consulta;
  `parent[]` guarda el árbol y permite emitir la arista (parent[u], u) cuando u entra.
- **Lista de adyacencia**: recorrer los vecinos de u cuesta O(grado(u)).

---

## GET /api/grafo/kruskal

**Árbol de recubrimiento mínimo (MST) con Kruskal y Union-Find.** En el dominio: la misma pregunta
que Prim, el mínimo de calles a despejar para conectar todos los lugares, resuelta mirando las
calles de la más corta a la más larga.

Implementación: `ArbolRecubrimiento.kruskal` (`algorithm/ArbolRecubrimiento.java`), que ordena con
el `Ordenamiento.mergeSort` propio.

**Parámetros:** ninguno. Se calcula sobre el grafo **no dirigido**.

**Respuesta:** `aristas` = calles del árbol **en el orden en que se aceptan** (costo creciente);
`costoTotal` = suma.

**Ejemplo**

```
GET /api/grafo/kruskal
```

```json
{"aristas":[{"origen":"CUN","destino":"MON","costo":18.0},{"origen":"HIT","destino":"PCE","costo":18.0},{"origen":"PMA","destino":"RET","costo":18.0},{"origen":"ABA","destino":"HIT","costo":19.0},{"origen":"OBE","destino":"PMA","costo":20.0},{"origen":"REC","destino":"RET","costo":20.0},{"origen":"HIT","destino":"PAL","costo":21.0},{"origen":"ABA","destino":"OBE","costo":28.0},{"origen":"CON","destino":"LBO","costo":29.0},{"origen":"CON","destino":"PMA","costo":30.0},{"origen":"MON","destino":"PAL","costo":66.0}],"costoTotal":287.0}
```

11 aristas, 287 minutos: mismo conjunto de calles que Prim, en otro orden. Cada calle se toma una
sola vez (no las dos copias de la lista de adyacencia), con sus extremos en orden alfabético de id.
Los costos salen no decrecientes; entre empates el orden es siempre el mismo porque MergeSort es
estable. Qué calles se aceptan y cuáles se rechazan por formar ciclo está en
[`VERIFICACIONES.md`](VERIFICACIONES.md#2-kruskal).

**Errores:** grafo no conexo → 422 `El grafo no es conexo`; grafo dirigido → 400
`El MST requiere grafo no dirigido` (los dos cubiertos por `ArbolRecubrimientoTest`; con los datos
del dominio no ocurren).

**Cómo funciona.**
1. Se arma la lista de las E calles (cada una una vez) y se ordena por costo creciente con
   **MergeSort propio**.
2. Se recorren en ese orden. Para cada calle (u, v): si `find(u) != find(v)`, u y v están en
   componentes distintas → se acepta y se hace `union(u, v)`. Si `find(u) == find(v)`, ya están
   conectados y la calle **cerraría un ciclo** → se descarta.
3. Se corta apenas el árbol tiene V − 1 aristas. Si se terminan las calles antes, el grafo no es
   conexo.

Es greedy: siempre toma la calle más barata que no forma ciclo, y no la reconsidera.

**Complejidad temporal: O(E log E) = O(E log V).**
- Armar la lista de calles: O(V + E).
- **Ordenar con MergeSort: O(E log E)**, que es lo que domina. Como E ≤ V², log E ≤ 2 log V, por
  eso también se escribe O(E log V).
- Recorrer las calles: a lo sumo E iteraciones con dos `find` y quizás un `union`. Con
  **compresión de caminos y unión por rango** cada operación cuesta O(α(V)) amortizado (α es la
  inversa de Ackermann, ≤ 4 para cualquier tamaño real): **prácticamente O(1)**, en total
  O(E α(V)), menor que el ordenamiento.

**Complejidad espacial: O(V + E)**: lista de calles y su copia ordenada (E, más el auxiliar de
MergeSort), arreglos `padre[]` y `rango[]` del Union-Find (V), aristas del árbol (V − 1).

**Estructura usada:**
- **Union-Find (conjuntos disjuntos)** implementado a mano: `find` con **compresión de caminos**
  (cada nodo recorrido queda apuntando directo a la raíz) y `union` **por rango** (el árbol más
  bajo se cuelga del más alto, así la altura queda en O(log V) aun sin compresión). Es lo que hace
  barata la pregunta "¿esta calle cierra un ciclo?": sin Union-Find habría que hacer un BFS/DFS por
  cada calle, O(V + E) cada vez, y el total sería O(E · (V + E)).
- **MergeSort propio** (sin `Collections.sort` ni `List.sort`): O(E log E) garantizado en cualquier
  caso, y estable.

**¿Prim o Kruskal?** Los dos dan O(E log V) en grafos dispersos como el nuestro.
- **Kruskal** conviene en grafos **dispersos** o cuando las aristas ya vienen como lista (o ya
  ordenadas): su costo lo marca ordenar E aristas, y no necesita lista de adyacencia.
- **Prim** conviene en grafos **densos** (E ≈ V²): con la versión O(V²) sobre matriz no depende de
  E, mientras que Kruskal tendría que ordenar ≈ V² aristas, O(V² log V).
- Prim necesita un origen y hace crecer un solo árbol; Kruskal arranca con V componentes sueltas
  (un bosque) y las va uniendo.

---

## GET /api/seleccion/quicksort

**Ordenamiento de suministros con QuickSort.** En el dominio: listar los suministros disponibles
según su peso, su utilidad o su ratio utilidad/peso (el criterio que usa el greedy de la mochila).

Implementación: `Ordenamiento.quickSort` (`algorithm/Ordenamiento.java`). Es genérico (recibe un
`Comparator`); el controller solo arma el comparador según el parámetro.

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `criterio` | String: `valor` \| `peso` \| `ratio` | no (default `ratio`) | Por qué se ordena: `valor` = utilidad, `peso` = kg, `ratio` = utilidad / kg. |
| `orden` | String: `asc` \| `desc` | no (default `asc`) | Orden ascendente (de menor a mayor) o descendente. |

**Respuesta:** la lista de suministros ordenada. Cada objeto trae `id`, `nombre`, `peso` (kg),
`valor` (utilidad) y `nodoId` (lugar donde está el suministro). La lista de entrada no se modifica:
se ordena una copia.

**Ejemplo** (abreviado: se omite `nodoId`)

```
GET /api/seleccion/quicksort?criterio=ratio
```

```json
[
  {"id":"S6","nombre":"Bate","peso":5.0,"valor":10.0},
  {"id":"S8","nombre":"Herramientas","peso":5.0,"valor":24.0},
  {"id":"S7","nombre":"Bolsa de dormir","peso":6.0,"valor":30.0},
  {"id":"S5","nombre":"Radio","peso":2.0,"valor":14.0},
  {"id":"S3","nombre":"Agua 7L","peso":7.0,"valor":55.0},
  {"id":"S4","nombre":"Linterna","peso":1.0,"valor":8.0},
  {"id":"S2","nombre":"Comida enlatada","peso":4.0,"valor":40.0},
  {"id":"S1","nombre":"Botiquin","peso":3.0,"valor":50.0}
]
```

Ratios: 2,0 · 4,8 · 5,0 · 7,0 · 7,86 · 8,0 · 10,0 · 16,67 (ascendente). Como los 8 ratios son
distintos, el resultado no depende del orden en que la base devuelve los items.

Para el greedy interesa el **mayor ratio primero**, con `orden=desc`:

```
GET /api/seleccion/quicksort?criterio=ratio&orden=desc
```

```json
[
  {"id":"S1","nombre":"Botiquin","peso":3.0,"valor":50.0},
  {"id":"S2","nombre":"Comida enlatada","peso":4.0,"valor":40.0},
  {"id":"S4","nombre":"Linterna","peso":1.0,"valor":8.0},
  {"id":"S3","nombre":"Agua 7L","peso":7.0,"valor":55.0},
  {"id":"S5","nombre":"Radio","peso":2.0,"valor":14.0},
  {"id":"S7","nombre":"Bolsa de dormir","peso":6.0,"valor":30.0},
  {"id":"S8","nombre":"Herramientas","peso":5.0,"valor":24.0},
  {"id":"S6","nombre":"Bate","peso":5.0,"valor":10.0}
]
```

`orden=desc` invierte el comparador (`Comparator.reversed()`) en el controller; el algoritmo es el
mismo, solo recibe otro criterio.

**Errores:**
- `criterio` distinto de `valor`, `peso` o `ratio` → 400
  `{"estado":"ERROR","detalle":"Criterio invalido: xyz. Use valor, peso o ratio."}`
- `orden` distinto de `asc` o `desc` → 400
  `{"estado":"ERROR","detalle":"Orden invalido: xyz. Use asc o desc"}`

**Cómo funciona.** Divide y conquista: se elige un **pivote**, se **particiona** (menores a la
izquierda, el resto a la derecha; el pivote queda en su posición definitiva) y se ordena
recursivamente cada lado. Partición de **Lomuto** con **pivote = elemento del medio** de la
sublista (se lleva al final y se recorre la sublista una vez).

**Recurrencias** (método de división / sustracción visto en clase):
- **Mejor caso y caso promedio:** el pivote parte la lista en dos mitades parecidas.
  T(n) = 2T(n/2) + O(n): a = 2 llamadas, b = 2 (tamaño n/2), k = 1 (particionar es lineal).
  Como a = b^k (2 = 2¹) → **O(n^k log n) = O(n log n)**.
- **Peor caso:** el pivote cae siempre en un extremo y una de las dos partes queda vacía.
  T(n) = T(n − 1) + O(n): recurrencia por sustracción con a = 1 → **O(n^(k+1)) = O(n²)**
  (n + (n − 1) + … + 1 comparaciones).

**¿Cuándo ocurre el peor caso?** Con pivote = primer elemento, una lista **ya ordenada** (o
invertida) es el peor caso; con el pivote del medio esa entrada se parte justo a la mitad (es el
mejor caso). El peor caso no desaparece, cambia de entrada:
- cuando **todos los elementos son iguales** según el criterio: Lomuto manda todo a un mismo lado
  (compara con `<` estricto) y cada partición deja n − 1 elementos;
- con entradas armadas para que el elemento del medio sea siempre el mínimo o el máximo.

**Complejidad espacial:** O(n) por la copia de la lista (para no modificar la entrada); la
partición es in-place sobre esa copia. La pila de recursión es O(log n) en promedio y O(n) en el
peor caso.

**No es estable:** los intercambios de la partición pueden invertir el orden relativo de dos
elementos con la misma clave (por ejemplo S6 y S8, que pesan 5 kg). En el test
`quickSortSuministrosPorPeso` se verifican solo los pesos, no el orden de los empatados.

**Estructura usada:** `ArrayList` (copia de la entrada), con acceso e intercambio por índice en
O(1), que es lo que necesita la partición in-place para ser O(n).

---

## GET /api/seleccion/mergesort

**Ordenamiento de suministros con MergeSort.** Mismo uso en el dominio que QuickSort; la diferencia
es que MergeSort es **estable** y su O(n log n) vale **en todos los casos**. Es el que usa Kruskal
para ordenar las calles.

Implementación: `Ordenamiento.mergeSort` (`algorithm/Ordenamiento.java`).

**Parámetros:** los mismos que QuickSort.

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `criterio` | String: `valor` \| `peso` \| `ratio` | no (default `ratio`) | Por qué se ordena: `valor` = utilidad, `peso` = kg, `ratio` = utilidad / kg. |
| `orden` | String: `asc` \| `desc` | no (default `asc`) | Orden ascendente o descendente. |

**Ejemplo** (abreviado: se omite `nodoId`)

```
GET /api/seleccion/mergesort?criterio=peso
```

```json
[
  {"id":"S4","nombre":"Linterna","peso":1.0,"valor":8.0},
  {"id":"S5","nombre":"Radio","peso":2.0,"valor":14.0},
  {"id":"S1","nombre":"Botiquin","peso":3.0,"valor":50.0},
  {"id":"S2","nombre":"Comida enlatada","peso":4.0,"valor":40.0},
  {"id":"S6","nombre":"Bate","peso":5.0,"valor":10.0},
  {"id":"S8","nombre":"Herramientas","peso":5.0,"valor":24.0},
  {"id":"S7","nombre":"Bolsa de dormir","peso":6.0,"valor":30.0},
  {"id":"S3","nombre":"Agua 7L","peso":7.0,"valor":55.0}
]
```

**Estabilidad:** S6 (Bate) y S8 (Herramientas) pesan los dos 5 kg y S6 viene antes en la entrada,
así que queda **antes** que S8 también en la salida. Lo garantiza la mezcla: ante un empate
(`compare <= 0`) toma primero el elemento de la mitad **izquierda**. Lo verifican
`mergeSortSuministrosPorPesoEsEstable` y `mergeSortEstableConMuchosEmpates`.

Con `?criterio=ratio&orden=desc` devuelve la misma lista que QuickSort (S1, S2, S4, S3, S5, S7,
S8, S6). Al invertir el orden la mezcla sigue siendo estable: el comparador invertido también da 0
en los empates (test `mergeSortDescendenteConEmpatesSigueSiendoEstable`).

**Errores:** igual que QuickSort (criterio u orden inválido → 400).

**Cómo funciona.** Divide y conquista: se parte la lista en dos mitades, se ordena cada una
recursivamente (caso base: 0 o 1 elemento) y se **mezclan** las dos mitades ordenadas en una
recorriéndolas una sola vez.

**Recurrencia:** T(n) = 2T(n/2) + O(n). Método de división: a = 2 (dos llamadas), b = 2 (cada una
con n/2), k = 1 (la mezcla es lineal). Como a = b^k → **O(n^k log n) = O(n log n)**. A diferencia
de QuickSort, la división es siempre por la mitad sin importar los datos, así que **no hay peor
caso cuadrático**: O(n log n) en mejor, promedio y peor caso. Intuición: log₂ n niveles de
recursión, y en cada nivel las mezclas suman n elementos.

**Complejidad espacial: O(n)** auxiliar: cada mezcla crea una lista nueva del tamaño de lo que
mezcla, y en un momento dado las listas vivas suman O(n). La pila de recursión es O(log n).

**Estructura usada:** `ArrayList` para las mitades y el resultado: acceso por índice O(1) y
`add` al final O(1) amortizado, lo que hace la mezcla O(n). No se usa `Collections.sort` ni
`List.sort`.

**QuickSort vs. MergeSort**

| | QuickSort | MergeSort |
|---|---|---|
| Promedio | O(n log n) | O(n log n) |
| Peor caso | O(n²) | O(n log n) |
| Espacio extra | O(log n) de pila (más la copia) | O(n) |
| Estable | No | Sí |
| Donde lo usamos | `/quicksort` | `/mergesort`, Kruskal y el greedy de la mochila (necesitan orden garantizado y determinista) |

---

### El problema de la mochila (común a `/greedy`, `/dinamica` y `/comparar`)

En el dominio: un sobreviviente sale del refugio con una mochila que aguanta `capacidad` kg y tiene
que decidir **qué suministros llevar**. Cada suministro se lleva **entero o no se lleva** (no se
puede llevar media radio): es la **mochila 0/1**.

- **Función objetivo:** maximizar la utilidad total Σ vᵢ de los suministros elegidos.
- **Restricción:** Σ pᵢ ≤ capacidad (pesos en kg).
- **Datos:** los 8 suministros de la base (tabla en el [README](../README.md#suministros-items)),
  leídos ordenados por id con `findAllByOrderByIdAsc()` para que el resultado sea reproducible.

Las dos técnicas están en `algorithm/Seleccion.java` y devuelven el mismo tipo de respuesta:

| Campo | Significado |
|---|---|
| `elegidos` | Suministros que van en la mochila. Cada uno con `id`, `nombre`, `peso` (kg), `valor` (utilidad) y `nodoId` (lugar donde está). |
| `valorTotal` | Utilidad total de lo elegido. |
| `pesoTotal` | kg ocupados (≤ capacidad). |
| `tecnica` | `GREEDY` o `PROGRAMACION_DINAMICA`. |

---

## GET /api/seleccion/greedy

**Mochila 0/1 resuelta con un algoritmo greedy.** En el dominio: llenar la mochila tomando primero
los suministros que más utilidad dan **por kg** (el ratio utilidad/peso), sin volver atrás.

Implementación: `Seleccion.greedy` (`algorithm/Seleccion.java`). El controller solo arma el
`Comparator` según `criterio` y `orden`; el algoritmo es el mismo para cualquier criterio.

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `capacidad` | double (≥ 0) | sí | kg que aguanta la mochila. Acepta decimales (greedy no indexa una tabla). |
| `criterio` | String: `valor` \| `peso` \| `ratio` | no (default `ratio`) | Qué se mira para decidir el orden en que se consideran los suministros. |
| `orden` | String: `asc` \| `desc` | no (default `desc`) | `desc` = el mayor primero. |

El greedy "de la materia" es el default, **ratio descendente**. Los otros criterios están para
comparar (por ejemplo `criterio=valor` es "lo más útil primero", `criterio=peso&orden=asc` es "lo
más liviano primero").

**Ejemplo**

```
GET /api/seleccion/greedy?capacidad=12
```

```json
{"elegidos":[{"id":"S1","nombre":"Botiquin","peso":3.0,"valor":50.0,"nodoId":"HIT"},{"id":"S2","nombre":"Comida enlatada","peso":4.0,"valor":40.0,"nodoId":"ABA"},{"id":"S4","nombre":"Linterna","peso":1.0,"valor":8.0,"nodoId":"OBE"},{"id":"S5","nombre":"Radio","peso":2.0,"valor":14.0,"nodoId":"RET"}],"valorTotal":112.0,"pesoTotal":10.0,"tecnica":"GREEDY"}
```

Los elegidos salen en el orden en que se tomaron (ratio decreciente). Traza:

| Orden | Suministro | kg | Utilidad | Ratio | Decisión | kg usados |
|---|---|---|---|---|---|---|
| 1 | S1 Botiquín | 3 | 50 | 16,67 | Toma | 3 |
| 2 | S2 Comida enlatada | 4 | 40 | 10,00 | Toma | 7 |
| 3 | S4 Linterna | 1 | 8 | 8,00 | Toma | 8 |
| 4 | S3 Agua 7L | 7 | 55 | 7,86 | **Saltea** (7 > 4 libres) | 8 |
| 5 | S5 Radio | 2 | 14 | 7,00 | Toma | 10 |
| 6 | S7 Bolsa de dormir | 6 | 30 | 5,00 | Saltea | 10 |
| 7 | S8 Herramientas | 5 | 24 | 4,80 | Saltea | 10 |
| 8 | S6 Bate | 5 | 10 | 2,00 | Saltea | 10 |

Resultado: **112 de utilidad con 10 kg**. El óptimo con 12 kg es **119** (`/dinamica`): este es el
juego de datos donde el greedy **no** da el óptimo. El análisis completo está en
[`VERIFICACIONES.md`](VERIFICACIONES.md#4-greedy-vs-mochila-01-con-pd).

**Saltea, no corta.** Cuando un suministro no entra en lo que queda, el algoritmo **sigue con el
siguiente** en vez de terminar, porque uno más liviano todavía puede entrar. Con 12 kg, S3 (7 kg) no
entra en los 4 kg libres, pero S5 (2 kg) sí: saltear da 112; **cortar en S3 daría 98** (S1 + S2 +
S4). Las dos variantes son greedy, pero cortar desperdicia capacidad sin ninguna razón.

**Errores**

```
GET /api/seleccion/greedy?capacidad=-1
```

```
400 Bad Request
{"estado":"ERROR","detalle":"La capacidad no puede ser negativa: -1.0"}
```

| Situación | Código | `detalle` |
|---|---|---|
| `capacidad` negativa | 400 | `La capacidad no puede ser negativa: ...` |
| `capacidad=NaN` o `Infinity` (Java los acepta como `double`) | 400 | `La capacidad tiene que ser un numero finito: NaN` |
| Algún suministro con peso o valor negativo | 400 | `Peso y valor no pueden ser negativos: <id>` |
| `capacidad` no numérica (ej. `abc`) | 400 | `Valor invalido para capacidad: abc` |
| Falta `capacidad` | 400 | `Falta el parametro: capacidad` |
| `criterio` u `orden` inválido | 400 | `Criterio invalido: ...` / `Orden invalido: ...` |

**Por qué es greedy.** Elige en cada paso **la mejor opción local** (el mayor ratio que todavía
entra) y **nunca reconsidera** una decisión: lo que entró no sale y lo que se salteó no se vuelve a
mirar. Es un único recorrido de la lista ordenada.

**No garantiza el óptimo en la mochila 0/1.** Como los suministros son indivisibles, tomar el mejor
ratio puede dejar capacidad sin usar que una combinación distinta aprovecharía (con 12 kg sobran
2 kg; el óptimo cambia S2 + S4 por S3). Si se pudiera **fraccionar** el primer suministro que no
entra, sería la **mochila fraccional de la clase 3**, y ahí el greedy por ratio **sí es óptimo**:
cada kg se llena con la mayor utilidad por kg disponible. Con 12 kg la fraccional daría
98 + 4/7 · 55 ≈ 129,4, una cota superior del óptimo 0/1 (119 ≤ 129,4).

**Complejidad temporal: O(n log n)**, con n = cantidad de suministros.
- Ordenar por el criterio con **MergeSort propio**: O(n log n) (recurrencia
  T(n) = 2T(n/2) + O(n), ver la ficha de MergeSort).
- Recorrer la lista ordenada una vez: O(n), cada decisión es una comparación y una resta.
- Validar los datos y sumar el resultado: O(n).
- Total: O(n log n) + O(n) = **O(n log n), dominada por el ordenamiento**.

**Complejidad espacial: O(n)**: la lista ordenada (MergeSort crea listas auxiliares O(n)) y la
lista de elegidos.

**Estructura usada:**
- **MergeSort propio** (`Ordenamiento.mergeSort`, sin `Collections.sort`): O(n log n) en todos los
  casos y **estable**, así que ante dos ratios iguales respeta el orden de entrada (por id) y el
  resultado es determinista.
- **`ArrayList`** para la lista ordenada y los elegidos: recorrido secuencial y `add` O(1)
  amortizado.

---

## GET /api/seleccion/dinamica

**Mochila 0/1 resuelta con programación dinámica.** En el dominio: la combinación de suministros
que da **la máxima utilidad posible** sin pasarse de los kg de la mochila. A diferencia del greedy,
siempre da el óptimo.

Implementación: `Seleccion.programacionDinamica` (`algorithm/Seleccion.java`), tabla bottom-up y
recuperación del camino.

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `capacidad` | int (≥ 0) | sí | kg que aguanta la mochila. Es **entero** porque indexa las columnas de la tabla. |

Los pesos de los suministros también tienen que ser **enteros** (son los índices j − pᵢ de la
tabla); los valores pueden ser reales.

**Ejemplo**

```
GET /api/seleccion/dinamica?capacidad=12
```

```json
{"elegidos":[{"id":"S1","nombre":"Botiquin","peso":3.0,"valor":50.0,"nodoId":"HIT"},{"id":"S3","nombre":"Agua 7L","peso":7.0,"valor":55.0,"nodoId":"PCE"},{"id":"S5","nombre":"Radio","peso":2.0,"valor":14.0,"nodoId":"RET"}],"valorTotal":119.0,"pesoTotal":12.0,"tecnica":"PROGRAMACION_DINAMICA"}
```

Botiquín + Agua 7L + Radio = 3 + 7 + 2 = **12 kg, 119 de utilidad**, contra 112 del greedy. Los
elegidos salen en el orden de entrada (por id).

Capacidad mayor que todo lo disponible:

```
GET /api/seleccion/dinamica?capacidad=1000000000
```

Devuelve los 8 suministros, `valorTotal` 231.0 y `pesoTotal` 33.0, en unos 0,04 s: la tabla se
arma con W = min(capacidad, Σ pesos) = 33 columnas útiles, no mil millones (ver más abajo).

**Errores**

| Situación | Código | `detalle` |
|---|---|---|
| `capacidad` negativa | 400 | `La capacidad no puede ser negativa: -1.0` |
| `capacidad` no entera o fuera de rango de `int` (ej. `12.5`) | 400 | `Valor invalido para capacidad: 12.5` |
| Algún suministro con peso no entero | 400 | `La programacion dinamica exige pesos enteros: <id> pesa <peso>` |
| Algún suministro con peso o valor negativo | 400 | `Peso y valor no pueden ser negativos: <id>` |

**Subproblema y recurrencia.** dp[i][j] = mejor utilidad usando **solo los primeros i suministros**
con una mochila de **j kg**.

```
dp[0][j] = 0                    (sin suministros no hay utilidad)
dp[i][0] = 0                    (mochila de 0 kg)
dp[i][j] = dp[i-1][j]                                si pᵢ > j   (el i no entra)
dp[i][j] = max(dp[i-1][j], dp[i-1][j - pᵢ] + vᵢ)     si pᵢ ≤ j   (no tomarlo / tomarlo)
```

Es PD porque cumple las dos condiciones de la clase 6:
- **Subestructura óptima:** si el suministro i está en la solución óptima para (i, j), el resto es
  óptimo para (i − 1, j − pᵢ); si no está, la solución es la óptima para (i − 1, j).
- **Subproblemas superpuestos:** distintas combinaciones de decisiones llegan al mismo (i, j). La
  recursión sin tabla resolvería los mismos subproblemas muchas veces:
  T(n) = 2T(n − 1) + O(1) → sustracción con a = 2 > 1 → O(2ⁿ) (es la fuerza bruta). La tabla
  calcula cada (i, j) **una sola vez**.

Se llena **bottom-up**, fila por fila: cada fila solo usa la anterior, que ya está completa. La
respuesta es dp[n][W].

**Recuperación del camino.** La tabla dice el máximo pero no **qué** suministros. Se recorre hacia
atrás desde dp[n][W]: si dp[i][j] ≠ dp[i − 1][j], el valor cambió al agregar el suministro i, así
que **se tomó**: se marca y se resta su peso (j −= pᵢ). Si son iguales, no se tomó. Se sube a la
fila i − 1 y se repite hasta i = 1. La tabla 4 × 9 del ejemplo de la clase 6 con su recuperación
está en [`VERIFICACIONES.md`](VERIFICACIONES.md#43-tabla-dp-a-mano-ejemplo-de-la-clase-6).

**W = min(capacidad, Σ pesos).** Si la mochila aguanta más que la suma de todos los pesos, entran
todos y las columnas de más repetirían el mismo valor. Acotar W da exactamente el mismo resultado y
evita reservar una tabla gigante (con `capacidad=1000000000` serían 9 × 10⁹ celdas).

**Complejidad temporal: O(n · W)**, con W = min(capacidad, Σ pesos).
- La tabla tiene (n + 1) × (W + 1) celdas y cada una se calcula en O(1) (una o dos lecturas de la
  fila anterior y una comparación).
- La recuperación recorre las n filas una vez: O(n). Validar y sumar pesos: O(n).
- **Es pseudopolinómica:** es polinómica en el **valor** numérico W, no en el **tamaño** de la
  entrada. W se escribe con log W bits; si se duplica la cantidad de bits de la capacidad, W puede
  elevarse al cuadrado. En función del tamaño de la entrada crece **exponencialmente**. Por eso
  acotar W con Σ pesos importa.

**Complejidad espacial: O(n · W)**: la tabla `double[n + 1][W + 1]` completa, más el arreglo
`tomado[]` (n). Se guarda la tabla entera (y no solo dos filas) porque la recuperación necesita
mirar todas las filas.

**Estructura usada:**
- **Matriz `double[][]`** (la tabla dp): acceso O(1) a dp[i − 1][j] y dp[i − 1][j − pᵢ] por índice,
  que es lo que hace O(1) cada celda.
- **Arreglo `boolean[] tomado`** para marcar los elegidos durante la recuperación y después
  devolverlos en el orden de entrada.

---

## GET /api/seleccion/comparar

**Greedy por ratio contra la mochila 0/1 con PD, sobre los mismos suministros y la misma
capacidad.** En el dominio: ¿cuánta utilidad se pierde por elegir "a ojo" (mejor ratio primero) en
vez de calcular el óptimo?

Implementación: el controller lee los suministros **una vez** y delega en `Seleccion.comparar`, que
corre `greedy` (criterio ratio descendente) y `programacionDinamica` sobre la misma lista y devuelve
el record `Seleccion.Comparacion`.

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `capacidad` | int (≥ 0) | sí | kg de la mochila (entero, porque lo usa la PD). |

**Ejemplo** (abreviado: los `elegidos` son los mismos de las dos fichas anteriores)

```
GET /api/seleccion/comparar?capacidad=12
```

```
{"greedy":{"elegidos":[S1, S2, S4, S5],"valorTotal":112.0,"pesoTotal":10.0,"tecnica":"GREEDY"},
 "dinamica":{"elegidos":[S1, S3, S5],"valorTotal":119.0,"pesoTotal":12.0,"tecnica":"PROGRAMACION_DINAMICA"}}
```

Con 12 kg el greedy pierde 7 puntos de utilidad (112 vs. 119). La tabla con otras capacidades
(0 a 33 kg) está en [`VERIFICACIONES.md`](VERIFICACIONES.md#44-resumen-greedy-vs-pd-por-capacidad).

**Errores:** los mismos que `/dinamica`.

**Complejidad:** O(n log n + n · W), la suma de las dos técnicas; en la práctica domina la tabla de
la PD. Espacio O(n · W).

**Greedy vs. PD**

| | Greedy por ratio | Mochila 0/1 con PD |
|---|---|---|
| Garantiza el óptimo (0/1) | No | **Sí** |
| Tiempo | O(n log n) | O(n · W), pseudopolinómico |
| Espacio | O(n) | O(n · W) |
| Capacidad y pesos | Reales | Enteros |
| Óptimo en la mochila fraccional | **Sí** | (no aplica) |

---

## Errores

Los errores se devuelven como JSON desde `config/ManejadorDeErrores`, nunca como stack trace.

| Situación | Código | Cuerpo |
|---|---|---|
| Id de lugar inexistente o parámetro inválido (`IllegalArgumentException`) | 400 | `{"estado":"ERROR","detalle":"..."}` |
| Falta un parámetro obligatorio (ej. `/dijkstra?origen=RET`) | 400 | `{"estado":"ERROR","detalle":"Falta el parametro: destino"}` |
| Parámetro con tipo incorrecto (ej. `/repartir?contenedores=abc`) | 400 | `{"estado":"ERROR","detalle":"Valor invalido para contenedores: abc"}` |
| Estado inválido (`IllegalStateException`) | 422 | `{"estado":"ERROR","detalle":"..."}` |
| Algoritmo todavía no implementado | 501 | `{"estado":"PENDIENTE","detalle":"..."}` |
| Ruta inexistente / método no permitido | 404 / 405 | `{"estado":"ERROR","detalle":"Not Found"}` |
| Error inesperado (ej. Neo4j caído) | 500 | `{"estado":"ERROR","detalle":"Error interno: <tipo>"}` (el detalle queda en el log) |

**Ejemplo real**

```
GET /api/grafo/bfs?origen=XXX
```

```
400 Bad Request
{"estado":"ERROR","detalle":"No existe el nodo con id: XXX"}
```

Lo lanza `Grafo.indiceDe` al traducir el id de origen, antes de empezar el recorrido. Lo mismo
pasa con `/api/grafo/dfs?origen=XXX`.

---

## Pendientes

Endpoints cableados cuyos algoritmos todavía no están implementados (responden 501). Su ficha se
escribe cuando estén implementados y probados.

| Método y ruta | Parámetros | Algoritmo |
|---|---|---|
| `GET /api/grafo/ucs` | `origen` (String), `destino` (String) | UCS (opcional) |
| `GET /api/grafo/rutas` | `origen` (String), `destino` (String), `costoMaximo` (double, default 1000), `saltosMaximos` (int, default 10) | Backtracking |
| `GET /api/seleccion/repartir` | `contenedores` (int, ≥ 1) | Branch & Bound |
