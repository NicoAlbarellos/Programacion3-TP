package uade.prog3.tpo.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import uade.prog3.tpo.model.Nodo;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio BLOQUEANTE (no reactivo).
 *
 * Se eligio Neo4jRepository y no ReactiveNeo4jRepository a proposito: en esta
 * materia el foco esta en los algoritmos, y mezclar Mono/Flux agrega
 * complejidad que no aporta nada al objetivo de aprendizaje.
 *
 * Lo usa la carga inicial para guardar los nodos con sus conexiones.
 * Para ARMAR el grafo en memoria GrafoService no usa findAll() (no garantiza
 * orden): hace consultas Cypher ordenadas por id para que la carga sea
 * determinista.
 */
@Repository
public interface NodoRepository extends Neo4jRepository<Nodo, String> {

    Optional<Nodo> findByNombre(String nombre);

    List<Nodo> findByTipo(String tipo);
}
