package com.landit.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * SQLite数据库初始化器
 * 应用启动时自动检测并创建数据库表结构
 *
 * @author Azir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqliteConfig implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            if (!isTableExists()) {
                log.info("检测到数据库表不存在，开始初始化表结构...");
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema.sql"));
                populator.execute(dataSource);
                log.info("数据库表结构初始化完成");
            } else {
                log.info("数据库表已存在，跳过初始化");
            }
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage(), e);
            System.exit(0);
        }
    }

    private boolean isTableExists() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='t_user'"
            );
            return rs.next();
        } catch (Exception e) {
            log.warn("检查表是否存在时出错: {}", e.getMessage());
            return false;
        }
    }

}
