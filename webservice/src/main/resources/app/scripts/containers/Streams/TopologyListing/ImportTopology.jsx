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
import _ from 'lodash';
import {Select2 as Select} from '../../../utils/SelectUtils';

/* import common utils*/
import TopologyREST from '../../../rest/TopologyREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import ProjectREST from '../../../rest/ProjectREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import Form from '../../../libs/form';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';
import app_state from '../../../app_state';

class ImportTopology extends Component {
  constructor(props) {
    super(props);
    this.state = {
      jsonFile: null,
      namespaceId: '',
      namespaceOptions: [],
      validInput: true,
      validSelect: true,
      showRequired: true,
      nameError : true,
      projects: [],
      projectId: props.defaultProjectId ? props.defaultProjectId : -1
    };
    this.fetchData();
  }

  fetchData = () => {
    let promiseArr = [EnvironmentREST.getAllNameSpaces()];
    promiseArr.push(ProjectREST.getMyProjects().then((res) => {
      const projects = res.entities;
      this.setState({projects: projects});
      return projects;
    }));
    Promise.all(promiseArr).then(result => {
      if (result[0].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt);
      } else {
        const resultSet = this.namespacesArr = result[0].entities;
        let namespaces = [];
        resultSet.map((e) => {
          namespaces.push(e.namespace);
        });
        this.setState({namespaceOptions: namespaces});
      }
    });
  }

  validate() {
    const {jsonFile, namespaceId} = this.state;
    let validDataFlag = true;
    if (!jsonFile) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if (namespaceId === '') {
      validDataFlag = false;
      this.setState({validSelect: false});
    } else {
      validDataFlag = true;
      this.setState({validInput: true, validSelect: true});
    }
    return validDataFlag;
  }

  handleSave = () => {
    const {jsonFile, namespaceId, projectId} = this.state;
    const topologyName = this.refs.topologyName.value.trim();
    let formData = new FormData();
    topologyName
      ? formData.append('topologyName', topologyName)
      : '';
    formData.append('file', jsonFile);
    formData.append('namespaceId', namespaceId);
    formData.append('projectId', projectId);

    return TopologyREST.importTopology({body: formData});
  }
  handleOnFileChange = (event) => {
    if (!event.target.files.length ||
        (event.target.files.length && event.target.files[0].name.indexOf('.json') < 0)
      ){
      this.setState({validInput: false, jsonFile: null});
    } else {
      this.parseFile(event);
      this.setState({validInput: true, jsonFile: event.target.files[0]});
    }
  }
  parseFile(event){
    let self = this;
    let reader = new FileReader();
    reader.onload = function(event){
      let jsonObj = JSON.parse(event.target.result);
      let engineId = jsonObj.engineId;
      self.syncNamespaces(engineId);
    };
    reader.readAsText(event.target.files[0]);
  }
  syncNamespaces(engineId){
    let engine = app_state.engines.find((e)=>{
      return e.id === engineId;
    });
    let engineName = engine ? engine.name.toLowerCase() : '';
    let namespaces = [];
    this.namespacesArr.map((e) => {
      let namespaceObj = e.mappings.find((o)=>{
        return o.serviceName.toLowerCase() === engineName.toLowerCase();
      });
      if(namespaceObj){
        namespaces.push(e.namespace);
      }
    });
    this.setState({namespaceOptions: namespaces, namespaceId: ''});
  }
  handleOnChangeEnvironment = (obj) => {
    if (obj) {
      this.setState({namespaceId: obj.id, validSelect: true});
    } else {
      this.setState({namespaceId: '', validSelect: false});
    }
  }
  handleOnChangeProject = (obj) => {
    this.setState({projectId: obj ? obj.id : -1});
  }
  topologyNameChange = (e) => {
    this.setState({nameError : Utils.noSpecialCharString(e.target.value)});
  }

  render() {
    const {validInput, validSelect, showRequired, namespaceId, namespaceOptions,nameError,
      projectId, projects} = this.state;
    const proj = projects.filter(proj => {
      return proj.id != -1;
    });
    return (
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label data-stest="selectJsonLabel">Select JSON File
            <span className="text-danger">*</span>
          </label>
          <div>
            <input type="file" className={validInput
              ? "form-control"
              : "form-control invalidInput"} accept=".json" name="files" title="Upload File" onChange={this.handleOnFileChange}/>
          </div>
          {!validInput ? <p className="text-danger">Required!</p>: null}
        </div>
        <div className="form-group">
          <label data-stest="topologyNameLabel">Application Name
          </label>
          <div>
            <input type="text" className={nameError ? "form-control" : "form-control invalidInput" } name="name" title="Application Name" ref="topologyName" onChange={this.topologyNameChange.bind(this)}/>
          </div>
        </div>
        <div className="form-group">
          <label data-stest="environmentLabel">Data Center
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={namespaceId} options={namespaceOptions} onChange={this.handleOnChangeEnvironment} className={!validSelect
              ? 'invalidInput'
              : ''} placeholder="Select Data Center" required={true} clearable={false} labelKey="name" valueKey="id"/>
          </div>
          {!validSelect ? <p className="text-danger">Required!</p>: null}
        </div>
        <div className="form-group">
          <label>Project
          </label>
          <div>
            <Select
              value={projectId}
              options={proj}
              onChange={this.handleOnChangeProject}
              placeholder="Select Project"
              required={true}
              clearable={true}
              labelKey="name"
              valueKey="id"/>
          </div>
        </div>
      </div>
    );
  }
}

export default ImportTopology;
