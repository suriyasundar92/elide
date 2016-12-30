/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.model.jsonapi;

import com.yahoo.elide.contrib.swagger.model.Enums;
import com.yahoo.elide.contrib.swagger.model.Properties;
import com.yahoo.elide.contrib.swagger.model.Schema;

public class Resource extends Schema {

    public static final Schema STRING_SCHEMA = new Schema();
    static {
        STRING_SCHEMA.type = Enums.Type.STRING;
    }

    public Resource() {
        type = Enums.Type.OBJECT;
        properties = new Properties();
        properties.put("type", STRING_SCHEMA);
        properties.put("id", STRING_SCHEMA);

        Schema attributes = new Schema();
        attributes.type = Enums.Type.OBJECT;
        properties.put("attributes", attributes);

        Schema relationships = new Schema();
        relationships.type = Enums.Type.OBJECT;
        properties.put("relationships", relationships);

    }

    public void addAttribute(String attributeName, Schema attributeSchema) {
        Schema attributes = properties.get("attributes");

        if (attributes.properties == null) {
            attributes.properties = new Properties();
        }

        attributes.properties.put(attributeName, attributeSchema);
    }

    public void addRelationship(String relationshipName, Schema relationshipSchema) {
        Schema relationships = properties.get("relationships");

       if (relationships.properties == null) {
            relationships.properties = new Properties();
        }

        relationships.properties.put(relationshipName, relationshipSchema);
    }
}
