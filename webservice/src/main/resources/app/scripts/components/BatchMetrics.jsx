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

export default class BatchMetrics extends Component{
  constructor(props) {
    super(props);
    this.state = {
      expanded: false
    };
    this.selectedComponent = {};
  }
  componentDidUpdate(){
  }
  handleExpandCollapse = () => {
    const {expanded} = this.state;
    this.setState({expanded: !expanded});
  }
  getHeader = () => {
    return <button className="btn-panels" onClick={this.handleExpandCollapse}><img src="styles/img/uWorc/clock.png"/></button>;
  }
  getBody = () => {
    const {lastUpdatedTime, topologyName, executionInfo, selectedExecution,
      onSelectExecution, getPrevPageExecutions, getNextPageExecutions,
      startDate, endDate, activeRangeLabel, isAppRunning, datePickerCallback,
      viewModeData} = this.props;

    this.selectedComponent = viewModeData.selectedComponent || this.selectedComponent;

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

    const selExeCreatedAt = moment(selectedExecution.createdAt);
    const selExeDate = moment(selectedExecution.executionDate);

    const content = (<div>
      <div className="right-sidebar-header">
        <div>
          <h6 className="version-control">Metrics</h6>
          <h6 className="version-control">{topologyName}</h6>
        </div>
        <div className="text-right">
          <DateTimePickerDropdown
              dropdownId="datepicker-dropdown"
              startDate={startDate}
              endDate={endDate}
              activeRangeLabel={activeRangeLabel}
              locale={locale}
              isDisabled={!isAppRunning}
              datePickerCallback={datePickerCallback} />
          <DropdownButton bsStyle="link" className="btn-sm" pullRight title={'DC'} id="version-dropdown" onSelect={(v) => {
          }} >
            {_.map(['DC'], (v, i) => {
              return <MenuItem active={'DC' === v.name ? true : false} eventKey={v.id} key={i} data-version-id={v.id}>{v.name}</MenuItem>;
            })
          }
          </DropdownButton>
        </div>
        <div className="text-right">
          <button type="button" className="close" style={{marginLeft:'5px'}} onClick={this.handleExpandCollapse}><span >×</span></button>
        </div>
      </div>
      <div className="right-sidebar-body">
        <div className="text-center" style={{margin: '5px 0 15px 0'}}>
          <span className="execution-page-btn" onClick={getPrevPageExecutions}><i className="fa fa-angle-left"></i></span>
          <span className="execution-range">{startDate.format('LL') +' - '+endDate.format('LL')}</span>
          <span className="execution-page-btn" onClick={getNextPageExecutions}><i className="fa fa-angle-right"></i></span>
        </div>
        <div className="executions-box-container">
          {_.map(executionInfo.executions, (e) => {
            return <div className={`execution-box ${e.status}`} onClick={onSelectExecution.bind(e)}></div>;
          })}
        </div>
        <div className="execution-metrics-container">
          <table>
            <tr>
              <td><span className="execution-metric-label">Status</span></td>
              <td><span className="execution-metric-value">{selectedExecution.status}</span></td>
            </tr>
            <tr>
              <td><span className="execution-metric-label">Created At</span></td>
              <td><span className="execution-metric-value">{selExeCreatedAt.format('MM/DD/YYYY HH:mm:ss')}</span></td>
            </tr>
            <tr>
              <td><span className="execution-metric-label">Execution Date</span></td>
              <td><span className="execution-metric-value">{selExeDate.format('MM/DD/YYYY HH:mm:ss')}</span></td>
            </tr>
          </table>
        </div>
        <div className="task-metrics">
          <div className="task-metrics-header">
            <div className="text-left">
              <span className="execution-metric-label display-block">Task Metrics</span>
              <span className="execution-metric-value display-block">{this.selectedComponent.uiname}</span>
            </div>
            <div className="text-right">
              <button type="button" className="close" style={{marginLeft:'5px'}} onClick={this.handleExpandCollapse}><span >×</span></button>
            </div>
          </div>
          <Tabs id="timeseries-metrics-tabs" className="timeseries-metrics-tabs">
            <Tab eventKey={1} title="Task Duration">
              <div style={{height: '150px'}}>
                <TimeSeriesChart />
              </div>
            </Tab>
            <Tab eventKey={2} title="Avg. CPU Usage">
              <div style={{height: '150px'}}>
                <TimeSeriesChart />
              </div>
            </Tab>
            <Tab eventKey={3} title="Memory Usage">
              <div style={{height: '150px'}}>
                <TimeSeriesChart />
              </div>
            </Tab>
          </Tabs>
        </div>
      </div>
    </div>);

    return content;
  }
  render(){
    const {expanded} = this.state;
    return <RightSideBar
      {...this.props}
      getHeader={this.getHeader}
      getBody={this.getBody}
      expanded={expanded}
    />;
  }
}