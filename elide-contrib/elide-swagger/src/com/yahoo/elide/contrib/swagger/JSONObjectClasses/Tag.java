package com.yahoo.elide.contrib.swagger.JSONObjectClasses;

public class Tag extends SwaggerComponent {
    public Tag(String name) {
        this.name = name;
    }

    private static final String[] REQUIRED = {"name"};
    public String name;
    public String description;
    public ExternalDocumentation externalDocs;
    public Tag()
    {
        required = REQUIRED;
    }
}
