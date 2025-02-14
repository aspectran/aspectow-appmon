package com.aspectran.appmon.persist.counter;

import com.aspectran.appmon.mybatis.mapper.CounterMapper;
import com.aspectran.core.component.bean.annotation.Component;

/**
 * <p>Created: 2025-02-13</p>
 */
@Component
public class CounterPersistDao {

    private final CounterMapper.Dao dao;

    public CounterPersistDao(CounterMapper.Dao dao) {
        this.dao = dao;
    }

    public void insert(String instanceName, long acquired, long current) {
        CounterVO counterVO = new CounterVO();
        counterVO.setInst(instanceName);
        counterVO.setCnt1(acquired);
        counterVO.setCnt2(current);
        dao.insertCounterData(counterVO);
    }

}
