# Verificaciones a mano

Evidencia que piden los hitos: los resultados de los algoritmos **calculados a mano sobre el grafo
del grupo** y comparados con la salida real de los endpoints y con los tests.

Grafo: 12 lugares de CABA, 21 calles no dirigidas, costo en **minutos a pie** (tabla y diagrama en
el [README](../README.md#calles-21-aristas-costo-en-minutos-a-pie)).

Notación de las tablas: `d/p` = distancia (o key) / vértice previo (o padre). `∞` = todavía no
alcanzado. En **negrita**, los vértices ya extraídos (su valor es definitivo).

## Índice

1. [Dijkstra desde PAL](#1-dijkstra-desde-pal)
2. [Kruskal](#2-kruskal)
3. [Prim desde RET](#3-prim-desde-ret)
4. [Greedy vs. mochila 0/1 con PD](#4-greedy-vs-mochila-01-con-pd)
5. [Floyd-Warshall](#5-floyd-warshall)
6. [Backtracking: rutas simples](#6-backtracking-rutas-simples)
7. [Branch & Bound: reparto en k contenedores](#7-branch--bound-reparto-en-k-contenedores)
8. [UCS: Dijkstra con parada temprana](#8-ucs-dijkstra-con-parada-temprana)
9. [Lugares infectados (Dijkstra y Backtracking)](#9-lugares-infectados-dijkstra-y-backtracking)

---

## 1. Dijkstra desde PAL

Consulta: `GET /api/grafo/dijkstra?origen=PAL&destino=CUN`. Interesa porque existe la calle
directa PAL – CUN de 90 minutos, pero la ruta más rápida es por el Monumental.

Inicialización: dist[PAL] = 0, el resto ∞; previo[] = — para todos; cola = {(PAL, 0)}.

En cada paso se extrae de la cola el vértice no procesado con menor dist[] y se **relaja** cada
calle (u, v): si dist[u] + costo < dist[v], se actualiza dist[v], previo[v] = u y se encola
(v, dist[v]). Los vecinos se miran en el orden de la lista de adyacencia.

### Relajaciones de cada paso

| Paso | Se extrae (dist) | Relajaciones exitosas | Relajaciones que no mejoran |
|---|---|---|---|
| 1 | PAL (0) | HIT 0+21=21, CUN 0+90=90, MON 0+66=66, REC 0+30=30, RET 0+45=45 | — |
| 2 | HIT (21) | ABA 21+19=40, PCE 21+18=39 | PAL ya procesado |
| 3 | REC (30) | OBE 30+25=55 | ABA 30+29=59 ≥ 40; RET 30+20=50 ≥ 45 |
| 4 | PCE (39) | — | ABA 39+30=69 ≥ 40 |
| 5 | ABA (40) | CON 40+36=76 | OBE 40+28=68 ≥ 55 |
| 6 | RET (45) | PMA 45+18=63 | OBE 45+22=67 ≥ 55 |
| 7 | OBE (55) | — | CON 55+31=86 ≥ 76; PMA 55+20=75 ≥ 63 |
| 8 | PMA (63) | LBO 63+36=99 | CON 63+30=93 ≥ 76 |
| 9 | MON (66) | **CUN 66+18=84 < 90** | — |
| 10 | CON (76) | — | LBO 76+29=105 ≥ 99 |
| 11 | CUN (84) | — | — |
| — | (CUN, 90) | entrada vieja: 90 > dist[CUN] = 84, **se descarta** | |
| 12 | LBO (99) | — | — |

(En "no mejoran" se omiten los vecinos ya procesados, que nunca mejoran con costos ≥ 0.)

### Estado de dist[] / previo[] después de cada paso

| Paso | Extrae | HIT | REC | PCE | ABA | RET | OBE | PMA | MON | CON | CUN | LBO |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 | — | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ |
| 1 | PAL | 21/PAL | 30/PAL | ∞ | ∞ | 45/PAL | ∞ | ∞ | 66/PAL | ∞ | 90/PAL | ∞ |
| 2 | HIT | **21/PAL** | 30/PAL | 39/HIT | 40/HIT | 45/PAL | ∞ | ∞ | 66/PAL | ∞ | 90/PAL | ∞ |
| 3 | REC | **21/PAL** | **30/PAL** | 39/HIT | 40/HIT | 45/PAL | 55/REC | ∞ | 66/PAL | ∞ | 90/PAL | ∞ |
| 4 | PCE | **21/PAL** | **30/PAL** | **39/HIT** | 40/HIT | 45/PAL | 55/REC | ∞ | 66/PAL | ∞ | 90/PAL | ∞ |
| 5 | ABA | **21/PAL** | **30/PAL** | **39/HIT** | **40/HIT** | 45/PAL | 55/REC | ∞ | 66/PAL | 76/ABA | 90/PAL | ∞ |
| 6 | RET | **21/PAL** | **30/PAL** | **39/HIT** | **40/HIT** | **45/PAL** | 55/REC | 63/RET | 66/PAL | 76/ABA | 90/PAL | ∞ |
| 7 | OBE | **21/PAL** | **30/PAL** | **39/HIT** | **40/HIT** | **45/PAL** | **55/REC** | 63/RET | 66/PAL | 76/ABA | 90/PAL | ∞ |
| 8 | PMA | **21/PAL** | **30/PAL** | **39/HIT** | **40/HIT** | **45/PAL** | **55/REC** | **63/RET** | 66/PAL | 76/ABA | 90/PAL | 99/PMA |
| 9 | MON | **21/PAL** | **30/PAL** | **39/HIT** | **40/HIT** | **45/PAL** | **55/REC** | **63/RET** | **66/PAL** | 76/ABA | 84/MON | 99/PMA |
| 10 | CON | **21/PAL** | **30/PAL** | **39/HIT** | **40/HIT** | **45/PAL** | **55/REC** | **63/RET** | **66/PAL** | **76/ABA** | 84/MON | 99/PMA |
| 11 | CUN | **21/PAL** | **30/PAL** | **39/HIT** | **40/HIT** | **45/PAL** | **55/REC** | **63/RET** | **66/PAL** | **76/ABA** | **84/MON** | 99/PMA |
| 12 | LBO | **21/PAL** | **30/PAL** | **39/HIT** | **40/HIT** | **45/PAL** | **55/REC** | **63/RET** | **66/PAL** | **76/ABA** | **84/MON** | **99/PMA** |

dist[PAL] = 0 durante todo el algoritmo (es el origen).

### Reconstrucción del camino PAL → CUN

Se sigue previo[] desde el destino: previo[CUN] = MON, previo[MON] = PAL, previo[PAL] = —.
Camino: **PAL → MON → CUN, 66 + 18 = 84 minutos**.

La calle directa PAL – CUN (90) dejó dist[CUN] = 90 en el paso 1; la relajación de MON – CUN en el
paso 9 la mejora a 84. Ese es el punto de esta verificación: si el algoritmo devolviera 90, no
estaría relajando bien.

### Comparación con la salida real

```
GET /api/grafo/dijkstra?origen=PAL&destino=CUN
{"vertices":["PAL","MON","CUN"],"costoTotal":84.0}
```

Coincide con el cálculo a mano. Lo verifica también
`CaminosMinimosTest.relajacionEvitaElDirectoPalermoCiudadUniversitaria`.

Conteo de la cola: 13 inserciones (el origen + 12 relajaciones exitosas) y 13 extracciones
(12 vértices procesados + 1 entrada vieja descartada), consistente con "a lo sumo E + 1 entradas".

Con la misma tabla se lee el resto de los tiempos mínimos desde PAL, por ejemplo PAL → LBO = 99
(PAL → RET → PMA → LBO). Para RET → CUN el endpoint devuelve RET → PAL → MON → CUN = 45 + 66 + 18
= 129 (por la calle directa PAL – CUN serían 135), también cubierto por
`CaminosMinimosTest.retiroACiudadUniversitaria`.

---

## 2. Kruskal

Consulta: `GET /api/grafo/kruskal`.

**Paso 1: ordenar las 21 calles por costo creciente** (MergeSort, estable: entre empates queda el
orden en que se listan las calles, por id de sus extremos).

**Paso 2: recorrerlas en orden.** Para cada calle (u, v): si find(u) ≠ find(v), están en
componentes distintas y se **acepta** (union(u, v)); si find(u) = find(v), ya están conectados y la
calle **cerraría un ciclo**: se **rechaza**. Se corta al llegar a V − 1 = 11 aristas.

| # | Calle | Costo | find(u) = find(v)? | Decisión | Componente resultante / ciclo que cerraría | Aristas | Total |
|---|---|---|---|---|---|---|---|
| 1 | CUN – MON | 18 | no | Acepta | {CUN, MON} | 1 | 18 |
| 2 | HIT – PCE | 18 | no | Acepta | {HIT, PCE} | 2 | 36 |
| 3 | PMA – RET | 18 | no | Acepta | {PMA, RET} | 3 | 54 |
| 4 | ABA – HIT | 19 | no | Acepta | {ABA, HIT, PCE} | 4 | 73 |
| 5 | OBE – PMA | 20 | no | Acepta | {OBE, PMA, RET} | 5 | 93 |
| 6 | REC – RET | 20 | no | Acepta | {OBE, PMA, REC, RET} | 6 | 113 |
| 7 | HIT – PAL | 21 | no | Acepta | {ABA, HIT, PAL, PCE} | 7 | 134 |
| 8 | OBE – RET | 22 | **sí** | Rechaza | OBE – PMA – RET | 7 | 134 |
| 9 | OBE – REC | 25 | **sí** | Rechaza | OBE – PMA – RET – REC | 7 | 134 |
| 10 | ABA – OBE | 28 | no | Acepta | {ABA, HIT, OBE, PAL, PCE, PMA, REC, RET} | 8 | 162 |
| 11 | ABA – REC | 29 | **sí** | Rechaza | ABA – OBE – PMA – RET – REC | 8 | 162 |
| 12 | CON – LBO | 29 | no | Acepta | {CON, LBO} | 9 | 191 |
| 13 | ABA – PCE | 30 | **sí** | Rechaza | ABA – HIT – PCE | 9 | 191 |
| 14 | CON – PMA | 30 | no | Acepta | {ABA, CON, HIT, LBO, OBE, PAL, PCE, PMA, REC, RET} | 10 | 221 |
| 15 | PAL – REC | 30 | **sí** | Rechaza | PAL – HIT – ABA – OBE – PMA – RET – REC | 10 | 221 |
| 16 | CON – OBE | 31 | **sí** | Rechaza | CON – PMA – OBE | 10 | 221 |
| 17 | ABA – CON | 36 | **sí** | Rechaza | ABA – OBE – PMA – CON | 10 | 221 |
| 18 | LBO – PMA | 36 | **sí** | Rechaza | LBO – CON – PMA | 10 | 221 |
| 19 | PAL – RET | 45 | **sí** | Rechaza | PAL – HIT – ABA – OBE – PMA – RET | 10 | 221 |
| 20 | MON – PAL | 66 | no | Acepta | los 12 lugares | **11** | **287** |
| 21 | CUN – PAL | 90 | — | No se evalúa: ya hay 11 aristas | | 11 | 287 |

**Resultado: 11 aristas, 287 minutos.** Se aceptaron 11 calles y se rechazaron 9 por formar ciclo;
la última (CUN – PAL, 90) ni se mira porque el árbol ya está completo.

Comparación con la salida real: el endpoint devuelve exactamente estas 11 calles, en este orden
(CUN-MON, HIT-PCE, PMA-RET, ABA-HIT, OBE-PMA, REC-RET, HIT-PAL, ABA-OBE, CON-LBO, CON-PMA,
MON-PAL) con `costoTotal` 287.0. Lo verifica `ArbolRecubrimientoTest.kruskalDominio`.

Los empates (tres calles de 18, dos de 20, tres de 30) no cambian el resultado: el MST de este
grafo es único (cada calle rechazada es estrictamente más cara que todas las del ciclo que
cerraría, por ejemplo ABA – REC = 29 contra un máximo de 28 en ABA – OBE – PMA – RET – REC).

---

## 3. Prim desde RET

Consulta: `GET /api/grafo/prim?origen=RET`.

Inicialización: key[RET] = 0, el resto ∞; parent[] = —; cola = {(RET, 0)}.

En cada paso entra al árbol el vértice de fuera con **menor key** (la calle más barata que lo une
al árbol). Después, para cada vecino v que no está en el árbol, si costo(u, v) < key[v] se
actualiza key[v], parent[v] = u y se encola (v, key[v]). Las entradas de vértices que ya están en
el árbol se descartan al extraerlas (eliminación perezosa).

| Paso | Entra | Por la calle | key | Total | Actualizaciones de key (v: antes → después) | Entradas viejas descartadas antes de este paso |
|---|---|---|---|---|---|---|
| 1 | RET | — (origen) | 0 | 0 | OBE ∞→22, PAL ∞→45, PMA ∞→18, REC ∞→20 | — |
| 2 | PMA | RET – PMA | 18 | 18 | OBE 22→20, CON ∞→30, LBO ∞→36 | — |
| 3 | REC | RET – REC | 20 | 38 | ABA ∞→29, PAL 45→30 | — |
| 4 | OBE | PMA – OBE | 20 | 58 | ABA 29→28 (CON 31 no mejora 30) | — |
| 5 | ABA | OBE – ABA | 28 | 86 | HIT ∞→19, PCE ∞→30 (CON 36 no mejora 30) | (OBE, 22) |
| 6 | HIT | ABA – HIT | 19 | 105 | PAL 30→21, PCE 30→18 | — |
| 7 | PCE | HIT – PCE | 18 | 123 | — | — |
| 8 | PAL | HIT – PAL | 21 | 144 | CUN ∞→90, MON ∞→66 | — |
| 9 | CON | PMA – CON | 30 | 174 | LBO 36→29 | (ABA, 29) |
| 10 | LBO | CON – LBO | 29 | 203 | — | — |
| 11 | MON | PAL – MON | 66 | 269 | CUN 90→18 | (PAL, 30), (PCE, 30), (LBO, 36), (PAL, 45) |
| 12 | CUN | MON – CUN | 18 | **287** | — | — |
| — | | | | | | (CUN, 90): la cola queda vacía |

**Resultado: 11 aristas, 287 minutos.** Orden en que entran los vértices: RET, PMA, REC, OBE, ABA,
HIT, PCE, PAL, CON, LBO, MON, CUN.

Observaciones:
- **Paso 3, empate:** REC (key 20 por RET – REC) y OBE (key 20 por PMA – OBE) empatan. Cualquiera
  de los dos órdenes es correcto y lleva al mismo árbol; la `PriorityQueue` extrajo primero REC,
  igual que en la salida real.
- **Prim no es "el vecino más cercano del último que entró":** en el paso 5 entra ABA (28) aunque
  el último en entrar fue OBE, y en el paso 9 entra CON colgando de PMA (que entró en el paso 2).
  Siempre se toma la calle más barata que cruza del árbol al resto, salga de donde salga.
- **Paso 3, actualización PAL 45 → 30:** por la key, PAL deja de depender de RET – PAL (45) y
  después, en el paso 6, baja a 21 por HIT – PAL. Por eso RET – PAL no queda en el árbol.
- Conteo de la cola: 19 inserciones (origen + 18 actualizaciones) = 12 vértices que entran + 7
  entradas viejas descartadas.

Comparación con la salida real: el endpoint devuelve las aristas en este mismo orden (RET→PMA,
RET→REC, PMA→OBE, OBE→ABA, ABA→HIT, HIT→PCE, HIT→PAL, PMA→CON, CON→LBO, PAL→MON, MON→CUN) con
`costoTotal` 287.0. Lo verifica `ArbolRecubrimientoTest.primDominioDesdeRetiro`.

**Prim y Kruskal dan el mismo árbol:** las 11 calles de la tabla de Prim son las mismas 11 que
aceptó Kruskal (en otro orden), como corresponde a un MST único. El test
`primDominioDaElMismoArbolDesdeOtroOrigen` comprueba además que arrancando desde CUN se obtiene el
mismo conjunto de calles.

---

## 4. Greedy vs. mochila 0/1 con PD

Lo pide el hito 6: **un juego de datos donde el greedy NO da el óptimo** y la mochila 0/1 con PD sí.
Datos: los 8 suministros del dominio (README). Consultas:
`GET /api/seleccion/greedy?capacidad=12`, `/dinamica?capacidad=12` y `/comparar?capacidad=12`.

### 4.1 Ratios utilidad/peso, ordenados de mayor a menor

| Orden | Id | Suministro | kg | Utilidad | Ratio |
|---|---|---|---|---|---|
| 1 | S1 | Botiquín | 3 | 50 | 16,67 |
| 2 | S2 | Comida enlatada | 4 | 40 | 10,00 |
| 3 | S4 | Linterna | 1 | 8 | 8,00 |
| 4 | S3 | Agua 7L | 7 | 55 | 7,86 |
| 5 | S5 | Radio | 2 | 14 | 7,00 |
| 6 | S7 | Bolsa de dormir | 6 | 30 | 5,00 |
| 7 | S8 | Herramientas | 5 | 24 | 4,80 |
| 8 | S6 | Bate | 5 | 10 | 2,00 |

Es el mismo orden que devuelve `GET /api/seleccion/mergesort?criterio=ratio&orden=desc`.

### 4.2 Capacidad 12: traza del greedy y solución óptima

**Greedy por ratio** (si entra se toma; si no entra se **saltea** y se sigue):

| Paso | Suministro | kg | ¿Entra en lo que queda? | Decisión | kg usados | Libres | Utilidad acumulada |
|---|---|---|---|---|---|---|---|
| 1 | S1 | 3 | 3 ≤ 12 | Toma | 3 | 9 | 50 |
| 2 | S2 | 4 | 4 ≤ 9 | Toma | 7 | 5 | 90 |
| 3 | S4 | 1 | 1 ≤ 5 | Toma | 8 | 4 | 98 |
| 4 | S3 | 7 | 7 > 4 | **Saltea** | 8 | 4 | 98 |
| 5 | S5 | 2 | 2 ≤ 4 | Toma | 10 | 2 | 112 |
| 6 | S7 | 6 | 6 > 2 | Saltea | 10 | 2 | 112 |
| 7 | S8 | 5 | 5 > 2 | Saltea | 10 | 2 | 112 |
| 8 | S6 | 5 | 5 > 2 | Saltea | 10 | 2 | 112 |

**Greedy = {S1, S2, S4, S5}: 112 de utilidad, 10 kg.** (Si cortara en el paso 4 en vez de saltear,
daría 98.)

**Óptimo (PD) = {S1, S3, S5}: 50 + 55 + 14 = 119 de utilidad, 3 + 7 + 2 = 12 kg.**

Salida real (resumida):

```
GET /api/seleccion/comparar?capacidad=12
greedy:   S1, S2, S4, S5   valorTotal 112.0   pesoTotal 10.0
dinamica: S1, S3, S5       valorTotal 119.0   pesoTotal 12.0
```

**Por qué el greedy pierde.** Los dos toman S1 y S5. La diferencia es qué hacen con los 7 kg
restantes: el greedy, mirando solo el ratio, los llena con S2 + S4 (5 kg, 48 de utilidad) y después
**ya no le entra** S3 (7 kg) en los 4 kg libres, así que terminan sobrando 2 kg. El óptimo usa esos
7 kg enteros con S3 (55 de utilidad), aunque su ratio (7,86) sea menor que el de S2 y S4. Con ítems
indivisibles, la elección localmente mejor (mayor ratio) puede dejar capacidad que después no se
aprovecha, y el greedy no vuelve atrás para corregirlo. La PD, en cambio, evalúa para cada
capacidad j las dos opciones (tomar o no cada ítem) y se queda con la mejor.

Comparación con la **mochila fraccional** (clase 3): con 12 kg tomaría S1, S2, S4 (8 kg, 98) y 4/7
de S3 (31,43): 129,43. Ahí el greedy por ratio es óptimo, y ese valor es una **cota superior** del
óptimo 0/1 (119 ≤ 129,43).

Lo verifica `SeleccionTest.capacidadDoceGreedyNoEsOptimo`.

**Tabla dp del dominio con capacidad 12** (ítems en el orden de entrada, por id; fila i = "usando
S1..Si"):

| i (se agrega) | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 (ninguno) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 1 (S1 3/50) | 0 | 0 | 0 | 50 | 50 | 50 | 50 | 50 | 50 | 50 | 50 | 50 | 50 |
| 2 (S2 4/40) | 0 | 0 | 0 | 50 | 50 | 50 | 50 | 90 | 90 | 90 | 90 | 90 | 90 |
| 3 (S3 7/55) | 0 | 0 | 0 | 50 | 50 | 50 | 50 | 90 | 90 | 90 | 105 | 105 | 105 |
| 4 (S4 1/8) | 0 | 8 | 8 | 50 | 58 | 58 | 58 | 90 | 98 | 98 | 105 | 113 | 113 |
| 5 (S5 2/14) | 0 | 8 | 14 | 50 | 58 | 64 | 72 | 90 | 98 | 104 | 112 | 113 | **119** |
| 6 (S6 5/10) | 0 | 8 | 14 | 50 | 58 | 64 | 72 | 90 | 98 | 104 | 112 | 113 | 119 |
| 7 (S7 6/30) | 0 | 8 | 14 | 50 | 58 | 64 | 72 | 90 | 98 | 104 | 112 | 113 | 119 |
| 8 (S8 5/24) | 0 | 8 | 14 | 50 | 58 | 64 | 72 | 90 | 98 | 104 | 112 | 113 | **119** |

Celda clave: dp[5][12] = max(dp[4][12], dp[4][10] + 14) = max(113, 105 + 14) = 119.

Recuperación desde dp[8][12] = 119:

| i | j | dp[i][j] | dp[i−1][j] | ¿Distintos? | Decisión | j siguiente |
|---|---|---|---|---|---|---|
| 8 (S8) | 12 | 119 | 119 | no | no se toma | 12 |
| 7 (S7) | 12 | 119 | 119 | no | no se toma | 12 |
| 6 (S6) | 12 | 119 | 119 | no | no se toma | 12 |
| 5 (S5) | 12 | 119 | 113 | **sí** | **toma S5** | 12 − 2 = 10 |
| 4 (S4) | 10 | 105 | 105 | no | no se toma | 10 |
| 3 (S3) | 10 | 105 | 90 | **sí** | **toma S3** | 10 − 7 = 3 |
| 2 (S2) | 3 | 50 | 50 | no | no se toma | 3 |
| 1 (S1) | 3 | 50 | 0 | **sí** | **toma S1** | 3 − 3 = 0 |

Resultado: {S1, S3, S5}, 119, igual que el endpoint.

### 4.3 Tabla dp a mano: ejemplo de la clase 6

Ejemplo chico para dibujar en el pizarrón: fichajes con presupuesto 8 (costo = peso,
rendimiento = valor). J1 5/60, J2 3/50, J3 4/70.

dp[i][j] = mejor rendimiento con los jugadores 1..i y presupuesto j.

| i (se agrega) | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|---|
| 0 (ninguno) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 1 (J1 5/60) | 0 | 0 | 0 | 0 | 0 | 60 | 60 | 60 | 60 |
| 2 (J2 3/50) | 0 | 0 | 0 | 50 | 50 | 60 | 60 | 60 | 110 |
| 3 (J3 4/70) | 0 | 0 | 0 | 50 | 70 | 70 | 70 | 120 | **120** |

Cómo se llenan algunas celdas:
- Fila 1: J1 cuesta 5; hasta j = 4 no entra (0), desde j = 5 vale 60.
- dp[2][3] = max(dp[1][3], dp[1][0] + 50) = max(0, 50) = 50: conviene J2 solo.
- dp[2][8] = max(dp[1][8], dp[1][5] + 50) = max(60, 110) = 110: J1 + J2.
- dp[3][4] = max(dp[2][4], dp[2][0] + 70) = max(50, 70) = 70.
- dp[3][7] = max(dp[2][7], dp[2][3] + 70) = max(60, 120) = 120.
- dp[3][8] = max(dp[2][8], dp[2][4] + 70) = max(110, 120) = 120.

Recuperación desde dp[3][8] = 120:

| i | j | dp[i][j] | dp[i−1][j] | Decisión | j siguiente |
|---|---|---|---|---|---|
| 3 (J3) | 8 | 120 | 110 | distintos → **toma J3** | 8 − 4 = 4 |
| 2 (J2) | 4 | 50 | 0 | distintos → **toma J2** | 4 − 3 = 1 |
| 1 (J1) | 1 | 0 | 0 | iguales → no se toma | — |

**Resultado: {J2, J3}, rendimiento 120, costo 7.** Coincide con la diapositiva y con
`SeleccionTest.fichajesClaseSeisCasoDondeAcierta`. Acá el greedy por ratio (J3 17,5 > J2 16,7 >
J1 12) también da 120: es el "caso donde acierta".

**Nota sobre el "caso donde falla" de la diapositiva** (J1 5/60, J2 3/50, J3 6/90, presupuesto 8).
La diapositiva dice "greedy por ratio" y toma primero J3 (ratio 15,0) obteniendo 90, pero J2 tiene
ratio 16,7 > 15,0. Ordenando bien por ratio (J2 16,7 > J3 15,0 > J1 12,0): toma J2 (3), J3 (6) no
entra en los 5 libres y se saltea, J1 (5) entra → **110**, que es el óptimo. El 90 de la diapositiva
es lo que da el greedy **por valor** (J3 90 primero; después ni J1 ni J2 entran en los 2 libres). Lo
verifica `SeleccionTest.fichajesClaseSeisCasoDondeFallaSegunLaDiapositiva`: por valor 90 {J3}, por
ratio 110 {J1, J2}. Por eso nuestro contraejemplo para el greedy por ratio es el de la sección 4.2.

### 4.4 Resumen greedy vs. PD por capacidad

Suministros del dominio, greedy por ratio descendente. Valores de `SeleccionTest`.

| Capacidad (kg) | Greedy | Elegidos greedy | PD (óptimo) | Elegidos PD | ¿Greedy óptimo? |
|---|---|---|---|---|---|
| 0 | 0 | — | 0 | — | Sí |
| 2 | 8 | S4 | 14 | S5 | **No** (pierde 6) |
| 5 | 58 | S1, S4 | 64 | S1, S5 | **No** (pierde 6) |
| 8 | 98 | S1, S2, S4 | 98 | S1, S2, S4 | Sí |
| 10 | 112 | S1, S2, S4, S5 | 112 | S1, S2, S4, S5 | Sí |
| 12 | 112 | S1, S2, S4, S5 | **119** | S1, S3, S5 | **No** (pierde 7) |
| 15 | 153 | S1, S2, S3, S4 | 153 | S1, S2, S3, S4 | Sí |
| 20 | 167 | S1, S2, S3, S4, S5 | 177 | S1, S2, S3, S4, S8 | **No** (pierde 10) |
| 33 | 231 | los 8 | 231 | los 8 | Sí (entra todo) |

Lecturas:
- **Capacidad 2:** el greedy toma S4 (1 kg, ratio 8) y ya no le entra S5 (2 kg); el óptimo es S5
  solo (14). Es el ejemplo mínimo de que el mejor ratio no siempre es la mejor elección.
- **Capacidad 20:** al greedy le sobran 3 kg (usa 17); el óptimo cambia S5 (2 kg, 14) por S8 (5 kg,
  24) y usa los 20.
- Con capacidad ≥ 33 (suma de pesos) entra todo; la PD acota W a 33 y da lo mismo (test
  `capacidadIgualOMayorALaSumaTomaTodo`, con 33 y con 40).

---

## 5. Floyd-Warshall

Consultas: `GET /api/grafo/floyd`, `/floyd/camino` y `/floyd/comparacion`.

### 5.1 Matriz de tiempos mínimos (minutos a pie)

Fila = origen, columna = destino. Simétrica porque el grafo es no dirigido; diagonal en 0.

| | ABA | CON | CUN | HIT | LBO | MON | OBE | PAL | PCE | PMA | REC | RET |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **ABA** | 0 | 36 | 124 | 19 | 65 | 106 | 28 | 40 | 30 | 48 | 29 | 49 |
| **CON** | 36 | 0 | 160 | 55 | 29 | 142 | 31 | 76 | 66 | 30 | 56 | 48 |
| **CUN** | 124 | 160 | 0 | 105 | 183 | 18 | 139 | 84 | 123 | 147 | 114 | 129 |
| **HIT** | 19 | 55 | 105 | 0 | 84 | 87 | 47 | 21 | 18 | 67 | 48 | 66 |
| **LBO** | 65 | 29 | 183 | 84 | 0 | 165 | 56 | 99 | 95 | 36 | 74 | 54 |
| **MON** | 106 | 142 | 18 | 87 | 165 | 0 | 121 | 66 | 105 | 129 | 96 | 111 |
| **OBE** | 28 | 31 | 139 | 47 | 56 | 121 | 0 | 55 | 58 | 20 | 25 | 22 |
| **PAL** | 40 | 76 | 84 | 21 | 99 | 66 | 55 | 0 | 39 | 63 | 30 | 45 |
| **PCE** | 30 | 66 | 123 | 18 | 95 | 105 | 58 | 39 | 0 | 78 | 59 | 79 |
| **PMA** | 48 | 30 | 147 | 67 | 36 | 129 | 20 | 63 | 78 | 0 | 38 | 18 |
| **REC** | 29 | 56 | 114 | 48 | 74 | 96 | 25 | 30 | 59 | 38 | 0 | 20 |
| **RET** | 49 | 48 | 129 | 66 | 54 | 111 | 22 | 45 | 79 | 18 | 20 | 0 |

Suma de las 144 celdas: **9206**, igual que `FloydWarshallTest.sumaDeTodasLasDistancias`. La salida
real de `/floyd` coincide en los pares consultados (`ABA->ABA` 0, `ABA->CON` 36, `ABA->CUN` 124,
`PAL->CUN` 84, `RET->CUN` 129, `LBO->CUN` 183).

### 5.2 Comparación con Dijkstra (algunos pares)

| Par | Floyd | Dijkstra (camino) | Coincide |
|---|---|---|---|
| PAL → CUN | 84 | PAL – MON – CUN = 66 + 18 (no la directa de 90) | Sí |
| RET → CUN | 129 | RET – PAL – MON – CUN = 45 + 66 + 18 | Sí |
| LBO → CUN | 183 | LBO – PMA – RET – PAL – MON – CUN = 36 + 18 + 45 + 66 + 18 | Sí |
| LBO → MON / MON → LBO | 165 / 165 | simétrico (no dirigido) | Sí |
| CON → PCE | 66 | CON – ABA – PCE = 36 + 30 | Sí |
| PCE → PMA | 78 | PCE – ABA – OBE – PMA = 30 + 28 + 20 | Sí |

La fila **PAL** de la matriz es exactamente la última fila de la tabla de Dijkstra desde PAL de la
[sección 1](#1-dijkstra-desde-pal) (HIT 21, REC 30, PCE 39, ABA 40, RET 45, OBE 55, PMA 63, MON 66,
CON 76, CUN 84, LBO 99).

Para los **144 pares**, `FloydWarshallTest.coincideConDijkstraEnTodosLosPares` compara los costos y
`caminoFloydIgualADijkstraEnTodosLosPares` compara además los vértices del camino reconstruido por
`/floyd/camino` (en el dominio no hay caminos mínimos empatados). Salidas reales de `/floyd/camino`:
PAL → CUN `["PAL","MON","CUN"]` 84.0 y LBO → CUN `["LBO","PMA","RET","PAL","MON","CUN"]` 183.0,
iguales a las de `/dijkstra`.

**Cómo llega Floyd a PAL → CUN = 84** (el orden de k es el de los índices: ABA, CON, CUN, HIT, LBO,
MON, …): al empezar d[PAL][CUN] = 90 (calle directa) y siguiente[PAL][CUN] = CUN. Ningún intermedio
anterior a MON mejora ese par. Con k = MON: d[PAL][MON] + d[MON][CUN] = 66 + 18 = 84 < 90, entonces
d[PAL][CUN] = 84 y siguiente[PAL][CUN] = siguiente[PAL][MON] = MON. Reconstrucción: PAL →
siguiente[PAL][CUN] = MON → siguiente[MON][CUN] = CUN.

### 5.3 Trabajo: Floyd vs. Dijkstra V veces

```
GET /api/grafo/floyd/comparacion
{"vertices":12,"operacionesFloyd":1728,"nodosExpandidosDijkstra":144,"aristasExaminadasDijkstra":504}
```

| Métrica | Cálculo a mano | Valor |
|---|---|---|
| Relajaciones de Floyd | V³ = 12³ | 1728 |
| Nodos expandidos por Dijkstra (12 corridas) | V corridas × V nodos (grafo conexo) | 144 |
| Aristas examinadas por Dijkstra (12 corridas) | V corridas × 2E = 12 × 42 | 504 |

El análisis (grafo disperso vs. denso) está en la ficha de
[`/floyd/comparacion`](ENDPOINTS.md#get-apigrafofloydcomparacion).

### 5.4 Ciclo negativo (tests)

Con los datos del dominio no hay costos negativos, así que se verifica con grafos de prueba en
`FloydWarshallTest`:

| Test | Grafo | Resultado esperado |
|---|---|---|
| `cicloNegativoDirigidoFalla` | Dirigido: A → B = 1, B → C = −3, C → A = 1 | Ciclo A → B → C → A = 1 − 3 + 1 = −1: la diagonal queda negativa → `Hay un ciclo de costo negativo` (422 en la API). |
| `aristaNegativaNoDirigidaEsCicloNegativo` | No dirigido: A – B = 2, B – C = −1 | B → C → B = −2: una calle negativa ya es un ciclo negativo → falla. |
| `aristaNegativaSinCicloSeAcepta` | Dirigido: A → B = 1, B → C = −2, A → C = 3 | Sin ciclos: `A->C` = 1 − 2 = **−1** (mejor que el directo 3), `A->B` = 1 y `B->A` **no aparece** (inalcanzable). Dijkstra rechazaría este grafo. |
| `caminoConCicloNegativoFalla` | Dirigido: A → B = 1, B → A = −3 | `/floyd/camino` también falla (A → B → A = −2). |
| `caminoInalcanzableFalla` | Dirigido: A → B = 1 | Camino de B a A: `No hay camino de B a A` (422). |

Traza de `cicloNegativoDirigidoFalla` (orden de k: A, B, C). Inicial: d[A][B] = 1, d[B][C] = −3,
d[C][A] = 1, diagonal 0, el resto ∞.
- k = A: d[C][B] = d[C][A] + d[A][B] = 1 + 1 = 2.
- k = B: d[A][C] = d[A][B] + d[B][C] = 1 − 3 = −2 y d[C][C] = d[C][B] + d[B][C] = 2 − 3 = **−1**.
- k = C: d[A][A] = d[A][C] + d[C][A] = −2 + 1 = **−1** (y d[B][B] = −3 + 2 = −1).

Al terminar hay valores negativos en la diagonal → ciclo negativo detectado.

---

## 6. Backtracking: rutas simples

Lo que pide el hito 8: **reportar nodos explorados con poda y sin poda**. Endpoint
[`/api/grafo/rutas`](ENDPOINTS.md#get-apigraforutas), test `BacktrackingTest`.

**Nodo explorado** = cada entrada a la función recursiva (raíz incluida), es decir, cada camino
parcial que se llega a considerar. **Con poda:** no se baja a un vecino si el costo acumulado
superaría `costoMaximo` o los tramos superarían `saltosMaximos` (poda por viabilidad). **Sin poda:**
solo se respeta el camino simple y el filtro se aplica recién al llegar al destino.

### 6.1 Tabla de nodos explorados

| Grafo | Origen → destino | costoMaximo / saltosMaximos | Rutas | Con poda | Sin poda | Ahorro |
|---|---|---|---|---|---|---|
| Semilla cátedra, **dirigida** | A → H | 25 / 10 | 6 | 26 | 38 | 32 % |
| Semilla cátedra, no dirigida | A → H | 25 / 10 | 6 | 37 | 78 | 53 % |
| Semilla cátedra, no dirigida | A → H | 25 / 5 | 5 | 36 | 78 | 54 % |
| Semilla cátedra, no dirigida | A → H | 25 / 4 | 2 | 30 | 78 | 62 % |
| Dominio CABA | RET → CUN | 150 / 4 | 4 | 118 | 684 | 83 % |
| Dominio CABA | RET → CUN | 130 / 5 | 1 | 144 | 684 | 79 % |
| Dominio CABA | CON → PCE | 90 / 4 | 3 | 42 | 684 | 94 % |
| Dominio CABA | OBE → HIT | 80 / 3 | 4 | 34 | 333 | 90 % |
| Dominio CABA | RET → CUN | 110 / 3 | 0 | 47 | 684 | 93 % |
| Dominio CABA | RET → CUN | 1000 / 10 | 110 | 682 | 684 | 0,3 % |

Todos los valores los verifica `BacktrackingTest`; el caso RET → CUN 150 / 4 coincide con la salida
real del endpoint.

Lectura:
- **Con y sin poda devuelven las mismas rutas** (lo verifica `conYSinPodaDevuelvenLasMismasRutas`):
  con costos ≥ 0 el costo acumulado nunca baja y los tramos solo suben, así que una rama podada no
  podía terminar en una ruta válida.
- **Sin poda la cantidad no depende de los límites**: para RET → CUN siempre son 684 nodos (todos
  los caminos simples que salen de RET sin atravesar CUN, más los que terminan en CUN). Para
  OBE → HIT son 333.
- **Cuanto más ajustados los límites, más poda.** Con 1000 / 10 casi no se ahorra: el camino simple
  más largo de RET a CUN tarda 327 minutos, así que el límite de costo nunca corta, y el de 10 tramos
  solo corta los 2 caminos de 11 tramos que pasan por los 12 lugares (684 − 2 = 682).
- Con 130 / 5 se exploran **más** nodos que con 150 / 4 (144 contra 118) aunque el tiempo máximo sea
  menor: el límite de tramos más holgado deja bajar un nivel más en muchas ramas.
- En la semilla no dirigida aparecen caminos como A – C – B – D – …, que no existen en la dirigida:
  el árbol sin poda crece de 38 a 78 nodos, aunque las rutas válidas sean las mismas 6 (todas las
  nuevas superan 25 minutos).

### 6.2 Traza a mano: semilla de la cátedra dirigida, A → H, costoMaximo = 25, saltosMaximos = 10

Aristas (en el orden de las listas de adyacencia): A → B 4, A → C 9, B → C 3, B → D 7, C → D 2,
C → E 11, D → E 5, D → F 8, E → F 6, E → G 10, F → H 3, G → H 4.

El límite de 10 tramos nunca se activa (el camino más largo tiene 6), así que todas las podas son
por costo. Cada línea numerada es un nodo explorado (`#n`) con el costo acumulado entre paréntesis;
`✂` es un hijo que **no se abre** porque `costo + arista > 25`.

```
#1  A (0)
#2  ├─ B (4)
#3  │  ├─ C (7)
#4  │  │  ├─ D (9)
#5  │  │  │  ├─ E (14)
#6  │  │  │  │  ├─ F (20)
#7  │  │  │  │  │  └─ H (23)            RUTA A-B-C-D-E-F-H = 23
#8  │  │  │  │  └─ G (24)
    │  │  │  │     └─ ✂ H: 24 + 4 = 28
#9  │  │  │  └─ F (17)
#10 │  │  │     └─ H (20)               RUTA A-B-C-D-F-H = 20
#11 │  │  └─ E (18)
#12 │  │     ├─ F (24)
    │  │     │  └─ ✂ H: 24 + 3 = 27
    │  │     └─ ✂ G: 18 + 10 = 28
#13 │  └─ D (11)
#14 │     ├─ E (16)
#15 │     │  ├─ F (22)
#16 │     │  │  └─ H (25)            RUTA A-B-D-E-F-H = 25
    │     │  └─ ✂ G: 16 + 10 = 26
#17 │     └─ F (19)
#18 │        └─ H (22)               RUTA A-B-D-F-H = 22
#19 └─ C (9)
#20    ├─ D (11)
#21    │  ├─ E (16)
#22    │  │  ├─ F (22)
#23    │  │  │  └─ H (25)            RUTA A-C-D-E-F-H = 25
       │  │  └─ ✂ G: 16 + 10 = 26
#24    │  └─ F (19)
#25    │     └─ H (22)               RUTA A-C-D-F-H = 22
#26    └─ E (20)
          ├─ ✂ F: 20 + 6 = 26
          └─ ✂ G: 20 + 10 = 30
```

**Con poda: 26 nodos y 6 rutas**, en el mismo orden que devuelve el algoritmo (test
`semillaDirigida`). Una ruta de costo exactamente 25 se acepta (la condición es `≤`).

**Sin poda: 38 nodos.** Se exploran los 8 hijos podados y, debajo de ellos, todo lo que cuelga:
12 nodos más (38 − 26).

| Nodo que la poda evita | Costo | Qué pasa sin poda |
|---|---|---|
| A-B-C-D-E-G-H | 28 | llega a H y se descarta al llegar |
| A-B-C-E-F-H | 27 | llega a H y se descarta al llegar |
| A-B-C-E-G | 28 | se explora |
| A-B-C-E-G-H | 32 | llega a H y se descarta al llegar |
| A-B-D-E-G | 26 | se explora |
| A-B-D-E-G-H | 30 | llega a H y se descarta al llegar |
| A-C-D-E-G | 26 | se explora |
| A-C-D-E-G-H | 30 | llega a H y se descarta al llegar |
| A-C-E-F | 26 | se explora |
| A-C-E-F-H | 29 | llega a H y se descarta al llegar |
| A-C-E-G | 30 | se explora |
| A-C-E-G-H | 34 | llega a H y se descarta al llegar |

Ninguno de los 12 da una ruta válida: las 7 llegadas a H superan los 25 minutos. Por eso las dos
corridas devuelven las mismas 6 rutas.

**Retroceso:** al volver de #7 se desmarca H, y al volver de #6 se desmarca F; por eso F se puede
volver a usar en #9 por otro camino (A-B-C-D-F). Lo mismo con D, que se usa en #4 (desde C) y en #13
(desde B). Si quedaran marcados, como en un DFS, solo se encontraría la primera ruta.

---

## 7. Branch & Bound: reparto en k contenedores

Lo que pide el hito 11: **explicitar cuál es la cota y por qué es optimista, y reportar los nodos
podados**. Endpoint [`/api/seleccion/repartir`](ENDPOINTS.md#get-apiseleccionrepartir), test
`RamificacionYPodaTest`.

Suministros en orden de id (kg): S1 3, S2 4, S3 7, S4 1, S5 2, S6 5, S7 6, S8 5. **Total 33 kg**,
n = 8, m = min(k, n) contenedores efectivos.

### 7.1 La cota

```
cota(hijo) = max( carga máxima después de ubicar el ítem , peso total / m )
```

Es una **cota inferior optimista** del makespan que se puede alcanzar desde ese nodo:
- las cargas solo crecen (pesos ≥ 0), así que el contenedor más cargado hoy pesa al menos eso al
  final;
- en cualquier reparto completo, el más cargado lleva al menos el promedio total / m (si k ≥ n,
  total / n ≤ el ítem más pesado ≤ óptimo, así que también vale).

Nunca sobreestima: si `cota >= mejor`, ningún reparto de esa rama puede mejorar a la mejor solución
conocida y se poda **sin perder el óptimo**. El óptimo se verifica contra fuerza bruta
(`optimoCoincideConFuerzaBruta`, k = 2, 3, 4, probando los k^n repartos).

### 7.2 Tabla por k

| k | Cota inicial (33 / m) | Carga máxima óptima | Explorados | Podados | Árbol completo sin poda (m^(n+1) − 1) / (m − 1) |
|---|---|---|---|---|---|
| 1 | 33 | 33 | 9 | 0 | 9 (un solo camino) |
| 2 | 16,5 | **17** | 84 | 74 | 511 |
| 3 | 11 | **11** | 61 | 87 | 9841 |
| 4 | 8,25 | **9** | 123 | 291 | 87381 |
| 8 | 4,125 | 7 | 50 | 72 | — |
| 9 o más | 4,125 (m = 8) | 7 | 50 | 72 | — |

- **k = 1:** un solo hijo por nivel: 9 nodos y nada que podar (`mejor` es +∞ hasta la única hoja).
- **k = 2:** la cota 16,5 no se alcanza porque los pesos son enteros; el óptimo es 17. La cota es
  optimista, no necesariamente alcanzable.
- **k = 3:** hay un reparto perfecto (3 + 7 + 1, 4 + 2 + 5, 6 + 5): el óptimo **alcanza la cota** y
  desde ese momento todo se poda. 61 nodos contra 9841 del árbol completo (0,6 %).
- **k = 4:** 33 / 4 = 8,25 pero el óptimo es 9: con pesos enteros las cargas son enteras, y cuatro
  contenedores de 8 kg suman 32 < 33, así que alguno lleva 9 o más. Lo mismo explica el 17 de k = 2.
- **k ≥ 8:** el óptimo es el ítem más pesado (S3, 7 kg), porque ningún reparto puede bajar de eso.
  El reparto devuelto es el primero que llega a 7 (S1+S2 = 7 kg juntos, Sobrevivientes 7 y 8
  vacíos); los demás repartos de 7 se podan por `>=`. Con k = 9 o
  k = 1.000.000 se usan solo m = 8 contenedores y los contadores son los mismos que con k = 8.
- Efecto de cada técnica en k = 3: árbol completo 9841 nodos; solo con ruptura de simetría, 1645;
  con simetría + cota, **61**. (El 1645 sale de simular el mismo recorrido sin la cota; el 61 lo
  verifica el test.)

### 7.3 Traza parcial, k = 2 (cota inicial 16,5)

Cada fila numerada es un nodo explorado; `✂` es un hijo podado. `Cargas` es el estado después de
ubicar el ítem. El primer ítem va siempre al contenedor 1 (ruptura de simetría: un contenedor vacío
solo se prueba si es el primero vacío).

| Nodo | Decisión | Cargas [C1, C2] | Cota del hijo vs. `mejor` | Resultado |
|---|---|---|---|---|
| 1 | raíz | [0, 0] | — | |
| 2–9 | S1 … S8 → C1 | [33, 0] | todas < +∞ | hoja: **mejor = 33** |
| 10 | S8 → C2 | [28, 5] | max(28; 16,5) = 28 < 33 | hoja: **mejor = 28** |
| 11 | S7 → C2 | [22, 6] | 22 < 28 | |
| 12 | S8 → C1 | [27, 6] | 27 < 28 | hoja: **mejor = 27** |
| 13 | S8 → C2 | [22, 11] | 22 < 27 | hoja: **mejor = 22** |
| 14 | S6 → C2 | [17, 5] | 17 < 22 | |
| ✂ | S7 → C1 | [23, 5] | 23 ≥ 22 | **podado** (1) |
| 15 | S7 → C2 | [17, 11] | 17 < 22 | |
| ✂ | S8 → C1 | [22, 11] | 22 ≥ 22 | **podado** (2) |
| 16 | S8 → C2 | [17, 16] | 17 < 22 | hoja: **mejor = 17** |
| 17 | S5 → C2 | [15, 2] | 15 < 17 | |
| ✂ | S6 → C1 | [20, 2] | 20 ≥ 17 | **podado** (3) |
| 18 | S6 → C2 | [15, 7] | 15 < 17 | |
| ✂ | S7 → C1 | [21, 7] | 21 ≥ 17 | **podado** (4) |
| 19 | S7 → C2 | [15, 13] | 15 < 17 | |
| ✂ | S8 → C1 | [20, 13] | 20 ≥ 17 | **podado** (5) |
| ✂ | S8 → C2 | [15, 18] | 18 ≥ 17 | **podado** (6) |

`mejor` baja 33 → 28 → 27 → 22 → **17** en los primeros 16 nodos. Después ninguna hoja mejora (con
pesos enteros 17 es el óptimo), pero cada hijo con cota ≥ 17 se corta: la rama del nodo 17 se
cierra después de 3 nodos y 4 podas sin llegar a ninguna hoja. Al terminar: **84 explorados, 74
podados, carga máxima 17** (S1–S5 = 17 kg, S6–S8 = 16 kg), igual que el endpoint y el test
`dosContenedores`.

Diferencia con el backtracking de rutas: los repartos podados **eran válidos** (todo reparto lo
es); se podan porque no pueden ser **mejores** (poda por optimalidad, no por viabilidad).

### 7.4 Cómo baja `mejor` con k = 3 (cota inicial 11)

| Nodo explorado | Hoja encontrada (cargas) | `mejor` | Podados hasta ahí |
|---|---|---|---|
| 9 | [33, 0, 0] | 33 | 0 |
| 10 | [28, 5, 0] | 28 | 0 |
| 12 | [27, 6, 0] | 27 | 0 |
| 13 | [22, 11, 0] | 22 | 0 |
| 16 | [17, 16, 0] | 17 | 3 |
| 20 | [15, 13, 5] | 15 | 9 |
| 25 | [14, 14, 5] | 14 | 16 |
| 31 | [10, 12, 11] | 12 | 24 |
| 61 | [11, 11, 11] | **11** | 81 |

En el nodo 61 `mejor` = 11 = cota inicial. Desde ahí todo hijo tiene cota ≥ 11 = `mejor` y se poda
(6 podas más, 87 en total) sin explorar ningún nodo nuevo: el algoritmo termina con la **prueba** de
que 11 es óptimo sin recorrer el resto del árbol.

---

## 8. UCS: Dijkstra con parada temprana

Endpoint [`/api/grafo/ucs`](ENDPOINTS.md#get-apigrafoucs), test `UcsTest`. Mismo bucle que
Dijkstra (sección 1), pero **termina cuando el destino sale de la cola**. **Nodo expandido** =
extracción válida de la cola (las entradas viejas descartadas no cuentan; el destino sí).

### 8.1 Traza a mano: LBO → HIT

Inicialización: dist[LBO] = 0, el resto ∞; cola = {(LBO, 0)}. Los vecinos se miran en el orden de
la lista de adyacencia (tabla en [ENDPOINTS.md](ENDPOINTS.md#carga-del-grafo-común-a-todos-los-endpoints-de-apigrafo)).

| Expandido | Se extrae (g) | Relajaciones exitosas | Relajaciones que no mejoran |
|---|---|---|---|
| 1 | LBO (0) | CON 0+29=29, PMA 0+36=36 | — |
| 2 | CON (29) | ABA 29+36=65, OBE 29+31=60 | PMA 29+30=59 ≥ 36 |
| 3 | PMA (36) | **OBE 36+20=56 < 60** (se reencola), RET 36+18=54 | — |
| 4 | RET (54) | PAL 54+45=99, REC 54+20=74 | OBE 54+22=76 ≥ 56 |
| 5 | OBE (56) | — | ABA 56+28=84 ≥ 65; REC 56+25=81 ≥ 74 |
| — | (OBE, 60) | entrada vieja: 60 > dist[OBE] = 56, **se descarta** (no cuenta) | |
| 6 | ABA (65) | HIT 65+19=84, PCE 65+30=95 | REC 65+29=94 ≥ 74 |
| 7 | REC (74) | — | PAL 74+30=104 ≥ 99 |
| 8 | **HIT (84)** | **es el destino: fin** (no se miran sus vecinos) | |

(Se omiten los vecinos ya procesados, que nunca mejoran con costos ≥ 0.)

Al cortar quedan en la cola PCE (95) y PAL (99) sin expandir; MON y CUN ni siquiera se
descubrieron (dist = ∞). Camino por `previo[]`: HIT ← ABA ← CON ← LBO, es decir
**LBO → CON → ABA → HIT = 29 + 36 + 19 = 84**. Cola: 11 inserciones y 9 extracciones
(8 válidas + 1 descartada); Dijkstra completo habría seguido con PCE, PAL, MON y CUN.

Notar que HIT se **descubre** en el paso 6 con 84 y recién se **saca** en el paso 8: en este caso
el valor no cambió en el medio, pero en PAL → CUN sí (se descubre con 90 por la calle directa y
sale con 84 por MON, ver sección 1). Por eso UCS corta al sacar y no al descubrir.

Salida real, coincide con la traza:

```
GET /api/grafo/ucs?origen=LBO&destino=HIT
{"vertices":["LBO","CON","ABA","HIT"],"costoTotal":84.0,"nodosExpandidosUcs":8,"nodosExpandidosDijkstra":12}
```

### 8.2 Nodos expandidos: UCS vs. Dijkstra

| Consulta | Camino | Costo (min) | UCS | Dijkstra | Por qué |
|---|---|---|---|---|---|
| LBO → HIT | LBO, CON, ABA, HIT | 84 | **8** | 12 | Traza 8.1. |
| PAL → CUN | PAL, MON, CUN | 84 | **11** | 12 | Solo queda afuera LBO (99), el más lejano de PAL (sección 1, paso 12). |
| RET → PMA | RET, PMA | 18 | **2** | 12 | PMA es el vecino más cercano de RET. |
| RET → CUN | RET, PAL, MON, CUN | 129 | 12 | 12 | **Peor caso:** CUN es el más lejano de RET, sale último. |
| RET → RET | RET | 0 | 1 | 12 | El origen es el destino: corta en la primera extracción. |
| Semilla cátedra A → H | A, B, C, D, F, H | 20 | **7** | 8 | Salen A 0, B 4, C 7, D 9, E 14, F 17, H 20; G queda en la cola con 24 (diapositiva de la clase 13). |

Las cuatro primeras son salidas reales del endpoint; todas están en `UcsTest`, que además verifica
en cada caso que camino y costo sean iguales a los de `dijkstra` y que
`nodosExpandidosUcs ≤ nodosExpandidosDijkstra`.

---

## 9. Lugares infectados (Dijkstra y Backtracking)

Parámetro opcional `infectados` de `/dijkstra` y `/rutas`: lugares que ninguna ruta puede pisar.
Dijkstra no relaja hacia un infectado (nunca entra a la cola); el backtracking los arranca como
visitados. Detalle y complejidad en
[`ENDPOINTS.md`](ENDPOINTS.md#lugares-infectados-en-dijkstra).

### 9.1 Casos calculados a mano (minutos a pie)

| Consulta | Sin infectados | Con infectados | Salida real |
|---|---|---|---|
| PAL → CUN, `infectados=MON` | PAL – MON – CUN = 66 + 18 = 84 | PAL – CUN = **90** | `{"vertices":["PAL","CUN"],"costoTotal":90.0}` |
| LBO → HIT, `infectados=CON` | LBO – CON – ABA – HIT = 29 + 36 + 19 = 84 | LBO – PMA – OBE – ABA – HIT = 36 + 20 + 28 + 19 = **103** | `{"vertices":["LBO","PMA","OBE","ABA","HIT"],"costoTotal":103.0}` |
| LBO → HIT, `infectados=CON,ABA` | 84 | LBO – PMA – RET – PAL – HIT = 36 + 18 + 45 + 21 = **120** | `{"vertices":["LBO","PMA","RET","PAL","HIT"],"costoTotal":120.0}` |
| RET → CUN, `infectados=PAL` | RET – PAL – MON – CUN = 129 | **sin camino** | 422 `No hay camino de RET a CUN` |
| PAL → CUN, `infectados=MON,PAL` | — | origen infectado | 400 `El origen PAL esta infectado: no se puede usar` |
| PAL → CUN, `infectados=XXX` | — | id inexistente | 400 `No existe el nodo con id: XXX` |

**Por qué PAL → CUN pasa a 90.** En la traza de la sección 1, dist[CUN] arranca en 90 por la calle
directa y baja a 84 cuando se extrae MON (66 + 18). Con MON infectado, al expandir PAL no se relaja
hacia MON: dist[MON] queda en ∞, MON nunca se extrae y la mejora a 84 no ocurre. CUN se queda con
90, previo[CUN] = PAL.

**Por qué LBO → HIT con CON, ABA da 120.** Sin CON ni ABA, a HIT solo se llega por PAL o PCE, y a
PCE solo por ABA o HIT: hay que llegar a PAL. Desde LBO la única salida es PMA (36). De PMA a PAL:
por RET, 18 + 45 = 63; por RET – REC, 18 + 20 + 30 = 68; por OBE – REC, 20 + 25 + 30 = 75. Gana RET: 36 + 63 + 21 = 120.

**Por qué RET → CUN queda sin camino.** Las calles de CUN son solo MON – CUN y PAL – CUN, y las de
MON solo PAL – MON y MON – CUN. Con PAL infectado, el conjunto {MON, CUN} no tiene ninguna calle
hacia el resto: el grafo sin PAL queda desconectado. Dijkstra termina con dist[CUN] = ∞ y responde 422.

### 9.2 Backtracking con un infectado

`GET /api/grafo/rutas?origen=RET&destino=CUN&costoMaximo=150&saltosMaximos=4&infectados=REC`

| | Rutas | Nodos con poda | Nodos sin poda |
|---|---|---|---|
| Sin infectados | 4 | 118 | 684 |
| `infectados=REC` | 2: RET – PAL – CUN (45 + 90 = 135) y RET – PAL – MON – CUN (45 + 66 + 18 = 129) | **57** | **212** |

Las dos rutas que desaparecen son las que pasaban por REC (RET – REC – PAL – CUN = 140 y
RET – REC – PAL – MON – CUN = 134). Por REC no se pasa, así que hay que llegar a PAL: directo (45) o
rodeando, por ejemplo RET – OBE – ABA – HIT – PAL = 22 + 28 + 19 + 21 = 90, y desde PAL faltan al
menos 84 (90 + 84 = 174 > 150). Por eso quedan solo las que usan RET – PAL. Con y sin poda devuelven
las mismas rutas. Los nodos explorados bajan en las dos corridas porque REC nunca se visita: se
eliminan todos los subárboles que colgaban de él, sin entrar a ninguno.

Casos cubiertos por `LugaresInfectadosTest` (sin Neo4j).
