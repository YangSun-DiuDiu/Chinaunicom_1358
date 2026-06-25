package com.ruoyi.common.core.service.impl;

import com.ruoyi.common.core.service.BaseCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 通用 CRUD Service 实现基类
 * @param <M> Mapper 类型
 * @param <E> 实体类型
 */
public abstract class BaseCrudServiceImpl<M, E> implements BaseCrudService<E> {

    @Autowired
    protected M mapper;

    protected abstract List<E> doSelectList(E entity);

    protected abstract E doSelectById(Long id);

    protected abstract int doInsert(E entity);

    protected abstract int doUpdate(E entity);

    protected abstract int doDeleteByIds(Long[] ids);

    @Override
    public List<E> selectList(E entity) {
        return doSelectList(entity);
    }

    @Override
    public E selectById(Long id) {
        return doSelectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(E entity) {
        return doInsert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(E entity) {
        return doUpdate(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids) {
        return doDeleteByIds(ids);
    }
}
