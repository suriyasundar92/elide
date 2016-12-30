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
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Relationship;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Response;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Responses;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Schema;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Swagger;
import com.yahoo.elide.contrib.swagger.JSONObjectClasses.Tag;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
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

        public String getRelationshipUrl() {
            if (lineage.isEmpty()) {
                throw new IllegalStateException("Root collections don't have relationships");
            }

            PathMetaData prior = lineage.peek();
            String baseUrl = prior.getInstanceUrl();

            return baseUrl + "/relationships/" + name;
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
            param.schema = new Schema(Enums.Type.STRING);
            param.name = typeName + "Id";
            param.description = typeName + " ID";

            return param;
        }

        private Parameter getBodyParameter(String typeName, boolean isPlural) {
            Parameter param = new Parameter();
            param.in =  Enums.Location.BODY;

            if (isPlural) {
                param.schema = new Datum(typeName);
            } else {
                param.schema = new Data(typeName);
            }

            param.required = true;
            param.name = typeName;

            return param;
        }
        
        public Path getRelationshipPath() {
            if (lineage.isEmpty()) {
                throw new IllegalStateException("Root collections don't have relationships");
            }

            String typeName = dictionary.getJsonAliasFor(type);
            Path path = new Path();
            path.parameters = lineage.stream()
                .map((item) -> item.getPathParameter())
                .collect(Collectors.toList())
                .toArray(new Parameter[0]);

            path.get = new Operation();
            path.patch = new Operation();

            path.get.description = "Returns the relationship identifiers for " + name;
            path.patch.description = "Replaces the relationship identifiers for " + name;

            path.get.tags = new String[] {getTag()};
            path.patch.tags = new String[] {getTag()};

            RelationshipType relationshipType = dictionary.getRelationshipType(type, name);

            /* Only to many relationships support POST & DELETE */
            if (relationshipType.isToMany()) {
                path.post = new Operation();
                path.delete = new Operation();

                path.post.description = "Appends to the relationship identifiers for " + name;
                path.delete.description = "Deletes the relationship identifiers for " + name;

                path.post.tags = new String[] {getTag()};
                path.delete.tags = new String[] {getTag()};

                path.get.responses.put(200, new Response(new Datum("relationship"), "Successful response"));
                path.post.responses.put(201, new Response(new Data("relationship"), "Successful response"));
                path.delete.responses.put(200, new Response("Successful response"));
                path.patch.responses.put(200, new Response("Successful response"));

                path.post.parameters = new Parameter[] {getBodyParameter("relationship", true)};
                path.delete.parameters = new Parameter[] {getBodyParameter("relationship", true)};
                path.patch.parameters = new Parameter[] {getBodyParameter("relationship", true)};

            } else {
                path.get.responses.put(200, new Response(new Data("relationship"), "Successful response"));
                path.patch.responses.put(200, new Response("Successful response"));

                path.patch.parameters = new Parameter[] {getBodyParameter("relationship", false)};
            }

            return path;
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
            path.post.parameters = new Parameter[] {getBodyParameter(typeName, true)};

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
            path.patch.parameters = new Parameter[] {getBodyParameter(typeName, true)};

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
        swagger.definitions.put("relationship", new Relationship());

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

            if (! pathDatum.lineage.isEmpty()) {
                paths.put(pathDatum.getRelationshipUrl(), pathDatum.getRelationshipPath());
            }
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
}
