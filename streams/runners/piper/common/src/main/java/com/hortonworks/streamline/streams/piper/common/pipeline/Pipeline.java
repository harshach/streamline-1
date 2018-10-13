package com.hortonworks.streamline.streams.piper.common.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pipeline {

    @JsonProperty("json_version")
    private String jsonVersion;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("pipeline_id")
    private String pipelineId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("pipeline_name")
    private String pipelineName;
    @JsonProperty("pipeline_description")
    private String pipelineDescription;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("owner")
    private String owner;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("owner_ldap_groups")
    private List<String> ownerLdapGroups = new ArrayList<String>();
    @JsonProperty("tenant_id")
    private String tenantId;
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<String>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("start_date")
    private String startDate;
    @JsonProperty("end_date")
    private String endDate;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("schedule_interval")
    private Object scheduleInterval;
    @JsonProperty("auto_backfill")
    private Boolean autoBackfill;
    @JsonProperty("complete_before_next_run")
    private Boolean completeBeforeNextRun;
    @JsonProperty("drain_previous")
    private Boolean drainPrevious;
    @JsonProperty("depends_on_past")
    private Boolean dependsOnPast;
    @JsonProperty("wait_for_downstream")
    private Boolean waitForDownstream;
    @JsonProperty("trigger_type")
    private Pipeline.TriggerType triggerType;
    @JsonProperty("secure")
    private Boolean secure;
    @JsonProperty("proxy_user")
    private String proxyUser;
    @JsonProperty("datacenter_choice_mode")
    private Pipeline.DatacenterChoiceMode datacenterChoiceMode;
    @JsonProperty("selected_datacenters")
    private Object selectedDatacenters;
    @JsonProperty("email")
    private Object email;
    @JsonProperty("email_on_retry")
    private Boolean emailOnRetry;
    @JsonProperty("email_on_failure")
    private Boolean emailOnFailure;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("tasks")
    private List<Task> tasks = new ArrayList<Task>();
    @JsonProperty("extra_payload")
    private ExtraPayload extraPayload;
    @JsonProperty("global_params")
    private GlobalParams globalParams;

    @JsonProperty("json_version")
    public String getJsonVersion() {
        return jsonVersion;
    }

    @JsonProperty("json_version")
    public void setJsonVersion(String jsonVersion) {
        this.jsonVersion = jsonVersion;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("pipeline_id")
    public String getPipelineId() {
        return pipelineId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("pipeline_id")
    public void setPipelineId(String pipelineId) {
        this.pipelineId = pipelineId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("pipeline_name")
    public String getPipelineName() {
        return pipelineName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("pipeline_name")
    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    @JsonProperty("pipeline_description")
    public String getPipelineDescription() {
        return pipelineDescription;
    }

    @JsonProperty("pipeline_description")
    public void setPipelineDescription(String pipelineDescription) {
        this.pipelineDescription = pipelineDescription;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("owner")
    public String getOwner() {
        return owner;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("owner")
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("owner_ldap_groups")
    public List<String> getOwnerLdapGroups() {
        return ownerLdapGroups;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("owner_ldap_groups")
    public void setOwnerLdapGroups(List<String> ownerLdapGroups) {
        this.ownerLdapGroups = ownerLdapGroups;
    }

    @JsonProperty("tenant_id")
    public String getTenantId() {
        return tenantId;
    }

    @JsonProperty("tenant_id")
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @JsonProperty("tags")
    public List<String> getTags() {
        return tags;
    }

    @JsonProperty("tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("start_date")
    public String getStartDate() {
        return startDate;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("start_date")
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    @JsonProperty("end_date")
    public String getEndDate() {
        return endDate;
    }

    @JsonProperty("end_date")
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("schedule_interval")
    public Object getScheduleInterval() {
        return scheduleInterval;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("schedule_interval")
    public void setScheduleInterval(Object scheduleInterval) {
        this.scheduleInterval = scheduleInterval;
    }

    @JsonProperty("auto_backfill")
    public Boolean getAutoBackfill() {
        return autoBackfill;
    }

    @JsonProperty("auto_backfill")
    public void setAutoBackfill(Boolean autoBackfill) {
        this.autoBackfill = autoBackfill;
    }

    @JsonProperty("complete_before_next_run")
    public Boolean getCompleteBeforeNextRun() {
        return completeBeforeNextRun;
    }

    @JsonProperty("complete_before_next_run")
    public void setCompleteBeforeNextRun(Boolean completeBeforeNextRun) {
        this.completeBeforeNextRun = completeBeforeNextRun;
    }

    @JsonProperty("drain_previous")
    public Boolean getDrainPrevious() {
        return drainPrevious;
    }

    @JsonProperty("drain_previous")
    public void setDrainPrevious(Boolean drainPrevious) {
        this.drainPrevious = drainPrevious;
    }

    @JsonProperty("depends_on_past")
    public Boolean getDependsOnPast() {
        return dependsOnPast;
    }

    @JsonProperty("depends_on_past")
    public void setDependsOnPast(Boolean dependsOnPast) {
        this.dependsOnPast = dependsOnPast;
    }

    @JsonProperty("wait_for_downstream")
    public Boolean getWaitForDownstream() {
        return waitForDownstream;
    }

    @JsonProperty("wait_for_downstream")
    public void setWaitForDownstream(Boolean waitForDownstream) {
        this.waitForDownstream = waitForDownstream;
    }

    @JsonProperty("trigger_type")
    public Pipeline.TriggerType getTriggerType() {
        return triggerType;
    }

    @JsonProperty("trigger_type")
    public void setTriggerType(Pipeline.TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    @JsonProperty("secure")
    public Boolean getSecure() {
        return secure;
    }

    @JsonProperty("secure")
    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    @JsonProperty("proxy_user")
    public String proxyUser() {
        return proxyUser;
    }

    @JsonProperty("tenant_id")
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    @JsonProperty("datacenter_choice_mode")
    public Pipeline.DatacenterChoiceMode getDatacenterChoiceMode() {
        return datacenterChoiceMode;
    }

    @JsonProperty("datacenter_choice_mode")
    public void setDatacenterChoiceMode(Pipeline.DatacenterChoiceMode datacenterChoiceMode) {
        this.datacenterChoiceMode = datacenterChoiceMode;
    }

    @JsonProperty("selected_datacenters")
    public Object getSelectedDatacenters() {
        return selectedDatacenters;
    }

    @JsonProperty("selected_datacenters")
    public void setSelectedDatacenters(Object selectedDatacenters) {
        this.selectedDatacenters = selectedDatacenters;
    }

    @JsonProperty("email")
    public Object getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(Object email) {
        this.email = email;
    }

    @JsonProperty("email_on_retry")
    public Boolean getEmailOnRetry() {
        return emailOnRetry;
    }

    @JsonProperty("email_on_retry")
    public void setEmailOnRetry(Boolean emailOnRetry) {
        this.emailOnRetry = emailOnRetry;
    }

    @JsonProperty("email_on_failure")
    public Boolean getEmailOnFailure() {
        return emailOnFailure;
    }

    @JsonProperty("email_on_failure")
    public void setEmailOnFailure(Boolean emailOnFailure) {
        this.emailOnFailure = emailOnFailure;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("tasks")
    public List<Task> getTasks() {
        return tasks;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("tasks")
    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    @JsonProperty("extra_payload")
    public ExtraPayload getExtraPayload() {
        return extraPayload;
    }

    @JsonProperty("extra_payload")
    public void setExtraPayload(ExtraPayload extraPayload) {
        this.extraPayload = extraPayload;
    }

    @JsonProperty("global_params")
    public GlobalParams getGlobalParams() {
        return globalParams;
    }

    @JsonProperty("global_params")
    public void setGlobalParams(GlobalParams globalParams) {
        this.globalParams = globalParams;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("jsonVersion", jsonVersion).append("pipelineId", pipelineId).append("pipelineName", pipelineName).append("pipelineDescription", pipelineDescription).append("owner", owner).append("ownerLdapGroups", ownerLdapGroups).append("tenantId", tenantId).append("tags", tags).append("startDate", startDate).append("endDate", endDate).append("scheduleInterval", scheduleInterval).append("autoBackfill", autoBackfill).append("completeBeforeNextRun", completeBeforeNextRun).append("drainPrevious", drainPrevious).append("dependsOnPast", dependsOnPast).append("waitForDownstream", waitForDownstream).append("secure", secure).append("datacenterChoiceMode", datacenterChoiceMode).append("selectedDatacenters", selectedDatacenters).append("email", email).append("emailOnRetry", emailOnRetry).append("emailOnFailure", emailOnFailure).append("tasks", tasks).append("extraPayload", extraPayload).append("globalParams", globalParams).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(ownerLdapGroups).append(emailOnRetry).append(endDate).append(datacenterChoiceMode).append(globalParams).append(secure).append(pipelineId).append(scheduleInterval).append(emailOnFailure).append(jsonVersion).append(waitForDownstream).append(email).append(tasks).append(owner).append(dependsOnPast).append(drainPrevious).append(completeBeforeNextRun).append(tags).append(autoBackfill).append(extraPayload).append(pipelineName).append(pipelineDescription).append(selectedDatacenters).append(tenantId).append(startDate).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Pipeline) == false) {
            return false;
        }
        Pipeline rhs = ((Pipeline) other);
        return new EqualsBuilder().append(ownerLdapGroups, rhs.ownerLdapGroups).append(emailOnRetry, rhs.emailOnRetry).append(endDate, rhs.endDate).append(datacenterChoiceMode, rhs.datacenterChoiceMode).append(globalParams, rhs.globalParams).append(secure, rhs.secure).append(pipelineId, rhs.pipelineId).append(scheduleInterval, rhs.scheduleInterval).append(emailOnFailure, rhs.emailOnFailure).append(jsonVersion, rhs.jsonVersion).append(waitForDownstream, rhs.waitForDownstream).append(email, rhs.email).append(tasks, rhs.tasks).append(owner, rhs.owner).append(dependsOnPast, rhs.dependsOnPast).append(drainPrevious, rhs.drainPrevious).append(completeBeforeNextRun, rhs.completeBeforeNextRun).append(tags, rhs.tags).append(autoBackfill, rhs.autoBackfill).append(extraPayload, rhs.extraPayload).append(pipelineName, rhs.pipelineName).append(pipelineDescription, rhs.pipelineDescription).append(selectedDatacenters, rhs.selectedDatacenters).append(tenantId, rhs.tenantId).append(startDate, rhs.startDate).isEquals();
    }

    public enum DatacenterChoiceMode {

        RUN_IN_ONE_DATACENTER("run_in_one_datacenter"),
        RUN_IN_CHOSEN_DATACENTERS("run_in_chosen_datacenters"),
        RUN_IN_ALL_DATACENTERS("run_in_all_datacenters");

        private final String value;
        private final static Map<String, Pipeline.DatacenterChoiceMode> CONSTANTS = new HashMap<String, Pipeline.DatacenterChoiceMode>();

        static {
            for (Pipeline.DatacenterChoiceMode c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private DatacenterChoiceMode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Pipeline.DatacenterChoiceMode fromValue(String value) {
            Pipeline.DatacenterChoiceMode constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum TriggerType {
        EXTERNAL_TRIGGER("external_trigger"),
        TIME_INTERVAL("time_interval");
        private final String value;
        private final static Map<String, Pipeline.TriggerType> CONSTANTS = new HashMap<String, Pipeline.TriggerType>();

        static {
            for (Pipeline.TriggerType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }
        private TriggerType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Pipeline.TriggerType fromValue(String value) {
            Pipeline.TriggerType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }
}
