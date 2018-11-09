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
import {Tabs, Tab} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import TopologyREST from '../../../rest/TopologyREST';
import Form from '../../../libs/form';
import StreamsSidebar from '../../../components/StreamSidebar';
import NotesForm from '../../../components/NotesForm';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import {Scrollbars} from 'react-custom-scrollbars';

export default class ProcessorNodeFormFields extends Component {
  constructor(props) {
    super(props);
    this.state = {
      formData: {},
      formErrors: {},
      streamObj: {},
      fetchLoader: true
    };
  }

  fetchFields = (clusterList) => {
    let obj = this.props.configData.topologyComponentUISpecification.fields;
    return obj;
  }

  handleSave(name) {
    let {topologyId, versionId, nodeType} = this.props;
    let nodeId = this.nodeData.id;
    let data = this.refs.Form.state.FormData;
    this.nodeData.config.properties = data;
    this.nodeData.name = name;
    if (this.nodeData.outputStreams.length > 0) {
      this.nodeData.outputStreams[0].fields = this.streamObj.fields;
    } else {
      this.nodeData.outputStreams.push({fields: [{"name": "dummyfield", "type": "STRING"}], streamId: this.streamObj.streamId, topologyId: topologyId});
    }
    this.nodeData.description = this.state.description;
    return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {
      body: JSON.stringify(this.nodeData)
    });
  }

  validateData(){
    return this.refs.Form.validate().isFormValid;
  }

  handleNotesChange(description) {
    this.setState({description: description});
  }

  render() {
    const {securityType, hasSecurity, activeTabKey, formErrors} = this.state;
    const parent = this.context.ParentForm;
    const {fetchLoader, processorNode} = parent.state;
    let formData = processorNode;

    let fields = Utils.genFields(this.fetchFields(), [], formData,[], securityType, hasSecurity,'');
    const disabledFields = this.props.testRunActivated ? true : !this.props.editMode;

    const form = fetchLoader ? 
      <div className="col-sm-12">
        <div className="loading-img text-center" style={{
          marginTop: "100px"
        }}>
          <img src="styles/img/start-loader.gif" alt="loading"/>
        </div>
      </div>
    : <div className="processor-node-fields-container">
      <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
        display: "none"
      }}/>}>
        <Form
          ref="Form"
          readOnly={disabledFields}
          showRequired={null}
          showSecurity={this.state.showSecurity}
          className="customFormClass"
          FormData={formData}
          Errors={formErrors}
        >
          {fields}
        </Form>
      </Scrollbars>
    </div>;
    return form;
  }
}

ProcessorNodeFormFields.contextTypes = {
  ParentForm: PropTypes.object
};
