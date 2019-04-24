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
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Link} from 'react-router';
import moment from 'moment';
import {
  DropdownButton,
  MenuItem,
  FormGroup,
  InputGroup,
  FormControl,
  Button
} from 'react-bootstrap';
import d3 from 'd3';
import {Table, Th, Td, Thead, Tr, unsafe} from 'reactable';
import SVGIcons from '../../../utils/SVGIcons';
/* import common utils*/
import TopologyREST from '../../../rest/TopologyREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import UserRoleREST from '../../../rest/UserRoleREST';
import MiscREST from '../../../rest/MiscREST';
import ProjectREST from '../../../rest/ProjectREST';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData, {BeginNew} from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt, accessCapabilities} from '../../../utils/Constants';
import Paginate from '../../../components/Paginate';
import Modal from '../../../components/FSModal';
import AddProject from '../ProjectListing/AddProject';
import AddWorkflowsToProject from '../ProjectListing/AddWorkflowsToProject';
import AddTopology from './AddTopology';
import ImportTopology from './ImportTopology';
import CloneTopology from './CloneTopology';
import CommonLoaderSign from '../../../components/CommonLoaderSign';
import app_state from '../../../app_state';
import {observer} from 'mobx-react';
import {hasEditCapability, hasViewCapability,findSingleAclObj,handleSecurePermission} from '../../../utils/ACLUtils';
import CommonShareModal from '../../../components/CommonShareModal';

@observer
class WorkflowListingTable extends Component {
  constructor(props) {
    super(props);
  }

  onActionClick = (eventKey,wId, pId) => {
    const {allACL} = this.props;
    const topologyId = wId;
    const projectId = pId;
    if(app_state.streamline_config.secureMode){
      let permissions = true,rights_share=true;
      const userInfo = app_state.user_profile !== undefined ? app_state.user_profile.admin : false;
      let aclObject = findSingleAclObj(Number(topologyId),allACL || []);
      if(!_.isEmpty(aclObject)){
        const {p_permission,r_share} = handleSecurePermission(aclObject,userInfo,"Applications");
        permissions = p_permission;
        rights_share = r_share;
      } else {
        aclObject = {objectId : topologyId, objectNamespace : "topology"};
        permissions = hasEditCapability("Applications");
      }

      if(permissions){
        if(eventKey.includes('share')){
          rights_share ? this.props.topologyAction(eventKey,topologyId,projectId,aclObject) : '';
        } else {
          this.props.topologyAction(eventKey,topologyId,projectId,aclObject);
        }
      }
    } else {
      this.props.topologyAction(eventKey,topologyId,projectId);
    }
  }
  viewMode = (Wid, projectId) => {
    this.context.router.push((Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+projectId+'/applications/' + Wid + '/view');
  }

  getTable(){
    const {data,allACL} = this.props;
    const userInfo = app_state.user_profile !== undefined ? app_state.user_profile.admin :false;
    let permission=true,aclObject={};
    if(app_state.streamline_config.secureMode){
      data.map((workflowObj)=> {
        aclObject = findSingleAclObj(workflowObj.id ,allACL || []);
        const {p_permission,r_share} = handleSecurePermission(aclObject,userInfo,"Applications");
        permission = p_permission;
        workflowObj.rights_share = r_share;
        workflowObj.showShareIcon = !_.isEmpty(aclObject) || userInfo;
      });
    }

    return (<div className="table u-form">
      <Table
        className="table no-margin table-workflow"
        currentPage={0}
        itemsPerPage={data.length > 20 ? 20 : 0}
        pageButtonLimit={5}
      >
        <Thead>
          <Th column="checkbox"><input type= "checkbox" checked={this.props.headerCheckbox} onChange={this.props.toggleHeaderCheckbox}></input></Th>
          <Th column="name">Name</Th>
          <Th column="type">Type</Th>
          <Th column="datacenterstatus">Data Center Status</Th>
          <Th column="version">Version</Th>
          <Th column="owner">Owner</Th>
          <Th column="actions">Actions</Th>
        </Thead>
        {data.map((workflowObj,i)=>{
          let engine = Utils.getEngineById(workflowObj.engineId);
          let datacenterArr=[];
          if(workflowObj.statusArr){
            workflowObj.statusArr.map((statusObj)=>{
              datacenterArr.push({
                clusterName: statusObj.namespaceName,
                status: statusObj.status,
                runtimeAppId: statusObj.runtimeAppId
              });
            });
          }

          return (
            <Tr key={i}>
              <Td column="checkbox" width="5%">{
                <input type="checkbox" checked={workflowObj.isSelected} onChange={this.props.handleCheckbox.bind(this, i)}></input>}
              </Td>
              <Td column="name">
                <span className= "workflow-name" onClick={this.viewMode.bind(this, workflowObj.id, workflowObj.projectId)}>{workflowObj.name}
                {datacenterArr[0]
                ?
                  datacenterArr[0].runtimeAppId ? "" : <span className="draft-label">Draft</span>
                : "" }
                <h6>{datacenterArr[0] ? datacenterArr[0].runtimeAppId : ""}</h6>
                </span>
              </Td>
              <Td column="type"><span>{engine.displayName} </span></Td>
              <Td column="datacenterstatus">
                {!workflowObj.statusArr ? <span>Loading Data</span> : this.dataCenterStatus(datacenterArr) }
              </Td>
              <Td column="version">{<span>Version 1.0 {/*<h6>Scheduling on mm/dd/yy</h6>*/}</span>}</Td>
              <Td column="owner">{<span>{workflowObj.config.properties['topology.owner'] || '---'} <h6>Last Modified on : {Utils.datetime(workflowObj.timestamp).value}</h6></span>}</Td>
              <Td column="actions">
                <span>
                  <a className="btn btn-link btn-xs" title="Edit" disabled={!permission} onClick={this.onActionClick.bind(this, "edit/" + workflowObj.id, workflowObj.id, workflowObj.projectId)}>
                    {SVGIcons.editIcon}</a>
                  { workflowObj.showShareIcon ?
                    <button
                      type="button"
                      className="btn btn-link btn-xs" onClick={this.onActionClick.bind(this, "share/" + workflowObj.id)}
                      disabled={!workflowObj.rights_share}
                    >
                      {SVGIcons.actionShareIcon}
                    </button>
                     :
                    null
                  }
                  <DropdownButton title={SVGIcons.actionEllipsis} noCaret bsStyle="link btn-xs" id={"lisitng-"+i} onClick={this.handleClick}>
                    <MenuItem title="Clone" disabled={!permission} onClick={this.onActionClick.bind(this, "clone/" + workflowObj.id, workflowObj.id, workflowObj.projectId)}>
                      <i className="fa fa-clone"></i>
                      &nbsp;Clone
                    </MenuItem>
                    <MenuItem title="Export" disabled={!permission} onClick={this.onActionClick.bind(this, "export/" + workflowObj.id, workflowObj.id, workflowObj.projectId)}>
                      <i className="fa fa-share-square-o"></i>
                      &nbsp;Export
                    </MenuItem>
                    <MenuItem title="Delete" disabled={!permission} onClick={this.onActionClick.bind(this, "delete/" + workflowObj.id, workflowObj.id, workflowObj.projectId)}>
                      <i className="fa fa-trash"></i>
                      &nbsp;Delete
                    </MenuItem>
                  </DropdownButton>
                </span>
              </Td>
            </Tr>
          );
        })}
      </Table>
    </div>);
  }

  dataCenterStatus(statusArr){
    let html = '';
    statusArr.map((statusObj, x)=>{
      html+= '<span class=datacenter key={'+x+'_stats}><div class="execution-box ' + this.getStatusBox(statusObj) + '"></div>' + statusObj.clusterName + '</span>';
    });
    return unsafe(html);
  }

  getStatusBox(status){
    switch(status.status.toLowerCase()){
    case 'not deployed':
    case 'unknown':
      return 'unknown';
      break;
    case 'enabled':
    case 'active':
      return 'done';
      break;
    case 'running':
      return 'running';
      break;
    case 'failed':
      return 'failed';
      break;
    case 'paused':
      return 'paused';
      break;
    default:
      return '';
      break;
    }
  }

  render() {
    const {topologyAction, topologyList,allACL, data} = this.props;
    return (
        <div className="col-sm-12">
          <div className = "workflow-widget">
            {this.getTable()}
          </div>
      </div>
    );
  }
}

@observer
class TopologyFooter extends Component{
  constructor(props){
    super(props);
    this.state = {
      folderClicked: false
    };
  }
  handleClick = () => {
    this.setState({folderClicked: !this.state.folderClicked});
  }
  render(){
    let {folderClicked} = this.state;
    let folderIcon= SVGIcons.folder(folderClicked);
    const folder = <DropdownButton title={folderIcon} noCaret bsStyle="link workflow-dropdown" onToggle={this.handleClick.bind(this)} dropup id="worflow-list-footer" onClick={this.handleClick}>
                    <MenuItem title="Create New project" onClick={this.props.newProject}>
                      Create New Project
                      </MenuItem>
                    <MenuItem title="Add Project" onClick={this.props.addtoProject}>
                      Add to Project
                      </MenuItem>
                  </DropdownButton>;

    const {selectedWorkflowArr} =this.props;
    return(
      <div className={`selected-workflow-panel animated ${selectedWorkflowArr.length ? "fadeInUp": "fadeOutDown"}`}>
        <div>
          <span className="u-form"><input type="checkbox" checked readOnly></input> {selectedWorkflowArr.length ==1 ?  "1 Workflow selected" : selectedWorkflowArr.length + " Workflows selected"}</span>
            {folder}
          <a className="btn btn-link" title="Delete" onClick={this.props.footerAction}>
            {SVGIcons.trashIcon}
          </a>
          {/*<a className="btn btn-link" title="share">
            {SVGIcons.shareIcon}
          </a>*/}
        </div>
      </div>
    );
  }
}

WorkflowListingTable.propTypes = {
  data: PropTypes.array.isRequired,
  topologyAction: PropTypes.func.isRequired
};

WorkflowListingTable.contextTypes = {
  router: PropTypes.object.isRequired
};

@observer
class TopologyListingContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      entities: [],
      filterValue: '',
      slideInput: false,
      sorted: {
        key: 'last_updated',
        text: 'Last Updated'
      },
      refIdArr: [],
      fetchLoader: true,
      pageIndex: 0,
      pageSize: 9,
      cloneFromId: null,
      topologyData: null,
      checkEnvironment: false,
      sourceCheck: false,
      searchLoader : false,
      allACL : [],
      shareObj : {},
      selectedCheckboxArr: [],
      isHeaderCheckbox : false , editModeData : {}
    };
    this.projectId = props.params.projectId;

    this.fetchData();
  }

  componentWillReceiveProps(newProps){
    if(newProps.params.projectId !== this.props.params.projectId){
      this.projectId = newProps.params.projectId;
      this.fetchData();
    }
  }

  fetchData() {
    const sortKey = this.state.sorted.key;
    const projectId = this.projectId;
    let promiseArr = [
      EnvironmentREST.getAllNameSpaces(),
      TopologyREST.getSourceComponent()
    ];
    if(projectId){
      promiseArr.push(TopologyREST.getAllTopologyWithoutConfig(projectId, sortKey));
      promiseArr.push(ProjectREST.getProject(projectId));
    } else {
      promiseArr.push(TopologyREST.getAllAvailableTopologies(sortKey));
    }
    if(app_state.streamline_config.secureMode){
      promiseArr.push(UserRoleREST.getAllACL('topology',app_state.user_profile.id,'USER'));
    }
    Promise.all(promiseArr).then((results) => {
      let environmentLen = 0,
        environmentFlag = false,
        sourceLen = 0,
        sourceFlag = false;
      _.map(results, (result) => {
        if(result.responseMessage !== undefined){
          this.setState({fetchLoader: false, checkEnvironment: false, sourceCheck: false});
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        }
      });
      let stateObj = {};
      // environment results[0]
      environmentLen = results[0].entities.length;
      stateObj.namespacesArr = results[0].entities;

      // source component results[1]
      sourceLen = results[1].entities.length;

      // All topology results[2]
      let resultEntities = Utils.sortArray(results[2].entities.slice(), 'timestamp', false);
      this.syncDatacenterStatus(resultEntities);
      if (sourceLen !== 0) {
        if (resultEntities.length === 0 && environmentLen > 1) {
          environmentFlag = true;
        }
      } else {
        sourceFlag = true;
      }

      stateObj.fetchLoader = false;
      stateObj.entities = resultEntities;
      stateObj.pageIndex = 0;
      stateObj.checkEnvironment = environmentFlag;
      stateObj.sourceCheck = sourceFlag ;
      stateObj.searchLoader = false;

      if(projectId){
        stateObj.projectData = results[3];
        // If the application is in secure mode result[4]
        if(results[4]){
          stateObj.allACL = results[4].entities;
        }
      } else {
        stateObj.projectData = null;
        // If the application is in secure mode result[3]
        if(results[3]){
          stateObj.allACL = results[3].entities;
        }
      }

      this.setState(stateObj);
    });
  }

  onFilterChange = (e) => {
    this.setState({filterValue: e.target.value.trim(), searchLoader: true}, () => {
      this.getFilteredEntities();
    });
  }

  getFilteredEntities = () => {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      const {filterValue,sorted} = this.state;
      this.setState({searchLoader: true}, () => {
        if(filterValue !== ''){
          MiscREST.searchEntities('topology', filterValue,sorted.key).then((topology)=>{
            if (topology.responseMessage !== undefined) {
              FSReactToastr.error(
                <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
              this.setState({searchLoader: false});
            } else {
              let results = topology.entities;
              this.syncDatacenterStatus(results);
              this.setState({searchLoader: false, entities: results, pageIndex: 0});
            }
          });
        } else {
          this.fetchData();
        }
      });
    }, 500);
  }

  syncDatacenterStatus = (workflowArr) => {
    let {selectedCheckboxArr} = this.state;
    let statusPromiseArr = [];
    let allSelected = true;
    workflowArr.map((obj)=>{
      if(selectedCheckboxArr.includes(obj.id)){
        obj.isSelected = true;
      } else {
        allSelected = false;
        obj.isSelected = false;
      }
      statusPromiseArr.push(TopologyREST.getTopologyActionStatus(obj.id,obj.versionId));
    });
    Promise.all(statusPromiseArr).then((statusResults) => {
      workflowArr.map((obj,index)=>{
        obj.statusArr = statusResults[index].entities;
      });
      this.setState({entities: workflowArr, isHeaderCheckbox: allSelected});
    });
  }

  handleAddTopology() {
    this.AddTopologyModelRef.show({
      btnOkText: 'Create'
    });
  }

  handleImportTopology() {
    this.ImportTopologyModelRef.show();
  }

  deleteSingleTopology = (id) => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete?'}).then((confirmBox) => {
      this.setState({fetchLoader: true});
      TopologyREST.deleteTopology(id).then((topology) => {
        // TopologyREST.deleteMetaInfo(id);
        this.fetchData();
        if (topology.responseMessage !== undefined) {
          this.setState({fetchLoader: false});
          FSReactToastr.error(
            <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Workflow deleted successfully</strong>
          );
        }
      });
      confirmBox.cancel();
    }, () => {});
  }

  cloneTopologyAction = (id) => {
    this.setState({
      cloneFromId: id
    }, () => {
      this.CloneTopologyModelRef.show();
    });
  }

  exportTopologyAction = (id) => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to export the topology?'}).then((confirmBox) => {
      TopologyREST.getExportTopology(id).then((exportTopology) => {
        if (exportTopology.responseMessage !== undefined) {
          let errorMag = exportTopology.responseMessage.indexOf('NoSuchElementException') !== -1
            ? "There might be some unconfigure Nodes. so please configure it first."
            : exportTopology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          this.exportTopologyDownload(id);
        }
      });
      confirmBox.cancel();
    }, () => {});
  }

  exportTopologyDownload = (id) => {
    this.refs.ExportTopology.href = TopologyREST.getExportTopologyURL(id);
    this.refs.ExportTopology.click();
    this.refs.BaseContainer.refs.Confirm.cancel();
  }

  actionHandler = (eventKey, id,projectId,obj) => {
    const key = eventKey.split('/');
    switch (key[0].toString()) {
    case "clone":
      this.cloneTopologyAction(id);
      break;
    case "export":
      this.exportTopologyAction(id);
      break;
    case "delete":
      this.deleteSingleTopology(id);
      break;
    case "share":
      this.shareSingleTopology(id,projectId,obj);
      break;
    case "edit":
      this.editSingleTopology(id,projectId);
      break;
    }
  }

  editSingleTopology =(id,projectId) => {
    this.context.router.push(
      (Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+projectId+'/applications/' + id + '/edit'
    );
  }

  shareSingleTopology = (id,projectId,obj) => {
    this.setState({shareObj : obj}, () => {
      this.refs.CommonShareModalRef.show();
    });
  }

  slideInput = (e) => {
    this.setState({slideInput: true});
    const input = document.querySelector('.inputAnimateIn');
    input.focus();
  }
  slideInputOut = () => {
    const input = document.querySelector('.inputAnimateIn');
    (_.isEmpty(input.value))
      ? this.setState({slideInput: false})
      : '';
  }

  onSortByClicked = (eventKey, el) => {
    const liList = el.target.parentElement.parentElement.children;
    for (let i = 0; i < liList.length; i++) {
      liList[i].setAttribute('class', '');
    }
    el.target.parentElement.setAttribute("class", "active");
    const sortKey = (eventKey.toString() === "name")
      ? "name"
      : eventKey;
    this.setState({searchLoader: true});
    const {filterValue} = this.state;

    MiscREST.searchEntities('topology', filterValue,sortKey).then((topology)=>{
      if (topology.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
        this.setState({searchLoader: false});
      } else {
        const sortObj = {
          key: eventKey,
          text: Utils.sortByKey(eventKey)
        };
        this.setState({searchLoader: false, entities: topology.entities, sorted: sortObj});
      }
    });
  }

  onActionMenuClicked = (eventKey) => {
    switch (eventKey.toString()) {
    case "create":
      this.handleAddTopology();
      break;
    case "import":
      this.handleImportTopology();
      break;
    }
  }

  pagePosition = (index) => {
    this.setState({
      pageIndex: index || 0
    });
  }
  handleSaveClicked = () => {
    const {projectId} = this.props.params;
    const newProjectId = projectId ? projectId : -1;
    if (this.addTopologyRef.validate()) {
      this.addTopologyRef.handleSave(newProjectId).then((topology) => {
        if (topology.responseMessage !== undefined) {
          let errorMag = topology.responseMessage.indexOf('already exists') !== -1
            ? "Workflow with same name already exists. Please choose a unique Workflow Name"
            : topology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          if(this.state.topologyData) { // Updated namespace
            FSReactToastr.success(
                <strong>Workflow's environment updated successfully</strong>
              );
            this.context.router.push(
              (Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+newProjectId+'/applications/' + topology.id + '/edit'
            );
          } else {
            this.addTopologyRef.saveMetadata(topology.id).then(() => {
              FSReactToastr.success(
                <strong>Workflow added successfully</strong>
              );
              this.context.router.push(
                (Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+newProjectId+'/applications/' + topology.id + '/edit'
              );
            });
          }
        }
      });
    }
  }

  handleImportSave = () => {
    const {projectId} = this.props.params;
    if (this.importTopologyRef.validate()) {
      this.importTopologyRef.handleSave().then((topology) => {
        if (topology.responseMessage !== undefined) {
          let errorMag = topology.responseMessage.indexOf('already exists') !== -1
            ? "Workflow with same name already exists. Please choose a unique Workflow Name"
            : topology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Workflow imported successfully</strong>
          );
          this.context.router.push(
            (Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+projectId+'/applications/' + topology.id + '/edit'
          );
        }
      });
    }
  }

  handleCloneSave = () => {
    const {projectId} = this.props.params;
    if (this.cloneTopologyRef.validate()) {
      this.cloneTopologyRef.handleSave().then((topology) => {
        if (topology.responseMessage !== undefined) {
          let errorMag = topology.responseMessage.indexOf('NoSuchElementException') !== -1
            ? "There might be some unconfigure Nodes. so please configure it first."
            : topology.responseMessage.indexOf('already exists') !== -1
              ? "Workflow with same name already exists. Please choose a unique Workflow Name"
              : topology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Workflow cloned successfully</strong>
          );
          this.context.router.push(
            (Utils.isFromSharedProjects() ? 'shared-projects/' : 'projects/')+projectId+'/applications/' + topology.id + '/edit'
          );
        }
      });
    }
  }

  handleKeyPress = (event) => {
    if (event.key === "Enter") {
      this.AddTopologyModelRef.state.show
        ? this.handleSaveClicked()
        : '';
      this.ImportTopologyModelRef.state.show
        ? this.handleImportSave()
        : '';
      this.CloneTopologyModelRef.state.show
        ? this.handleCloneSave()
        : '';
    }
  }

  handleShareSave = () => {
    this.refs.CommonShareModalRef.hide();
    this.refs.CommonShareModal.handleSave().then((shareTopology) => {
      let flag = true;
      _.map(shareTopology, (share) => {
        if(share.responseMessage !== undefined){
          flag = false;
          FSReactToastr.error(
            <CommonNotification flag="error" content={share.responseMessage}/>, '', toastOpt);
        }
        this.setState({shareObj : {}});
      });
      if(flag){
        shareTopology.length !== 0
        ? FSReactToastr.success(
            <strong>Workflow has been shared successfully</strong>
          )
        : '';
      }
    });
  }

  handleShareCancel = () => {
    this.refs.CommonShareModalRef.hide();
  }

  getHeaderContent() {
    const {projectData} = this.state;
    if(projectData){
      return (
        <span>
          {Utils.isFromSharedProjects() ?
            <Link to="/shared-projects">Shared Projects</Link>
            :
            <Link to="/">My Projects</Link>
          }
          <i className="fa fa-angle-right title-separator"></i>
          {projectData.name}
        </span>
      );
    } else {
      return [<Link to="/" className="header-link active">Workflows</Link>,<Link to="/projects" className="header-link">Projects</Link>];
    }
  }

  handleCheckbox = (index, event) => {
    let {entities, selectedCheckboxArr, isHeaderCheckbox} = this.state;
    if(event.target.checked){
      entities[index].isSelected = true;
      selectedCheckboxArr.push({ "WorkflowID" :entities[index].id});
    } else {
      entities[index].isSelected = false;
      selectedCheckboxArr.splice(selectedCheckboxArr.indexOf(entities[index]),1);
    }
    if(entities.length === selectedCheckboxArr.length){
      isHeaderCheckbox = true;
    } else if(isHeaderCheckbox && selectedCheckboxArr.length < entities.length){
      isHeaderCheckbox = false;
    } else if(selectedCheckboxArr.length == 0){
      isHeaderCheckbox = false;
    }
    this.setState({selectedCheckboxArr, entities, isHeaderCheckbox});
  }

  toggleHeaderCheckbox = (event) => {
    let {entities, isHeaderCheckbox} = this.state;
    let arr = [];
    entities.map((workflowObj)=>{
      if(event.target.checked){
        isHeaderCheckbox =true;
        workflowObj.isSelected = true;
        arr.push(workflowObj.id);
      } else{
        isHeaderCheckbox = false;
        workflowObj.isSelected = false;
      }
    });
    this.setState({selectedCheckboxArr : arr, entities: entities, isHeaderCheckbox});
  }

  deleteAction = () => {
    let {selectedCheckboxArr} = this.state;
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete?'}).then((confirmBox) => {
      let deleteArr = selectedCheckboxArr;
      this.setState({fetchLoader: true, selectedCheckboxArr : []});
      let PromiseArr =[];
      deleteArr.map((id)=>{
        PromiseArr.push(TopologyREST.deleteTopology(id.WorkflowID));
      });

      Promise.all(PromiseArr).then((results) =>{
        results.map((result) => {
          if(result.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          } else {
            FSReactToastr.success(
              <strong>Workflow deleted successfully</strong>
            );
          }
        });
        this.fetchData();
      });
      confirmBox.cancel();
    }, () => {});
  }

  handleCreate = () => {
    this.refs.addModal.show({
      btnOkText: 'Create'
    });
  }

  handleCreateProjectSave = () => {
    if (this.refs.createProject.validate()) {
      this.refs.createProject.handleSave().then((project) => {
        this.refs.addModal.hide();
        if (project.responseMessage !== undefined) {
          this.setState({fetchLoader: false});
          let errorMag = project.responseMessage.indexOf('already exists') !== -1
            ? "Project with the same name is already existing"
            : project.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          this.updateWorkflowProjects(project.id);
        }
      });
    }
  }
  addtoProject =() => {
    this.refs.addToProjectsModal.show({
      btnOkText: 'Add'
    });
  }

  handleAddToProjectSave = () => {
    let pid = this.refs.addProject.handleSave();
    this.refs.addToProjectsModal.hide();
    this.updateWorkflowProjects(pid);
  }

  updateWorkflowProjects = (projectId) => {
    let {selectedCheckboxArr,entities} = this.state;

    let promiseArr = [];
    selectedCheckboxArr.map((obj,i)=>{
      let workflowObj = entities.find((wObj) => {return wObj.id === obj.WorkflowID;});
      if(workflowObj){
        workflowObj.projectId = projectId;
        delete workflowObj.isSelected;
        delete workflowObj.statusArr;
        promiseArr.push(TopologyREST.putTopology(workflowObj.id, workflowObj.versionId, {body: JSON.stringify(workflowObj)}) );
      }
    });

    Promise.all(promiseArr).then((results) => {
      if(results){
        this.setState({
          fetchLoader: true, selectedCheckboxArr: []
        }, () => {
          this.fetchData();
          FSReactToastr.success(
            <strong>Selected workflow added successfully</strong>
          );
        });
      }
    });
  }

  render() {
    const {
      entities,
      filterValue,
      fetchLoader,
      slideInput,
      pageSize,
      pageIndex,
      checkEnvironment,
      sourceCheck,
      refIdArr,
      searchLoader,
      allACL,
      shareObj,
      topologyData,
      namespacesArr,
      selectedCheckboxArr,
      isHeaderCheckbox
    } = this.state;
    const splitData = _.chunk(entities, pageSize) || [];
    const btnIcon = <span><i className="fa fa-plus"></i> &ensp; New Workflow</span>;
    const sortTitle = <span>Sort:<span style={{
      color: "#006ea0"
    }}>&nbsp;{this.state.sorted.text}</span>
    </span>;

    const components = (splitData.length === 0)
    ? <NoData imgName={"default"} searchVal={filterValue} userRoles={app_state.user_profile}/>
    : <WorkflowListingTable data ={entities} toggleHeaderCheckbox={this.toggleHeaderCheckbox}
        handleCheckbox={this.handleCheckbox} projectId={this.projectId}
        topologyAction={this.actionHandler} refIdArr={refIdArr} allACL={allACL}
        headerCheckbox={isHeaderCheckbox}
      />;

    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.getHeaderContent()}>
        {!fetchLoader
          ? ((filterValue && splitData.length === 0) || splitData.length !== 0)
            ? <div className="row">
                <div className="page-title-box clearfix">
                  <div className="search-container text-right">
                    <FormGroup className="search-box">
                      <InputGroup>
                        <InputGroup.Addon>
                          <i className="fa fa-search"></i>
                        </InputGroup.Addon>
                        <FormControl data-stest="searchBox" type="text" placeholder="Search by name" onKeyUp={this.onFilterChange} className="" />
                      </InputGroup>
                    </FormGroup>
                  </div>
                  {hasEditCapability(accessCapabilities.APPLICATION) && (entities.length || filterValue) ?
                    <div className="add-btn text-center">
                      <DropdownButton title={btnIcon} id="actionDropdown" className="actionDropdown success text-medium" noCaret>
                        <MenuItem onClick={this.onActionMenuClicked.bind(this, "create")}>
                          &nbsp;New Workflow
                        </MenuItem>
                        <MenuItem onClick={this.onActionMenuClicked.bind(this, "import")}>
                          &nbsp;Import Workflow
                        </MenuItem>
                      </DropdownButton>
                    </div>
                    : null
                  }
                </div>
              </div>
            : null
          : null
        }
        {entities.length ?
          <h4 className="m-b-lg workflowCount">{entities.length == 1 ? "1 Workflow" : entities.length + " Workflows"}</h4>
        : null}
        <div className="row">
          {(fetchLoader || searchLoader)
            ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"applications"}/>]
            : filterValue || entities.length ? components : <BeginNew type="Workflow" onClick={this.onActionMenuClicked.bind(this, "create")}/>
          }
        </div>

        <TopologyFooter selectedWorkflowArr={selectedCheckboxArr} footerAction={this.deleteAction} newProject={this.handleCreate.bind()} addtoProject={this.addtoProject.bind()}></TopologyFooter>

        <Modal className="u-form" ref={(ref) => this.AddTopologyModelRef = ref} data-title={topologyData ? "Update Engine" : "Add Workflow"} onKeyPress={this.handleKeyPress} data-resolve={this.handleSaveClicked} data-reject={()=>{this.setState({topologyData: null});this.AddTopologyModelRef.hide();}}>
          <AddTopology ref={(ref) => this.addTopologyRef = ref} topologyData={topologyData} namespacesArr={namespacesArr}/>
        </Modal>
        <Modal className="u-form" ref={(ref) => this.ImportTopologyModelRef = ref} data-title="Import Workflow" onKeyPress={this.handleKeyPress} data-resolve={this.handleImportSave}>
          <ImportTopology
            ref={(ref) => this.importTopologyRef = ref}
            defaultProjectId={this.projectId}/>
        </Modal>
        <Modal className="u-form" ref={(ref) => this.CloneTopologyModelRef = ref} data-title="Clone Workflow" onKeyPress={this.handleKeyPress} data-resolve={this.handleCloneSave}>
          <CloneTopology
            topologyId={this.state.cloneFromId}
            ref={(ref) => this.cloneTopologyRef = ref}
            defaultProjectId={this.projectId}/>
        </Modal>
        {/* CommonShareModal */}
        <Modal className="u-form" ref={"CommonShareModalRef"} data-title="Share Workflow"  data-resolve={this.handleShareSave.bind(this)} data-reject={this.handleShareCancel.bind(this)}>
          <CommonShareModal ref="CommonShareModal" shareObj={shareObj}/>
        </Modal>
        <a className="btn-download" ref="ExportTopology" hidden download href=""></a>

        <Modal ref="addModal" className="u-form" data-title="Create new project"
          data-resolve={this.handleCreateProjectSave.bind(this)}>
          <AddProject ref="createProject" editData={this.state.editModeData}/>
        </Modal>

        <Modal ref="addToProjectsModal" className="u-form" data-title="Add to Project"
         data-resolve={this.handleAddToProjectSave.bind(this)}>
          <AddWorkflowsToProject ref="addProject"></AddWorkflowsToProject>
        </Modal>
      </BaseContainer>
    );
  }
}

TopologyListingContainer.contextTypes = {
  router: PropTypes.object.isRequired
};

export default TopologyListingContainer;

TopologyListingContainer.defaultProps = {};
