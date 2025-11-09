package com.example.inventory.repositories.product_reservations;

import com.example.inventory.repositories.product_reservations.dto.ProductReservedAmount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Repository
public class ProductReservationsRepositoryImp implements ProductReservationsRepository {

    @Autowired
    private DatabaseClient db;


    @Override
    public Flux<ProductReservedAmount> sumReservedAmounts(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Flux.empty();
        }

        var idsSQLVarsMap = new HashMap<String, String>();
        var idsSQLVarNames = new ArrayList<String>();
        for (int i = 0; i < productIds.size(); i++) {
            idsSQLVarsMap.put("id" + i, productIds.get(i));
            idsSQLVarNames.add(":id" + i);
        }

        var sql = """
            SELECT
                product_id,
                COALESCE(SUM(reserved_amount), 0) AS reserved_amount
            FROM product_reservations
            WHERE
                product_id IN (%s)
                AND (status = 'OK' OR status = 'INSUFFICIENT_STOCK')
                AND expires_at > CURRENT_TIMESTAMP
            GROUP BY product_id
        """;
        sql = String.format(sql, String.join(",", idsSQLVarNames));

        var spec = db.sql(sql);
        for (var name: idsSQLVarsMap.keySet()) {
            spec = spec.bind(name, idsSQLVarsMap.get(name));
        }

        return spec
            .map((row, meta) -> new ProductReservedAmount(row.get("product_id", String.class), row.get("reserved_amount", Integer.class)))
            .all()
        ;
    }

    @Override
    public Mono<Void> syncReservations(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Mono.empty();
        }

        var idsSQLVarsMap = new HashMap<String, String>();
        var idsSQLVarNames = new ArrayList<String>();
        for (int i = 0; i < productIds.size(); i++) {
            idsSQLVarsMap.put("id" + i, productIds.get(i));
            idsSQLVarNames.add(":id" + i);
        }

        var sql = """
            UPDATE product_reservations pr
            JOIN (
              SELECT
                pr_inner.id,
                pr_inner.product_id,
                pr_inner.desired_amount,
                pr_inner.expires_at,
                pr_inner.updated_at,
                p.stock,
                COALESCE(
                  SUM(
                    CASE
                      WHEN pr_inner.expires_at > CURRENT_TIMESTAMP() AND pr_inner.status != 'EXPIRED'
                        THEN pr_inner.desired_amount
                      ELSE 0
                    END
                  ) OVER (
                    PARTITION BY pr_inner.product_id
                    ORDER BY pr_inner.updated_at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                  ), 0
                ) AS reservation_accumulation
              FROM product_reservations pr_inner
              JOIN products p ON p.id = pr_inner.product_id
              WHERE pr_inner.product_id IN (%s)
            ) AS a ON pr.id = a.id
            SET
              pr.reserved_amount = CASE
                WHEN a.expires_at <= CURRENT_TIMESTAMP() THEN 0
                WHEN a.stock <= a.reservation_accumulation THEN 0
                WHEN a.stock < a.reservation_accumulation + a.desired_amount
                  THEN a.stock - a.reservation_accumulation
                ELSE a.desired_amount
              END,
              pr.status = CASE
                WHEN a.expires_at <= CURRENT_TIMESTAMP() THEN 'EXPIRED'
                WHEN a.stock <= a.reservation_accumulation THEN 'INSUFFICIENT_STOCK'
                WHEN a.stock < a.reservation_accumulation + a.desired_amount THEN 'INSUFFICIENT_STOCK'
                ELSE 'OK'
              END,
              pr.updated_at = CURRENT_TIMESTAMP()
            WHERE pr.product_id IN (%s)
        """;
        sql = String.format(sql,
            String.join(",", idsSQLVarNames),
            String.join(",", idsSQLVarNames)
        );

        var spec = db.sql(sql);
        for (var name: idsSQLVarsMap.keySet()) {
            spec = spec.bind(name, idsSQLVarsMap.get(name));
        }

        return spec.fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Void> removeZeroAmountReservations(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Mono.empty();
        }

        var idsSQLVarsMap = new HashMap<String, String>();
        var idsSQLVarNames = new ArrayList<String>();
        for (int i = 0; i < productIds.size(); i++) {
            idsSQLVarsMap.put("id" + i, productIds.get(i));
            idsSQLVarNames.add(":id" + i);
        }

        var sql = """
            DELETE FROM product_reservations
            WHERE product_id IN (%s) AND desired_amount = 0
        """;
        sql = String.format(sql, String.join(",", idsSQLVarNames));

        var spec = db.sql(sql);
        for (var name: idsSQLVarsMap.keySet()) {
            spec = spec.bind(name, idsSQLVarsMap.get(name));
        }

        return spec.fetch().rowsUpdated().then();
    }
}
