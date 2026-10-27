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
- [GET /api/grafo/rutas](#get-apigraforutas) — Backtracking (rutas simples con límites)
- [GET /api/seleccion/quicksort](#get-apiseleccionquicksort) — QuickSort
- [GET /api/seleccion/mergesort](#get-apiseleccionmergesort) — MergeSort
- [GET /api/seleccion/greedy](#get-apiselecciongreedy) — Greedy (mochila 0/1 por ratio)
- [GET /api/seleccion/dinamica](#get-apiselecciondinamica) — Mochila 0/1 con programación dinámica
- [GET /api/seleccion/comparar](#get-apiseleccioncomparar) — Greedy vs. PD sobre la misma mochila
- [GET /api/seleccion/repartir](#get-apiseleccionrepartir) — Branch & Bound (reparto en k contenedores)
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

**Origen = destino** (ej. `?origen=RET&destino=RET`): 200 con `{"vertices":["RET"],"costoTotal":0.0}`.
No es un caso especial: `dist[origen] = 0` y la reconstrucción termina enseguida.

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

**Origen = destino:** 200 con `{"vertices":["RET"],"costoTotal":0.0}` (la diagonal vale 0 y
`siguiente[i][i] = i`), igual que Dijkstra.

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

## GET /api/grafo/rutas

**Rutas simples con Backtracking.** En el dominio: **todas las rutas de escape a pie** entre dos
refugios que no tarden más de un tiempo máximo (minutos) ni tengan más de una cantidad máxima de
tramos (paradas), sin pasar dos veces por el mismo lugar. A diferencia de Dijkstra, no busca
**la** más rápida: enumera **todas** las que cumplen las restricciones, para tener alternativas si
una calle se llena de zombies.

Implementación: `Backtracking.rutasSimples` (`algorithm/Backtracking.java`). El grafo se carga
**no dirigido** (`DIRIGIDO = false`).

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `origen` | String | sí | Id del lugar de partida (ej. `RET`). |
| `destino` | String | sí | Id del lugar de llegada (ej. `CUN`). |
| `costoMaximo` | double (≥ 0) | no (default `1000`) | Tiempo máximo de la ruta, en minutos a pie. Se aceptan rutas con costo **≤** costoMaximo. |
| `saltosMaximos` | int (≥ 0) | no (default `10`) | Cantidad máxima de tramos (aristas) de la ruta. |

**Respuesta**

| Campo | Qué es |
|---|---|
| `rutas` | Lista de rutas válidas, en el orden en que las encuentra el backtracking (orden de las listas de adyacencia). Cada una con `vertices` (camino, origen y destino incluidos) y `costoTotal` (minutos). |
| `nodosConPoda` | Nodos del árbol de búsqueda explorados **con** la poda por viabilidad. |
| `nodosSinPoda` | Nodos explorados por la **misma** búsqueda **sin** poda (solo respeta el camino simple y filtra al llegar al destino). |

**Nodo explorado** = cada entrada a la función recursiva `explorar`, raíz incluida. Cada nodo del
árbol de búsqueda es un camino parcial que sale del origen.

**Ejemplo**

```
GET /api/grafo/rutas?origen=RET&destino=CUN&costoMaximo=150&saltosMaximos=4
```

```json
{"rutas":[{"vertices":["RET","PAL","CUN"],"costoTotal":135.0},{"vertices":["RET","PAL","MON","CUN"],"costoTotal":129.0},{"vertices":["RET","REC","PAL","CUN"],"costoTotal":140.0},{"vertices":["RET","REC","PAL","MON","CUN"],"costoTotal":134.0}],"nodosConPoda":118,"nodosSinPoda":684}
```

- 4 rutas de Retiro a Ciudad Universitaria en ≤ 150 minutos y ≤ 4 tramos. La más rápida,
  RET – PAL – MON – CUN = 45 + 66 + 18 = **129**, coincide con `/dijkstra`.
- RET – REC – PAL – CUN = 20 + 30 + 90 = 140. Las rutas que salen por OBE o PMA superan los 150
  minutos o los 4 tramos (ej. RET – OBE – REC – PAL – CUN = 22 + 25 + 30 + 90 = 167).
- La poda redujo la búsqueda de **684 a 118 nodos** (−83 %) y devolvió las mismas 4 rutas.

Con un límite más ajustado (`costoMaximo=130&saltosMaximos=5`) queda una sola ruta,
RET – PAL – MON – CUN (129), con 144 nodos con poda contra 684 sin poda (test
`retiroCiudadUniversitaria130Minutos`). La tabla con todos los casos y una traza a mano están en
[`VERIFICACIONES.md`](VERIFICACIONES.md#6-backtracking-rutas-simples).

**Origen = destino:** una sola ruta `[RET]` de costo 0 y 1 nodo explorado (la raíz ya es el caso
base).

**Errores**

```
GET /api/grafo/rutas?origen=RET&destino=XXX
```

```
400 Bad Request
{"estado":"ERROR","detalle":"No existe el nodo con id: XXX"}
```

| Situación | Código | `detalle` |
|---|---|---|
| `origen` o `destino` inexistente | 400 | `No existe el nodo con id: XXX` |
| `costoMaximo` negativo (ej. `-5`) | 400 | `El costo maximo no puede ser negativo: -5.0` |
| `saltosMaximos` negativo (ej. `-1`) | 400 | `Los saltos maximos no pueden ser negativos: -1` |
| Falta `origen` o `destino` | 400 | `Falta el parametro: destino` |
| Parámetro no numérico (ej. `costoMaximo=abc`) | 400 | `Valor invalido para costoMaximo: abc` |

Si no hay ninguna ruta que cumpla, no es error: devuelve `rutas: []` con los contadores (ej.
RET → CUN con 110 minutos y 3 tramos: 0 rutas, 47 nodos con poda, 684 sin poda).

**Cómo funciona (backtracking de tipo 2).** Es el esquema de la clase 9 de N reinas o Sudoku: cada
llamada recorre **en un bucle las opciones** (los vecinos del lugar actual) y hace una llamada
recursiva por cada opción viable.

1. **Solución parcial:** el camino desde el origen (`camino`) y el arreglo `visitado[]`.
2. **Caso base:** si el lugar actual es el destino, se registra la ruta (una copia del camino) y se
   vuelve. No se sigue de largo: una ruta simple que ya llegó al destino no puede volver a pasar por él.
3. **Opciones:** para cada vecino w no visitado (camino **simple**: no se repiten lugares).
4. **Poda por viabilidad:** si `costo + costo(v, w) > costoMaximo` o `saltos + 1 > saltosMaximos`,
   no se baja a w. Esa rama ya no puede dar ninguna ruta válida, así que se poda **sin completar el
   camino**.
5. **Avanzar:** se marca w, se agrega al camino y se llama recursivamente con el costo y los saltos
   acumulados.
6. **Retroceso (deshacer):** al volver de la llamada se saca w del camino y se **desmarca**
   (`visitado[w] = false`). Desmarcar es lo que distingue al backtracking de un DFS: en DFS un lugar
   visitado queda marcado para siempre; acá w puede volver a usarse **por otro camino** (por ejemplo,
   PAL aparece en las 4 rutas del ejemplo, llegando desde RET o desde REC).

**¿Por qué con y sin poda dan las mismas rutas?** La poda solo corta cuando el costo acumulado ya
supera el máximo o los saltos ya superan el límite. Como los costos son **no negativos** (minutos a
pie) el costo de un camino nunca baja al agregarle tramos, y los saltos solo suben: ningún camino que
pase por un nodo podado puede terminar siendo válido. La corrida sin poda explora esos caminos y los
descarta recién al llegar al destino (`registrarSiCumple`). Por eso la poda **no pierde soluciones**,
solo ahorra trabajo. Lo verifica `conYSinPodaDevuelvenLasMismasRutas`. (Con costos negativos la poda
por costo sería incorrecta; en el dominio no existen.)

**¿Por qué `nodosSinPoda` vale siempre 684 para RET → CUN?** Sin poda la búsqueda no mira los
límites, así que su árbol es siempre el mismo: todos los caminos simples que salen de RET sin pasar
por CUN como intermedio (más los que terminan en CUN). Solo depende del origen, el destino y el grafo.

**¿Por qué con `costoMaximo=1000&saltosMaximos=10` la poda casi no ahorra (682 contra 684)?** Porque
los límites no restringen: el camino simple más largo de RET a CUN tarda 327 minutos (muy por debajo
de 1000), así que la poda por costo nunca se activa. La de saltos solo corta los 2 caminos de 11
tramos, los que recorren los 12 lugares (RET – PMA – LBO – CON – OBE – REC – ABA – PCE – HIT – PAL –
MON – CUN y RET – REC – OBE – PMA – LBO – CON – ABA – PCE – HIT – PAL – MON – CUN): 684 − 2 = 682.
Devuelve 110 rutas. **La poda sirve cuando las restricciones son ajustadas**; con límites holgados
el backtracking tiende a la enumeración completa.

**Complejidad temporal: O(b^L) nodos, O(V!) en el peor caso.**
- L = min(saltosMaximos, V − 1) es la profundidad máxima del árbol (un camino simple tiene a lo sumo
  V − 1 tramos) y b el grado máximo (cantidad de opciones por nodo).
- Recurrencia: cada nodo hace hasta b llamadas con un nivel menos de profundidad y un trabajo O(b)
  afuera (recorrer la lista de vecinos): T(L) = b · T(L − 1) + O(b). Por **sustracción** con a = b > 1
  → O(b^L).
- Peor caso: grafo completo y límites que no podan. Los caminos simples que salen del origen son
  (V−1) + (V−1)(V−2) + … + (V−1)! ≤ e · (V − 1)!, es decir **O(V!)**: crece factorialmente.
- Cada ruta encontrada se copia en O(V); con R rutas, se suma O(R · V).
- Eso es la corrida **con poda**: O(b^L), porque los límites acotan la profundidad.
- La corrida **sin poda** no usa los límites para cortar: recorre todos los caminos simples desde
  el origen, O(b^(V − 1)) (peor caso O(V!)).
- **El endpoint corre las dos búsquedas** (con y sin poda) para poder reportar los dos contadores, así
  que **su costo lo domina la corrida sin poda**. La poda baja el costo de la búsqueda que devuelve
  las rutas; la otra está solo para medir.

**Complejidad espacial: O(V)** (más la salida): `visitado[]` (V), el camino parcial (≤ V) y la pila
de recursión, de profundidad ≤ L + 1 ≤ V. La salida ocupa O(R · V).

**Estructura usada:**
- **Arreglo `boolean[] visitado`**: marcar, consultar y desmarcar en O(1); es lo que hace barato el
  retroceso.
- **`ArrayList<Integer> camino`** usado como pila: agregar y sacar el último en O(1).
- **Pila implícita** (la recursión): cada nivel guarda el lugar actual, el costo y los saltos
  acumulados, que viajan como parámetros (no hay que recalcularlos).
- **Lista de adyacencia**: las opciones de cada nodo son sus vecinos, recorridos en O(grado).
- El estado va en un objeto `Busqueda` por llamada (no en campos del `@Component`, que es singleton),
  así dos requests simultáneos no se pisan.

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

## GET /api/seleccion/repartir

**Reparto de suministros en k contenedores con Branch & Bound (ramificación y poda).** En el
dominio: hay que repartir **todos** los suministros entre k sobrevivientes de modo que **el que más
carga lleve la menor cantidad de kg posible** (nadie queda sobrecargado y el grupo se mueve al ritmo
del más lento). Es el problema de minimizar el **makespan** (la carga máxima) con contenedores
idénticos; es NP-difícil, por eso se resuelve con búsqueda exhaustiva podada.

Implementación: `RamificacionYPoda.repartir` (`algorithm/RamificacionYPoda.java`). El controller lee
los suministros de la base (ordenados por id) y delega.

**Parámetros**

| Nombre | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `contenedores` | int (≥ 1) | sí | k = cantidad de sobrevivientes entre los que se reparte. |

**Respuesta**

| Campo | Qué es |
|---|---|
| `porContenedor` | Mapa `"Sobreviviente 1"` … `"Sobreviviente m"` → suministros que lleva cada uno (objetos completos: `id`, `nombre`, `peso`, `valor`, `nodoId`). m = min(k, n) entradas (ver más abajo). |
| `cargaMaxima` | kg del sobreviviente más cargado en el reparto óptimo (el makespan mínimo). |
| `cotaInicial` | La cota optimista en la raíz: peso total / m. |
| `nodosExplorados` | Entradas a la recursión `ramificar`, raíz incluida. |
| `nodosPodados` | Hijos que **no se abrieron** porque su cota no mejoraba la mejor solución conocida. |

**Ejemplo**

```
GET /api/seleccion/repartir?contenedores=3
```

```json
{"porContenedor":{
  "Sobreviviente 1":[{"id":"S1","nombre":"Botiquin","peso":3.0,"valor":50.0,"nodoId":"HIT"},{"id":"S3","nombre":"Agua 7L","peso":7.0,"valor":55.0,"nodoId":"PCE"},{"id":"S4","nombre":"Linterna","peso":1.0,"valor":8.0,"nodoId":"OBE"}],
  "Sobreviviente 2":[{"id":"S2","nombre":"Comida enlatada","peso":4.0,"valor":40.0,"nodoId":"ABA"},{"id":"S5","nombre":"Radio","peso":2.0,"valor":14.0,"nodoId":"RET"},{"id":"S6","nombre":"Bate","peso":5.0,"valor":10.0,"nodoId":"MON"}],
  "Sobreviviente 3":[{"id":"S7","nombre":"Bolsa de dormir","peso":6.0,"valor":30.0,"nodoId":"CUN"},{"id":"S8","nombre":"Herramientas","peso":5.0,"valor":24.0,"nodoId":"CON"}]},
 "cargaMaxima":11.0,"cotaInicial":11.0,"nodosExplorados":61,"nodosPodados":87}
```

(Respuesta real, con saltos de línea agregados para leerla.) Cargas: 3 + 7 + 1 = 11, 4 + 2 + 5 = 11 y
6 + 5 = 11. El total es 33 kg y 33 / 3 = 11, así que el reparto es **perfecto**: alcanza la cota
optimista, y por eso es óptimo sin necesidad de mirar nada más. Se exploraron **61 nodos** y se
podaron **87 hijos**; el árbol completo sin poda tendría 9841 nodos.

```
GET /api/seleccion/repartir?contenedores=2
```

Sobreviviente 1 = S1, S2, S3, S4, S5 (3 + 4 + 7 + 1 + 2 = **17 kg**) y Sobreviviente 2 = S6, S7, S8
(5 + 6 + 5 = 16 kg). `"cargaMaxima":17.0,"cotaInicial":16.5,"nodosExplorados":84,"nodosPodados":74`.
La cota es 16,5 pero con pesos enteros no se puede bajar de 17: la cota es optimista, no siempre
alcanzable.

Con `contenedores=1000000` responde en unos 0,04 s con 8 sobrevivientes (m = min(k, n) = 8):
Sobreviviente 1 = S1+S2 (7 kg), S3, S4+S5, S6, S7 y S8 en los siguientes, y los Sobrevivientes 7 y 8
vacíos. `cargaMaxima` 7.0 (no puede ser menor: el Agua 7L pesa 7), 50 explorados y 72 podados. B&B se
queda con el **primer** reparto que alcanza 7 y poda el resto, porque la poda es con `>=`: no busca
otro reparto igual de bueno. La tabla por k y una
traza a mano están en [`VERIFICACIONES.md`](VERIFICACIONES.md#7-branch--bound-reparto-en-k-contenedores).

**Errores**

```
GET /api/seleccion/repartir?contenedores=0
```

```
400 Bad Request
{"estado":"ERROR","detalle":"La cantidad de contenedores debe ser al menos 1: 0"}
```

| Situación | Código | `detalle` |
|---|---|---|
| `contenedores` < 1 | 400 | `La cantidad de contenedores debe ser al menos 1: 0` |
| Falta `contenedores` | 400 | `Falta el parametro: contenedores` |
| `contenedores` no entero (ej. `abc`) | 400 | `Valor invalido para contenedores: abc` |
| Algún suministro con peso negativo o no numérico | 400 | `Peso invalido (negativo o no numerico) en: ...` |
| No existe ningún reparto factible | 422 | `No hay un reparto factible` |

El 422 no puede ocurrir con el problema actual (sin capacidad por sobreviviente, todo reparto es
factible): queda preparado para el caso en que se agregue una restricción de capacidad máxima por
sobreviviente y ninguna asignación la cumpla.

**Cómo funciona.** Branch & Bound es **backtracking más una cota** (clase 12). Recorre en profundidad
(DFS recursivo) un árbol donde el nivel i decide a qué sobreviviente va el suministro i (en orden de
id).

- **Ramificar:** el suministro i puede ir a cualquiera de los contenedores; cada opción es un hijo.
- **Acotar:** para cada hijo se calcula una **cota inferior optimista** del makespan que se podría
  alcanzar completando ese reparto parcial:

  ```
  cota = max( carga máxima después de ubicar el ítem i , peso total / m )
  ```

- **Podar:** si `cota >= mejor` (la mejor carga máxima encontrada hasta ahora), el hijo **no se abre**
  y se cuenta en `nodosPodados`. `mejor` arranca en +∞ (no hay solución inicial), así que la primera
  rama baja hasta una hoja sin podas.
- **Hoja** (todos los ítems ubicados): si su carga máxima es menor que `mejor`, se guarda como nueva
  mejor solución (copia de la asignación).
- **Retroceso:** al volver del hijo se resta el peso del contenedor (`cargas[c] -= pesos[i]`).

**La cota y por qué es optimista** (lo que pide el hito 11). Es una cota **inferior**: ningún reparto
completo que se arme desde ese nodo puede tener una carga máxima menor. Los dos términos lo garantizan:
1. **Carga máxima actual tras ubicar el ítem:** los pesos son no negativos, así que las cargas solo
   crecen al agregar ítems. El contenedor más cargado hoy va a pesar **al menos** eso al final.
2. **Peso total / m:** reparta como se reparta, los m contenedores suman el total, así que el más
   cargado lleva **al menos el promedio**. Si k ≥ n (m = n), total / n ≤ peso del ítem más pesado ≤
   óptimo, así que sigue siendo una cota válida.

Como la cota **nunca sobreestima** el mejor valor alcanzable, si ya es ≥ `mejor`, ningún reparto de
esa rama puede mejorar a la solución conocida, y descartarla **no pierde el óptimo**. Una cota que
sobreestimara podría podar la rama del óptimo. Lo comprueba `optimoCoincideConFuerzaBruta`
(k = 2, 3, 4 contra los k^n repartos).

**Poda por optimalidad vs. poda por viabilidad.** El backtracking de `/rutas` poda por
**viabilidad**: corta una rama porque ya **no puede dar una solución válida** (se pasó del tiempo o de
los tramos). Acá todas las ramas dan repartos válidos; se poda por **optimalidad**: la rama podría dar
una solución, pero **no una mejor** que la que ya se tiene. Para eso hace falta la cota y la mejor
solución conocida, que va bajando: cuanto antes baja `mejor`, más se poda.

Cuando `mejor` llega a la `cotaInicial` (k = 3: `mejor` = 11 = 33 / 3), **toda** rama tiene cota
≥ 11 = `mejor` y se poda de inmediato: la búsqueda termina sola porque se probó que no se puede
mejorar.

**Ruptura de simetría.** Los contenedores son **idénticos**: el reparto {S1, S3, S4} / {S2, S5, S6} /
{S7, S8} es el mismo con los nombres de los sobrevivientes permutados, y sin cuidado aparecería k!
veces. Por eso un contenedor vacío solo se prueba si es el **primero vacío**: el ítem i puede ir a
los contenedores ya usados o al siguiente (`limite = min(m, usados + 1)`). Por ejemplo, el primer
ítem va siempre al Sobreviviente 1. Solo con esto (sin cota) el árbol de k = 3 baja de 9841 a 1645
nodos; con la cota, a 61.

**Solo m = min(k, n) contenedores.** Con pesos no negativos, un reparto óptimo nunca necesita más
contenedores que ítems (los que sobran quedan vacíos y no cambian la carga máxima). Por eso con
k = 1.000.000 no se reserva un arreglo de un millón de cargas ni un mapa de un millón de entradas:
la respuesta tiene 8 sobrevivientes.

Tanto la ruptura de simetría como el recorte a min(k, n) **valen solo porque los contenedores son
idénticos**. Si cada sobreviviente tuviera una capacidad distinta, cambiar los nombres sí cambiaría
la solución y habría que probar todos los contenedores.

**Complejidad temporal: O(m^n) nodos en el peor caso**, con m = min(k, n).
- Recurrencia: cada nodo abre hasta m hijos con un ítem menos por ubicar, y fuera de las llamadas
  hace O(m) (evaluar la cota de cada hijo en O(1)): T(n) = m · T(n − 1) + O(m). Por **sustracción**
  con a = m > 1 → **O(m^n)**.
- Sin poda ni simetría el árbol completo tiene (m^(n+1) − 1) / (m − 1) nodos: con n = 8, **511**
  (k = 2), **9841** (k = 3) y **87381** (k = 4). Con k = 1 es un solo camino: n + 1 = 9 nodos.
- Cada nodo cuesta O(1) por hijo evaluado: la carga máxima viaja como parámetro y no se recalcula.
  Cada mejora copia la asignación en O(n) (pocas veces: en k = 3, 9 mejoras).
- La poda no cambia el peor caso (es exponencial), pero en la práctica el árbol se reduce muchísimo:
  61 nodos de 9841 en k = 3.

**Complejidad espacial: O(n)** (más la salida): arreglos `pesos[]`, `asignacion[]` y
`mejorAsignacion[]` (n), `cargas[]` (m ≤ n) y la pila de recursión, de profundidad n + 1. Sin el
recorte a min(k, n) sería O(n + k). La salida ocupa O(n + m).

**Estructura usada:**
- **Arreglo `double[] cargas`** (uno por contenedor): sumar y restar un peso en O(1), que es lo que
  hace O(1) ramificar y retroceder.
- **Arreglos `int[] asignacion` y `mejorAsignacion`**: la solución parcial (ítem → contenedor) y la
  mejor encontrada; se copia con `clone()` solo al mejorar.
- **Pila implícita** (recursión DFS): explorar en profundidad llega rápido a hojas y baja `mejor`
  temprano, que es lo que hace efectiva la poda; con memoria O(n), a diferencia de un B&B por
  mejor-primero con cola de prioridad, que puede guardar muchos nodos abiertos.
- **`LinkedHashMap`** para la respuesta: los sobrevivientes salen en orden (1, 2, 3, …).
- El estado va en un objeto `Busqueda` por llamada, porque el `@Component` es singleton.

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

Endpoints cableados cuyo algoritmo no está implementado (responden 501). UCS es **opcional** en la
consigna y no suma punto: no se implementa.

| Método y ruta | Parámetros | Algoritmo |
|---|---|---|
| `GET /api/grafo/ucs` | `origen` (String), `destino` (String) | UCS (opcional) |
