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
import {Link} from 'react-router';
import {NavItem} from 'react-bootstrap';
import app_state from '../app_state';
import {observer} from 'mobx-react';
import Modal from '../components/FSModal';
import {hasModuleAccess} from '../utils/ACLUtils';
import {menuName} from '../utils/Constants';

@observer
export default class Sidebar extends Component {
  constructor(props) {
    super(props);
  }
  componentWillMount() {
    var element = document.getElementsByTagName('body')[0];
    element.classList.add('sidebar-mini');
    if (app_state.sidebar_isCollapsed) {
      element.classList.add('sidebar-collapse');
    }
  }
  componentDidUpdate() {
    var element = document.getElementsByTagName('body')[0];
    if (app_state.sidebar_isCollapsed) {
      element.classList.add('sidebar-collapse');
    } else {
      element.classList.remove('sidebar-collapse');
    }
  }
  toggleSidebar() {
    const alphaIcon = document.querySelector('.alpha-icon');
    app_state.sidebar_isCollapsed = !app_state.sidebar_isCollapsed;
  }
  toggleMenu() {
    if (app_state.sidebar_isCollapsed) {
      return;
    }
    app_state.sidebar_activeKey = app_state.sidebar_toggleFlag
      ? ''
      : 3;
    app_state.sidebar_toggleFlag = !app_state.sidebar_toggleFlag;
  }
  handleClick(key, e) {
    app_state.sidebar_activeKey = key;
    if (key === 3) {
      app_state.sidebar_toggleFlag = true;
    } else {
      app_state.sidebar_toggleFlag = false;
    }
  }
  handleClickOnWatchTower(key, e) {
    app_state.sidebar_activeKey = key;
  }
  confirmLeave(flag) {
    if (flag) {
      this.refs.leaveEditable.hide();
      this.navigateToDashboard();
    }
  }
  render() {
    let config = app_state.streamline_config;
    // let registryURL = window.location.protocol + "//" + config.registry.host + ":" + config.registry.port + '/ui/';
    let watchTowerURL = "http://watchtower.uberinternal.com";
    return (
      <aside className="main-sidebar">
        <section className="sidebar">
          <ul className="sidebar-menu">
            {hasModuleAccess(menuName.APPLICATION) ?
              <li className={app_state.sidebar_activeKey === 1
                ? 'active'
                : ''} onClick={this.handleClick.bind(this, 1)}>
                <Link to="/">
                  <img src="/styles/img/uWorc/workflow.svg"/>
                  <span>Workflows</span>
                </Link>
              </li>
              : null
            }
            {hasModuleAccess(menuName.APPLICATION) ?
              <li className={app_state.sidebar_activeKey === 4
                ? 'active'
                : ''} onClick={this.handleClick.bind(this, 4)}>
                <Link to="/shared-projects">
                  <img src="/styles/img/uWorc/share.svg"/>
                  <span>Shared Projects</span>
                </Link>
              </li>
              : null
            }
            {hasModuleAccess(menuName.WATCH_TOWER) ?
              <li className={app_state.sidebar_activeKey === 2
                ? 'active'
                : ''} onClick={this.handleClickOnWatchTower.bind(this, 2)}>
                <a href={watchTowerURL} target="_blank">
                  <img src="/styles/img/uWorc/sr.svg"/>
                  <span>Watch Tower</span>
                </a>
              </li>
              : null
            }
            {hasModuleAccess(menuName.UDF) || hasModuleAccess(menuName.NOTIFIER) || hasModuleAccess(menuName.CUSTOM_PROCESSOR) ||
              hasModuleAccess(menuName.SERVICE_POOL) || hasModuleAccess(menuName.ENVIRONMENT) || hasModuleAccess(menuName.AUTHORIZER) ?
              <li className={app_state.sidebar_activeKey === 3
                ? 'treeview active'
                : 'treeview'}>
                <a href="javascript:void(0);" onClick={this.toggleMenu.bind(this)}>
                  <img src="/styles/img/uWorc/config.svg"/>
                  <span>Configuration</span>
                  <span className="pull-right-container">
                    <i className={app_state.sidebar_toggleFlag
                      ? "fa fa-angle-down pull-right"
                      : (app_state.sidebar_isCollapsed
                        ? "fa fa-angle-down pull-right"
                        : "fa fa-angle-left pull-right")}></i>
                  </span>
                </a>
                <ul className={app_state.sidebar_toggleFlag
                  ? "treeview-menu menu-open"
                  : "treeview-menu"}>
                  {hasModuleAccess(menuName.UDF) || hasModuleAccess(menuName.NOTIFIER) || hasModuleAccess(menuName.CUSTOM_PROCESSOR) ?
                    <li onClick={this.handleClick.bind(this, 3)}>
                      <Link to="/application-resources">Application Resources</Link>
                    </li>
                    : null
                  }
                  {hasModuleAccess(menuName.SERVICE_POOL) ?
                    <li onClick={this.handleClick.bind(this, 3)}>
                      <Link to="/service-pool">Service Pool</Link>
                    </li>
                    : null
                  }
                  {hasModuleAccess(menuName.ENVIRONMENT) && config.serviceDiscovery.showEnvironments ?
                    <li onClick={this.handleClick.bind(this, 3)}>
                      <Link to="/environments">Environments Shadow</Link>
                    </li>
                    : null
                  }
                  {/*hasModuleAccess(menuName.COMPONENT_DEFINITIONS) ?
                    <li onClick={this.handleClick.bind(this, 3)}>
                      <Link to="/component-definition">Component Definitions</Link>
                    </li>
                    : null
                  */}
                  {hasModuleAccess(menuName.AUTHORIZER) ?
                    (app_state.streamline_config.secureMode ?
                      <li onClick={this.handleClick.bind(this, 3)}>
                        <Link to="/authorizer">Users</Link>
                      </li>
                      : null)
                    : null
                  }
                </ul>
              </li>
              : null
            }
          </ul>
        </section>
        {/*<a href="javascript:void(0);" className="sidebar-toggle" onClick={this.toggleSidebar.bind(this)} data-toggle="offcanvas" role="button">
          {!app_state.sidebar_isCollapsed ? <span>Version: {config.version} </span> : null}
          <i className={app_state.sidebar_isCollapsed
            ? "fa fa-angle-double-right"
            : "fa fa-angle-double-left"}></i>
        </a>*/}
        <Modal className="u-form" ref="leaveEditable" data-title="Confirm Box" dialogClassName="confirm-box" data-resolve={this.confirmLeave.bind(this, true)}>
          {< p > Are you sure want to navigate away from this page? </p>}
        </Modal>
      </aside>
    );
  }
}
