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

import React, { Component } from 'react';
import PropTypes from 'prop-types';
import {observer} from 'mobx-react';
import {observable} from 'mobx';
import {DragDropContext, DropTarget} from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import {ItemTypes, Components, iconsFrom} from '../utils/Constants';
import ComponentNodeContainer, {
  PiperComponentNodeContainer,
  AthenaXComponentNodeContainer,
  StormComponentNodeContainer} from '../containers/Streams/TopologyEditor/ComponentNodeContainer';
import TopologyGraphComponent from './TopologyGraphComponent';
import SpotlightSearch from './SpotlightSearch';
import state from '../../scripts/app_state';
import Utils from '../../scripts/utils/Utils';
import FSReactToastr from './FSReactToastr';
import CommonNotification from '../utils/CommonNotification';
import {toastOpt} from '../utils/Constants';

import TopologyREST from '../rest/TopologyREST';
import TopologyUtils from '../utils/TopologyUtils';

const componentTarget = {
  drop(props, monitor, component) {
    const item = monitor.getItem();
    const delta = monitor.getDifferenceFromInitialOffset();
    const left = Math.round(item.left + delta.x);
    const top = Math.round(item.top + delta.y);

    component.moveBox(left, top);
  }
};

function collect(connect, monitor) {
  return {connectDropTarget: connect.dropTarget()};
};

class EditorGraph extends Component {
  static propTypes = {
    connectDropTarget: PropTypes.func.isRequired
  };
  componentWillReceiveProps(newProps) {}
  constructor(props) {
    super(props);
    let left = window.innerWidth - 300;
    this.state = {
      boxes: {
        top: 50,
        left: left
      },
      bundleArr: null,
      loading: true,
      mapSlideInterval: []
    };
  }
  componentWillMount(){
    this.setState({
      loading: true
    }, () => {
      this.fetchBundles();
    });
  }
  moveBox(left, top) {
    this.setState(update(this.state, {
      boxes: {
        $merge: {
          left: left,
          top: top
        }
      }
    }));
  }

  /*
    addComponent callback method accepts the component details from SpotlightSearch and
    gets node name in case of custom processor
    invokes method to add component in TopologyGraphComponent
  */
  addComponent(item) {
    let obj = {
      type: item.type,
      imgPath: 'styles/img/'+iconsFrom+'icon-' + item.subType.toLowerCase() + '.png',
      name: item.subType,
      nodeLabel: item.subType,
      nodeType: item.subType,
      topologyComponentBundleId: item.id
    };
    if(item.subType === 'CUSTOM') {
      let config = item.topologyComponentUISpecification.fields,
        name = _.find(config, {fieldName: "name"});
      obj.name = name ? name.defaultValue : 'Custom';
      obj.nodeLabel = name ? name.defaultValue : 'Custom';
      obj.nodeType = 'Custom';
    }
    this.refs.TopologyGraph.decoratedComponentInstance.addComponentToGraph(obj);
  }

  render() {
    const actualHeight = (window.innerHeight - (this.props.viewMode
      ? 155
      : 155)) + 'px';
    const {
      versionsArr,
      connectDropTarget,
      viewMode,
      topologyId,
      versionId,
      graphData,
      getModalScope,
      setModalContent,
      getEdgeConfigModal,
      setLastChange,
      topologyConfigMessageCB,
      testRunActivated,
      testItemSelected,
      testCaseList,
      selectedTestObj,
      addTestCase,
      eventLogData,
      hideEventLog,
      testRunningMode,
      isAppRunning,
      viewModeData,
      startDate,
      endDate,
      compSelectCallback,
      componentLevelAction,
      contextRouter,
      engine,
      topologyData,
      selectedExecutionComponentsStatus,
      versionName
    } = this.props;
    const {boxes, bundleArr, loading} = this.state;

    if(loading){
      return <div>Loading...</div>;
    }else{
      const componentsBundle = this.componentsBundle;
      const ComponentNode = this.ComponentNodeContainer;

      return connectDropTarget(
        <div>
          <div className="" style={{
            height: actualHeight
          }}>
            <TopologyGraphComponent ref="TopologyGraph"
              height={parseInt(actualHeight, 10)}
              data={graphData}
              topologyId={topologyId}
              versionId={versionId}
              versionsArr={versionsArr}
              viewMode={viewMode}
              getModalScope={getModalScope}
              setModalContent={setModalContent}
              getEdgeConfigModal={getEdgeConfigModal}
              setLastChange={setLastChange}
              topologyConfigMessageCB={topologyConfigMessageCB}
              testRunActivated={testRunActivated}
              eventLogData={eventLogData}
              hideEventLog={hideEventLog}
              viewModeData={viewModeData}
              startDate={startDate}
              endDate={endDate}
              compSelectCallback={compSelectCallback}
              isAppRunning={isAppRunning}
              componentLevelAction={componentLevelAction}
              viewModeContextRouter={contextRouter}
              componentsBundle={componentsBundle}
              engine={engine}
              topologyData={topologyData}
              selectedExecutionComponentsStatus={selectedExecutionComponentsStatus || []}
            />
            {state.showComponentNodeContainer && !viewMode && versionName == 'CURRENT'
              ? <ComponentNode
                testRunningMode={testRunningMode}
                left={boxes.left}
                top={boxes.top}
                hideSourceOnDrag={true}
                viewMode={viewMode}
                customProcessors={this.props.customProcessors}
                bundleArr={bundleArr}
                testRunActivated={testRunActivated}
                testItemSelected={testItemSelected}
                testCaseList={testCaseList}
                selectedTestObj={selectedTestObj}
                addTestCase={addTestCase}
                eventLogData={eventLogData}
                engine={engine}
                topologyData={topologyData} />
              : null
            }
            {state.showSpotlightSearch && !viewMode ? <SpotlightSearch viewMode={viewMode} componentsList={Utils.sortArray(componentsBundle, 'name', true)} addComponentCallback={this.addComponent.bind(this)}/> : ''}
          </div>
        </div>
      );
    }

  }
}

@DragDropContext(HTML5Backend)
@DropTarget(ItemTypes.ComponentNodes, componentTarget, collect)
@observer
export class StreamEditorGraph extends EditorGraph{
  get ComponentNodeContainer(){
    return StormComponentNodeContainer;
  }
  fetchBundles(){
    let promiseArr = [];

    const {graphData} = this.props;

    graphData.metaInfo.sources = graphData.metaInfo.sources || [];
    graphData.metaInfo.processors = graphData.metaInfo.processors || [];
    graphData.metaInfo.sinks = graphData.metaInfo.sinks || [];

    const {engineId, templateId} = this.props.topologyData;
    promiseArr.push(TopologyREST.getSourceComponent(engineId, templateId));
    promiseArr.push(TopologyREST.getProcessorComponent(engineId, templateId));
    promiseArr.push(TopologyREST.getSinkComponent(engineId, templateId));
    promiseArr.push(TopologyREST.getLinkComponent());
    promiseArr.push(TopologyREST.getAllNodes(this.props.topologyId, this.props.versionId, 'sources'));
    promiseArr.push(TopologyREST.getAllNodes(this.props.topologyId, this.props.versionId, 'processors'));
    promiseArr.push(TopologyREST.getAllNodes(this.props.topologyId, this.props.versionId, 'sinks'));
    promiseArr.push(TopologyREST.getAllNodes(this.props.topologyId, this.props.versionId, 'edges'));

    Promise.all(promiseArr).then((resultsArr) => {

      this.sourceConfigArr = resultsArr[0].entities;
      this.processorConfigArr = resultsArr[1].entities;
      this.sinkConfigArr = resultsArr[2].entities;
      this.linkConfigArr = resultsArr[3].entities;

      graphData.linkShuffleOptions = TopologyUtils.setShuffleOptions(this.linkConfigArr);

      let sourcesNode = resultsArr[4].entities || [];
      let processorsNode = resultsArr[5].entities || [];
      let sinksNode = resultsArr[6].entities || [];
      let edgesArr = resultsArr[7].entities || [];

      graphData.nodes = TopologyUtils.syncNodeData(sourcesNode, processorsNode, sinksNode, graphData.metaInfo, this.sourceConfigArr, this.processorConfigArr, this.sinkConfigArr, this.notifyReconfigureCallback);

      graphData.uinamesList = [];
      graphData.nodes.map(node => {
        graphData.uinamesList.push(node.uiname);
      });

      graphData.edges = TopologyUtils.syncEdgeData(edgesArr, graphData.nodes);

      this.customProcessors = this.getCustomProcessors();
      this.processorSlideInterval(processorsNode); // NTUC

      this.setState({
        bundleArr: {
          sourceBundle: this.sourceConfigArr,
          processorsBundle: this.processorConfigArr,
          sinksBundle: this.sinkConfigArr
        },
        loading: false
      });
    });
  }
  processorSlideInterval(processors) {
    const {topologyTimeSec, topologyData, topologyVersion} = this.props;
    let tempIntervalArr = [];
    const pString = "JOIN,AGGREGATE";
    const pIndex = _.findIndex(processors, function(processor) {
      const name = processor.name !== undefined
        ? processor.name.split('-')
        : '';
      return pString.indexOf(name[0]) !== -1;
    });
    if (pIndex === -1) {
      this.tempIntervalArr = [];
      const {topologyConfig} = this.props;
      topologyConfig["topology.message.timeout.secs"] = topologyTimeSec;
      this.setState({
        mapTopologyConfig: topologyConfig,
        mapSlideInterval: tempIntervalArr
      }, () => {
        return;
      });
    } else {
      processors.map((processor) => {
        if (processor.name !== undefined) {
          if (processor.name.indexOf("JOIN") !== -1 && processor.config.properties.window !== undefined) {
            this.mapSlideInterval(processor.id, processor.config.properties.window);
            this.props.setTopologyConfig(topologyData.name, topologyVersion);
          } else {
            if (processor.name.indexOf("AGGREGATE") !== -1 && processor.config.properties.rules !== undefined) {
              this.fetchWindowSlideInterval(processor).then((result) => {
                this.props.setTopologyConfig(topologyData.name, topologyVersion);
              });
            }
          }
        }
      });
    }
  }
  mapSlideInterval(id, timeObj) {
    const {defaultTimeSec} = this.state;
    this.tempIntervalArr = this.state.mapSlideInterval;
    let timeoutSec = this.props.topologyConfig["topology.message.timeout.secs"];
    let slideIntVal = 0,
      totalVal = 0;
    _.keys(timeObj).map((x) => {
      _.keys(timeObj[x]).map((k) => {
        if (k === "durationMs") {
          // the server give value only in millseconds
          totalVal += timeObj[x][k];
        }
      });
    });
    slideIntVal = Utils.convertMillsecondsToSecond(totalVal);
    const index = this.tempIntervalArr.findIndex((x) => {
      return x.id === id;
    });
    if (index === -1) {
      this.tempIntervalArr.push({
        id: id,
        value: slideIntVal + 5
      });
    } else {
      timeoutSec = defaultTimeSec;
      this.tempIntervalArr[index].value = slideIntVal + 5;
    }
    const sum = _.sumBy(this.tempIntervalArr, "value");
    const sumVal = sum + 2; // 2 is for delta
    this.props.topologyConfig["topology.message.timeout.secs"] = timeoutSec >= sumVal
      ? timeoutSec
      : sumVal;
  }
  fetchWindowSlideInterval(obj) {
    if (_.keys(obj.config.properties).length > 0) {
      const ruleId = obj.config.properties.rules[0];
      const id = obj.id;
      return TopologyREST.getNode(obj.topologyId, obj.versionId, 'windows', ruleId).then((node) => {
        if (node.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={node.responseMessage}/>, '', toastOpt);
        } else {
          this.mapSlideInterval(id, node.window);
        }
      });
    }
  }
  getCustomProcessors() {
    return this.processorConfigArr.filter((o) => {
      return o.subType === 'CUSTOM';
    });
  }
  get componentsBundle(){
    const {bundleArr} = this.state;
    return [...bundleArr.sourceBundle, ...bundleArr.processorsBundle, ...bundleArr.sinksBundle];
  }
  getModalScope(node) {
    let obj = {
        testRunActivated : this.state.testRunActivated,
        editMode: !this.props.viewMode,
        topologyId: this.props.topologyId,
        versionId: this.props.versionId,
        namespaceId: this.props.namespaceId
      },
      config = [];
    switch (node.parentType) {
    case 'SOURCE':
      config = this.sourceConfigArr.filter((o) => {
        return o.subType === node.currentType.toUpperCase();
      });
      if (config.length > 0) {
        config = config[0];
      }
      obj.configData = config;
      break;
    case 'PROCESSOR':
      config = this.processorConfigArr.filter((o) => {
        return o.subType.toUpperCase() === node.currentType.toUpperCase();
      });
      //Check for custom processor
      if (node.currentType.toLowerCase() === 'custom') {
        let index = null;
        let customNames = this.graphData.metaInfo.customNames;
        let customNameObj = _.find(customNames, {uiname: node.uiname});
        config.map((c, i) => {
          let configArr = c.topologyComponentUISpecification.fields;
          configArr.map(o => {
            if (o.fieldName === 'name' && o.defaultValue === customNameObj.customProcessorName) {
              index = i;
            }
          });
        });
        if (index !== null) {
          config = config[index];
        } else {
          console.error("Not able to get Custom Processor Configurations");
        }
      } else {
        //For all the other processors except CP
        if (config.length > 0) {
          config = config[0];
        }
      }
      obj.configData = config;
      break;
    case 'SINK':
      config = this.sinkConfigArr.filter((o) => {
        return o.subType === node.currentType.toUpperCase();
      });
      if (config.length > 0) {
        config = config[0];
      }
      obj.configData = config;
      break;
    }
    return obj;
  }
}

@DragDropContext(HTML5Backend)
@DropTarget(ItemTypes.ComponentNodes, componentTarget, collect)
@observer
export class BatchEditorGraph extends EditorGraph{
  get ComponentNodeContainer(){
    return PiperComponentNodeContainer;
  }

  fetchBundles(){
    let promiseArr = [];

    const {graphData} = this.props;

    graphData.metaInfo.tasks = graphData.metaInfo.tasks || [];

    const {engineId, templateId} = this.props.topologyData;
    promiseArr.push(TopologyREST.getTaskComponent(engineId, templateId));
    promiseArr.push(TopologyREST.getLinkComponent());
    promiseArr.push(TopologyREST.getAllNodes(this.props.topologyId, this.props.versionId, 'tasks'));
    promiseArr.push(TopologyREST.getAllNodes(this.props.topologyId, this.props.versionId, 'edges'));

    Promise.all(promiseArr).then((resultsArr) => {

      this.tasksConfigArr = resultsArr[0].entities;
      this.linkConfigArr = resultsArr[1].entities;

      graphData.linkShuffleOptions = TopologyUtils.setShuffleOptions(this.linkConfigArr);

      let tasksNode = resultsArr[2].entities || [];
      let edgesArr = resultsArr[3].entities || [];

      graphData.nodes = [];
      TopologyUtils.generateNodeData(tasksNode, this.tasksConfigArr, graphData.metaInfo.tasks, graphData.nodes, {reconfigure: false});

      graphData.uinamesList = [];
      graphData.nodes.map(node => {
        graphData.uinamesList.push(node.uiname);
      });

      graphData.edges = TopologyUtils.syncEdgeData(edgesArr, graphData.nodes);

      this.setState({
        bundleArr: {
          tasks: this.tasksConfigArr
        },
        loading: false
      });
    });
  }
  get componentsBundle(){
    const {bundleArr} = this.state;
    return [...bundleArr.tasks];
  }
  getModalScope(node){
    let config = this.tasksConfigArr.filter((o) => {
      return o.id === node.topologyComponentBundleId;
    });
    if (config.length > 0) {
      config = config[0];
    }
    return {
      testRunActivated : this.state.testRunActivated,
      editMode: !this.props.viewMode,
      topologyId: this.props.topologyId,
      versionId: this.props.versionId,
      namespaceId: this.props.namespaceId,
      configData: config
    };
  }
}

export default EditorGraph;
