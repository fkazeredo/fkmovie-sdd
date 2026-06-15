package com.fksoft.domain.screening;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Movie} (SPEC-0008). Module-internal. */
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    /**
     * Paginated catalog search: optional status filter and case-insensitive title contains.
     * {@code search} is an empty string (not null) when unset — a null bound parameter inside
     * {@code lower(...)} is untyped and Postgres rejects it as {@code lower(bytea)}.
     */
    @Query(
            """
            select m from Movie m
            where (:status is null or m.status = :status)
              and (:search = '' or lower(m.title) like lower(concat('%', :search, '%')))
            """)
    Page<Movie> search(@Param("status") MovieStatus status, @Param("search") String search, Pageable pageable);
}
