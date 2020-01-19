package org.dbsyncer.parser.model;

import org.dbsyncer.connector.config.ConnectorConfig;

import java.util.List;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2019/10/1 12:40
 */
public class Connector extends ConfigModel{

    /**
     * 表名,["MY_USER", "T_MY_USER", "table_999"]
     */
    private List<String> table;

    /**
     * 连接器配置
     */
    private ConnectorConfig config;

    public List<String> getTable() {
        return table;
    }

    public Connector setTable(List<String> table) {
        this.table = table;
        return this;
    }

    public ConnectorConfig getConfig() {
        return config;
    }

    public Connector setConfig(ConnectorConfig config) {
        this.config = config;
        return this;
    }
}
