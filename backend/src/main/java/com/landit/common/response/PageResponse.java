package com.landit.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应封装类
 *
 * @author Azir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer pages;

    public static <T> PageResponse<T> of(List<T> list, Long total, Integer page, Integer size) {
        int pages = (int) Math.ceil((double) total / size);
        return PageResponse.<T>builder()
                .list(list)
                .total(total)
                .page(page)
                .size(size)
                .pages(pages)
                .build();
    }

}
