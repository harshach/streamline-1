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

class VersionThumbnail extends Component{
  constructor(props) {
    super(props);
  }
  componentDidMount(){
    this.renderGraph();
  }
  renderGraph(){
    const {data, currentVersionDagThumbnail} = this.props;
    const graphStr = data.name == 'CURRENT' ? currentVersionDagThumbnail : data.dagThumbnail;
    if(graphStr){
      const container = d3.select(this.refs.thumbContainer);
      container.html(graphStr);
      const graphG = container.select('g.graph');
      const graphTransform = graphG.attr('transform');
      graphG.attr('transform', () => {
        return graphTransform.split('scale')[0] + 'scale(0.15)';
      });
    }
  }
  render(){
    const {data, onClick, selectedVersionName} = this.props;
    return <div className={`version-container ${data.name == selectedVersionName ? 'selected' : ''}`} onClick={() => onClick(data.id)}>
      <div className="version-thumb" ref="thumbContainer">
      </div>
      <div className="version-info">
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
      sliderSettings: {
        // dots: true,
        infinite: false,
        speed: 500,
        slidesToShow: 4,
        slidesToScroll: 1
      }
    };
  }
  componentDidUpdate(){
    this.checkForButtons();
  }
  checkForButtons = () => {
    const slickSlider = document.querySelector('.slick-slider');
    if(!slickSlider){
      return;
    }

    const slickPrev = slickSlider.querySelector('.slick-prev');
    if(!slickPrev){
      d3.select(slickSlider).append('button')
        .classed('slick-arrow slick-prev disabled', true);
    }
    const slickNext = slickSlider.querySelector('.slick-next');
    if(!slickNext){      
      d3.select(slickSlider).append('button')
        .classed('slick-arrow slick-next disabled', true);
    }
  }
  getHeader = () => {
    const {selectedVersionName, setCurrentVersion} = this.props;
    return <div className="clearfix topology-foot-top">
      <div className="clearfix version-control-title">
        Version Control
      </div>
      {selectedVersionName != 'CURRENT' && 
        <span className=" version-control-set-current pull-right" onClick={setCurrentVersion}>Set {selectedVersionName} as current version</span>
      }
    </div>;
  }
  getBody = () => {
    const {sliderSettings} = this.state;
    const {versions, handleVersionChange, selectedVersionName, currentVersionDagThumbnail} = this.props;
    const footTop = document.querySelector('.topology-foot-top');
    if(footTop){
      const trackWidth = footTop.clientWidth-160;
      sliderSettings.slidesToShow = Math.floor(trackWidth/215);
    }
    const verComps = _.map(versions, (v, i) => {
      return <div style={{width:'195px'}}>
        <VersionThumbnail
          key={i}
          data={v}
          onClick={handleVersionChange}
          selectedVersionName={selectedVersionName}
          currentVersionDagThumbnail={currentVersionDagThumbnail}
        />
      </div>;
    });
    if(verComps.length < sliderSettings.slidesToShow){
      const diff = sliderSettings.slidesToShow - verComps.length;
      for(let i=0; i<diff;i++){
        verComps.push(<div style={{width:'195px'}}></div>);
      }
    }

    const body = <Slider
      {...sliderSettings}
    >
      {verComps}
    </Slider>;

    setTimeout(this.checkForButtons, 500);

    return body;
  }
  render(){
    const {selectedVersionName} = this.props;
    return <EditorFooter
      getHeader={this.getHeader}
      getBody={this.getBody}
      selectedVersionName={selectedVersionName}
    />;
  }
}