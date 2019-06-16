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
package com.hortonworks.streamline.streams.layout.storm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;


import java.io.IOException;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class JoinBoltFluxComponentTest {

    @Test
    public void testFluxGen() throws Exception {
        JoinBoltFluxComponent me = new JoinBoltFluxComponent();
        List<Map.Entry<String, Map<String, Object>>> map = getYamlMap_CountedWindow(me);
        String yamlStr = makeYaml(map);
        System.out.println(yamlStr);

    }

    @Test
    public void testFluxGen_EventTime() throws Exception {
        JoinBoltFluxComponent me = new JoinBoltFluxComponent();
        List<Map.Entry<String, Map<String, Object>>> map = getYamlMap_EventTimeWindow(me);
        String yamlStr = makeYaml(map);
        System.out.println(yamlStr);

    }


    @Test
    public void testMakeWindowFromJson() throws IOException {
        String wjson = "{\"windowLength\":{\"class\":\".Window$Count\",\"count\":100},\"slidingInterval\":{\"class\":\".Window$Count\",\"count\":100},\"tsField\":null,\"lagMs\":0}";
        Window w = new Window(wjson);  // should not throw an exception
    }

    @Test
    public void testMakeEventTimeWindowFromJson() throws IOException {
        String json = "{\"windowLength\" : {\"class\":\".Window$Count\", \"count\":100}, \"slidingInterval\":{\"class\":\".Window$Count\", \"count\":100}, " +
                           "\"tsFields\": [\"stream1:tsField1\" , \"stream2:tsField2\"], \"lagMs\":10 , \"lateStream\": \"optionalLateStream\"} }" ;  // event time window related settings
        Window w = new Window(json);  // should not throw an exception
    }

    public static List<Map.Entry<String, Map<String, Object>>> getYamlMap_CountedWindow(FluxComponent fluxComponent) throws IOException {
        String json = "{\n" +
            "\"from\" : {\"stream\": \"stream1\", \"key\": \"k1\"},\n" +
            "\"joins\" :\n" +
            "  [\n" +
            "    {\"type\" : \"left\",  \"stream\": \"s2\", \"key\":\"k2\", \"with\": \"s1\"},\n" +
            "    {\"type\" : \"left\",  \"stream\": \"s3\", \"key\":\"k3\", \"with\": \"s1\"},\n" +
            "    {\"type\" : \"inner\", \"stream\": \"s4\", \"key\":\"k4\", \"with\": \"s2\"}\n" +
            "  ],\n" +
            "  \"outputKeys\" : [ \"k1\", \"k2\" ],\n" +
            "  \"window\" : {\"windowLength\":{\"class\":\".Window$Count\",\"count\":100},\"slidingInterval\":{\"class\":\".Window$Count\",\"count\":100},\"tsField\":null,\"lagMs\":null},\n" +
            "  \"outputStream\" : \"outStream1\"}\n" +
            "}";

        return getYamlComponents(fluxComponent, json);
    }


    public static List<Map.Entry<String, Map<String, Object>>> getYamlMap_EventTimeWindow(FluxComponent fluxComponent) throws IOException {
        String json = "{\n" +
            "\"from\" : {\"stream\": \"s1\", \"key\": \"k1\"},\n" +
            "\"joins\" :\n" +
            "  [" +
               " {\"type\" : \"left\",  \"stream\": \"s2\", \"key\":\"k2\", \"with\": \"s1\"},\n" +
            "    {\"type\" : \"inner\",  \"stream\": \"s3\", \"key\":\"k3\", \"with\": \"s1\"}\n" +
            "  ],\n" +
            "\"outputKeys\" : [ \"k1\", \"k2\" ],\n" +
            "\"window\":{ \"windowLength\":{\"class\":\".Window$Duration\", \"durationMs\":2000 },\n" +
                         "     \"slidingInterval\":{\"class\":\".Window$Duration\", \"durationMs\":2000},\n" +
                         "     \"tsFields\": [\"s1:tsField\", \"s2:field2.innerTsField\", \"s3:tsField3\"], \n" +
                         "     \"lagMs\":1000, \n" +
                         "     \"lateStream\": \"optionalLateStream\"\n" +
                         "     },\n" +
            "  \"outputStream\" : \"outStream1\"}\n" +
            " }";

        return getYamlComponents(fluxComponent, json);
    }

    private static List<Entry<String, Map<String, Object>>> getYamlComponents(FluxComponent fluxComponent, String json) throws IOException {
        Map<String, Object> props = new ObjectMapper().readValue(json, new TypeReference<HashMap<String, Object>>(){});

        fluxComponent.withConfig(props);

        List<Entry<String, Map<String, Object>>> keysAndComponents = new ArrayList<>();

        for (Map<String, Object> referencedComponent : fluxComponent.getReferencedComponents()) {
            keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_COMPONENTS, referencedComponent));
        }

        Map<String, Object> yamlComponent = fluxComponent.getComponent();
        yamlComponent.put(StormTopologyLayoutConstants.YAML_KEY_ID, "roshan");


        keysAndComponents.add( makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,  yamlComponent) );

        return keysAndComponents;
    }

    private static Map.Entry<String, Map<String, Object>> makeEntry(String key, Map<String, Object> component) {
        return new AbstractMap.SimpleImmutableEntry<>(key, component);
    }


    private static String makeYaml(List<Map.Entry<String, Map<String, Object>>> keysAndComponents) {
        Map<String, Object> yamlMap;
        StringWriter yamlStr = new StringWriter();
        yamlMap = new LinkedHashMap<>();
        yamlMap.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "topo1");

        for (Map.Entry<String, Map<String, Object>> entry: keysAndComponents) {
            addComponentToCollection(yamlMap, entry.getValue(), entry.getKey());
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setSplitLines(false);
        Yaml yaml = new Yaml (options);
        yaml.dump(yamlMap, yamlStr);
        return yamlStr.toString();
    }

    private static void addComponentToCollection (Map<String, Object> yamlMap, Map<String, Object> yamlComponent, String collectionKey) {
        if (yamlComponent == null ) {
            return;
        }

        List<Map<String, Object>> components = (List<Map<String, Object>>) yamlMap.get(collectionKey);
        if (components == null) {
            components = new ArrayList<>();
            yamlMap.put(collectionKey, components);
        }
        components.add(yamlComponent);
    }
}
