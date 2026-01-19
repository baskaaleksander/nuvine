package com.baskaaleksander.nuvine.integration.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public abstract class BaseRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected void truncateTables(String... tables) {
        for (String table : tables) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE");
        }
    }

}
