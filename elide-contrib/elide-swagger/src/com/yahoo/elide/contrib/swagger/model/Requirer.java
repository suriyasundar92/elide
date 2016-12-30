package com.yahoo.elide.contrib.swagger.model;

interface Requirer {
    public abstract void checkRequired() throws SwaggerValidationException;
}
