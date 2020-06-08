package org.dbsyncer.listener.oracle.dcn;

import oracle.jdbc.OracleStatement;
import oracle.jdbc.dcn.*;
import oracle.jdbc.driver.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import oracle.sql.ROWID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Properties;

/**
 * @version 1.0.0
 * @Author AE86
 * @Date 2020-06-08 21:53
 */
public class OracleRemoteClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private OracleConnection conn;
    private OracleStatement statement;
    private DatabaseChangeRegistration dcr;

    public void init() throws SQLException {
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setUser("admin");
        dataSource.setPassword("admin");
        dataSource.setURL("jdbc:oracle:thin:@127.0.0.1:1521:orcl");
        conn = (OracleConnection) dataSource.getConnection();

        // 配置监听参数
        Properties prop = new Properties();
        prop.setProperty(OracleConnection.NTF_TIMEOUT, "0");
        prop.setProperty(OracleConnection.DCN_NOTIFY_ROWIDS, "true");
        prop.setProperty(OracleConnection.DCN_IGNORE_UPDATEOP,"true");
        prop.setProperty(OracleConnection.DCN_IGNORE_INSERTOP,"true");
        prop.setProperty(OracleConnection.DCN_IGNORE_DELETEOP, "true");

        statement = (OracleStatement) conn.createStatement();
        dcr = conn.registerDatabaseChangeNotification(prop);
        dcr.addListener(new DataBaseChangeListener());
        statement.setDatabaseChangeRegistration(dcr);

        // 监听的表
        statement.executeQuery("select * from USER t where 1=2");

        statement.close();
        conn.close();
        logger.info("数据库更改通知开启");
    }

    public void close() throws SQLException {
        if(null != statement){
            statement.close();
        }

        if(null != conn){
            conn.unregisterDatabaseChangeNotification(dcr);
            conn.close();
        }
    }

    final class DataBaseChangeListener implements DatabaseChangeListener {

        @Override
        public void onDatabaseChangeNotification(DatabaseChangeEvent event) {
            TableChangeDescription[] tds = event.getTableChangeDescription();
            logger.info("=============================");

            logger.info("'TableChangeDescription'(数据表的变化次数):{}", tds.length);
            for (TableChangeDescription td : tds) {
                logger.info("数据库表id：{}", td.getObjectNumber());
                logger.info("数据表名称：{}", td.getTableName());

                // 获得返回的行级变化描述通知 行id、影响这一行的DML操作(行是插入、更新或删除的一种)
                RowChangeDescription[] rds = td.getRowChangeDescription();
                for (RowChangeDescription rd : rds) {
                    RowChangeDescription.RowOperation rowOperation = rd.getRowOperation();
                    logger.info("数据库表行级变化：", rowOperation.toString());

                    ROWID rowid = rd.getRowid();
                    logger.info(rowid.stringValue());
                }
            }
        }
    }

    public static void main(String[] args) throws SQLException {
        OracleRemoteClient client = new OracleRemoteClient();
        client.init();
        client.close();
    }

}
