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
import {Link} from 'react-router';
import PropTypes from 'prop-types';
import _ from 'lodash';

import FSReactToastr from '../../../components/FSReactToastr';

import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt, iconsFrom, menuName} from '../../../utils/Constants';
import ProjectREST from '../../../rest/ProjectREST';
import TopologyREST from '../../../rest/TopologyREST';
import app_state from '../../../app_state';
import ProjectListingContainer from './ProjectListingContainer';


class SharedProjectListingContainer extends ProjectListingContainer {
  constructor(props) {
    super(props);
  }
  fetchData = () => {
    ProjectREST.getSharedProjects().then((projects) => {
      if (projects.responseMessage !== undefined) {
        this.setState({fetchLoader: false});
        FSReactToastr.error(
          <CommonNotification flag="error" content={projects.responseMessage}/>, '', toastOpt);
      } else {
        let data = projects.entities;
        let promiseArr = [];
        data.map((p)=>{
          p.applicationEngines = [];
          promiseArr.push(TopologyREST.getAllTopologyWithoutConfig(p.id));
        });
        Promise.all(promiseArr).then((results)=>{
          results.map((result, index)=>{
            result.entities.map((topology)=>{
              let engineObj = app_state.engines.find((e)=>{return e.id === topology.engineId;});
              data[index].applicationEngines.push(engineObj.name);
            });
          });
          this.setState({entities: data, fetchLoader: false});
        });
      }
    }).catch((err) => {
      this.setState({fetchLoader: false});
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }
}
SharedProjectListingContainer.contextTypes = {
  router: PropTypes.object.isRequired
};
export default SharedProjectListingContainer;
