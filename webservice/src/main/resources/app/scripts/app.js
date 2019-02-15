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
import React, { Component } from 'react';
import routes from './routers/routes';
import { render } from 'react-dom';
import { Router, browserHistory, hashHistory } from 'react-router';
import FSReactToastr from './components/FSReactToastr';
import { toastOpt, unknownAccessCode } from './utils/Constants';
import Utils from './utils/Utils';
import MiscREST from './rest/MiscREST';
import UserRoleREST from './rest/UserRoleREST';
import EngineREST from './rest/EngineREST';
import app_state from './app_state';
import CommonNotification from './utils/CommonNotification';
import UnKnownAccess  from './components/UnKnownAccess';
import {observer} from 'mobx-react';

@observer
class App extends Component {
  constructor(){
    super();
    this.state = {
      showLoading: true
    };
    this.fetchData();
  }
  fetchData(){
    let promiseArr = [
      MiscREST.getAllConfigs(),
      EngineREST.getAllEngines(),
      EngineREST.getAllEngineMetricsTemplate()
    ];

    Promise.all(promiseArr)
      .then((results)=>{
        if(results[0].responseMessage !== undefined){
          FSReactToastr.error(<CommonNotification flag="error" content={results[0].responseMessage}/>, '', toastOpt);
          this.setState({showLoading: false});
        } else {
          let streamModule = results[0].modules.find( module => {
            return module.name == 'streams';
          });
          let serviceDiscovery = {
            showEnvironments: !(streamModule.config.enableShadowNamespaces)
          };
          app_state.streamline_config = {
            registry: results[0].registry,
            dashboard: results[0].dashboard,
            secureMode: results[0].authorizer ? true : false,
            version: results[0].version || '',
            serviceDiscovery: serviceDiscovery
          };

          app_state.engines = results[1].entities;
          _.each(app_state.engines, (e) => {
            e.deploymentModes = JSON.parse(e.deploymentModes);
            e.componentTypes = JSON.parse(e.componentTypes);
            e.config = JSON.parse(e.config);
            Utils.defineEngineProps(e);
          });

          app_state.enginesMetricsTemplates = results[2].entities;

          if(app_state.streamline_config.secureMode){
            // let userProfile = {
            //   "id":1,
            //   "name":"streamline-hdp",
            //   "email":"streamline-hdp@auto-generated",
            //   "timestamp":1494566927285,
            //   "admin":true,
            //   "roles":["ROLE_ADMIN"] //   "roles":["ROLE_ANALYST", "ROLE_DEVELOPER"]
            // };
            // app_state.user_profile = userProfile;
            // this.syncSidebarMenu(userProfile.roles[0]);
            MiscREST.getUserProfile().then((userProfile) => {
              if(userProfile.responseMessage !== undefined){
                FSReactToastr.error(<CommonNotification flag="error" content={userProfile.responseMessage}/>, '', toastOpt);
              } else {
                app_state.user_profile = userProfile;
                this.syncSidebarMenu(userProfile.roles);
              }
            }).catch((err) => {
              this.errorHandler(err);
            });
          } else {
            this.setState({showLoading: false});
          }
        }
      }).catch((err) => {
        this.errorHandler(err);
      });
  }

  errorHandler = (err) => {
    let errorPage=false;
    err.message.indexOf('Not Found') !== -1
      ? errorPage=true
      : err.message.indexOf('Unauthorized') !== -1
        ? errorPage=true
        : '';
    if(errorPage){
      app_state.unKnownUser = unknownAccessCode.unknownUser;
    }
    if(err.response){
      err.response.then((res) => {
        res.responseMessage.indexOf('user database') === -1
        ? res.responseMessage.indexOf('Not authorized') === -1
          ? FSReactToastr.error(<CommonNotification flag="error" content={res.responseMessage}/>, '', toastOpt)
          : ''
        : '';
      });
    }else{
      if(err.responseMessage && err.responseMessage.indexOf("No such user") !== -1){
        location.hash = 'no-access';
      } else {
        console.error(err);
      }
    }
  }

  syncSidebarMenu(roles) {
    UserRoleREST.getAllRoles().then(response=>{
      const roleInfo = response.entities.filter((e)=>{
        e.metadata = JSON.parse(e.metadata);
        return roles.indexOf(e.name) > -1;
      });
      app_state.roleInfo = roleInfo;
      this.setState({showLoading: false});
    });
  }
  render() {
    const {showLoading} = this.state;
    const component = showLoading ? <div></div> : <Router ref="router" history={hashHistory} routes={routes} />;
    return (
      (app_state.unKnownUser === unknownAccessCode.unknownUser || app_state.unKnownUser === unknownAccessCode.loggedOut)
      ? <UnKnownAccess />
      : component
    );
  }
}

/*const app = render(
  <App />, document.getElementById('app_container')
)*/

export default App;
