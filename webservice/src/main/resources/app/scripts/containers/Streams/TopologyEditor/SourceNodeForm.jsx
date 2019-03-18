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

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Tabs, Tab} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import TopologyREST from '../../../rest/TopologyREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import Form from '../../../libs/form';
import StreamsSidebar from '../../../components/StreamSidebar';
import NotesForm from '../../../components/NotesForm';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import {Scrollbars} from 'react-custom-scrollbars';

export default class SourceNodeForm extends Component {
  static propTypes = {
    nodeData: PropTypes.object.isRequired,
    configData: PropTypes.object.isRequired,
    editMode: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    versionId: PropTypes.number.isRequired
  };

  constructor(props) {
    super(props);
    this.fetchData();
    this.state = {
      formData: {},
      formErrors: {},
      streamObj: {},
      description: '',
      showRequired: true,
      showSecurity: false,
      hasSecurity: false,
      activeTabKey: 1,
      clusterArr: [],
      configJSON: [],
      clusterName: '',
      fetchLoader: true,
      securityType : ''
    };
    this.schemaTopicKeyName = '';
    this.schemaVersionKeyName = '';
    this.schemaBranchKeyName = '';
  }

  fetchData() {
    let {topologyId, versionId, nodeType, nodeData, namespaceId} = this.props;
    const sourceParams = nodeData.parentType + '/' + nodeData.topologyComponentBundleId;
    let stateExecuted = false;
    let promiseArr = [
      TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId),
      EnvironmentREST.getAllNamespaceFromService("kafka")
    ];

    Promise.all(promiseArr).then((results) => {
      let stateObj = {}, hasSecurity = false,
        tempArr = [];
      if (results[0].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={results[0].responseMessage}/>, '', toastOpt);
      } else {
        this.nodeData = results[0];
        if (this.nodeData.outputStreams.length === 0) {
          this.streamObj = {
            streamId: null,
            fields: []
          };
        } else {
          this.streamObj = this.nodeData.outputStreams[0];
        }
        stateObj.streamObj = this.streamObj;
      }
      if (results[1].responseMessage !== undefined) {
        this.setState({fetchLoader: false});
        FSReactToastr.error(
          <CommonNotification flag="error" content={results[1].responseMessage}/>, '', toastOpt);
      } else {
        const clusters = results[1].entities;
        let clusterArr = [];
        clusters.map((clusterObj, index)=>{
          let cObj = clusterObj.namespace;
          cObj.config = clusterObj.serviceConfigurationMap.server.configurationMap;
          clusterArr.push(cObj);

          const obj = {
            fieldName: cObj.name + '@#$' + cObj.id,
            uiName: cObj.name
          };
          tempArr.push(obj);
        });
        //need to check if security is enabled or not
        //hasSecurity = clusters[x][k].authentication.enabled;
        stateObj.clusterArr = _.isEmpty(clusterArr) ? [] : clusterArr;
      }
      if(this.nodeData.outputStreams.length > 0){
        let clusterId = this.nodeData.config.properties.cluster;
        TopologyREST.getSourceComponentClusters(sourceParams, clusterId).then((response)=>{
          let clusterObj = stateObj.clusterArr.find((c)=>{return c.id == clusterId;});
          if(response && clusterObj){
            clusterObj.config.topic = response[clusterId].hints.topic;
            if(stateExecuted){
              this.setState({clusterArr: stateObj.clusterArr},()=>{
                this.updateClusterFields(stateObj.formData.clusters);
              });
            }
          }
        });
      }

      stateObj.configJSON = this.fetchFields(stateObj.clusterArr);
      if (stateObj.clusterArr && stateObj.clusterArr.length > 0) {
        stateObj.configJSON = this.pushClusterFields(tempArr, stateObj.configJSON);
      }

      this.schemaTopicKeyName = Utils.getSchemaKeyName(stateObj.configJSON,'schema');

      stateObj.formData = this.nodeData.config.properties;
      stateObj.description = this.nodeData.description;
      stateObj.fetchLoader = false;
      stateObj.hasSecurity = hasSecurity;
      stateObj.securityType = stateObj.formData.securityProtocol || '';
      this.setState(stateObj, () => {
        stateExecuted = true;
        if (stateObj.formData.clusters !== undefined) {
          this.updateClusterFields(stateObj.formData.clusters);
          this.setState({streamObj: this.state.streamObj});
        }
      });
    });
  }

  fetchFields = (clusterList) => {
    let obj = this.props.configData.topologyComponentUISpecification.fields;
    if(clusterList.length > 0){
      const clusterFlag = obj.findIndex(x => {
        return x.fieldName === 'clusters';
      });
      if (clusterFlag === -1) {
        obj.unshift(Utils.clusterField());
      }
    }
    return obj;
  }

  pushClusterFields = (opt, uiSpecification) => {
    const obj = uiSpecification.map(x => {
      if (x.fieldName === 'clusters') {
        x.options = opt;
      }
      return x;
    });
    return obj;
  }

  populateClusterFields(val) {
    const {clusterArr} = this.state;
    const {nodeData} = this.props;
    const sourceParams = nodeData.parentType + '/' + nodeData.topologyComponentBundleId;
    const tempObj = Object.assign({}, this.state.formData, {topic: '', bootstrapServers: ''});
    // split the val to find the key by id
    let splitValues = val.split('@#$');
    let obj;
    if(!_.isEmpty(splitValues[1])){
      obj = Utils.getClusterKey(splitValues[1], false,clusterArr);
    } else {
      obj = Utils.getClusterKey(splitValues[0], true,clusterArr);
    }

    TopologyREST.getSourceComponentClusters(sourceParams, obj.id).then((response)=>{
      let clusterObj = clusterArr.find((c)=>{return c.id == obj.id;});
      if(response && clusterObj){
        clusterObj.config.topic = response[obj.id].hints.topic;
        tempObj.topic = response[obj.id].hints.topic;
        tempObj.bootstrapServers = response[obj.id].hints.bootstrapServers["PLAINTEXT"];
      }
      this.setState({
        clusterName: obj.key,
        streamObj: '',
        formData: tempObj,
        clusterArr: clusterArr
      }, () => {
        this.updateClusterFields();
      });
    });
  }

  updateClusterFields(name) {
    const {clusterArr, clusterName, streamObj, formData,configJSON} = this.state;
    const {FormData} = this.refs.Form.state;

    const mergeData = Utils.deepmerge(formData,FormData);
    let tempFormData = _.cloneDeep(mergeData);
    let stateObj = {};

    const {obj,tempData} = Utils.mergeFormDataFields(name, clusterArr, clusterName, tempFormData, configJSON);
    stateObj.configJSON = obj;
    stateObj.formData = tempData;
    if(clusterArr.length === 0 && formData.cluster !== ''){
      let tempObj = this.props.configData.topologyComponentUISpecification.fields;
      tempObj.unshift(Utils.clusterField());
      stateObj.configJSON = tempObj;
      FSReactToastr.error(
        <CommonNotification flag="error" content={'Cluster is not available'}/>, '', toastOpt);
    }
    this.setState(stateObj);
  }

  validateData() {
    let validDataFlag = false;
    if (!this.state.fetchLoader) {
      const {isFormValid, invalidFields} = this.refs.Form.validate();
      if (isFormValid) {
        validDataFlag = true;
        this.setState({activeTabKey: 1, showRequired: true});
      }else{
        const invalidField = invalidFields[0];

        if(invalidField.props.fieldJson.isOptional === false
            && invalidField.props.fieldJson.hint
            && invalidField.props.fieldJson.hint.indexOf('security_') > -1){
          this.setState({
            activeTabKey: 4,
            showRequired: false,
            showSecurity: true
          });
        }else if(invalidField.props.fieldJson.isOptional === false){
          this.setState({
            activeTabKey: 1,
            showRequired: true,
            showSecurity: false
          });
        }
      }
      if (this.streamObj.fields.length === 0) {
        validDataFlag = false;
        FSReactToastr.error(
          <CommonNotification flag="error" content={"Output stream fields cannot be blank."}/>, '', toastOpt);
      }
    }
    return validDataFlag;
  }

  handleSave(name) {
    let {topologyId, versionId, nodeType} = this.props;
    let nodeId = this.nodeData.id;
    let data = this.refs.Form.state.FormData;
    this.nodeData.config.properties = data;
    this.nodeData.name = name;
    if (this.nodeData.outputStreams.length > 0) {
      this.nodeData.outputStreams[0].fields = this.streamObj.fields;
      this.nodeData.outputStreams[0].streamId = this.streamObj.streamId;
    } else {
      this.nodeData.outputStreams.push({fields: this.streamObj.fields, streamId: this.streamObj.streamId, topologyId: topologyId});
    }
    this.nodeData.description = this.state.description;
    return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {
      body: JSON.stringify(this.nodeData)
    });
  }

  showOutputStream(resultArr,flag) {
    let tempConfigJson = _.cloneDeep(this.state.configJSON);
    let tempFormData = Utils.deepmerge(this.state.formData,this.refs.Form.state.FormData);
    if(flag){
      this.streamObj = this.updateStreamObj(resultArr, tempFormData.topic);
    } else {
      this.streamObj.fields = [];

      tempFormData[this.schemaVersionKeyName] = '';
      tempConfigJson = Utils.populateSchemaVersionOptions(resultArr,tempConfigJson);
    }
    this.setState({streamObj : this.streamObj, configJSON : tempConfigJson,formData:tempFormData});
  }

  showSchemaBranches(resultArr) {
    let tempConfigJson = _.cloneDeep(this.state.configJSON);
    let tempFormData = Utils.deepmerge(this.state.formData,this.refs.Form.state.FormData);
    tempFormData[this.schemaBranchKeyName] =  tempFormData[this.schemaBranchKeyName] || 'MASTER';
    this.fetchSchemaVersions(tempFormData);
    this.streamObj.fields = [];
    tempFormData[this.schemaVersionKeyName] = '';
    _.map(tempConfigJson, (config) => {
      if(config.hint !== undefined && config.hint.indexOf(this.schemaVersionKeyName) !== -1){
        config.options = [];
      }
    });
    tempConfigJson = Utils.populateSchemaBranchOptions(resultArr,tempConfigJson,this.schemaTopicKeyName);
    this.setState({configJSON : tempConfigJson,formData:tempFormData});
  }

  updateStreamObj = (resultArr, topicName) => {
    return {
      streamId: topicName + '_' + this.nodeData.id,
      fields: resultArr
    };
  }

  onSelectTab = (eventKey) => {
    let stateObj={},activeTabKey =1,showRequired=true,showSecurity=false;
    stateObj.formData = Utils.deepmerge(this.state.formData,this.refs.Form.state.FormData);
    if (eventKey == 1) {
      activeTabKey =1;
      showRequired=true;
      showSecurity=false;
    } else if (eventKey == 2) {
      activeTabKey =2;
      showRequired=false;
      showSecurity=false;
    } else if (eventKey == 3) {
      activeTabKey =3;
    } else if (eventKey == 4) {
      activeTabKey =4;
      showRequired=false;
      showSecurity=true;
    }
    stateObj.activeTabKey = activeTabKey;
    stateObj.showRequired = showRequired;
    stateObj.showSecurity = showSecurity;
    this.setState(stateObj);
  }

  handleNotesChange(description) {
    this.setState({description: description});
  }

  handleSecurityProtocol = (securityKey) => {
    const {clusterArr,formData,clusterName} = this.state;
    const {cluster} = formData;
    let {Errors,FormData} = this.refs.Form.state;
    let tempObj = Utils.deepmerge(formData,FormData);
    if(clusterName !== undefined){
      const tempData =  Utils.mapSecurityProtocol(clusterName,securityKey,tempObj,clusterArr);
      delete Errors.bootstrapServers;
      this.refs.Form.setState({Errors});
      this.setState({formData : tempData ,securityType : securityKey});
    }
  }

  render() {
    const {configJSON, fetchLoader, securityType, hasSecurity, activeTabKey, formErrors} = this.state;
    let formData = this.state.formData;

    let fields = Utils.genFields(configJSON, [], formData,[], securityType, hasSecurity,'');
    const disabledFields = this.props.testRunActivated ? true : !this.props.editMode;
    const form = fetchLoader
      ? <div className="col-sm-12">
          <div className="loading-img text-center" style={{
            marginTop: "100px"
          }}>
            <img src="styles/img/start-loader.gif" alt="loading"/>
          </div>
        </div>
      : <div className="source-modal-form position-absolute">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          <Form ref="Form" readOnly={disabledFields} showRequired={this.state.showRequired}
            showSecurity={this.state.showSecurity} className="customFormClass"
            FormData={formData} Errors={formErrors}
            populateClusterFields={this.populateClusterFields.bind(this)}
            callback={this.showOutputStream.bind(this)}
            schemaBranchesCallback={this.showSchemaBranches.bind(this)}
            handleSecurityProtocol={this.handleSecurityProtocol.bind(this)}
          >
            {fields}
          </Form>
        </Scrollbars>
      </div>;
    const outputSidebar = <StreamsSidebar ref="StreamSidebar" streamObj={this.state.streamObj} streamKind="output"/>;
    return (
      <Tabs id="SinkForm" activeKey={this.state.activeTabKey} className="modal-tabs" onSelect={this.onSelectTab}>
        <Tab eventKey={1} title="REQUIRED">
          {outputSidebar}
          {activeTabKey == 1 || activeTabKey == 3 ? form : null}
        </Tab>
        {
        this.state.hasSecurity ?
        <Tab eventKey={4} title="SECURITY">
          {outputSidebar}
          {activeTabKey == 4 ? form : null}
        </Tab>
        : ''
        }
        <Tab eventKey={2} title="OPTIONAL">
          {outputSidebar}
          {activeTabKey == 2 ? form : null}
        </Tab>
        <Tab eventKey={3} title="NOTES">
          <NotesForm ref="NotesForm" description={this.state.description} editable={disabledFields} onChangeDescription={this.handleNotesChange.bind(this)}/>
        </Tab>
      </Tabs>
    );
  }
}
