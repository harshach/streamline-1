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
import Metrics from '../../../components/Metrics';
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
    // this.fetchData(); //Data being fetched from TopologyEditorContainer
    this.checkAuth = true;
    this.sampleInputNotify = false;
    this.timeseriesData = [];
    this.fetchMetrics = true;
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
    bundleArr: null,
    availableTimeSeriesDb: false,
    fetchLoader: true,
    startDate: null,
    endDate: null,
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
    executionInfoPageSize: 300,
    executionInfoPage: 0,
    executionInfo: {},
    selectedExecution: {},
    selectedExecutionComponentsStatus: []
  };

  fetchNameSpace(isAppRunning, namespaceId){
    if(isAppRunning){
      EnvironmentREST.getNameSpace(namespaceId).then((res) => {
        const namespaceData = res;
        this.selectedDataCenter = namespaceData.namespace.name;
        this.selectedDataCenterId = namespaceData.namespace.id;
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
    let time_interval = this.statusObj.extra.executionInterval;
    let time_unit = this.statusObj.extra.executionIntervalUnit;
    if(isAppRunning) {
      if(this.engine.type === 'batch'){
        let currentOffset = new Date().getTimezoneOffset();
        let latestExTimeObj = moment(this.statusObj.extra.latestExecutionDate);
        let startExTimeObj = moment(this.statusObj.extra.startExecutionDate);
        startExTimeObj.add(-(currentOffset), 'minutes');
        let startDate = startExTimeObj.valueOf();
        let timeObj = Utils.findBeginingEndingTime(null, null, latestExTimeObj, null, time_unit, time_interval, currentOffset);
        let startTime = timeObj.begining;
        let endTime = timeObj.ending;
        this.fetchCatalogInfoAndMetrics(startTime, endTime,startDate);
      } else {
        this.setState({startDate: moment().subtract(6, 'hours'), endDate: moment()},()=>{
          this.fetchCatalogInfoAndMetrics(this.state.startDate, this.state.endDate);
        });
      }
      // this.fetchCatalogInfoAndMetrics(this.state.startDate.toDate().getTime(), this.state.endDate.toDate().getTime());
      // this.fetchTopologyLevelSampling();
    }
  }

  fetchTopologyLevelSampling(){
    return;
    const {viewModeData} = this.state;
    //if(this.engine.type != 'stream'){
    //  return;
    //}
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

  onSelectExecution = (ex, viewModeData) => {
    if(ex){
      const {executionInfo} = this.state;
      if(!viewModeData){
        viewModeData = this.state.viewModeData;
      }
      ex.loading = true;
      ViewModeREST.getComponentExecutions(this.topologyId, ex.executionDate, this.selectedDataCenterId).then((res) => {
        const selectedExecutionComponentsStatus = res.components || [];
        ex.loading = false;

        const taskMetrics = [];

        _.each(selectedExecutionComponentsStatus, (compEx) => {
          const timeSeriesMetricsData = _.find(this.timeseriesData || [], (d) => {
            return d.component.id == compEx.componentId;
          });
          const compMetrics = {};
          compMetrics.component = {
            id: compEx.componentId
          };
          compMetrics.overviewMetrics = {
            metrics: compEx
          };
          compMetrics.timeSeriesMetrics = timeSeriesMetricsData ? timeSeriesMetricsData.timeSeriesMetrics : {};
          taskMetrics.push(compMetrics);
        });

        viewModeData.taskMetrics = taskMetrics;


        viewModeData.topologyMetrics = {
          overviewMetrics: {
            metrics: ex
          }
        };
        this.syncComponentData(viewModeData, {selectedExecutionComponentsStatus: selectedExecutionComponentsStatus, selectedExecution: ex});
      });
    }
  }

  fetchExecutions = (fromTime, toTime) => {
    let {viewModeData, executionInfoPageSize, executionInfoPage, startDate, endDate} = this.state;
    this.fromTimeInMS = startDate ? startDate.valueOf() : fromTime;
    this.toTimeInMS = endDate ? endDate.valueOf() : toTime;

    return ViewModeREST.getAllExecutions(this.topologyId, {
      from: this.fromTimeInMS,
      to: this.toTimeInMS,
      pageSize: executionInfoPageSize,
      page: executionInfoPage,
      namespaceId: this.selectedDataCenterId
    }).then((res) => {
      this.state.executionInfo = res;
      let executions = Utils.sortArray(res.executions, "executionDate", true);
      this.state.executionInfo.executions = executions;

      const latestExecution = executions.length > 0 ? executions[executions.length - 1] : null;

      viewModeData.topologyMetrics = {
        overviewMetrics: {
          metrics: latestExecution
        }
      };

      this.onSelectExecution(latestExecution, viewModeData);

      return res;
    });
  }

  fetchTimeSeriesMetrics = (fromTime, toTime) => {
    this.timeseriesData = this.timeseriesData || [];

    let {viewModeData, startDate, endDate, topologyNamespaces} = this.state;

    const promiseArr = [];
    const bundle = _.find(app_state.engineTemplateMetricsBundles, (bundle) => {
      return this.engine.name == bundle.engine && this.template.name == bundle.template;
    });
    const timeSeriesMetrics = bundle.metricsUISpec.timeseries;
    _.each(timeSeriesMetrics, (m) => {
      _.each(m.metricKeyName, (mKey) => {
        let name = m.name;
        let metricQuery = m.metricQuery;
        let interpolate = m.interpolate;

        const queryParams = {
          from: startDate ? startDate.valueOf() : fromTime,
          to: endDate ? endDate.valueOf() : toTime,
          metricQuery: metricQuery,
          namespaceId: this.selectedDataCenterId
        };

        const onSuccess = (res, name, interpolate) => {
          _.each(res, (timeseriesData, compId) => {
            let compData = _.find(this.timeseriesData, (d, id) => {
              return compId == d.component.id;
            });
            if(!compData){
              compData = {
                component: {
                  id:compId
                },
                timeSeriesMetrics: {
                  metrics: {
                    [name]: timeseriesData
                  }
                },
                interpolate: interpolate
              };
              this.timeseriesData.push(compData);
            }else{
              compData.timeSeriesMetrics.metrics[name] = timeseriesData;
            }
          });
        };

        const req = ViewModeREST.getTimeseries(this.topologyId, this.engine.type.toLowerCase(), name, queryParams).then((res) => {
          onSuccess(res, name, interpolate);
        }, (err) => {
          console.error(err);
        });
        promiseArr.push(req);
      });
    });
    return promiseArr;
  }

  fetchCatalogInfoAndMetrics(fromTime, toTime, startDate) {
    let {viewModeData, executionInfoPageSize, executionInfoPage} = this.state;

    let promiseArr = [];

    if(this.engine.type == 'batch'){
      const req = this.fetchExecutions(startDate ? startDate : fromTime, toTime);
      promiseArr.push(req);
      const timeseriesMetricReqs = this.fetchTimeSeriesMetrics(fromTime, toTime);
      promiseArr.push.apply(promiseArr, [...timeseriesMetricReqs]);
    }else if(this.engine.type == 'stream'){
      const timeseriesMetricReqs = this.fetchTimeSeriesMetrics(fromTime, toTime);
      promiseArr.push.apply(promiseArr, [...timeseriesMetricReqs]);
    }else {
      let q_params = {
        from: fromTime,
        to: toTime,
        namespaceId: this.selectedDataCenterId
      };
      promiseArr.push(ViewModeREST.getTopologyMetrics(this.topologyId, q_params).then((res)=>{
        viewModeData.topologyMetrics = res;
        return res;
      }, (err) => {}));
      _.each(this.engine.componentTypes, (type) => {
        const typeInLCase = type.toLowerCase();
        promiseArr.push(ViewModeREST.getComponentMetrics(this.topologyId, typeInLCase+'s', q_params).then((res) => {
          viewModeData[typeInLCase+'Metrics'] = res.entities;
          return res;
        }));
      });
    }
    this.fetchMetrics = true;
    Promise.all(promiseArr).then((responseArr)=>{
      this.fetchMetrics = false;
      this.syncComponentData(viewModeData);
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
      this.setState({viewModeData : tempViewModeData}, () => {this.syncComponentData(tempViewModeData);});
    });
  }

  syncComponentData(viewModeData, compStatusObj) {
    let {selectedComponentId, selectedComponent} = viewModeData;
    let overviewMetrics, timeSeriesMetrics;

    if(this.fetchMetrics) {
      if(compStatusObj){
        this.setState(compStatusObj);
      }
      return;
    }
    if(selectedComponent) {
      const typeInLCase = selectedComponent.parentType.toLowerCase();

      let compObj = viewModeData[typeInLCase+'Metrics'].find((entity)=>{
        return entity.component.id === selectedComponentId;
      });

      if(compObj){
        overviewMetrics = compObj.overviewMetrics;
        timeSeriesMetrics = compObj.timeSeriesMetrics;
      }

    } else {
      overviewMetrics = viewModeData.topologyMetrics.overviewMetrics;
      timeSeriesMetrics = viewModeData.topologyMetrics.timeSeriesMetrics;
    }
    viewModeData.overviewMetrics = overviewMetrics || {};
    viewModeData.timeSeriesMetrics = timeSeriesMetrics || {};
    let stateObj = {viewModeData: viewModeData};
    if(compStatusObj){
      Object.assign(stateObj, compStatusObj);
    }
    this.setState(stateObj,()=>{
      this.triggerUpdateGraph();
    });
  }
  compSelectCallback = (id, obj) => {
    let {viewModeData} = this.state;
    viewModeData.selectedComponentId = id;
    viewModeData.selectedComponent = obj;
    this.syncComponentData(viewModeData);
  }
  handleVersionChange(value) {
    this.fetchData(value);
  }
  datePickerCallback = (startDate, endDate, activeRangeLabel) => {
    if(this.refs.metricsPanelRef){
      this.refs.metricsPanelRef.setState({loadingRecord: true});
    }
    this.setState({
      startDate: startDate,
      endDate: endDate,
      activeRangeLabel: activeRangeLabel ? activeRangeLabel : null
    }, ()=>{
      this.fetchCatalogInfoAndMetrics(startDate.toDate().getTime(), endDate.toDate().getTime());
    });
  }
  handleClickCallback = (begining, ending) =>{
    this.fetchTimeSeriesMetrics(begining,ending);
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
          {Utils.isFromSharedProjects() ?
            <Link to="/shared-projects">Shared Projects</Link>
            :
            <Link to="/">My Projects</Link>
          }
          <i className="fa fa-angle-right title-separator"></i>
          <Link to={(Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+projectData.id+"/applications"}>{projectData.name}</Link>
          <i className="fa fa-angle-right title-separator"></i>
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
    if(this.refs.EditorGraph && this.refs.EditorGraph.child && this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph){
      this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.updateGraph();
    }
  }

  handleDataCenterChange = (value) => {
    let {startDate, endDate, topologyNamespaces} = this.state;
    this.selectedDataCenter = value;
    this.selectedDataCenterId = topologyNamespaces[value].namespaceId;
    this.fetchCatalogInfoAndMetrics(startDate.toDate().getTime(), endDate.toDate().getTime());
  }

  getRightSideBar = () => {
    const {topologyName, executionInfo, selectedExecution, topologyNamespaces} = this.state;
    let namespacesArr = [];
    _.keys(topologyNamespaces).map((name)=>{
      namespacesArr.push({
        id: topologyNamespaces[name].namespaceId,
        name: name
      });
    });
    return <Metrics
        {...this.state}
        executionInfo={executionInfo}
        lastUpdatedTime={this.lastUpdatedTime}
        topologyName={topologyName}
        onSelectExecution={this.onSelectExecution}
        getPrevPageExecutions={this.getPrevPageExecutions}
        getNextPageExecutions={this.getNextPageExecutions}
        compSelectCallback={this.compSelectCallback}
        components={this.graphData.nodes}
        datePickerCallback={this.datePickerCallback}
        handleClickCallback={this.handleClickCallback}
        timeseriesData={this.timeseriesData}
        handleDataCenterChange={this.handleDataCenterChange}
        selectedDataCenter={this.selectedDataCenter}
        dataCenterList={namespacesArr}
        isBatchEngine={this.engine.type.toLowerCase() == 'batch'}
        engine={this.engine}
        template={this.template}
        runtimeAppUrl={this.runtimeAppUrl}
        start_time={new Date(this.statusObj.extra.startExecutionDate).getTime()}
        end_time={new Date(this.statusObj.extra.latestExecutionDate).getTime()}
        time_interval = {this.statusObj.extra.executionInterval || "5"}
        time_unit= {this.statusObj.extra.executionIntervalUnit || "Minute"}
        topologyId={this.topologyId}
        namespaceId={this.selectedDataCenterId}
        fromTimeInMS={this.fromTimeInMS}
        toTimeInMS={this.toTimeInMS}
    />;
  }

  render() {
    const {fetchLoader,allACL, viewModeData, startDate, endDate, isAppRunning} = this.state;
    let nodeType = this.node
      ? this.node.currentType
      : '';
      // this.viewMode && (nodeType.toLowerCase() === 'join' || nodeType.toLowerCase() === 'window') ? "modal-xl" : "modal-fixed-height modal-xl"
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
      <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false"
        headerContent={this.getTopologyHeader()} siblingContent={isAppRunning ? this.getRightSideBar() : null}
      >
        <div className="topology-view-mode-container">
          {fetchLoader
            ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"viewMode"}/>]
            : <div>
              {
                this.checkAuth
                ? [<ZoomPanelComponent
                    key="zoomPanel"
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
                    isAppRunning={isAppRunning}
                    engineType={this.engine.type}
                    runtimeAppId={this.runtimeAppId}
                    runtimeAppUrl={this.runtimeAppUrl}
                    topologyStatus={this.state.topologyStatus}
                    deployedVersion={this.deployedVersion}
                  />,
                  <TopologyViewMode
                    allACL={allACL} key={"1"} {...this.state}
                    runtimeAppId={this.runtimeAppId}
                    projectId={this.projectData.id}
                    topologyId={this.topologyId}
                    killTopology={this.killTopology.bind(this)}
                    handleVersionChange={this.handleVersionChange.bind(this)}
                    setCurrentVersion={this.setCurrentVersion.bind(this)}
                    datePickerCallback={this.datePickerCallback}
                    modeSelectCallback={this.modeSelectCallback}
                    stormClusterId={this.state.stormClusterId}
                    namespaceId={this.selectedDataCenterId}
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
        <Modal className="u-form" ref="NodeModal"
          dialogClassName={nodeClassName}
          data-title={this.modalTitle}
          data-resolve={this.handleSaveNodeModal.bind(this)}
        >
          {this.modalContent()}
        </Modal>
        {/*this.state.isAppRunning && this.graphData.nodes.length > 0 && this.versionName.toLowerCase() == 'current' && this.engine.type == 'stream' ?
        <TopologyViewModeMetrics
          ref="metricsPanelRef"
          {...this.state}
          topologyId={this.topologyId}
          topologyName={this.state.topologyName}
          components={this.graphData.nodes}
          compSelectCallback={this.compSelectCallback}
          datePickerCallback={this.datePickerCallback}
          engine={this.engine}
        />
        : null*/}
      </BaseContainer>
    );
  }
}

TopologyViewContainer.contextTypes = {
  router: PropTypes.object.isRequired
};

export default withRouter(TopologyViewContainer);
