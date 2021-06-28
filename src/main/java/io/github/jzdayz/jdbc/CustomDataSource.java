package io.github.jzdayz.jdbc;

import io.github.jzdayz.encryption.EncryptionHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.logging.Logger;

@AllArgsConstructor
@Slf4j
@Getter
public class CustomDataSource implements DataSource {

    private DataSource dataSource;

    @Override
    public Connection getConnection() throws SQLException {
        return proxy(getDataSource().getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return proxy(getDataSource().getConnection(username, password));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return getDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        getDataSource().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        getDataSource().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getDataSource().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getDataSource().getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getDataSource().isWrapperFor(iface);
    }

    public static Connection proxy(Connection source) {
        ClassLoader cl = CustomDataSource.class.getClassLoader();
        return (Connection) Proxy.newProxyInstance(cl, source.getClass().getInterfaces(), (proxy, method, args) -> {
            if (method.getName().equals("prepareStatement")) {
                PreparedStatement ps = (PreparedStatement) method.invoke(source, args);
                return (PreparedStatement)Proxy.newProxyInstance(cl, ps.getClass().getInterfaces(), (psProxy, psMethod, psArgs) -> {
                    // TODO 设置变量需要解析sql，因为我们无法知道目前是哪张表的哪个字段正在被设置值
                    if (psMethod.getName().equals("setNString") || psMethod.getName().equals("setString")) {
                        int index = (int) psArgs[0];
                        String value = (String) psArgs[1];
                        // 根据规则加密
                        psArgs = new Object[]{index, encrypt(index, value, null)};
                    } else if ("executeQuery".equals(psMethod.getName())) {
                        ResultSet rs = ps.executeQuery();
                        ResultSetMetaData metaData = rs.getMetaData();
                        return (ResultSet)Proxy.newProxyInstance(cl, rs.getClass().getInterfaces(), (rsProxy, rsMethod, rsArgs) -> {
                            if (rsMethod.getName().equals("getString") || rsMethod.getName().equals("getNString")) {
                                return decrypt(rsArgs[0], (String) rsMethod.invoke(rs, rsArgs), metaData);
                            }
                            return rsMethod.invoke(rs, rsArgs);
                        });
                    }
                    return psMethod.invoke(ps, psArgs);
                });
            }
            return method.invoke(source, args);
        });
    }

    private static Object encrypt(int index, String value, ResultSetMetaData metaData) {
        try {
            String tableName = metaData.getTableName(index);
            String columnName = metaData.getColumnName(index);
            return EncryptionHelper.encrypt(value, tableName, columnName);
        } catch (SQLException throwables) {
            log.error("发送异常，加解密失败，使用原本的值", throwables);
            return value;
        }
    }

    private static Object decrypt(Object index, String value, ResultSetMetaData metaData) {
        try {
            if (index instanceof Integer) {
                String tableName = metaData.getTableName((Integer) index);
                String columnName = metaData.getColumnName((Integer) index);
                return EncryptionHelper.decrypt(value, tableName, columnName);
            }
            // TODO 需要根据列名找index，jdbc的api不支持，这样就不知道当前表。
            // TODO 当然我们可以先统一根据index全部遍历一遍，从1开始到ResultSetMetaData#getColumnCount
            throw new RuntimeException("目前不支持");
        } catch (SQLException throwables) {
            log.error("发送异常，加解密失败，使用原本的值", throwables);
            return value;
        }
    }
}
