package mx.juarezdeoriente.solicitudes.requests.infrastructure.web.dto;

import java.util.List;

public record RequestStatsResponse(
        long borradores,
        long emitidas,
        long canceladas,
        List<RequestResponse> recent
) {}
