package com.cloudpulse.statuspage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusPageRepository extends JpaRepository<StatusPage, Long> {

    List<StatusPage> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<StatusPage> findByIdAndUserId(Long id, Long userId);

    Optional<StatusPage> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT COUNT(s) > 0 FROM StatusPage s WHERE s.slug = :slug AND s.id <> :id")
    boolean existsBySlugAndIdNot(@Param("slug") String slug, @Param("id") Long id);
}
