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
require('file?name=[name].[ext]!../../index.html'); //for production build

import debug from 'debug';
import 'babel-polyfill';
import React, { Component } from 'react';
import { render } from 'react-dom';
import App from './app';
import {AppContainer} from 'react-hot-loader';

import '../styles/css/font-awesome.min.css';
import '../styles/css/bootstrap.css';
import 'animate.css/animate.css';
import 'bootstrap-daterangepicker/daterangepicker.css';
import 'react-select/dist/react-select.css';
import '../styles/css/toastr.min.css';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint.css';
import 'codemirror/addon/hint/show-hint.css';
import '../styles/css/customScroll.css';
import '../styles/css/paper-bootstrap-wizard.css';
import '../styles/css/stepzilla.css';
import "../styles/css/datepicker.css";
import '../styles/css/toggleSwitch.css';
import '../styles/css/style.css';
import '../styles/css/graph-style.css';


render(
  <AppContainer>
  <App/>
</AppContainer>, document.getElementById('app_container'));

if (module.hot) {
  module.hot.accept('./app', () => {
    const NextApp = require('./app').default;
    render(
      <AppContainer>
      <NextApp/>
    </AppContainer>, document.getElementById('app_container'));
  });
}
