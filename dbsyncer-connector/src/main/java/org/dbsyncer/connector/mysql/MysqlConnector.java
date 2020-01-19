package org.dbsyncer.connector.mysql;

import org.dbsyncer.connector.config.DatabaseConfig;
import org.dbsyncer.connector.constant.DatabaseConstant;
import org.dbsyncer.connector.database.AbstractDatabaseConnector;

public final class MysqlConnector extends AbstractDatabaseConnector {

    @Override
    public String getMetaSql(DatabaseConfig config, String tableName) {
        return new StringBuilder().append("SELECT * FROM ").append(tableName).toString();
    }

    @Override
    public String getPageSql(DatabaseConfig config, String tableName, String pk, String querySQL) {
        // Mysql 分页查询
        return querySQL + DatabaseConstant.MYSQL_PAGE_SQL;
    }

}