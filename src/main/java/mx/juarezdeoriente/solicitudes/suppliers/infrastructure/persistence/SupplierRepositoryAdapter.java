package mx.juarezdeoriente.solicitudes.suppliers.infrastructure.persistence;

import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.suppliers.domain.model.Supplier;
import mx.juarezdeoriente.solicitudes.suppliers.domain.port.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class SupplierRepositoryAdapter implements SupplierRepository {

    private final SupplierJpaRepository jpa;

    SupplierRepositoryAdapter(SupplierJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Supplier save(Supplier s) {
        SupplierJpaEntity e = jpa.findById(s.getId()).orElse(new SupplierJpaEntity());
        e.setId(s.getId());
        e.setCode(s.getCode());
        e.setName(s.getName());
        e.setPhone(s.getPhone());
        e.setActive(s.isActive());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(java.time.Instant.now());
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<Supplier> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Supplier> findByCode(String code) {
        return jpa.findByCode(code).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public PageResult<Supplier> search(String query, Boolean active, int page, int size) {
        String pattern = query != null ? "%" + query.toLowerCase() + "%" : null;
        Page<SupplierJpaEntity> p = jpa.search(pattern, active, PageRequest.of(page, size));
        return PageResult.of(p.getContent().stream().map(this::toDomain).toList(),
                page, size, p.getTotalElements());
    }

    private SupplierJpaEntity toEntity(Supplier s) {
        SupplierJpaEntity e = new SupplierJpaEntity();
        e.setId(s.getId());
        e.setCode(s.getCode());
        e.setName(s.getName());
        e.setPhone(s.getPhone());
        e.setActive(s.isActive());
        e.setCreatedAt(s.getCreatedAt());
        return e;
    }

    private Supplier toDomain(SupplierJpaEntity e) {
        return Supplier.reconstitute(e.getId(), e.getCode(), e.getName(),
                e.getPhone(), e.isActive(), e.getCreatedAt());
    }
}
