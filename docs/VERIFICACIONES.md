# Verificaciones a mano

Evidencia que piden los hitos: los resultados de los algoritmos **calculados a mano sobre el grafo
del grupo** y comparados con la salida real de los endpoints y con los tests.

Grafo: 31 lugares de CABA, 51 calles no dirigidas, costo en **minutos a pie** (tabla de calles y
diagrama en el [README](../README.md); fuente única de los datos: `seed/DominioCaba.java`).

Notación de las tablas: `d/p` = distancia (o key) / vértice previo (o padre). `∞` = todavía no
alcanzado. En **negrita**, los vértices ya extraídos (su valor es definitivo).

Los vecinos de cada lugar se miran en el orden de su lista de adyacencia, que es el orden
alfabético de ids (las calles se cargan ordenadas por `(origen, destino)`):

| Lugar | Vecinos (minutos) | Lugar | Vecinos (minutos) |
|---|---|---|---|
| ABA | BOE 36, CGR 27, HIT 15, REC 36 | LBO | CON 32, HGA 51, SAN 34 |
| AEP | BEL 59, CUN 44, PLA 21 | MAT | FLO 73, VEL 45 |
| AGR | CHA 45, DEV 43, VUR 31 | MON | BEL 24, CUN 20, SAA 43 |
| BEL | AEP 59, COL 27, MON 24, SAA 44 | OBE | CGR 18, CON 40, PMA 28, REC 31, RET 23 |
| BOE | ABA 36, CAB 27, HGA 36, PCH 35 | PAL | CHA 43, HIT 43, PLA 23, REC 40 |
| CAB | BOE 27, FLO 43, PCE 20, PCH 29 | PCE | CAB 20, CHA 37, HIT 20 |
| CGR | ABA 27, CON 33, OBE 18 | PCH | BOE 35, CAB 29, FLO 36 |
| CHA | AGR 45, COL 19, PAL 43, PCE 37 | PLA | AEP 21, COL 50, PAL 23 |
| COL | BEL 27, CHA 19, PLA 50, VUR 56 | PMA | OBE 28, RES 15, RET 36, SAN 20 |
| CON | CGR 33, HGA 15, LBO 32, OBE 40, SAN 18 | REC | ABA 36, OBE 31, PAL 40, RET 26 |
| CUN | AEP 44, MON 20 | RES | PMA 15 |
| DEV | AGR 43, VEL 58 | RET | OBE 23, PMA 36, REC 26 |
| FLO | CAB 43, MAT 73, PCH 36, VEL 79 | SAA | BEL 44, MON 43, VUR 35 |
| HGA | BOE 36, CON 15, LBO 51 | SAN | CON 18, LBO 34, PMA 20 |
| HIT | ABA 15, PAL 43, PCE 20 | VEL | DEV 58, FLO 79, MAT 45 |
| | | VUR | AGR 31, COL 56, SAA 35 |

Cómo se obtuvieron los números: las trazas se calcularon a mano y se contrastaron con una copia
instrumentada de las clases de `algorithm/` (mismo código, con impresiones en cada paso) corrida
sobre `DominioCaba`, con los tests (`./mvnw test`) y con la salida real de los endpoints.

## Índice

1. [Dijkstra desde HGA](#1-dijkstra-desde-hga)
2. [Kruskal](#2-kruskal)
3. [Prim desde RET](#3-prim-desde-ret)
4. [Greedy vs. mochila 0/1 con PD](#4-greedy-vs-mochila-01-con-pd)
5. [Floyd-Warshall](#5-floyd-warshall)
6. [Backtracking: rutas simples](#6-backtracking-rutas-simples)
7. [Branch & Bound: reparto en k contenedores](#7-branch--bound-reparto-en-k-contenedores)
8. [UCS: Dijkstra con parada temprana](#8-ucs-dijkstra-con-parada-temprana)
9. [Lugares infectados (Dijkstra y Backtracking)](#9-lugares-infectados-dijkstra-y-backtracking)

---

## 1. Dijkstra desde HGA

Consulta: `GET /api/grafo/dijkstra?origen=HGA&destino=LBO`. Interesa porque existe la calle
directa Garrahan – La Boca de **51** minutos (rodea las vías del Roca y la autopista), pero la ruta
más rápida es por Constitución: **15 + 32 = 47**. Si el algoritmo devolviera 51, no estaría
relajando bien.

Inicialización: dist[HGA] = 0, el resto ∞; previo[] = — para todos; cola = {(HGA, 0)}.

En cada paso se extrae de la cola el vértice no procesado con menor dist[] y se **relaja** cada
calle (u, v): si dist[u] + costo < dist[v], se actualiza dist[v], previo[v] = u y se encola
(v, dist[v]). Si sale de la cola una entrada con distancia mayor que dist[u] (entrada vieja), se
descarta (eliminación perezosa, porque `PriorityQueue` no tiene decreaseKey).

`/dijkstra` calcula el árbol de caminos mínimos **completo** (no corta al sacar el destino; eso es
lo que agrega UCS, sección 8), así que la traza tiene los 31 pasos.

### Relajaciones de cada paso

| Paso | Se extrae (dist / previo) | Relajaciones exitosas | Relajaciones que no mejoran |
|---|---|---|---|
| 1 | HGA (0 / —) | BOE 0+36=36, CON 0+15=15, LBO 0+51=51 | — |
| 2 | CON (15 / HGA) | CGR 15+33=48, **LBO 15+32=47 < 51**, OBE 15+40=55, SAN 15+18=33 | — |
| 3 | SAN (33 / CON) | PMA 33+20=53 | LBO 33+34=67 ≥ 47 |
| 4 | BOE (36 / HGA) | ABA 36+36=72, CAB 36+27=63, PCH 36+35=71 | — |
| 5 | **LBO (47 / CON)** | — | — |
| 6 | CGR (48 / CON) | — | ABA 48+27=75 ≥ 72; OBE 48+18=66 ≥ 55 |
| — | (LBO, 51) | entrada vieja: 51 > dist[LBO] = 47, **se descarta** | |
| 7 | PMA (53 / SAN) | RES 53+15=68, RET 53+36=89 | OBE 53+28=81 ≥ 55 |
| 8 | OBE (55 / CON) | REC 55+31=86, **RET 55+23=78 < 89** | — |
| 9 | CAB (63 / BOE) | FLO 63+43=106, PCE 63+20=83 | PCH 63+29=92 ≥ 71 |
| 10 | RES (68 / PMA) | — | — |
| 11 | PCH (71 / BOE) | — | FLO 71+36=107 ≥ 106 |
| 12 | ABA (72 / BOE) | HIT 72+15=87 | REC 72+36=108 ≥ 86 |
| 13 | RET (78 / OBE) | — | REC 78+26=104 ≥ 86 |
| 14 | PCE (83 / CAB) | CHA 83+37=120 | HIT 83+20=103 ≥ 87 |
| 15 | REC (86 / OBE) | PAL 86+40=126 | — |
| 16 | HIT (87 / ABA) | — | PAL 87+43=130 ≥ 126 |
| — | (RET, 89) | entrada vieja: 89 > dist[RET] = 78, **se descarta** | |
| 17 | FLO (106 / CAB) | MAT 106+73=179, VEL 106+79=185 | — |
| 18 | CHA (120 / PCE) | AGR 120+45=165, COL 120+19=139 | PAL 120+43=163 ≥ 126 |
| 19 | PAL (126 / REC) | PLA 126+23=149 | — |
| 20 | COL (139 / CHA) | BEL 139+27=166, VUR 139+56=195 | PLA 139+50=189 ≥ 149 |
| 21 | PLA (149 / PAL) | AEP 149+21=170 | — |
| 22 | AGR (165 / CHA) | DEV 165+43=208 | VUR 165+31=196 ≥ 195 |
| 23 | BEL (166 / COL) | MON 166+24=190, SAA 166+44=210 | AEP 166+59=225 ≥ 170 |
| 24 | AEP (170 / PLA) | CUN 170+44=214 | — |
| 25 | MAT (179 / FLO) | — | VEL 179+45=224 ≥ 185 |
| 26 | VEL (185 / FLO) | — | DEV 185+58=243 ≥ 208 |
| 27 | MON (190 / BEL) | **CUN 190+20=210 < 214** | SAA 190+43=233 ≥ 210 |
| 28 | VUR (195 / COL) | — | SAA 195+35=230 ≥ 210 |
| 29 | DEV (208 / AGR) | — | — |
| 30 | SAA (210 / BEL) | — | — |
| 31 | CUN (210 / MON) | — | — |
| — | (CUN, 214) | entrada vieja: 214 > dist[CUN] = 210, **se descarta**; la cola queda vacía | |

(En "no mejoran" se omiten los vecinos ya procesados, que nunca mejoran con costos ≥ 0. En los
pasos 30 y 31 SAA y CUN empatan en 210: cualquier orden es correcto y no cambia ningún valor.)

### Estado de dist[] / previo[] hasta que LBO queda definitivo

Solo las columnas de los lugares que se descubren en esos pasos (el resto sigue en ∞).

| Paso | Extrae | CON | SAN | BOE | LBO | CGR | PMA | OBE | CAB | PCH | ABA |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 | — | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ |
| 1 | HGA | 15/HGA | ∞ | 36/HGA | 51/HGA | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ |
| 2 | CON | **15/HGA** | 33/CON | 36/HGA | 47/CON | 48/CON | ∞ | 55/CON | ∞ | ∞ | ∞ |
| 3 | SAN | **15/HGA** | **33/CON** | 36/HGA | 47/CON | 48/CON | 53/SAN | 55/CON | ∞ | ∞ | ∞ |
| 4 | BOE | **15/HGA** | **33/CON** | **36/HGA** | 47/CON | 48/CON | 53/SAN | 55/CON | 63/BOE | 71/BOE | 72/BOE |
| 5 | LBO | **15/HGA** | **33/CON** | **36/HGA** | **47/CON** | 48/CON | 53/SAN | 55/CON | 63/BOE | 71/BOE | 72/BOE |

dist[HGA] = 0 durante todo el algoritmo (es el origen). Desde el paso 5 dist[LBO] = 47 ya no puede
cambiar: todo lo que queda en la cola cuesta 48 o más y los costos son ≥ 0.

### Reconstrucción del camino HGA → LBO

Se sigue previo[] desde el destino: previo[LBO] = CON, previo[CON] = HGA, previo[HGA] = —.
Camino: **HGA → CON → LBO, 15 + 32 = 47 minutos**.

La calle directa HGA – LBO dejó dist[LBO] = 51 en el paso 1; la relajación de CON – LBO en el paso 2
la mejora a 47 y previo[LBO] pasa de HGA a CON. La entrada (LBO, 51) queda en la cola y se
descarta cuando sale, después del paso 6.

Con la misma tabla se lee cualquier otro tiempo mínimo desde HGA. Por ejemplo HGA → CUN = 210:
previo[CUN] = MON, previo[MON] = BEL, previo[BEL] = COL, previo[COL] = CHA, previo[CHA] = PCE,
previo[PCE] = CAB, previo[CAB] = BOE, previo[BOE] = HGA, o sea
HGA → BOE → CAB → PCE → CHA → COL → BEL → MON → CUN = 36 + 27 + 20 + 37 + 19 + 27 + 24 + 20 = 210.
Ahí también hubo relajación: CUN se descubrió con 214 por Aeroparque (paso 24) y bajó a 210 por el
Monumental (paso 27).

### Comparación con la salida real

```
GET /api/grafo/dijkstra?origen=HGA&destino=LBO
{"vertices":["HGA","CON","LBO"],"costoTotal":47.0}
```

Coincide con el cálculo a mano. Lo verifica también
`CaminosMinimosTest.relajacionEvitaElDirectoGarrahanLaBoca`.

Conteo de la cola: 34 inserciones (el origen + 33 relajaciones exitosas) y 34 extracciones
(31 vértices procesados + 3 entradas viejas descartadas: LBO 51, RET 89, CUN 214). Las 33
relajaciones exitosas no superan E = 51: en un grafo no dirigido cada calle mejora a lo sumo una
vez (desde el primero de sus extremos que se procesa), consistente con "a lo sumo E + 1 entradas".

---

## 2. Kruskal

Consulta: `GET /api/grafo/kruskal`.

**Paso 1: ordenar las 51 calles por costo creciente** (MergeSort propio, estable: entre empates
queda el orden en que se listan, que es el de las listas de adyacencia, por id de sus extremos).

**Paso 2: recorrerlas en orden.** Para cada calle (u, v): si find(u) ≠ find(v), están en
componentes distintas y se **acepta** (union(u, v)); si find(u) = find(v), ya están conectados y la
calle **cerraría un ciclo**: se **rechaza**. Se corta al llegar a V − 1 = 30 aristas.

En "Componente" se indica cuántos lugares tiene la componente que queda después de aceptar; en los
rechazos, el ciclo que cerraría (el camino que ya los une en el bosque) y su calle más cara.

| # | Calle | Costo | ¿find(u) = find(v)? | Decisión | Componente resultante / ciclo que cerraría | Aristas | Total |
|---|---|---|---|---|---|---|---|
| 1 | ABA – HIT | 15 | no | Acepta | 2 lugares | 1 | 15 |
| 2 | CON – HGA | 15 | no | Acepta | 2 lugares | 2 | 30 |
| 3 | PMA – RES | 15 | no | Acepta | 2 lugares | 3 | 45 |
| 4 | CGR – OBE | 18 | no | Acepta | 2 lugares | 4 | 63 |
| 5 | CON – SAN | 18 | no | Acepta | 3 lugares {CON, HGA, SAN} | 5 | 81 |
| 6 | CHA – COL | 19 | no | Acepta | 2 lugares | 6 | 100 |
| 7 | CAB – PCE | 20 | no | Acepta | 2 lugares | 7 | 120 |
| 8 | CUN – MON | 20 | no | Acepta | 2 lugares | 8 | 140 |
| 9 | HIT – PCE | 20 | no | Acepta | 4 lugares {ABA, CAB, HIT, PCE} | 9 | 160 |
| 10 | PMA – SAN | 20 | no | Acepta | 5 lugares {CON, HGA, PMA, RES, SAN} | 10 | 180 |
| 11 | AEP – PLA | 21 | no | Acepta | 2 lugares | 11 | 201 |
| 12 | OBE – RET | 23 | no | Acepta | 3 lugares {CGR, OBE, RET} | 12 | 224 |
| 13 | PAL – PLA | 23 | no | Acepta | 3 lugares {AEP, PAL, PLA} | 13 | 247 |
| 14 | BEL – MON | 24 | no | Acepta | 3 lugares {BEL, CUN, MON} | 14 | 271 |
| 15 | REC – RET | 26 | no | Acepta | 4 lugares {CGR, OBE, REC, RET} | 15 | 297 |
| 16 | ABA – CGR | 27 | no | Acepta | 8 lugares | 16 | 324 |
| 17 | BEL – COL | 27 | no | Acepta | 5 lugares {BEL, CHA, COL, CUN, MON} | 17 | 351 |
| 18 | BOE – CAB | 27 | no | Acepta | 9 lugares | 18 | 378 |
| 19 | OBE – PMA | 28 | no | Acepta | 14 lugares | 19 | 406 |
| 20 | CAB – PCH | 29 | no | Acepta | 15 lugares | 20 | 435 |
| 21 | AGR – VUR | 31 | no | Acepta | 2 lugares | 21 | 466 |
| 22 | OBE – REC | 31 | **sí** | Rechaza | OBE – RET – REC (máx. 26) | 21 | 466 |
| 23 | CON – LBO | 32 | no | Acepta | 16 lugares | 22 | 498 |
| 24 | CGR – CON | 33 | **sí** | Rechaza | CGR – OBE – PMA – SAN – CON (máx. 28) | 22 | 498 |
| 25 | LBO – SAN | 34 | **sí** | Rechaza | LBO – CON – SAN (máx. 32) | 22 | 498 |
| 26 | BOE – PCH | 35 | **sí** | Rechaza | BOE – CAB – PCH (máx. 29) | 22 | 498 |
| 27 | SAA – VUR | 35 | no | Acepta | 3 lugares {AGR, SAA, VUR} | 23 | 533 |
| 28 | ABA – BOE | 36 | **sí** | Rechaza | ABA – HIT – PCE – CAB – BOE (máx. 27) | 23 | 533 |
| 29 | ABA – REC | 36 | **sí** | Rechaza | ABA – CGR – OBE – RET – REC (máx. 27) | 23 | 533 |
| 30 | BOE – HGA | 36 | **sí** | Rechaza | BOE – CAB – PCE – HIT – ABA – CGR – OBE – PMA – SAN – CON – HGA (máx. 28) | 23 | 533 |
| 31 | FLO – PCH | 36 | no | Acepta | 17 lugares | 24 | 569 |
| 32 | PMA – RET | 36 | **sí** | Rechaza | PMA – OBE – RET (máx. 28) | 24 | 569 |
| 33 | CHA – PCE | 37 | no | Acepta | 22 lugares | 25 | 606 |
| 34 | CON – OBE | 40 | **sí** | Rechaza | CON – SAN – PMA – OBE (máx. 28) | 25 | 606 |
| 35 | PAL – REC | 40 | no | Acepta | 25 lugares | 26 | 646 |
| 36 | AGR – DEV | 43 | no | Acepta | 4 lugares {AGR, DEV, SAA, VUR} | 27 | 689 |
| 37 | CAB – FLO | 43 | **sí** | Rechaza | CAB – PCH – FLO (máx. 36) | 27 | 689 |
| 38 | CHA – PAL | 43 | **sí** | Rechaza | CHA – PCE – HIT – ABA – CGR – OBE – RET – REC – PAL (máx. 40) | 27 | 689 |
| 39 | HIT – PAL | 43 | **sí** | Rechaza | HIT – ABA – CGR – OBE – RET – REC – PAL (máx. 40) | 27 | 689 |
| 40 | MON – SAA | 43 | no | Acepta | 29 lugares | 28 | 732 |
| 41 | AEP – CUN | 44 | **sí** | Rechaza | AEP – PLA – PAL – REC – RET – OBE – CGR – ABA – HIT – PCE – CHA – COL – BEL – MON – CUN (máx. 40) | 28 | 732 |
| 42 | BEL – SAA | 44 | **sí** | Rechaza | BEL – MON – SAA (máx. 43) | 28 | 732 |
| 43 | AGR – CHA | 45 | **sí** | Rechaza | AGR – VUR – SAA – MON – BEL – COL – CHA (máx. 43) | 28 | 732 |
| 44 | MAT – VEL | 45 | no | Acepta | 2 lugares | 29 | 777 |
| 45 | COL – PLA | 50 | **sí** | Rechaza | COL – CHA – PCE – HIT – ABA – CGR – OBE – RET – REC – PAL – PLA (máx. 40) | 29 | 777 |
| 46 | HGA – LBO | 51 | **sí** | Rechaza | HGA – CON – LBO (máx. 32) | 29 | 777 |
| 47 | COL – VUR | 56 | **sí** | Rechaza | COL – BEL – MON – SAA – VUR (máx. 43) | 29 | 777 |
| 48 | DEV – VEL | 58 | no | Acepta | los 31 lugares | **30** | **835** |
| 49 | AEP – BEL | 59 | — | No se evalúa: ya hay 30 aristas | | 30 | 835 |
| 50 | FLO – MAT | 73 | — | No se evalúa | | 30 | 835 |
| 51 | FLO – VEL | 79 | — | No se evalúa | | 30 | 835 |

**Resultado: 30 aristas, 835 minutos.** De las 48 calles evaluadas se aceptaron 30 y se rechazaron
18 por formar ciclo; las 3 últimas ni se miran porque el árbol ya está completo.

Lecturas:
- **La calle de la relajación (HGA – LBO, 51) no entra al árbol:** cuando se evalúa (fila 46) HGA y
  LBO ya están unidos por HGA – CON – LBO (15 + 32). Es la misma razón por la que Dijkstra no la usa.
- **Filas 40 y 48:** recién con MON – SAA se une el bloque del oeste {AGR, DEV, SAA, VUR} al resto
  (29 lugares), y la última calle aceptada, DEV – VEL (58), une el bloque {MAT, VEL} (que se había
  formado en la fila 44) con todo lo demás.
- **El MST es único:** cada calle rechazada cuesta **estrictamente más** que todas las del ciclo que
  cerraría (columna "máx."), por ejemplo BEL – SAA = 44 contra un máximo de 43 en BEL – MON – SAA.
  Lo mismo vale para las 3 no evaluadas: el ciclo de AEP – BEL en el árbol final tiene máximo 40 y
  los de FLO – MAT y FLO – VEL pasan por DEV – VEL (58), todos menores que 59, 73 y 79. Por eso los
  empates de costo (15, 18, 20, 23, 27, 36, 43…) no cambian el resultado.

Comparación con la salida real: el endpoint devuelve exactamente estas 30 calles, en el orden de
aceptación de la tabla, con `costoTotal` 835.0. Lo verifica `ArbolRecubrimientoTest.kruskalDominio`
(y `kruskalAgregaEnOrdenCrecienteDeCosto`, que los costos aparezcan en orden no decreciente).

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
| 1 | RET | — (origen) | 0 | 0 | OBE ∞→23, PMA ∞→36, REC ∞→26 | — |
| 2 | OBE | RET – OBE | 23 | 23 | CGR ∞→18, CON ∞→40, PMA 36→28 (REC 31 no mejora 26) | — |
| 3 | CGR | OBE – CGR | 18 | 41 | ABA ∞→27, CON 40→33 | — |
| 4 | REC | RET – REC | 26 | 67 | PAL ∞→40 (ABA 36 no mejora 27) | — |
| 5 | ABA | CGR – ABA | 27 | 94 | BOE ∞→36, HIT ∞→15 | — |
| 6 | HIT | ABA – HIT | 15 | 109 | PCE ∞→20 (PAL 43 no mejora 40) | — |
| 7 | PCE | HIT – PCE | 20 | 129 | CAB ∞→20, CHA ∞→37 | — |
| 8 | CAB | PCE – CAB | 20 | 149 | BOE 36→27, FLO ∞→43, PCH ∞→29 | — |
| 9 | BOE | CAB – BOE | 27 | 176 | HGA ∞→36 (PCH 35 no mejora 29) | — |
| 10 | PMA | OBE – PMA | 28 | 204 | RES ∞→15, SAN ∞→20 | — |
| 11 | RES | PMA – RES | 15 | 219 | — | — |
| 12 | SAN | PMA – SAN | 20 | 239 | CON 33→18, LBO ∞→34 | — |
| 13 | CON | SAN – CON | 18 | 257 | HGA 36→15, LBO 34→32 | — |
| 14 | HGA | CON – HGA | 15 | 272 | — (LBO 51 no mejora 32) | — |
| 15 | PCH | CAB – PCH | 29 | 301 | FLO 43→36 | — |
| 16 | LBO | CON – LBO | 32 | 333 | — | — |
| 17 | FLO | PCH – FLO | 36 | 369 | MAT ∞→73, VEL ∞→79 | (CON, 33), (LBO, 34), (PMA, 36), (BOE, 36) |
| 18 | CHA | PCE – CHA | 37 | 406 | AGR ∞→45, COL ∞→19 (PAL 43 no mejora 40) | (HGA, 36) |
| 19 | COL | CHA – COL | 19 | 425 | BEL ∞→27, PLA ∞→50, VUR ∞→56 | — |
| 20 | BEL | COL – BEL | 27 | 452 | AEP ∞→59, MON ∞→24, SAA ∞→44 | — |
| 21 | MON | BEL – MON | 24 | 476 | CUN ∞→20, SAA 44→43 | — |
| 22 | CUN | MON – CUN | 20 | 496 | AEP 59→44 | — |
| 23 | PAL | REC – PAL | 40 | 536 | PLA 50→23 | (CON, 40) |
| 24 | PLA | PAL – PLA | 23 | 559 | AEP 44→21 | — |
| 25 | AEP | PLA – AEP | 21 | 580 | — | — |
| 26 | SAA | MON – SAA | 43 | 623 | VUR 56→35 | (FLO, 43) |
| 27 | VUR | SAA – VUR | 35 | 658 | AGR 45→31 | — |
| 28 | AGR | VUR – AGR | 31 | 689 | DEV ∞→43 | — |
| 29 | DEV | AGR – DEV | 43 | 732 | VEL 79→58 | — |
| 30 | VEL | DEV – VEL | 58 | 790 | MAT 73→45 | (SAA, 44), (AEP, 44), (AGR, 45), (PLA, 50), (VUR, 56) |
| 31 | MAT | VEL – MAT | 45 | **835** | — | — |
| — | | | | | | (AEP, 59), (MAT, 73), (VEL, 79): la cola queda vacía |

**Resultado: 30 aristas, 835 minutos.** Orden en que entran los 30 vértices después de RET: OBE,
CGR, REC, ABA, HIT, PCE, CAB, BOE, PMA, RES, SAN, CON, HGA, PCH, LBO, FLO, CHA, COL, BEL, MON, CUN,
PAL, PLA, AEP, SAA, VUR, AGR, DEV, VEL, MAT.

Observaciones:
- **Prim no es "el vecino más cercano del último que entró":** en el paso 4 entra REC colgando de
  RET (que entró en el paso 1), en el paso 10 entra PMA colgando de OBE (paso 2) y en el paso 23 entra
  PAL colgando de REC (paso 4). Siempre se toma la calle más barata que cruza del árbol al resto,
  salga de donde salga.
- **Constitución cambia de padre tres veces:** key[CON] = 40 por OBE (paso 2), 33 por CGR (paso 3) y
  18 por SAN (paso 12). Entra en el paso 13 por SAN – CON; ni OBE – CON ni CGR – CON quedan en el
  árbol (Kruskal también las rechaza: filas 34 y 24).
- **PMA 36 → 28 en el paso 2:** la calle RET – PMA (36) nunca entra al árbol; PMA queda colgando de
  OBE.
- **El oeste entra al final:** el bloque SAA, VUR, AGR, DEV, VEL, MAT se conecta recién desde el
  paso 26, porque las calles que lo unen al resto (MON – SAA 43, COL – VUR 56, AGR – CHA 45,
  FLO – VEL 79…) son de las más caras. VEL entra por DEV – VEL (58) y no por FLO – VEL (79): en el
  paso 29 key[VEL] baja de 79 a 58.
- Conteo de la cola: 46 inserciones (origen + 45 actualizaciones) = 31 vértices que entran + 15
  entradas viejas descartadas.

Comparación con la salida real: el endpoint devuelve las aristas en este mismo orden (RET→OBE,
OBE→CGR, RET→REC, CGR→ABA, …, DEV→VEL, VEL→MAT) con `costoTotal` 835.0. Lo verifica
`ArbolRecubrimientoTest.primDominioDesdeRetiro`.

**Prim y Kruskal dan el mismo árbol:** las 30 calles de la tabla de Prim son las mismas 30 que
aceptó Kruskal (en otro orden), como corresponde a un MST único. El test
`primDominioDaElMismoArbolDesdeOtroOrigen` comprueba además que arrancando desde otro origen se
obtiene el mismo conjunto de calles.

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

La matriz completa es de 31 × 31 = 961 celdas; acá va un extracto de 8 lugares (los de las trazas
de este documento). Fila = origen, columna = destino. Simétrica porque el grafo es no dirigido;
diagonal en 0.

| | RET | OBE | CON | HGA | LBO | HIT | PAL | CUN |
|---|---|---|---|---|---|---|---|---|
| **RET** | 0 | 23 | 63 | 78 | 90 | 77 | 66 | 154 |
| **OBE** | 23 | 0 | 40 | 55 | 72 | 60 | 71 | 159 |
| **CON** | 63 | 40 | 0 | 15 | 32 | 75 | 111 | 199 |
| **HGA** | 78 | 55 | 15 | 0 | 47 | 87 | 126 | 210 |
| **LBO** | 90 | 72 | 32 | 47 | 0 | 107 | 143 | 231 |
| **HIT** | 77 | 60 | 75 | 87 | 107 | 0 | 43 | 131 |
| **PAL** | 66 | 71 | 111 | 126 | 143 | 43 | 0 | 88 |
| **CUN** | 154 | 159 | 199 | 210 | 231 | 131 | 88 | 0 |

`/floyd` devuelve los **961 pares** (el grafo es conexo, no falta ninguno) y la suma de todas las
celdas es **111730**, igual que `FloydWarshallTest.dominioTieneTodosLosParesYDiagonalCero` y
`FloydWarshallTest.sumaDeTodasLasDistancias`. El par más lejano es MAT → CUN = **263** (diámetro
del grafo).

Notar que la matriz de adyacencia de entrada tiene HGA – LBO = 51 (la calle directa,
`MatrizDominioTest.guardaElCostoDirectoAunqueNoSeaElMinimo`), y en la de salida la celda vale 47.

### 5.2 Comparación con Dijkstra (algunos pares)

| Par | Floyd | Dijkstra (camino) | Coincide |
|---|---|---|---|
| HGA → LBO | 47 | HGA – CON – LBO = 15 + 32 (no la directa de 51) | Sí |
| RET → CUN | 154 | RET – REC – PAL – PLA – AEP – CUN = 26 + 40 + 23 + 21 + 44 | Sí |
| PAL → CUN | 88 | PAL – PLA – AEP – CUN = 23 + 21 + 44 | Sí |
| OBE → HIT | 60 | OBE – CGR – ABA – HIT = 18 + 27 + 15 | Sí |
| LBO → HIT | 107 | LBO – CON – CGR – ABA – HIT = 32 + 33 + 27 + 15 | Sí |
| CON → PCE | 95 | CON – CGR – ABA – HIT – PCE = 33 + 27 + 15 + 20 | Sí |
| PCE → PMA | 108 | PCE – HIT – ABA – CGR – OBE – PMA = 20 + 15 + 27 + 18 + 28 | Sí |
| LBO → VEL | 232 | LBO – CON – HGA – BOE – CAB – FLO – VEL = 32 + 15 + 36 + 27 + 43 + 79 | Sí |
| MAT → CUN / CUN → MAT | 263 / 263 | MAT – FLO – CAB – PCE – CHA – COL – BEL – MON – CUN = 73 + 43 + 20 + 37 + 19 + 27 + 24 + 20; simétrico | Sí |

La fila **HGA** del extracto es la misma que la tabla de Dijkstra desde HGA de la
[sección 1](#1-dijkstra-desde-hga) (CON 15, HGA 0, LBO 47, OBE 55, RET 78, HIT 87, PAL 126,
CUN 210).

Para los **961 pares**, `FloydWarshallTest.coincideConDijkstraEnTodosLosPares` compara los costos y
`caminoFloydIgualADijkstraEnTodosLosPares` compara además los vértices del camino reconstruido por
`/floyd/camino` (en el dominio ningún par tiene dos caminos mínimos empatados, así que los dos
algoritmos tienen que dar exactamente el mismo camino). Salidas reales de `/floyd/camino`:
HGA → LBO `["HGA","CON","LBO"]` 47.0 y RET → CUN `["RET","REC","PAL","PLA","AEP","CUN"]` 154.0,
iguales a las de `/dijkstra`.

**Cómo llega Floyd a HGA → LBO = 47.** El orden de k es el de los índices (orden alfabético de
ids): ABA, AEP, AGR, BEL, BOE, CAB, CGR, CHA, COL, **CON** (k = 9), CUN, …
- Al empezar, d[HGA][LBO] = 51 (calle directa) y siguiente[HGA][LBO] = LBO.
- Con k = ABA … COL no mejora: a LBO solo se llega desde CON, HGA o SAN, y ni CON ni SAN están
  todavía habilitados como intermedios.
- Con k = CON: d[HGA][CON] + d[CON][LBO] = 15 + 32 = 47 < 51, entonces d[HGA][LBO] = 47 y
  siguiente[HGA][LBO] = siguiente[HGA][CON] = CON.
- Ningún k posterior baja de 47 (es el mínimo real).

Reconstrucción: HGA → siguiente[HGA][LBO] = CON → siguiente[CON][LBO] = LBO.

### 5.3 Trabajo: Floyd vs. Dijkstra V veces

```
GET /api/grafo/floyd/comparacion
{"vertices":31,"operacionesFloyd":29791,"nodosExpandidosDijkstra":961,"aristasExaminadasDijkstra":3162}
```

| Métrica | Cálculo a mano | Valor |
|---|---|---|
| Comparaciones de Floyd (triple bucle) | V³ = 31³ | 29791 |
| Nodos expandidos por Dijkstra (31 corridas) | V corridas × V nodos (grafo conexo: cada corrida saca a todos) | 961 |
| Aristas examinadas por Dijkstra (31 corridas) | V corridas × 2E entradas de adyacencia = 31 × 102 | 3162 |

Lo verifica `FloydWarshallTest.comparacionConDijkstraEnElDominio`.

Lectura: Floyd hace 29791 comparaciones sin importar cuántas calles haya (depende solo de V).
Dijkstra corrido desde los 31 lugares mira 3162 entradas de adyacencia, unas 9 veces menos, y a
eso se suma el costo del heap: en las 31 corridas hay 1071 inserciones y 1071 extracciones (961
válidas + 110 entradas viejas), con una cola que nunca pasa de 14 elementos (≈ 4 niveles de heap).
Aun contando eso, Dijkstra V veces queda por debajo de Floyd porque el grafo es **ralo**: E = 51
contra 31 · 30 / 2 = 465 calles posibles (≈ 11 %), y el orden V · (V + E) log V crece mucho más
lento que V³ mientras E esté lejos de V². Floyd conviene en grafos densos o cuando hay aristas
negativas (que Dijkstra no admite). Análisis completo en la ficha de
[`/floyd/comparacion`](ENDPOINTS.md#get-apigrafofloydcomparacion).

(Los conteos del heap salen de la misma copia instrumentada de `arbolDesde`; el endpoint no los
devuelve.)

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
| Dominio CABA | HGA → LBO | 60 / 3 | 2 | 9 | 489.963 | 99,998 % |
| Dominio CABA | OBE → HIT | 80 / 3 | 1 | 27 | 367.114 | 99,993 % |
| Dominio CABA | CON → PCE | 120 / 5 | 3 | 72 | 358.249 | 99,98 % |
| Dominio CABA | LBO → HIT | 140 / 5 | 5 | 106 | 522.659 | 99,98 % |
| Dominio CABA | RET → CUN | 150 / 5 | 0 | 153 | 807.257 | 99,98 % |
| Dominio CABA | RET → CUN | 180 / 6 | 1 | 305 | 807.257 | 99,96 % |
| Dominio CABA | RET → CUN | **200 / 8 (defaults del endpoint)** | 3 | 542 | 807.257 | 99,93 % |
| Dominio CABA | RET → CUN | 1000 / 10 | 145 | 6.018 | 807.257 | 99,25 % |

Ahorro = 1 − con poda / sin poda. Todas las filas las verifica `BacktrackingTest` salvo
CON → PCE, que sale de la app (`GET /api/grafo/rutas?origen=CON&destino=PCE&costoMaximo=120&saltosMaximos=5`)
y de la copia instrumentada. Rutas de los casos chicos:
- HGA → LBO (60 / 3): HGA – CON – LBO 47 y HGA – LBO 51 (traza en 6.2).
- CON → PCE (120 / 5): CON – CGR – ABA – HIT – PCE 95, CON – HGA – BOE – CAB – PCE 98 y
  CON – OBE – CGR – ABA – HIT – PCE 120 (exactamente en el límite: la condición es `≤`).
- RET → CUN (200 / 8), en el orden en que las encuentra: RET – OBE – REC – PAL – PLA – AEP – CUN 182,
  RET – REC – PAL – CHA – COL – BEL – MON – CUN 199 y RET – REC – PAL – PLA – AEP – CUN 154.
- LBO → HIT (140 / 5): 107, 134, 132, 138 y 127 minutos (detalle en `BacktrackingTest`).

Lectura:
- **Con y sin poda devuelven las mismas rutas** (lo verifica `conYSinPodaDevuelvenLasMismasRutas`):
  con costos ≥ 0 el costo acumulado nunca baja y los tramos solo suben, así que una rama podada no
  podía terminar en una ruta válida.
- **Sin poda la cantidad no depende de los límites**: para RET → CUN siempre son 807.257 nodos
  (todos los caminos simples que salen de RET sin atravesar CUN, más los que terminan en CUN),
  pidas 150 / 5 o 1000 / 10. Con 31 lugares y 51 calles esa enumeración ya es enorme; por eso el
  ahorro de la poda es de más del 99 % en todos los casos del dominio.
- **Cuanto más holgados los límites, más nodos con poda:** RET → CUN pasa de 153 (150 / 5) a 305
  (180 / 6), 542 (200 / 8) y 6.018 (1000 / 10). Con 1000 / 10 el límite de costo nunca corta
  (10 tramos × 79, la calle más cara, = 790 < 1000): toda la poda viene del límite de 10 tramos.
- **Sin rutas no significa sin trabajo:** con 150 / 5 no hay ninguna ruta (la más rápida tarda 154),
  pero igual se exploran 153 nodos hasta comprobarlo.

### 6.2 Traza a mano: HGA → LBO, costoMaximo = 60, saltosMaximos = 3

Vecinos en el orden de la lista de adyacencia (tabla del encabezado): HGA: BOE 36, CON 15, LBO 51;
CON: CGR 33, HGA 15, LBO 32, OBE 40, SAN 18; SAN: CON 18, LBO 34, PMA 20.

Cada línea numerada es un nodo explorado (`#n`) con el costo acumulado y los tramos entre
paréntesis; `✂` es un hijo que **no se abre** porque `costo + calle > 60` o `tramos + 1 > 3`. Los
vecinos ya visitados (camino simple) no se listan.

```
#1  HGA (0, 0)
#2  ├─ BOE (36, 1)
    │  ├─ ✂ ABA: 36 + 36 = 72
    │  ├─ ✂ CAB: 36 + 27 = 63
    │  └─ ✂ PCH: 36 + 35 = 71
#3  ├─ CON (15, 1)
#4  │  ├─ CGR (48, 2)
    │  │  ├─ ✂ ABA: 48 + 27 = 75
    │  │  └─ ✂ OBE: 48 + 18 = 66
#5  │  ├─ LBO (47, 2)                 RUTA HGA-CON-LBO = 47
#6  │  ├─ OBE (55, 2)
    │  │  ├─ ✂ CGR: 55 + 18 = 73
    │  │  ├─ ✂ PMA: 55 + 28 = 83
    │  │  ├─ ✂ REC: 55 + 31 = 86
    │  │  └─ ✂ RET: 55 + 23 = 78
#7  │  └─ SAN (33, 2)
    │     ├─ ✂ LBO: 33 + 34 = 67
#8  │     └─ PMA (53, 3)
    │        ├─ ✂ OBE: 53 + 28 = 81 y 4 tramos
    │        ├─ ✂ RES: 53 + 15 = 68 y 4 tramos
    │        └─ ✂ RET: 53 + 36 = 89 y 4 tramos
#9  └─ LBO (51, 1)                    RUTA HGA-LBO = 51
```

**Con poda: 9 nodos y 2 rutas**, en el mismo orden que devuelve el algoritmo (test
`garrahanLaBocaDevuelveElAtajoYLaCalleDirecta`): primero HGA – CON – LBO (47) y después la calle
directa HGA – LBO (51), porque CON aparece antes que LBO en la lista de HGA. Se podaron 13 hijos.

**Sin poda: 489.963 nodos.** Cada uno de esos 13 hijos se habría abierto, y debajo de cada uno se
enumeran todos los caminos simples que siguen desde ahí, sin ningún límite. Ninguno da una ruta válida: o llegan a LBO con más de 60 minutos o con más de 3 tramos, o no
llegan. Por eso las dos corridas devuelven las mismas 2 rutas.

**Retroceso:** al volver de #5 se desmarca LBO, y al volver de #3 (después de #4, #6, #7 y #8) se
desmarca CON. Por eso LBO se puede volver a visitar en #9 por otro camino (la calle directa). Si
quedaran marcados, como en un DFS, la segunda ruta no aparecería.

### 6.3 Traza a mano: semilla de la cátedra dirigida, A → H, costoMaximo = 25, saltosMaximos = 10

Caso de la clase, sobre un grafo chico donde también se puede enumerar a mano la corrida sin poda.

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

### 8.1 Traza a mano: RET → PMA

Inicialización: dist[RET] = 0, el resto ∞; cola = {(RET, 0)}. Vecinos en el orden de la lista de
adyacencia (tabla del encabezado).

| Expandido | Se extrae (g) | Relajaciones exitosas | Relajaciones que no mejoran |
|---|---|---|---|
| 1 | RET (0) | OBE 0+23=23, **PMA 0+36=36**, REC 0+26=26 | — |
| 2 | OBE (23) | CGR 23+18=41, CON 23+40=63 | PMA 23+28=51 ≥ 36; REC 23+31=54 ≥ 26 |
| 3 | REC (26) | ABA 26+36=62, PAL 26+40=66 | — |
| 4 | **PMA (36)** | **es el destino: fin** (no se miran sus vecinos) | |

(Se omiten los vecinos ya procesados, que nunca mejoran con costos ≥ 0.)

PMA se **descubre** en el primer paso (calle directa RET – PMA, 36), pero no se puede cortar ahí:
en la cola todavía hay lugares más cercanos (OBE 23, REC 26) por los que podría existir un camino
más barato. Recién cuando PMA **sale** de la cola todo lo que queda cuesta 36 o más, y como los
costos son ≥ 0 ya nada puede mejorarlo. En este caso OBE ofrece 51, que no mejora.

Al cortar quedan en la cola CGR (41), ABA (62), CON (63) y PAL (66) sin expandir, y los otros 23
lugares ni siquiera se descubrieron (dist = ∞). Camino por `previo[]`: PMA ← RET, es decir
**RET → PMA = 36**. Cola: 8 inserciones y 4 extracciones; Dijkstra completo habría expandido los 31.

**HGA → LBO es el caso donde el valor cambia entre que se descubre y que se saca:** son los
primeros 5 pasos de la [sección 1](#1-dijkstra-desde-hga). LBO se descubre con 51 por la calle
directa (expandido 1), baja a 47 por CON (expandido 2) y sale con 47 en el expandido 5, después de
SAN (33) y BOE (36). Si UCS cortara al descubrir, devolvería 51. Por eso corta al sacar.

Salidas reales, coinciden con las trazas:

```
GET /api/grafo/ucs?origen=RET&destino=PMA
{"vertices":["RET","PMA"],"costoTotal":36.0,"nodosExpandidosUcs":4,"nodosExpandidosDijkstra":31}

GET /api/grafo/ucs?origen=HGA&destino=LBO
{"vertices":["HGA","CON","LBO"],"costoTotal":47.0,"nodosExpandidosUcs":5,"nodosExpandidosDijkstra":31}
```

### 8.2 Nodos expandidos: UCS vs. Dijkstra

Regla para calcularlo a mano: UCS expande los lugares que están **más cerca del origen que el
destino**, más el destino. Por ejemplo RET → PMA: RET 0, OBE 23 y REC 26 están a menos de 36, más
PMA = 4; HGA → LBO: HGA 0, CON 15, SAN 33 y BOE 36 están a menos de 47, más LBO = 5 (ver la columna
dist de la sección 1). Si otro lugar empata en distancia con el destino, depende de cuál saque
primero la cola.

| Consulta | Camino | Costo (min) | UCS | Dijkstra | Comentario |
|---|---|---|---|---|---|
| RET → PMA | RET, PMA | 36 | **4** | 31 | Traza 8.1. |
| HGA → LBO | HGA, CON, LBO | 47 | **5** | 31 | Relajación 51 → 47 antes de sacarlo. |
| OBE → HIT | OBE, CGR, ABA, HIT | 60 | **11** | 31 | |
| LBO → HIT | LBO, CON, CGR, ABA, HIT | 107 | **13** | 31 | |
| PAL → CUN | PAL, PLA, AEP, CUN | 88 | **14** | 31 | |
| CON → PCE | CON, CGR, ABA, HIT, PCE | 95 | **16** | 31 | |
| RET → CUN | RET, REC, PAL, PLA, AEP, CUN | 154 | **23** | 31 | |
| LBO → VEL | LBO, CON, HGA, BOE, CAB, FLO, VEL | 232 | **27** | 31 | |
| MAT → CUN | MAT, FLO, CAB, PCE, CHA, COL, BEL, MON, CUN | 263 | 31 | 31 | **Peor caso:** es el par más lejano (diámetro), CUN sale último. |
| RET → RET | RET | 0 | 1 | 31 | El origen es el destino: corta en la primera extracción. |
| Semilla cátedra A → H | A, B, C, D, F, H | 20 | **7** | 8 | Salen A 0, B 4, C 7, D 9, E 14, F 17, H 20; G queda en la cola con 24 (diapositiva de la clase 13). |

`UcsTest` cubre RET → PMA, HGA → LBO, OBE → HIT, MAT → CUN, RET → RET y la semilla, y en cada caso
verifica que camino y costo sean iguales a los de `dijkstra` y que
`nodosExpandidosUcs ≤ nodosExpandidosDijkstra`. Las demás filas son salidas reales del endpoint.

Lectura: cuanto más cerca está el destino, más ahorra UCS (RET → PMA expande 4 de 31). En el peor
caso (destino más lejano o inalcanzable) expande todo y cuesta lo mismo que Dijkstra:
O((V + E) log V).

---

## 9. Lugares infectados (Dijkstra y Backtracking)

Parámetro opcional `infectados` de `/dijkstra` y `/rutas`: lugares que ninguna ruta puede pisar.
Dijkstra no relaja hacia un infectado (nunca entra a la cola); el backtracking los arranca como
visitados. Detalle y complejidad en la ficha de
[`/dijkstra`](ENDPOINTS.md#get-apigrafodijkstra).

### 9.1 Casos calculados a mano (minutos a pie)

| Consulta | Sin infectados | Con infectados | Salida real |
|---|---|---|---|
| HGA → LBO, `infectados=CON` | HGA – CON – LBO = 15 + 32 = 47 | HGA – LBO = **51** (la calle directa) | `{"vertices":["HGA","LBO"],"costoTotal":51.0}` |
| LBO → HIT, `infectados=CON` | LBO – CON – CGR – ABA – HIT = 32 + 33 + 27 + 15 = 107 | LBO – HGA – BOE – ABA – HIT = 51 + 36 + 36 + 15 = **138** | `{"vertices":["LBO","HGA","BOE","ABA","HIT"],"costoTotal":138.0}` |
| LBO → HIT, `infectados=CON,BOE` | 107 | LBO – SAN – PMA – OBE – CGR – ABA – HIT = 34 + 20 + 28 + 18 + 27 + 15 = **142** | `{"vertices":["LBO","SAN","PMA","OBE","CGR","ABA","HIT"],"costoTotal":142.0}` |
| OBE → HIT, `infectados=CGR` | OBE – CGR – ABA – HIT = 18 + 27 + 15 = 60 | OBE – REC – ABA – HIT = 31 + 36 + 15 = **82** | `{"vertices":["OBE","REC","ABA","HIT"],"costoTotal":82.0}` |
| RET → CUN, `infectados=PLA` | RET – REC – PAL – PLA – AEP – CUN = 154 | RET – REC – PAL – CHA – COL – BEL – MON – CUN = 26 + 40 + 43 + 19 + 27 + 24 + 20 = **199** | `{"vertices":["RET","REC","PAL","CHA","COL","BEL","MON","CUN"],"costoTotal":199.0}` |
| OBE → RES, `infectados=PMA` | OBE – PMA – RES = 28 + 15 = 43 | **sin camino** | 422 `No hay camino de OBE a RES` |
| PAL → CUN, `infectados=PAL` | — | origen infectado | 400 `El origen PAL esta infectado: no se puede usar` |
| PAL → CUN, `infectados=XXX` | — | id inexistente | 400 `No existe el nodo con id: XXX` |

Todos los casos están en `LugaresInfectadosTest` (sin Neo4j).

**Por qué HGA → LBO pasa a 51.** En la traza de la sección 1, dist[LBO] arranca en 51 por la calle
directa y baja a 47 cuando se expande CON (paso 2). Con CON infectado, al expandir HGA no se relaja
hacia CON: dist[CON] queda en ∞, CON nunca se extrae y la mejora a 47 no ocurre. El otro acceso a
LBO es por SAN, pero sin CON llegar a SAN obliga a rodear por Puerto Madero y cuesta mucho más. LBO
se queda con 51, previo[LBO] = HGA.

**Por qué LBO → HIT con CON da 138 y con CON, BOE da 142.** LBO tiene tres calles: CON 32, HGA 51
y SAN 34. Sin CON hay dos salidas: por HGA – BOE – ABA – HIT (51 + 36 + 36 + 15 = 138) o por
SAN – PMA – OBE – CGR – ABA – HIT (34 + 20 + 28 + 18 + 27 + 15 = 142); gana la primera, que usa justo
la calle "cara" HGA – LBO. Si además se infecta BOE, la primera desaparece y queda la de 142.

**Por qué OBE → HIT con CGR da 82.** CGR es el paso natural del Obelisco hacia el Abasto
(18 + 27). Sin CGR, a ABA se llega por REC (31 + 36 = 67) y de ahí a HIT (15): 82.

**Por qué RET → CUN con PLA da 199.** CUN tiene dos calles: AEP – CUN y MON – CUN. Sin PLA, a AEP
solo se llega por BEL (59), así que conviene entrar por el Monumental: RET – REC – PAL (66), y de
PAL hacia el norte por CHA – COL – BEL – MON – CUN (43 + 19 + 27 + 24 + 20 = 133): 199.

**Por qué OBE → RES queda sin camino.** La Reserva Ecológica tiene una sola calle, PMA – RES: Puerto
Madero es un **punto de articulación**. Con PMA infectado, RES queda aislado del resto; Dijkstra
termina con dist[RES] = ∞ y responde 422.

### 9.2 Backtracking con un infectado

`GET /api/grafo/rutas?origen=LBO&destino=HIT&costoMaximo=140&saltosMaximos=5&infectados=CON`

| | Rutas | Nodos con poda | Nodos sin poda |
|---|---|---|---|
| Sin infectados | 5 (107, 134, 132, 138, 127) | 106 | 522.659 |
| `infectados=CON` | 1: LBO – HGA – BOE – ABA – HIT (138) | **22** | **71.600** |

Cuatro de las cinco rutas pasaban por CON; queda solo la que sale por la calle directa a HGA, que
es además el camino de Dijkstra con CON infectado (9.1). La alternativa por SAN – PMA – OBE – CGR –
ABA – HIT cuesta 142 > 140 y usa 6 tramos > 5, así que no entra. Lo verifica
`LugaresInfectadosTest.rutasSinConstitucion`.

`GET /api/grafo/rutas?origen=RET&destino=CUN&costoMaximo=200&saltosMaximos=8&infectados=PLA`

| | Rutas | Nodos con poda | Nodos sin poda |
|---|---|---|---|
| Sin infectados | 3 (182, 199, 154) | 542 | 807.257 |
| `infectados=PLA` | 1: RET – REC – PAL – CHA – COL – BEL – MON – CUN (199) | **506** | **246.669** |

Las dos rutas que desaparecen (182 y 154) entraban a CUN por PLA – AEP; queda la de 199, que es
también el camino de Dijkstra con PLA infectado. Salida real del endpoint con los defaults.

En los dos casos con y sin poda devuelven las mismas rutas, y los nodos explorados bajan en las dos
corridas porque el infectado nunca se visita: se eliminan todos los subárboles que colgaban de él,
sin entrar a ninguno. La baja es mucho mayor sin poda (807.257 → 246.669) que con poda
(542 → 506). Los caminos que no tocan PLA se exploran igual en las dos corridas, así que la
diferencia es exactamente lo que colgaba de PLA: sin poda, 560.588 nodos (todos los caminos simples
que pasaban por PLA); con poda, solo 36, porque la mayoría de esas ramas ya se cortaba por costo o
tramos.
