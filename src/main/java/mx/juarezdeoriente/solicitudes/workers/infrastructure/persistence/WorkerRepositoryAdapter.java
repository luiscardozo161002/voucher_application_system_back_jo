package mx.juarezdeoriente.solicitudes.workers.infrastructure.persistence;

import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.workers.domain.model.Worker;
import mx.juarezdeoriente.solicitudes.workers.domain.port.WorkerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class WorkerRepositoryAdapter implements WorkerRepository {

    private final WorkerJpaRepository jpa;

    WorkerRepositoryAdapter(WorkerJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Worker save(Worker w) {
        WorkerJpaEntity e = jpa.findById(w.getId()).orElse(new WorkerJpaEntity());
        e.setId(w.getId());
        e.setCompanyCode(w.getCompanyCode());
        e.setEmployeeNumber(w.getEmployeeNumber());
        e.setName(w.getName());
        e.setPhone(w.getPhone());
        e.setWorkerType(w.getWorkerType());
        e.setActive(w.isActive());
        e.setCreatedAt(w.getCreatedAt());
        e.setUpdatedAt(java.time.Instant.now());
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<Worker> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public PageResult<Worker> search(String query, Boolean active, int page, int size) {
        String pattern = query != null ? "%" + query.toLowerCase() + "%" : null;
        Page<WorkerJpaEntity> p = jpa.search(pattern, active, PageRequest.of(page, size));
        return PageResult.of(p.getContent().stream().map(this::toDomain).toList(),
                page, size, p.getTotalElements());
    }

    private WorkerJpaEntity toEntity(Worker w) {
        WorkerJpaEntity e = new WorkerJpaEntity();
        e.setId(w.getId());
        e.setCompanyCode(w.getCompanyCode());
        e.setEmployeeNumber(w.getEmployeeNumber());
        e.setName(w.getName());
        e.setPhone(w.getPhone());
        e.setWorkerType(w.getWorkerType());
        e.setActive(w.isActive());
        e.setCreatedAt(w.getCreatedAt());
        return e;
    }

    private Worker toDomain(WorkerJpaEntity e) {
        return Worker.reconstitute(e.getId(), e.getCompanyCode(), e.getEmployeeNumber(),
                e.getName(), e.getPhone(), e.getWorkerType(), e.isActive(), e.getCreatedAt());
    }
}
