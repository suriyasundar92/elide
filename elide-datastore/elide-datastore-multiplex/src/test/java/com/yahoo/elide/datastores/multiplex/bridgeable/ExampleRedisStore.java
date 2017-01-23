/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex.bridgeable;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.expression.Visitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.multiplex.BridgeableStoreIT;
import com.yahoo.elide.example.hbase.beans.RedisActions;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ExampleRedisStore implements DataStore {
    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        dictionary.bindEntity(RedisActions.class);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new ExampleRedisTransaction();
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return new ExampleRedisTransaction();
    }

    public class ExampleRedisTransaction implements DataStoreTransaction {

        @Override
        public Object loadObject(Class<?> entityClass,
                                 Serializable id,
                                 Optional<FilterExpression> filterExpression,
                                 RequestScope scope) {
            if (entityClass != RedisActions.class) {
                log.debug("Tried to load unexpected object from redis: {}", entityClass);
                throw new RuntimeException("Tried to load unexpected object from redis!");
            }

            String key = RedisActions.class.getCanonicalName();

            return filterExpression
                    .map(fe -> { throw new UnsupportedOperationException("Filtering unsupported for test."); })
                    .orElseGet(() -> fetchValues(key, v -> id.equals(v.split(":")[1])));
        }

        @Override
        public Iterable<Object> loadObjects(Class<?> entityClass,
                                            Optional<FilterExpression> filterExpression,
                                            Optional<Sorting> sorting,
                                            Optional<Pagination> pagination,
                                            RequestScope scope) {
            if (entityClass != RedisActions.class) {
                log.debug("Tried to load unexpected object from redis: {}", entityClass);
                throw new RuntimeException("Tried to load unexpected object from redis!");
            }

            String key = RedisActions.class.getCanonicalName();

            return filterExpression
                    .map(fe -> {
                        RedisFilter filter = fe.accept(new FilterExpressionParser());
                        if ("user_id".equals(filter.getFieldName())) {
                            return fetchValues(key,
                                    v -> v.startsWith("user" + filter.getValues().get(0).toString() + ":"));
                        }
                        log.error("Received bad filter: {} for type: {}", filter, entityClass);
                        throw new UnsupportedOperationException("Cannot filter object of that type");
                    })
                    .orElseGet(() -> fetchValues(key, unused -> true));
        }

        private Iterable<Object> fetchValues(String key, java.util.function.Predicate<String> filterVal) {
            Jedis client = BridgeableStoreIT.REDIS_CLIENT;
            return client.hgetAll(key).entrySet().stream()
                    .filter(e -> filterVal.test(e.getKey()))
                    .map(this::deserializeAction)
                    .collect(Collectors.toList());
        }

        private RedisActions deserializeAction(Map.Entry<String, String> entry) {
            String[] idParts = entry.getKey().split(":");
            String actionId = idParts[1];

            RedisActions action = new RedisActions();
            action.setId(actionId);
            action.setDescription(entry.getValue());

            return action;
        }

        @Override
        public Object getRelation(DataStoreTransaction relationTx,
                                  Object entity,
                                  String relationName,
                                  Optional<FilterExpression> filterExpression,
                                  Optional<Sorting> sorting,
                                  Optional<Pagination> pagination, RequestScope scope) {
            return null;
        }

        @Override
        public Object getAttribute(Object entity, String attributeName, RequestScope scope) {
            return null;
        }

        // ---- Unsupported operations ----

        @Override
        public <T> Long getTotalRecords(Class<T> entityClass) {
            return null;
        }

        @Override
        public void updateToManyRelation(DataStoreTransaction relationTx, Object entity, String relationName, Set<Object> newRelationships, Set<Object> deletedRelationships, RequestScope scope) {

        }

        @Override
        public void updateToOneRelation(DataStoreTransaction relationTx, Object entity, String relationName, Object relationshipValue, RequestScope scope) {

        }

        @Override
        public void setAttribute(Object entity, String attributeName, Object attributeValue, RequestScope scope) {

        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public User accessUser(Object opaqueUser) {
            return null;
        }

        @Override
        public void save(Object entity, RequestScope scope) {

        }

        @Override
        public void delete(Object entity, RequestScope scope) {

        }

        @Override
        public void flush(RequestScope scope) {

        }

        @Override
        public void commit(RequestScope requestScope) {

        }

        @Override
        public void preCommit() {

        }

        @Override
        public void createObject(Object entity, RequestScope scope) {

        }

        @Override
        public <T> T createNewObject(Class<T> entityClass) {
            return null;
        }
    }

    /**
     * Example structure for parsing filter expression.
     */
    @AllArgsConstructor
    private static class RedisFilter {
        @Getter private final String fieldName;
        @Getter private final Operator operator;
        @Getter private final List<Object> values;
    }

    /**
     * Small example parser.
     */
    private static class FilterExpressionParser implements Visitor<RedisFilter> {
        @Override
        public RedisFilter visitPredicate(Predicate predicate) {
            return new RedisFilter(
                    predicate.getField(),
                    predicate.getOperator(),
                    predicate.getValues()
            );
        }

        @Override
        public RedisFilter visitAndExpression(AndFilterExpression expression) {
            throw new UnsupportedOperationException("Unsupported operation");
        }

        @Override
        public RedisFilter visitOrExpression(OrFilterExpression expression) {
            throw new UnsupportedOperationException("Unsupported operation");
        }

        @Override
        public RedisFilter visitNotExpression(NotFilterExpression expression) {
            throw new UnsupportedOperationException("Unsupported operation");
        }
    }
}
