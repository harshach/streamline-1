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
import state from '../../scripts/app_state';
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
      mode
    } = this.props;
    return (
      <div className="col-md-12 zoomWrap clearfix">
        <div className="editor-header row">
          {mode === 'edit' ?
            <div className="pull-left">
              <span className="graph-action"><img src="styles/img/uWorc/undo.png" /> Undo</span>
              <span className="graph-action"><img src="styles/img/uWorc/redo.png" /> Redo</span>
              <span className="graph-action"><img src="styles/img/uWorc/command.png" /> Shortcuts</span>
            </div>
            : null
          }
          <div className="pull-right">
            <button className={`btn-panels ${mode == 'view' ? 'active' : ''}`} onClick={this.onViewClick}><img src="styles/img/uWorc/view.png" /></button>
            <button className={`btn-panels ${mode == 'edit' ? 'active' : ''}`} onClick={this.onEditClick}><img src="styles/img/uWorc/edit.png" /></button>
          </div>
        </div>
        <div className="topology-editor-controls pull-left">
          <div className="zoom-btn-container">
            <OverlayTrigger placement="top" overlay={<Tooltip id ="tooltip"> Zoom Out </Tooltip>}>
              <a href="javascript:void(0);" className="zoom-out" onClick={zoomOutAction}>
                <i className="fa fa-minus"></i>
              </a>
            </OverlayTrigger>
            <OverlayTrigger placement="top" overlay={<Tooltip id ="tooltip"> Zoom In </Tooltip>}>
              <a href="javascript:void(0);" className="zoom-in" onClick={zoomInAction}>
                <i className="fa fa-plus"></i>
              </a>
            </OverlayTrigger>
          </div>
          { mode == 'edit' &&
          <OverlayTrigger placement="top" overlay={<Tooltip id ="tooltip"> Configure </Tooltip>}>
            <a href="javascript:void(0);" className="config" onClick={showConfig}>
              <i className="fa fa-gear"></i>
            </a>
          </OverlayTrigger>
          }
        </div>
      </div>
    );
  }
}

export default ZoomPanelComponent;
