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
import TopologyUtils from '../../../utils/TopologyUtils';
import TopologyREST from '../../../rest/TopologyREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import Form from '../../../libs/form';
import StreamsSidebar from '../../../components/StreamSidebar';
import NotesForm from '../../../components/NotesForm';
import ClusterREST from '../../../rest/ClusterREST';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import CommonNotification from '../../../utils/CommonNotification';
import {Scrollbars} from 'react-custom-scrollbars';

export default class SinkNodeForm extends Component {
  static propTypes = {
    nodeData: PropTypes.object.isRequired,
    configData: PropTypes.object.isRequired,
    editMode: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    versionId: PropTypes.number.isRequired,
    sourceNodes: PropTypes.array.isRequired
  };

  constructor(props) {
    super(props);
    this.sourceNodesId = [];
    this.sourceChildNodeType = [];
    // this.tempStreamFieldArr = [];
    props.sourceNodes.map((node) => {
      this.sourceNodesId.push(node.nodeId);
    });
    this.state = {
      formData: {},
      inputStreamArr : [],
      streamObj: {},
      description: '',
      showRequired: true,
      showSecurity: false,
      hasSecurity: false,
      activeTabKey: 1,
      uiSpecification: [],
      clusterArr: [],
      clusterName: '',
      fetchLoader: true,
      securityType : '',
      validSchema: false,
      formErrors:{}
    };
    this.fetchNotifier().then(() => {
      this.fetchData();
    });
    this.schemaTopicKeyName = '';
    this.schemaVersionKeyName = '';
    this.schemaBranchKeyName = '';
  }

  getChildContext() {
    return {ParentForm: this};
  }

  fetchData() {
    let {
      topologyId,
      versionId,
      nodeType,
      nodeData,
      sourceNodes,
      namespaceId
    } = this.props;
    const sourceParams = nodeData.parentType + '/' + nodeData.topologyComponentBundleId;
    let stateExecuted = false;
    let sourceNodeType = null,sourceNodePromiseArr= [];
    let promiseArr = [
      TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId),
      TopologyREST.getAllNodes(topologyId, versionId, 'edges'),
      EnvironmentREST.getAllNamespaceFromService("kafka")
    ];
    if (sourceNodes.length > 0) {
      _.map(sourceNodes, (sourceNode) => {
        sourceNodePromiseArr.push(TopologyREST.getNode(topologyId, versionId ,TopologyUtils.getNodeType(sourceNode.parentType) ,sourceNode.nodeId));
      });
    }
    Promise.all(promiseArr).then(results => {
      let stateObj = {}, hasSecurity = false,
        tempArr = [];
      this.nodeData = results[0];
      if (results[1].entities) {
        let tempStreamArr = [];
        results[1].entities.map((edge) => {
          if (edge.toId === nodeData.nodeId && this.sourceNodesId.indexOf(edge.fromId) !== -1) {
            //TODO - Once we support multiple input streams, need to fix this.
            TopologyREST.getNode(topologyId, versionId, 'streams', edge.streamGroupings[0].streamId).then(streamResult => {
              tempStreamArr.push(streamResult);
              this.setState({inputStreamArr: tempStreamArr, streamObj :tempStreamArr[0],streamObjArr : tempStreamArr.length > 1 ? tempStreamArr : []});
            });
          }
        });
      }
      if (results[2].responseMessage !== undefined) {
        this.setState({fetchLoader: false});
        FSReactToastr.error(
          <CommonNotification flag="error" content={results[2].responseMessage}/>, '', toastOpt);
      } else {
        const clusters = results[2].entities;
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
      if(this.nodeData.config.properties.cluster){
        const sourceParams = nodeData.parentType + '/' + nodeData.topologyComponentBundleId;
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
      if (stateObj.clusterArr && stateObj.clusterArr.length > 0) {
        stateObj.uiSpecification = this.pushClusterFields(tempArr);
      }
      stateObj.formData = this.nodeData.config.properties;
      stateObj.description = this.nodeData.description;
      stateObj.formData.nodeType = this.props.nodeData.parentType;
      stateObj.fetchLoader = false;
      stateObj.securityType = stateObj.formData.securityProtocol || '';
      stateObj.hasSecurity = hasSecurity;
      stateObj.validSchema = true;
      this.schemaTopicKeyName = Utils.getSchemaKeyName(stateObj.uiSpecification,'schema');
      this.setState(stateObj, () => {
        stateExecuted = true;
        if (stateObj.formData.clusters !== undefined) {
          this.updateClusterFields(stateObj.formData.clusters);
        }
      });

      Promise.all(sourceNodePromiseArr).then(connectedNodes => {
        _.map(connectedNodes, (connectedNode) => {
          if(connectedNode.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={connectedNode.responseMessage}/>, '', toastOpt);
          }
        });

        let sourcePromiseArr = [];
        _.map(connectedNodes, (connectedNode,index) => {
          // sourceChildNodeType are processor nodes inner child, window or rule
          let type = sourceNodes[index].currentType.toLowerCase();
          this.sourceChildNodeType[index] = type === 'window'
            ? 'windows'
            : (type === 'rule' || type === 'projection'
              ? 'rules'
              : 'branchrules');

          if (connectedNode.config.properties && connectedNode.config.properties.rules && connectedNode.config.properties.rules.length > 0) {
            connectedNode.config.properties.rules.map((id) => {
              sourcePromiseArr.push(TopologyREST.getNode(topologyId, versionId, this.sourceChildNodeType[index], id));
            });
          }
        });

        Promise.all(sourcePromiseArr).then(sourceResults => {
          this.allSourceChildNodeData = sourceResults;
        });
      });
    });
  }

  fetchFields = () => {
    let obj = this.props.configData.topologyComponentUISpecification.fields;
    let flag = this.props.configData.cluster;
    const clusterFlag = obj.findIndex(x => {
      return x.fieldName === 'clusters';
    });
    if (clusterFlag === -1 && flag) {
      obj.unshift(Utils.clusterField());
    }
    return obj;
  }

  pushClusterFields = (opt) => {
    const uiSpecification = this.fetchFields();
    const obj = uiSpecification.map(x => {
      if (x.fieldName === 'clusters') {
        x.options = opt;
      }
      return x;
    });
    return obj;
  }

  fetchNotifier = () => {
    return ClusterREST.getAllNotifier().then(notifier => {
      if (notifier.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={notifier.responseMessage}/>, '', toastOpt);
      } else {
        const obj = notifier.entities.filter(x => {
          return x.name.indexOf("Email Notifier") !== -1;
        });

        let {configData} = this.props;
        const {topologyComponentUISpecification} = configData;
        let uiFields = topologyComponentUISpecification.fields || [];

        uiFields.map(x => {
          if (x.fieldName === "jarFileName") {
            x.defaultValue = obj.length >= 1 ? obj[0].jarFileName : '';
            if (x.hint !== undefined && x.hint.indexOf('hidden') === -1) {
              x.hint = x.hint + ',hidden';
            } else {
              x.hint = "hidden";
            }
          }
          if (x.fieldName === "notifierName") {
            x.defaultValue = obj.length >= 1 ? obj[0].name : '';
            if (x.hint !== undefined && x.hint.indexOf('hidden') === -1) {
              x.hint = x.hint + ',hidden';
            } else {
              x.hint = "hidden";
            }
          }
          if (x.fieldName === "className") {
            x.defaultValue = obj.length >= 1 ? obj[0].className : '';
            if (x.hint !== undefined && x.hint.indexOf('hidden') === -1) {
              x.hint = x.hint + ',hidden';
            } else {
              x.hint = "hidden";
            }
          }
        });
        this.setState({uiSpecification: uiFields});
      }
    }).catch(err => {
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
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
      if(!this.state.validSchema){
        validDataFlag = false;
      }
    }
    return validDataFlag;
  }

  handleSave(name) {
    let {topologyId, versionId, nodeType, nodeData} = this.props;
    const {uiSpecification,inputStreamArr} = this.state;
    let nodeId = this.nodeData.id;
    let data = this.refs.Form.state.FormData;
    delete data.nodeType;
    this.nodeData.config.properties = data;
    let oldName = this.nodeData.name;
    this.nodeData.name = name;
    this.nodeData.description = this.state.description;
    let promiseArr = [TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {
      body: JSON.stringify(this.nodeData)
    })
    ];

    if (this.allSourceChildNodeData && this.allSourceChildNodeData.length > 0) {
      this.allSourceChildNodeData.map((childData,index) => {
        let child = childData;
        let obj = child.actions.find((o) => {
          return o.outputStreams[0] == (inputStreamArr[index] !== undefined ? inputStreamArr[index].streamId : '') && o.name === 'notifierAction';
        });
        if (obj) {
          if (nodeData.currentType.toLowerCase() == 'notification') {
            obj.outputFieldsAndDefaults = this.nodeData.config.properties.fieldValues || {};
            obj.notifierName = this.nodeData.config.properties.notifierName || '';
          }
          promiseArr.push(TopologyREST.updateNode(topologyId, versionId, this.sourceChildNodeType[index], child.id, {body: JSON.stringify(child)}));
        }
      });
    }
    return Promise.all(promiseArr);
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

  populateClusterFields(val) {
    const {clusterArr} = this.state;
    const {nodeData} = this.props;
    const sourceParams = nodeData.parentType + '/' + nodeData.topologyComponentBundleId;
    const tempObj = Object.assign({}, this.state.formData, {topic: '', bootstrapServers: ''});
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
        formData: tempObj,
        clusterArr: clusterArr
      }, () => {
        this.updateClusterFields();
      });
    });
  }

  updateClusterFields(name) {
    const {clusterArr, clusterName, formData,uiSpecification} = this.state;
    const {FormData} = this.refs.Form.state;

    const mergeData = Utils.deepmerge(formData,FormData);
    let tempFormData = _.cloneDeep(mergeData);
    let stateObj = {};

    const {obj,tempData} = Utils.mergeFormDataFields(name,clusterArr, clusterName, tempFormData, uiSpecification);
    stateObj.uiSpecification = obj;
    stateObj.formData = tempData;
    if(clusterArr.length === 0 && formData.cluster !== ''){
      let tempObj = this.props.configData.topologyComponentUISpecification.fields;
      tempObj.unshift(Utils.clusterField());
      stateObj.uiSpecification = tempObj;
      FSReactToastr.error(
        <CommonNotification flag="error" content={'Cluster is not available'}/>, '', toastOpt);
    }
    this.setState(stateObj);
  }

  validateTopic(resultArr,flag){
    if(!flag){
      let configJSON = _.cloneDeep(this.state.uiSpecification);
      let tempFormData = Utils.deepmerge(this.state.formData,this.refs.Form.state.FormData);
      configJSON =  Utils.populateSchemaVersionOptions(resultArr,configJSON);
      let validate =  false;
      validate = _.isEmpty(resultArr) ? false : true;
      tempFormData[this.schemaVersionKeyName] = '';
      this.setState({validSchema: validate,uiSpecification :configJSON,formData :tempFormData });
    }
  }

  validateBranches(resultArr) {
    let configJSON = _.cloneDeep(this.state.uiSpecification);
    let tempFormData = Utils.deepmerge(this.state.formData,this.refs.Form.state.FormData);
    _.map(configJSON, (config) => {
      if(config.hint !== undefined && config.hint.indexOf(this.schemaVersionKeyName) !== -1){
        config.options = [];
      }
    });
    configJSON = Utils.populateSchemaBranchOptions(resultArr,configJSON,this.schemaTopicKeyName);
    let validate =  false;
    validate = _.isEmpty(resultArr) ? false : true;
    tempFormData[this.schemaBranchKeyName] = tempFormData[this.schemaBranchKeyName] || 'MASTER';
    this.fetchSchemaVersions(tempFormData);
    tempFormData[this.schemaVersionKeyName] = '';
    this.setState({validSchema: validate,uiSpecification : configJSON,formData:tempFormData});
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
    let {
      formData,
      streamObj = {},
      streamObjArr = [],
      uiSpecification,
      fetchLoader,
      securityType,
      hasSecurity,
      activeTabKey,
      formErrors,
      inputStreamArr
    } = this.state;

    let fields = Utils.genFields(uiSpecification, [], formData, inputStreamArr, securityType, hasSecurity,'sink');
    const disabledFields = this.props.testRunActivated ? true : !this.props.editMode;
    const form = fetchLoader
      ? <div className="col-sm-12">
          <div className="loading-img text-center" style={{
            marginTop: "100px"
          }}>
            <img src="styles/img/start-loader.gif" alt="loading"/>
          </div>
        </div>
      : <div className="sink-modal-form">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          <Form ref="Form" readOnly={disabledFields} showRequired={this.state.showRequired} showSecurity={this.state.showSecurity} FormData={formData} Errors={formErrors} className="customFormClass" populateClusterFields={this.populateClusterFields.bind(this)}  callback={this.validateTopic.bind(this)} schemaBranchesCallback={this.validateBranches.bind(this)} handleSecurityProtocol={this.handleSecurityProtocol.bind(this)}>
            {fields}
          </Form>
        </Scrollbars>
      </div>;
    const inputSidebar = <StreamsSidebar ref="StreamSidebar" streamObj={streamObj} inputStreamOptions={streamObjArr} streamKind="input"/>;
    return (
      <Tabs id="SinkForm" activeKey={this.state.activeTabKey} className="modal-tabs" onSelect={this.onSelectTab}>
        <Tab eventKey={1} title="REQUIRED">
          {inputSidebar}
          {activeTabKey == 1 || activeTabKey == 3 ? form : null}
        </Tab>
        {
        this.state.hasSecurity ?
        <Tab eventKey={4} title="SECURITY">
          {inputSidebar}
          {activeTabKey == 4 ? form : null}
        </Tab>
        : ''
        }
        <Tab eventKey={2} title="OPTIONAL">
          {inputSidebar}
          {activeTabKey == 2 ? form : null}
        </Tab>
        <Tab eventKey={3} title="NOTES">
          <NotesForm ref="NotesForm" description={this.state.description} editable={disabledFields} onChangeDescription={this.handleNotesChange.bind(this)}/>
        </Tab>
      </Tabs>
    );
  }
}

SinkNodeForm.childContextTypes = {
  ParentForm: PropTypes.object
};
