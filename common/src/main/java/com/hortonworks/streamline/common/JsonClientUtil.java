/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.exception.WrappedWebApplicationException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonClientUtil {

    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE;

    public static <T> T getEntity(WebTarget target, Class<T> clazz) {
        return getEntity(target, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> T getEntity(WebTarget target, MediaType mediaType, Class<T> clazz) {
        return getEntity(target, new HashMap<>(), mediaType, clazz);

    }

    public static <T> T getEntity(WebTarget target, Map<String, String> headers,
                                  MediaType mediaType, Class<T> clazz) {
        try {
            Invocation.Builder builder = target.request();
            for ( Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
            builder.accept(mediaType);
            String response = builder.get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node, clazz);
        }  catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T getEntity(WebTarget target, String fieldName, Class<T> clazz) {
        return getEntity(target, fieldName, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> T getEntity(WebTarget target, String fieldName, MediaType mediaType, Class<T> clazz) {
        try {
            String response = target.request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return mapper.treeToValue(node.get(fieldName), clazz);
        }  catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T getEntityWithHeaders(WebTarget target, Map<String, String> headers, MediaType mediaType, Class<T> clazz) {
        Invocation.Builder builder = target.request();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        builder.accept(mediaType);

        return builder.get(clazz);
    }

    public static <T> List<T> getEntities(WebTarget target, Class<T> clazz) {
        return getEntities(target, new HashMap<>(), DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> List<T> getEntities(WebTarget target, Map<String, String> headers, Class<T> clazz) {
        return getEntities(target, headers, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> List<T> getEntities(WebTarget target, Map<String, String> headers,
                                          MediaType mediaType, Class<T> clazz) {

        Invocation.Builder builder = target.request();
        for ( Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        builder.accept(mediaType);

        List<T> entities = new ArrayList<>();
        try {
            String response = target.request().get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                entities.add(mapper.treeToValue(it.next(), clazz));
            }
            return entities;
        }  catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> List<T> getEntities(WebTarget target, String fieldName, Class<T> clazz) {
        return getEntities(target, fieldName, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> List<T> getEntities(WebTarget target, String fieldName, MediaType mediaType, Class<T> clazz) {
        List<T> entities = new ArrayList<>();
        try {
            String response = target.request(mediaType).get(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            Iterator<JsonNode> it = node.get(fieldName).elements();
            while (it.hasNext()) {
                entities.add(mapper.treeToValue(it.next(), clazz));
            }
            return entities;
        }  catch (WebApplicationException e) {
            throw WrappedWebApplicationException.of(e);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T postForm(WebTarget target, MultivaluedMap<String, String> form, Class<T> clazz) {
        return postForm(target, form, DEFAULT_MEDIA_TYPE, clazz);
    }

    public static <T> T postForm(WebTarget target, MultivaluedMap<String, String> form, MediaType mediaType, Class<T> clazz) {
        return target.request(mediaType).post(Entity.form(form), clazz);
    }

    public static <T> T postEntity(WebTarget target, Object entity, MediaType mediaType, Class<T> clazz) {
        return target.request(mediaType).post(Entity.json(entity), clazz);
    }

    public static <T> T putEntity(WebTarget target, Object entity, MediaType mediaType, Class<T> clazz) {
        return putEntity(target, new HashMap<>(), entity, mediaType, clazz);
    }

    public static <T> T putEntity(WebTarget target, Map<String, String> headers, Object entity,
                                        MediaType mediaType, Class<T> clazz) {

        Invocation.Builder builder = target.request();
        for ( Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        builder.accept(mediaType);

        return builder.put(Entity.json(entity), clazz);

    }

    public static <T> T postEntityWithHeaders(WebTarget target, Map<String, String> headers, Object entity, MediaType mediaType, Class<T> clazz) {
        Invocation.Builder builder = target.request();
        for ( Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        builder.accept(mediaType);

        return builder.post(Entity.json(entity), clazz);
    }

    public static Object convertRequestToJson(Object request) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(request);
    }
}
