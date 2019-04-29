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
import {Link} from 'react-router';
import app_state from '../app_state';
import {observer} from 'mobx-react';
import {Nav, Navbar, NavItem, NavDropdown, MenuItem,DropdownButton} from 'react-bootstrap';
import _ from 'lodash';

@observer
export default class Header extends Component {

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <header className="main-header">
        <Link to="/" className="logo">
          <span className="logo-mini">
            <img src="/styles/img/uWorc/logo.svg" data-stest="logo-collapsed" width="40"/>
          </span>
        </Link>
        <nav className="navbar navbar-default navbar-static-top">
          <div>
            <div className="headContentText">
              {this.props.headerContent}
            </div>
          </div>
        </nav>
      </header>
    );
  }
}

Header.contextTypes = {
  router: PropTypes.object.isRequired
};
