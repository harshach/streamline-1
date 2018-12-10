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
export default class RightSidebar extends Component {
  constructor(props) {
    super(props);
  }
  render() {
    const {getHeader, getBody, expanded} = this.props;
    return (
      <aside className={`right-sidebar ${expanded ? 'active' : ''}`}>
        <div className="icon-panel">
          {getHeader()}
        </div>
        <div className="sidebar-content">
          {getBody()}
        </div>
      </aside>
    );
  }
}
