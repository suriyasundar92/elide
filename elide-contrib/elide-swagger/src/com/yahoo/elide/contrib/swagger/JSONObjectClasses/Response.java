package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class Response extends SwaggerComponent {
    private static final String[] REQUIRED = {"description"};
    public String description;
    public Schema schema;
    public Headers headers;
    public Example examples;

    public Response()
    {
        required = REQUIRED;
    }

    public Response(Schema schema) {
        required = REQUIRED;
        this.schema = schema;
    }

    public Response(Schema schema, String description) {
        required = REQUIRED;
        this.schema = schema;
        this.description = description;
    }

    public Response(String description) {
        required = REQUIRED;
        this.description = description;
    }
}
