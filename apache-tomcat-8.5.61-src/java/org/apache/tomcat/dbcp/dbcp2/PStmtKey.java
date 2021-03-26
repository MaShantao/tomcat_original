/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.dbcp.dbcp2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.apache.tomcat.dbcp.dbcp2.PoolingConnection.StatementType;

/**
 * A key uniquely identifying {@link java.sql.PreparedStatement PreparedStatement}s.
 *
 * @since 2.0
 */
public class PStmtKey {

    /**
     * Builder for prepareCall(String sql).
     */
    private class PreparedCallSQL implements StatementBuilder {
        @Override
        public Statement createStatement(final Connection connection) throws SQLException {
            return connection.prepareCall(sql);
        }
    }

    /**
     * Builder for prepareCall(String sql, int resultSetType, int resultSetConcurrency).
     */
    private class PreparedCallWithResultSetConcurrency implements StatementBuilder {
        @Override
        public Statement createStatement(final Connection connection) throws SQLException {
            return connection.prepareCall(sql, resultSetType.intValue(), resultSetConcurrency.intValue());
        }
    }

    /**
     * Builder for prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability).
     */
    private class PreparedCallWithResultSetHoldability implements StatementBuilder {
        @Override
        public Statement createStatement(final Connection connection) throws SQLException {
            return connection.prepareCall(sql, resultSetType.intValue(), resultSetConcurrency.intValue(),
                    resultSetHoldability.intValue());
        }
    }

    /**
     * Builder for prepareStatement(String sql).
     */
    private class PreparedStatementSQL implements StatementBuilder {
        @Override
        public Statement createStatement(final Connection connection) throws SQLException {
            return connection.prepareStatement(sql);
        }
    }

    /**
     * Builder for prepareStatement(String sql, int autoGeneratedKeys).
     */
    private class PreparedStatementWithAutoGeneratedKeys implements StatementBuilder {
        @Override
        public Statement createStatement(final Connection connection) throws SQLException {
            return connection.prepareStatement(sql, autoGeneratedKeys.intValue());
        }
    }

    /**
     * Builder for prepareStatement(String sql, int[] columnIndexes).
     */
    private class PreparedStatementWithColumnIndexes implements StatementBuilder {
        @Override
        public Statement createStatement(final Connection connection) throws SQLException {
            return connection.prepareStatement(sql, columnIndexes);
        }
    }

    /**
     * Builder for prepareStatement(String sql, String[] columnNames).
     */
    private class PreparedStatementWithColumnNames implements StatementBuilder {
        @Override
        public Statement createStatement(final Connection connection) throws SQLException {
            return connection.prepareStatement(sql, columnNames);
        }
    }

    /**
     * Builder for prepareStatement(String sql, int resultSetType, int resultSetConcurrency).
     */
    private class PreparedStatementWithResultSetConcurrency implements StatementBuilder {
        @Override
        public Statement createStatement(final Connection connection) throws SQLException {
            return connection.prepareStatement(sql, resultSetType.intValue(), resultSetConcurrency.intValue());
        }
    }

    /**
     * Builder for prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability).
     */
    private class PreparedStatementWithResultSetHoldability implements StatementBuilder {
        @Override
        public Statement createStatement(final Connection connection) throws SQLException {
            return connection.prepareStatement(sql, resultSetType.intValue(), resultSetConcurrency.intValue(),
                    resultSetHoldability.intValue());
        }
    }

    /**
     * Interface for Prepared or Callable Statement.
     */
    private interface StatementBuilder {
        Statement createStatement(Connection connection) throws SQLException;
    }

    /**
     * SQL defining Prepared or Callable Statement
     */
    private final String sql;

    /**
     * Result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>, <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>,
     * or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>.
     */
    private final Integer resultSetType;

    /**
     * Result set concurrency. A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     * <code>ResultSet.CONCUR_UPDATABLE</code>.
     */
    private final Integer resultSetConcurrency;

    /**
     * Result set holdability. One of the following <code>ResultSet</code> constants:
     * <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>.
     */
    private final Integer resultSetHoldability;

    /**
     * Database catalog.
     */
    private final String catalog;

    /**
     * Database schema.
     */
    private final String schema;

    /**
     * A flag indicating whether auto-generated keys should be returned; one of
     * <code>Statement.RETURN_GENERATED_KEYS</code> or <code>Statement.NO_GENERATED_KEYS</code>.
     */
    private final Integer autoGeneratedKeys;

    /**
     * An array of column indexes indicating the columns that should be returned from the inserted row or rows.
     */
    private final int[] columnIndexes;

    /**
     * An array of column names indicating the columns that should be returned from the inserted row or rows.
     */
    private final String[] columnNames;

    /**
     * Statement type, prepared or callable.
     */
    private final StatementType statementType;

    /**
     * Statement builder
     */
    private transient StatementBuilder builder;

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @deprecated Use {@link #PStmtKey(String, String, String)}.
     */
    @Deprecated
    public PStmtKey(final String sql) {
        this(sql, null, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql                  The SQL statement.
     * @param resultSetType        A result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>.
     * @param resultSetConcurrency A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>.
     * @deprecated Use {@link #PStmtKey(String, String, String, int, int)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final int resultSetType, final int resultSetConcurrency) {
        this(sql, null, resultSetType, resultSetConcurrency, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql     The SQL statement.
     * @param catalog The catalog.
     * @deprecated Use {@link #PStmtKey(String, String, String)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog) {
        this(sql, catalog, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql               The SQL statement.
     * @param catalog           The catalog.
     * @param autoGeneratedKeys A flag indicating whether auto-generated keys should be returned; one of
     *                          <code>Statement.RETURN_GENERATED_KEYS</code> or <code>Statement.NO_GENERATED_KEYS</code>.
     * @deprecated Use {@link #PStmtKey(String, String, String, int)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int autoGeneratedKeys) {
        this(sql, catalog, StatementType.PREPARED_STATEMENT, Integer.valueOf(autoGeneratedKeys));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql                  The SQL statement.
     * @param catalog              The catalog.
     * @param resultSetType        A result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>.
     * @param resultSetConcurrency A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>.
     * @deprecated Use @link {@link #PStmtKey(String, String, String, int, int)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int resultSetType, final int resultSetConcurrency) {
        this(sql, catalog, resultSetType, resultSetConcurrency, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql                  The SQL statement.
     * @param catalog              The catalog.
     * @param resultSetType        a result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>.
     * @param resultSetConcurrency A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>
     * @param resultSetHoldability One of the following <code>ResultSet</code> constants: <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code>
     *                             or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>.
     * @deprecated Use {@link #PStmtKey(String, String, String, int, int, int)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int resultSetType, final int resultSetConcurrency,
                    final int resultSetHoldability) {
        this(sql, catalog, resultSetType, resultSetConcurrency, resultSetHoldability, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql                  The SQL statement.
     * @param catalog              The catalog.
     * @param resultSetType        a result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @param resultSetConcurrency A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>.
     * @param resultSetHoldability One of the following <code>ResultSet</code> constants: <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code>
     *                             or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>.
     * @param statementType        The SQL statement type, prepared or callable.
     * @deprecated Use {@link #PStmtKey(String, String, String, int, int, int, PoolingConnection.StatementType)}
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int resultSetType, final int resultSetConcurrency,
                    final int resultSetHoldability, final StatementType statementType) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = null;
        this.resultSetType = Integer.valueOf(resultSetType);
        this.resultSetConcurrency = Integer.valueOf(resultSetConcurrency);
        this.resultSetHoldability = Integer.valueOf(resultSetHoldability);
        this.statementType = statementType;
        this.autoGeneratedKeys = null;
        this.columnIndexes = null;
        this.columnNames = null;
        // create builder
        if (statementType == StatementType.PREPARED_STATEMENT) {
            this.builder = new PreparedStatementWithResultSetHoldability();
        } else if (statementType == StatementType.CALLABLE_STATEMENT) {
            this.builder = new PreparedCallWithResultSetHoldability();
        }
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql                  The SQL statement.
     * @param catalog              The catalog.
     * @param resultSetType        A result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>.
     * @param resultSetConcurrency A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>.
     * @param statementType        The SQL statement type, prepared or callable.
     * @deprecated Use {@link #PStmtKey(String, String, String, int, int, PoolingConnection.StatementType)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int resultSetType, final int resultSetConcurrency,
                    final StatementType statementType) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = null;
        this.resultSetType = Integer.valueOf(resultSetType);
        this.resultSetConcurrency = Integer.valueOf(resultSetConcurrency);
        this.resultSetHoldability = null;
        this.statementType = statementType;
        this.autoGeneratedKeys = null;
        this.columnIndexes = null;
        this.columnNames = null;
        // create builder
        if (statementType == StatementType.PREPARED_STATEMENT) {
            this.builder = new PreparedStatementWithResultSetConcurrency();
        } else if (statementType == StatementType.CALLABLE_STATEMENT) {
            this.builder = new PreparedCallWithResultSetConcurrency();
        }
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql           The SQL statement.
     * @param catalog       The catalog.
     * @param columnIndexes An array of column indexes indicating the columns that should be returned from the inserted row or
     *                      rows.
     * @deprecated Use {@link #PStmtKey(String, String, String, int[])}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int[] columnIndexes) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = null;
        this.statementType = StatementType.PREPARED_STATEMENT;
        this.autoGeneratedKeys = null;
        this.columnIndexes = columnIndexes == null ? null : Arrays.copyOf(columnIndexes, columnIndexes.length);
        this.columnNames = null;
        this.resultSetType = null;
        this.resultSetConcurrency = null;
        this.resultSetHoldability = null;
        // create builder
        this.builder = new PreparedStatementWithColumnIndexes();
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql           The SQL statement.
     * @param catalog       The catalog.
     * @param statementType The SQL statement type, prepared or callable.
     * @deprecated Use {@link #PStmtKey(String, String, String, PoolingConnection.StatementType)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final StatementType statementType) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = null;
        this.statementType = statementType;
        this.autoGeneratedKeys = null;
        this.columnIndexes = null;
        this.columnNames = null;
        this.resultSetType = null;
        this.resultSetConcurrency = null;
        this.resultSetHoldability = null;
        // create builder
        if (statementType == StatementType.PREPARED_STATEMENT) {
            this.builder = new PreparedStatementSQL();
        } else if (statementType == StatementType.CALLABLE_STATEMENT) {
            this.builder = new PreparedCallSQL();
        }
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql               The SQL statement.
     * @param catalog           The catalog.
     * @param statementType     The SQL statement type, prepared or callable.
     * @param autoGeneratedKeys A flag indicating whether auto-generated keys should be returned; one of
     *                          <code>Statement.RETURN_GENERATED_KEYS</code> or <code>Statement.NO_GENERATED_KEYS</code>.
     * @deprecated Use {@link #PStmtKey(String, String, String, PoolingConnection.StatementType, Integer)}
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final StatementType statementType,
                    final Integer autoGeneratedKeys) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = null;
        this.statementType = statementType;
        this.autoGeneratedKeys = autoGeneratedKeys;
        this.columnIndexes = null;
        this.columnNames = null;
        this.resultSetType = null;
        this.resultSetConcurrency = null;
        this.resultSetHoldability = null;
        // create builder
        if (statementType == StatementType.PREPARED_STATEMENT) {
            this.builder = new PreparedStatementWithAutoGeneratedKeys();
        } else if (statementType == StatementType.CALLABLE_STATEMENT) {
            this.builder = new PreparedCallSQL();
        }
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql     The SQL statement.
     * @param catalog The catalog.
     * @param schema  The schema
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema) {
        this(sql, catalog, schema, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql               The SQL statement.
     * @param catalog           The catalog.
     * @param schema            The schema
     * @param autoGeneratedKeys A flag indicating whether auto-generated keys should be returned; one of
     *                          <code>Statement.RETURN_GENERATED_KEYS</code> or <code>Statement.NO_GENERATED_KEYS</code>.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int autoGeneratedKeys) {
        this(sql, catalog, schema, StatementType.PREPARED_STATEMENT, Integer.valueOf(autoGeneratedKeys));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql                  The SQL statement.
     * @param catalog              The catalog.
     * @param schema               The schema
     * @param resultSetType        A result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>.
     * @param resultSetConcurrency A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>.
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int resultSetType, final int resultSetConcurrency) {
        this(sql, catalog, schema, resultSetType, resultSetConcurrency, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql                  The SQL statement.
     * @param catalog              The catalog.
     * @param schema               The schema
     * @param resultSetType        a result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>.
     * @param resultSetConcurrency A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>
     * @param resultSetHoldability One of the following <code>ResultSet</code> constants: <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code>
     *                             or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int resultSetType, final int resultSetConcurrency,
                    final int resultSetHoldability) {
        this(sql, catalog, schema, resultSetType, resultSetConcurrency, resultSetHoldability, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql                  The SQL statement.
     * @param catalog              The catalog.
     * @param schema               The schema.
     * @param resultSetType        a result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @param resultSetConcurrency A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>.
     * @param resultSetHoldability One of the following <code>ResultSet</code> constants: <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code>
     *                             or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>.
     * @param statementType        The SQL statement type, prepared or callable.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int resultSetType, final int resultSetConcurrency,
                    final int resultSetHoldability, final StatementType statementType) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = schema;
        this.resultSetType = Integer.valueOf(resultSetType);
        this.resultSetConcurrency = Integer.valueOf(resultSetConcurrency);
        this.resultSetHoldability = Integer.valueOf(resultSetHoldability);
        this.statementType = statementType;
        this.autoGeneratedKeys = null;
        this.columnIndexes = null;
        this.columnNames = null;
        // create builder
        if (statementType == StatementType.PREPARED_STATEMENT) {
            this.builder = new PreparedStatementWithResultSetHoldability();
        } else if (statementType == StatementType.CALLABLE_STATEMENT) {
            this.builder = new PreparedCallWithResultSetHoldability();
        }
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql                  The SQL statement.
     * @param catalog              The catalog.
     * @param schema               The schema.
     * @param resultSetType        A result set type; one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *                             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>.
     * @param resultSetConcurrency A concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                             <code>ResultSet.CONCUR_UPDATABLE</code>.
     * @param statementType        The SQL statement type, prepared or callable.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int resultSetType, final int resultSetConcurrency,
                    final StatementType statementType) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = schema;
        this.resultSetType = Integer.valueOf(resultSetType);
        this.resultSetConcurrency = Integer.valueOf(resultSetConcurrency);
        this.resultSetHoldability = null;
        this.statementType = statementType;
        this.autoGeneratedKeys = null;
        this.columnIndexes = null;
        this.columnNames = null;
        // create builder
        if (statementType == StatementType.PREPARED_STATEMENT) {
            this.builder = new PreparedStatementWithResultSetConcurrency();
        } else if (statementType == StatementType.CALLABLE_STATEMENT) {
            this.builder = new PreparedCallWithResultSetConcurrency();
        }
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql           The SQL statement.
     * @param catalog       The catalog.
     * @param schema        The schema.
     * @param columnIndexes An array of column indexes indicating the columns that should be returned from the inserted row or
     *                      rows.
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int[] columnIndexes) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = schema;
        this.statementType = StatementType.PREPARED_STATEMENT;
        this.autoGeneratedKeys = null;
        this.columnIndexes = columnIndexes == null ? null : Arrays.copyOf(columnIndexes, columnIndexes.length);
        this.columnNames = null;
        this.resultSetType = null;
        this.resultSetConcurrency = null;
        this.resultSetHoldability = null;
        // create builder
        this.builder = new PreparedStatementWithColumnIndexes();
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql           The SQL statement.
     * @param catalog       The catalog.
     * @param schema        The schema.
     * @param statementType The SQL statement type, prepared or callable.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final StatementType statementType) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = schema;
        this.statementType = statementType;
        this.autoGeneratedKeys = null;
        this.columnIndexes = null;
        this.columnNames = null;
        this.resultSetType = null;
        this.resultSetConcurrency = null;
        this.resultSetHoldability = null;
        // create builder
        if (statementType == StatementType.PREPARED_STATEMENT) {
            this.builder = new PreparedStatementSQL();
        } else if (statementType == StatementType.CALLABLE_STATEMENT) {
            this.builder = new PreparedCallSQL();
        }
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql               The SQL statement.
     * @param catalog           The catalog.
     * @param schema            The schema.
     * @param statementType     The SQL statement type, prepared or callable.
     * @param autoGeneratedKeys A flag indicating whether auto-generated keys should be returned; one of
     *                          <code>Statement.RETURN_GENERATED_KEYS</code> or <code>Statement.NO_GENERATED_KEYS</code>.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final StatementType statementType,
                    final Integer autoGeneratedKeys) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = schema;
        this.statementType = statementType;
        this.autoGeneratedKeys = autoGeneratedKeys;
        this.columnIndexes = null;
        this.columnNames = null;
        this.resultSetType = null;
        this.resultSetConcurrency = null;
        this.resultSetHoldability = null;
        // create builder
        if (statementType == StatementType.PREPARED_STATEMENT) {
            this.builder = new PreparedStatementWithAutoGeneratedKeys();
        } else if (statementType == StatementType.CALLABLE_STATEMENT) {
            this.builder = new PreparedCallSQL();
        }
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql         The SQL statement.
     * @param catalog     The catalog.
     * @param schema      The schema.
     * @param columnNames An array of column names indicating the columns that should be returned from the inserted row or rows.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final String[] columnNames) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = schema;
        this.statementType = StatementType.PREPARED_STATEMENT;
        this.autoGeneratedKeys = null;
        this.columnIndexes = null;
        this.columnNames = columnNames == null ? null : Arrays.copyOf(columnNames, columnNames.length);
        this.resultSetType = null;
        this.resultSetConcurrency = null;
        this.resultSetHoldability = null;
        // create builder
        builder = new PreparedStatementWithColumnNames();
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql         The SQL statement.
     * @param catalog     The catalog.
     * @param columnNames An array of column names indicating the columns that should be returned from the inserted row or rows.
     * @deprecated Use {@link #PStmtKey(String, String, String, String[])}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final String[] columnNames) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = null;
        this.statementType = StatementType.PREPARED_STATEMENT;
        this.autoGeneratedKeys = null;
        this.columnIndexes = null;
        this.columnNames = columnNames == null ? null : Arrays.copyOf(columnNames, columnNames.length);
        this.resultSetType = null;
        this.resultSetConcurrency = null;
        this.resultSetHoldability = null;
        // create builder
        builder = new PreparedStatementWithColumnNames();
    }

    /**
     * Creates a new Statement from the given Connection.
     *
     * @param connection The Connection to use to create the statement.
     * @return The statement.
     * @throws SQLException Thrown when there is a problem creating the statement.
     */
    public Statement createStatement(final Connection connection) throws SQLException {
        if (builder == null) {
            throw new IllegalStateException("Prepared statement key is invalid.");
        }
        return builder.createStatement(connection);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PStmtKey other = (PStmtKey) obj;
        if (autoGeneratedKeys == null) {
            if (other.autoGeneratedKeys != null) {
                return false;
            }
        } else if (!autoGeneratedKeys.equals(other.autoGeneratedKeys)) {
            return false;
        }
        if (catalog == null) {
            if (other.catalog != null) {
                return false;
            }
        } else if (!catalog.equals(other.catalog)) {
            return false;
        }
        if (!Arrays.equals(columnIndexes, other.columnIndexes)) {
            return false;
        }
        if (!Arrays.equals(columnNames, other.columnNames)) {
            return false;
        }
        if (resultSetConcurrency == null) {
            if (other.resultSetConcurrency != null) {
                return false;
            }
        } else if (!resultSetConcurrency.equals(other.resultSetConcurrency)) {
            return false;
        }
        if (resultSetHoldability == null) {
            if (other.resultSetHoldability != null) {
                return false;
            }
        } else if (!resultSetHoldability.equals(other.resultSetHoldability)) {
            return false;
        }
        if (resultSetType == null) {
            if (other.resultSetType != null) {
                return false;
            }
        } else if (!resultSetType.equals(other.resultSetType)) {
            return false;
        }
        if (schema == null) {
            if (other.schema != null) {
                return false;
            }
        } else if (!schema.equals(other.schema)) {
            return false;
        }
        if (sql == null) {
            if (other.sql != null) {
                return false;
            }
        } else if (!sql.equals(other.sql)) {
            return false;
        }
        if (statementType != other.statementType) {
            return false;
        }
        return true;
    }

    /**
     * Gets a flag indicating whether auto-generated keys should be returned; one of
     * <code>Statement.RETURN_GENERATED_KEYS</code> or <code>Statement.NO_GENERATED_KEYS</code>.
     *
     * @return a flag indicating whether auto-generated keys should be returned.
     */
    public Integer getAutoGeneratedKeys() {
        return autoGeneratedKeys;
    }

    /**
     * The catalog.
     *
     * @return The catalog.
     */
    public String getCatalog() {
        return catalog;
    }

    /**
     * Gets an array of column indexes indicating the columns that should be returned from the inserted row or rows.
     *
     * @return An array of column indexes.
     */
    public int[] getColumnIndexes() {
        return columnIndexes;
    }

    /**
     * Gets an array of column names indicating the columns that should be returned from the inserted row or rows.
     *
     * @return An array of column names.
     */
    public String[] getColumnNames() {
        return columnNames;
    }

    /**
     * Gets the result set concurrency type; one of <code>ResultSet.CONCUR_READ_ONLY</code> or
     * <code>ResultSet.CONCUR_UPDATABLE</code>.
     *
     * @return The result set concurrency type.
     */
    public Integer getResultSetConcurrency() {
        return resultSetConcurrency;
    }

    /**
     * Gets the result set holdability, one of the following <code>ResultSet</code> constants:
     * <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>.
     *
     * @return The result set holdability.
     */
    public Integer getResultSetHoldability() {
        return resultSetHoldability;
    }

    /**
     * Gets the result set type, one of <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     * <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>.
     *
     * @return the result set type.
     */
    public Integer getResultSetType() {
        return resultSetType;
    }

    /**
     * The schema.
     *
     * @return The catalog.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Gets the SQL statement.
     *
     * @return the SQL statement.
     */
    public String getSql() {
        return sql;
    }

    /**
     * The SQL statement type.
     *
     * @return The SQL statement type.
     */
    public StatementType getStmtType() {
        return statementType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((autoGeneratedKeys == null) ? 0 : autoGeneratedKeys.hashCode());
        result = prime * result + ((catalog == null) ? 0 : catalog.hashCode());
        result = prime * result + Arrays.hashCode(columnIndexes);
        result = prime * result + Arrays.hashCode(columnNames);
        result = prime * result + ((resultSetConcurrency == null) ? 0 : resultSetConcurrency.hashCode());
        result = prime * result + ((resultSetHoldability == null) ? 0 : resultSetHoldability.hashCode());
        result = prime * result + ((resultSetType == null) ? 0 : resultSetType.hashCode());
        result = prime * result + ((schema == null) ? 0 : schema.hashCode());
        result = prime * result + ((sql == null) ? 0 : sql.hashCode());
        result = prime * result + ((statementType == null) ? 0 : statementType.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("PStmtKey: sql=");
        buf.append(sql);
        buf.append(", catalog=");
        buf.append(catalog);
        buf.append(", schema=");
        buf.append(schema);
        buf.append(", resultSetType=");
        buf.append(resultSetType);
        buf.append(", resultSetConcurrency=");
        buf.append(resultSetConcurrency);
        buf.append(", resultSetHoldability=");
        buf.append(resultSetHoldability);
        buf.append(", autoGeneratedKeys=");
        buf.append(autoGeneratedKeys);
        buf.append(", columnIndexes=");
        buf.append(Arrays.toString(columnIndexes));
        buf.append(", columnNames=");
        buf.append(Arrays.toString(columnNames));
        buf.append(", statementType=");
        buf.append(statementType);
        return buf.toString();
    }
}
