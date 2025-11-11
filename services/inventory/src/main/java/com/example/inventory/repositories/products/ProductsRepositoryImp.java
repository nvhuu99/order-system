package com.example.inventory.repositories.products;

import com.example.inventory.repositories.products.dto.ListProductsRequest;
import com.example.inventory.repositories.products.entities.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
public class ProductsRepositoryImp implements ProductsRepository {

    @Autowired
    private R2dbcEntityTemplate template;

    @Override
    public Flux<Product> list(ListProductsRequest request) {
        var criteria = Criteria.empty();
        var nameSearch = request.getNameSearch();
        if (nameSearch != null && !nameSearch.isBlank()) {
            criteria = criteria.and("name").like("%" + nameSearch + "%");
        }

        var query = Query
            .query(criteria)
            .limit(request.getLimit())
            .offset((request.getPage() - 1) * request.getLimit())
            .sort(Sort.by(Sort.Order.asc("id")))
        ;

        return template.select(query, Product.class);
    }
}
