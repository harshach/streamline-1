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

import {DropdownButton, MenuItem, Button ,FormGroup,InputGroup,FormControl} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';

import BaseContainer from '../../../containers/BaseContainer';
import Utils from '../../../utils/Utils';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt, iconsFrom, menuName} from '../../../utils/Constants';
import Modal from '../../../components/FSModal';
import AddProject from './AddProject';
import NoData, {BeginNew} from '../../../components/NoData';
import ProjectREST from '../../../rest/ProjectREST';
import TopologyREST from '../../../rest/TopologyREST';
import CommonLoaderSign from '../../../components/CommonLoaderSign';
import app_state from '../../../app_state';
import moment from 'moment';

class ProjectCard extends Component {
  constructor(props){
    super(props);
  }
  onActionClick = (eventKey) => {
    const projectId =  this.projectRef.dataset.id;
    this.props.actionClick(eventKey, parseInt(projectId,10));
  }
  render(){
    let {data} = this.props;
    const ellipseIcon = <i className="fa fa-ellipsis-v"></i>;

    const applicationsGroup = _.groupBy(data.applicationEngines, (name) => {
      return name;
    });

    const engineCounts = data.applicationEngines.length > 0 ?
      <ul className="project-engines ">
        {_.map(applicationsGroup, (arr, engineName)=>{
          let name = '';
          const displayName = _.find(app_state.engines, (e) => {
            return e.name == engineName;
          }).displayName;
          switch(engineName.toLowerCase()){
          case 'athenax':
            name = 'athenax';
            break;
          case 'piper':
            name = 'piper';
            break;
          case 'storm':
            name = 'storm';
            break;
          }
          return <li className={name} key={name+data.id}>
            <span className="engine-name">{displayName}</span>
            <span className="badge">{arr.length}</span>
          </li>;
        })}
      </ul>
      :
      <ul className="project-engines ">
        <li className="no-workflow">
          <span className="engine-name">No Workflow</span>
          <span className="badge">0</span>
        </li>
      </ul>;

    return (
      <div className="col-md-4">
        <div className="service-box card" data-id={data.id} ref={(ref) => this.projectRef = ref}>
          {/*<div className="service-head clearfix">

            <div className="service-action-btn">
              <DropdownButton noCaret title={ellipseIcon} id="dropdown" bsStyle="link" className="dropdown-toggle" data-stest="project-actions">
                <MenuItem onClick={this.onActionClick.bind(this, "edit/")} data-stest="edit-project">
                  <i className="fa fa-pencil"></i>
                  &nbsp;Edit
                </MenuItem>
                <MenuItem onClick={this.onActionClick.bind(this, "delete/")} data-stest="delete-project">
                  <i className="fa fa-trash"></i>
                  &nbsp;Delete
                </MenuItem>
              </DropdownButton>
            </div>
          </div>*/}
          <div className="clearfix padding-24">
            {engineCounts}
            <h6 className="no-margin project-name">
              <Link
                to={
                  (Utils.isFromSharedProjects() ? `shared-projects/${data.id}/applications` : `projects/${data.id}/applications`)
                }
              >{data.name}</Link>
            </h6>
            <span className="display-block project-description">{data.description}</span>
            <span className="display-block project-timestamp">Last modified on {Utils.datetime(data.timestamp).value}</span>
          </div>
        </div>
      </div>
    );
  }
}

class ProjectListingContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      entities: [],
      filterValue: '',
      fetchLoader: true,
      editModeData: {}
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
  onFilterChange = (e) => {
    this.setState({filterValue: e.target.value.trim()});
  }

  handleAdd = () => {
    this.refs.addModal.show({
      btnOkText: 'Create'
    });
  }

  handleSave = () => {
    if (this.refs.addProject.validate()) {
      this.refs.addProject.handleSave().then((project) => {
        this.refs.addModal.hide();
        if (project.responseMessage !== undefined) {
          this.setState({fetchLoader: false});
          let errorMag = project.responseMessage.indexOf('already exists') !== -1
            ? "Project with the same name is already existing"
            : project.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          this.setState({
            fetchLoader: true
          }, () => {
            this.fetchData();
            FSReactToastr.success(
              <strong>Project added successfully</strong>
            );
          });
        }
      });
    }
  }

  handleDelete(id) {
    let BaseContainer = this.refs.BaseContainer;
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this project?'}).then((confirmBox) => {
      ProjectREST.deleteProject(id).then((project) => {
        this.fetchData();
        confirmBox.cancel();
        if (project.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={project.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Project deleted successfully</strong>
          );
        }
      });
    }, (Modal) => {});
  }

  handleKeyPress = (event) => {
    if (event.key === "Enter") {
      this.refs.addModal.state.show
        ? this.handleSave()
        : '';
    }
  }

  handleEdit(id, e) {
    ProjectREST.getProject(id).then((project) => {
      if (project.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={project.responseMessage}/>, '', toastOpt);
      } else {
        const data = {
          id: id,
          name: project.name,
          description: project.description
        };
        this.setState({
          editModeData: data
        }, () => {
          this.refs.addModal.show();
        });
      }
    });
  }

  projectActionClick = (eventKey, id,obj) => {
    const key = eventKey.split('/');
    switch (key[0].toString()) {
    case "edit":
      this.handleEdit(id);
      break;
    case "delete":
      this.handleDelete(id);
      break;
    default:
      break;
    }
  }

  getHeaderContent() {
    if(Utils.isFromSharedProjects()){
      return this.props.routes[this.props.routes.length - 1].name;
    } else {
      return [
        <Link to="/" className="header-link">Workflows</Link>,
        <Link to="/projects" className="header-link active">Projects</Link>
      ];
    }
  }

  render() {
    const {entities, filterValue, fetchLoader, editModeData} = this.state;
    const filteredEntities = Utils.filterByName(entities, filterValue);

    const components = filteredEntities.length == 0
    ? <NoData imgName={"default"} searchVal={filterValue}/>
    : filteredEntities.map((project, index)=>{
      return <ProjectCard key={index} data={project} actionClick={this.projectActionClick} />;
    });

    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.getHeaderContent()}>
        <div className="row">
          <div className="col-sm-12">
            <div className="page-title-box clearfix">
              <div className="search-container">
                {((filterValue && filteredEntities.length === 0) || filteredEntities.length !== 0) && entities.length
                  ? <FormGroup className="search-box">
                      <InputGroup>
                        <InputGroup.Addon>
                          <i className="fa fa-search"></i>
                        </InputGroup.Addon>
                        <FormControl data-stest="searchBox" type="text" placeholder="Search..." onKeyUp={this.onFilterChange} className="" />
                      </InputGroup>
                    </FormGroup>
                  : ''}
              </div>
              {entities.length !== 0 &&
              <div className="add-btn text-center">
                <a href="javascript:void(0);" className="success actionDropdown text-medium" data-target="#addEnvironment" onClick={this.handleAdd.bind(this)}>
                  <i className="fa fa-plus"></i> &ensp; New Project
                </a>
              </div>
              }
            </div>
          </div>
        </div>
        <div className="row">
          {fetchLoader
            ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"default"}/>]
            : entities.length ? components : <BeginNew type="Project" onClick={this.handleAdd.bind(this)}/>
          }
        </div>
        <Modal ref="addModal" className="u-form" data-title={`${editModeData.id
          ? "Edit"
          : "Create new"} project`} onKeyPress={this.handleKeyPress} data-resolve={this.handleSave.bind(this)}>
          <AddProject ref="addProject" editData={editModeData}/>
        </Modal>
      </BaseContainer>
    );
  }
}
ProjectListingContainer.contextTypes = {
  router: PropTypes.object.isRequired
};
export default ProjectListingContainer;
