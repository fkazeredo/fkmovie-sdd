package com.fksoft.shared.pagination;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Stable pagination envelope for API responses. Used instead of serializing Spring Data's
 * {@code Page} directly, whose JSON shape is not a committed contract.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Maps a Spring Data {@link Page} into the stable response envelope. */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
