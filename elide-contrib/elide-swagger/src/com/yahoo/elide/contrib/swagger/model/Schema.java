/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Schema extends SwaggerComponent {
    public static final String[] REQUIRED = {};
    public String title;

    @JsonProperty("$ref")
    public String ref;
    public Enums.DataType format;
    public String description;
    // default is a Java reserved word, so I have this
    public String defaultValue;
    public Integer maximum;
    public Boolean exclusiveMaximum;
    public Integer minimum;
    public Boolean exclusiveMinimum;
    public Integer maxLength;
    public Integer minLength;
    public String pattern;
    public Integer maxItems;
    public Integer minItems;
    public Boolean uniqueItems;
    // It doesn't say what these are supposed to be. I need to look at the 
    // other spec to find out.
    public Integer maxProperties;
    public Integer minProperties;
    // I guess this is everything this is allowed to be? 
    // I dunno. May the gods help you in filling this one out.

    @JsonProperty("enum")
    public Object[] enumeration;
    public Integer multipleOf;
    public Enums.Type type;
    public Properties properties;
    public Schema items;
    public Schema[] oneOf;
    public Schema()
    {
        required = REQUIRED;
    }

    public Schema(Enums.Type type) {
        required = REQUIRED;
        this.type = type;

        if (type == Enums.Type.OBJECT) {
            this.properties = new Properties();
        }
    }

    public Schema(String reference) {
        required = REQUIRED;
        this.ref = reference;
    }

    @Override
    public void checkRequired() throws SwaggerValidationException
    {
        super.checkRequired();
        if(!Util.validateRef(ref))
            throw new SwaggerValidationException("The ref is invalid!");
    }
}
