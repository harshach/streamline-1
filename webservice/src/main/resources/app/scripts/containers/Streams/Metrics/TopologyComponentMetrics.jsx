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

import React,{Component} from 'react';
import _ from 'lodash';
import TimeSeriesChart from '../../../components/TimeSeriesChart';
import d3 from 'd3';
import Utils from '../../../utils/Utils';
import ContentScrollableComponent from '../../../components/ContentScrollableComponent';

class TopologyComponentMetrics extends Component {

  constructor(props){
    super(props);
    this.state = {
      activeIndex : 0,
      graphHeight: 50,
      loadingRecord: false
    };
  }

  getGraph(name, data, interpolation, renderGraph) {
    const self = this;
    return renderGraph ? <TimeSeriesChart color={d3.scale.category20c().range(['#44abc0', '#8b4ea6'])} ref={name} data={data} interpolation={interpolation} height={this.state.graphHeight} setXDomain={function() {
      this.x.domain([self.props.startDate.toDate(), self.props.endDate.toDate()]);
    }} setYDomain={function() {
      const min = d3.min(this.mapedData, (c) => {
        return d3.min(c.values, (v) => {
          return v.value;
        });
      });
      this.y.domain([
        min > 0
          ? 0
          : min,
        d3.max(this.mapedData, (c) => {
          return d3.max(c.values, (v) => {
            return v.value;
          });
        })
      ]);
    }} getXAxis={function(){
      return d3.svg.axis().orient("bottom").tickFormat("");
    }} getYAxis={function() {
      return d3.svg.axis().orient("left").tickFormat("");
    }} drawBrush={function(){
    }} showTooltip={function(d) {
    }} hideTooltip={function() {
    }} /> : null ;
  }

  getMetrics(overviewMetrics){
    const {
      viewModeData,
      compData,
      engine
    } = this.props;

    const componentType = compData.parentType;
    const componentName = null;
    let template = Utils.getViewModeDAGMetricsTemplate(engine, componentType, compData.currentType);

    const metrics = [];
    if(!_.isUndefined(overviewMetrics.metrics)){
      _.each(template, (t) => {
        const oValue = _.get(overviewMetrics.metrics, t.metricKeyName);
        const value = Utils[t.valueFormat](oValue);

        const colTimeWidth =  60, colStaticWidth = 40;

        const uiNameArr = t.uiName.split(' ');

        const component = <div className="component-metric-widget" style={{margin : "0 10px 10px 0"}}>
            <h6>{uiNameArr[0]}</h6>
            {uiNameArr[1] ? <h6>{uiNameArr[1]}</h6> : <h6>&nbsp;</h6>}
            <h4>{value.value}
            <small>{value.suffix}</small></h4>
          </div>;
        metrics.push(component);
      });
    }

    return metrics;
  }

  getTimeseriesMetrics(timeSeriesMetrics){
    const {
      viewModeData,
      compData,
      engine
    } = this.props;;
    const showMetrics = viewModeData.selectedMode == 'Metrics' ? true : false;

    const componentType = compData.parentType;
    const componentName = null;
    let template = Utils.getViewModeDAGTimeseriesMetricsTemplate(engine, componentType, compData.currentType);

    const metrics = [];
    if(!_.isUndefined(timeSeriesMetrics.metrics)){

      const loader = <img src="styles/img/start-loader.gif" alt="loading" style={{
        width: "80px",
        marginTop: "0px"
      }}/>;

      _.each(template, (t) => {
        const graphData = [];
        const firstLineData = timeSeriesMetrics.metrics[t.metricKeyName[0]];
        for(const key in firstLineData) {
          const obj = {
            date: new Date(parseInt(key))
          };
          _.each(t.metricKeyName, (kName) => {
            obj[kName] = timeSeriesMetrics[kName][key] || 0;
          });
          graphData.push(obj);
        }

        const component = <div className="component-metric-graph">
          <div style={{textAlign: "left"}}>{t.uiName}</div>
          <div style={{
            height: '25px',
            textAlign: 'center',
            backgroundColor: '#f2f3f2'
          }}>
            {this.state.loadingRecord ? loader : this.getGraph(t.uiName, graphData, t.interpolate, showMetrics)}
          </div>
        </div>;

        metrics.push(component);
      });
    }

    return metrics;
  }

  render () {
    let {viewModeData, compData, engine} = this.props;
    const {componentLevelActionDetails} = viewModeData;
    let overviewMetrics = {}, timeSeriesMetrics = {},samplingVal= 0,
      logLevels = viewModeData.logTopologyLevel;

    const typeInLCase = compData.parentType.toLowerCase();
    let compObj = viewModeData[typeInLCase+'Metrics'].find((entity)=>{
      return entity.component.id === compData.nodeId;
    });;


    if(!_.isUndefined(compObj)){
      overviewMetrics = compObj.overviewMetrics;
      timeSeriesMetrics = compObj.timeSeriesMetrics;
    }

    const showMetrics = viewModeData.selectedMode == 'Metrics' ? true : false;

    if(!_.isEmpty(componentLevelActionDetails)){
      const sampleObj =  _.find(componentLevelActionDetails.samplings, (sample) => sample.componentId === compData.nodeId);
      samplingVal = sampleObj !== undefined && sampleObj.enabled ? sampleObj.duration : 0;
    }
    const colTimeWidth =  60, colStaticWidth = 40;
    // increase the parent with multiple the number of columns..
    const parentWidth = compData.parentType == 'SOURCE'
                        ? ((colTimeWidth * 1) + (colStaticWidth*3)) + (4*7.5)
                        : ((colTimeWidth * 2) + (colStaticWidth*3)) + (5*7.5);
    return (
      <div className="overviewDiv" style={{width : parentWidth+'px'}}>
      <div className="metric-bg top"></div>
      <div className="component-metric-top">
        {overviewMetrics && this.getMetrics(overviewMetrics)}
      </div>
      {showMetrics ?
      (
      <ContentScrollableComponent>
        <div className="metric-graphs-container">
          {timeSeriesMetrics && this.getTimeseriesMetrics(timeSeriesMetrics)}
        </div>
      </ContentScrollableComponent>
      )
      : ''
      }
      {engine.type == 'stream' &&
      <div className="metric-bg bottom">
        <span className="pull-left">Log: <span style={{color: '#2787ad'}}>{logLevels}</span></span>
        <span className="pull-right">Sampling: <span style={{color: '#2787ad'}}>{samplingVal}%</span></span>
      </div>
      }
      </div>
    );
  }

}

export default TopologyComponentMetrics;
