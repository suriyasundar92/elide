/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.model;


public class Items extends Schema {
    public Items()
    {
        required = REQUIRED;
    }
    @Override
    public void checkRequired() throws SwaggerValidationException
    {
        super.checkRequired();

        if(type == Enums.Type.ARRAY && items == null)
            throw new SwaggerValidationException("If the type is an array, then the items (ie, the thing describing what's in the array) can't be null");
    }
}
