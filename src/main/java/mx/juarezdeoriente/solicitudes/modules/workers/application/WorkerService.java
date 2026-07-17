package mx.juarezdeoriente.solicitudes.modules.workers.application;
import mx.juarezdeoriente.solicitudes.exception.DomainException;
import mx.juarezdeoriente.solicitudes.modules.workers.application.WorkerEvents;
import mx.juarezdeoriente.solicitudes.modules.workers.application.dto.WorkerDto;
import mx.juarezdeoriente.solicitudes.modules.workers.infrastructure.WorkerRepository;
import mx.juarezdeoriente.solicitudes.modules.workers.domain.WorkerType;
import mx.juarezdeoriente.solicitudes.modules.workers.domain.Worker;

import mx.juarezdeoriente.solicitudes.config.CacheConfig;
import mx.juarezdeoriente.solicitudes.exception.NotFoundException;
import mx.juarezdeoriente.solicitudes.shared.PageResult;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
                         String name, String phone, WorkerType workerType, UUID actorId) {
        Worker worker = Worker.create(companyCode, employeeNumber, name, phone, workerType);
        Worker saved  = workerRepository.save(worker);
        eventPublisher.publishEvent(new WorkerEvents.Created(saved.getId(), saved.getName(), actorId));
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
        return search(query, active, null, page, size);
    }

    @Transactional(readOnly = true)
    public PageResult<Worker> search(String query, Boolean active, WorkerType workerType, int page, int size) {
        String pattern = query != null ? "%" + query.toLowerCase() + "%" : null;
        Page<Worker> p = workerRepository.search(pattern, active, workerType, PageRequest.of(page, size));
        return PageResult.of(p.getContent(), page, size, p.getTotalElements());
    }

    @CacheEvict(cacheNames = CacheConfig.WORKERS, allEntries = true)
    public Worker update(UUID id, String companyCode, String employeeNumber,
                         String name, String phone, WorkerType workerType, UUID actorId) {
        Worker worker = findById(id);
        worker.update(companyCode, employeeNumber, name, phone, workerType);
        Worker saved = workerRepository.save(worker);
        eventPublisher.publishEvent(new WorkerEvents.Updated(saved.getId(), saved.getName(), actorId));
        return saved;
    }

    @CacheEvict(cacheNames = CacheConfig.WORKERS, allEntries = true)
    public void delete(UUID id, UUID actorId) {
        Worker worker = findById(id);
        eventPublisher.publishEvent(
                new WorkerEvents.Deleted(worker.getId(), worker.getEmployeeNumber(), worker.getName(), actorId));
        workerRepository.deleteById(id);
    }

    @CacheEvict(cacheNames = CacheConfig.WORKERS, allEntries = true)
    public Worker setActive(UUID id, boolean active) {
        Worker worker = findById(id);
        worker.toggleActive(active);
        return workerRepository.save(worker);
    }
}
