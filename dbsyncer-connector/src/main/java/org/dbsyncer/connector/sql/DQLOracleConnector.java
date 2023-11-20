package org.dbsyncer.connector.sql;

import org.dbsyncer.sdk.config.ReaderConfig;
import org.dbsyncer.sdk.connector.database.AbstractDQLConnector;
import org.dbsyncer.sdk.constant.DatabaseConstant;
import org.dbsyncer.sdk.model.PageSql;
import org.springframework.stereotype.Component;

@Component
public final class DQLOracleConnector extends AbstractDQLConnector {

    private final String TYPE = "DqlOracle";

    @Override
    public String getConnectorType() {
        return TYPE;
    }

    @Override
    public String getPageSql(PageSql config) {
        return DatabaseConstant.ORACLE_PAGE_SQL_START + config.getQuerySql() + DatabaseConstant.ORACLE_PAGE_SQL_END;
    }

    @Override
    public Object[] getPageArgs(ReaderConfig config) {
        int pageSize = config.getPageSize();
        int pageIndex = config.getPageIndex();
        return new Object[]{pageIndex * pageSize, (pageIndex - 1) * pageSize};
    }

    @Override
    public String buildSqlWithQuotation() {
        return "\"";
    }

    @Override
    public String getValidationQuery() {
        return "select 1 from dual";
    }

}