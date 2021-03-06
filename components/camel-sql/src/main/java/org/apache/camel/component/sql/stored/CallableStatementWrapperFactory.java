/**
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
package org.apache.camel.component.sql.stored;

import java.sql.SQLException;

import org.apache.camel.component.sql.stored.template.TemplateParser;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.util.LRUCache;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Statefull class that cached template functions.
 */
public class CallableStatementWrapperFactory extends ServiceSupport {

    public static final int TEMPLATE_CACHE_DEFAULT_SIZE = 200;

    public static final int BATCH_TEMPLATE_CACHE_DEFAULT_SIZE = 200;

    final JdbcTemplate jdbcTemplate;

    final TemplateParser templateParser;

    private final LRUCache<String, TemplateStoredProcedure> templateCache = new LRUCache<>(TEMPLATE_CACHE_DEFAULT_SIZE);

    private final LRUCache<String, BatchCallableStatementCreatorFactory> batchTemplateCache = new LRUCache<>(BATCH_TEMPLATE_CACHE_DEFAULT_SIZE);

    public CallableStatementWrapperFactory(JdbcTemplate jdbcTemplate, TemplateParser
            templateParser) {
        this.jdbcTemplate = jdbcTemplate;
        this.templateParser = templateParser;
    }

    public StatementWrapper create(String sql) throws SQLException {
        return new CallableStatementWrapper(sql, this);
    }

    public BatchCallableStatementCreatorFactory getTemplateForBatch(String sql) {
        BatchCallableStatementCreatorFactory template = this.batchTemplateCache.get(sql);
        if (template != null) {
            return template;
        }

        template = new BatchCallableStatementCreatorFactory(templateParser.parseTemplate(sql));
        this.batchTemplateCache.put(sql, template);

        return template;
    }

    public TemplateStoredProcedure getTemplateStoredProcedure(String sql) {
        TemplateStoredProcedure templateStoredProcedure = this.templateCache.get(sql);
        if (templateStoredProcedure != null) {
            return templateStoredProcedure;
        }

        templateStoredProcedure = new TemplateStoredProcedure(jdbcTemplate, templateParser.parseTemplate(sql));

        this.templateCache.put(sql, templateStoredProcedure);

        return templateStoredProcedure;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
        try {
            // clear cache when we are stopping
            templateCache.clear();
        } catch (Exception ex) {
            //noop
        }
        try {
            // clear cache when we are stopping
            batchTemplateCache.clear();
        } catch (Exception ex) {
            //noop
        }
    }
}
