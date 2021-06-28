package io.github.jzdayz.encryption;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.DES;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EncryptionHelper {
    @Data
    private static class EncryptionObj {
        private String tableName;
        private String[] columnNames;
    }

    private static Map<String, Set<String>> container;

    private static DES DES = SecureUtil.des("123456789".getBytes(StandardCharsets.UTF_8));

    static {
        ClassPathResource cpr = new ClassPathResource("rule.json");
        try (
                InputStream inputStream = cpr.getInputStream();
        ) {
            String json = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
            container = JSON.parseArray(json, EncryptionObj.class)
                    .stream()
                    .collect(Collectors.toMap(EncryptionObj::getTableName, k -> new HashSet<>(Arrays.asList(k.getColumnNames()))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encrypt(String value, String table, String column) {
        Set<String> columns = container.get(table);
        if (columns == null || !columns.contains(column)) {
            return value;
        }
        return DES.encryptHex(value);
    }

    public static String decrypt(String value, String table, String column) {
        Set<String> columns = container.get(table);
        if (columns == null || !columns.contains(column)) {
            return value;
        }
        return DES.decryptStr(value);
    }

}
