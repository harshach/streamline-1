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
import BaseContainer from '../containers/BaseContainer';

export default class AccessDeniedComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {};
  }
  render() {
    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.props.routes[this.props.routes.length - 1].name}>
        <div className="row m-t-md">
          <div className="col-sm-5">
            <div className="u-form text-center no-access-note">
              <img src="styles/img/uWorc/error.png" />
              <h4 className="headline">You have no access.</h4>
              <p className="info">
                The first release is open to a small group of users. If you are interested, please email us at uworc-eng-group@uber.com.
                <br/>
                More info can be found here &nbsp;
                <a href="https://eng.uberinternal.com/display/DATAWORKFLOW/uWorc+Home" target="_blank">
                  https://eng.uberinternal.com/display/DATAWORKFLOW/uWorc+Home
                </a>
              </p>
            </div>
          </div>
          <div className="col-sm-7">
            <img src="styles/img/uWorc/workflow.gif"/>
          </div>
        </div>
      </BaseContainer>
    );
  }
}
