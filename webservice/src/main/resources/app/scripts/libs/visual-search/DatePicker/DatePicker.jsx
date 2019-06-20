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
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import moment from 'moment';

import Year from './Year';
import Month from './Month';
import Days from './Days';

export default class DatePicker extends Component {

  constructor(props) {
    super(props);
    this.state = {};
    this.setData(props);
  }

  getChildContext(){
    return {
      DatePicker: this
    };
  }

  setData = (props) => {
    this.state.mode = props.mode;
    this.state.date = moment();
    if (props.data.hasOwnProperty("value")) {
      this.state.date = moment(props.data.value, props.format);
    }
  }

  setMode = (mode) => {
    this.setState({mode: mode});
  }

  setDate = (date) => {
    this.setState({date: date});
  }

  setValue = (date) => {
    let value = date.format(this.props.format);
    this.props.setValue(value);
  }

  setCurrentDate = () => {
    let date = moment();
    this.setValue(date);
  }

  renderDate = () => {
    if (this.state.mode.toLowerCase() === "year") {
      return (<Year date={this.state.date} setDate={this.setDate}/>);
    } else if (this.state.mode.toLowerCase() === "month") {
      return (<Month date={this.state.date} setMode={this.setMode} setDate={this.setDate}/>);
    } else {
      return (<Days date={this.state.date} setMode={this.setMode} setDate={this.setDate}/>);
    }
  }

  render() {
    return (<div className="">
      {this.renderDate()}
    </div>);
  }
}

DatePicker.defaultProps = {
  format: "DD/MM/YYYY",
  mode: "days"
};

DatePicker.childContextTypes = {
  DatePicker: PropTypes.object
};

DatePicker.contextTypes = {
  InputDate: PropTypes.object
};
