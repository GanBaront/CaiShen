package com.caishen.foodlab.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页返回体
 */
@Data
public class PageResult<T> {

    private long total;
    private long page;
    private long size;
    private long pages;
    private List<T> list = new ArrayList<>();

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        result.setList(page.getRecords());
        return result;
    }

    public static <T> PageResult<T> of(long total, long page, long size, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setPages(size <= 0 ? 0 : (total + size - 1) / size);
        result.setList(list);
        return result;
    }
}
