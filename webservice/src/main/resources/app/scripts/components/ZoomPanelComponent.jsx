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
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import Utils from '../utils/Utils';
import app_state from '../app_state';
import {observer} from 'mobx-react';
import {MenuItem, DropdownButton} from 'react-bootstrap';

@observer
class  ZoomPanelComponent extends Component {
  onEditClick = () => {
    const {router, projectId, topologyId} = this.props;
    router.push((Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+ projectId +'/applications/'+ topologyId +'/edit');
  }
  onViewClick = () => {
    const {router, projectId, topologyId} = this.props;
    router.push((Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+ projectId +'/applications/'+ topologyId +'/view');
  }
  renderActionButton = () => {
    const {isAppRunning, topologyStatus, killTopology, deployTopology} = this.props;
    let btn = [
      <button key="configure" className="btn btn-default btn-sm workflow-action-btn m-r-xs" onClick={this.props.showConfig}>
        <i className="fa fa-gear"></i>
      </button>
    ];
    if(isAppRunning){
      // btn.push(
      //   <button key="kill" className="btn btn-default btn-sm workflow-action-btn m-r-xs" onClick={killTopology}>
      //     <i className="fa fa-pause"></i>
      //   </button>
      // );
      if(topologyStatus == 'enabled'){
        btn.push(
          <button key="redeploy" className="btn btn-primary btn-sm workflow-action-btn" onClick={deployTopology}>
            <i className="fa fa-play workflow-btn"></i> Redeploy
          </button>
        );
      } else {
        btn.push(
          <button key="deploy" className="btn btn-primary btn-sm workflow-action-btn" onClick={deployTopology}>
            <i className="fa fa-play workflow-btn"></i> Deploy
          </button>
        );
      }
    } else {
      btn.push(
        <button key="deploy" className="btn btn-primary btn-sm workflow-action-btn" onClick={deployTopology}>
          <i className="fa fa-play workflow-btn"></i> Deploy
        </button>
      );
    }
    return btn;
  }
  render(){
    const {lastUpdatedTime,
      versionName,
      zoomInAction,
      zoomOutAction,
      showConfig,
      confirmMode,
      testRunActivated,
      testCompleted,
      handleEventLogHide,
      mode,
      isAppRunning,
      killTopology,
      deployTopology,
      topologyStatus,
      engineType,
      runtimeAppId,
      runtimeAppUrl,
      namespaceName,
      deployedVersion
    } = this.props;
    const modeTitle = <span><small>Mode</small>{mode === 'view' ? "Monitor" : "Edit"}</span>;
    return (
      <div className={`editor-header clearfix`}>
        {mode === 'view' ?
          null
        :
          <div className={`control-widget text-center`}>
            {this.renderActionButton()}
          </div>
        }
        <div className="pull-left">
          <div className="workflow-info">
            <h6>Workflow Status</h6>
            <h5>
              <span>
                <i className={`fa fa-circle ${topologyStatus} workflow-status`}></i> {Utils.capitaliseFirstLetter(topologyStatus)}</span>
            </h5>
          </div>
          {runtimeAppUrl ?
            <div className="workflow-info">
              <h6>Deployed Version</h6>
              <h5>
                {deployedVersion}
              </h5>
            </div>
          : null}
          <div className="workflow-info">
            <h6>Workflow Last Updated</h6>
            <h5>{Utils.datetime(lastUpdatedTime).value}</h5>
          </div>
          {mode === 'edit' ?
          <div className="workflow-info">
            <h6>Data Center</h6>
            <h5>{namespaceName}</h5>
          </div>
          : null }
          {runtimeAppUrl ?
            <div className="workflow-info">
              <h6>Piper ID</h6>
              <h5>
                {runtimeAppId}
              </h5>
            </div>
          : null}
        </div>
        <div className="pull-right">
          <DropdownButton className="btn-mode"
            pullRight
            title={modeTitle} id="mode-dropdown"
            onSelect={(key) => {
              if(key === 'view'){
                this.onViewClick();
              } else {
                this.onEditClick();
              }
            }} >
            <MenuItem active={mode === 'view' ? true : false}
              eventKey={'view'}> Monitor
            </MenuItem>
            <MenuItem active={mode === 'edit' ? true : false}
              eventKey={'edit'}> Edit
            </MenuItem>
          </DropdownButton>
        </div>
      </div>
    );
  }
}

export default ZoomPanelComponent;
