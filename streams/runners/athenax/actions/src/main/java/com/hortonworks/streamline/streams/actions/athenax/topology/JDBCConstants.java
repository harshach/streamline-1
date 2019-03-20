package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.amazonaws.services.identitymanagement.model.transform.ListGroupsForUserResultStaxUnmarshaller;

public class JDBCConstants {
    // JDBC topology component field names
    public static final String NAME = "name";
    public static final String CONNECTION_STRING = "connectionString";
    public static final String TABLE = "table";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String FIELD_NAMES = "fieldNames";
    public static final String PRODUCE_QUERY = "produceQuery";
    // JDBC connector property keys
    public static final String CONNECTOR_KEY_CONNECTION_STRING = "conn.string";
    public static final String CONNECTOR_KEY_USERNAME = "username";
    public static final String CONNECTOR_KEY_PASSWORD = "password";
    public static final String CONNECTOR_KEY_QUERY = "query";
}
