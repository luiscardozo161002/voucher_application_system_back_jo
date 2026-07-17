package mx.juarezdeoriente.solicitudes.suppliers;
import mx.juarezdeoriente.solicitudes.exception.DomainException;
import mx.juarezdeoriente.solicitudes.suppliers.SupplierEvents;
import mx.juarezdeoriente.solicitudes.suppliers.dto.SupplierDto;
import mx.juarezdeoriente.solicitudes.suppliers.SupplierRepository;
import mx.juarezdeoriente.solicitudes.suppliers.Supplier;

import mx.juarezdeoriente.solicitudes.config.CacheConfig;
import mx.juarezdeoriente.solicitudes.exception.ConflictException;
import mx.juarezdeoriente.solicitudes.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.shared.PageResult;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository     repository;
    private final ApplicationEventPublisher events;
    private final CacheManager           cacheManager;

    public SupplierService(SupplierRepository repository,
                           ApplicationEventPublisher events,
                           CacheManager cacheManager) {
        this.repository   = repository;
        this.events       = events;
        this.cacheManager = cacheManager;
    }

    public Supplier create(String code, String name, String phone, UUID actorId) {
        if (repository.existsByCode(code.trim().toUpperCase()))
            throw new ConflictException("Ya existe un proveedor con la clave: " + code);

        Supplier supplier = Supplier.create(code, name, phone);
        repository.save(supplier);
        evictCache();
        events.publishEvent(new SupplierEvents.Created(supplier.getId(), supplier.getCode(), supplier.getName(), actorId));
        return supplier;
    }

    @Transactional(readOnly = true)
    public Supplier findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proveedor", id));
    }

    @Cacheable(value = CacheConfig.SUPPLIERS, key = "#q + '-' + #active + '-' + #page + '-' + #size")
    @Transactional(readOnly = true)
    public PageResult<Supplier> search(String q, Boolean active, int page, int size) {
        String pattern = (q != null && !q.isBlank()) ? "%" + q.toLowerCase() + "%" : null;
        var pageResult = repository.search(pattern, active,
                PageRequest.of(page, size, Sort.by("name").ascending()));
        return PageResult.of(pageResult.getContent(), page, size, pageResult.getTotalElements());
    }

    public Supplier update(UUID id, String name, String phone, UUID actorId) {
        Supplier supplier = findById(id);
        supplier.update(name, phone);
        repository.save(supplier);
        evictCache();
        events.publishEvent(new SupplierEvents.Updated(supplier.getId(), supplier.getName(), actorId));
        return supplier;
    }

    public Supplier setActive(UUID id, boolean active) {
        Supplier supplier = findById(id);
        supplier.toggleActive(active);
        repository.save(supplier);
        evictCache();
        return supplier;
    }

    public void delete(UUID id, UUID actorId) {
        Supplier supplier = findById(id);
        events.publishEvent(new SupplierEvents.Deleted(supplier.getId(), supplier.getCode(), supplier.getName(), actorId));
        repository.deleteById(id);
        evictCache();
    }

    private void evictCache() {
        Cache cache = cacheManager.getCache(CacheConfig.SUPPLIERS);
        if (cache != null) cache.clear();
    }
}
