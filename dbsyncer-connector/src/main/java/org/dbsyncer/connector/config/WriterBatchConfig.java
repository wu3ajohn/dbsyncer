package org.dbsyncer.connector.config;

import java.util.List;
import java.util.Map;

public class WriterBatchConfig {

    /**
     * 表名
     */
    private String tableName;
    /**
     * 事件
     */
    private String event;
    /**
     * 执行命令
     */
    private Map<String, String> command;
    /**
     * 字段信息
     */
    private List<Field> fields;
    /**
     * 集合数据
     */
    private List<Map> data;
    /**
     * 强制更新
     */
    private boolean isForceUpdate;

    public WriterBatchConfig(String tableName, String event, Map<String, String> command, List<Field> fields, List<Map> data) {
        this(tableName, event, command, fields, data, false);
    }

    public WriterBatchConfig(String tableName, String event, Map<String, String> command, List<Field> fields, List<Map> data, boolean isForceUpdate) {
        this.tableName = tableName;
        this.event = event;
        this.command = command;
        this.fields = fields;
        this.data = data;
        this.isForceUpdate = isForceUpdate;
    }

    public String getTableName() {
        return tableName;
    }

    public String getEvent() {
        return event;
    }

    public Map<String, String> getCommand() {
        return command;
    }

    public List<Field> getFields() {
        return fields;
    }

    public List<Map> getData() {
        return data;
    }

    public boolean isForceUpdate() {
        return isForceUpdate;
    }
}