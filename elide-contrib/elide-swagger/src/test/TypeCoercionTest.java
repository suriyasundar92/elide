/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package test;

import com.google.common.collect.Maps;
import com.yahoo.elide.contrib.swagger.TypeCoercion;
import com.yahoo.elide.contrib.swagger.model.Enums;
import com.yahoo.elide.contrib.swagger.model.Schema;
import com.yahoo.elide.core.EntityDictionary;
import org.testng.Assert;
import org.testng.annotations.Test;
import test.models.Author;
import test.models.AuthorType;
import test.models.Book;
import test.models.Publisher;

public class TypeCoercionTest {

    @Test
    public void testEntityCoercion() {
        EntityDictionary dictionary = new EntityDictionary(Maps.newHashMap());

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);

        TypeCoercion coercion = new TypeCoercion(dictionary);

        Schema schema = coercion.coerce(Book.class);

        Schema attributes = schema.properties.get("attributes");
        Assert.assertEquals(attributes.properties.size(), 1);

        Schema relationships = schema.properties.get("relationships");
        Assert.assertEquals(relationships.properties.size(), 2);

    }

    @Test
    public void testPrimitiveCoercion() {
        EntityDictionary dictionary = new EntityDictionary(Maps.newHashMap());
        TypeCoercion coercion = new TypeCoercion(dictionary);

        Schema schema = coercion.coerce(String.class);
        Assert.assertEquals(schema.type, Enums.Type.STRING);
    }

    @Test
    public void testArrayCoercion() {
        EntityDictionary dictionary = new EntityDictionary(Maps.newHashMap());
        TypeCoercion coercion = new TypeCoercion(dictionary);

        String[] strings = new String[1];

        Schema schema = coercion.coerce(strings.getClass());

        Assert.assertEquals(schema.type, Enums.Type.ARRAY);
        Assert.assertEquals(schema.items.type, Enums.Type.STRING);
    }

    @Test
    public void testEnumCoercion() {
        EntityDictionary dictionary = new EntityDictionary(Maps.newHashMap());
        TypeCoercion coercion = new TypeCoercion(dictionary);

        Schema schema = coercion.coerce(AuthorType.class);

        Assert.assertEquals(schema.enumeration.length, 3);
    }
}
