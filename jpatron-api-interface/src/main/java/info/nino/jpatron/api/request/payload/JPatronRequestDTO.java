package info.nino.jpatron.api.request.payload;

import info.nino.jpatron.api.request.JPatronApiRequest;

import java.util.Set;

public record JPatronRequestDTO(Integer pageSize,
                                Integer pageNumber,
                                Set<JPatronSort> sorts,
                                Set<JPatronFilter> filters,
                                Set<JPatronDistinct> distincts,
                                Set<JPatronMeta> metas) {

    public record JPatronSort(String columnPath, JPatronApiRequest.SortDirection direction) {

    }

    public record JPatronFilter(String columnPath, JPatronApiRequest.Comparator comparator, String[] values) {

    }

    public record JPatronDistinct(String name, String valuePath, String labelPath) {

    }

    public record JPatronMeta(String name, String valuePath, JPatronApiRequest.Function function, String[] labelPaths) {

    }
}
