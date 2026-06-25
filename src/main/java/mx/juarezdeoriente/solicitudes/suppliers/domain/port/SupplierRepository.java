package mx.juarezdeoriente.solicitudes.suppliers.domain.port;

import mx.juarezdeoriente.solicitudes.shared.domain.model.PageResult;
import mx.juarezdeoriente.solicitudes.suppliers.domain.model.Supplier;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(UUID id);

    Optional<Supplier> findByCode(String code);

    boolean existsByCode(String code);

    PageResult<Supplier> search(String query, Boolean active, int page, int size);
}
