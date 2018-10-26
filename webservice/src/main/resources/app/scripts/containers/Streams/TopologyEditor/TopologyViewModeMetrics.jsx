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
import {Button, PanelGroup, Panel, DropdownButton, MenuItem} from 'react-bootstrap';
import TimeSeriesChart from '../../../components/TimeSeriesChart';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import app_state from '../../../app_state';
import Utils from '../../../utils/Utils';
import d3 from 'd3';
import {Select2 as Select} from '../../../utils/SelectUtils';
import _ from 'lodash';
import moment from 'moment';
import Modal from '../../../components/FSModal';
import DateTimePickerDropdown from '../../../components/DateTimePickerDropdown';

@observer class TopologyViewModemetrics extends Component {
  constructor(props) {
    super();
    this.state = {
      showMetrics: props.viewModeData.selectedComponentId == '' ? false : true,
      selectedComponentId: '',
      inputOutputData: [],
      ackedData: [],
      failedData: [],
      queueData: [],
      latency: [],
      processTimeData: [],
      loadingRecord: true,
      graphHeight: 80,
      graphTitle: '',
      graphData: [],
      graphType: '',
      interpolation: ''
    };
  }
  componentWillReceiveProps(props) {
    if(props.viewModeData.selectedComponentId !== '') {
      this.setState({showMetrics: true});
    }
  }
  getGraph(name, data, interpolation, graphHeight) {
    const self = this;
    return <div style={graphHeight ? {height: graphHeight} : {height: this.state.graphHeight}}><TimeSeriesChart color={d3.scale.category20c().range(['#44abc0', '#8b4ea6'])} ref={name} data={data} interpolation={interpolation} height={graphHeight ? graphHeight : this.state.graphHeight} setXDomain={function() {
      this.x.domain([self.props.startDate.toDate(), self.props.endDate.toDate()]);
    }}    setYDomain={function() {
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
    }}
    getYAxis={function() {
      return d3.svg.axis().scale(this.y).orient("left").tickSize(-this.width, 0, 0).tickFormat(function(y) {
        var abs_y = Math.abs(y);
        if (abs_y >= 1000000000000) {
          return y / 1000000000000 + "T";
        } else if (abs_y >= 1000000000) {
          return y / 1000000000 + "B";
        } else if (abs_y >= 1000000) {
          return y / 1000000 + "M";
        } else if (abs_y >= 1000) {
          return y / 1000 + "K";
        } else if (y % 1 != 0) {
          return y.toFixed(1);
        } else {
          return y;
        }
      });
    }} onBrushEnd={function() {
      if (!this.brush.empty()) {
        const newChartXRange = this.brush.extent();
        self.props.datePickerCallback(moment(newChartXRange[0]), moment(newChartXRange[1]));
      }
    }} /></div>;
  }
  handleSelectComponent = (key, event) => {
    if (key) {
      let compId = parseInt(event.target.dataset.nodeid);
      let compObj = this.props.components.find((c)=>{return c.nodeId === compId;});
      this.props.compSelectCallback(compId, compObj);
    } else {
      this.props.compSelectCallback('', null);
    }
  }
  showGraphInModal = (graphTitle, graphType, graphData, interpolation) => {
    this.setState({graphType, graphTitle, graphData, interpolation});
    this.graphModalRef.show();
  }
  getMetrics(){
    const {
      topologyMetric,
      components,
      viewModeData,
      engine
    } = this.props;
    const selectedComponentId = viewModeData.selectedComponentId;
    let selectedComponent = selectedComponentId !== '' ? components.find((c)=>{return c.nodeId === parseInt(selectedComponentId);}) : {};

    const componentType = selectedComponent.parentType || 'topology';
    const componentName = null;
    let template = Utils.getViewModeMetricsTemplate(engine, componentType, selectedComponent.currentType);

    const metrics = [];
    if(!_.isUndefined(viewModeData.overviewMetrics) && !_.isUndefined(viewModeData.overviewMetrics.metrics)){
      _.each(template, (t) => {
        const oValue = _.get(viewModeData.overviewMetrics.metrics, t.metricKeyName);
        const value = Utils[t.valueFormat](oValue);
        const diffValue = Utils[t.valueFormat](oValue - _.get(viewModeData.overviewMetrics.prevMetrics, t.metricKeyName));

        const component = <div className="topology-foot-widget">
          <h6>{t.uiName}
            <big>
              <i className={diffValue.value <= 0 ? "fa fa-arrow-down" : "fa fa-arrow-up"}></i>
            </big>
          </h6>
          <h4>{value.value}{value.suffix}&nbsp;
            <small>{diffValue.value <= 0 || diffValue.value ? '' : '+'}
              {diffValue.value}{diffValue.suffix}
            </small>
          </h4>
        </div>;
        metrics.push(component);
      });
    }

    return metrics;
  }
  getTimeseriesMetrics(){
    const {
      topologyMetric,
      components,
      viewModeData,
      engine
    } = this.props;
    const selectedComponentId = viewModeData.selectedComponentId;
    let selectedComponent = selectedComponentId !== '' ? components.find((c)=>{return c.nodeId === parseInt(selectedComponentId);}) : {};

    const componentType = selectedComponent.parentType || 'topology';
    const componentName = null;
    let template = Utils.getViewModeTimeseriesMetricsTemplate(engine, componentType, selectedComponent.currentType);

    const metrics = [];
    if(!_.isUndefined(viewModeData.timeSeriesMetrics)){
      const timeSeriesMetrics = viewModeData.timeSeriesMetrics.metrics;

      const loader = <img src="styles/img/start-loader.gif" alt="loading" style={{
        width: "80px",
        marginTop: "0px"
      }}/>;

      _.each(template, (t) => {
        const graphData = [];
        const firstLineData = timeSeriesMetrics[t.metricKeyName[0]];
        for(const key in firstLineData) {
          const obj = {
            date: new Date(parseInt(key))
          };
          _.each(t.metricKeyName, (kName) => {
            obj[kName] = timeSeriesMetrics[kName][key] || 0;
          });
          graphData.push(obj);
        }

        const component = <div className="col-md-3">
          <div className="topology-foot-graphs">
            <div style={{textAlign: "left", marginLeft: '10px', cursor:'pointer', textTransform: 'uppercase'}} onClick={this.showGraphInModal.bind(this, t.uiName, '_'+t.uiName, graphData, t.interpolate)}>{t.uiName}</div>
            <div style={{
              height: '82px',
              textAlign: 'center'
            }}>
              {this.state.loadingRecord ? loader : this.getGraph(t.uiName, graphData, t.interpolate)}
            </div>
          </div>
        </div>;

        metrics.push(component);
      });
    }

    return metrics;
  }
  render() {
    const {
      topologyMetric,
      components,
      viewModeData
    } = this.props;
    /*const {metric} = topologyMetric || {
      metric: (topologyMetric === '')
        ? ''
        : topologyMetric.metric
    };*/
    const metricWrap = topologyMetric;
    const appMisc = metricWrap.misc || null;
    const workersTotal = appMisc? appMisc.workersTotal : 0, executorsTotal = appMisc ? appMisc.executorsTotal : 0;
    let overviewMetrics = {};
    let timeSeriesMetrics = {};

    if(!_.isUndefined(viewModeData.overviewMetrics)) {
      overviewMetrics = viewModeData.overviewMetrics;
      timeSeriesMetrics = viewModeData.timeSeriesMetrics;
    }

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

    const selectedComponentId = viewModeData.selectedComponentId;
    let selectedComponent = selectedComponentId !== '' ? components.find((c)=>{return c.nodeId === parseInt(selectedComponentId);}) : {};
    const loader = <img src="styles/img/start-loader.gif" alt="loading" style={{
      width: "80px",
      marginTop: "0px"
    }}/>;
    const topologyFooter = (
      <div className="topology-foot">
        <div className="clearfix topology-foot-top">
          <div className="topology-foot-component">
          <div>
            {selectedComponentId !== '' ?
            (<div className="pull-left" style={{display: 'inline-block', padding: '10px'}}>
            <img src={selectedComponent.imageURL} className="component-img" ref="img" style={{width: "26px", height: "26px"}} />
            </div>
            ) : ''
            }
            <div className="pull-right">
              <DropdownButton title={selectedComponent.uiname || 'All Components'} dropup id="component-dropdown" onSelect={this.handleSelectComponent}>
                {this.props.components.map((c, i) => {
                  return <MenuItem active={parseInt(selectedComponentId) === c.nodeId ? true : false} eventKey={i+1} key={i+1} data-nodeid={c.nodeId} data-uiname={c.uiname}>{c.uiname}</MenuItem>;
                })
                }
                <MenuItem active={selectedComponentId === '' ? true : false}>All Components</MenuItem>
              </DropdownButton>
              {selectedComponentId !== '' ? <div><h6>{selectedComponent.currentType}</h6></div> : ''}
            </div>
          </div>
          </div>

          {this.getMetrics()}

          <div className="topology-foot-action" onClick={()=>{this.setState({showMetrics: !this.state.showMetrics});}}>
            {this.state.showMetrics ?
              <span>Hide Metrics <i className="fa fa-chevron-down"></i></span>
            : <span>Show Metrics <i className="fa fa-chevron-up"></i></span>
            }
          </div>
        </div>
      </div>
    );
    const sourceGraphDivContent = function(){
      let content=[];
      content.push(
        <div className="col-md-3" key={33}>
          <div className="topology-foot-graphs">
            <div style={{textAlign: "left", marginLeft: '10px', cursor:'pointer'}} onClick={this.showGraphInModal.bind(this, 'Latency', '_Latency', completeLatency, 'step-before')}>{selectedComponent.parentType === 'SOURCE' ? 'Complete Latency' : 'Latency'}</div>
            <div style={{
              height: '82px',
              textAlign: 'center'
            }}>
              {this.state.loadingRecord ? loader : this.getGraph('Latency', completeLatency, 'step-before')}
            </div>
          </div>
        </div>
      );
      if(selectedComponent.currentType === "Kafka"){
        content.push(
          <div className="col-md-3" key={34}>
            <div className="topology-foot-graphs">
              <div style={{textAlign: "left", marginLeft: '10px', cursor:'pointer'}} onClick={this.showGraphInModal.bind(this, 'Kafka Offset Lag', '_KafkaLagOffset', kafkaLagOffsetData, 'step-before')}>Kafka Offset Lag</div>
              <div style={{
                height: '82px',
                textAlign: 'center'
              }}>
                {this.state.loadingRecord ? loader : this.getGraph('KafkaLagOffset', kafkaLagOffsetData, 'step-before')}
              </div>
            </div>
          </div>
        );
      }
      return content;
    };
    return (
      <div className="topology-metrics-container" style={app_state.sidebar_isCollapsed ? {} : {paddingLeft: '230px'}}>
        <Panel
          header={topologyFooter}
          collapsible
          expanded={this.state.showMetrics}
          onEnter={()=>{this.setState({loadingRecord: true});}}
          onEntered={()=>{this.setState({loadingRecord: false});}}
        >
          {this.state.showMetrics ?
          [<div className="row" key={1}>
            {this.getTimeseriesMetrics()}
            
          </div>]
          : null}
        </Panel>
        <Modal
          dialogClassName="modal-xl"
          ref={(ref) => this.graphModalRef = ref}
          data-title={this.state.graphTitle}
          hideOkBtn={true}
        >
          <div className="component-graph-modal-container">
            <div className="pull-right" style={{marginLeft: '15px',marginBottom: '15px'}}>
              <DateTimePickerDropdown
                dropdownId="log-search-datepicker-dropdown"
                startDate={this.props.startDate}
                endDate={this.props.endDate}
                activeRangeLabel={this.props.activeRangeLabel}
                locale={locale}
                isDisabled={false}
                datePickerCallback={this.props.datePickerCallback} />
            </div>
            <div>
            {this.getGraph(this.state.graphType, this.state.graphData, this.state.interpolation, 400)}
            </div>
          </div>
        </Modal>
      </div>
    );
  }
}

export default TopologyViewModemetrics;
