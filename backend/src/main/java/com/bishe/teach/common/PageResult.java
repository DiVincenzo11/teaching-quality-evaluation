package com.bishe.teach.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private Long   total;
    private Long   pages;
    private Long   current;
    private Long   size;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> pr = new PageResult<>();
        pr.total   = page.getTotal();
        pr.pages   = page.getPages();
        pr.current = page.getCurrent();
        pr.size    = page.getSize();
        pr.records = page.getRecords();
        return pr;
    }
}
