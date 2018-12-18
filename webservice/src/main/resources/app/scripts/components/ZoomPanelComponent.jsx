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

@observer
class  ZoomPanelComponent extends Component {
  onEditClick = () => {
    const {router, projectId, topologyId} = this.props;
    router.push('projects/'+ projectId +'/applications/'+ topologyId +'/edit');
  }
  onViewClick = () => {
    const {router, projectId, topologyId} = this.props;
    router.push('projects/'+ projectId +'/applications/'+ topologyId +'/view');
  }
  renderActionButton = () => {
    const {isAppRunning, topologyStatus, killTopology, deployTopology} = this.props;
    let btn = [];
    if(isAppRunning){
      btn.push(
        <button key="kill" className="btn btn-primary btn-sm workflow-action-btn m-r-xs" onClick={killTopology}>
          <i className="fa fa-ban workflow-btn"></i> Kill
        </button>
      );
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
      topologyStatus
    } = this.props;
    let isActive = false;
    if(!app_state.versionPanelCollapsed && mode == 'edit'){
      isActive = true;
    } else if(!app_state.versionPanelCollapsed && mode == 'view' && isAppRunning){
      isActive = true;
    }
    return (
      <div>
        <div className={`control-widget right ${isActive ? 'active' : ''}`}>
          <div className="control-top">
            <h5>Last Edited <span>{Utils.datetime(lastUpdatedTime).value}</span></h5>
            {isAppRunning ? <h5>Workflow Status<span><i className="fa fa-circle text-primary workflow-status"></i> Running</span></h5> : null}
          </div>
          <div className="control-bottom text-center">
            {mode === 'view' ?
              <button className="btn btn-primary btn-sm workflow-action-btn" onClick={this.onEditClick}><i className="fa fa-pencil workflow-btn"></i> Edit Workflow</button>
            :
              this.renderActionButton()
            }
          </div>
        </div>
        <div className="control-widget left">
          <div className="row">
            <div className="col-sm-12">
              <div className="image-control-slider">
                <button className="btn btn-xs" onClick={zoomOutAction}>
                  <i className="fa fa-minus"></i>
                </button> <input type="range" readOnly value={app_state.zoomScale}
                  min={1} max={8} step={(8 - 1) / 100}
                /> <button className="btn btn-xs" onClick={zoomInAction}>
                  <i className="fa fa-plus"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
        <div className="col-md-12 zoomWrap clearfix">
          <div className={`editor-header row ${isActive ? 'active' : ''} ${isAppRunning ? 'app-running' : ''}`}>
            {mode === 'edit' ?
              <div className="pull-left">
                <span className="graph-action"><img src="styles/img/uWorc/undo.png" /> Undo</span>
                <span className="graph-action"><img src="styles/img/uWorc/redo.png" /> Redo</span>
                <span className="graph-action"><img src="styles/img/uWorc/command.png" /> Shortcuts</span>
                <span className="graph-action" onClick={showConfig}><img src="styles/img/uWorc/setting.png" /> Configure Settings</span>
              </div>
              : null
            }
            <div className="pull-right">
              <button className={`btn-panels ${mode == 'view' ? 'active' : ''}`} onClick={this.onViewClick}><img src="styles/img/uWorc/view.png" /></button>
              <button className={`btn-panels ${mode == 'edit' ? 'active' : ''}`} onClick={this.onEditClick}><img src="styles/img/uWorc/edit.png" /></button>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default ZoomPanelComponent;
