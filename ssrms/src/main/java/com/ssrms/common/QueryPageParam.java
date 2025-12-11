package com.ssrms.common;

import lombok.Data;

import java.util.HashMap;

@Data
public class QueryPageParam {
    // 默认页大小 & 默认页码
    private static int PAGE_SIZE = 10;
    private static int PAGE_NUM = 1;

    // 当前页大小 & 当前页码
    private int pageSize;
    private int pageNum;

    private HashMap param = new HashMap();
}
