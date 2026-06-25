package com.ruoyi.common.core.service;

import java.util.List;

/**
 * 通用 CRUD Service 接口
 * @param <E> 实体类型
 */
public interface BaseCrudService<E> {

    List<E> selectList(E entity);

    E selectById(Long id);

    int insert(E entity);

    int update(E entity);

    int deleteByIds(Long[] ids);
}
