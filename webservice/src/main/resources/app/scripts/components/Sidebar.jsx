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
import {menuName, rolePriorities} from '../utils/Constants';
import MiscREST from '../rest/MiscREST';
import SVGIcons from '../utils/SVGIcons';
import onClickOutside from "react-onclickoutside";

@observer
class Sidebar extends Component {
  constructor(props) {
    super(props);
    this.state = {
      showUserDetails: null
    };
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
  confirmLeave(flag) {
    if (flag) {
      this.refs.leaveEditable.hide();
      this.navigateToDashboard();
    }
  }
  handleClickOutside = () => {
    if(this.state.showUserDetails){
      this.setState({
        showUserDetails: false
      });
    }
  }
  handleUserClick = () => {
    this.setState({
      showUserDetails: !this.state.showUserDetails
    });
  }
  handleLogOut = (e) => {
    MiscREST.userSignOut()
      .then((r)=>{
        app_state.unKnownUser = unknownAccessCode.loggedOut;
      });
  }
  render() {
    let {showUserDetails} = this.state;
    let displayNames = [];
    let metadata = "", priority = 0;
    app_state.roleInfo.forEach((role)=>{
      let obj = rolePriorities.find((o)=>{return o.name === role.name;});
      if(obj.priority > priority) {
        priority = obj.priority;
        metadata = role.metadata;
      }
      displayNames.push(role.displayName);
    });
    let config = app_state.streamline_config;
    let userDropdownClass = "displayNone";
    if(showUserDetails !== null){
      userDropdownClass = showUserDetails ? "user-dropdown animated fadeInUp" : "user-dropdown animated fadeOutDown";
    }
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
            {hasModuleAccess(menuName.CONFIGURATION) ?
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
        {!_.isEmpty(app_state.user_profile) ?
          <section className="sidebar-user">
            <div className="user-btn-group">
              <a href="javascript:void(0)" onClick={this.handleUserClick}>
                <img src="styles/img/uWorc/Avatar.png" className="img-responsive"/>
              </a>
              <span className="user-status"></span>
              <div className={userDropdownClass}>
                <div className="dropdown-head">
                  <h6>{app_state.user_profile.name}</h6>
                  <p>{displayNames.join(', ')}</p>
                </div>
                <ul>
                  <li><a href="javascript:void(0)">{SVGIcons.bugIcon} Report Bug</a></li>
                  <li><a href="javascript:void(0)">{SVGIcons.helpIcon} Help</a></li>
                  <li><a href="javascript:void(0)" onClick={this.handleLogOut}>
                    {SVGIcons.logoutIcon} Logout</a>
                  </li>
                  <li className="user-note">Version 1.0</li>
                </ul>
                <div className="dropdown-foot">Status
                  <span><i className="fa fa-circle"></i> Online</span>
                </div>
            	</div>
            </div>
          </section>
          : null}
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
export default onClickOutside(Sidebar);
