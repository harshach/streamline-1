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
import {
  Button,
  Form,
  FormGroup,
  Col,
  FormControl,
  Checkbox,
  Radio,
  ControlLabel,
  Popover,
  InputGroup,
  OverlayTrigger
} from 'react-bootstrap';
import {Select2 as Select,Creatable} from '../../utils/SelectUtils';
import validation from './ValidationRules';
import _ from 'lodash';
import Utils from '../../utils/Utils';
import TopologyREST from '../../rest/TopologyREST';
import ProcessorUtils from '../../utils/ProcessorUtils';
import DatetimeRangePicker from 'react-bootstrap-datetimerangepicker';
import DateTimeFormatter from '../../components/DateTimeFormatter';
import moment from 'moment';
import Cron from '../cron';
import app_state from '../../app_state';
import Switch from "../../components/ToggleSwitch.js";
import SVGIcons from '../../utils/SVGIcons';

import CodeMirror from 'codemirror';
import CommonCodeMirror from '../../components/CommonCodeMirror';

export class BaseField extends Component {
  type = 'FormField';
  getField = () => {}
  validate(value) {
    let errorMsg = '';
    if (this.props.validation) {
      this.props.validation.forEach((v) => {
        if (errorMsg == '') {
          errorMsg = validation[v](value, this.context.Form, this);
        } else {
          return;
        }
      });
    }
    const {Form} = this.context;
    Form.state.Errors[this.props.valuePath] = errorMsg;
    Form.setState(Form.state);
    return !errorMsg;
  }

  getLabel(){
    const popoverContent = (
      <Popover id="popover-trigger-hover-focus">
        {this.props.fieldJson.tooltip}
      </Popover>
    );
    return  <label>{this.props.label} {this.props.validation && this.props.validation.indexOf('required') !== -1
              ? <span className="text-danger">*</span>
              : null}
              <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
                <i className="fa fa-info-circle info-label"></i>
              </OverlayTrigger>
            </label>;
  }

  toggleModal = () => {
    $('.customFormClass').toggleClass('expanded');
    $('.fa').toggleClass('fa-compress fa-expand');
  }

  render() {
    const {className} = this.props;
    const labelHint = this.props.fieldJson.hint || null;
    const showMaxBtn = this.props.fieldJson.type === 'sql';
    return (
      <FormGroup className={className}>
        {labelHint !== null && labelHint.toLowerCase().indexOf("hidden") !== -1
          ? ''
          : this.getLabel()
        }
        {showMaxBtn ? <button type="button" className="pull-right btn btn-link btn-xs" onClick={this.toggleModal}>
          <i className="fa fa-expand"></i>
          </button>
        : null}
        {this.getField()}
        <p className="text-danger">{this.context.Form.state.Errors[this.props.valuePath]}</p>
      </FormGroup>
    );
  }
}

BaseField.contextTypes = {
  Form: PropTypes.object
};

export class file extends BaseField {
  handleChange = (e) => {
    const {Form} = this.context;
    if(e.target.files.length){
      const fileData = e.target.files[0];
      let fileType = fileData.name;
      if(fileType.indexOf(this.props.fieldJson.hint) !== -1){
        this.props.data[this.props.value] = fileType;
        Form.setState(Form.state, () => {
          if(this.validate()){
            this.context.Form.props.fetchFileData(fileData,this.props.fieldJson.fieldName);
          }
        });
      }
    } else {
      this.props.data[this.props.value] = '';
      Form.setState(Form.state);
    }
  }

  handleUpload = () => {
    this.refs.fileName.click();
  }

  validate() {
    let isValid = super.validate(this.props.data[this.props.value]);
    if(!isValid){
      this.refs.fileName.scrollIntoViewIfNeeded();
    }
    return isValid;
  }

  getField = () => {
    const inputHint = this.props.fieldJson.hint || null;
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (inputHint !== null && inputHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
          <input ref="fileName" accept={`.${this.props.fieldJson.hint}`}
            type={this.props.fieldJson.hint !== undefined
            ? this.props.fieldJson.hint.toLowerCase().indexOf("file") !== -1
              ? "file"
              : "file"
            : "file"
            } placeholder="Select file"  className="hidden-file-input"
            onChange={(event) => {
              this.handleChange.call(this, event);
            }}
            {...this.props.attrs}
            required={true}/>
          <div>
            <InputGroup>
              <InputGroup.Addon className="file-upload">
                <Button
                  type="button"
                  className="browseBtn btn-primary"
                  onClick={this.handleUpload.bind(this)}
                >
                  <i className="fa fa-folder-open-o"></i>&nbsp;Browse
                </Button>
              </InputGroup.Addon>
              <FormControl
                type="text"
                placeholder="No file chosen"
                disabled={disabledField}
                value={this.props.data[this.props.value]}
                className={this.context.Form.state.Errors[this.props.valuePath]
                ? "form-control invalidInput"
                : "form-control"}
              />
            </InputGroup>
          </div>
        </div>);
  }

}

const getSchema = function(topic, branch, flag, id) {
  if (branch != '') {
    clearTimeout(this.topicTimer);
    this.topicTimer = setTimeout(() => {
      getSchemaFromName.call(this,topic,branch,id,flag);
    }, 700);
  }
};

const getSchemaFromName = function(topicName, branch, id, flag) {
  let resultArr = [];
  let versionsId = flag ? id : '';
  let promiseArr = [];
  if(flag) {
    promiseArr.push(TopologyREST.getSchemaForKafka(topicName, versionsId));
  } else {
    promiseArr.push(TopologyREST.getSchemaVersionsForKafka(topicName, branch));
  }
  Promise.all(promiseArr).then(result => {
    if (result[0].responseMessage !== undefined) {
      this.refs.select2.className = "form-control invalidInput";
      this.context.Form.state.Errors[this.props.valuePath] = flag ? 'Schema Not Found': 'Branch Not Found';
      this.context.Form.setState(this.context.Form.state);
    } else {
      this.refs.select2.className = "form-control";
      resultArr = result[0];
      if (typeof resultArr === 'string') {
        resultArr = JSON.parse(resultArr);
      }
      this.context.Form.state.Errors[this.props.valuePath] =  resultArr.length === 0 ? (flag ? 'Schema Not Found' : 'Branch Not Found') : '';
      this.context.Form.setState(this.context.Form.state);
    }
    if (this.context.Form.props.callback) {
      this.context.Form.props.callback(resultArr,flag);
    }
  });
};

const getSchemaBranches =  function(val) {
  if (val != '') {
    clearTimeout(this.branchTimer);
    this.branchTimer = setTimeout(() => {
      getSchemaBranchesFromName.call(this,val);
    }, 700);
  }
};

const getSchemaBranchesFromName = function(topicName) {
  let resultArr = [];
  TopologyREST.getSchemaBranchesForKafka(topicName).then(result => {
    const elRef = this.refs.input || this.refs.select2;
    if (result.responseMessage !== undefined) {
      elRef.className = "form-control invalidInput";
      this.context.Form.state.Errors[this.props.valuePath] = 'Schema Not Found';
      this.context.Form.setState(this.context.Form.state);
    } else {
      elRef.className = "form-control";
      resultArr = result;
      if (typeof resultArr === 'string') {
        resultArr = JSON.parse(resultArr);
      }
      this.context.Form.state.Errors[this.props.valuePath] =  resultArr.length === 0 ? 'Schema Not Found' : '';
      this.context.Form.setState(this.context.Form.state);
    }
    if (this.context.Form.props.schemaBranchesCallback) {
      this.context.Form.props.schemaBranchesCallback(resultArr);
    }
  });
};

const dependsOnHintsFunction = function(val){
  const {Form} = this.context;
  if (this.validate() && (this.props.fieldJson !== undefined && this.props.fieldJson.hint !== undefined && Utils.matchStringInArr(this.props.fieldJson.hint, 'schema'))) {
    if(Utils.matchStringInArr(this.props.fieldJson.hint, 'override')){
      getSchemaBranches.call(this,this.props.data[this.props.value]);
    } else {
      //Get schema directly from topic
      getSchema.call(this, this.props.data[this.props.value], 'MASTER', true);
    }
  } else if (this.props.fieldJson.fieldName === "securityProtocol"){
    if(Form.props.handleSecurityProtocol){
      Form.props.handleSecurityProtocol(val.value);
    }
  } else if (this.validate() && (this.props.fieldJson !== undefined && this.props.fieldJson.hint !== undefined  && Utils.matchStringInArr(this.props.fieldJson.hint, "schemaBranch"))) {
    let fieldName = this.props.fieldJson.hint.split(',')
                      .filter((h)=>{return h.indexOf('dependsOn') !== -1;})[0]
                      .split('-')[1];
    const topicName = this.props.data[fieldName];
    getSchema.call(this,topicName, this.props.data[this.props.value], false);
  } else if (this.validate() && (this.props.fieldJson !== undefined && this.props.fieldJson.hint !== undefined  && Utils.matchStringInArr(this.props.fieldJson.hint, "schemaVersion"))) {
    let branchName = this.props.fieldJson.hint.split(',')
                      .filter((h)=>{return h.indexOf('dependsOn') !== -1;})[0]
                      .split('-')[1];
    const branch = this.props.data[branchName];
    const topic = this.props.data[getSchemasFieldName.call(this)];
    getSchema.call(this,topic, branch, true, this.props.data[this.props.value]);
  }
};

const getSchemasFieldName = function(){
  const {Form} = this.context;
  let name = '';
  _.map(_.keys(Form.refs), (refKey) => {
    if(Form.refs[refKey].props.fieldJson.hint !== undefined){
      const hints = Form.refs[refKey].props.fieldJson.hint.split(',');
      const index = _.findIndex(hints, (h) => h === 'schema');
      if(index !== -1){
        name = Form.refs[refKey].props.fieldJson.fieldName;
      }
    }
  });
  return name;
};

export class string extends BaseField {
  handleChange = () => {
    const value = this.refs.input.value;
    const {Form} = this.context;
    this.props.data[this.props.value] = value;
    Form.setState(Form.state, () => {
      if (this.validate() && (this.props.fieldJson.hint !== undefined && this.props.fieldJson.hint.toLowerCase().indexOf("schema") !== -1)) {
        getSchema.call(this,this.props.data[this.props.value],false);
      }
    });
  }

  handleOnBlur = () => {
    const value = this.refs.input.value.trim();
    const {Form} = this.context;
    this.props.data[this.props.value] = value;
    Form.setState(Form.state, () => {
      dependsOnHintsFunction.call(this,value);
    });
  }

  validate() {
    let {validation} = this.props.fieldJson;
    let value = this.props.data[this.props.value];
    let isValid = super.validate(value);
    if(isValid){
      if(validation && validation.regex){
        let reg = new RegExp(validation.regex);
        if(!reg.test(value)){
          this.context.Form.state.Errors[this.props.valuePath] = validation.errorMessage;
          isValid = false;
        }
      }
    }
    if(!isValid){
      this.refs.input.scrollIntoViewIfNeeded();
    }
    return isValid;
  }

  getField = () => {
    const inputHint = this.props.fieldJson.hint || null;
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    let value = this.props.data[this.props.value] || '';
    if(inputHint && inputHint.toLowerCase().indexOf("user") !== -1 && value.trim() == ''){
      value = app_state.user_profile.name || "";
      this.props.data[this.props.value] = value;
    }
    return (inputHint !== null && inputHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : this.props.fieldJson.hint !== undefined && this.props.fieldJson.hint.toLowerCase().indexOf("textarea") !== -1
          ? <textarea className={this.context.Form.state.Errors[this.props.valuePath]
              ? "form-control invalidInput"
              : "form-control"} placeholder={this.props.fieldJson.placeholder || '' } ref="input" disabled={disabledField} value={value} {...this.props.attrs} onChange={this.handleChange}/>
            : <input name={this.props.value} placeholder="Select file" type={this.props.fieldJson.hint !== undefined
            ? this.props.fieldJson.hint.toLowerCase().indexOf("password") !== -1
              ? "password"
              : this.props.fieldJson.hint.toLowerCase().indexOf("email") !== -1
                ? "email"
                : "text"
            : "text"} className={this.context.Form.state.Errors[this.props.valuePath]
            ? "form-control invalidInput"
            : "form-control"} ref="input" value={value}
            disabled={disabledField} {...this.props.attrs} onChange={this.handleChange}
            placeholder={this.props.fieldJson.placeholder || '' } onBlur={this.handleOnBlur}/>
    );
  }
}

export class date extends BaseField {
  get dateFormat(){
    return 'YYYY-MM-DD';
  }
  get datePickerOptions(){
    return {};
  }
  componentDidMount(){
    if(this.parentEl != this.datePickerRef){
      this.parentEl = $(this.datePickerRef);
      this.forceUpdate();
    }
  }
  handleChange = (e, datePicker) => {
    const {Form} = this.context;
    this.props.data[this.props.value] = datePicker.startDate.format(this.dateFormat);
    Form.setState(Form.state, () => {
      this.validate();
    });
  }
  validate() {
    let isValid = super.validate(this.props.data[this.props.value]);
    if(!isValid){
      this.datePickerRef.scrollIntoViewIfNeeded();
    }
    return isValid;
  }
  getField = () => {
    const form_value = this.props.data[this.props.value];

    const value = form_value && moment(form_value) || moment();

    const errorClass = this.context.Form.state.Errors[this.props.valuePath] ? 'invalidInput' : '';

    return (<div
      ref={(ref) => this.datePickerRef = ref}
      style={{position: 'relative'}}
    >
    {this.parentEl ?
      <DatetimeRangePicker
        singleDatePicker
        autoUpdateInput={true}
        showDropdowns
        startDate={value}
        endDate={value}
        onApply={this.handleChange}
        parentEl={this.parentEl}
        {...this.datePickerOptions}
      >
        <InputGroup className="selected-date-range-btn form-datepicker-group">
          <Button className={`${errorClass}`}>
            <div className="pull-right">
              <i className="fa fa-calendar"/>
            </div>
            {form_value ?
              <span className="pull-left">{value.format(this.dateFormat)}</span>
              :
              <label className="place-holder">Select Date</label>}
            &nbsp;
          </Button>
        </InputGroup>
      </DatetimeRangePicker>
    : null}
    </div>);
  }
}

export class datetime extends date {
  get dateFormat(){
    return 'YYYY-MM-DD HH:mm:ss';
  }
  get datePickerOptions(){
    return {
      timePicker: true,
      timePicker24Hour:true,
      timePickerSeconds: true
    };
  }
}

export class datetimerange extends BaseField{

  // This class stores 2 representation of the dates, one that matches
  // the format in the input and another that matches format expected by
  // the server.  The change handlers update both.
  toServerFormat(dateString) {
    if (dateString) {
      const date = moment(dateString, "MM-DD-YY HH:mm:ss", true);
      return date.isValid() ? date.format("YYYY-MM-DD HH:mm:ss") : 'Invalid';
    }
    return '';
  }

  toFieldFormat(dateString) {
    if (dateString) {
      const date = moment(dateString, "YYYY-MM-DD HH:mm:ss");
      return date.isValid() ? date.format("MM-DD-YY HH:mm:ss") : '';
    }
    return '';
  }

  componentWillMount() {
    // On init, if values aren't set, set default as now with no seconds
    if (!this.props.data[this.props.value]) {
      const nowString = new moment().format("YYYY-MM-DD HH:mm:00");
      this.props.data[this.props.value] = nowString;
    }

    if (!this.props.data["topology.endDate"]) {
      this.props.data["topology.endDate"] = '';
    }

    // Set intermediate value in client format
    this.startDate = this.toFieldFormat(this.props.data[this.props.value]);
    this.endDate = this.toFieldFormat(this.props.data["topology.endDate"]);

    const {Form} = this.context;
    Form.setState(Form.state);
  }

  handleChangeOnStart = (obj) => {
    const {Form} = this.context;
    let startDate = obj.date ? obj.date : '';
    let startTime = obj.time ? obj.time : '';

    this.startDate = startDate.replace(/\//g, '-') + ' ' + startTime;
    this.props.data[this.props.value] = this.toServerFormat(this.startDate);
    Form.setState(Form.state);
  }

  handleChangeOnEnd = (obj) => {
    const {Form} = this.context;
    let endDate = obj.date ? obj.date : '';
    let endTime = obj.time ? obj.time : '';

    this.endDate = endDate.replace(/\//g, '-') + ' ' + endTime;
    this.props.data['topology.endDate'] = this.toServerFormat(this.endDate);
    Form.setState(Form.state);
  }

  validate() {
    const startValue = this.props.data[this.props.value];
    const endValue = this.props.data['topology.endDate'];

    let isValid = super.validate(startValue);

    if(isValid){
      let errorMsg = endValue ? validation['datetime'](endValue, this.context.Form, this) : '';
      if (errorMsg) {
        const {Form} = this.context;
        Form.state.Errors[this.props.valuePath] = 'End ' + errorMsg;
        Form.setState(Form.state);
        isValid = false;
      }
    }
    if(!isValid){
      this.datePickerRef.scrollIntoViewIfNeeded();
    }
    return isValid;
  }

  getStartDate = () => {
    let dateRange = this.startDate.split(' ');
    let value = {
      date: dateRange[0],
      time: dateRange[1]
    };
    return (<DateTimeFormatter value={value} placeholder="Start Date" callBack={this.handleChangeOnStart}/>);
  }

  getEndDate = () => {
    let dateRange = this.endDate.split(' ');
    let value = {
      date: dateRange[0],
      time: dateRange[1]
    };
    return (<DateTimeFormatter value={value} placeholder="End Date" callBack={this.handleChangeOnEnd}/>);
  }

  render() {
    const {className} = this.props;
    const labelHint = this.props.fieldJson.hint || null;
    const popoverContent = (
      <Popover id="popover-trigger-hover-focus">
        {this.props.fieldJson.tooltip}
      </Popover>
    );
    return (
      <div>
        <FormGroup className={"row "+className} >
          <div className="col-sm-5" ref={(ref) => this.datePickerRef = ref}>
            <label>{this.props.label} {this.props.validation && this.props.validation.indexOf('required') !== -1
              ? <span className="text-danger">*</span>
              : null}
              <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
                <i className="fa fa-info-circle info-label"></i>
              </OverlayTrigger>
            </label>
              {this.getStartDate()}
          </div>
          <div className="col-sm-1 text-center datetime-arrow">
            <div className="date-arrow"><a className="date-arrow">{SVGIcons.rightArrowIcon}</a></div>
          </div>
          <div className="col-sm-5">
            <label>End Date</label>
            {this.getEndDate()}
          </div>
        </FormGroup>
        <p className="text-danger">{this.context.Form.state.Errors[this.props.valuePath]}</p>
      </div>
    );
  }
}

export class sql extends BaseField {
  handleChange = (value) => {
    const {fieldJson} = this.props;
    const {Form} = this.context;
    this.props.data[this.props.value] = value;
    Form.setState(Form.state);
    this.validate();
    if(fieldJson.onChange){
      clearTimeout(this.sqlTimer);
      this.sqlTimer = setTimeout(()=>{
        this.onChange();
      }, 1000);
    }
  }
  onChange(){
    const {fieldJson} = this.props;
    fieldJson.onChange(this.props.data[this.props.value]);
  }
  validate() {
    let isValid = super.validate(this.props.data[this.props.value]);
    if(!isValid){
      this.refs.codemirror.codeWrapper.scrollIntoView();
    }
    return isValid;
  }
  getField = () => {
    const {fieldJson} = this.props;
    const width = fieldJson.width || '100%';
    const height = fieldJson.height || '250px';

    return <div
      className={this.context.Form.props.readOnly ? "disable-codemirror" : null}
      style={{position: 'relative'}}
    ><CommonCodeMirror
      ref="codemirror"
      modeType="sql"
      value={this.props.data[this.props.value]}
      callBack={this.handleChange}
      placeholder={this.props.fieldJson.placeholder || ' ' }
      editMode={true}
      width="100%"
      editMode={false}
      modeOptions={{
        readOnly: this.context.Form.props.readOnly ? "nocursor" : false,
        mode:"text/x-sql",
        hint: CodeMirror.hint.sql,
        hintOptions:fieldJson.hintOptions || [],
        viewportMargin: Infinity
      }}
      hintOptions={fieldJson.hintOptions}
    /></div>;
  }
}

export class shell extends sql {
  getField = () => {
    const {fieldJson} = this.props;
    const width = fieldJson.width || '100%';
    const height = fieldJson.height || '250px';

    return <div
      className={this.context.Form.props.readOnly ? "disable-codemirror" : null}
      style={{position: 'relative'}}
    ><CommonCodeMirror
      ref="codemirror"
      modeType="shell"
      value={this.props.data[this.props.value]}
      callBack={this.handleChange}
      placeholder={this.props.fieldJson.placeholder || ' ' }
      editMode={true}
      width="100%"
      height={height}
      editMode={false}
      modeOptions={{
        readOnly: this.context.Form.props.readOnly ? "nocursor" : false
      }}
    /></div>;
  }
}

export class keyvalue extends BaseField {
  componentWillMount(){
    const {fieldJson, valuePath} = this.props;
    const {Form} = this.context;
    Form.state.Errors[valuePath] = Form.state.Errors[valuePath] || [];
  }
  handleChange = () => {
    const {Form} = this.context;
    let value = {};
    _.each(this.keyValueArr, (keyValue) => {
      value[keyValue.fieldName] = keyValue.fieldValue || '';
    });
    this.props.data[this.props.value] = JSON.stringify(value);
    Form.setState(Form.state);
  }
  validate() {
    let validate = true;

    _.each(this.keyValueArr, (keyValue, i) => {
      if(!this.validateKeyField(keyValue.fieldName, i)){
        validate = false;
      }
      if(!this.validateValueField(keyValue.fieldValue, i)){
        validate = false;
      }
    });
    return validate;
  }
  validateKeyField(value, i){
    const {Form} = this.context;

    const {fieldJson, valuePath} = this.props;
    let fieldErrors = Form.state.Errors[valuePath];
    let validate = true;
    fieldErrors[i] = fieldErrors[i] || {};

    if(!fieldJson.isOptional && !value){
      validate = false;
      fieldErrors[i].fieldName = "Required!";
    }else{
      fieldErrors[i].fieldName = "";
    }
    Form.setState(Form.state);
    return validate;
  }
  validateValueField(value, i){
    const {Form} = this.context;

    const {fieldJson, valuePath} = this.props;
    let fieldErrors = Form.state.Errors[valuePath];
    let validate = true;
    fieldErrors[i] = fieldErrors[i] || {};

    if(!fieldJson.isOptional && !value){
      validate = false;
      fieldErrors[i].fieldValue = "Required!";
    }else{
      fieldErrors[i].fieldValue = "";
    }
    Form.setState(Form.state);
    return validate;
  }
  onKeyChange(i, e) {
    const value = e.currentTarget.value;
    this.keyValueArr[i].fieldName = value;
    this.validateKeyField(value, i);
    this.handleChange();
  }
  onValueChange(i, e) {
    const value = e.currentTarget.value;
    this.keyValueArr[i].fieldValue = value;
    this.validateValueField(value, i);
    this.handleChange();
  }
  addKeyValue(i) {
    this.keyValueArr.splice(i+1, 0, {fieldName: '', fieldValue: ''});
    this.forceUpdate();
  }
  removeKeyValue(i){
    this.keyValueArr.splice(i, 1);
    this.handleChange();
    this.forceUpdate();
  }
  getField = () => {
    const {valuePath} = this.props;
    const value = this.props.data[this.props.value] || JSON.stringify({});
    let obj;

    obj = JSON.parse(value);

    this.keyValueArr = this.keyValueArr || _.map(obj, (val, key) => {
      return {
        fieldName: key,
        fieldValue: val
      };
    });

    if(!this.keyValueArr.length){
      this.keyValueArr.push({fieldName: '', fieldValue: ''});
    }

    let disabled = this.context.Form.props.readOnly;
    const iconCursor = !disabled ? 'pointer' : 'not-allowed';

    const {Form} = this.context;
    let fieldErrors = Form.state.Errors[valuePath] || [];

    return <div style={{position: 'relative'}}>
      <div className="row">
        <div className="col-sm-5">
          <label>Key</label>
        </div>
        <div className="col-sm-5">
          <label>Value</label>
        </div>
        <div className="col-sm-2"></div>
      </div>
    {
      _.map(this.keyValueArr, (keyValue, i) => {
        const fieldName = keyValue.fieldName || '';
        const fieldValue = keyValue.fieldValue || '';
        return <div className="row" key={i}>
          <div className="col-sm-5">
            <div className="form-group">
              <input type="text" value={fieldName} disabled={disabled} key={"fieldName"+i} className="form-control" onChange={this.onKeyChange.bind(this, i)} />
              <p className="text-danger">{fieldErrors[i] && fieldErrors[i].fieldName}</p>
            </div>
          </div>
          <div className="col-sm-5">
            <div className="form-group">
              <input type="text" value={fieldValue} disabled={disabled} key={"fieldValue"+i} className="form-control" onChange={this.onValueChange.bind(this, i)} />
              <p className="text-danger">{fieldErrors[i] && fieldErrors[i].fieldValue}</p>
            </div>
          </div>
          <div className="col-sm-2 m-t-xs">
            <i className="fa fa-plus text-secondary m-r-xs" style={{cursor: iconCursor}} onClick={disabled ? null : this.addKeyValue.bind(this, i)}></i>
            {i != 0 ?
              <i className="fa fa-trash text-danger" style={{cursor: iconCursor}} onClick={disabled ? null : this.removeKeyValue.bind(this, i)}></i>
            : null}
          </div>
        </div>;
      })
    }
    </div>;
  }
  render() {
    const {className} = this.props;
    const labelHint = this.props.fieldJson.hint || null;
    return (
      <FormGroup className={className}>
        {labelHint !== null && labelHint.toLowerCase().indexOf("hidden") !== -1
          ? ''
          : this.getLabel()
}
        {this.getField()}
      </FormGroup>
    );
  }
}

export class cron extends BaseField {
  handleChange = (value) => {
    const {Form} = this.context;
    this.props.data[this.props.value] = value;
    Form.setState(Form.state);
    this.validate();
  }
  validate() {
    return super.validate(this.props.data[this.props.value]);
  }
  getField = () => {
    return <Cron
      value={this.props.data[this.props.value]}
      onChange={this.handleChange}
      className="form-field cron-field"
      disabled={this.context.Form.props.readOnly}
    />;
  }
}

export class number extends BaseField {
  handleChange = () => {
    const value = parseFloat(this.refs.input.value);
    const {Form} = this.context;
    this.props.data[this.props.value] = value;
    Form.setState(Form.state);
    this.validate();
  }
  validate() {
    let {validation} = this.props.fieldJson;
    let value = this.props.data[this.props.value];
    let isValid = super.validate(value);
    if(isValid){
      if(validation && !_.isUndefined(validation.min) && value < validation.min){
        isValid = false;
      }
      if(validation && !_.isUndefined(validation.max) && value > validation.max){
        isValid = false;
      }
      if(!isValid){
        this.context.Form.state.Errors[this.props.valuePath] = validation.errorMessage;
      }
    } else {
      this.refs.input.scrollIntoViewIfNeeded();
    }
    return isValid;
  }
  getField = () => {
    const numberHint = this.props.fieldJson.hint || null;
    let {validation} = this.props.fieldJson;
    const min = (validation && !_.isUndefined(validation.min)) ? validation.min : 0;
    const max = (validation && !_.isUndefined(validation.max)) ? validation.max : Number.MAX_SAFE_INTEGER;
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (numberHint !== null && numberHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      :<input name={this.props.value} type="number" className={this.context.Form.state.Errors[this.props.valuePath]
          ? "form-control invalidInput"
          : "form-control"} ref="input" value={this.props.data[this.props.value]}
          disabled={disabledField} {...this.props.attrs} min={min} max={max}
          placeholder={this.props.fieldJson.placeholder || '' } onChange={this.handleChange}/>
    );
  }
}

export class boolean extends BaseField {
  handleChange = (evt) => {
    const {Form} = this.context;
    this.props.data[this.props.value] = evt.SWITCH_STATE.enabled;
    Form.setState(Form.state);
    this.validate();
  }
  validate() {
    return true;
  }
  render() {
    const booleanHint = this.props.fieldJson.hint || null;
    const {className} = this.props;
    const popoverContent = (
      <Popover id="popover-trigger-hover-focus">
        {this.props.fieldJson.tooltip}
      </Popover>
    );
    let disabledField = this.props.fieldJson.readOnly !== undefined
                          ? this.props.fieldJson.readOnly
                          : this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (booleanHint !== null && booleanHint.toLowerCase().indexOf("hidden") !== -1
      ? null
      : <FormGroup className={"fg-checkbox "+className}>
          <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
            <label>{this.props.label}
              {this.props.validation && this.props.validation.indexOf('required') !== -1
                ? <span className="text-danger">*</span>
                : null}</label>
          </OverlayTrigger>
          <label className="toggle-group">
            <Switch theme="graphite-small" ref="input" className="d-flex" enabled={this.props.data[this.props.value]} onClick={this.handleChange} />
          </label>
      </FormGroup>);
  }
}

export class enumstring extends BaseField {
  handleChange = (val) => {
    this.props.data[this.props.value] = val.value;
    const {Form} = this.context;
    Form.setState(Form.state, () => {
      dependsOnHintsFunction.call(this,val);
    });
  }

  validate() {
    let isValid = super.validate(this.props.data[this.props.value]);
    if(!isValid){
      this.refs.select2.wrapper.scrollIntoViewIfNeeded();
    }
    return isValid;
  }
  getField = () => {
    const enumStringHint = this.props.fieldJson.hint || null;
    const fieldsShown = _.filter(this.context.Form.props.children, (x) => {
      return x.props.fieldJson.isOptional == false;
    });
    const lastChild = _.last(fieldsShown) || {props: {}};
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    let value = this.props.data[this.props.value];
    if(value instanceof Array){
      value = value[0];
    }
    return (enumStringHint !== null && enumStringHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
        <Select name={this.props.value} ref="select2" clearable={false} onChange={this.handleChange} {...this.props.fieldAttr} disabled={disabledField} value={value} className={`${lastChild.props.label === this.props.fieldJson.uiName && fieldsShown.length > 4
        ? "menu-outer-top"
        : ''}${this.context.Form.state.Errors[this.props.valuePath]
          ? "invalidSelect"
          : ""}`} placeholder={this.props.fieldJson.placeholder || '' }/>
        </div>);
  }
}

export class CustomEnumstring extends BaseField {
  handleChange = (val) => {
    this.props.data[this.props.value] = val.value;
    const {Form} = this.context;
    Form.setState(Form.state);
    this.validate();
    this.context.Form.props.populateClusterFields(val.value);
  }
  validate() {
    let isValid = super.validate(this.props.data[this.props.value]);
    if(!isValid){
      this.refs.select2.wrapper.scrollIntoViewIfNeeded();
    }
    return isValid;
  }
  renderFieldOption(node) {
    const name = this.splitFields(node);
    let styleObj = {
      fontWeight: "bold"
    };
    let styleObj2 = {
      paddingLeft: (10 * name[0]) + "px",
      fontSize: 12,
      fontWeight: "normal"
    };
    return (
      <span style={styleObj}>{node.label}</span>
    );
  }
  splitFields(text) {
    const nameObj = _.isObject(text)
      ? text.label.split('@#$')
      : text
        ? text.split('@#$')[0]
        : '';
    return nameObj;
  }
  getField = () => {
    const customEnumHint = this.props.fieldJson.hint || null;
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    const selectedValue = this.props.fieldAttr.options.find((d) => {
      return d.label == this.props.data[this.props.value];
    });
    return (customEnumHint !== null && customEnumHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
        <Select placeholder={this.props.fieldJson.placeholder || 'Select..' } ref="select2" clearable={false} onChange={this.handleChange}
          {...this.props.fieldAttr} disabled={disabledField} value={selectedValue || ''}
          className={this.context.Form.state.Errors[this.props.valuePath]
        ? "invalidSelect"
        : ""} optionRenderer={this.renderFieldOption.bind(this)}/>
        </div>);
  }
}

export class arraystring extends BaseField {
  handleChange = (val) => {
    const value = val.map((d) => {
      return d.value;
    });
    this.props.data[this.props.value] = value;
    const {Form} = this.context;
    Form.setState(Form.state);
    this.validate();
  }
  validate() {
    let isValid = super.validate(this.props.data[this.props.value]);
    if(!isValid){
      this.refs.select2.wrapper.scrollIntoViewIfNeeded();
    }
    return isValid;
  }
  getField = () => {
    const arraystringHint = this.props.fieldJson.hint || null;
    const fieldsShown = _.filter(this.context.Form.props.children, (x) => {
      return x.props.fieldJson.isOptional == false;
    });
    const lastChild = _.last(fieldsShown) || {props: {}};
    const arr = [];
    let dataArr = this.props.data[this.props.value];
    if (dataArr && dataArr instanceof Array) {
      dataArr.map((value) => {
        arr.push({value: value});
      });
    }
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (arraystringHint !== null && arraystringHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <Creatable onChange={this.handleChange} multi={true} disabled={disabledField} {...this.props.fieldAttr} className={`${lastChild.props.label === this.props.fieldJson.uiName && fieldsShown.length > 4
        ? "menu-outer-top"
        : ''}${this.context.Form.state.Errors[this.props.valuePath]
          ? "invalidSelect"
          : ""}`} valueKey="value" labelKey="value" value={arr} ref="select2"
          placeholder={this.props.fieldJson.placeholder || '' }/>);
  }
}

export class creatableField extends BaseField {
  handleChange = (val) => {
    this.props.data[this.props.value] = val.value;
    const {Form} = this.context;
    Form.setState(Form.state, () => {
      if (this.validate() && (this.props.fieldJson.hint !== undefined && this.props.fieldJson.hint.toLowerCase().indexOf("schema") !== -1)) {
        this.getSchema(this.props.data[this.props.value]);
      }
    });
  }

  getSchema(val){
    if (val != '') {
      clearTimeout(this.branchTimer);
      this.branchTimer = setTimeout(() => {
        this.getSchemaForKafka(val);
      }, 700);
    }
  }

  getSchemaForKafka(){
    //Get schema directly from topic
    getSchema.call(this, this.props.data[this.props.value], 'MASTER', true);
  }

  validate() {
    let isValid = super.validate(this.props.data[this.props.value]);
    if(!isValid){
      this.refs.select2.scrollIntoViewIfNeeded();
    }
    return isValid;
  }
  getField = () => {
    const creatableHint = this.props.fieldJson.hint || null;
    const fieldsShown = _.filter(this.context.Form.props.children, (x) => {
      return x.props.fieldJson.isOptional == false;
    });
    const lastChild = _.last(fieldsShown) || {props: {}};
    const val = {
      value: this.props.data[this.props.value]
    };
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (creatableHint !== null && creatableHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <Creatable placeholder={this.props.fieldJson.placeholder || 'Select..' } ref="select2" clearable={false} onChange={this.handleChange} multi={false} disabled={disabledField} {...this.props.fieldAttr} className={`${lastChild.props.label === this.props.fieldJson.uiName && fieldsShown.length > 4
        ? "menu-outer-top"
        : ''}${this.context.Form.state.Errors[this.props.valuePath]
          ? "invalidSelect"
          : ""}`} valueKey="value" labelKey="value" value={val.value ? val : ''}/>);
  }
}

export class arrayenumstring extends BaseField {
  handleChange = (val) => {
    this.props.data[this.props.value] = val.map(d => {
      return d.value;
    });
    const {Form} = this.context;
    Form.setState(Form.state);
    this.validate(val);
  }
  handleSelectAll = () => {
    const optVal = this.props.fieldAttr.options;
    const val = this.props.data[this.props.value] = ProcessorUtils.selectAllOutputFields(optVal,'sinkForm');
    const {Form} = this.context;
    Form.setState(Form.state);
    this.validate(val);
  }
  validate(val) {
    if(val && this.props.fieldJson.hint && this.props.fieldJson.hint.indexOf("noNestedFields") !== -1) {
      let nestedField = val.findIndex(v => {return v.type === 'NESTED';});
      if(nestedField > -1) {
        this.context.Form.state.Errors[this.props.valuePath] = 'Invalid!';
        this.context.Form.setState(this.context.Form.state);
        this.refs.select2.wrapper.scrollIntoViewIfNeeded();
        return false;
      }
    }
    let isValid = super.validate(this.props.data[this.props.value]);
    if(!isValid){
      this.refs.select2.wrapper.scrollIntoViewIfNeeded();
    }
    return isValid;
  }
  getLabel(){
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    const popoverContent = (
      <Popover id="popover-trigger-hover-focus">
        {this.props.fieldJson.tooltip}
      </Popover>
    );
    return  <span>
              <label>{this.props.label} {this.props.validation && this.props.validation.indexOf('required') !== -1
                ? <span className="text-danger">*</span>
                : null}
                <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
                  <i className="fa fa-info-circle info-label"></i>
                </OverlayTrigger>
              </label>
              {disabledField ? null : <a className="pull-right" href="javascript:void(0)" onClick={this.handleSelectAll}>Select All</a>}
            </span> ;
  }
  renderFieldOption(node) {
    let styleObj = {
      paddingLeft: (10 * node.level) + "px"
    };
    if (node.disabled) {
      styleObj.fontWeight = "bold";
    }
    return (
      <span style={styleObj}>{node.name}</span>
    );
  }
  getField = () => {
    const arrayEnumHint = this.props.fieldJson.hint || null;
    const fieldsShown = _.filter(this.context.Form.props.children, (x) => {
      return x.props.fieldJson.isOptional == false;
    });
    const lastChild = _.last(fieldsShown) || {props: {}};
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    const tempAttrObj = {};
    if(this.props.fieldJson.hint && this.props.fieldJson.hint.indexOf("inputFields") !== -1){
      tempAttrObj.optionRenderer = this.renderFieldOption;
    }
    return (arrayEnumHint !== null && arrayEnumHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
        <Select onChange={this.handleChange} multi={true} disabled={disabledField} {...this.props.fieldAttr} value={this.props.data[this.props.value]} className={`${lastChild.props.label === this.props.fieldJson.uiName && fieldsShown.length > 4
        ? "menu-outer-top"
        : ''}${this.context.Form.state.Errors[this.props.valuePath]
          ? "invalidSelect"
          : ""}`} {...tempAttrObj} ref="select2" placeholder={this.props.fieldJson.placeholder || '' }/>
        </div>);
  }
}

export class object extends BaseField {
  handleChange = () => {
    this.props.data[this.props.value] = this.refs.input.value;
    const {Form} = this.context;
    Form.setState(Form.state);
    // this.context.Form.setValue(this.props.value, this.refs.input.value, this);
  }
  validate() {
    const {Form} = this.context;
    return Form.validate.call(this);
  }
  render() {
    const {className} = this.props;
    const inputHint = this.props.fieldJson.hint || null;
    return (inputHint !== null && inputHint.toLowerCase().indexOf("hidden") !== -1
      ? null
      : <fieldset className={className + " fieldset-default"}>
          <legend>{this.props.label}</legend>
          {this.getField()}
        </fieldset>
    );
  }
  getField = () => {
    return this.props.children.map((child, i) => {
      return React.cloneElement(child, {
        ref: child.props
          ? (child.props._ref || i)
          : i,
        key: i,
        data: this.props.data[this.props.value]
      });
    });
  }
}

export class arrayobject extends BaseField {
  handleChange = () => {
    // this.context.Form.setValue(this.props.value, this.refs.input.value, this);
  }
  validate = () => {
    const {Form} = this.context;
    return Form.validate.call(this);
  }
  onAdd = () => {
    this.props.data[this.props.value].push({});
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  onRemove(index) {
    this.props.data[this.props.value].splice(index, 1);
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  render() {
    const {className, fieldJson} = this.props;
    if(fieldJson.hint && fieldJson.hint.indexOf("table") !== -1){
      //remove object which is the parent field
      let newOptionArr = [];
      fieldJson.options.map((field)=>{
        if(field.fieldType){
          newOptionArr.push(field);
        }
      });
      if(!this.props.data[this.props.value] && newOptionArr.length > 0){
        this.props.data[this.props.value] = newOptionArr.map((f)=>{return {};});
      }
      return (
        <div className="table-responsive">
          <table className="table table-bordered table-sink">
            <thead>
              {fieldJson.fields.map((field)=>{
                return (
                  <th>{field.uiName}</th>
                );
              })}
            </thead>
            <tbody>
              {newOptionArr.map((inputFields, index)=>{
                let d = this.props.data[this.props.value][index];
                if(!d.name){
                  d.name = inputFields.fieldName;
                  d.type = inputFields.type.toLowerCase();
                }
                const optionsFields = Utils.genFields(fieldJson.fields, [
                  ...this.props.valuePath.split('.'),
                  index
                ], d);
                return (
                  <tr>{optionsFields.map((child, i) => {
                    return (
                      <td>{React.cloneElement(child, {
                        ref: child.props
                          ? (child.props._ref || i)
                          : i,
                        key: i,
                        data: d
                      })}</td>
                    );
                  })}</tr>
                );
              })}
            </tbody>
          </table>
        </div>
      );
    } else {
      return (
        <fieldset className={className + " fieldset-default"}>
          <legend>{this.props.label} {this.context.Form.props.readOnly
              ? ''
              : <i className="fa fa-plus" aria-hidden="true" onClick={this.onAdd}></i>}</legend>
          {this.getField()}
        </fieldset>
      );
    }
  }
  getField = () => {
    const fields = this.props.fieldJson.fields;
    this.props.data[this.props.value] = this.props.data[this.props.value] || [{}];
    return this.props.data[this.props.value].map((d, i) => {
      const splitElem = i > 0
        ? <hr/>
        : null;
      const removeElem = <i className="fa fa-trash delete-icon" onClick={this.onRemove.bind(this, i)}></i>;
      const optionsFields = Utils.genFields(fields, [
        ...this.props.valuePath.split('.'),
        i
      ], d);
      return [
        splitElem,
        removeElem,
        optionsFields.map((child, i) => {
          return React.cloneElement(child, {
            ref: child.props
              ? (child.props._ref || i)
              : i,
            key: i,
            data: d
          });
        })
      ];
    });
  }
}

export class enumobject extends BaseField {
  constructor(props) {
    super(props);
    if (props.list) {
      this.data = props.data;
    } else {
      this.data = this.props.data[this.props.value];
    }
  }
  componentWillUpdate(props) {
    if (props.list) {
      this.data = props.data;
    } else {
      this.data = this.props.data[this.props.value];
    }
  }
  handleChange = (val) => {
    delete this.data[Object.keys(this.data)[0]];
    this.data[val.value] = {};
    this.validate(val.value);
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  validate = () => {
    const {Form} = this.context;
    return Form.validate.call(this);
  }
  getField = () => {
    const enumObjectHint = this.props.fieldJson.hint || null;
    const value = Object.keys(this.data)[0];
    return (enumObjectHint !== null && enumObjectHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
        <Select clearable={false} onChange={this.handleChange} {...this.props.fieldAttr} value={value}/>
        </div>);
  }
  render() {
    const value = Object.keys(this.data)[0];
    const selected = _.find(this.props.fieldJson.options, (d) => {
      return d.fieldName == value;
    });
    const optionsFields = Utils.genFields(selected.fields, this.props.valuePath.split('.'), this.data[value]);
    const {className} = this.props;
    return (
      <div className={className}>
        <FormGroup>
          <label>{this.props.label}</label>
          {this.getField()}
          <p className="text-danger">{this.context.Form.state.Errors[this.props.valuePath]}</p>
        </FormGroup>
        {optionsFields.map((child, i) => {
          return React.cloneElement(child, {
            ref: child.props
              ? (child.props._ref || i)
              : i,
            key: i,
            data: this.data[value]
          });
        })}
      </div>
    );
  }
}

const EnumObject = enumobject;

export class arrayenumobject extends BaseField {
  onAdd = () => {
    this.props.data[this.props.value].push({
      [this.props.fieldJson.options[0].fieldName]: {}
    });
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  onRemove(index) {
    this.props.data[this.props.value].splice(index, 1);
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  validate = () => {
    const {Form} = this.context;
    return Form.validate.call(this);
  }
  render() {
    const {className} = this.props;
    return (
      <fieldset className={className + " fieldset-default"}>
        <legend>{this.props.label} {this.context.Form.props.readOnly
            ? ''
            : <i className="fa fa-plus" aria-hidden="true" onClick={this.onAdd}></i>}</legend>
        {this.getField()}
      </fieldset>
    );
  }
  getField = () => {
    const fields = this.props.fieldJson.options;
    if (this.props.fieldJson.isOptional) {
      this.props.data[this.props.value] = this.props.data[this.props.value] || [];
    } else {
      this.props.data[this.props.value] = this.props.data[this.props.value] || [
        {
          [this.props.fieldJson.options[0].fieldName]: {}
        }
      ];
    }
    return this.props.data[this.props.value].map((d, i) => {
      const splitElem = i > 0
        ? <hr/>
        : null;
      const removeElem = <i className="fa fa-trash delete-icon" onClick={this.onRemove.bind(this, i)}></i>;
      return ([
        splitElem, removeElem, < EnumObject {
          ...this.props
        }
        ref = {
          this.props.valuePath + '.' + i
        }
        key = {
          i
        }
        data = {
          d
        }
        valuePath = {
          [
            ...this.props.valuePath.split('.'),
            i
          ].join('.')
        }
        list = {
          true
        } />
      ]);
    });
  }
}
