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
import ProcessorUtils  from '../../../utils/ProcessorUtils';
import AggregateUdfREST from '../../../rest/AggregateUdfREST';
import NotesForm from '../../../components/NotesForm';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import {Scrollbars} from 'react-custom-scrollbars';
import SqliteParser from 'sqlite-parser';

export default class SqlProcessorNodeForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      formData: {},
      formErrors: {},
      streamObj: {},
      fetchLoader: true
    };
    this.tableMapping = {};
    this.udfList = [];
    this.fetchUdfs();
  }

  componentWillUpdate() {
    if(this.context.ParentForm.state.inputStreamOptions.length > 0 && !(this.fetchDataAgain)){
      this.setParentContextOutputStream();
    }
  }

  fetchUdfs = () => {
    AggregateUdfREST.getAllUdfs().then((udfResult) => {
      if(udfResult.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={results.responseMessage}/>, '', toastOpt);
      } else {
        //Gather all "FUNCTION" functions only
        this.udfList = ProcessorUtils.populateFieldsArr(udfResult.entities , "FUNCTION");
        // if(this.context.ParentForm.state.inputStreamOptions.length){
        //   this.getDataFromParentFormContext();
        // }
      }
    });
  }

  fetchFields = (clusterList) => {
    let obj = this.props.configData.topologyComponentUISpecification.fields;
    return obj;
  }

  setParentContextOutputStream() {
    this.fetchDataAgain = true;
    this.nodeData = this.nodeData || this.context.ParentForm.state.processorNode;
    if(this.nodeData.outputStreams.length){
      this.streamData = this.nodeData.outputStreams[0];
      // this.streamData.fields = this.contextInputStream[0].fields;
      this.setState({showLoading : false});
      this.context.ParentForm.setState({outputStreamObj: this.streamData});
    }else{
      this.streamData = {
        streamId: this.props.configData.subType.toLowerCase() + '_stream_' + this.nodeData.id,
        fields:[]
      };
    }
  }

  handleSave(name) {
    const parent = this.context.ParentForm;
    const {fetchLoader, processorNode, inputStreamOptions} = parent.state;

    this.nodeData = processorNode;
    let {topologyId, versionId, nodeType} = this.props;
    let nodeId = this.nodeData.id;
    let data = this.refs.Form.state.FormData;
    this.nodeData.config.properties = data;
    this.nodeData.name = name;

    this.streamObj = this.streamData;
    if (this.nodeData.outputStreams.length > 0) {
      this.nodeData.outputStreams[0].fields = this.streamObj.fields;
    } else {
      this.nodeData.outputStreams.push({fields: this.streamObj.fields, streamId: this.streamObj.streamId, topologyId: topologyId});
    }
    this.nodeData.description = this.state.description;
    return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {
      body: JSON.stringify(this.nodeData)
    });
  }

  validateData(){
    const {sqlError} = this.state;
    if(sqlError){
      const form = this.refs.Form;
      const {Errors} = form.state;
      Errors.sql = sqlError;
      form.setState({Errors});
      return false;
    }else{
      return this.refs.Form.validate().isFormValid;
    }
  }

  handleNotesChange(description) {
    this.setState({description: description});
  }

  onSqlChange = (valStr) => {
    let error = "";
    if(!valStr.trim()){
      return;
    }
    let data = {
      "inputSchemas": {},
      "sqlStatement": valStr
    };
    const parent = this.context.ParentForm;
    const {inputStreamOptions} = parent.state;

    //Preparing data format as desired by backend
    inputStreamOptions.map((streamObj)=>{
      let str = "{{";
      streamObj.fields.map((fieldObj, index)=>{
        if(index){
          str += "},{";
        }
        str += "name='"+fieldObj.name+"', type="+fieldObj.type;
      });
      str += "}}";
      let topicName = this.getTopicName(streamObj.streamId);
      data.inputSchemas[topicName] = str;
    });

    TopologyREST.getSqlOutputSchema({body: JSON.stringify(data)}).then((response)=>{
      const form = this.refs.Form;
      const Errors = form.state.Errors;
      Errors.sql = '';
      if(response.responseMessage !== undefined){
        error = response.responseMessage;
        Errors.sql = error;
      } else {
        this.streamData.fields = response;
        this.context.ParentForm.setState({outputStreamObj: this.streamData});
      }
      form.setState({Errors});
      this.setState({sqlError: error});
    });
    this.streamData.fields = [];
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }

  getTopicName(streamId){
    const streamIdWOId = streamId.split('_');
    streamIdWOId.splice(streamIdWOId.length-1, 1);
    return streamIdWOId.join('_');
  }

  render() {
    const {securityType, hasSecurity, activeTabKey, formErrors} = this.state;
    const parent = this.context.ParentForm;
    const {fetchLoader, processorNode, inputStreamOptions} = parent.state;

    let formData = {};
    if(processorNode.config && processorNode.config.properties){
      formData = processorNode.config.properties;
    }

    const uiSpec = this.fetchFields();

    this.hintOptions=[];
    if(uiSpec.length){
      const allFields = [];
      const tableNames = [];
      _.each(inputStreamOptions, (stream) => {
        const topicNameFromStreamId = this.getTopicName(stream.streamId);
        const formatedTableName = 'hdrone.'+topicNameFromStreamId.replace(/-/g, '_');
        this.tableMapping[formatedTableName.toLowerCase()] = topicNameFromStreamId;
        tableNames.push({
          name: formatedTableName,
          type: 'TABLE'
        });

        let fieldsArr = [];
        stream.fields.map((field)=>{
          let obj = {
            name: "msg."+field.name,
            type: field.type,
            optional: field.optional
          };
          fieldsArr.push(obj);
        });

        allFields.push.apply(allFields, fieldsArr);
      });

      Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(allFields,"ARGS"));
      Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(this.udfList,"FUNCTION"));
      Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(tableNames,"TABLE"));

      uiSpec[0].hintOptions = this.hintOptions;
      uiSpec[0].onChange = this.onSqlChange;
      uiSpec[0].width = '100%';
      uiSpec[0].height = '350px';
    }

    let fields = Utils.genFields(uiSpec, [], formData,[], securityType, hasSecurity,'');
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

SqlProcessorNodeForm.contextTypes = {
  ParentForm: PropTypes.object
};
