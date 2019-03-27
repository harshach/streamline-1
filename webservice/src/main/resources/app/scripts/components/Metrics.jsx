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
import ReactDOM from 'react-dom';
import {observer} from 'mobx-react';
import {Button, PanelGroup, Panel, DropdownButton, MenuItem,
  Tabs, Tab} from 'react-bootstrap';
import {EditorFooter} from '../containers/Streams/TopologyEditor/TopologyViewModeMetrics';
import Slider from "react-slick";
import moment from 'moment';
import d3 from 'd3';
import RightSideBar from './RightSideBar';
import DateTimePickerDropdown from './DateTimePickerDropdown';
import TimeSeriesChart from './TimeSeriesChart';
import app_state from '../app_state';
import Utils from '../utils/Utils';
import CommonLoaderSign from './CommonLoaderSign';
import _ from 'lodash';

export default class Metrics extends Component{
  constructor(props) {
    super(props);
    let templates = Utils.getViewModeTimeseriesMetricsTemplate(props.engine, 'topology');
    this.state = {
      timeseriesTemplate: templates,
      selectedMetrics: templates[0] ? templates[0].uiName : '',
      selectedMetricsName: templates[0] ? templates[0].name : ''
    };
  }
  componentDidMount(){
    app_state.versionPanelCollapsed = false;
  }
  handleExpandCollapse = () => {
    app_state.versionPanelCollapsed = !app_state.versionPanelCollapsed;
  }
  getTimeSeriesData = (componentId, keyName) => {
    const {timeseriesData, components} = this.props;
    let finalObj = {
      graphData: [], interpolate: ''
    };
    const graphData = [];
    const componentNames = [];
    let selectedComponentObj = null;
    if(componentId){
      selectedComponentObj = components.find((c)=>{return c.nodeId == componentId;});
    }
    if(timeseriesData){
      timeseriesData.map((data)=>{
        let componentObj = components.find((c)=>{return c.nodeId == data.component.id;});
        if(componentObj){
          let componentName = componentObj.uiname;
          let firstLineData = data.timeSeriesMetrics.metrics[keyName];
          if(firstLineData){
            componentNames.push(componentName);
          }
          for(const key in firstLineData) {
            let date = new Date(parseInt(key));
            // check if array already has data for that particular timestamp
            // and if so, update that particular object
            let existingObj = graphData.find((d)=>{
              return d.date.getTime() == date.getTime();
            });
            if(existingObj){
              existingObj[componentName] = firstLineData[key];
            } else {
              const obj = {
                date: date
              };
              obj[componentName] = firstLineData[key];
              graphData.push(obj);
            }
          }
        }
        finalObj.interpolate = data.interpolate;
      });
    }
    finalObj.componentNames = (selectedComponentObj ? [selectedComponentObj.uiname] : componentNames);
    this.syncGraphData(graphData, componentNames, finalObj);
    return finalObj;
  }
  syncGraphData = (graphData, componentNames, finalObj) => {
    // sort data by data
    graphData.sort((a,b)=>{
      return a.date - b.date;
    });
    // check if any component is missing data for that particular date
    // if so, then add data with previous date's value
    for(let i = 0; i < graphData.length; i++){
      let dataObj = graphData[i];
      let obj = {
        date: dataObj.date
      };
      componentNames.map((name)=>{
        if(!dataObj.hasOwnProperty(name)){
          obj[name] = (i == 0 ? 0 : finalObj.graphData[i-1][name]);
        } else {
          obj[name] = dataObj[name];
        }
      });
      finalObj.graphData.push(obj);
    }
  }
  getHeader = () => {
    return <button className="btn-panels" onClick={this.handleExpandCollapse}><img src="styles/img/uWorc/graph.png"/></button>;
  }
  getBody = () => {
    const {lastUpdatedTime, topologyName, executionInfo, selectedExecution,
      onSelectExecution, getPrevPageExecutions, getNextPageExecutions,
      startDate, endDate, activeRangeLabel, isAppRunning, datePickerCallback,
      viewModeData, timeseriesData, dataCenterList, selectedDataCenter,
      handleDataCenterChange, isBatchEngine} = this.props;

    const {timeseriesTemplate, selectedMetrics, selectedMetricsName} = this.state;

    const locale = {
      format: 'YYYY-MM-DD',
      separator: ' - ',
      applyLabel: 'OK',
      cancelLabel: 'Cancel',
      weekLabel: 'W',
      customRangeLabel: 'Custom Range',
      daysOfWeek: moment.weekdaysMin(),
      monthNames: moment.monthsShort(),
      firstDay: moment.localeData().firstDayOfWeek()
    };

    const selExeCreatedAt = isBatchEngine ? moment(selectedExecution.createdAt) : '';
    const selExeDate = isBatchEngine ? moment(selectedExecution.executionDate) : '';

    const content = (<div>
      <div className="right-sidebar-header">
        <div>
          <h6 className="version-control">Metrics</h6>
          <h6 className="version-control text-black">{topologyName}</h6>
        </div>
        <div className="text-right btn-group-metrics">
          <DateTimePickerDropdown
            dropdownId="datepicker-dropdown"
            startDate={startDate}
            endDate={endDate}
            activeRangeLabel={activeRangeLabel}
            locale={locale}
            isDisabled={!isAppRunning}
            datePickerCallback={datePickerCallback}
          />
          <DropdownButton className="btn-default" pullRight title={selectedDataCenter || ''} id="version-dropdown" onSelect={(n) => {
            handleDataCenterChange(n);
          }} >
            {_.map(dataCenterList, (n, i) => {
              return <MenuItem active={selectedDataCenter === n.name ? true : false} eventKey={n.name} key={i} data-version-id={n.id}>{n.name}</MenuItem>;
            })
          }
          </DropdownButton>
        </div>
        <div className="text-right">
          <button type="button" className="close" style={{marginLeft:'5px'}} onClick={this.handleExpandCollapse}><span >×</span></button>
        </div>
      </div>
      <div className="right-sidebar-body">
        {isBatchEngine && isAppRunning ?
          <div className="execution-metrics">
            <div className="text-center">
              <span className="execution-page-btn" onClick={getPrevPageExecutions}><i className="fa fa-angle-left"></i></span>
              <span className="execution-range">{startDate.format('LL') +' - '+endDate.format('LL')}</span>
              <span className="execution-page-btn" onClick={getNextPageExecutions}><i className="fa fa-angle-right"></i></span>
            </div>
            <div className="executions-box-container">
              {_.map(executionInfo.executions, (e, i) => {
                return <div key={"executions-"+i} className={`execution-box ${e.status} ${e.createdAt === selectedExecution.createdAt ? "execute-selected" : ""}`} onClick={onSelectExecution.bind(this, e)}></div>;
              })}
            </div>
            <div className="execution-metrics-container">
              <table>
                <tbody>
                  <tr>
                    <td><span className="execution-metric-label">Current Status</span></td>
                    <td><span className="execution-metric-value">{selectedExecution.status && selectedExecution.status.toUpperCase()}</span></td>
                  </tr>
                  <tr>
                    <td><span className="execution-metric-label">Created At</span></td>
                    <td><span className="execution-metric-value">{selExeCreatedAt.format('MM/DD/YYYY HH:mm:ss')}</span></td>
                  </tr>
                  <tr>
                    <td><span className="execution-metric-label">Execution Date</span></td>
                    <td><span className="execution-metric-value">{selExeDate.format('MM/DD/YYYY HH:mm:ss')}</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        : null}
        {timeseriesTemplate.length > 0 ?
          <div className="task-metrics">
            <div className="task-metrics-header">
              <div className="text-left">
                <span className="execution-metric-label display-block">Workflow Metrics</span>
              </div>
            </div>
            {_.chunk(timeseriesTemplate, 3).map((templatesArr, index)=>{
              return(<Tabs id={"timeseries-metrics-tabs-"+index} className="timeseries-metrics-tabs" mountOnEnter={true} key={index}>
                {templatesArr.map((template, i)=>{
                  return(<Tab eventKey={i+1} title={template.uiName} key={"tab-"+i}>
                    <div className="metrics-timeseries-graph" style={{height: '160px'}}>
                      {this.renderGraph(viewModeData.selectedComponentId, template.name)}
                    </div>
                  </Tab>);
                })}
              </Tabs>);
            })}
          </div>
          : <p>No timeseries template found</p>
        }
      </div>
    </div>);

    return content;
  }
  handleMetricsChange = (template) => {
    this.setState({
      selectedMetrics: template.uiName,
      selectedMetricsName: template.name
    });
  }
  renderGraph = (componentId, keyName) => {
    const data = this.getTimeSeriesData(componentId, keyName);
    if(data.graphData.length === 0){
      if(this.props.timeseriesData.length === 0){
        return (<CommonLoaderSign/>);
      } else {
        return "No Data Found";
      }
    }
    return (
      <TimeSeriesChart
        data={data.graphData}
        interpolation={data.interpolate}
        color={d3.scale.category10()}
        showLines={data.componentNames}
        getXAxis={function(){
          return d3.svg.axis().scale(this.x).orient("bottom");
        }}
        getYAxis={function(){
          return d3.svg.axis().scale(this.y).orient("left").tickSize(-this.width, 0, 0).tickFormat((y)=>{
            return Utils.numValueFormatter(y);
          });
        }}
        drawBrush={function(){}}
      />
    );
  }
  render(){
    return <RightSideBar
      {...this.props}
      getHeader={this.getHeader}
      getBody={this.getBody}
    />;
  }
}
