package mx.juarezdeoriente.solicitudes.workers.application.service;

import mx.juarezdeoriente.solicitudes.config.CacheConfig;
import mx.juarezdeoriente.solicitudes.shared.domain.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.workers.domain.model.Worker;
import mx.juarezdeoriente.solicitudes.workers.domain.model.WorkerType;
import mx.juarezdeoriente.solicitudes.workers.domain.port.WorkerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WorkerService(WorkerRepository workerRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.workerRepository = workerRepository;
        this.eventPublisher   = eventPublisher;
    }

    @CacheEvict(cacheNames = CacheConfig.WORKERS, allEntries = true)
    public Worker create(String companyCode, String employeeNumber,
                         String name, String phone, WorkerType workerType) {
        Worker worker = Worker.create(companyCode, employeeNumber, name, phone, workerType);
        Worker saved  = workerRepository.save(worker);
        worker.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return saved;
    }

    @Transactional(readOnly = true)
    public Worker findById(UUID id) {
        return workerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trabajador", id));
    }

    @Cacheable(cacheNames = CacheConfig.WORKERS, key = "#query + '-' + #active + '-' + #page + '-' + #size")
    @Transactional(readOnly = true)
    public PageResult<Worker> search(String query, Boolean active, int page, int size) {
        return workerRepository.search(query, active, page, size);
    }

    @CacheEvict(cacheNames = CacheConfig.WORKERS, allEntries = true)
    public Worker update(UUID id, String companyCode, String employeeNumber,
                         String name, String phone, WorkerType workerType) {
        Worker worker = findById(id);
        worker.update(companyCode, employeeNumber, name, phone, workerType);
        Worker saved = workerRepository.save(worker);
        worker.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return saved;
    }

    @CacheEvict(cacheNames = CacheConfig.WORKERS, allEntries = true)
    public Worker setActive(UUID id, boolean active) {
        Worker worker = findById(id);
        if (active) worker.activate(); else worker.deactivate();
        return workerRepository.save(worker);
    }
}
