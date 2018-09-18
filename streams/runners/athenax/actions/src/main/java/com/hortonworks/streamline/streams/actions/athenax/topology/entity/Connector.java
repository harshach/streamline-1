package com.hortonworks.streamline.streams.actions.athenax.topology.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Properties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Connector {
  public static final String KAFKA = "kafka";
  public static final String CASSANDRA = "cassandra";
  public static final String TCHANNEL = "tchannel";
  public static final String M3 = "m3";
  public static final String JDBC = "jdbc";
  public static final String ES = "elasticsearch";
  public static final String HIVE = "hive";
  public static final String HTTP = "http";
  public static final String HDFS = "hdfs";
  public static final String RTA = "rta";

  @JsonProperty
  private String uri;

  @JsonProperty
  private String name;

  @JsonProperty
  private String type;

  @JsonProperty
  private Properties props;

  public String type() {
    return this.type;
  }
  public void setType(String type) { this.type = type; }

  public String name() { return name; }
  public void setName(String name) { this.name = name; }

  public String uri() { return uri; }
  public void setUri(String uri) { this.uri = uri; }

  public Properties properties() { return props; }
  public void setProperties(Properties props) {
    this.props = props;
  }
}