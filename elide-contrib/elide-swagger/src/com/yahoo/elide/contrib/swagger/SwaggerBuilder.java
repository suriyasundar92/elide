package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Data;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Datum;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Definitions;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Enums;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Info;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Operation;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Parameter;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Path;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Paths;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Response;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Responses;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Schema;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Swagger;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Tag;
import com.yahoo.elide.core.EntityDictionary;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class SwaggerBuilder {
    EntityDictionary dictionary;
    Set<Class<?>> rootClasses;
    Swagger swagger;

    @AllArgsConstructor
    public class PathMetaData {
        Stack<PathMetaData> lineage;

        @Getter
        String name;

        @Getter
        Class<?> type;

        public PathMetaData(Class<?> type) {
            this.type = type;
            lineage = new Stack<>();
            name = dictionary.getJsonAliasFor(type);
        }

        public String getCollectionUrl() {
            if (lineage.isEmpty()) {
                return "/" + name;
            } else {
                return lineage.peek().getInstanceUrl() + "/" + name;
            }
        }

        public String getInstanceUrl() {
            String typeName = dictionary.getJsonAliasFor(type);
            return getCollectionUrl() + "/{" + typeName + "Id}";
        }

        private String getTag() {
            if (lineage.isEmpty()) {
                return name;
            } else {
                return lineage.get(0).getName();
            }
        }

        private Parameter getPathParameter() {
            String typeName = dictionary.getJsonAliasFor(type);

            Parameter param = new Parameter();
            param.in =  Enums.Location.PATH;
            param.schema = new Schema();
            param.schema.type = Enums.Type.STRING;
            param.required = true;
            param.name = typeName + "Id";
            param.description = typeName + " ID";

            return param;
        }

        private Parameter getBodyParameter() {
            String typeName = dictionary.getJsonAliasFor(type);

            Parameter param = new Parameter();
            param.in =  Enums.Location.BODY;
            param.schema = new Datum(typeName);
            param.required = true;
            param.name = typeName;

            return param;
        }

        public Path getCollectionPath() {
            String typeName = dictionary.getJsonAliasFor(type);
            Path path = new Path();

            if (! lineage.isEmpty()) {
                path.parameters = lineage.stream()
                        .map((item) -> item.getPathParameter())
                        .collect(Collectors.toList())
                        .toArray(new Parameter[0]);
            }

            path.get = new Operation();
            path.post = new Operation();

            if (lineage.isEmpty()) {
                path.get.description = "Returns the collection of type " + typeName;
            } else {
                path.get.description = "Returns the relationship " + name;
            }

            path.get.responses = new Responses();
            path.post.responses = new Responses();

            path.get.tags = new String[] {getTag()};
            path.post.tags = new String[] {getTag()};

            Response okResponse = new Response();
            okResponse.description = "Successful response";
            okResponse.schema = new Data(typeName);
            path.get.responses.put(200, okResponse);

            okResponse = new Response();
            okResponse.description = "Successful response";
            okResponse.schema = new Datum(typeName);
            path.post.responses.put(201, okResponse);
            path.post.parameters = new Parameter[] {getBodyParameter()};

            return path;
        }

        public Path getInstancePath() {
            String typeName = dictionary.getJsonAliasFor(type);
            Path path = new Path();

            path.parameters = getFullLineage().stream()
                .map((item) -> item.getPathParameter())
                .collect(Collectors.toList())
                .toArray(new Parameter[0]);

            path.get = new Operation();
            path.patch = new Operation();
            path.delete = new Operation();

            path.get.description = "Returns an instance of type " + typeName;
            path.patch.description = "Modifies an instance of type " + typeName;
            path.delete.description = "Deletes an instance of type " + typeName;

            path.get.responses = new Responses();
            path.patch.responses = new Responses();
            path.delete.responses = new Responses();

            path.get.tags = new String[] {getTag()};
            path.patch.tags = new String[] {getTag()};
            path.delete.tags = new String[] {getTag()};

            Response okResponse = new Response();
            okResponse.description = "Successful response";
            okResponse.schema = new Datum(typeName);
            path.get.responses.put(200, okResponse);

            okResponse = new Response();
            okResponse.description = "Successful response";
            path.patch.responses.put(200, okResponse);
            path.patch.parameters = new Parameter[] {getBodyParameter()};

            okResponse = new Response();
            okResponse.description = "Successful response";
            path.delete.responses.put(200, okResponse);

            return path;
        }

        public Stack<PathMetaData> getFullLineage() {
            Stack<PathMetaData> fullLineage = new Stack<>();

            fullLineage.addAll(lineage);
            fullLineage.add(this);
            return fullLineage;
        }

        public boolean lineageContainsType(Class<?> type) {
            if (this.type.equals(type)) {
                return true;
            }

            if (lineage.isEmpty()) {
                return false;
            }

            return lineage.peek().lineageContainsType(type);
        }
    }

    public SwaggerBuilder(EntityDictionary dictionary, Info info) {
        this.dictionary = dictionary;
        TypeCoercion coercion = new TypeCoercion(dictionary);

        swagger = new Swagger();
        swagger.definitions = new Definitions();

        Set<Class<?>> allClasses = dictionary.getBindings();

        for (Class<?> clazz : allClasses) {
            swagger.definitions.put(
                    dictionary.getJsonAliasFor(clazz),
                    coercion.coerce(clazz)
            );
        }

        rootClasses =  allClasses.stream()
                .filter(dictionary::isRoot)
                .collect(Collectors.toSet());

        Set<PathMetaData> pathData =  rootClasses.stream()
                .map(this::find)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Paths paths = new Paths();

        for (PathMetaData pathDatum : pathData) {
            paths.put(pathDatum.getCollectionUrl(), pathDatum.getCollectionPath());
            paths.put(pathDatum.getInstanceUrl(), pathDatum.getInstancePath());
        }

        swagger.info = info;
        swagger.paths = paths;

        List<Tag> tags = rootClasses.stream()
                .map((clazz) -> dictionary.getJsonAliasFor(clazz))
                .map((name) -> new Tag(name))
                .collect(Collectors.toList());

        swagger.tags = tags.toArray(new Tag[0]);
    }

    public Swagger build() {
        return swagger;
    }

    public Set<PathMetaData> find(Class<?> rootClass) {
        Queue<PathMetaData> toVisit = new ArrayDeque<>();
        Set<PathMetaData> paths = new HashSet<>();

        toVisit.add(new PathMetaData(rootClass));

        while (! toVisit.isEmpty()) {
            PathMetaData next = toVisit.remove();

            List<String> relationshipNames = dictionary.getRelationships(next.getType());

            for (String relationshipName : relationshipNames) {
                Class<?> relationshipClass = dictionary.getParameterizedType(next.getType(), relationshipName);

                if (next.lineageContainsType(relationshipClass)) {
                    continue;
                }

                if (!rootClasses.contains(relationshipClass)) {
                    toVisit.add(new PathMetaData(next.getFullLineage(), relationshipName, relationshipClass));
                }

            }

            paths.add(next);
        }
        return paths;
    }

    // public SwaggerBuilder(EntityDictionary entityDictionary)
    // {
    //     Swagger retval = new Swagger();
    //     // Most of the stuff in this object is for humans to read, which will make 
    //     // it really hard to automatically generate. We might need some annotations
    //     // to make this work.
    //     Info info = new Info();
    //     retval.info = info;

    //     // Since the server implementation is separate from Elide (I think), we can't know
    //     // this. Knowing this means that the user would have to write it twice, once, for 
    //     // the server and once for us, but I don't know how to get around that.
    //     // TODO: Find a better way to know this.
    //     Enums.Scheme[] schemes = new Enum.Scheme[] {Enum.Scheme.HTTP};
    //     retval.schemes = schemes;

    //     // If I understand correctly, swagger is set up so that it can only accept and 
    //     // return json text, so I think we can hardcode this. Maybe?
    //     retval.consumes = new MimeType[] {new MimeType("application/json")};
    //     retval.produces = new MimeType[] {new MimeType("application/json")};

    //     // This is going to be the fun part. I think most of the work will be filling this out
    //     Paths paths = new Paths();
    //     retval.paths = paths;
    //     // I'm pretty sure, if I understand correctly, that this should be a hashmap 
    //     // of all the data model classes we have and all the things in them. I wonder 
    //     // if it can be recursive...
    //     // Anyway, I don't think this will actually be that hard. We'll learn about
    //     // reflection. 
    //     Definitions definitions = new Definitions();
    //     retval.definitions = definitions;

    //     // I still don't wholly understand this. I think it is for the benefit of a
    //     // human writing this, which doesn't matter to us. We might well leave it blank.
    //     ParametersDefinitions parameters = new ParametersDefinitions();
    //     retval.parameters = paramters;

    //     // I think this is just like a ParametersDefinitions in that it's for the benefit of a
    //     // human so we probably won't use it.
    //     ResponsesDefinitions responses = new ResponsesDefinitions();
    //     retval.responses = responses;

    //     // I really hope that the EntityDictionary knows all the right things to make this
    //     // becuause otherwise this could be quite complicated to fill out.
    //     SecurityDefinitions securityDefinitions = new SecurityDefinitions();
    //     retval.securityDefinitions = securityDefinitions;

    //     // Implementing this could also be complicated unless the EntityDictionary
    //     // knows exactly what this needs to have.
    //     SecurityRequirement[] security;
    //     retval.security = security;

    //     // I don't know what this does, but I think the odds are more than even that 
    //     // it doesn't get used by us.
    //     Tag[] tags;
    //     retval.tags = tags;

    //     // This should at least be simple. We'll need to have another annotation,
    //     // put the URL from there into here, and we're done. 
    //     ExternalDocumentation externalDocs;
    //     retval.externalDocs = externalDocs;
    // }
}
