package com.ai.southernquiet.idgenerator;

import ch.qos.logback.core.db.dialect.MySQLDialect;
import ch.qos.logback.core.db.dialect.PostgreSQLDialect;
import instep.dao.sql.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IdGeneratorWorkerTable extends Table {
    public IdGeneratorWorkerTable(String tableName) {
        super(tableName);
    }

    /**
     * 对应id中的workerId部分
     */
    public IntegerColumn workerId = integer("worker_id").primary();

    /**
     * worker的名字，便于查看。
     */
    public StringColumn workerName = varchar("worker_name", 512);

    /**
     * worker上报的时间，用于防止时间回退导致id重复。
     */
    public DateTimeColumn workerTime = instant("worker_time");

    /**
     * worker所在应用的标识，方便应用重启后获取其上次用过的workerId。
     */
    public StringColumn appId = varchar("app_id", 128).unique();

    @SuppressWarnings("unused")
    public static class Cleaner {
        private final static Log log = LogFactory.getLog(Cleaner.class);

        private IdGeneratorWorkerTable workerTable;
        private InstepSQL instepSQL;
        private JdbcIdGeneratorAutoConfiguration.Properties properties;

        public void clearConsiderDowned() {
            SQLPlan plan = workerTable.delete().where(lastWorkerTimePlusIntervalLesserThanNow()).debug();
            int rowAffected;
            try {
                rowAffected = instepSQL.executor().executeUpdate(plan);
            }
            catch (SQLPlanExecutionException e) {
                throw new RuntimeException(e);
            }

            if (rowAffected > 0) {
                log.info(String.format("已清理%s个长时间无上报的Worker", rowAffected));
            }
        }

        private Condition lastWorkerTimePlusIntervalLesserThanNow() {
            Dialect dialect = workerTable.getDialect();

            long interval = properties.getConsiderWorkerDowned().getSeconds();

            if (PostgreSQLDialect.class.isInstance(dialect)) {
                return Condition.Companion.plain(workerTable.workerTime.getName() + "+ INTERVAL '" + interval + " SECONDS') < CURRENT_TIMESTAMP");
            }
            else if (MySQLDialect.class.isInstance(dialect)) {
                return Condition.Companion.plain(
                    "DATE_ADD(" + workerTable.workerTime.getName() +
                        ", INTERVAL " + interval + " SECOND) < CURRENT_TIMESTAMP");
            }
            else {
                throw new UnsupportedOperationException("不支持当前数据库：" + dialect.getClass().getSimpleName());
            }
        }
    }
}
