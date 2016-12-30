/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Enums {
    public enum Location {
        @JsonProperty("query")
        QUERY,

        @JsonProperty("header")
        HEADER,

        @JsonProperty("path")
        PATH,

        @JsonProperty("formData")
        FORM_DATA, 

        @JsonProperty("body")
        BODY
    };

    public enum Format {
        @JsonProperty("csv")
        CSV, 

        @JsonProperty("SSV")
        SSV,

        @JsonProperty("TSV")
        TSV,

        @JsonProperty("pipes")
        PIPES,

        @JsonProperty("multi")
        MULTI
    };

    public enum Type {
        @JsonProperty("string")
        STRING, 

        @JsonProperty("number")
        NUMBER,

        @JsonProperty("integer")
        INTEGER,

        @JsonProperty("boolean")
        BOOLEAN,

        @JsonProperty("array")
        ARRAY,

        @JsonProperty("file")
        FILE,

        @JsonProperty("null")
        NULL,

        @JsonProperty("object")
        OBJECT
    };

    public enum Scheme {
        @JsonProperty("wss")
        WSS, 

        @JsonProperty("ws")
        WS, 

        @JsonProperty("http")
        HTTP,

        @JsonProperty("https")
        HTTPS
    };

    public enum DataType {
        @JsonProperty("integer")
        INTEGER,

        @JsonProperty("long")
        LONG,

        @JsonProperty("float")
        FLOAT,

        @JsonProperty("double")
        DOUBLE,

        @JsonProperty("string")
        STRING,

        @JsonProperty("byte")
        BYTE,

        @JsonProperty("binary")
        BINARY,

        @JsonProperty("boolean")
        BOOLEAN,

        @JsonProperty("date")
        DATE,

        @JsonProperty("dateTime")
        DATETIME,

        @JsonProperty("password")
        PASSWORD
    };

    public enum SecurityType {
        @JsonProperty("basic")
        BASIC, 

        @JsonProperty("apiKey")
        APIKEY, 

        @JsonProperty("oauth2")
        OAUTH2
    };

    public enum ApiKeyLocation {
        @JsonProperty("query")
        QUERY, 

        @JsonProperty("header")
        HEADER
    };

    public enum Flow {
        @JsonProperty("implicit")
        IMPLICIT,
        @JsonProperty("password")
        PASSWORD,
        @JsonProperty("application")
        APPLICATION,
        @JsonProperty("accessCode")
        ACCESSCODE
    }
}
