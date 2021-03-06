/**
 * Copyright 2016 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.query;

import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractSqlQuery;

public class MySqlQuery extends AbstractSqlQuery {
    private final String sql;

    public MySqlQuery(String sql) {
        this.sql = sql;
    }

    @Override
    protected String createParameterizedSql() {
        LOG.debug(sql);
        return sql;
    }
}
