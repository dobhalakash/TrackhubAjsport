package com.dreamnest.repository;

import com.dreamnest.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByTrendingTrueAndActiveTrue();

    Page<Product> findByBusinessUserId(Long businessUserId, Pageable pageable);

    long countByBusinessUserId(Long businessUserId);

    long countByBusinessUserIdAndStockLessThan(Long businessUserId, Integer threshold);

    @Query("SELECT COUNT(DISTINCT p.category.id) FROM Product p WHERE p.businessUser.id = :businessUserId")
    long countDistinctCategoriesByBusinessUserId(@Param("businessUserId") Long businessUserId);

    boolean existsBySku(String sku);

    @Query("SELECT p FROM Product p WHERE p.active = true " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchAndFilter(@Param("categoryId") Long categoryId,
                                   @Param("brand") String brand,
                                   @Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true " +
            "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(p.category.name) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY p.trending DESC, p.name ASC")
    List<Product> findSuggestions(@Param("q") String q, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId AND p.id != :excludeId " +
            "ORDER BY p.trending DESC, p.averageRating DESC")
    List<Product> findRelated(@Param("categoryId") Long categoryId, @Param("excludeId") Long excludeId, Pageable pageable);
}
