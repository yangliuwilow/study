package com.willow.tx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeptService {

    @Autowired
    public DeptDao deptDao;

    @Transactional(transactionManager = "")
    public void add() {
        deptDao.insert();
        int i=10/0;
    }
}
