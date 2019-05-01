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
import ReactDOM, {findDOMNode} from 'react-dom';
import update from 'react/lib/update';
import {ItemTypes, Components, toastOpt,pageSize} from '../../../utils/Constants';
import BaseContainer from '../../BaseContainer';
import {Link, withRouter} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import {OverlayTrigger, Tooltip, Popover, Accordion, Panel} from 'react-bootstrap';
import Switch from 'react-bootstrap-switch';
import TopologyConfig from './TopologyConfigContainer';
import EdgeConfig from './EdgeConfigContainer';
import FSReactToastr from '../../../components/FSReactToastr';
import {observer} from 'mobx-react';
import {observable} from 'mobx';
import _ from 'lodash';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import Modal from '../../../components/FSModal';
import Editable from '../../../components/Editable';
import state from '../../../app_state';
import CommonNotification from '../../../utils/CommonNotification';
import AnimatedLoader from '../../../components/AnimatedLoader';
import CommonLoaderSign from '../../../components/CommonLoaderSign';
import TestSourceNodeModal from '../TestRunComponents/TestSourceNodeModal';
import TestSinkNodeModal from '../TestRunComponents/TestSinkNodeModel';
import ZoomPanelComponent from '../../../components/ZoomPanelComponent';
import VersionControl from '../../../components/VersionControl';
import RightSideBar from '../../../components/RightSideBar';
import {
  StreamEditorGraph,
  BatchEditorGraph
} from '../../../components/EditorGraph';
import TestRunREST from '../../../rest/TestRunREST';
import ProjectREST from '../../../rest/ProjectREST';
import {Select2 as Select,Creatable} from '../../../utils/SelectUtils';
import EventGroupPagination from '../../../components/EventGroupPagination';
import {
  getAllTestCase,
  SaveTestSourceNodeModal,
  deleteAllEventLogData,
  deleteTestCase,
  checkConfigureTestCaseType,
  updateTestCase,
  downloadTestFileCallBack,
  excuteTestCase,
  populatePaginationData,
  syncNodeDataAndEventLogData,
  fetchSingleEventLogData
} from '../../../utils/TestModeUtils/TestModeUtils';
import moment from 'moment';

@observer
export class TopologyEditorContainer extends Component {
  constructor(props) {
    super(props);
    this.projectId = this.props.params.projectId;
    this.topologyId = this.props.params.id;
    this.versionId = 1;
    this.versionName = '';
    this.lastUpdatedTime = '';
    this.customProcessors = [];
    this.fetchData();
    this.getDeploymentState();
    this.tempIntervalArr = [];
  }
  componentDidUpdate() {
    this.state.fetchLoader
      ? ''
      : document.getElementsByTagName('body')[0].classList.add('graph-bg');
    document.querySelector('.editorHandler').setAttribute("class", "editorHandler contentEditor-wrapper animated fadeIn ");
  }
  componentWillMount() {
    state.showComponentNodeContainer = true;
  }
  componentWillUnmount() {
    clearInterval(this.interval);
    document.getElementsByTagName('body')[0].classList.remove('graph-bg');
    document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
    document.querySelector('.editorHandler').setAttribute("class", "editorHandler contentEditor-wrapper animated fadeIn ");
  }

  @observable viewMode = false;
  @observable modalTitle = '';
  modalContent = () => {};

  showHideComponentNodeContainer() {
    state.showComponentNodeContainer = !state.showComponentNodeContainer;
  }

  state = {
    topologyName: '',
    altFlag: true,
    isAppRunning: false,
    topologyStatus: '',
    progressCount: 0,
    progressBarColor: 'blue',
    fetchLoader: true,
    mapSlideInterval: [],
    topologyTimeSec: 0,
    defaultTimeSec : 0,
    deployStatus : 'DEPLOYING_TOPOLOGY',
    testRunActivated : false,
    selectedTestObj : '',
    testCaseList : [],
    testCaseLoader : true,
    testSourceConfigure : [],
    testSinkConfigure : [],
    eventLogData : [],
    testName : '',
    showError : false,
    hideEventLog : true,
    testHistory : {},
    testCompleted : false,
    deployFlag : false,
    testRunDuration: 30,
    testRunningMode :false,
    abortTestCase : false,
    activePage : 1,
    activePageList : [],
    allEventObj : {}
  };

  getStatusFromNamespaces(namespacesObj){
    let status = 'unknown';
    let namespaceObj = namespacesObj[this.namespaceName];
    if(namespaceObj.status){
      status = namespaceObj.status.toLowerCase();
    }
    this.runtimeAppId = namespaceObj.runtimeAppId;
    this.runtimeAppUrl = namespaceObj.runtimeAppUrl;
    return status;
  }

  syncNamespaceObj(namespaces){
    let namespaceObj = {};
    namespaces.map((obj)=>{
      namespaceObj[obj.namespaceName] = obj;
    });
    return namespaceObj;
  }

  fetchData(versionId) {

    TopologyREST.getAllVersions(this.topologyId).then((response)=>{
      this.versionId = versionId;
      let versions = response.entities;

      Utils.sortArray(versions, 'timestamp', false);

      if(this.viewMode && !this.versionId){
        this.versionId = versions.length > 1 ? versions[1].id : versions[0].id;
        this.versionName = versions.length > 1 ? versions[1].name : versions[0].name;
      } else if(!this.viewMode && !this.versionId){
        this.versionId = versions[0].id;
        this.versionName = versions[0].name;
      } else if(this.versionId){
        versions.map((versionObj)=>{
          if(versionObj.id == this.versionId){
            this.versionName = versionObj.name;
          }
        });
      }

      if(versions.length > 1){
        this.deployedVersion = versions[1].name;
      }

      let promises = [
        TopologyREST.getTopologyWithoutMetrics(this.topologyId, versionId),
        TopologyREST.getTopologyActionStatus(this.topologyId, versionId)
      ];
      Promise.all(promises).then((responses)=>{
        // responses[1].entities[0].start_time = 1555943794000;
        // responses[1].entities[0].end_time = 1556174194000;
        // responses[1].entities[0].units = 'hours';
        // responses[1].entities[0].timeInterval = 1;
        let data = {
          topology: responses[0],
          namespaces: this.syncNamespaceObj(responses[1].entities)
        };

        this.engine = Utils.getEngineById(data.topology.engineId);
        this.template = Utils.getTemplateById(data.topology.templateId);

        if(typeof this.template.config === 'string'){
          this.template.config = JSON.parse(this.template.config);
        }

        this.namespaceName = _.keys(data.namespaces)[0];
        this.namespaceId = data.namespaces[this.namespaceName].namespaceId;
        this.lastUpdatedTime = new Date(data.topology.timestamp);
        this.statusObj = data.namespaces[this.namespaceName];

        this.topologyName = data.topology.name;
        this.topologyConfig = (data.topology.config && data.topology.config.properties) ? data.topology.config.properties : {};
        this.topologyTimeSec = this.topologyConfig["topology.message.timeout.secs"];

        this.status = this.getStatusFromNamespaces(data.namespaces);
        let isAppRunning = this.getAppRunningStatus(this.status);

        let promiseArr = [];
        promiseArr.push(TopologyREST.getTopologyConfig(data.topology.engineId, data.topology.templateId));
        promiseArr.push(ProjectREST.getProject(this.projectId));
        promiseArr.push(TopologyREST.getMetaInfo(this.topologyId, this.versionId));

        this.fetchNameSpace(isAppRunning, this.namespaceId);

        Promise.all(promiseArr).then((resultsArr) => {

          this.topologyConfigData = resultsArr[0].entities[0] || [];
          this.projectData = resultsArr[1];

          let defaultTimeSecVal = this.getDefaultTimeSec(this.topologyConfigData);

          this.graphData.metaInfo = _.extend(this.graphData.metaInfo, JSON.parse(resultsArr[2].data));

          this.setState({
            topologyData: data.topology,
            topologyNamespaces: data.namespaces,
            timestamp: data.topology.timestamp,
            topologyName: this.topologyName,
            isAppRunning: isAppRunning,
            topologyStatus: this.status,
            topologyVersion: this.versionId,
            versionsArr: versions,
            fetchLoader: false,
            mapTopologyConfig: this.topologyConfig,
            topologyTimeSec: this.topologyTimeSec,
            defaultTimeSec : defaultTimeSecVal,
            topologyNameValid: true,
            projectData: this.projectData
          }, () => {
            this.onFetchedData();
          });
        });
      });
    });
    this.graphData = {
      nodes: [],
      edges: [],
      uinamesList: [],
      linkShuffleOptions: [],
      metaInfo: {
        graphTransforms: {
          dragCoords: [
            0, 0
          ],
          zoomScale: 0.8
        }
      }
    };
  }

  fetchNameSpace(isAppRunning, namespaceId){
    /*Namespace required for view mode*/
  }

  onFetchedData(){
    /*Required For view mode */
  }

  //To check if a user is deploying the topology
  getDeploymentState(topology) {
    this.interval = setInterval(() => {
      TopologyREST.deployTopologyState(this.topologyId).then((topologyState) => {
        if(topologyState.responseMessage === undefined){
          if(topologyState.name.indexOf('TOPOLOGY_STATE_DEPLOYED')  !== -1){
            this.setState({deployStatus : topologyState.name}, () => {
              const clearTimer = setTimeout(() => {
                clearInterval(this.interval);
                if(this.refs.deployLoadingModal){
                  this.refs.deployLoadingModal.hide();
                }
                if(topology) {
                  this.saveTopologyVersion(topology.timestamp);
                } else {
                  const isAppRunning = this.getAppRunningStatus(this.status);
                  this.setState({topologyStatus: this.status, progressCount: 0,isAppRunning});
                  // this.fetchData();
                }
              },1000);
            });
          } else if (topologyState.name.indexOf('TOPOLOGY_STATE_DEPLOYMENT_FAILED') !== -1 || topologyState.name.indexOf('TOPOLOGY_STATE_SUSPENDED') !== -1 || topologyState.name.indexOf('TOPOLOGY_STATE_INITIAL') !== -1) {
            this.setState({deployStatus : topologyState.name}, () => {
              const clearTimer = setTimeout(() => {
                clearInterval(this.interval);
                if(this.refs.deployLoadingModal){
                  this.refs.deployLoadingModal.hide();
                }
                if(topology) {
                  this.setState({topologyStatus: this.status});
                  FSReactToastr.error(
                    <CommonNotification flag="error" content={topologyState.description}/>, '', toastOpt);
                } else {
                  const isAppRunning = this.getAppRunningStatus(this.status);
                  this.setState({topologyStatus: this.status || 'NOT RUNNING', progressCount: 0,isAppRunning});
                }
              },1000);
            },1000);
          } else {
            if(topology === undefined) {
              if(this.refs.deployLoadingModal){
                this.refs.deployLoadingModal.show();
              }
              this.setState({topologyStatus: 'DEPLOYING...', progressCount: 12});
            }
          }
          this.setState({deployStatus : topologyState.name});
        } else {
          if(topology) {
            this.setState({topologyStatus: this.status});
            this.refs.deployLoadingModal.hide();
            FSReactToastr.error(
              <CommonNotification flag="error" content={topologyState.responseMessage}/>, '', toastOpt);
          }
          clearInterval(this.interval);
        }
      });
    },3000);
  }

  // get the App Running status
  getAppRunningStatus = (status) => {
    let isAppRunning = false;
    if (status && (status == 'enabled' || status == 'inactive' || status == 'paused')) {
      isAppRunning = true;
    }
    return isAppRunning;
  }

  // get the default time sec of topologyName
  getDefaultTimeSec(data){
    const fields = data.topologyComponentUISpecification.fields || [];
    const obj = _.find(fields, (field) => {
      return field.fieldName === "topology.message.timeout.secs";
    });
    return obj ? obj.defaultValue : 0;
  }

  // fetchProcessors on graph render
  fetchProcessors() {
    const {topologyVersion, topologyTimeSec, topologyName} = this.state;
    TopologyREST.getAllNodes(this.topologyId, topologyVersion, 'processors').then((processor) => {
      if (processor.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={processor.responseMessage}/>, '', toastOpt);
      } else {
        this.topologyConfig["topology.message.timeout.secs"] = topologyTimeSec;
        if (processor.entities.length > 0) {
          // this.processorSlideInterval(processor.entities);
          this.refs.EditorGraph.child.decoratedComponentInstance.processorSlideInterval(processor.entities);
        } else {
          this.tempIntervalArr = [];
          this.setState({
            mapTopologyConfig: this.topologyConfig,
            mapSlideInterval: this.tempIntervalArr
          }, () => {
            this.setTopologyConfig(topologyName, topologyVersion);
          });
        }
      }
    });
  }
  setTopologyConfig = (topologyName, topologyVersion) => {
    let dataObj = {
      name: topologyName,
      config: {
        properties: this.state.mapTopologyConfig
      },
      namespaceId: this.namespaceId,
      projectId: this.projectData.id,
      engineId: this.state.topologyData.engineId,
      templateId: this.state.topologyData.templateId
    };
    this.setState({
      mapSlideInterval: this.tempIntervalArr
    }, () => {
      TopologyREST.putTopology(this.topologyId, topologyVersion, {body: JSON.stringify(dataObj)});
    });
  }
  topologyConfigMessageCB(id) {
    const {topologyTimeSec} = this.state;
    this.tempIntervalArr = this.state.mapSlideInterval;
    if (id) {
      this.topologyConfig["topology.message.timeout.secs"] = topologyTimeSec;
      const index = this.tempIntervalArr.findIndex((x) => {
        return x.id === id;
      });
      if (index !== -1) {
        this.tempIntervalArr.splice(index, 1);
      }
      this.setState({
        mapSlideInterval: this.tempIntervalArr
      }, () => {
        FSReactToastr.success(
          <strong>Component deleted successfully</strong>
        );
        this.fetchProcessors();
      });
    }
  }
  showConfig() {
    this.refs.TopologyConfigModal.show();
  }
  handleNameChange(e) {
    let name = e.target.value;
    this.validateName(name);
    this.setState({topologyName: name});
  }
  validateName(name) {
    if (name.trim === '') {
      this.refs.topologyNameEditable.setState({errorMsg: "Topology name cannot be blank"});
      this.setState({topologyNameValid: false});
      return false;
    } else if (/[^A-Za-z0-9_\-\s]/g.test(name)) { //matches any character that is not a alphanumeric, underscore or hyphen
      this.refs.topologyNameEditable.setState({errorMsg: "Topology name contains invalid characters"});
      this.setState({topologyNameValid: false});
      return false;
    } else if (!/[A-Za-z0-9]/g.test(name)) { //to check if name contains only special characters
      this.refs.topologyNameEditable.setState({errorMsg: "Topology name is not valid"});
      this.setState({topologyNameValid: false});
      return false;
    } else {
      this.refs.topologyNameEditable.setState({errorMsg: ""});
      this.setState({topologyNameValid: true});
      return true;
    }
  }
  saveTopologyName() {
    let {topologyName, mapTopologyConfig, topologyData} = this.state;
    if (this.validateName(topologyName)) {
      let data = {
        name: topologyName,
        config: {
          properties: mapTopologyConfig
        },
        namespaceId: this.namespaceId,
        projectId: this.projectData.id,
        engineId: topologyData.engineId,
        templateId: topologyData.templateId
      };
      TopologyREST.putTopology(this.topologyId, this.versionId, {body: JSON.stringify(data)}).then(topology => {
        if (topology.responseMessage !== undefined) {
          let errorMag = topology.responseMessage.indexOf('already exists') !== -1
            ? "Workflow with same name already exists. Please choose a unique Workflow Name"
            : topology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Workflow name updated successfully</strong>
          );
          this.topologyName = topology.name;
          this.topologyConfig = (topology.config && topology.config.properties) ? topology.config.properties : {};
          this.setState({mapTopologyConfig: this.topologyConfig});
        }
        if(this.refs.TopologyNameSpace.state.show){
          this.refs.TopologyNameSpace.hide();
        } else {
          this.refs.topologyNameEditable.hideEditor();
        }
      });
    }
  }
  handleEditableReject() {
    this.setState({topologyName: this.topologyName});
    this.refs.topologyNameEditable.setState({
      errorMsg: ""
    }, () => {
      this.refs.topologyNameEditable.hideEditor();
    });
  }
  handleSaveConfig() {
    this.refs.topologyConfig.handleSave().then(config => {
      this.refs.TopologyConfigModal.hide();
      if (config.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={config.responseMessage}/>, '', toastOpt);
      } else {
        FSReactToastr.success(
          <strong>Configuration updated successfully</strong>
        );
        this.topologyName = config.name;
        this.topologyConfig = (config.config && config.config.properties) ? config.config.properties : {};
        this.lastUpdatedTime = new Date(config.timestamp);
        this.setState({topologyName: this.topologyName, mapTopologyConfig: this.topologyConfig},() => {
          if(this.state.deployFlag){
            this.deployTopology();
          }
        });
      }
    });
  }
  getModalScope(node) {
    return this.refs.EditorGraph.child.decoratedComponentInstance.getModalScope(node);
  }
  deployTopology() {
    // this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to deploy this Workflow?'}).then((confirmBox) => {
    this.refs.deployLoadingModal.show();
    this.setState({topologyStatus: 'DEPLOYING...', progressCount: 12,deployFlag : false});
    TopologyREST.validateTopology(this.topologyId, this.versionId).then(result => {
      if (result.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        this.refs.deployLoadingModal.hide();
        this.setState({topologyStatus: this.status});
      } else {
        TopologyREST.deployTopology(this.topologyId, this.versionId).then(topology => {
          if (topology.responseMessage !== undefined) {
            FSReactToastr.error(
              <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
            this.refs.deployLoadingModal.hide();
            this.setState({topologyStatus: this.status});
          } else {
            this.getDeploymentState(topology);
          }
        });
      }
    });
    //   confirmBox.cancel();
    // }, () => {});
  }
  saveTopologyVersion(timestamp){
    FSReactToastr.success(
      <strong>Workflow Deployed Successfully</strong>
    );
    this.lastUpdatedTime = new Date(timestamp);
    this.setState({
      altFlag: !this.state.altFlag
    });
    const thumbElStr = this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.svg.node().outerHTML;
    let versionData = {
      name: 'V' + this.state.topologyVersion,
      description: 'version description auto generated',
      dagThumbnail: thumbElStr
    };
    TopologyREST.saveTopologyVersion(this.topologyId, {body: JSON.stringify(versionData)}).then((versionResponse) => {
      let versions = this.state.versionsArr;
      let savedVersion = _.find(versions, {id: versionResponse.id});
      savedVersion.name = versionResponse.name;

      TopologyREST.getTopology(this.topologyId).then((result) => {
        let data = result;
        this.status = this.getStatusFromNamespaces(data.namespaces);
        let isAppRunning = this.getAppRunningStatus(this.status);
        this.versionId = data.topology.versionId;
        versions.push({id: data.topology.versionId, topologyId: this.topologyId, name: "CURRENT", description: ""});
        this.setState({isAppRunning: isAppRunning, topologyStatus: this.status, topologyVersion: data.topology.versionId, versionsArr: versions});
      });
    });
  }
  killTopology() {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to stop this Workflow?'}).then((confirmBox) => {
      document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay";
      this.setState({topologyStatus: 'KILLING...'});
      TopologyREST.killTopology(this.topologyId).then(topology => {
        if (topology.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
          document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
          this.setState({topologyStatus: this.status});
        } else {
          this.lastUpdatedTime = new Date(topology.timestamp);
          FSReactToastr.success(
            <strong>Workflow Stopped Successfully</strong>
          );
          TopologyREST.getTopologyActionStatus(this.topologyId, this.versionId).then((result)=>{
            this.status = this.getStatusFromNamespaces(this.syncNamespaceObj(result.entities));
            let isAppRunning = this.getAppRunningStatus(this.status);
            document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
            this.setState({isAppRunning: isAppRunning, topologyStatus: this.status});
          });
        }
      });
      confirmBox.cancel();
    }, () => {});
  }
  setModalContent(node, updateGraphMethod, content,currentEdges, allNodes) {
    if (typeof content === 'function') {
      this.modalContent = content;
      this.tempGraphNode = allNodes;
      this.processorNode = node.parentType.toLowerCase() === 'processor'
        ? true
        : false;
      this.setState({
        altFlag: !this.state.altFlag,
        testRunCurrentEdges : currentEdges,
        nodeData : node
      }, () => {
        const nodeText = node.parentType.toLowerCase();
        this.node = node;
        this.modalTitle = this.state.testRunActivated ? (nodeText === "source" || nodeText === "sink") ? `TEST-${this.node.parentType}`  : this.node.uiname : this.node.uiname;
        /*
          On the bases of nodeText and testCaseList.length
          we show the Modal || info notification on UI
        */
        this.state.testRunActivated
        ? nodeText === "source" && this.state.testCaseList.length
          ? this.refs.NodeModal.show()
          : nodeText === "sink" && this.state.testCaseList.length
            ? this.refs.NodeModal.show()
            : nodeText === "source" || nodeText === "sink"
              ? FSReactToastr.info(
                <CommonNotification flag="error" content={`Please create atleast one Test Case to configure ${nodeText}`}/>, '', toastOpt)
              : this.refs.NodeModal.show()
        : (this.node.currentType.toLowerCase() === 'rt-join' && currentEdges.filter((o)=>{return o.target === node;}).length !== 2)
          ? FSReactToastr.info(
            <CommonNotification flag="error" content={`Two incoming streams are required for configuring ${this.node.uiname} processor`}/>, '', toastOpt)
          :  this.refs.NodeModal.show();
        this.updateGraphMethod = updateGraphMethod;
      });
    }
  }
  handleSaveNodeName(editable) {
    if (this.validateNodeName(this.modalTitle)) {
      this.handleSaveNodeModal(this);
      editable.hideEditor();
    }
  }
  handleRejectNodeName(editable) {
    this.modalTitle = this.node.uiname;
    editable.hideEditor();
  }
  handleNodeNameChange(e) {
    let isValid = this.validateNodeName(e.target.value);
    this.modalTitle = e.target.value;
  }
  validateNodeName(name) {
    let nodeNamesList = this.graphData.uinamesList;
    if (name === '') {
      this.refs.editableNodeName.setState({errorMsg: "Node name cannot be blank"});
      return false;
    // } else if (name.search(' ') !== -1) {
    //   this.refs.editableNodeName.setState({errorMsg: "Node name cannot have space in between"});
    //   return false;
    } else if (nodeNamesList.indexOf(name) !== -1) {
      this.refs.editableNodeName.setState({errorMsg: "Node name is already present. Please use some other name."});
      this.validateFlag = false;
      return false;
    } else {
      this.refs.editableNodeName.setState({errorMsg: ""});
      return true;
    }
  }
  handleRejectTopologyName(editable) {
    this.setState({topologyName: this.topologyName});
    editable.hideEditor();
  }
  getValidationData(){
    const promiseObj = this.refs.ConfigModal.validateData();
    if(Utils.isPromise(promiseObj)){
      promiseObj.then((res) => {
        let flag=false;
        if(res){
          this.handleNodeModalSaveCallBack();
        }
      });
    } else {
      return promiseObj;
    }
  }
  handleSaveNodeModal() {
    if (!this.viewMode) {
      const validator = this.getValidationData();
      const validBoolean = _.isBoolean(validator) ? validator : false;
      if (validBoolean) {
        this.handleNodeModalSaveCallBack();
      }
    } else {
      this.refs.NodeModal.hide();
    }
  }

  handleNodeModalSaveCallBack() {
    //Make the save request
    this.refs.ConfigModal.handleSave(this.modalTitle).then((savedNode) => {
      let errorMsg='';
      if (savedNode instanceof Array) {
        if (this.node.currentType.toLowerCase() === 'window' || this.node.currentType.toLowerCase() === 'join' || this.node.currentType.toLowerCase() === 'rt-join') {
          let updatedEdges = [];
          savedNode.map((n, i) => {
            if (i > 0 && n.streamGroupings) {
              updatedEdges.push(n);
            }
          });
          TopologyUtils.updateGraphEdges(this.graphData.edges, updatedEdges);
        }
        this.refs.EditorGraph.child.decoratedComponentInstance.processorSlideInterval(savedNode);
        _.map(savedNode, (node) => {
          if(node.responseMessage !== undefined){
            errorMsg = node.responseMessage;
          }
        });
        savedNode = savedNode[0];
      }
      if (savedNode.responseMessage !== undefined) {
        let msg = savedNode.responseMessage;
        if (savedNode.responseMessage.indexOf("Stream with empty fields") !== -1) {
          msg = "Output stream fields cannot be blank.";
        }
        FSReactToastr.error(
          <CommonNotification flag="error" content={msg}/>, '', toastOpt);
      } else {
        this.syncEdgeData(savedNode);
        this.lastUpdatedTime = new Date(savedNode.timestamp);
        this.setState({
          altFlag: !this.state.altFlag
        });
        if (_.keys(savedNode.config.properties).length > 0) {
          this.node.isConfigured = true;
          const index = _.findIndex(this.tempGraphNode,(t) => { return t.nodeId === this.node.nodeId;});
          if(index !== -1){
            this.tempGraphNode[index].reconfigure = false;
          }
        }
        let i = this.graphData.uinamesList.indexOf(this.node.uiname);
        if (this.node.currentType === 'Custom') {
          let obj = _.find(this.graphData.metaInfo.customNames, {uiname: this.node.uiname});
          obj.uiname = savedNode.name;
          this.node.uiname = savedNode.name;
          TopologyUtils.updateMetaInfo(this.topologyId, this.versionId, this.node, this.graphData.metaInfo);
        }
        this.node.uiname = savedNode.name;
        this.node.parallelismCount = savedNode.config.properties.parallelism || 1;
        if (i > -1) {
          this.graphData.uinamesList[i] = this.node.uiname;
        }

        if(errorMsg === ''){
          //Show notifications from the view
          FSReactToastr.success(
            <strong>{this.node.uiname} updated successfully.</strong>
          );
          //render graph again
          this.reconfigurationNode();
        } else {
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMsg}/>, '', toastOpt);
        }
      }
    });
  }

  //this is only required for those components who are added via template and
  //treated as a one time activity to update the edges with appropriate output streams
  syncEdgeData(componentNode){
    let edgesArr = this.refs.EditorGraph.child.decoratedComponentInstance.edgesArr;
    let updateEdgesArr = [];
    edgesArr.map((o)=>{
      if(o.fromId === componentNode.id && o.streamGroupings.length === 0){
        updateEdgesArr.push(o);
      }
    });
    if(updateEdgesArr.length > 0){
      let promiseArr = [];
      //source can have multiple outgoing edges
      updateEdgesArr.map((edge)=>{
        edge.streamGroupings = [{
          streamId: componentNode.outputStreams[0].id,
          grouping: "SHUFFLE"
        }];
        //update edges via Api
        promiseArr.push(
          TopologyREST.updateNode(this.topologyId, this.versionId, 'edges', edge.id, {body: JSON.stringify(edge)})
        );
      });
      Promise.all(promiseArr).then((results)=>{
        //update edges for graph
        TopologyUtils.updateGraphEdges(this.graphData.edges, results);
      });
    }
  }

  reconfigurationNode(){
    TopologyREST.getReconfigurationNodes(this.topologyId).then((result) => {
      if(result.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      } else {
        this.setNodeConfigurationFlag(result);
        this.refs.NodeModal.hide();
      }
    });
  }

  setNodeConfigurationFlag(obj){
    let errorMsg =[];
    _.map(this.tempGraphNode,(node) => {
      const reconfigNode = _.pick(obj,node.parentType);
      if(!_.isEmpty(reconfigNode)){
        const index = _.findIndex(reconfigNode[node.parentType], (r) => {return r === node.nodeId;});
        if(index !== -1){
          errorMsg.push(node.node);
          node.reconfigure = true;
        }
      }
    });
    this.updateGraphMethod();
    if(errorMsg.length){
      this.notifyReconfigureCallback();
    }
  }

  notifyReconfigureCallback(){
    FSReactToastr.warning(
      <CommonNotification flag="warning" content={"Re-evaluate the configuration for the nodes marked in \"Yellow\""}/>, '', toastOpt);
  }

  showEdgeConfigModal(topologyId, versionId, newEdge, edges, callback, node, streamName, grouping, groupingFields) {
    this.edgeConfigData = {
      topologyId: topologyId,
      versionId: versionId,
      edge: newEdge,
      edges: edges,
      callback: callback,
      streamName: streamName,
      grouping: grouping,
      groupingFields: groupingFields
    };
    this.edgeConfigTitle = newEdge.source.uiname + '-' + newEdge.target.uiname;
    let nodeType = newEdge.source.currentType.toLowerCase();
    let promiseArr = [];
    if(newEdge.target.currentType.toLowerCase() === 'notification'){
      let targetNodeType = TopologyUtils.getNodeType(newEdge.target.parentType);
      promiseArr.push(TopologyREST.getNode(topologyId,versionId,targetNodeType,newEdge.target.nodeId));
    }
    if (node && nodeType !== 'rule' && nodeType !== 'branch') {
      let edgeData = {
        fromId: newEdge.source.nodeId,
        toId: newEdge.target.nodeId,
        streamGroupings: [
          {
            streamId: node.outputStreams[0].id,
            grouping: 'SHUFFLE'
          }
        ]
      };

      if (newEdge.target.currentType.toLowerCase() === 'window'
          || newEdge.target.currentType.toLowerCase() === 'join') {
        edgeData.streamGroupings[0].grouping = 'FIELDS';
        edgeData.streamGroupings[0].fields = null;
      }

      if (node && nodeType === 'window' || nodeType === 'projection') {
        let outputStreamObj = {};
        if (node.config.properties.rules && node.config.properties.rules.length > 0) {
          let saveRulesPromiseArr = [];
          node.config.properties.rules.map((id) => {
            promiseArr.push(TopologyREST.getNode(topologyId, versionId, nodeType === 'window' ? 'windows' : 'rules', id));
          });
          Promise.all(promiseArr).then((results) => {
            let targetNodeObj = {};
            // check the targetNode is present in result array
            if(newEdge.target.currentType.toLowerCase() === 'notification'){
              targetNodeObj = results[0];
              // remove the target node from the results Arr
              results.splice(0,1);
            }
            // only rules arr is present in results
            let rulesNodeData = results[0];
            if (newEdge.target.currentType.toLowerCase() === 'notification') {
              outputStreamObj = _.find(node.outputStreams, {streamId: rulesNodeData.outputStreams[1]});
              edgeData.streamGroupings[0].streamId = outputStreamObj.id;
            } else {
              outputStreamObj = _.find(node.outputStreams, {streamId: rulesNodeData.outputStreams[0]});
              edgeData.streamGroupings[0].streamId = outputStreamObj.id;
            }
            results.map((result) => {
              let data = result;
              let actionObj = {
                outputStreams: [outputStreamObj.streamId]
              };
              if (newEdge.target.currentType.toLowerCase() === 'notification') {
                actionObj.outputFieldsAndDefaults = targetNodeObj.config.properties.fieldValues || {};
                actionObj.notifierName = targetNodeObj.config.properties.notifierName || '';
                actionObj.name = 'notifierAction';
                actionObj.__type = "com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction";
              } else {
                actionObj.name = 'transformAction';
                actionObj.__type = "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction";
                actionObj.transforms = [];
              }
              let hasActionType = false;
              if (data.actions.length > 0) {
                data.actions.map((a) => {
                  if (a.__type === actionObj.__type) {
                    hasActionType = true;
                  }
                });
              }
              if (!hasActionType) {
                data.actions.push(actionObj);
              }
              saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType === 'window' ? 'windows' : 'rules', data.id, {body: JSON.stringify(data)}));
            });
            Promise.all(saveRulesPromiseArr).then((windowResult) => {
              TopologyREST.createNode(topologyId, versionId, 'edges', {body: JSON.stringify(edgeData)}).then((edge) => {
                newEdge.edgeId = edge.id;
                newEdge.streamGrouping = edge.streamGroupings[0];
                edges.push(newEdge);
                this.lastUpdatedTime = new Date(edge.timestamp);
                this.setState({
                  altFlag: !this.state.altFlag
                });
                //call the callback to update the graph
                callback();
              });
            });
          });
        }
      } else {
        TopologyREST.createNode(topologyId, versionId, 'edges', {body: JSON.stringify(edgeData)}).then((edge) => {
          newEdge.edgeId = edge.id;
          newEdge.streamGrouping = edge.streamGroupings ? edge.streamGroupings[0] : null;
          edges.push(newEdge);
          this.lastUpdatedTime = new Date(edge.timestamp);
          this.setState({
            altFlag: !this.state.altFlag
          });
          //call the callback to update the graph
          callback();
        });
      }
    } else {
      this.setState({
        altFlag: !this.state.altFlag
      }, () => {
        this.refs.EdgeConfigModal.show();
      });
    }
  }
  handleSaveEdgeConfig() {
    if (this.refs.EdgeConfig.validate()) {
      this.refs.EdgeConfig.handleSave();
      this.refs.EdgeConfigModal.hide();
    }
  }
  handleCancelEdgeConfig() {
    this.refs.EdgeConfigModal.hide();
  }
  focusInput(component) {
    if (component) {
      ReactDOM.findDOMNode(component).focus();
    }
  }
  getTopologyHeader() {
    const {projectData, topologyName} = this.state;
    if(projectData){
      return (
        <span>
          {Utils.isFromSharedProjects() ?
            <Link to="/shared-projects">Shared Projects</Link>
            :
            <Link to="/">My Projects</Link>
          }
          <i className="fa fa-angle-right title-separator"></i>
          <Link to={(Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+projectData.id+"/applications"}>{projectData.name}</Link>
          <i className="fa fa-angle-right title-separator"></i>
          <Editable id="applicationName" ref="topologyNameEditable" inline={true} resolve={this.saveTopologyName.bind(this)} reject={this.handleRejectTopologyName.bind(this)}>
            <input ref={this.focusInput} defaultValue={this.state.topologyName} onKeyPress={this.handleKeyPress.bind(this)} onChange={this.handleNameChange.bind(this)}/>
          </Editable>
        </span>
      );
    } else {
      return '';
    }
  }
  graphZoomAction(zoomType) {
    this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.zoomAction(zoomType);
  }
  setLastChange(timestamp) {
    if (timestamp) {
      this.lastUpdatedTime = new Date(timestamp);
    }
    this.setState({
      altFlag: !this.state.altFlag
    });
  }
  handleKeyPress(event) {
    const that = this;
    if(event.key === "Enter" && event.target.nodeName.toLowerCase() === "input" && this.refs.TopologyNameSpace.state.show){
      this.saveTopologyName(this);
    }else if (event.key === "Enter" && event.target.nodeName.toLowerCase() != "textarea" && event.target.nodeName.toLowerCase() != 'button') {
      this.refs.TopologyConfigModal.state.show
        ? this.handleSaveConfig(this)
        : '';
      this.refs.NodeModal.state.show
        ? this.handleSaveNodeModal(this)
        : '';
      this.refs.EdgeConfigModal.state.show
        ? this.handleSaveEdgeConfig(this)
        : '';
      this.refs.TestSourceNodeModal.state.show
        ? this.handleSaveTestSourceNodeModal()
        : '';
      this.refs.modeChangeModal.state.show
        ? this.modeChangeConfirmModal(this,true)
        : '';
    }
  }

  /*
    runTestClicked invoke getAllTestCase methods
    And if the testRunActivated is true set all the test case variable to empty
  */
  runTestClicked(){
    if(!this.state.testRunActivated){
      if(this.graphData.nodes.length){
        // getAllTestCase " Method from TestModeUtils.js "
        getAllTestCase.call(this);
      } else {
        FSReactToastr.info(
          <CommonNotification flag="error" content={"please configure some nodes before switching to test mode"}/>, '', toastOpt);
      }
    } else {
      if(!this.state.testRunningMode){
        this.setState({testRunActivated : false ,testHistory : [] ,selectedTestObj : {}, eventLogData : [] , hideEventLog : true , testCompleted : false,activePage : 1,activePageList : []},() => {
          // deleteAllEventLogData " Method from TestModeUtils.js "
          deleteAllEventLogData.call(this);
        });
      } else {
        FSReactToastr.info(
          <CommonNotification flag="error" content={"Test case is running. Please wait till completion or stop test to navigate away."}/>, '', toastOpt);
      }
    }
  }


  /*
    handleSaveTestSourceNodeModal is call to save the TestSourceNodeModal
    And add the testCase to poolIndex for notification
    configure = GET Api call
    update = PUT Api call
  */
  handleSaveTestSourceNodeModal(){
    // SaveTestSourceNodeModal " Method from TestModeUtils.js "
    SaveTestSourceNodeModal.call(this);
  }

  /*
    testCaseListChange accept the obj select from UI
    and SET the selectedTestObj
  */
  testCaseListChange = (obj,type) => {
    if(obj){
      if(!!type){
        this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete the Test Case?'}).then((confirmBox) => {
          // deleteTestCase " Method from TestModeUtils.js "
          deleteTestCase.call(this,obj);
          confirmBox.cancel();
        }, () => {});
      } else {
        this.setState({selectedTestObj : _.isPlainObject(obj) ? obj: {}}, () => {
          this.refs.TestSourceNodeModal.show();
        });
      }
    }
  }

  addTestCaseHandler = (showAddTestModel) => {
    if(showAddTestModel){
      this.setState({selectedTestObj : {}}, () => {
        this.refs.TestSourceNodeModal.show();
      });
    }
  }

  /*
    checkConfigureTestCase accept id and nodeText
    Is called from TestSourceNodeModal and TestSinkNodeModal
    used to check whether the testCase is already configure
    by pushing the id to testSourceConfigure array vice versa for testSinkConfigure
    And SET the tempConfig usin nodeText
  */
  checkConfigureTestCase = (Id,nodeText) => {
    // checkConfigureTestCaseType " Method from TestModeUtils.js "
    checkConfigureTestCaseType.call(this,Id,nodeText);
  }

  /*
    runTestCase which trigger the confirmRunTestModal
    if testSourceConfigure is true
  */
  runTestCase(){
    this.refs.confirmRunTestModal.show();
  }

  /*
    confirmRunTest accept the true Or false
    if true we create a API call-pool on each and every 3sec for getting test results
  */
  confirmRunTest = (confirm) => {
    this.refs.confirmRunTestModal.hide();
    if(confirm){
      const {selectedTestObj} = this.state;
      this.setState({eventLogData :[] ,hideEventLog :false, testHistory : {},testCompleted : false, testRunningMode : true,abortTestCase : false,activePage : 1,activePageList : []});
      // clear all EventLogData from the graphData
      // deleteAllEventLogData " Method from TestModeUtils.js "
      deleteAllEventLogData.call(this);
      let testCaseData = { topologyId : this.topologyId , testCaseId : selectedTestObj.id };
      if(this.state.testRunDuration !== '') {
        testCaseData.durationSecs = parseInt(this.state.testRunDuration, 10);
      }
      // excuteTestCase " Method from TestModeUtils.js "
      excuteTestCase.call(this,testCaseData);
    } else {
      this.setState({testRunDuration: 30});
    }
  }

  /*
    modeChangeConfirmModal accept true Or false
    to change the mode to Dev || Test
  */
  modeChangeConfirmModal = (flag) => {
    if(flag){
      this.runTestClicked();
    }
    this.refs.modeChangeModal.hide();
  }

  /*
    confirmMode method show the modeChangeModal
  */
  confirmMode = () => {
    this.refs.modeChangeModal.show();
  }

  updateTestCaseList = (obj) => {
    // updateTestCase " Method from TestModeUtils.js "
    const testList = updateTestCase(obj,this.state.testCaseList);
    this.setState({testCaseList : testList , selectedTestObj : obj});
  }

  handleDownloadTestFile(){
    const {testHistory,eventLogData} = this.state;
    if(testHistory.id && eventLogData.length){
      this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to download the Test file?'}).then((confirmBox) => {
        // downloadTestFileCallBack  " Method from TestModeUtils.js "
        downloadTestFileCallBack.call(this,this.topologyId,testHistory.id);
        confirmBox.cancel();
      });
    }
  }

  handleDeployTopology = () => {
    if(this.graphData.nodes.length > 0){
      let allowDeploy = true;
      this.graphData.nodes.map((node)=>{
        if(allowDeploy){
          allowDeploy = node.isConfigured;
        }
      });
      if(allowDeploy){
        if(this.engine.type.toLowerCase() == 'stream' && this.graphData.edges.length == 0){
          FSReactToastr.warning(<strong>No components are connected. Please connect & configure components before deploying.</strong>);
        } else {
          this.setState({deployFlag : true}, () => {
            this.refs.TopologyConfigModal.show();
          });
        }
      } else {
        FSReactToastr.warning(<strong>One or more components are not configured. Please configure before deploying.</strong>);
      }
    } else {
      FSReactToastr.info(<strong>No components found. Please add & configure components before deploying.</strong>);
    }
  }

  handleCancelConfig = () => {
    if(this.refs.topologyConfig.refs.StepZilla.refs.activeComponent.refs.Form){
      this.refs.topologyConfig.refs.StepZilla.refs.activeComponent.refs.Form.clearErrors();
    }
    this.setState({deployFlag : false}, () => {
      this.refs.TopologyConfigModal.hide();
    });
  }

  handleTestCaseDurationChange = (e) => {
    const val = e.target.value.trim() !== '' ? e.target.value.trim() : '';
    this.setState({testRunDuration: val});
  }

  handleKillTestRun = () => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to Kill this Test Run?'}).then((confirmBox) => {
      const {testHistory} = this.state;
      TestRunREST.killTestCase(this.topologyId, testHistory.id).then((killStatus) => {
        if(killStatus.responseMessage !== undefined){
          this.setState({abortTestCase : killStatus.flagged});
          FSReactToastr.info(
            <CommonNotification flag="error" content={killStatus.responseMessage}/>, '', toastOpt);
        } else {
          this.setState({abortTestCase : killStatus.flagged,hideEventLog : true});
        }
      });
      confirmBox.cancel();
    }, () => {});
  }

  paginationCallBack = (eventKey) => {
    const {testHistory} = this.state;
    this.setState({activePage : eventKey}, () => {
      // Always call the eventGroup[0] for fetching event log data
      // fetchSingleEventLogData  " Method from TestModeUtils.js "
      fetchSingleEventLogData.call(this,this.state.activePageList[(eventKey-1)][eventKey].eventGroup[0]);
    });
  }

  deleteAllEventLogData = () => {
    _.map(this.graphData.nodes, (node) => {
      delete node.eventLogData;
    });
    this.triggerGraphUpdate();
  }

  triggerGraphUpdate = () => {
    this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.updateGraph();
  }

  getEditorGraph(){
    const {
      progressCount, progressBarColor, fetchLoader, mapTopologyConfig,
      deployStatus, testRunActivated, testCaseList, selectedTestObj, testCaseLoader,
      testRunCurrentEdges, testResult, nodeData, testName, showError, testSinkConfigure,
      nodeListArr, hideEventLog, eventLogData, testHistory, testCompleted, deployFlag,
      testRunningMode, abortTestCase, notifyCheck, activePage, activePageList, topologyData,
      isAppRunning
    } = this.state;

    let EditorComp = null;

    switch(this.engine.type){
    case 'stream':
      EditorComp = StreamEditorGraph;
      break;
    case 'batch':
      EditorComp = BatchEditorGraph;
      break;
    }

    return <EditorComp
      testRunningMode={testRunningMode}
      hideEventLog={hideEventLog}
      ref="EditorGraph"
      eventLogData={eventLogData || []}
      addTestCase={this.addTestCaseHandler}
      selectedTestObj={selectedTestObj || {}}
      testItemSelected={this.testCaseListChange}
      testCaseList={testCaseList}
      graphData={this.graphData}
      viewMode={this.viewMode}
      topologyId={this.topologyId}
      versionId={this.versionId}
      namespaceId={this.namespaceId}
      topologyConfig={this.topologyConfig}
      topologyTimeSec={this.state.topologyTimeSec}
      versionsArr={this.state.versionsArr}
      getModalScope={this.getModalScope.bind(this)}
      setModalContent={this.setModalContent.bind(this)}
      customProcessors={this.customProcessors}
      getEdgeConfigModal={this.showEdgeConfigModal.bind(this)}
      setLastChange={this.setLastChange.bind(this)}
      topologyConfigMessageCB={this.topologyConfigMessageCB.bind(this)}
      showComponentNodeContainer={state.showComponentNodeContainer}
      testRunActivated={this.state.testRunActivated}
      engine={this.engine}
      template={this.template}
      topologyData={topologyData}
      setTopologyConfig={this.setTopologyConfig}
      viewModeData={this.state.viewModeData || {}}
      isAppRunning={isAppRunning}
      compSelectCallback={this.compSelectCallback}
      versionName={this.versionName}
      {...this.getEditorProps()}
    />;
  }

  getEditorProps = () => {
    return {}; /*Required for view mode*/
  }
  handleVersionChange = (value) => {
    this.setState({
      fetchLoader: true
    },()=>{
      this.fetchData(value);
    });
  }
  setCurrentVersion = () => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to set this version as your current one?'}).then((confirmBox) => {
      this.setState({
        fetchLoader: true
      },()=>{
        TopologyREST.activateTopologyVersion(this.topologyId, this.versionId).then(result => {
          if (result.responseMessage !== undefined) {
            this.setState({
              fetchLoader: false
            });
            FSReactToastr.error(
              <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          } else {
            FSReactToastr.success(
              <strong>Version switched successfully</strong>
            );
            this.fetchData();
          }
        });
      });
      confirmBox.cancel();
    }, () => {});
  }
  getCurrentVersionThumbnail = () => {
    return this.refs.EditorGraph && this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph ?
     this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.svg.node().outerHTML
     : '';
  }
  getRightSideBar = () => {
    return <VersionControl
      versions={this.state.versionsArr}
      handleVersionChange={this.handleVersionChange}
      selectedVersionName={this.versionName}
      setCurrentVersion={this.setCurrentVersion}
      getCurrentVersionThumbnail={this.getCurrentVersionThumbnail}
      lastUpdatedTime={this.lastUpdatedTime}
    />;
  }

  render() {
    const {progressCount, progressBarColor, fetchLoader, mapTopologyConfig,deployStatus,testRunActivated,
      testCaseList,selectedTestObj,testCaseLoader,testRunCurrentEdges,testResult,nodeData,testName,showError,
      testSinkConfigure,nodeListArr,hideEventLog,eventLogData,testHistory,testCompleted,deployFlag,testRunningMode,
      abortTestCase,notifyCheck,activePage,activePageList, topologyData, isAppRunning} = this.state;
    let nodeType = this.node
      ? this.node.currentType.toLowerCase()
      : '';

    let nodeClassName = "";
    if(this.node){
      if(this.node.parentType.toLowerCase() === 'source'){
        nodeClassName = "modal-fixed-height modal-lg";
      } else if(this.node.parentType.toLowerCase() === 'sink'){
        if(nodeType === 'rta'){
          nodeClassName = "modal-fixed-height modal-xl";
        } else {
          nodeClassName = "modal-fixed-height modal-xl";
        }
      } else if(this.node.parentType.toLowerCase() === 'task'){
        nodeClassName = "modal-fixed-height";
      } else if(nodeType === 'join' || nodeType === 'window' || nodeType === 'projection' || nodeType === 'rt-join' || nodeType === 'sql'){
        nodeClassName = "modal-fixed-height modal-xl";
      } else {
        nodeClassName = "modal-fixed-height modal-xl";
      }
    }

    return (
      <BaseContainer
        ref="BaseContainer" routes={this.props.routes} onLandingPage="false"
        headerContent={this.getTopologyHeader()} siblingContent={this.getRightSideBar()}
      >
        {fetchLoader
          ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"viewMode"}/>]
          : <div className="graph-region">
            <ZoomPanelComponent
              mode="edit"
              router={this.props.router}
              projectId={this.projectId}
              topologyId={this.topologyId}
              testCompleted={testCompleted}
              lastUpdatedTime={this.lastUpdatedTime}
              versionName={this.versionName}
              zoomInAction={this.graphZoomAction.bind(this, 'zoom_in')}
              zoomOutAction={this.graphZoomAction.bind(this, 'zoom_out')}
              showConfig={this.showConfig.bind(this)}
              confirmMode={this.confirmMode.bind(this)}
              testRunActivated={testRunActivated}
              isAppRunning={isAppRunning}
              killTopology={this.killTopology.bind(this)}
              deployTopology={this.handleDeployTopology.bind(this)}
              topologyStatus={this.state.topologyStatus}
              engineType={this.engine.type}
              namespaceName={this.namespaceName}
              deployedVersion={this.deployedVersion}
            />
            {this.getEditorGraph()}
          </div>
        }
        <Modal
          className="u-form" ref="TopologyConfigModal"
          data-title={deployFlag ? "Are you sure want to continue with this configuration?" : "Workflow Configuration"}
          onKeyPress={this.handleKeyPress.bind(this)} data-resolve={this.handleSaveConfig.bind(this)}
          data-reject={this.handleCancelConfig.bind(this)} dialogClassName="modal-fixed-height config-modal"
          hideFooter={true}
        >
          <TopologyConfig ref="topologyConfig" topologyData={topologyData}
            projectId={this.projectId} topologyId={this.topologyId}
            versionId={this.versionId} data={mapTopologyConfig}
            topologyName={this.state.topologyName} uiConfigFields={this.topologyConfigData}
            testRunActivated={this.state.testRunActivated} topologyNodes={this.graphData.nodes}
            handleSaveConfig={this.handleSaveConfig.bind(this)} engine={this.engine}
          />
        </Modal>
        {/* NodeModal for Development Mode for source*/}
        <Modal className="u-form" ref="NodeModal" onKeyPress={this.handleKeyPress.bind(this)}
          // bsSize={this.processorNode && nodeType.toLowerCase() !== 'join' ? "large" : null}
          dialogClassName={nodeClassName}
          btnOkDisabled={this.state.testRunActivated}
          data-title={<Editable ref="editableNodeName" inline={true}
            resolve={this.handleSaveNodeName.bind(this)}
            reject={this.handleRejectNodeName.bind(this)} enforceFocus={true}>
            <input defaultValue={this.modalTitle} onChange={this.handleNodeNameChange.bind(this)}/>
          </Editable>}
          data-resolve={this.handleSaveNodeModal.bind(this)}>
          {this.modalContent()}
        </Modal>

        {/* TestNodeModel for TestRun Mode for source */}
        <Modal className="u-form" ref="TestSourceNodeModal" onKeyPress={this.handleKeyPress.bind(this)} dialogClassName="modal-fixed-height modal-xl" data-title={"Test Case"}
          data-resolve={this.handleSaveTestSourceNodeModal.bind(this)}>
          <TestSourceNodeModal ref="TestSourceNodeContentRef" topologyId={this.topologyId} versionId={this.versionId} nodeData={nodeData} testCaseObj={selectedTestObj || {}}  checkConfigureTestCase={this.checkConfigureTestCase} nodeListArr={nodeListArr} updateTestCaseList={this.updateTestCaseList}/>
        </Modal>

        {/*ConfirmBox to Change Mode to Dev || Test*/}
        <Modal className="u-form" ref="modeChangeModal" data-title="Confirm Box" dialogClassName="confirm-box" data-resolve={this.modeChangeConfirmModal.bind(this, true)} data-reject={this.modeChangeConfirmModal.bind(this, false)}>
          {<p> Are you sure you want change mode?</p>}
        </Modal>

        {/*ConfirmBox to Run TestCase*/}
        <Modal className="u-form" ref="confirmRunTestModal" data-title="Are you sure you want to run the test case with the following configuration ?" data-resolve={this.confirmRunTest.bind(this, true)} data-reject={this.confirmRunTest.bind(this, false)}>
          {
          <div className="test-run-modal-form">
            <div className="form-group">
              <label>Timeout Duration in Seconds</label>
              <input name="durationSecs" placeholder="Duration in seconds" onChange={this.handleTestCaseDurationChange} type="number" className="form-control" value={this.state.testRunDuration} min="0" inputMode="numeric"/>
            </div>
          </div>
          }
        </Modal>

        <Modal className="u-form" ref="EdgeConfigModal" onKeyPress={this.handleKeyPress.bind(this)} data-title={this.edgeConfigTitle} data-resolve={this.handleSaveEdgeConfig.bind(this)} data-reject={this.handleCancelEdgeConfig.bind(this)}>
          <EdgeConfig ref="EdgeConfig" data={this.edgeConfigData}/>
        </Modal>
        <Modal className="u-form" ref="deployLoadingModal" hideHeader={true} hideFooter={true}>
          <AnimatedLoader progressBar={progressCount} progressBarColor={progressBarColor} deployStatus={deployStatus}/>
        </Modal>
        <Modal
          className="u-form"
          ref="TopologyNameSpace"
          data-title={"Rename this application without spaces"}
          data-resolve={this.saveTopologyName.bind(this)}
          hideCloseBtn={true}
          closeOnEsc={false}
        >
            <div className="config-modal-form">
              <div className="form-group">
                <label>Workflow Name: <span className="text-danger">*</span></label>
                <div>
                  <input
                    type="text"
                    placeholder="Workflow name"
                    required
                    className={this.state.topologyNameValid ? "form-control" : "form-control invalidInput"}
                    value={this.state.topologyName}
                    onChange={this.handleNameChange.bind(this)}
                  />
                </div>
              </div>
            </div>
        </Modal>
        <a className="btn-download" ref="downloadTest" hidden download href=""></a>
      </BaseContainer>
    );
  }
}


export default withRouter(TopologyEditorContainer);
