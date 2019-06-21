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
import ReactDOM, {findDOMNode} from 'react-dom';
import {Link} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import ClusterREST from '../../../rest/ClusterREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import MiscREST from '../../../rest/MiscREST';
import Utils from '../../../utils/Utils';
import Form from '../../../libs/form';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import CommonNotification from '../../../utils/CommonNotification';
import {Tabs, Tab} from 'react-bootstrap';
import {Scrollbars} from 'react-custom-scrollbars';
import StepZilla from "react-stepzilla";

export default class TopologyConfigContainer extends Component {
  static propTypes = {
    topologyId: PropTypes.string.isRequired
  };

  constructor(props) {
    super(props);
    this.state = {
      formData: {},
      formField: {},
      fetchLoader: true,
      activeTabKey: 1,
      hasSecurity: false,
      advancedField : [{
        fieldName : '',
        fieldValue : ''
      }],
      clustersArr: []
    };
    this.allClusterList = [];
    this.fetchData();
  }

  fetchData = () => {
    const {topologyId, versionId, uiConfigFields} = this.props;
    let promiseArr = [
      TopologyREST.getTopologyWithoutMetrics(topologyId, versionId),
      ClusterREST.getAllCluster(),
      EnvironmentREST.getAllNamespaceFromService(uiConfigFields.engine.toLowerCase()),
      MiscREST.getUserGroup()
    ];
    Promise.all(promiseArr).then(result => {
      const formField = JSON.parse(JSON.stringify(uiConfigFields.topologyComponentUISpecification));

      // formField.fields[5].type="datetimerange";
      // delete result[0].config.properties["topology.startDate"];
      // delete formField.fields.splice(6,1);

      let ldapGroupObj = formField.fields.find((o)=>{return o.fieldName === 'topology.ownerLDAPGroups';});
      if(ldapGroupObj){
        ldapGroupObj.type = "array.enumstring";
        ldapGroupObj.options = result[3] || [];
      }

      const config = result[0].config && result[0].config.properties ? result[0].config.properties : {};
      const {f_Data,adv_Field}= this.fetchAdvanedField(formField, config);
      this.namespaceId = result[0].namespaceId;
      const clustersConfig = result[1].entities;
      this.allClusterList = result[2].entities;

      let dcListObj = formField.fields.find((o)=>{return o.hint === 'datacenter-showAll';});
      if(dcListObj){
        result[2].entities.map((o)=>{
          let c = clustersConfig.find((c)=>{return c.cluster.id === o.namespace.id;});
          dcListObj.options.push({value: o.namespace.id, label: c.cluster.name});
        });
        dcListObj.defaultValue = result[0].namespaceId;
      } else {
        dcListObj = formField.fields.find((o)=>{return o.hint === 'datacenter-showCurrent';});
        if(dcListObj){
          let currentNamepaceObj = result[2].entities.find((e)=>{return e.namespace.id === result[0].namespaceId;});
          if(currentNamepaceObj){
            let c = clustersConfig.find((c)=>{return c.cluster.id === currentNamepaceObj.namespace.id;});
            dcListObj.options.push({value: currentNamepaceObj.namespace.id, label: c.cluster.name});
          }
          dcListObj.defaultValue = result[0].namespaceId;
        }
      }

      EnvironmentREST.getNameSpace(this.namespaceId)
        .then((r)=>{
          let mappings = r.mappings.filter((c)=>{
            return c.namespaceId === this.namespaceId && (c.serviceName === 'HBASE' || c.serviceName === 'HDFS' || c.serviceName === 'HIVE');
          });
          let clusters = [];
          mappings.map((m)=>{
            let c = clustersConfig.find((o)=>{return o.cluster.id === m.clusterId;});
            const obj = {
              id: c.cluster.id,
              fieldName: c.cluster.name + '@#$' + c.cluster.ambariImportUrl,
              uiName: c.cluster.name
            };
            clusters.push(obj);
          });
          clusters = _.uniqBy(clusters, 'id');
          let securityFieldsObj = _.find(formField.fields, {"fieldName": "clustersSecurityConfig"});
          if(securityFieldsObj){
            var securityFields = securityFieldsObj.fields;
            if(securityFields){
              let fieldObj = _.find(securityFields, {"fieldName": "clusterId"});
              if(fieldObj){
                fieldObj.options = clusters;
                fieldObj.type = 'CustomEnumstring';
              }
            }
          }
          /*
            setting value of cluster name in form data from corresponding id
          */
          if(f_Data.clustersSecurityConfig) {
            f_Data.clustersSecurityConfig.map((c)=>{
              if(c.clusterId) {
                c.id = c.clusterId;
                let clusterObj = clusters.find((o)=>{return o.id === c.clusterId;});
                if(clusterObj !== undefined){
                  c.clusterId = clusterObj.uiName;
                }
              }
            });
          }
          this.setState({formData: f_Data, formField: formField, fetchLoader: false,advancedField : adv_Field, clustersArr: clusters});
          let mapObj = r.mappings.find((m) => {
            return m.serviceName.toLowerCase() === 'storm';
          });
          if (mapObj) {
            var stormClusterId = mapObj.clusterId;
            let hasSecurity = false, principalsArr = [], keyTabsArr = [];
            // ClusterREST.getStormSecurityDetails(stormClusterId)
            //   .then((entity)=>{
            //     if(entity.responseMessage !== undefined) {
            //       let msg = entity.responseMessage.indexOf('StreamlinePrincipal') !== -1
            //       ? " Please contact admin to get access for this application's services."
            //       : entity.responseMessage;
            //       FSReactToastr.error(<CommonNotification flag="error" content={msg}/>, '', toastOpt);
            //     } else {
            //       if(entity.security.authentication.enabled) {
            //         hasSecurity = true;
            //       }
                  // let stormPrincipals = (entity.security.principals && entity.security.principals["storm"]) || [];
                  // stormPrincipals.map((o)=>{
                  //   principalsArr.push(o.name);
                  // });

                  // _.keys(entity.security.keytabs).map((kt)=>{
                  //   keyTabsArr.push(entity.security.keytabs[kt]);
                  // });

            let principalFieldObj = _.find(securityFields, {"fieldName": "principal"});
            if(principalFieldObj){
              principalFieldObj.options = principalsArr;
            }

            let keyTabFieldObj = _.find(securityFields, {"fieldName": "keytabPath"});
            if(keyTabFieldObj){
              keyTabFieldObj.options = keyTabsArr;
            }
                // }
                //removing security related fields for non-secure mode
            if(hasSecurity === false) {
              if(formField.fields && formField.fields.length > 0) {
                formField.fields = _.filter(formField.fields, (f)=>{
                  if(f.hint && f.hint.indexOf('security_') !== -1) {
                    return false;
                  } else {
                    return true;
                  }
                });
              }
            } else {
              let nodes = this.props.topologyNodes.filter((c)=>{
                return c.currentType.toLowerCase() === 'hbase' || c.currentType.toLowerCase() === 'hdfs' || c.currentType.toLowerCase() === 'hive';
              });
              if(nodes.length == 0) {
                let nameField = _.find(securityFields, {"fieldName": "clusterId"});
                nameField.isOptional = true;

                let principalField = _.find(securityFields, {"fieldName": "principal"});
                principalField.isOptional = true;

                let keyTabField = _.find(securityFields, {"fieldName": "keytabPath"});
                keyTabField.isOptional = true;
              }
            }
            this.setState({hasSecurity: hasSecurity, formField: formField});
              // });
          } else {
            //topology created using TestEnvironment does not have mappings in the environment
            //so removing the security fields from the config.
            if(formField.fields && formField.fields.length > 0) {
              formField.fields = _.filter(formField.fields, (f)=>{
                if(f.hint && f.hint.indexOf('security_') !== -1) {
                  return false;
                } else {
                  return true;
                }
              });
            }
            this.setState({formField: formField});
          }
        });
    }).catch(err => {
      this.setState({fetchLoader: false});
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }

  fetchAdvanedField = (formField,config) => {
    let f_Data = {}, adv_Field = [];
    _.map(_.keys(config), (key) => {
      const index = _.findIndex(formField.fields, (fd) => { return fd.fieldName === key;});
      if(index !== -1){
        f_Data[key] = config[key];
      } else if(key === "topology.namespaceIds"){
        f_Data['deploymentSettings.namespaceIds'] = JSON.parse(config[key]);
      } else if(key === "topology.endDate"){
        f_Data[key] = config[key];
      } else {
        adv_Field.push({fieldName : key , fieldValue : config[key] instanceof Array ? JSON.stringify(config[key]) : config[key]});
      }
    });
    if(adv_Field.length === 0){
      adv_Field.push({
        fieldName : '',
        fieldValue : ''
      });
    }

    return {f_Data,adv_Field};
  }

  populateClusterFields(val) {
    if(val) {
      const name = val.split('@#$')[0];
      let clusterSecurityConfig = this.refs.Form.state.FormData["clustersSecurityConfig"];
      let i = clusterSecurityConfig.findIndex((c)=>{return c.clusterId && c.clusterId.indexOf('@#$') > -1;});
      clusterSecurityConfig[i].clusterId = name;
      let obj = this.state.clustersArr.find((o)=>{return o.fieldName === val;});
      clusterSecurityConfig[i].id = obj.id;
      this.setState({formData: this.refs.Form.state.FormData});
    }
  }

  checkAdvancedField = (fields) => {
    let merge = true;
    _.map(fields, (field) => {
      if(field.fieldName === '' || field.fieldValue === ''){
        merge = false;
      }
    });
    return merge;
  }

  generateOutputFields = (fieldList) => {
    let mergeData = {};
    _.map(fieldList,(fd) => {
      mergeData[fd.fieldName] =  Utils.checkTypeAndReturnValue(fd.fieldValue);
    });
    return mergeData;
  }

  handleSave() {
    const {topologyName, topologyId, versionId, projectId, topologyData} = this.props;
    const {advancedField} = this.state;
    let data = this.finalFormData;
    // check if advancedField doesn't has empty field and ready to mergeData
    if(this.checkAdvancedField(advancedField)){
      data = Utils.deepmerge(data , this.generateOutputFields(advancedField));
    }

    if(data.clustersSecurityConfig) {
      data.clustersSecurityConfig.map((c, i)=>{
        if(c.clusterId) {
          c.clusterId = c.id;
        } else {
          delete c.clusterId;
        }
        delete c.id;
      });
    }

    if(data['deploymentSettings.namespaceIds']){
      let namespaceIdList = data['deploymentSettings.namespaceIds'];
      if(!(namespaceIdList instanceof Array)){
        namespaceIdList = [namespaceIdList];
      }
      data['topology.namespaceIds'] = JSON.stringify(namespaceIdList);
      if(namespaceIdList.length === this.allClusterList.length){
        data['topology.deploymentMode'] = "ALL";
      } else {
        data['topology.deploymentMode'] = "CHOSEN_REGION";
      }
      delete data['deploymentSettings.namespaceIds'];
    }

    let dataObj = {
      name: topologyName,
      config: {
        properties: data
      },
      namespaceId: this.namespaceId,
      projectId: projectId,
      engineId: topologyData.engineId,
      templateId: topologyData.templateId
    };
    return TopologyREST.putTopology(topologyId, versionId, {body: JSON.stringify(dataObj)});
  }

  advanedFieldChange = (fieldType,index,event) => {
    let tempField = _.cloneDeep(this.state.advancedField);
    const val = event.target.value;
    tempField[index][fieldType] = val;
    this.setState({advancedField : tempField});
  }

  addAdvancedRowField = () => {
    let tempField = _.cloneDeep(this.state.advancedField);
    tempField.push({fieldName : '',fieldValue : ''});
    this.setState({advancedField : tempField});
  }

  deleteAdvancedRowField = (index) => {
    let tempField = _.cloneDeep(this.state.advancedField);
    tempField.splice(index,1);
    this.setState({advancedField : tempField});
  }

  getFormData = (data) => {
    this.finalFormData = _.cloneDeep(data);
  }

  getStepsComponents(stepKey, isLastStep){
    const {formData, formField, advancedField} = this.state;
    let fields = Utils.genFields(formField.fields || [], [], formData);
    const disabledFields = this.props.testRunActivated ? true : false;

    switch(stepKey){
    case "workflow":{
      return <Settings
                formData={formData}
                disabledFields={disabledFields}
                fields={fields}
                showDCFields={false}
                showSecurity={false}
                FormRef={this.refs.Form}
                getFormData={this.getFormData}
                handleSaveConfig={isLastStep ? this.props.handleSaveConfig : false}
              />;
      break;
    }
    case "datacenter":{
      return <Settings
                formData={formData}
                disabledFields={disabledFields}
                fields={fields}
                showDCFields={true}
                showSecurity={false}
                FormRef={this.refs.Form}
                getFormData={this.getFormData}
                engine={this.props.engine}
                handleSaveConfig={isLastStep ?  this.props.handleSaveConfig : false}
              />;
      break;
    }
    case "security": {
      return <Settings
                formData={formData}
                disabledFields={disabledFields}
                fields={fields}
                showDCFields={false}
                showSecurity={true}
                FormRef={this.refs.Form}
                populateClusterFields={this.populateClusterFields.bind(this)}
                getFormData={this.getFormData}
                engine={this.props.engine}
                handleSaveConfig={isLastStep ?  this.props.handleSaveConfig : false}
              />;
      break;
    }
    case "advanced":{
      return <AdvSetting
                disabledFields={disabledFields}
                advancedField={advancedField}
                advanedFieldChange={this.advanedFieldChange.bind(this)}
                addAdvancedRowField={this.addAdvancedRowField.bind(this)}
                deleteAdvancedRowField={this.deleteAdvancedRowField.bind(this)}
                handleSaveConfig={isLastStep ? this.props.handleSaveConfig : false}
              />;
      break;
    }
    }
  }

  getSteps(){
    let {uiConfigFields} = this.props;
    let stepConfigArr = uiConfigFields.topologyComponentUISpecification.steps;
    let steps = stepConfigArr.map((step, index) => {
      let isLastStep = false;
      if(index === (stepConfigArr.length - 1)){
        isLastStep = true;
      }
      return {
        name: step.label, component: this.getStepsComponents(step.key, isLastStep)
      };
    });

    //this is required for the stepzilla library to be present as the last step
    //we are hiding it from the UI via CSS
    steps.push({
      name: 'Blank',
      component: <Blank/>
    });
    return steps;
  }

  render() {
    const {fetchLoader} = this.state;
    const steps = this.getSteps();
    return (
      <div>
        {fetchLoader
          ? <div className="col-sm-12">
              <div className="loading-img text-center" style={{
                marginTop: "150px"
              }}>
                <img src="styles/img/start-loader.gif" alt="loading"/>
              </div>
            </div>
          : <div className='step-progress'>
            <StepZilla
              ref="StepZilla"
              steps={steps}
              stepsNavigation={false}
              nextTextOnFinalActionStep="Save"
              backButtonCls="btn btn-next btn-link btn-lg"
              nextButtonCls="btn btn-prev btn-primary btn-lg"
            />
          </div>
        }
      </div>
    );
  }
}
class Settings extends Component{
  constructor(props){
    super(props);
  }
  isValidated(){
    let {showDCFields, getFormData, engine, handleSaveConfig} = this.props;
    let validDataFlag = false, validateError = [];
    const {isFormValid, invalidFields} = this.refs.Form.validate();
    if (isFormValid) {
      validDataFlag = true;
    } else {
      const invalidField = invalidFields[0];

      if(invalidField.props.fieldJson.isOptional === false
        && invalidField.props.fieldJson.hint
        && (invalidField.props.fieldJson.hint.indexOf('security_') > -1
            || invalidField.props.fieldJson.hint.indexOf('datacenter') > -1
        )
      ){
        if(showDCFields){
          return false;
        }
      }else if(invalidField.props.fieldJson.isOptional === false){
        if(!showDCFields){
          return false;
        }
      }
    }
    getFormData(this.refs.Form.state.FormData);
    if(handleSaveConfig){
      handleSaveConfig();
    }
    return true;
  }
  render(){
    let {formData, disabledFields, fields, showDCFields, showSecurity, populateClusterFields} = this.props;
    return (
      <div className="source-modal-form app-config">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          <Form
            ref="Form" FormData={formData}
            readOnly={disabledFields} showRequired={null}
            showDataCenter={showDCFields}
            showSecurity={showSecurity}
            populateClusterFields={populateClusterFields ? populateClusterFields: null}
            className="modal-form config-modal-form"
          >
            {fields}
          </Form>
        </Scrollbars>
      </div>
    );
  }
}
class AdvSetting extends Component{
  constructor(props){
    super(props);
  }
  isValidated(){
    const {advancedField, handleSaveConfig} = this.props;
    let validateError = [];
    if(advancedField.length > 1){
      _.map(advancedField, (adv) => {
        if((adv.fieldName !== '' && adv.fieldValue === '') || (adv.fieldName === '' && adv.fieldValue !== '')){
          validateError.push(false);
        }
      });
    }
    if(validateError.length === 0){
      if(handleSaveConfig){
        handleSaveConfig();
      } else {
        return true;
      }
    } else {
      return false;
    }
  }
  render(){
    let {advancedField, advanedFieldChange, addAdvancedRowField, deleteAdvancedRowField, disabledFields } = this.props;
    return (
      <form className="source-modal-form app-config">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          <div className="clearfix">
            <div className="col-sm-5">
              <div className="form-group">
                <label>Field Name
                  <span className="text-danger">*</span>
                </label>
              </div>
            </div>
            <div className="col-sm-5">
              <div className="form-group">
                <label>Field Value
                  <span className="text-danger">*</span>
                </label>
              </div>
            </div>
          </div>
          {
            _.map(advancedField , (adv,i) => {
              return <div className="clearfix" key={i}>
                <div className="col-sm-5">
                  <div className="form-group">
                    <input type="text" value={adv.fieldName} className="form-control" onChange={event => advanedFieldChange('fieldName',i, event)} />
                  </div>
                </div>
                <div className="col-sm-5">
                  <div className="form-group">
                    <input type="text" value={adv.fieldValue} className="form-control" onChange={event => advanedFieldChange('fieldValue',i, event)} />
                  </div>
                </div>
                {!this.props.testRunActivated
                  ? <div className="col-sm-2">
                      <button className="btn btn-default btn-sm" disabled={disabledFields} type="button" onClick={event => addAdvancedRowField()}>
                        <i className="fa fa-plus"></i>
                      </button>&nbsp; {i > 0
                        ? <button className="btn btn-sm btn-danger" type="button" onClick={event => deleteAdvancedRowField(i)}>
                            <i className="fa fa-trash"></i>
                          </button>
                        : null}
                    </div>
                  : null
                }
              </div>;
            })
          }
        </Scrollbars>
      </form>
    );
  }
}
class Blank extends Component{
  render(){
    return(
      <div></div>
    );
  }
}
