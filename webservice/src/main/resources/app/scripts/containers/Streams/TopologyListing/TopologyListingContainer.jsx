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
import {toastOpt, PieChartColor, accessCapabilities} from '../../../utils/Constants';
import PieChart from '../../../components/PieChart';
import Paginate from '../../../components/Paginate';
import Modal from '../../../components/FSModal';
import AddTopology from './AddTopology';
import ImportTopology from './ImportTopology';
import CloneTopology from './CloneTopology';
import CommonLoaderSign from '../../../components/CommonLoaderSign';
import app_state from '../../../app_state';
import {observer} from 'mobx-react';
import {hasEditCapability, hasViewCapability,findSingleAclObj,handleSecurePermission} from '../../../utils/ACLUtils';
import CommonShareModal from '../../../components/CommonShareModal';

class CustPieChart extends PieChart {
  drawPie() {
    super.drawPie();

    this.svg.selectAll('title').remove();

    this.svg.selectAll('.pie-latency').remove();

    this.container.append('text').attr({class: 'pie-latency', y: -15, 'text-anchor': 'middle', 'font-size': "9", fill: "#888e99"}).text('LATENCY');

    const text = this.container.append('text').attr({class: 'pie-latency', 'text-anchor': 'middle'});
    const latencyDefaultTxt = Utils.secToMinConverter(this.props.latency, "graph").split('/');
    const tspan = text.append('tspan').attr({'font-size': "28", 'fill': "#323133", y: 20}).text(latencyDefaultTxt[0]);

    const secText = text.append('tspan').attr({fill: "#6d6f72", "font-size": 10}).text(' ' + latencyDefaultTxt[1]);

    if (!this.props.empty) {
      this.container.selectAll('path').on('mouseenter', (d) => {
        const val = Utils.secToMinConverter(d.value, "graph").split('/');
        tspan.text(val[0]);
        secText.text(val[1]);
      }).on('mouseleave', (d) => {
        tspan.text(latencyDefaultTxt[0]);
        secText.text(' ' + latencyDefaultTxt[1]);
      });
    }
  }
}

@observer
class TopologyItems extends Component {
  constructor(props) {
    super(props);
  }

  onActionClick = (eventKey) => {
    const {allACL} = this.props;
    const topologyId =  this.streamRef.dataset.id;
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
      // permission true only for refresh
      eventKey.includes('refresh') ? permissions = true : '';

      if(permissions){
        if(eventKey.includes('share')){
          rights_share ? this.props.topologyAction(eventKey,topologyId,aclObject) : '';
        } else {
          this.props.topologyAction(eventKey,topologyId,aclObject);
        }
      }
    } else {
      this.props.topologyAction(eventKey,topologyId);
    }
  }
  streamBoxClick = (id, event) => {
    const {allACL, projectId} = this.props;
    // check whether the element of streamBox is click..
    if ((event.target.nodeName !== 'BUTTON' && event.target.nodeName !== 'I' && event.target.nodeName !== 'A')) {
      this.context.router.push('projects/'+projectId+'/applications/' + id + '/view');
    } else if (event.target.title === "Edit") {
      if(app_state.streamline_config.secureMode){
        let permissions = true;
        const userInfo = app_state.user_profile !== undefined ? app_state.user_profile.admin : false;
        const aclObject = findSingleAclObj(Number(id),allACL || []);
        if(!_.isEmpty(aclObject)){
          const {p_permission} = handleSecurePermission(aclObject,userInfo,"Applications");
          permissions = p_permission;
        } else {
          permissions = hasEditCapability("Applications");
        }
        if(permissions){
          this.context.router.push('projects/'+projectId+'/applications/' + id + '/edit');
        }

      } else {
        this.context.router.push('projects/'+projectId+'/applications/' + id + '/edit');
      }
    }
  }
  checkRefId = (id) => {
    const index = this.props.refIdArr.findIndex((x) => {
      return x === id;
    });
    return index !== -1
      ? true
      : false;
  }

  getMetrics(){
    const {topologyList} = this.props;
    const {
      topology,
      runtime = {},
      namespaceName
    } = topologyList;
    const {metric, latencyTopN} = runtime;
    const metricWrap = metric ? metric.metrics : {};

    const engine = Utils.getEngineById(topologyList.topology.engineId);
    const template = Utils.getListingMetricsTemplate(engine);
    const layout = engine.metricsTemplate.metricsUISpec.layout.listing;

    const getMetric = (name) => {
      return _.find(engine.metricsTemplate.metricsUISpec.metrics, (m) => {
        return m.name == name;
      });
    };

    const designs = {
      title: (metricName) => {
        return <span className="metric-title">{topologyList.namespaceName}</span>;
      },
      status: (metricName) => {
        const metric = getMetric(metricName);
        return <span className="metric-status">{metricWrap[metric.metricKeyName]} &nbsp;</span>;
      },
      labelValue: (metricName) => {
        const metric = getMetric(metricName);
        const val = Utils[metric.valueFormat](metricWrap[metric.metricKeyName]);
        return [
          <span className="metric-label">{metric.uiName}</span>,
          <span className="metric-value">{val.value} &nbsp;</span>
        ];
      },
      duration: (metricName) => {
        const metric = getMetric(metricName);
        const oVal = metricWrap[metric.metricKeyName] || 0;
        const val = Utils[metric.valueFormat](oVal);
        return <span className="metric-duration">Duration {val.value + (val.prefix || '')}</span>;
      },
      legendValue: (metricName) => {
        const metric = getMetric(metricName);
        return <span className="metric-legend-status">{metricWrap[metric.metricKeyName]}</span>;
      }
    };

    const metrics = [];

    _.each(layout, (row, index) => {
      const left = _.map(row.left, (m) => {
        return designs[m.type](m.name);
      });
      const right = _.map(row.right, (m) => {
        return designs[m.type](m.name);
      });
      metrics.push(<div className="metric-row">
        <div className="metric-left">{left}</div>
        <div className="metric-right text-right">{right}</div>
      </div>);
    });

    return <div className="metric-container">{metrics}</div>;
  }
  getFooter(){
    const {topologyList} = this.props;
    const {
      topology,
      runtime = {},
      namespaceName
    } = topologyList;
    const {metric, latencyTopN} = runtime;
    const metricWrap = metric ? metric.metrics : {};

    return <div className="card-footer">
      <div className="display-table">
        <div className="metric-left">
          <span className="app-name">{topologyList.topology.name}</span>
          <span className="app-last-update-label">Last Updated on</span>
          <span className="app-last-update-value">{Utils.datetime(topologyList.topology.timestamp).value}</span>
        </div>
        <div className="metric-right text-right">
          <span className="app-run-type">{topologyList.running}</span>
          {/*<span className="app-run-duration">Run every 1 day at 11:00:00</span>*/}
        </div>
      </div>
    </div>;
  }

  render() {
    const {topologyAction, topologyList,allACL} = this.props;
    const {
      topology,
      runtime = {},
      namespaceName
    } = topologyList;
    const {metric, latencyTopN} = runtime;
    const metricWrap = metric ? metric.metrics : {};
    let latencyWrap = latencyTopN || [];
    let graphData = [],
      graphVal = 0;
    latencyWrap.map((d, v) => {
      graphData.push({
        name: Object.keys(d)[0],
        value: d[Object.keys(d)[0]]
      });
      graphVal += d[Object.keys(d)[0]];
    });
    const unitLeft = _.slice(latencyWrap, 0, latencyWrap.length / 2);
    const unitRight = _.slice(latencyWrap, latencyWrap.length / 2, latencyWrap.length);
    const ellipseIcon = <i className="fa fa-ellipsis-v"></i>;
    const userInfo = app_state.user_profile !== undefined ? app_state.user_profile.admin :false;
    let permission=true,rights_share=true,aclObject={};
    if(app_state.streamline_config.secureMode){
      aclObject = findSingleAclObj(topology.id,allACL || []);
      const {p_permission,r_share} = handleSecurePermission(aclObject,userInfo,"Applications");
      permission = p_permission;
      rights_share = r_share;
    }

    const dropdown = <div className="pull-right">
      <div className="stream-actions">
        <DropdownButton title={ellipseIcon} id="actionDropdown" className="dropdown-toggle" noCaret bsStyle="link">
          <MenuItem title="Refresh" onClick={this.onActionClick.bind(this, "refresh/" + topology.id)}>
            <i className="fa fa-refresh"></i>
            &nbsp;Refresh
          </MenuItem>
          <MenuItem title="Edit" disabled={!permission} onClick={this.onActionClick.bind(this, "edit/" + topology.id)}>
            <i className="fa fa-pencil"></i>
            &nbsp;Edit
          </MenuItem>
          { !_.isEmpty(aclObject) || userInfo
            ? <MenuItem title="Share" disabled={!rights_share} onClick={this.onActionClick.bind(this, "share/" + topology.id)}>
                <i className="fa fa-share"></i>
                &nbsp;Share
              </MenuItem>
            : ''
          }
          <MenuItem title="Clone" disabled={!permission}  onClick={this.onActionClick.bind(this, "clone/" + topology.id)}>
            <i className="fa fa-clone"></i>
            &nbsp;Clone
          </MenuItem>
          <MenuItem title="Export" disabled={!permission}  onClick={this.onActionClick.bind(this, "export/" + topology.id)}>
            <i className="fa fa-share-square-o"></i>
            &nbsp;Export
          </MenuItem>
          {metricWrap.status !== 'ACTIVE' && metricWrap.status !== 'INACTIVE' ?
          <MenuItem title="Update Engine" disabled={!permission} onClick={this.onActionClick.bind(this, "update/" + topology.id)}>
            <i className="fa fa-wrench"></i>
            &nbsp;Update Engine
          </MenuItem>
          : null
          }
          <MenuItem title="Delete" disabled={!permission} onClick={this.onActionClick.bind(this, "delete/" + topology.id)}>
            <i className="fa fa-trash"></i>
            &nbsp;Delete
          </MenuItem>
        </DropdownButton>
        {
          aclObject.owner !== undefined
          ? !permission
            ? ''
            : <a href="javascript:void(0)" title="Delete" className="close" onClick={this.onActionClick.bind(this, "delete/" + topology.id)}>
                <i className="fa fa-times-circle"></i>
              </a>
          : <a href="javascript:void(0)" title="Delete" className="close" onClick={this.onActionClick.bind(this, "delete/" + topology.id)}>
              <i className="fa fa-times-circle"></i>
            </a>
        }
      </div>
    </div>;

    return (
      <div className="col-sm-4">
        <div className={`card ${(this.checkRefId(topology.id))
          ? ''
          : metricWrap.status || 'NOTRUNNING'}`} data-id={topology.id} ref={(ref) => this.streamRef = ref} onClick={this.streamBoxClick.bind(this, topology.id)}>
          {/*<div className="stream-head clearfix">
            <div className="pull-left m-t-xs">

            </div>
            {dropdown}
          </div>*/}
          {(this.checkRefId(topology.id))
            ? <div className="stream-body">
                <div className="loading-img text-center">
                  <img src="styles/img/start-loader.gif" alt="loading" style={{
                    width: "100px"
                  }}/>
                </div>
              </div>
            :
            <div className="stream-body">
              <div className="card-content">
                {this.getMetrics()}
              </div>
              {this.getFooter()}
            </div>
          }

        </div>
      </div>
    );
  }
}

TopologyItems.propTypes = {
  topologyList: PropTypes.object.isRequired,
  topologyAction: PropTypes.func.isRequired
};

TopologyItems.contextTypes = {
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
      shareObj : {}
    };

    this.fetchData();
  }

  fetchData() {
    const sortKey = this.state.sorted.key;
    const projectId = this.props.params.projectId;
    let promiseArr = [
      EnvironmentREST.getAllNameSpaces(),
      TopologyREST.getSourceComponent(),
      TopologyREST.getAllTopology(projectId, sortKey),
      ProjectREST.getProject(projectId)
    ];
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
      // environment result[0]
      environmentLen = results[0].entities.length;

      // source component result[1]
      sourceLen = results[1].entities.length;

      // All topology result[2]
      let stateObj = {};
      let resultEntities = Utils.sortArray(results[2].entities.slice(), 'timestamp', false);
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

      stateObj.projectData = results[3];

      // If the application is in secure mode result[4]
      if(results[4]){
        stateObj.allACL = results[4].entities;
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
              let result = topology.entities;
              this.setState({searchLoader: false, entities: result, pageIndex: 0});
            }
          });
        } else {
          this.fetchData();
        }
      });
    }, 500);
  }

  fetchSingleTopology = (ID) => {
    const {refIdArr} = this.state;
    const id = +ID;
    const tempArr = refIdArr;
    tempArr.push(id);
    this.setState({
      refIdArr: tempArr
    }, () => {
      TopologyREST.getTopology(id).then((topology) => {
        const entities = this.updateSingleTopology(topology, id);
        const tempDataArray = this.spliceTempArr(id);
        this.setState({refIdArr: tempDataArray, entities});
      }).catch((err) => {
        const tempDataArray = this.spliceTempArr(id);
        this.setState({refIdArr: tempDataArray});
        FSReactToastr.error(
          <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
      });
    });
  }

  spliceTempArr = (id) => {
    const tempArr = this.state.refIdArr;
    const index = tempArr.findIndex((x) => {
      return x === id;
    });
    if (index !== -1) {
      tempArr.splice(index, 1);
    }
    return tempArr;
  }

  updateSingleTopology(newTopology, id) {
    let entitiesWrap = [];
    const elPosition = this.state.entities.map(function(x) {
      return x.topology.id;
    }).indexOf(id);
    entitiesWrap = this.state.entities;
    entitiesWrap[elPosition] = newTopology;
    return entitiesWrap;
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

  actionHandler = (eventKey, id,obj) => {
    const key = eventKey.split('/');
    switch (key[0].toString()) {
    case "refresh":
      this.fetchSingleTopology(id);
      break;
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
      this.shareSingleTopology(id,obj);
      break;
    case "update":
      this.updateEnvironment(id);
      break;
    default:
      break;
    }
  }

  updateEnvironment = (id) => {
    let obj = this.state.entities.find((e)=>{return e.topology.id == id;});
    this.setState({
      topologyData: obj
    }, () => {
      this.AddTopologyModelRef.show();
    });
  }

  shareSingleTopology = (id,obj) => {
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
    default:
      break;
    }
  }
  componentDidUpdate() {
    // this.btnClassChange();
  }
  componentDidMount() {
    // this.btnClassChange();
  }
  btnClassChange = () => {
    const actionMenu = document.querySelector('.actionDropdown');
    if (!this.state.fetchLoader && actionMenu) {
      actionMenu.setAttribute("class", "actionDropdown hb lg success ");
      if (this.state.entities.length !== 0) {
        actionMenu.parentElement.setAttribute("class", "dropdown");
        const sortDropdown = document.querySelector('.sortDropdown');
        sortDropdown.setAttribute("class", "sortDropdown");
        sortDropdown.parentElement.setAttribute("class", "dropdown");
      }
    }
  }
  pagePosition = (index) => {
    this.setState({
      pageIndex: index || 0
    });
  }
  handleSaveClicked = () => {
    const {projectId} = this.props.params;
    if (this.addTopologyRef.validate()) {
      this.addTopologyRef.handleSave(projectId).then((topology) => {
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
            this.context.router.push('projects/'+projectId+'/applications/' + topology.id + '/edit');
          } else {
            this.addTopologyRef.saveMetadata(topology.id).then(() => {
              FSReactToastr.success(
                <strong>Workflow added successfully</strong>
              );
              this.context.router.push('projects/'+projectId+'/applications/' + topology.id + '/edit');
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
          this.context.router.push('projects/'+projectId+'/applications/' + topology.id + '/edit');
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
          this.context.router.push('projects/'+projectId+'/applications/' + topology.id + '/edit');
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
          <Link to="/">My Projects</Link>
          <span className="title-separator">/</span>
          {projectData.name}
          <span className="title-separator">/</span>
          My Workflows
        </span>
      );
    } else {
      return '';
    }
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
      topologyData
    } = this.state;
    const splitData = _.chunk(entities, pageSize) || [];
    const btnIcon = <span><i className="fa fa-plus"></i> New Workflow</span>;
    const sortTitle = <span>Sort:<span style={{
      color: "#006ea0"
    }}>&nbsp;{this.state.sorted.text}</span>
    </span>;

    const components = (splitData.length === 0)
    ? <NoData imgName={"default"} searchVal={filterValue} userRoles={app_state.user_profile}/>
    : splitData[pageIndex].map((list) => {
      return <TopologyItems key={list.topology.id} projectId={this.props.params.projectId} topologyList={list} topologyAction={this.actionHandler} refIdArr={refIdArr} allACL={allACL}/>;
    });

    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.getHeaderContent()}>
        {!fetchLoader
          ? <div>
              {hasEditCapability(accessCapabilities.APPLICATION) && (entities.length || filterValue) ?
                <div className="add-btn text-center">
                  <DropdownButton title={btnIcon} id="actionDropdown" className="actionDropdown success" noCaret>
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
              {((filterValue && splitData.length === 0) || splitData.length !== 0)
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

                      {/*<div className="col-md-3 text-center">
                        <DropdownButton title={sortTitle} id="sortDropdown" className="sortDropdown ">
                          <MenuItem active={this.state.sorted.key === "name" ? true : false } onClick={this.onSortByClicked.bind(this, "name")}>
                            &nbsp;Name
                          </MenuItem>
                          <MenuItem active={this.state.sorted.key === "last_updated" ? true : false } onClick={this.onSortByClicked.bind(this, "last_updated")}>
                            &nbsp;Last Update
                          </MenuItem>
                        </DropdownButton>
                      </div>*/}
                      <div className="col-md-1 col-sm-3 text-left"></div>
                    </div>
                  </div>
                : ''
}
            </div>
          : ''
}
        <div className="row">
          {(fetchLoader || searchLoader)
            ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"applications"}/>]
            : filterValue || entities.length ? components : <BeginNew type="Workflow" onClick={this.onActionMenuClicked.bind(this, "create")}/>
}
        </div>
        {(entities.length > pageSize)
          ? <Paginate len={entities.length} splitData={splitData} pagesize={pageSize} pagePosition={this.pagePosition}/>
          : ''
}
        <Modal className="u-form" ref={(ref) => this.AddTopologyModelRef = ref} data-title={topologyData ? "Update Engine" : "Add Workflow"} onKeyPress={this.handleKeyPress} data-resolve={this.handleSaveClicked} data-reject={()=>{this.setState({topologyData: null});this.AddTopologyModelRef.hide();}}>
          <AddTopology ref={(ref) => this.addTopologyRef = ref} topologyData={topologyData} />
        </Modal>
        <Modal className="u-form" ref={(ref) => this.ImportTopologyModelRef = ref} data-title="Import Workflow" onKeyPress={this.handleKeyPress} data-resolve={this.handleImportSave}>
          <ImportTopology
            ref={(ref) => this.importTopologyRef = ref}
            defaultProjectId={this.props.params.projectId}/>
        </Modal>
        <Modal className="u-form" ref={(ref) => this.CloneTopologyModelRef = ref} data-title="Clone Workflow" onKeyPress={this.handleKeyPress} data-resolve={this.handleCloneSave}>
          <CloneTopology
            topologyId={this.state.cloneFromId}
            ref={(ref) => this.cloneTopologyRef = ref}
            defaultProjectId={this.props.params.projectId}/>
        </Modal>
        {/* CommonShareModal */}
        <Modal className="u-form" ref={"CommonShareModalRef"} data-title="Share Workflow"  data-resolve={this.handleShareSave.bind(this)} data-reject={this.handleShareCancel.bind(this)}>
          <CommonShareModal ref="CommonShareModal" shareObj={shareObj}/>
        </Modal>
        <a className="btn-download" ref="ExportTopology" hidden download href=""></a>
      </BaseContainer>
    );
  }
}

TopologyListingContainer.contextTypes = {
  router: PropTypes.object.isRequired
};

export default TopologyListingContainer;

TopologyListingContainer.defaultProps = {};
