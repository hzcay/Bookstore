package com.example.Bookstore.repository.spec;

import org.springframework.data.jpa.domain.Specification;
import com.example.Bookstore.entity.Book;

public class BookSpecifications {

    public static Specification<Book> active() {
        return (root, cq, cb) -> cb.equal(root.get("status"), 1);
    }

    public static Specification<Book> titleLike(String q) {
        if (q == null || q.isBlank()) return (r, cq, cb) -> cb.conjunction();
        String p = "%" + q.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get("title")), p);
    }

    public static Specification<Book> categoryIdEquals(String id) {
        return (root, cq, cb) -> (id == null || id.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("category").get("categoryId"), id);
    }

    public static Specification<Book> authorIdEquals(String id) {
        return (root, cq, cb) -> (id == null || id.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("author").get("authorId"), id);
    }

    public static Specification<Book> publisherIdEquals(String id) {
        return (root, cq, cb) -> (id == null || id.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("publisher").get("publisherId"), id);
    }

    public static Specification<Book> priceGte(Double v) {
        return (root, cq, cb) -> v == null ? cb.conjunction() : cb.ge(root.get("salePrice"), v);
    }

    public static Specification<Book> priceLte(Double v) {
        return (root, cq, cb) -> v == null ? cb.conjunction() : cb.le(root.get("salePrice"), v);
    }

    public static Specification<Book> inStock(boolean only) {
        return (root, cq, cb) -> !only ? cb.conjunction() : cb.greaterThan(root.get("quantity"), 0);
    }
}

