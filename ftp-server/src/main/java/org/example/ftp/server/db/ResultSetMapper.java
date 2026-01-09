package org.example.ftp.server.db;

import java.sql.ResultSet;

@FunctionalInterface
public interface ResultSetMapper<T> {
    T map(ResultSet rs) throws Exception;
}
