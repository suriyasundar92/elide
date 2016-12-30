/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

/**
 * Represents a JSON-API relationship object
 */
public class Relationship extends Schema {

    public Relationship() {
        type = Enums.Type.OBJECT;
        properties = new Properties();
        properties.put("type", new Schema(Enums.Type.STRING));
        properties.put("id", new Schema(Enums.Type.STRING));
    }
}
