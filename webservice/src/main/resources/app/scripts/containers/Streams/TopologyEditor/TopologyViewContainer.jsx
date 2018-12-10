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
import ReactDOM, {findDOMNode} from 'react-dom';
import {DragDropContext, DropTarget} from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import {ItemTypes, Components, toastOpt} from '../../../utils/Constants';
import BaseContainer from '../../BaseContainer';
import {Link, withRouter} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import ViewModeREST from '../../../rest/ViewModeREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import {OverlayTrigger, Tooltip, Accordion, Panel} from 'react-bootstrap';
import TopologyGraphComponent from '../../../components/TopologyGraphComponent';
import FSReactToastr from '../../../components/FSReactToastr';
import {observer} from 'mobx-react';
import {observable} from 'mobx';
import _ from 'lodash';
import moment from 'moment';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import Modal from '../../../components/FSModal';
import CommonNotification from '../../../utils/CommonNotification';
import {TopologyEditorContainer} from './TopologyEditorContainer';
import TopologyViewMode from './TopologyViewMode';
import ZoomPanelComponent from '../../../components/ZoomPanelComponent';
import CommonLoaderSign from '../../../components/CommonLoaderSign';
import ErrorStatus from '../../../components/ErrorStatus';
import app_state from '../../../app_state';
import UserRoleREST from '../../../rest/UserRoleREST';
import TopologyViewModeMetrics from './TopologyViewModeMetrics';
import EditorGraph from '../../../components/EditorGraph';
import MetricsREST from '../../../rest/MetricsREST';
import ProjectREST from '../../../rest/ProjectREST';

@observer
class TopologyViewContainer extends TopologyEditorContainer {
  constructor(props) {
    super(props);
    this.topologyId = this.props.params.id;
    this.versionId = 1;
    this.versionName = '';
    this.customProcessors = [];
    this.showLogSearch = false;
    this.fetchData();
    this.checkAuth = true;
    this.sampleInputNotify = false;
  }

  componentDidUpdate(){}

  setRouteLeaveHook(){}

  componentWillUnmount() {
    document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
  }

  @observable viewMode = true;
  @observable modalTitle = '';
  modalContent = () => {};

  state = {
    topologyName: '',
    topologyMetric: '',
    altFlag: true,
    isAppRunning: false,
    topologyStatus: '',
    unknown: '',
    bundleArr: null,
    availableTimeSeriesDb: false,
    fetchLoader: true,
    fetchMetrics: true,
    startDate: moment().subtract(30, 'minutes'),
    endDate: moment(),
    activeRangeLabel: null,
    viewModeData: {
      topologyMetrics: {},
      sourceMetrics: [],
      processorMetrics: [],
      sinkMetrics: [],
      taskMetrics: [],
      selectedMode: 'Overview',
      selectedComponentId: '',
      overviewMetrics: {},
      timeSeriesMetrics: {},
      componentLevelActionDetails:{},
      sampleTopologyLevel : '',
      logTopologyLevel : 'None',
      durationTopologyLevel :  0
    },
    executionInfoPageSize: 5,
    executionInfoPage: 0,
    executionInfo: {},
    selectedExecution: {},
    selectedExecutionComponentsStatus: []
  };
  getDeploymentState(){}

  fetchNameSpace(isAppRunning, data){
    if(isAppRunning){
      EnvironmentREST.getNameSpace(data.topology.namespaceId).then((res) => {
        const namespaceData = res;
        if(namespaceData.responseMessage !== undefined){
          this.checkAuth = false;
        } else {
          if (namespaceData.mappings.length) {
            let mapObj = namespaceData.mappings.find((m) => {
              return m.serviceName.toLowerCase() === 'storm';
            });
            if (mapObj) {
              this.stormClusterId = mapObj.clusterId;
            }
            let infraObj = namespaceData.mappings.find((m) => {
              return m.serviceName.toLowerCase() === 'ambari_infra_solr';
            });
            if (infraObj) {
              this.showLogSearch = true;
            }
          }
        }
        this.setState({availableTimeSeriesDb:namespaceData.namespace !== undefined
                ? namespaceData.namespace.timeSeriesDB
                  ? true
                  : false
                : false});
      });
    }
  }

  onFetchedData(){
    const {isAppRunning} = this.state;
    if(isAppRunning) {
      this.fetchCatalogInfoAndMetrics(this.state.startDate.toDate().getTime(), this.state.endDate.toDate().getTime());
      this.fetchTopologyLevelSampling();
    }
  }

  fetchTopologyLevelSampling(){
    const {viewModeData} = this.state;
    if(this.engine.type != 'stream'){
      return;
    }
    ViewModeREST.getTopologySamplingStatus(this.topologyId).then((result)=>{
      if(result.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      } else {
        viewModeData.sampleTopologyLevel = result.enabled ? Number(result.pct) : 0;
        this.setState({viewModeData});
      }
    });
  }

  handleTopologyLevelDetails = (type,value) => {
    switch(type){
    case 'LOGS' : this.handleTopologyLevelLogs(value);
      break;
    case 'SAMPLE' : this.topologyLevelInputSampleChange(value);
      break;
    case 'DURATIONS' : this.handleTopologyLevelDurations(value);
      break;
    default : break;
    }
  }

  handleTopologyLevelLogs = (value) => {
    let tempViewModeData = JSON.parse(JSON.stringify(this.state.viewModeData));
    tempViewModeData.logTopologyLevel = value;
    this.setState({viewModeData : tempViewModeData},() => {
      this.triggerUpdateGraph();
    });
  }

  handleTopologyLevelDurations = (value) => {
    let tempViewModeData = JSON.parse(JSON.stringify(this.state.viewModeData));
    tempViewModeData.durationTopologyLevel = value;
    this.setState({viewModeData : tempViewModeData}, () => {
      this.triggerUpdateGraph();
    });
  }

  topologyLevelInputSampleChange = (value) => {
    let tempViewModeData = _.cloneDeep(this.state.viewModeData);
    if(value !== 'disable' && value !== 'enable'){
      tempViewModeData.sampleTopologyLevel = value;
      this.sampleInputNotify = true;
      this.setState({viewModeData : tempViewModeData});
    } else if(value === 'enable' || value === 'disable'){
      if(this.sampleInputNotify){
        this.handleTopologyLevelSample(value);
      }
    }
  }

  disabledTopologyLevelSampling = () => {
    this.topologyLevelInputSampleChange(0);
  }

  handleTopologyLevelSample = (value) => {
    const {viewModeData} = this.state;
    const {sampleTopologyLevel} = viewModeData;
    const val = value !== 'disable' ? sampleTopologyLevel : '';
    const status = value === 'disable' ? 'disable' : 'enable';
    ViewModeREST.postTopologySamplingStatus(this.topologyId,status,val)
    .then((res)=>{
      if(res.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={res.responseMessage}/>, '', toastOpt);
      } else {
        viewModeData.sampleTopologyLevel = res.pct !== undefined ? res.pct : 0;
        this.setState({viewModeData}, () => {
          this.sampleInputNotify = false;
          const statusText = status === 'disable' ? 'disabled' : 'enabled';
          const msg = <strong>Sampling {statusText} successfully</strong>;
          FSReactToastr.success(msg);
          this.fetchComponentLevelDetails(this.graphData.nodes);
        });
      }
    });
  }

  fetchComponentLevelDetails = (allGraphNodes) => {
    let promiseArr=[],that=this;
    _.map(allGraphNodes, (node) => {
      promiseArr.push(ViewModeREST.getComponentSamplingStatus(this.topologyId,node.nodeId));
    });

    Promise.all(promiseArr).then((results) => {
      let errorMsg='';
      _.map(results, (result) => {
        if(result.responseMessage !== undefined){
          errorMsg = result.responseMessage;
        }
      });
      if(!!errorMsg){
        FSReactToastr.error(
          <CommonNotification flag="error" content={errorMsg}/>, '', toastOpt);
      }
      let compSampling=[];
      _.map(results, (result) => {
        // This is to Identify the Samplings API, Which response contains of percentage 'pct'
        if(result.pct){
          const val = result.enable ? result.pct : that.state.viewModeData.sampleTopologyLevel ;
          compSampling.push(this.populateComponentLevelSample(result));
        }
        const viewDataObj =  JSON.parse(JSON.stringify(that.state.viewModeData));
        viewDataObj.componentLevelActionDetails.samplings = compSampling;
        viewDataObj.componentLevelActionDetails.logs = [];
        viewDataObj.componentLevelActionDetails.durations = [];
        that.setState({viewModeData : viewDataObj},() => {
          this.triggerUpdateGraph();
        });
      });
    });
  }

  getEditorProps = () => {
    const {selectedExecutionComponentsStatus} = this.state;
    return {
      selectedExecutionComponentsStatus: selectedExecutionComponentsStatus
    };
  }

  onSelectExecution = (ex) => {
    const {executionInfo, viewModeData} = this.state;
    ex.loading = true;

    this.setState({executionInfo: executionInfo}, () => {
      ViewModeREST.getComponentExecutions(this.topologyId, ex.executionDate).then((res) => {
        const selectedExecutionComponentsStatus = res.components || [];
        ex.loading = false;

        const taskMetrics = [];

        _.each(selectedExecutionComponentsStatus, (compEx) => {
          const timeSeriesMetricsData = _.find(this.batchTimeseries || [], (d) => {
            return d.component.id == compEx.componentId;
          });
          const compMetrics = {};
          compMetrics.component = {
            id: compEx.componentId
          };
          compMetrics.overviewMetrics = {
            metrics: compEx
          };
          compMetrics.timeSeriesMetrics = timeSeriesMetricsData.timeSeriesMetrics;
          taskMetrics.push(compMetrics);
        });

        viewModeData.taskMetrics = taskMetrics;


        viewModeData.topologyMetrics = {
          overviewMetrics: {
            metrics: ex
          }
        };
        this.setState({
          selectedExecution: ex,
          selectedExecutionComponentsStatus: selectedExecutionComponentsStatus,
          viewModeData: viewModeData
        }, () => {
          this.syncComponentData();
          this.triggerUpdateGraph();
        });
      });
    });
  }
  getPrevPageExecutions = () => {
    const {executionInfoPageSize} = this.state;
    this.setState({executionInfoPageSize: executionInfoPageSize+1}, () => {
      this.fetchExecutions();
    });
  }
  getNextPageExecutions = () => {
    const {executionInfoPageSize} = this.state;
    this.setState({executionInfoPageSize: executionInfoPageSize-1}, () => {
      this.fetchExecutions();
    });
  }

  fetchExecutions = () => {
    let {viewModeData, executionInfoPageSize, executionInfoPage, startDate, endDate} = this.state;

    return ViewModeREST.getAllExecutions(this.topologyId, {
      from: startDate.unix(),
      to: endDate.unix(),
      pageSize: executionInfoPageSize,
      page: executionInfoPage
    }).then((res) => {
      this.state.executionInfo = res;

      const latestExecution = res.executions[res.executions.length - 1];

      viewModeData.topologyMetrics = {
        overviewMetrics: {
          metrics: latestExecution
        }
      };

      this.setState({
        viewModeData: viewModeData
      }, () => {
        this.onSelectExecution(latestExecution);
      });

      return res;
    });
  }

  fetchBatchTimeSeriesMetrics = () => {
    this.batchTimeseries = this.batchTimeseries || [];

    let {viewModeData, startDate, endDate, topologyData} = this.state;
    const selectedDC = _.keys(topologyData.namespaces)[0];
    const pipeline = topologyData.namespaces[selectedDC].runtimeTopologyId;
    const dc = selectedDC;

    const promiseArr = [];
    const template = _.find(app_state.enginesMetricsTemplates, (template) => {
      return this.engine.name == template.engine;
    });
    const timeSeriesMetrics = template.metricsUISpec.timeseries;
    _.each(timeSeriesMetrics, (m) => {
      _.each(m.metricKeyName, (mKey) => {
        let metricQuery = m.metricQuery;
        metricQuery = metricQuery.replace('$pipeline', pipeline);
        metricQuery = metricQuery.replace('$deployment', dc);

        const queryParams = {
          from: startDate.unix(),
          to: endDate.unix(),
          metricQuery: metricQuery,
          dc: dc
        };

        const onSuccess = (res, mKey) => {
          _.each(res, (timeseriesData, compId) => {
            let compData = _.find(this.batchTimeseries, (d, id) => {
              return compId == d.component.id;
            });
            if(!compData){
              compData = {
                component: {
                  id:compId
                },
                timeSeriesMetrics: {
                  metrics: {
                    [mKey]: timeseriesData
                  }
                }
              };
              this.batchTimeseries.push(compData);
            }else{
              compData.timeSeriesMetrics.metrics[mKey] = timeseriesData;
            }
          });
        };

        const req = ViewModeREST.getBatchTimeseries(this.topologyId, mKey, queryParams).then((res) => {
          onSuccess(res, mKey);
        }, (err) => {
          // onSuccess({"1":{"1539024600000":0,"1539028260000":0,"1539032700000":0,"1539037260000":0,"1539125040000":0,"1539208860000":0,"1539295500000":0,"1539381720000":0,"1539470580000":0,"1539554460000":0,"1539640920000":0,"1539727260000":0,"1539813660000":0,"1539900060000":0,"1539989820000":0,"1540072860000":0,"1540159620000":0,"1540245660000":0,"1540332720000":0,"1540799100000":0,"1541200500000":1,"1541282940000":0,"1541368860000":0,"1541455260000":0,"1541541660000":0,"1541628060000":0},"7":{"1539026820000":2180,"1539031980000":3708,"1539036720000":3984,"1539126540000":1478,"1539212100000":3217,"1539298320000":2779,"1539383520000":1733,"1539472320000":1650,"1539555540000":1062,"1539643800000":2616,"1539729600000":2324,"1539827340000":3666,"1539924000000":7244,"1540002420000":2322,"1540075920000":3013,"1540163700000":4066,"1540248600000":2353,"1540334040000":1302,"1540799700000":557,"1541201220000":157,"1541283180000":193,"1541369700000":381,"1541455500000":203,"1541543340000":1575,"1541628720000":533}}, mKey);
          console.error(err);
        });
        promiseArr.push(req);
      });
    });
    return promiseArr;
  }

  fetchCatalogInfoAndMetrics(fromTime, toTime) {
    let {viewModeData, executionInfoPageSize, executionInfoPage} = this.state;

    let promiseArr = [];

    if(this.engine.type == 'batch'){
      const req = this.fetchExecutions();
      promiseArr.push(req);
      const timeseriesMetricReqs = this.fetchBatchTimeSeriesMetrics();
      promiseArr.push.apply(promiseArr, [...timeseriesMetricReqs]);
    }else{
      promiseArr.push(ViewModeREST.getTopologyMetrics(this.topologyId, fromTime, toTime).then((res)=>{
        viewModeData.topologyMetrics = res;
        return res;
      }, (err) => {}));
      _.each(this.engine.componentTypes, (type) => {
        const typeInLCase = type.toLowerCase();
        promiseArr.push(ViewModeREST.getComponentMetrics(this.topologyId, typeInLCase+'s', fromTime, toTime).then((res) => {
          viewModeData[typeInLCase+'Metrics'] = res.entities;
          return res;
        }));
      });
    }

    this.setState({fetchMetrics: true});
    Promise.all(promiseArr).then((responseArr)=>{
      this.setState({viewModeData: viewModeData, fetchMetrics: false}, ()=>{
        const {graphData} = this;
        const kafkaSource = _.filter(graphData.nodes, (node) => {
          return node.parentType === "SOURCE" && node.currentType === "Kafka";
        });
        kafkaSource.length > 0 ? this.fetchKafkaOffset(kafkaSource): this.syncComponentData();
      });
      if(this.refs.metricsPanelRef){
        this.refs.metricsPanelRef.setState({loadingRecord: false});
      }
    }).catch((err) => {
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }

  fetchKafkaOffset(kafkaSourceArr) {
    let promiseArr=[];
    _.map(kafkaSourceArr, (kSource) => {
      promiseArr.push(MetricsREST.getKafkaTopicOffsetMetrics(this.topologyId,kSource.nodeId,this.state.startDate.toDate().getTime(),this.state.endDate.toDate().getTime()));
    });

    Promise.all(promiseArr).then((results) => {
      _.map(results, (result) => {
        if(result.responseMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        }
      });
      let tempViewModeData = this.state.viewModeData;
      _.map(kafkaSourceArr, (kSource,i) => {
        const index = _.findIndex(tempViewModeData.sourceMetrics, (sMetric) => sMetric.component.id === kSource.nodeId);
        if(index !== -1){
          tempViewModeData.sourceMetrics[index].timeSeriesMetrics.kafkaLagOffset = results[i];
        }
      });
      this.setState({viewModeData : tempViewModeData}, () => {this.syncComponentData();});
    });
  }

  syncComponentData() {
    let {viewModeData, fetchMetrics} = this.state;
    let {selectedComponentId, selectedComponent} = viewModeData;
    let overviewMetrics, timeSeriesMetrics;

    if(fetchMetrics) {
      return;
    }
    if(selectedComponent) {
      const typeInLCase = selectedComponent.parentType.toLowerCase();

      let compObj = viewModeData[typeInLCase+'Metrics'].find((entity)=>{
        return entity.component.id === selectedComponentId;
      });;

      overviewMetrics = compObj.overviewMetrics;
      timeSeriesMetrics = compObj.timeSeriesMetrics;
    } else {
      overviewMetrics = viewModeData.topologyMetrics.overviewMetrics;
      timeSeriesMetrics = viewModeData.topologyMetrics.timeSeriesMetrics;
    }
    viewModeData.overviewMetrics = overviewMetrics;
    viewModeData.timeSeriesMetrics = timeSeriesMetrics;
    this.setState({viewModeData: viewModeData});
  }
  compSelectCallback = (id, obj) => {
    let {viewModeData} = this.state;
    viewModeData.selectedComponentId = id;
    viewModeData.selectedComponent = obj;
    this.syncComponentData();
  }
  handleVersionChange(value) {
    this.fetchData(value);
  }
  datePickerCallback = (startDate, endDate, activeRangeLabel) => {
    this.refs.metricsPanelRef.setState({loadingRecord: true});
    this.setState({
      startDate: startDate,
      endDate: endDate,
      activeRangeLabel: activeRangeLabel ? activeRangeLabel : null
    }, ()=>{
      this.fetchCatalogInfoAndMetrics(startDate.toDate().getTime(), endDate.toDate().getTime());
    });
  }
  modeSelectCallback = (selectedMode) => {
    let {viewModeData} = this.state;
    viewModeData.selectedMode = selectedMode;
    this.setState({
      viewModeData: viewModeData
    }, () => {
      if(viewModeData.selectedMode === "Sample"){
        this.context.router.push({
          pathname : 'sampling/'+this.topologyId,
          state : {
            graphData : this.graphData,
            selectedComponentId : this.state.viewModeData.selectedComponentId,
            topologyId : this.topologyId,
            topologyName : this.state.topologyName
          }
        });
      } else {
        this.triggerUpdateGraph();
      }
    });
  }
  setModalContent(node, updateGraphMethod, content) {
    if (typeof content === 'function') {
      this.modalContent = content;
      this.processorNode = node.parentType.toLowerCase() === 'processor'
        ? true
        : false;
      this.setState({
        altFlag: !this.state.altFlag
      }, () => {
        this.node = node;
        this.modalTitle = this.node.uiname;
        this.refs.NodeModal.show();
        this.updateGraphMethod = updateGraphMethod;
      });
    }
  }
  getCustomProcessors() {
    return this.processorConfigArr.filter((o) => {
      return o.subType === 'CUSTOM';
    });
  }
  getTopologyHeader() {
    const {projectData, topologyName} = this.state;
    if(projectData){
      return (
        <span>
          <Link to="/">My Projects</Link>
          <span className="title-separator">/</span>
          {projectData.name}
          <span className="title-separator">/</span>
          <Link to={"/projects/"+projectData.id+"/applications"}>My Application</Link>
          <span className="title-separator">/</span>
          View: {topologyName}
        </span>
      );
    } else {
      return '';
    }
  }
  setCurrentVersion() {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to set this version as your current one?'}).then((confirmBox) => {
      TopologyREST.activateTopologyVersion(this.topologyId, this.versionId).then(result => {
        if (result.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Version switched successfully</strong>
          );
          this.fetchData();
        }
      });
      confirmBox.cancel();
    }, () => {});
  }
  handleSaveNodeModal() {
    this.refs.NodeModal.hide();
  }
  getTitleFromId(id) {
    if (id && this.props.versionsArr != undefined) {
      let obj = this.props.versionsArr.find((o) => {
        return o.id == id;
      });
      if (obj) {
        return obj.name;
      }
    } else {
      return '';
    }
  }
  zoomAction(zoomType) {
    this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.zoomAction(zoomType);
  }

  componentLevelAction = (type,componentId,value) => {
    if(type === "SAMPLE"){
      this.postComponentLevelSample(this.topologyId,componentId,value);
    }
  }

  postComponentLevelSample = (topologyId,componentId,value) => {
    const val = value === 0 ? '' : value;
    const status = value === 0 ? 'disable' : 'enable';
    ViewModeREST.postComponentSamplingStatus(topologyId,componentId,status,val).then((result) => {
      if(result.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      } else {
        const tempViewMode = JSON.parse(JSON.stringify(this.state.viewModeData));
        result.enabled = status === "disable" ? false : true;
        const newObj = this.populateComponentLevelSample(result);
        const index = _.findIndex(tempViewMode.componentLevelActionDetails.samplings, (old) => old.componentId === newObj.componentId);
        if(index !== -1){
          tempViewMode.componentLevelActionDetails.samplings[index] = newObj;
        }
        this.setState({viewModeData : tempViewMode}, () => {
          const statusText = status === 'disable' ? 'disabled' : 'enabled';
          const msg = <strong>Component sampling {statusText} successfully</strong>;
          FSReactToastr.success(msg);
          this.triggerUpdateGraph();
        });
      }
    });
  }

  populateComponentLevelSample = (sampleObj) => {
    const val = sampleObj.enabled ? sampleObj.pct : 0 ;
    return {componentId : sampleObj.componentId, duration : val,enabled : sampleObj.enabled };
  }

  triggerUpdateGraph = () => {
    this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.updateGraph();
  }

  render() {
    const {fetchLoader,allACL, viewModeData, startDate, endDate} = this.state;
    let nodeType = this.node
      ? this.node.currentType
      : '';
    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" headerContent={this.getTopologyHeader()}>
        <div className="topology-view-mode-container">
          {fetchLoader
            ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"viewMode"}/>]
            : <div>
              {
                this.checkAuth
                ? [<ZoomPanelComponent
                    mode="view"
                    router={this.props.router}
                    projectId={this.projectId}
                    topologyId={this.topologyId}
                    lastUpdatedTime={this.lastUpdatedTime}
                    versionName={this.versionName}
                    zoomInAction={this.graphZoomAction.bind(this, 'zoom_in')}
                    zoomOutAction={this.graphZoomAction.bind(this, 'zoom_out')}
                    showConfig={this.showConfig.bind(this)}
                    confirmMode={this.confirmMode.bind(this)}
                  />,
                  <TopologyViewMode
                    allACL={allACL} key={"1"} {...this.state}
                    runtimeObj={this.runtimeObj}
                    projectId={this.projectData.id}
                    topologyId={this.topologyId}
                    killTopology={this.killTopology.bind(this)}
                    handleVersionChange={this.handleVersionChange.bind(this)}
                    setCurrentVersion={this.setCurrentVersion.bind(this)}
                    datePickerCallback={this.datePickerCallback}
                    modeSelectCallback={this.modeSelectCallback}
                    stormClusterId={this.state.stormClusterId}
                    nameSpaceName={this.nameSpace}
                    namespaceId={this.namespaceId}
                    showLogSearchBtn={this.showLogSearch}
                    topologyLevelDetailsFunc={this.handleTopologyLevelDetails}
                    disabledTopologyLevelSampling={this.disabledTopologyLevelSampling}
                    engine={this.engine}
                   />,
                  <div id="viewMode" className="graph-bg" key={"2"}>
                    {this.getEditorGraph()}
                  </div>]
                : <ErrorStatus imgName={"viewMode"} />
              }
            </div>
}
        </div>
        <Modal ref="NodeModal" bsSize={this.processorNode
          ? "large"
          : null} dialogClassName={this.viewMode && (nodeType.toLowerCase() === 'join' || nodeType.toLowerCase() === 'window')
          ? "modal-xl"
          : "modal-fixed-height"} data-title={this.modalTitle} data-resolve={this.handleSaveNodeModal.bind(this)}>
          {this.modalContent()}
        </Modal>
        {this.state.isAppRunning && this.graphData.nodes.length > 0 && this.versionName.toLowerCase() == 'current' ?
        <TopologyViewModeMetrics
          ref="metricsPanelRef"
          {...this.state}
          topologyId={this.topologyId}
          topologyName={this.state.topologyName}
          components={this.graphData.nodes}
          compSelectCallback={this.compSelectCallback}
          datePickerCallback={this.datePickerCallback}
          engine={this.engine}
          onSelectExecution={this.onSelectExecution}
          getPrevPageExecutions={this.getPrevPageExecutions}
          getNextPageExecutions={this.getNextPageExecutions}
        />
        : null}
      </BaseContainer>
    );
  }
}

TopologyViewContainer.contextTypes = {
  router: PropTypes.object.isRequired
};

export default withRouter(TopologyViewContainer);
