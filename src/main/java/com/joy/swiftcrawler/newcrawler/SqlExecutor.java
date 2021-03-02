package com.joy.swiftcrawler.newcrawler;

import com.joy.swiftcrawler.config.CrawlerConfig;
import com.joy.swiftcrawler.entity.SwiftCode;
import com.joy.swiftcrawler.util.NamedDaemonThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 一个异步的sql执行器，执行批量插入，从一个异步的队列获取swiftCode，每个线程一次批量插入100条记录，并发5个线程。
 */
@SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "SqlNoDataSourceInspection", "SqlResolve", "TryFinallyCanBeTryWithResources"})
@Slf4j
@Component
public class SqlExecutor implements InitializingBean {
    private final ConcurrentLinkedQueue<SwiftCode> swiftCodes = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService executor;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CrawlerConfig config;

    public void addSwiftCode(SwiftCode swiftCode) {
        swiftCodes.add(swiftCode);
    }

    public void shutDown() {
        executor.shutdown();
    }

    @Override
    public void afterPropertiesSet() {
        executor = Executors.newScheduledThreadPool(config.getSqlConcurrentCount(), new NamedDaemonThreadFactory("swiftCodeSqlExecutor"));
        for (int i = 0; i < config.getSqlConcurrentCount(); i++)
            executor.scheduleAtFixedRate(this::executorSql, 100, config.getSqlSleepIntervalMillis(), TimeUnit.MILLISECONDS);
    }

    public void executorSql() {
        List<SwiftCode> codes = new ArrayList<>(128);
        for (int i = 0; i < config.getSqlBatchSize(); i++) {
            SwiftCode swiftCode = swiftCodes.poll();
            if (swiftCode == null) {
                break;
            }
            codes.add(swiftCode);
        }
        if (codes.isEmpty()) {
            return;
        }
        Connection conn = null;
        PreparedStatement pStmt = null;
        try {
            conn = dataSource.getConnection();
            pStmt = conn.prepareStatement("INSERT INTO swift_code (swift_code, country, bank, branch, city, zipcode, address) VALUES (?,?,?,?,?,?,?)");
            for (var code : codes) {
                pStmt.setString(1, code.getSwiftCode());
                pStmt.setString(2, code.getCountry());
                pStmt.setString(3, code.getBank());
                pStmt.setString(4, code.getBranch());
                pStmt.setString(5, code.getCity());
                pStmt.setString(6, code.getZipcode());
                pStmt.setString(7, code.getAddress());
                pStmt.addBatch();
            }
            pStmt.executeBatch();
            log.info("insert [{}] swift codes!", codes.size());
        } catch (SQLException e) {
            log.error("sql exception!", e);
        } finally {
            if (pStmt != null) {
                try {
                    pStmt.close();
                } catch (Exception e) {
                    //noop
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    //noop
                }
            }
        }
    }
}
