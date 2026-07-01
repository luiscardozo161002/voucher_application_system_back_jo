package mx.juarezdeoriente.solicitudes.workers.domain.port;

import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.workers.domain.model.Worker;
import mx.juarezdeoriente.solicitudes.workers.domain.model.WorkerType;

import java.util.Optional;
import java.util.UUID;

public interface WorkerRepository {

    Worker save(Worker worker);

    Optional<Worker> findById(UUID id);

    PageResult<Worker> search(String query, Boolean active, int page, int size);

    PageResult<Worker> search(String query, Boolean active, WorkerType workerType, int page, int size);

    void deleteById(UUID id);
}
