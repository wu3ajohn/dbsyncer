package org.dbsyncer.parser;

import org.dbsyncer.connector.config.MetaInfo;
import org.dbsyncer.connector.enums.FilterEnum;
import org.dbsyncer.connector.enums.OperationEnum;
import org.dbsyncer.parser.enums.ConvertEnum;
import org.dbsyncer.parser.model.Connector;
import org.dbsyncer.parser.model.Mapping;
import org.dbsyncer.parser.model.TableGroup;

import java.util.List;

/**
 * @author AE86
 * @version 1.0.0
 * @date 2019/10/20 21:11
 */
public interface Parser {

    /**
     * 解析连接器配置是否可用
     *
     * @param json
     * @return
     */
    boolean alive(String json);

    /**
     * 获取表元信息
     *
     * @param connectorId
     * @param tableName
     * @return
     */
    MetaInfo getMetaInfo(String connectorId, String tableName);

    /**
     * 解析连接器配置为Connector
     *
     * @param json
     * @return
     */
    Connector parseConnector(String json);

    /**
     * 解析连接器配置为Connector
     *
     * @param json
     * @param checkAlive
     * @return
     */
    Connector parseConnector(String json, boolean checkAlive);

    /**
     * 解析驱动映射关系配置为Mapping
     *
     * @param json
     * @return
     */
    Mapping parseMapping(String json);

    /**
     * 解析驱动映射关系配置为Mapping
     *
     * @param json
     * @param checkAlive
     * @return
     */
    Mapping parseMapping(String json, boolean checkAlive);

    /**
     * 解析表映射关系
     *
     * @param json
     * @return
     */
    TableGroup parseTableGroup(String json);

    /**
     * 解析表映射关系
     *
     * @param json
     * @param checkAlive
     * @return
     */
    TableGroup parseTableGroup(String json, boolean checkAlive);

    /**
     * 获取所有条件类型
     *
     * @return
     */
    List<OperationEnum> getOperationEnumAll();

    /**
     * 获取所有运算符类型
     *
     * @return
     */
    List<FilterEnum> getFilterEnumAll();

    /**
     * 获取所有转换类型
     *
     * @return
     */
    List<ConvertEnum> getConvertEnumAll();
}