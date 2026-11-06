package cl.duoc.mesatech.catalogo.repository;

import cl.duoc.mesatech.catalogo.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

	boolean existsByNombreIgnoreCase(String nombre);

	List<Categoria> findByActivaTrueOrderByIdAsc();
}
