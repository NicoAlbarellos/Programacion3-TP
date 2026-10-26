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
