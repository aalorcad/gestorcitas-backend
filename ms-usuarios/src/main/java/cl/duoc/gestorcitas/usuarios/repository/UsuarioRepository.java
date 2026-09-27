package cl.duoc.gestorcitas.usuarios.repository;

import cl.duoc.gestorcitas.usuarios.entity.RolUsuario;
import cl.duoc.gestorcitas.usuarios.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByOid(String oid);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Todos los usuarios (sin filtro de texto). */
    List<Usuario> findAllByOrderByNombreAsc();

    /** Usuarios de un rol (sin filtro de texto). */
    @Query("""
            select distinct u from Usuario u join u.roles r
            where r = :rol
            order by u.nombre
            """)
    List<Usuario> listarPorRol(@Param("rol") RolUsuario rol);

    /**
     * Búsqueda por nombre o email. {@code patron} llega ya armado, escapado y en minúsculas
     * desde el servicio (ej. "%soto%"). El escape explícito '!' evita que Hibernate agregue
     * {@code escape ''}, que en Oracle equivale a NULL y anularía el LIKE.
     */
    @Query("""
            select distinct u from Usuario u
            where lower(u.nombre) like :patron escape '!'
               or lower(u.email)  like :patron escape '!'
            order by u.nombre
            """)
    List<Usuario> buscar(@Param("patron") String patron);

    @Query("""
            select distinct u from Usuario u join u.roles r
            where r = :rol
              and (lower(u.nombre) like :patron escape '!'
                or lower(u.email)  like :patron escape '!')
            order by u.nombre
            """)
    List<Usuario> buscarPorRol(@Param("rol") RolUsuario rol, @Param("patron") String patron);

    /** Médicos activos con especialidad asignada (agendables). */
    @Query("""
            select distinct u from Usuario u join u.roles r
            where r = cl.duoc.gestorcitas.usuarios.entity.RolUsuario.MEDICO
              and u.activo = true
              and u.perfilMedico.especialidadId is not null
            order by u.nombre
            """)
    List<Usuario> findMedicosAgendables();

    @Query("""
            select distinct u from Usuario u join u.roles r
            where r = cl.duoc.gestorcitas.usuarios.entity.RolUsuario.MEDICO
              and u.activo = true
              and u.perfilMedico.especialidadId = :especialidadId
            order by u.nombre
            """)
    List<Usuario> findMedicosAgendablesPorEspecialidad(@Param("especialidadId") Long especialidadId);

    @Query("select count(distinct u) from Usuario u join u.roles r where r = :rol")
    long contarPorRol(@Param("rol") RolUsuario rol);

    @Query("select count(distinct u) from Usuario u join u.roles r where r = :rol and u.activo = true")
    long contarActivosPorRol(@Param("rol") RolUsuario rol);

    long countByActivoFalse();
}
