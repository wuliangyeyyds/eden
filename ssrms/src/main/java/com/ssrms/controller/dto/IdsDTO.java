package com.ssrms.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量操作通用入参
 */
@Data
public class IdsDTO {
    private List<Long> ids;
}
