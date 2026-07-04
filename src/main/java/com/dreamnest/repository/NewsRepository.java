package com.dreamnest.repository;

import com.dreamnest.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsRepository extends JpaRepository<News, Long> {

    Page<News> findByActiveTrueOrderByPublishDateDesc(Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.active = true " +
            "AND (:category IS NULL OR LOWER(n.category) = LOWER(:category)) " +
            "ORDER BY n.publishDate DESC")
    Page<News> findByCategory(@Param("category") String category, Pageable pageable);
}
