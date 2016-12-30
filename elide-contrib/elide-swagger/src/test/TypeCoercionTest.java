/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package test;

import com.google.common.collect.Maps;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.contrib.swagger.model.Enums;
import com.yahoo.elide.contrib.swagger.model.Schema;
import com.yahoo.elide.contrib.swagger.TypeCoercion;
import com.yahoo.elide.core.EntityDictionary;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.Set;

public class TypeCoercionTest {
    @Entity
    @Include
    public class Author {
        @OneToMany
        public Set<Book> getBooks() {
            return null;
        }

        @OneToMany
        public Set<Publisher> getPublisher() {
            return null;
        }
    }

    @Entity
    @Include(rootLevel = true)
    public class Publisher {

        @OneToMany
        public Set<Book> getBooks() {
            return null;
        }

        @OneToMany
        public Set<Author> getExclusiveAuthors() {
            return null;
        }
    }

    @Entity
    @Include(rootLevel = true)
    public class Book {
        @OneToMany
        public Set<Author> getAuthors() {
            return null;
        }

        @OneToOne
        public Publisher getPublisher() {
            return null;
        }

        public String title;
    }

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
}
