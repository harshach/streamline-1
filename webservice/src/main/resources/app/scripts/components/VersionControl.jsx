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
import {EditorFooter} from '../containers/Streams/TopologyEditor/TopologyViewModeMetrics';
import Slider from "react-slick";
import moment from 'moment';
import d3 from 'd3';
import RightSideBar from './RightSideBar';
import app_state from '../app_state';

class VersionThumbnail extends Component{
  constructor(props) {
    super(props);
  }
  componentDidMount(){
    this.renderGraph();
  }
  componentDidUpdate(){
    this.renderGraph();
  }
  renderGraph(){
    const {data, getCurrentVersionThumbnail} = this.props;
    const graphStr = data.name == 'CURRENT' ? getCurrentVersionThumbnail() : data.dagThumbnail;
    if(graphStr){
      const container = d3.select(this.refs.thumbContainer);
      container.html(graphStr);
      const graphG = container.select('g.graph');
      const graphTransform = graphG.attr('transform');
      graphG.attr('transform', () => {
        return 'translate(0,0)scale(0.15)';
      });
    }
  }
  render(){
    const {data, onClick, selectedVersionName, isDeployedVersion} = this.props;
    return <div className={`version-container ${data.name == selectedVersionName ? 'selected' : ''}`} onClick={() => onClick(data.id)}>
      <div className="version-thumb" ref="thumbContainer">
      </div>
      <div className="version-info">
        {data.name == 'CURRENT' && <span className="version-current-label">Draft</span>}
        {isDeployedVersion && <span className="version-current-label">Deployed</span>}
        <span className="version-name">{data.name == 'CURRENT' ? "DRAFT" : data.name}</span>
        <span className="version-updated-label">Last updated on</span>&nbsp;
        <span className="version-updated-value">{moment(data.timestamp).format('MM/DD/YYYY HH:mm')}</span>
      </div>
    </div>;
  }
}

export default class VersionControl extends Component{
  constructor(props) {
    super(props);
    this.state = {
      versionPanelCollapsed: true
    };
  }
  componentDidMount(){
  }
  handleExpandCollapse = () => {
    let {versionPanelCollapsed} = this.state;
    this.setState({versionPanelCollapsed: !versionPanelCollapsed});
  }
  getHeader = () => {
    let {versionPanelCollapsed} = this.state;
    return <button
      className="btn-bottom-panel"
      onClick={this.handleExpandCollapse}
    ><i className={versionPanelCollapsed ? "fa fa-chevron-up" : "fa fa-chevron-down"}></i></button>;
  }
  getBody = () => {
    const {sliderSettings} = this.state;
    const {versions, handleVersionChange, selectedVersionName, getCurrentVersionThumbnail, setCurrentVersion, lastUpdatedTime} = this.props;

    const verComps = _.map(versions, (v, i) => {
      return <VersionThumbnail
          key={i}
          data={v}
          onClick={handleVersionChange}
          selectedVersionName={selectedVersionName}
          getCurrentVersionThumbnail={getCurrentVersionThumbnail}
          lastUpdatedTime={lastUpdatedTime}
          isDeployedVersion={i==1}
        />;
    });

    const content = ([
      <div className="bottom-panel-header" key="version-header">
        <DropdownButton bsStyle="link" className="btn-sm"
          title={selectedVersionName == 'CURRENT' ? 'DRAFT' : selectedVersionName} id="version-dropdown"
          onSelect={(v) => {
            handleVersionChange(v);
          }} >
          {_.map(versions, (v, i) => {
            return <MenuItem active={selectedVersionName === v.name ? true : false}
              eventKey={v.id} key={i} data-version-id={v.id}>{v.name == 'CURRENT' ? 'DRAFT' : v.name}
            </MenuItem>;
          })
        }
        </DropdownButton>
        <button className="btn btn-primary btn-sm set-version-btn" onClick={setCurrentVersion}>Set as Draft</button>
      </div>,
      <div className="bottom-panel-content" key="metrics-body">
        <div className="version-wrapper">
          {verComps}
        </div>
      </div>
    ]);

    return content;
  }
  render(){
    let {versionPanelCollapsed} = this.state;
    const {selectedVersionName, currentVersionDagThumbnail} = this.props;
    return <RightSideBar
      getHeader={this.getHeader}
      getBody={this.getBody}
      selectedVersionName={selectedVersionName}
      currentVersionDagThumbnail={currentVersionDagThumbnail}
      className={versionPanelCollapsed ? "bottom-panel version-control" : "bottom-panel version-control active"}
    />;
  }
}
