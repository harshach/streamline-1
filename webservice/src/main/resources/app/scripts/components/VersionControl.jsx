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
    const {data, onClick, selectedVersionName} = this.props;
    return <div className={`version-container ${data.name == selectedVersionName ? 'selected' : ''}`} onClick={() => onClick(data.id)}>
      <div className="version-thumb" ref="thumbContainer">
      </div>
      <div className="version-info">
        {data.name == 'CURRENT' && <span className="version-current-label">Current</span>}
        <span className="version-name">{data.name}</span>
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
      expanded: false
    };
  }
  componentDidUpdate(){
  }
  handleExpandCollapse = () => {
    const {expanded} = this.state;
    this.setState({expanded: !expanded});
  }
  getHeader = () => {
    const {selectedVersionName, setCurrentVersion} = this.props;
    return <button className="btn-panels" onClick={this.handleExpandCollapse}><img src="styles/img/uWorc/version-icon.png"/></button>;
  }
  getBody = () => {
    const {sliderSettings} = this.state;
    const {versions, handleVersionChange, selectedVersionName, getCurrentVersionThumbnail, setCurrentVersion, lastUpdatedTime} = this.props;

    const verComps = _.map(versions, (v, i) => {
      return <div>
        <VersionThumbnail
          key={i}
          data={v}
          onClick={handleVersionChange}
          selectedVersionName={selectedVersionName}
          getCurrentVersionThumbnail={getCurrentVersionThumbnail}
          lastUpdatedTime={lastUpdatedTime}
        />
      </div>;
    });

    const content = (<div>
      <div className="right-sidebar-header">
        <div>
          <h6 className="version-control">Version Control</h6>
          <DropdownButton bsStyle="link" className="btn-sm" title={selectedVersionName} id="version-dropdown" onSelect={(v) => {
            handleVersionChange(v);
          }} >
            {_.map(versions, (v, i) => {
              return <MenuItem active={selectedVersionName === v.name ? true : false} eventKey={v.id} key={i} data-version-id={v.id}>{v.name}</MenuItem>;
            })
          }
          </DropdownButton>
        </div>
        <div className="text-right">
          <button className="btn btn-primary btn-sm set-version-btn" onClick={setCurrentVersion}>Set as Current</button>
        </div>
        <div className="text-right">
          <button type="button" className="close" style={{marginLeft:'5px'}} onClick={this.handleExpandCollapse}><span >×</span></button>
        </div>
      </div>
      <div className="right-sidebar-body">
        {verComps}
      </div>
    </div>);

    return content;
  }
  render(){
    const {expanded} = this.state;
    const {selectedVersionName, currentVersionDagThumbnail} = this.props;
    return <RightSideBar
      getHeader={this.getHeader}
      getBody={this.getBody}
      selectedVersionName={selectedVersionName}
      expanded={expanded}
      currentVersionDagThumbnail={currentVersionDagThumbnail}
    />;
  }
}
