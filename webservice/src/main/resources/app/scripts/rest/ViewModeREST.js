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

import fetch from 'isomorphic-fetch';
import {
  baseUrl
} from '../utils/Constants';

import {
  CustomFetch
} from '../utils/Overrides';
const sampleModeBaseUrl = '/api/v1/catalog/topologies';
import Utils from '../utils/Utils';

const ViewModeREST = {
  getTopologyMetrics(id, fromTime, toTime, options, isStream = true) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    const streamBatch = isStream ? 'stream/' : 'batch/';
    const url = baseUrl + streamBatch + 'topologies/' + id + '/metrics?from='+fromTime+'&to='+toTime;
    return fetch(url, options)
      .then(Utils.checkStatus);
  },
  getComponentMetrics(id, compType, fromTime, toTime, options, isStream = true) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    const streamBatch = isStream ? 'stream/' : 'batch/';
    const url = baseUrl + streamBatch + 'topologies/' + id + '/'+compType+'/metrics?from='+fromTime+'&to='+toTime;
    return fetch(url, options)
      .then(Utils.checkStatus);
  },
  getTopologyLogConfig(id,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id + '/logconfig', options)
    .then((response) => {
      return response.json();
    });
  },
  postTopologyLogConfig(id,loglevel,durationSecs,options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/'+ id +'/logconfig?targetLogLevel='+loglevel+'&durationSecs='+durationSecs;
    return fetch(url, options)
    .then((response) => {
      return response.json();
    });
  },
  getTopologySamplingStatus(topologyId,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    const url = sampleModeBaseUrl+'/'+topologyId+'/sampling';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  postTopologySamplingStatus(topologyId,status,pct, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = sampleModeBaseUrl+'/'+topologyId+'/sampling/'+status;
    if(pct !== ''){
      url += '/'+pct;
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getComponentSamplingStatus(topologyId,componentId,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    const url = sampleModeBaseUrl+'/'+topologyId+'/component/'+componentId+'/sampling';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  postComponentSamplingStatus(topologyId,componentId,status,pct, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = sampleModeBaseUrl+'/'+topologyId+'/component/'+componentId+'/sampling/'+status;
    if(pct !== ''){
      url += '/'+pct;
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getSamplingEvents(topologyId,params,options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    const url = sampleModeBaseUrl+'/'+topologyId+'/events?'+params;
    return fetch(url, options)
      .then(Utils.checkStatus);
  },
  getAllExecutions(topologyId, params, options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    const q_params = jQuery.param(params, true);
    /*const url = baseUrl+'batch/topologies/'+topologyId+'/executions?'+q_params;
    return fetch(url, options)
      .then(Utils.checkStatus);*/

    const url = baseUrl + 'system/engines';
    return fetch(url, options)
      .then(Utils.checkStatus)
      .then((res) => {
        return {"totalResults":34,"executions":[{"createdAt":"2018-10-29T22:01:04","executionDate":"2018-10-26T00:00:00","status":"failed"},{"createdAt":"2018-10-29T22:01:34","executionDate":"2018-10-26T01:00:00","status":"done"},{"createdAt":"2018-10-29T22:02:04","executionDate":"2018-10-26T02:00:00","status":"done"},{"createdAt":"2018-10-29T22:02:34","executionDate":"2018-10-26T03:00:00","status":"done"},{"createdAt":"2018-10-29T22:03:05","executionDate":"2018-10-26T04:00:00","status":"done"}],"pageSize":5,"page":0};
      });
  },
  getComponentExecutions(topologyId, dateTimeStr, options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    /*const url = baseUrl+'batch/topologies/'+topologyId+'/executions/'+dateTimeStr;
    return fetch(url, options)
      .then(Utils.checkStatus);*/

    const url = baseUrl + 'system/engines';
    return fetch(url, options)
      .then(Utils.checkStatus)
      .then((res) => {
        return {"components":[{"taskRetryCount":1,"taskEndDate":"2018-10-29T22:11:06","taskRetries":0,"componentId":22,"taskDuration":1,"taskStartDate":"2018-10-29T22:11:05","executionDate":"2018-10-26T19:00:00","taskPool":"adhoc","taskStatus":"success"},{"taskRetryCount":1,"taskEndDate":"2018-10-29T22:10:50","taskRetries":0,"componentId":6,"taskDuration":0,"taskStartDate":"2018-10-29T22:10:50","executionDate":"2018-10-26T19:00:00","taskPool":"adhoc","taskStatus":"success"}]};
      });
  },
  getBatchTimeseries(topologyId, mKey, queryParams={}, options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    const q_params = jQuery.param(queryParams, true);
    const url = baseUrl+'batch/topologies/'+topologyId+'/metrics/'+ mKey +'?'+q_params;
    return fetch(url, options)
      .then(Utils.checkStatus);
  }
};
export default ViewModeREST;
