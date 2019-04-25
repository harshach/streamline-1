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
import PropTypes from 'prop-types';
import _ from 'lodash';
import {Link} from 'react-router';
import moment from 'moment';
import DateTimePickerDropdown from '../../../components/DateTimePickerDropdown';
import {DropdownButton, MenuItem, InputGroup, OverlayTrigger, Tooltip, Button, ButtonGroup, ToggleButtonGroup,
  ToggleButton, Panel} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import ViewModeREST from '../../../rest/ViewModeREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import ClusterREST from '../../../rest/ClusterREST';
import TopologyUtils from '../../../utils/TopologyUtils';
import app_state from '../../../app_state';
import {hasEditCapability, hasViewCapability,findSingleAclObj,handleSecurePermission} from '../../../utils/ACLUtils';
import {observer} from 'mobx-react';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import ComponentLogActions from '../../../containers/Streams/Metrics/ComponentLogActions';

@observer
class TopologyViewMode extends Component {
  constructor(props) {
    super(props);
    this.state = {
      stormViewUrl: '',
      displayTime: '0:0',
      sampling: 0,
      selectedMode: props.viewModeData.selectedMode,
      startDate: props.startDate,
      endDate: props.endDate,
      showLogSearchBtn: props.showLogSearchBtn,
      loading : false
    };
    this.stormClusterChkID(props.stormClusterId);
    this.logInputNotify = false;
  }
  stormClusterChkID = (id) => {
    if (id) {
      this.fetchData(id);
    }
  }
  componentDidMount() {
    const container = document.querySelector('.content-wrapper');
    container.setAttribute("class", "content-wrapper view-mode-wrapper");
    if(this.props.isAppRunning){
      setTimeout(() => {
        // this.getLogLevel();
      });
    }
    this.compUnMountFlag = false;
  }
  componentWillUnmount() {
    const container = document.querySelector('.content-wrapper');
    container.setAttribute("class", "content-wrapper");
    this.compUnMountFlag = true;
  }
  componentWillReceiveProps(props) {
    if (props.stormClusterId) {
      this.fetchData(props.stormClusterId);
    }
    if(this.props.viewModeData.logTopologyLevel !== props.viewModeData.logTopologyLevel || this.props.viewModeData.durationTopologyLevel !== props.viewModeData.durationTopologyLevel){
      this.logInputNotify = true;
    }
    this.setState({startDate: props.startDate, endDate: props.endDate, showLogSearchBtn: props.showLogSearchBtn});
  }
  fetchData(stormClusterId) {
    ClusterREST.getStormViewUrl(stormClusterId).then((obj) => {
      if (obj.url) {
        this.setState({stormViewUrl: obj.url});
      }
    });
  }
  getLogLevel() {
    this.setState({loading: true});
    ViewModeREST.getTopologyLogConfig(this.props.topologyId).then((result)=>{
      if(result.responseMessage == undefined){
        if(moment(result.epoch).diff(moment()) >= 0) {
          this.props.topologyLevelDetailsFunc('LOGS',result.logLevel);
          this.setTimer(result.epoch);
        }
        this.setState({loading:false});
      }
    });
  }
  changeLogLevel = (value) => {
    this.props.topologyLevelDetailsFunc('LOGS',value);
  }
  changeLogDuration = (value) => {
    this.props.topologyLevelDetailsFunc('DURATIONS',value);
  }
  changeSampling = (value) => {
    this.props.topologyLevelDetailsFunc('SAMPLE',value);
  }
  toggleLogLevelDropdown = (isOpen) => {
    let {logTopologyLevel, durationTopologyLevel,sampleTopologyLevel} = this.props.viewModeData;
    if(!isOpen && logTopologyLevel !== 'None' && durationTopologyLevel > 0 ) {
      this.topologySampleLavelCallBack(sampleTopologyLevel);
      this.postTopologyLogConfigCallBack(logTopologyLevel,durationTopologyLevel);
      if(this.logInputNotify){
        FSReactToastr.success(<strong>Changing log level successfully</strong>);
        this.logInputNotify = false;
      }
    } else if(!isOpen && logTopologyLevel === 'None' && durationTopologyLevel <= 0){
      this.topologySampleLavelCallBack(sampleTopologyLevel);
    }
  }

  topologySampleLavelCallBack = (sampleTopologyLevel) => {
    const flag = sampleTopologyLevel > 0 ? 'enable' : 'disable' ;
    this.props.topologyLevelDetailsFunc('SAMPLE',flag);
  }

  postTopologyLogConfigCallBack = (logTopologyLevel,durationTopologyLevel) => {
    ViewModeREST.postTopologyLogConfig(this.props.topologyId, logTopologyLevel, durationTopologyLevel)
    .then((res)=>{
      if(res.epoch) {
        clearInterval(this.intervalId);
        this.setTimer(res.epoch);
      }
    });
  }

  setTimer(epochTime) {
    var interval = 1000;
    this.intervalId = setInterval(function(){
      var currentTime = moment();
      var leftTime = moment.duration(moment(epochTime).diff(currentTime));
      var minutes = leftTime.minutes();
      var seconds = leftTime.seconds();
      if(minutes == 0 && seconds == 0) {
        clearInterval(this.intervalId);
        this.props.topologyLevelDetailsFunc('LOGS','None');
        this.props.topologyLevelDetailsFunc('DURATIONS',0);
      }
      !this.compUnMountFlag ? this.setState({displayTime: minutes+':'+seconds}) : null;
    }.bind(this), interval);
  }
  changeMode = (value) => {
    this.props.modeSelectCallback(value);
  }
  handleSelectVersion(eventKey, event) {
    let versionId = event.target.dataset.versionId;
    this.props.handleVersionChange(versionId);
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

  navigateToLogSearch = () => {
    const {viewModeData,topologyId} = this.props;
    this.context.router.push({
      pathname : 'logsearch/'+topologyId,
      state : {
        componentId : viewModeData.selectedComponentId
      }
    });
  }

  render() {
    let {stormViewUrl, startDate, endDate, ranges, rangesInHoursMins, rangesInDaysToYears, rangesPrevious, displayTime, showLogSearchBtn,loading} = this.state;
    const {
      projectId,
      topologyId,
      topologyName,
      isAppRunning,
      killTopology,
      setCurrentVersion,
      runtimeAppId,
      timestamp,
      topologyVersion,
      versionsArr = [],
      allACL,
      viewModeData,
      disabledTopologyLevelSampling,
      engine
    } = this.props;

    let versionName = this.getTitleFromId(topologyVersion);

    if (runtimeAppId && stormViewUrl.length) {
      if (stormViewUrl.indexOf('/main/views/') == -1) {
        stormViewUrl = stormViewUrl + '/topology.html?id=' + runtimeAppId;
      } else {
        //Storm view requires the path to be encoded
        stormViewUrl = stormViewUrl + '?viewpath=%23%2Ftopology%2F' + encodeURIComponent(runtimeAppId);
      }
    }
    const userInfo = app_state.user_profile !== undefined ? app_state.user_profile.admin : false;
    let permission=true, aclObject={};
    if(app_state.streamline_config.secureMode){
      aclObject = findSingleAclObj(topologyId, allACL || []);
      const {p_permission} = handleSecurePermission(aclObject,userInfo,"Applications");
      permission = p_permission;
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
    let labelStart = this.state.startDate.format('YYYY-MM-DD HH:mm:ss');
    let labelEnd = this.state.endDate.format('YYYY-MM-DD HH:mm:ss');

    const titleContent = (
      <span>All Components &emsp; {displayTime == '0:0' ? '' : (<span>Timer: <span style={{color: '#d06831'}}>{displayTime}</span> &emsp; </span>)}Log: <span style={{color: '#2787ad'}}>{_.capitalize(viewModeData.logTopologyLevel)}</span> &emsp; Sampling: <span style={{color: '#2787ad'}}>{viewModeData.sampleTopologyLevel}%</span></span>
    );
    const datePickerTitleContent = (
      <span><i className="fa fa-clock-o"></i> {moment.duration(startDate.diff(endDate)).humanize()}</span>
    );
    return (
      null
    );
    return (
      <div>
        <div className="view-mode-title-box">
          {/*
          engine.type == 'stream' &&
          <div className="col-sm-4">
            <DropdownButton title={titleContent}
              id="log-duration-dropdown"
              disabled={!isAppRunning}
              onToggle={this.toggleLogLevelDropdown}
              pullRight
            >
              <ComponentLogActions
                logLevelValue={viewModeData.logTopologyLevel}
                logLevelChangeFunc={this.changeLogLevel}
                durationValue={viewModeData.durationTopologyLevel}
                durationChangeFunc={this.changeLogDuration}
                samlpingValue={viewModeData.sampleTopologyLevel}
                samplingChangeFunc={this.changeSampling}
                allComponentLevelAction={viewModeData.allComponentLevelAction}
                topologyId={topologyId}
                refType="view-container"
                disabledTopologyLevelSampling={disabledTopologyLevelSampling}
              />
            </DropdownButton>
          </div>
          */}
          {/*
          engine.type == 'stream' &&
          <div className="col-sm-4 text-right">
            <div>
              <span className="text-muted">Mode: </span>
                <ToggleButtonGroup type="radio" name="mode-select-options" defaultValue={viewModeData.selectedMode} onChange={this.changeMode}>
                  <ToggleButton className="mode-select-btn left" disabled={!isAppRunning} value="Overview">OVERVIEW</ToggleButton>
                  {<ToggleButton className="mode-select-btn right" disabled={!isAppRunning} value="Metrics">METRICS</ToggleButton>}
                  <ToggleButton className="mode-select-btn right" disabled={!isAppRunning} value="Sample">SAMPLE</ToggleButton>
              </ToggleButtonGroup>
            </div>
          </div>
          */}
          {showLogSearchBtn ?
            <div className="pull-right">
              <a href="javascript:void(0)" style={{marginLeft: '10px', top: '5px'}} key={3}
                className="hb sm default-blue"
                onClick={this.navigateToLogSearch}
              >
                <i className="fa fa-book"></i>
              </a>
            </div>
          : null}
          {/*
          engine.type == 'stream' &&
          <div className="pull-right" style={{marginLeft: '10px'}}>
            <DateTimePickerDropdown
              dropdownId="datepicker-dropdown"
              startDate={startDate}
              endDate={endDate}
              activeRangeLabel={this.props.activeRangeLabel}
              locale={locale}
              isDisabled={!isAppRunning}
              datePickerCallback={this.props.datePickerCallback} />
          </div>
          */}
          <div className="pull-right">
            {stormViewUrl.length
            ? <a href={stormViewUrl} target="_blank" className="btn btn-default"><img src="styles/img/storm-btn.png" width="20"/></a>
            : null}
          </div>
        </div>
      </div>
    );
  }
}

TopologyViewMode.contextTypes = {
  router: PropTypes.object.isRequired
};

export default TopologyViewMode;
