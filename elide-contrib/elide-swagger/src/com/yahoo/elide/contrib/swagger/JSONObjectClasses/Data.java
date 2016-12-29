/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class Data extends Schema {

    public Data(String definitionName) {
        type = Enums.Type.OBJECT;
        properties = new Properties();

        Schema resourceSchema = new Schema();
        resourceSchema.ref = "#/definitions/" + definitionName;

        Schema resourcesSchema = new Schema();
        resourcesSchema.items = resourceSchema;
        resourcesSchema.type = Enums.Type.ARRAY;

        properties.put("data", resourcesSchema);
    }
}
