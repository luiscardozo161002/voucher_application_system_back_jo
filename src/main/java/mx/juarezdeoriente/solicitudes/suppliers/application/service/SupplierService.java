package mx.juarezdeoriente.solicitudes.suppliers.application.service;

import mx.juarezdeoriente.solicitudes.config.CacheConfig;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.ConflictException;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.suppliers.domain.event.SupplierDeletedEvent;
import mx.juarezdeoriente.solicitudes.suppliers.domain.model.Supplier;
import mx.juarezdeoriente.solicitudes.suppliers.domain.port.SupplierRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository     supplierRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SupplierService(SupplierRepository supplierRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.supplierRepository = supplierRepository;
        this.eventPublisher     = eventPublisher;
    }

    @CacheEvict(cacheNames = CacheConfig.SUPPLIERS, allEntries = true)
    public Supplier create(String code, String name, String phone, UUID actorId) {
        if (supplierRepository.existsByCode(code.trim().toUpperCase())) {
            throw new ConflictException("Ya existe un proveedor con la clave: " + code);
        }
        Supplier supplier = Supplier.create(code, name, phone, actorId);
        Supplier saved    = supplierRepository.save(supplier);
        supplier.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return saved;
    }

    @Transactional(readOnly = true)
    public Supplier findById(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proveedor", id));
    }

    @Cacheable(cacheNames = CacheConfig.SUPPLIERS, key = "#query + '-' + #active + '-' + #page + '-' + #size")
    @Transactional(readOnly = true)
    public PageResult<Supplier> search(String query, Boolean active, int page, int size) {
        return supplierRepository.search(query, active, page, size);
    }

    @CacheEvict(cacheNames = CacheConfig.SUPPLIERS, allEntries = true)
    public Supplier update(UUID id, String name, String phone, UUID actorId) {
        Supplier supplier = findById(id);
        supplier.update(name, phone, actorId);
        Supplier saved = supplierRepository.save(supplier);
        supplier.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return saved;
    }

    @CacheEvict(cacheNames = CacheConfig.SUPPLIERS, allEntries = true)
    public void delete(UUID id, UUID actorId) {
        Supplier supplier = findById(id);
        eventPublisher.publishEvent(
                new SupplierDeletedEvent(supplier.getId(), supplier.getCode(), supplier.getName(), actorId));
        supplierRepository.deleteById(id);
    }

    @CacheEvict(cacheNames = CacheConfig.SUPPLIERS, allEntries = true)
    public Supplier setActive(UUID id, boolean active) {
        Supplier supplier = findById(id);
        if (active) supplier.activate(); else supplier.deactivate();
        return supplierRepository.save(supplier);
    }
}
