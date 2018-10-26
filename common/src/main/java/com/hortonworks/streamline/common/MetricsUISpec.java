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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.exception.ComponentConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetricsUISpec {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsUISpec.class);
    private static final String NOT_APPLICABLE_PROPERTY = "%s property for %s of type %s is not applicable";
    private static final String DEFAULT_VALUE_TYPE = "expected type for defaultValue for field %s of type %s is %s actual type %s";
    private static final String PROPERTY_REQUIRED = "property %s required for field %s of type %s";
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetricField {
        private static final String NAME = "name";
        private static final String UI_NAME = "uiName";
        private static final String METRIC_KEY_NAME = "metricKeyName";
        private static final String METRIC_PREV_KEY_NAME = "metricPrevKeyName";
        private static final String VALUE_FORMAT = "valueFormat";
        private static final String DEFAULT_VALUE = "defaultValue";
        private String name;
        private String uiName;
        private String metricKeyName;
        private String metricPrevKeyName;
        private String valueFormat;
        private String defaultValue;

        public MetricField () {}

        public MetricField (MetricField metricField) {
            this.name = metricField.name;
            this.uiName = metricField.uiName;
            this.metricKeyName = metricField.metricKeyName;
            this.metricPrevKeyName = metricField.metricPrevKeyName;
            this.valueFormat = metricField.valueFormat;
            this.defaultValue = metricField.defaultValue;
        }
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUiName() {
            return uiName;
        }

        public void setUiName(String uiName) {
            this.uiName = uiName;
        }

        public String getMetricKeyName() {
            return metricKeyName;
        }

        public void setMetricKeyName(String metricKeyName) {
            this.metricKeyName = metricKeyName;
        }

        public String getMetricPrevKeyName() {
            return metricPrevKeyName;
        }

        public void setMetricPrevKeyName(String metricPrevKeyName) {
            this.metricPrevKeyName = metricPrevKeyName;
        }

        public String getValueFormat() {
            return valueFormat;
        }

        public void setValueFormat(String valueFormat) {
            this.valueFormat = valueFormat;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public String toString() {
            return "MetricField{" +
                    "name='" + name + '\'' +
                    ", uiName='" + uiName + '\'' +
                    ", metricKeyName='" + metricKeyName + '\'' +
                    ", metricPrevKeyName='" + metricPrevKeyName + '\'' +
                    ", valueFormat=" + valueFormat +
                    ", defaultValue='" + defaultValue + '\'' +
                    '}';
        }

    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeseriesMetricField {
        private static final String NAME = "name";
        private static final String UI_NAME = "uiName";
        private static final String METRIC_KEY_NAME = "metricKeyName";
        private static final String INTERPOLATE = "interpolate";
        private String name;
        private String uiName;
        private List<String> metricKeyName;
        private String interpolate;

        public TimeseriesMetricField () {}

        public TimeseriesMetricField (TimeseriesMetricField timeseriesMetricField) {
            this.name = timeseriesMetricField.name;
            this.uiName = timeseriesMetricField.uiName;
            this.metricKeyName = timeseriesMetricField.metricKeyName;
            this.interpolate = timeseriesMetricField.interpolate;
        }
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUiName() {
            return uiName;
        }

        public void setUiName(String uiName) {
            this.uiName = uiName;
        }

        public List<String> getmetricKeyName() {
            return metricKeyName;
        }

        public void setmetricKeyName(List<String> metricKeyName) {
            this.metricKeyName = metricKeyName;
        }

        public String getInterpolate() {
            return interpolate;
        }

        public void setInterpolate(String interpolate) {
            this.interpolate = interpolate;
        }

        @Override
        public String toString() {
            return "MetricField{" +
                    "name='" + name + '\'' +
                    ", uiName='" + uiName + '\'' +
                    ", valueKeys='" + metricKeyName + '\'' +
                    ", interpolate=" + interpolate +
                    '}';
        }

    }

    private List<MetricField> metrics;
    private List<TimeseriesMetricField> timeseries;
    private Object layout;

    public List<MetricField> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricField> metrics) {
        this.metrics = metrics;
    }

    public List<TimeseriesMetricField> getTimeseries() {
        return timeseries;
    }

    public void setTimeseries(List<TimeseriesMetricField> timeseries) {
        this.timeseries = timeseries;
    }
    
    public Object getLayout() {
        return layout;
    }

    public void setLayout(Object layout) {
        this.layout = layout;
    }

    @Override
    public String toString() {
        return "MetricsUISpec{" +
                "metrics='" + metrics +'\'' +
                ", timeseries='" + timeseries + '\'' +
                ", layout='" + layout+ '\'' +
                '}';
    }

}
