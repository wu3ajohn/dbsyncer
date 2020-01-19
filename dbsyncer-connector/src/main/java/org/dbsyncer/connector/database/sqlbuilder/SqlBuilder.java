package org.dbsyncer.connector.database.sqlbuilder;

import org.dbsyncer.connector.config.DatabaseConfig;
import org.dbsyncer.connector.config.Table;
import org.dbsyncer.connector.database.Database;

import java.util.List;

/**
 * SQL生成器
 *
 * @author AE86
 * @version 1.0.0
 * @date 2019/9/26 23:49
 */
public interface SqlBuilder {

    /**
     * 生成SQL
     * @param config
     * @param tableName
     * @param pk
     * @param filedNames
     * @param queryFilter
     * @param database
     * @return
     */
    String buildSql(DatabaseConfig config, String tableName, String pk, List<String> filedNames, String queryFilter, Database database);

}