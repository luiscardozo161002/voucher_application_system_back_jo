package mx.juarezdeoriente.solicitudes.requests;
import mx.juarezdeoriente.solicitudes.requests.RequestStatus;
import mx.juarezdeoriente.solicitudes.requests.Request;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RequestSpecification {

    public static Specification<Request> build(String folioPattern, UUID supplierId,
                                         UUID workerId, Instant from, Instant to,
                                         RequestStatus status, UUID createdBy) {
        return build(folioPattern, supplierId, workerId, from, to, status, createdBy, false);
    }

    public static Specification<Request> build(String folioPattern, UUID supplierId,
                                         UUID workerId, Instant from, Instant to,
                                         RequestStatus status, UUID createdBy,
                                         boolean excludeCancelled) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (folioPattern != null) {
                predicates.add(cb.like(root.get("folio").as(String.class), folioPattern));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplierId"), supplierId));
            }
            if (workerId != null) {
                var items = root.join("items", JoinType.LEFT);
                predicates.add(cb.equal(items.get("workerId"), workerId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else if (excludeCancelled) {
                predicates.add(cb.notEqual(root.get("status"), RequestStatus.CANCELADA));
            }
            if (createdBy != null) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }

            return predicates.isEmpty() ? cb.conjunction()
                                        : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
