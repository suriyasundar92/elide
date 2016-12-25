package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Info;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Operation;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Path;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Paths;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Response;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Responses;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Swagger;
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

        public Path getCollectionPath() {
            String typeName = dictionary.getJsonAliasFor(type);
            Path path = new Path();
            path.get = new Operation();

            if (lineage.isEmpty()) {
                path.get.description = "Returns the collection of type " + typeName;
            } else {
                path.get.description = "Returns the relationship " + name;
            }

            path.get.responses = new Responses();

            Response okResponse = new Response();
            okResponse.description = "Successful response";
            path.get.responses.put(200, okResponse);

            return path;
        }

        public Path getInstancePath() {
            String typeName = dictionary.getJsonAliasFor(type);
            Path path = new Path();
            path.get = new Operation();
            path.get.description = "Returns an instance of type " + typeName;

            path.get.responses = new Responses();

            Response okResponse = new Response();
            okResponse.description = "Successful response";
            path.get.responses.put(200, okResponse);

            return path;
        }

        public Stack<PathMetaData> getFullLineage() {
            Stack<PathMetaData> fullLineage = new Stack<>();

            //TODO - This maybe in the wrong order.
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
        Set<Class<?>> allClasses = dictionary.getBindings();
        rootClasses =  allClasses.stream()
                .filter(dictionary::isRoot)
                .collect(Collectors.toSet());

        Set<PathMetaData> pathData =  rootClasses.stream()
                .map(this::find)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        swagger = new Swagger();
        Paths paths = new Paths();

        for (PathMetaData pathDatum : pathData) {
            paths.put(pathDatum.getCollectionUrl(), pathDatum.getCollectionPath());
            paths.put(pathDatum.getInstanceUrl(), pathDatum.getInstancePath());
        }

        swagger.info = info;
        swagger.paths = paths;
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
