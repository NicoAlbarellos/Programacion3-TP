# Documentación de endpoints

Una ficha por endpoint: qué responde en el dominio (apocalipsis zombie en CABA), parámetros,
ejemplo **real** de request y respuesta, complejidad justificada y estructura de datos usada.

Notación: **V** = cantidad de lugares (vértices), **E** = cantidad de calles (aristas). En nuestro
grafo V = 12 y E = 21. El costo de una arista son **minutos a pie**.

Los ejemplos se obtuvieron corriendo la aplicación contra Neo4j con la carga inicial
(`seed/CargaInicial.java`).

## Índice

- [GET /api/grafo/resumen](#get-apigraforesumen)
- [GET /api/grafo/bfs](#get-apigrafobfs) — BFS
- [GET /api/grafo/dfs](#get-apigrafodfs) — DFS
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

## Errores

Los errores se devuelven como JSON desde `config/ManejadorDeErrores`, nunca como stack trace.

| Situación | Código | Cuerpo |
|---|---|---|
| Id de lugar inexistente (`IllegalArgumentException`) | 400 | `{"estado":"ERROR","detalle":"..."}` |
| Estado inválido (`IllegalStateException`) | 422 | `{"estado":"ERROR","detalle":"..."}` |
| Algoritmo todavía no implementado | 501 | `{"estado":"PENDIENTE","detalle":"..."}` |

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
| `GET /api/grafo/dijkstra` | `origen` (String), `destino` (String) | Dijkstra |
| `GET /api/grafo/floyd` | — | Floyd-Warshall |
| `GET /api/grafo/ucs` | `origen` (String), `destino` (String) | UCS (opcional) |
| `GET /api/grafo/prim` | `origen` (String) | Prim |
| `GET /api/grafo/kruskal` | — | Kruskal |
| `GET /api/grafo/rutas` | `origen` (String), `destino` (String), `costoMaximo` (double, default 1000), `saltosMaximos` (int, default 10) | Backtracking |
| `GET /api/seleccion/quicksort` | `criterio` (`valor` \| `peso` \| `ratio`, default `ratio`) | QuickSort |
| `GET /api/seleccion/mergesort` | `criterio` (`valor` \| `peso` \| `ratio`, default `ratio`) | MergeSort |
| `GET /api/seleccion/greedy` | `capacidad` (double) | Greedy |
| `GET /api/seleccion/dinamica` | `capacidad` (int) | Mochila 0/1 con PD |
| `GET /api/seleccion/repartir` | `contenedores` (int, ≥ 1) | Branch & Bound |
