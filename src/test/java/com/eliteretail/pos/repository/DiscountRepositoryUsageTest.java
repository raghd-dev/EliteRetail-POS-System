package com.eliteretail.pos.repository;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscountRepositoryUsageTest {
    @Test
    void recordsEachAppliedIdOnceUsingConditionalSql() throws SQLException {
        List<Integer> updated = new ArrayList<>();
        List<String> sqlQueries = new ArrayList<>();
        Connection connection = fakeConnection(updated, sqlQueries, 1);
        new DiscountRepository().incrementUsage(connection, Set.of(9, 7));
        assertEquals(List.of(7, 9), updated);
        assertEquals(1, sqlQueries.size());
        org.junit.jupiter.api.Assertions.assertTrue(sqlQueries.get(0).contains("times_used < usage_limit"));
    }

    @Test
    void refusesExhaustedDiscountSoCallerCanRollbackSale() {
        Connection connection = fakeConnection(new ArrayList<>(), new ArrayList<>(), 0);
        assertThrows(SQLException.class,
                () -> new DiscountRepository().incrementUsage(connection, Set.of(7)));
    }

    @Test
    void emptyDiscountSetRequiresNoDatabaseWrite() throws SQLException {
        List<Integer> updated = new ArrayList<>();
        new DiscountRepository().incrementUsage(fakeConnection(updated, new ArrayList<>(), 1), Set.of());
        assertEquals(List.of(), updated);
    }

    private static Connection fakeConnection(List<Integer> updated, List<String> sqlQueries,
                                             int rowsAffected) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        sqlQueries.add((String) args[0]);
                        AtomicInteger activeId = new AtomicInteger();
                        return (PreparedStatement) Proxy.newProxyInstance(
                                PreparedStatement.class.getClassLoader(),
                                new Class<?>[]{PreparedStatement.class}, (statement, m, parameters) -> {
                                    if (m.getName().equals("setInt")) {
                                        activeId.set((Integer) parameters[1]);
                                        return null;
                                    }
                                    if (m.getName().equals("executeUpdate")) {
                                        updated.add(activeId.get());
                                        return rowsAffected;
                                    }
                                    if (m.getName().equals("close")) return null;
                                    throw new UnsupportedOperationException(m.getName());
                                });
                    }
                    if (method.getName().equals("close")) return null;
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
