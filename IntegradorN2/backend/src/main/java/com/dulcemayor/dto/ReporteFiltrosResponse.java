package com.dulcemayor.dto;

import java.util.List;

public record ReporteFiltrosResponse(
        List<ReporteOpcionResponse> departamentos,
        List<ReporteOpcionResponse> preventores) {}
