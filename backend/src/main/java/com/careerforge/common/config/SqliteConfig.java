package com.careerforge.common.config;

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
import java.util.HashSet;
import java.util.Set;

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
                // 检查并添加新字段（数据库迁移）
                migrateResumeScoreColumns();
            }
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage(), e);
            System.exit(0);
        }
    }

    /**
     * 迁移简历评分字段（批量检查并添加）
     * 优化：单次 PRAGMA 调用检查所有列
     */
    private void migrateResumeScoreColumns() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 单次 PRAGMA 调用获取所有现有列
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(t_resume)");
            Set<String> existingColumns = new HashSet<>();
            while (rs.next()) {
                existingColumns.add(rs.getString("name").toLowerCase());
            }
            rs.close();

            // 定义需要添加的列
            String[][] columnsToAdd = {
                    {"overall_score", "INTEGER DEFAULT 0"},
                    {"content_score", "INTEGER DEFAULT 0"},
                    {"structure_score", "INTEGER DEFAULT 0"},
                    {"matching_score", "INTEGER DEFAULT 0"},
                    {"competitiveness_score", "INTEGER DEFAULT 0"}
            };

            // 批量添加缺失的列
            for (String[] column : columnsToAdd) {
                String columnName = column[0];
                String columnDef = column[1];
                if (!existingColumns.contains(columnName.toLowerCase())) {
                    String sql = "ALTER TABLE t_resume ADD COLUMN " + columnName + " " + columnDef;
                    stmt.execute(sql);
                    log.info("成功添加字段: t_resume.{}", columnName);
                }
            }
        } catch (Exception e) {
            log.warn("迁移简历评分字段时出错: {}", e.getMessage());
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
