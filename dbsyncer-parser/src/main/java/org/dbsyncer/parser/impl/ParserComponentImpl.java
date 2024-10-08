/**
 * DBSyncer Copyright 2020-2024 All Rights Reserved.
 */
package org.dbsyncer.parser.impl;

import org.dbsyncer.common.model.Result;
import org.dbsyncer.common.util.CollectionUtils;
import org.dbsyncer.common.util.StringUtil;
import org.dbsyncer.connector.base.ConnectorFactory;
import org.dbsyncer.parser.ParserComponent;
import org.dbsyncer.parser.ProfileComponent;
import org.dbsyncer.parser.event.FullRefreshEvent;
import org.dbsyncer.parser.model.BatchWriter;
import org.dbsyncer.parser.model.Connector;
import org.dbsyncer.parser.model.FieldMapping;
import org.dbsyncer.parser.model.Mapping;
import org.dbsyncer.parser.model.Picker;
import org.dbsyncer.parser.model.TableGroup;
import org.dbsyncer.parser.model.Task;
import org.dbsyncer.parser.strategy.FlushStrategy;
import org.dbsyncer.parser.util.ConvertUtil;
import org.dbsyncer.parser.util.PickerUtil;
import org.dbsyncer.plugin.PluginFactory;
import org.dbsyncer.plugin.enums.ProcessEnum;
import org.dbsyncer.plugin.impl.FullPluginContext;
import org.dbsyncer.sdk.config.CommandConfig;
import org.dbsyncer.sdk.config.ReaderConfig;
import org.dbsyncer.sdk.config.WriterBatchConfig;
import org.dbsyncer.sdk.connector.ConnectorInstance;
import org.dbsyncer.sdk.constant.ConnectorConstant;
import org.dbsyncer.sdk.model.ConnectorConfig;
import org.dbsyncer.sdk.model.Field;
import org.dbsyncer.sdk.model.MetaInfo;
import org.dbsyncer.sdk.model.Table;
import org.dbsyncer.sdk.plugin.PluginContext;
import org.dbsyncer.sdk.util.PrimaryKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2019/9/29 22:38
 */
@Component
public class ParserComponentImpl implements ParserComponent {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private ConnectorFactory connectorFactory;

    @Resource
    private PluginFactory pluginFactory;

    @Resource
    private FlushStrategy flushStrategy;

    @Resource
    private ProfileComponent profileComponent;

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public MetaInfo getMetaInfo(String connectorId, String tableName) {
        Connector connector = profileComponent.getConnector(connectorId);
        ConnectorInstance connectorInstance = connectorFactory.connect(connector.getConfig());
        MetaInfo metaInfo = connectorFactory.getMetaInfo(connectorInstance, tableName);
        if (!CollectionUtils.isEmpty(connector.getTable())) {
            for (Table t : connector.getTable()) {
                if (t.getName().equals(tableName)) {
                    metaInfo.setTableType(t.getType());
                    metaInfo.setSql(t.getSql());
                    break;
                }
            }
        }
        return metaInfo;
    }

    @Override
    public Map<String, String> getCommand(Mapping mapping, TableGroup tableGroup) {
        ConnectorConfig sConnConfig = getConnectorConfig(mapping.getSourceConnectorId());
        ConnectorConfig tConnConfig = getConnectorConfig(mapping.getTargetConnectorId());
        Table sourceTable = tableGroup.getSourceTable();
        Table targetTable = tableGroup.getTargetTable();
        Table sTable = sourceTable.clone().setColumn(new ArrayList<>());
        Table tTable = targetTable.clone().setColumn(new ArrayList<>());
        List<FieldMapping> fieldMapping = tableGroup.getFieldMapping();
        if (!CollectionUtils.isEmpty(fieldMapping)) {
            fieldMapping.forEach(m -> {
                if (null != m.getSource()) {
                    sTable.getColumn().add(m.getSource());
                }
                if (null != m.getTarget()) {
                    tTable.getColumn().add(m.getTarget());
                }
            });
        }
        final CommandConfig sourceConfig = new CommandConfig(sConnConfig.getConnectorType(), sTable, sConnConfig, tableGroup.getFilter());
        final CommandConfig targetConfig = new CommandConfig(tConnConfig.getConnectorType(), tTable, tConnConfig, null);
        // 获取连接器同步参数
        return connectorFactory.getCommand(sourceConfig, targetConfig);
    }

    @Override
    public long getCount(String connectorId, Map<String, String> command) {
        ConnectorInstance connectorInstance = connectorFactory.connect(getConnectorConfig(connectorId));
        return connectorFactory.getCount(connectorInstance, command);
    }

    @Override
    public void execute(Task task, Mapping mapping, TableGroup tableGroup, Executor executor) {
        final String metaId = task.getId();
        final String sourceConnectorId = mapping.getSourceConnectorId();
        final String targetConnectorId = mapping.getTargetConnectorId();

        ConnectorConfig sConfig = getConnectorConfig(sourceConnectorId);
        Assert.notNull(sConfig, "数据源配置不能为空.");
        ConnectorConfig tConfig = getConnectorConfig(targetConnectorId);
        Assert.notNull(tConfig, "目标源配置不能为空.");
        TableGroup group = PickerUtil.mergeTableGroupConfig(mapping, tableGroup);
        Map<String, String> command = group.getCommand();
        Assert.notEmpty(command, "执行命令不能为空.");
        List<FieldMapping> fieldMapping = group.getFieldMapping();
        Table sourceTable = group.getSourceTable();
        String sTableName = sourceTable.getName();
        String tTableName = group.getTargetTable().getName();
        Assert.notEmpty(fieldMapping, String.format("数据源表[%s]同步到目标源表[%s], 映射关系不能为空.", sTableName, tTableName));
        // 获取同步字段
        Picker picker = new Picker(fieldMapping);
        List<String> primaryKeys = PrimaryKeyUtil.findTablePrimaryKeys(sourceTable);
        boolean supportedCursor = StringUtil.isNotBlank(command.get(ConnectorConstant.OPERTION_QUERY_CURSOR));
        int pageSize = mapping.getReadNum();
        int batchSize = mapping.getBatchNum();
        final ConnectorInstance sConnectorInstance = connectorFactory.connect(sConfig);
        final ConnectorInstance tConnectorInstance = connectorFactory.connect(tConfig);
        final String event = ConnectorConstant.OPERTION_INSERT;
        final FullPluginContext context = new FullPluginContext(sConnectorInstance, tConnectorInstance, sTableName, tTableName, event, group.getPluginExtInfo());
        // 0、插件前置处理
        pluginFactory.process(group.getPlugin(), context, ProcessEnum.BEFORE);

        for (; ; ) {
            if (!task.isRunning()) {
                logger.warn("任务被中止:{}", metaId);
                break;
            }

            // 1、获取数据源数据
            ReaderConfig readerConfig = new ReaderConfig(sourceTable, command, new ArrayList<>(), supportedCursor, task.getCursors(), task.getPageIndex(), pageSize);
            Result reader = connectorFactory.reader(sConnectorInstance, readerConfig);
            List<Map> source = reader.getSuccessData();
            if (CollectionUtils.isEmpty(source)) {
                logger.info("完成全量同步任务:{}, [{}] >> [{}]", metaId, sTableName, tTableName);
                break;
            }

            // 2、映射字段
            List<Map> target = picker.pickTargetData(source);

            // 3、参数转换
            ConvertUtil.convert(group.getConvert(), target);

            // 4、插件转换
            context.setSourceList(source);
            context.setTargetList(target);
            pluginFactory.process(group.getPlugin(), context, ProcessEnum.CONVERT);

            // 5、写入目标源
            BatchWriter batchWriter = new BatchWriter(tConnectorInstance, command, tTableName, event, picker.getTargetFields(), target, batchSize, mapping.isForceUpdate());
            Result result = writeBatch(context, batchWriter, executor);

            // 6、更新结果
            task.setPageIndex(task.getPageIndex() + 1);
            task.setCursors(PrimaryKeyUtil.getLastCursors(source, primaryKeys));
            result.setTableGroupId(tableGroup.getId());
            result.setTargetTableGroupName(tTableName);
            flush(task, result);

            // 7、同步完成后通知插件做后置处理
            pluginFactory.process(group.getPlugin(), context, ProcessEnum.AFTER);

            // 8、判断尾页
            if (source.size() < pageSize) {
                logger.info("完成全量:{}, [{}] >> [{}]", metaId, sTableName, tTableName);
                break;
            }
        }
    }

    @Override
    public Result writeBatch(PluginContext pluginContext, BatchWriter batchWriter, Executor executor) {
        final Result result = new Result();
        // 终止同步数据到目标源库
        if (pluginContext.isTerminated()) {
            result.getSuccessData().addAll(batchWriter.getDataList());
            return result;
        }

        List<Map> dataList = batchWriter.getDataList();
        int batchSize = batchWriter.getBatchSize();
        String tableName = batchWriter.getTableName();
        String event = batchWriter.getEvent();
        Map<String, String> command = batchWriter.getCommand();
        List<Field> fields = batchWriter.getFields();
        // 总数
        int total = dataList.size();
        // 单次任务
        if (total <= batchSize) {
            return connectorFactory.writer(batchWriter.getConnectorInstance(), new WriterBatchConfig(tableName, event, command, fields, dataList, batchWriter.isForceUpdate()));
        }

        // 批量任务, 拆分
        int taskSize = total % batchSize == 0 ? total / batchSize : total / batchSize + 1;

        final CountDownLatch latch = new CountDownLatch(taskSize);
        int fromIndex = 0;
        int toIndex = batchSize;
        for (int i = 0; i < taskSize; i++) {
            final List<Map> data;
            if (toIndex > total) {
                toIndex = fromIndex + (total % batchSize);
                data = dataList.subList(fromIndex, toIndex);
            } else {
                data = dataList.subList(fromIndex, toIndex);
                fromIndex += batchSize;
                toIndex += batchSize;
            }

            executor.execute(() -> {
                try {
                    Result w = connectorFactory.writer(batchWriter.getConnectorInstance(), new WriterBatchConfig(tableName, event, command, fields, data, batchWriter.isForceUpdate()));
                    result.addSuccessData(w.getSuccessData());
                    result.addFailData(w.getFailData());
                    result.getError().append(w.getError());
                } catch (Exception e) {
                    logger.error(e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        return result;
    }

    /**
     * 更新缓存
     *
     * @param task
     * @param result
     */
    private void flush(Task task, Result result) {
        flushStrategy.flushFullData(task.getId(), result, ConnectorConstant.OPERTION_INSERT);

        // 发布刷新事件给FullExtractor
        task.setEndTime(Instant.now().toEpochMilli());
        applicationContext.publishEvent(new FullRefreshEvent(applicationContext, task));
    }

    /**
     * 获取连接配置
     *
     * @param connectorId
     * @return
     */
    private ConnectorConfig getConnectorConfig(String connectorId) {
        return profileComponent.getConnector(connectorId).getConfig();
    }

}