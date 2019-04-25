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
import FSReactToastr from '../../../components/FSReactToastr';
import Utils from '../../../utils/Utils';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import ProjectREST from '../../../rest/ProjectREST';
import {Select2 as Select} from '../../../utils/SelectUtils';

class AddWorkflowsToProject extends Component {
  constructor(props) {
    super(props);
    this.state = {
      entities : [],SelectedProjectId: 1,
      fetchLoader : true
    };
    this.fetchData();
  }

  fetchData = () => {
    ProjectREST.getMyProjects().then((projects) => {
      if (projects.responseMessage !== undefined) {
        this.setState({fetchLoader: false});
        FSReactToastr.error(
        <CommonNotification flag="error" content={projects.responseMessage}/>, '', toastOpt);
      } else {
        this.setState({entities : projects.entities});
      }
    });
  }
  handleChange=(selectedOption)=>{
    let value = selectedOption.id;
    this.setState({SelectedProjectId : value});
  }

  handleSave = ()=>{
    return this.state.SelectedProjectId;
  }
  render() {
    let {entities, SelectedProjectId} = this.state;
    return (
      <div className="modal-form">
        <div className="form-group">
          <label data-stest="projectNameLabel">Select Project
          </label>
          <Select labelKey="name" valueKey="id" value={SelectedProjectId} options={entities} onChange={this.handleChange}>
          </Select>
        </div>
      </div>
    );
  }
}

export default AddWorkflowsToProject;
