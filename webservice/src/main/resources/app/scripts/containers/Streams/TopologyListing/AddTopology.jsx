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
import EngineREST from '../../../rest/EngineREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import Form from '../../../libs/form';
import app_state from '../../../app_state';
import {FormGroup, InputGroup, FormControl} from 'react-bootstrap';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';

class AddTopology extends Component {
  constructor(props) {
    super(props);
    this.state = {
      topologyName: props.topologyData ? props.topologyData.topology.name : '',
      namespaceId: props.topologyData ? props.topologyData.topology.namespaceId : '',
      namespaceOptions: [],
      engineId: props.topologyData ? props.topologyData.topology.engineId : '',
      templateId: props.topologyData ? props.topologyData.topology.templateId : '',
      validInput: true,
      validSelect: true,
      validEngine: true,
      validTemplate: true,
      formField: {},
      showRequired: true,
      batchOptions: [],
      streamOptions: [],
      templateOptions: [],
      filterStr:''
    };
    this.fetchData();
  }

  fetchData = () => {
    let promiseArr = [TopologyREST.getTopologyConfig(), EnvironmentREST.getAllNameSpaces()];
    if(this.props.topologyData){
      promiseArr.push(EngineREST.getAllTemplates(this.props.topologyData.topology.engineId));
    }
    Promise.all(promiseArr).then(result => {
      this.allEngineSettings = result[0];
      let stateObj = {};
      //setting all the engines from app_state
      stateObj.batchOptions = app_state.engines.filter((f)=>{return f.type == 'batch';});
      stateObj.streamOptions = app_state.engines.filter((f)=>{return f.type == 'stream';});

      if (this.allEngineSettings.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={this.allEngineSettings.responseMessage}/>, '', toastOpt);
      } else {
        if(this.props.topologyData){
          const engineData = this.getEngineDataById(this.props.topologyData.topology.engineId);
          const settings = this.findSettingsByEngineName(engineData.name);
          stateObj.formField = settings.topologyComponentUISpecification;
        }
        stateObj.formField = null;
      }

      if (result[1].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[1].responseMessage}/>, '', toastOpt);
      } else {
        const resultSet = result[1].entities;
        let namespaces = [];
        resultSet.map((e) => {
          namespaces.push(e.namespace);
        });
        this.setState({namespaceOptions: namespaces});
      }
      if(result[2]){
        if(result[2].responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={result[2].responseMessage}/>, '', toastOpt);
        } else {
          stateObj.templateOptions = result[2].entities;
        }
      }
      this.setState(stateObj);
    }).catch(err => {
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }

  validateName() {
    const {topologyName} = this.state;
    let validDataFlag = true;
    if (topologyName.trim().length < 1) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if (/[^A-Za-z0-9_\-\s]/g.test(topologyName)) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if (!/[A-Za-z0-9]/g.test(topologyName)) {
      validDataFlag = false;
      this.setState({validInput: false});
    // } else if(Utils.checkWhiteSpace(topologyName)){
    //   validDataFlag = false;
    //   this.setState({validInput: false});
    } else {
      validDataFlag = true;
      this.setState({validInput: true});
    }
    return validDataFlag;
  }
  validate() {
    const {topologyName, namespaceId, engineId, templateId} = this.state;
    let validDataFlag = true;
    if (!this.validateName()) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if(namespaceId === ''){
      validDataFlag = false;
      this.setState({validSelect: false});
    } else if(engineId === ''){
      validDataFlag = false;
      this.setState({validEngine: false});
    } else if(templateId === ''){
      validDataFlag = false;
      this.setState({validTemplate: false});
    } else {
      validDataFlag = true;
      this.setState({validInput: true, validSelect: true, validEngine: true, validTemplate: true});
    }
    return validDataFlag;
  }

  handleSave = (projectId) => {
    if (!this.validate()) {
      return;
    }
    const {topologyName, namespaceId, engineId, templateId} = this.state;
    const {topologyData} = this.props;
    let configData = this.refs.Form.state.FormData;
    let data = {
      name: topologyName,
      namespaceId: namespaceId,
      engineId: engineId,
      templateId: templateId,
      config: JSON.stringify(configData)
    };
    if(topologyData) {
      data.projectId = projectId;
      return TopologyREST.putTopology(topologyData.topology.id, topologyData.topology.versionId, {body: JSON.stringify(data)});
    } else {
      return TopologyREST.postTopology(projectId, {body: JSON.stringify(data)});
    }
  }
  saveMetadata = (id) => {
    let metaData = {
      topologyId: id,
      data: JSON.stringify({sources: [], processors: [], sinks: []})
    };
    return TopologyREST.postMetaInfo({body: JSON.stringify(metaData)});
  }
  handleOnChange = (e) => {
    this.setState({topologyName: e.target.value.trim()});
    this.validateName();
  }
  handleOnChangeEnvironment = (obj) => {
    if (obj) {
      this.setState({namespaceId: obj.id, validSelect: true});
    } else {
      this.setState({namespaceId: '', validSelect: false});
    }
  }
  handleOnChangeEngine = (obj) => {
    if (obj) {
      EngineREST.getAllTemplates(obj.id).then(templates=>{
        const engineData = this.getEngineDataById(obj.id);
        const settings = this.findSettingsByEngineName(engineData.name);
        this.setState({
          engineId: obj.id,
          validEngine: true,
          templateOptions: templates.entities,
          templateId: templates.entities[0].id,
          validTemplate: true,
          formField: settings.topologyComponentUISpecification
        });
      });
    } else {
      this.setState({
        engineId: '',
        validEngine: false,
        templateOptions: [],
        templateId: '',
        validTemplate: false,
        formField: null
      });
    }
  }
  getEngineDataById(id){
    return app_state.engines.find((e)=>{
      return e.id === id;
    });
  }
  findSettingsByEngineName(name){
    return this.allEngineSettings.entities.find((e)=>{
      return e.engine === name;
    });
  }
  handleOnChangeTemplate = (obj) => {
    if (obj) {
      this.setState({templateId: obj.id, validTemplate: true});
    } else {
      this.setState({templateId: '', validTemplate: false});
    }
  }
  handleDescriptionChange = (event) => {
    this.setState({description: event.target.value});
  }
  onFilterChange = (e) => {
    const val = e.target.value;
    this.setState({filterStr: val});
  }

  render() {
    const {
      formField,
      validInput,
      showRequired,
      topologyName,
      namespaceId,
      namespaceOptions,
      validSelect,
      engineId,
      batchOptions,
      streamOptions,
      validEngine,
      templateId,
      templateOptions,
      validTemplate,
      description,
      filterStr
    } = this.state;
    const formData = {};
    let fields = formField ? Utils.genFields(formField.fields || [], [], formData) : null;

    const filteredTemplates = _.filter(templateOptions, (t) => {
      return Utils.matchStr(t.name, filterStr) || Utils.matchStr(t.description, filterStr);
    });

    return (
      <div className="modal-form">
        <div className="form-group">
          <label data-stest="nameLabel">Name
            <span className="text-danger">*</span>
          </label>
          <div>
            <input type="text" ref={(ref) => this.nameRef = ref} name="topologyName" defaultValue={topologyName} placeholder="Workflow name" required="true" className={validInput
              ? "form-control"
              : "form-control invalidInput"} onKeyUp={this.handleOnChange} autoFocus="true" disabled={!!this.props.topologyData} />
          </div>
        </div>
        <div className="form-group">
          <label data-stest="projectDescriptionLabel">Description<span className="optional">(optional)</span>
          </label>
          <div>
            <textarea data-stest="description" type="text" value={description} className={"form-control"} onChange={this.handleDescriptionChange} placeholder="Description"/>
          </div>
        </div>
        <hr />
        <div className="form-group m-b-xs">
          <label data-stest="selectEnvLabel">Choose the Engine to run on
            <span className="text-danger">*</span>
          </label>
        </div>
        <div className="row m-b-xs">
          <label className="col-sm-2 engine-type">Batch:</label>
          <div className="col-sm-6">
            {_.map(batchOptions, (e) => {
              return <span className="radio-container" onClick={() => this.handleOnChangeEngine(e)} key={e.id}>
                <input type="radio" name="engine" checked={engineId == e.id}/>
                <label>{e.displayName}</label>
              </span>;
            })}
          </div>
        </div>
        <div className="row m-b-lg">
          <label className="col-sm-2 engine-type">Streaming:</label>
          <div className="col-sm-6">
            {_.map(streamOptions, (e) => {
              return <span className="radio-container" onClick={() => this.handleOnChangeEngine(e)} key={e.id}>
                <input type="radio" name="engine" checked={engineId == e.id}/>
                <label>{e.displayName}</label>
              </span>;
            })}
          </div>
        </div>
        <div className="form-group">
          <label data-stest="selectEnvLabel">Workflow Templates
            <span className="text-danger">*</span>
          </label>
          <FormGroup className="search-box">
            <InputGroup>
              <InputGroup.Addon>
                <i className="fa fa-search"></i>
              </InputGroup.Addon>
              <FormControl data-stest="searchBox" type="text" placeholder="Search..." onKeyUp={this.onFilterChange} className="" />
            </InputGroup>
          </FormGroup>
          <div className="row templates-container">{filteredTemplates.length ?
            _.map(filteredTemplates, (t) => {
              return <div className={`col-md-6 template-box ${templateId == t.id ? 'selected-template' : ''}`} key={t.name}>
                <span className="name">{t.name}</span>
                <span className="description">{t.description}</span>
              </div>;
            })
            :
            <div className="text-center">No template found!</div>
          }
          </div>
        </div>
        <hr />
        <div className="form-group">
          <label data-stest="selectEnvLabel">Choose the Regions
            <span className="text-danger">*</span>
          </label>
          <div className="m-t-xs">{
            _.map(namespaceOptions, (n) => {
              return <span className="radio-container" onClick={() => this.handleOnChangeEnvironment(n)} key={n.name}>
                <input type="radio" name="environment" checked={namespaceId == n.id}/>
                <label>{n.name}</label>
              </span>;
            })
          }
          {validSelect === false && <p className="text-danger m-t-xs">Please select region</p>}
          </div>
        </div>
        {fields ?
          <Form ref="Form" FormData={formData} className="hidden">
            {fields}
          </Form>
          :
          null
        }
      </div>
    );
  }
}

export default AddTopology;
