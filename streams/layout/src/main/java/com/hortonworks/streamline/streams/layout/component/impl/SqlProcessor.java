/**
 * Copyright 2017 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/


package com.hortonworks.streamline.streams.layout.component.impl;

import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;

/**
 * Represents a design time sql processor.
 * Note that derived classes should implement its copy constructor.
 */
public class SqlProcessor extends StreamlineProcessor {
  public static final String CONFIG_KEY_SQL = "sql";

  private String sqlStatement;

  public SqlProcessor() {
  }

  public SqlProcessor(String sqlStatement) {
    this.sqlStatement = sqlStatement;
  }

  public SqlProcessor(SqlProcessor other) {
    super(other);
    this.sqlStatement = other.getSqlStatement();
  }

  public String getSqlStatement() {
    return sqlStatement;
  }

  public void setSqlStatement(String sql) {
    this.sqlStatement = sql;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || !(o instanceof SqlProcessor)) {
      return false;
    }

    SqlProcessor sqlProcessor = (SqlProcessor) o;
    if (this.sqlStatement == null) {
      return sqlProcessor.getSqlStatement() == null;
    }

    return this.sqlStatement.equals(sqlProcessor.getSqlStatement());
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (sqlStatement != null ? sqlStatement.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SqlProcessor{" +
        "sql=" + sqlStatement +
        '}';
  }

  @Override
  public void accept(TopologyDagVisitor visitor) {
    visitor.visit(this);
  }
}
