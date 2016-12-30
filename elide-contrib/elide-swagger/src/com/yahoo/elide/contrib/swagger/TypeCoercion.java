/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;


import com.yahoo.elide.contrib.swagger.model.Enums;
import com.yahoo.elide.contrib.swagger.model.Properties;
import com.yahoo.elide.contrib.swagger.model.Schema;
import com.yahoo.elide.contrib.swagger.model.jsonapi.Resource;
import com.yahoo.elide.core.EntityDictionary;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;

/**
 * Converts classes to JSON Schemas.
 */
public class TypeCoercion {
    public static final Schema STRING_SCHEMA = new Schema();
    public static final Schema NULL_SCHEMA = new Schema();
    public static final Schema NUMBER_SCHEMA = new Schema();
    public static final Schema BOOLEAN_SCHEMA = new Schema();
    public static final Schema RELATIONSHIP_SCHEMA = new Schema();

    static {
        STRING_SCHEMA.type = Enums.Type.STRING;
        NULL_SCHEMA.type = Enums.Type.NULL;
        NUMBER_SCHEMA.type = Enums.Type.NUMBER;
        BOOLEAN_SCHEMA.type = Enums.Type.BOOLEAN;

        RELATIONSHIP_SCHEMA.type = Enums.Type.OBJECT;
        RELATIONSHIP_SCHEMA.properties = new Properties();
        RELATIONSHIP_SCHEMA.properties.put("type", STRING_SCHEMA);
        RELATIONSHIP_SCHEMA.properties.put("id", STRING_SCHEMA);

    }

    private EntityDictionary dictionary;

    public TypeCoercion(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public Schema coerce(Class<?> sourceType) {

        if (sourceType.isPrimitive() || String.class.isAssignableFrom(sourceType)) {
            return coercePrimitive(sourceType);
        }

        if (sourceType.isArray()) {
            Schema arraySchema = new Schema();
            arraySchema.type = Enums.Type.ARRAY;

            Class<?> componentClazz = sourceType.getComponentType();
            arraySchema.items = coerce(componentClazz);

            return arraySchema;
        }

        if (sourceType.isEnum()) {
            Schema enumSchema = new Schema();

            Enum[] values = (Enum[])sourceType.getEnumConstants();
            String[] names = new String[values.length];

            for (int idx = 0; idx < values.length; idx++) {
                names[idx] = values[idx].name();
            }

            enumSchema.enumeration = names;

            return enumSchema;
        }

        if (Collection.class.isAssignableFrom(sourceType) &&
                sourceType.getGenericSuperclass() instanceof ParameterizedType) {
            Schema arraySchema = new Schema();
            arraySchema.type = Enums.Type.ARRAY;

            ParameterizedType parameterizedType = (ParameterizedType)sourceType.getGenericSuperclass();
            Class<?> componentClazz = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            arraySchema.items = coerce(componentClazz);

            return arraySchema;
        }

        if (dictionary.getJsonAliasFor(sourceType) != null) {
            return coerceEntity(sourceType);
        }

        //TODO - coerce non entity objects.  We'll probably use Jackson -> JSON Schema conversion.
        return NULL_SCHEMA;
    }

    public Schema coerceEntity(Class<?> sourceType) {
        String typeAlias = dictionary.getJsonAliasFor(sourceType);

        Resource entitySchema = new Resource();

        List<String> attributeNames = dictionary.getAttributes(sourceType);
        for (String attributeName : attributeNames) {
            Class<?> attributeClazz = dictionary.getType(sourceType, attributeName);

            entitySchema.addAttribute(attributeName, coerce(attributeClazz));
        }

        List<String> relationshipNames = dictionary.getRelationships(sourceType);
        for (String relationshipName : relationshipNames) {
            entitySchema.addRelationship(relationshipName, RELATIONSHIP_SCHEMA);
        }

        return entitySchema;
    }

    public Schema coercePrimitive(Class<?> sourceType) {
        if (String.class.isAssignableFrom(sourceType)) {
            return STRING_SCHEMA;
        } else if (Number.class.isAssignableFrom(sourceType)) {
            return NUMBER_SCHEMA;
        } else if (Boolean.class.isAssignableFrom(sourceType)) {
            return BOOLEAN_SCHEMA;
        }

        throw new IllegalArgumentException(sourceType.toString() + "is not primitive");
    }
}
