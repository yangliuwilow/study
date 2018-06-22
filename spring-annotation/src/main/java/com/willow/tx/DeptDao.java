package com.willow.tx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public class DeptDao {

    @Autowired
    public JdbcTemplate jdbcTemplate;

    public void  insert(){
        String sql="insert into department (department_name) values(?)";
        String username = UUID.randomUUID().toString().substring(0, 5);
        jdbcTemplate.update(sql,username);
    }
}
