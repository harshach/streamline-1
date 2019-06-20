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
import {Overlay, Popover} from 'react-bootstrap';

import ValueComponent from './ValueComponent';
import DatePicker from './DatePicker/DatePicker';

export default class InputDate extends Component {

  constructor(props){
    super(props);
    this.state = {
      showInput: true,
      showOptions: false,
      inputValue: ""
    };
    this.setCurrentDate = true;
    this.setData(this.props);
  }

  getChildContext(){
    return {InputDate:this};
  }

  onFocus = () => {
    this.setCurrentDate = true;
    this.setState({
      showOptions: true
    });
  }

  onBlur = () => {
    if(this.state.inputValue === "" && this.setCurrentDate){
      this.DatePickerComp.setCurrentDate();
    } else if(this.setCurrentDate){
      this.setState({
        showOptions: false,
        showInput: false
      });
    }
  }

  onChange = () => {
  }

  onKeyPress = (e) => {
    if(e.which === 13) {
      this.setState({
        showInput: false
      });
    }
  }

  componentDidMount(){
    if(this.state.showInput){
      this.inputDateTarget.focus();
    }
  }

  setData = (props) => {
    this.state.data = props.data;
    if (props.data.hasOwnProperty("value")) {
      this.state.showInput = false;
      this.state.inputValue = props.data.value;
    }
  }

  setValue = (value) => {
    let data = this.state.data;
    data.value = value;
    this.setState({
      data: data,
      inputValue: value,
      showInput: false
    },()=>{
      this.props.onUpdateFilter();
    });
  }

  onValueClick = () => {
    this.setState({
      showInput: true,
      showOptions: true
    });
  }

  renderInput = () => {
    if(this.state.showInput){
      return (<span>
        <input
          ref= {(input)=>{this.inputDateTarget = input;}}
          className= "input_text_value"
          key= "input_date"
          value= {this.state.inputValue || ""}
          onChange= {this.onChange}
          onKeyPress= {this.onKeyPress}
          onFocus= {this.onFocus}
          onBlur= {this.onBlur}
        >
        </input>
        {this.state.showOptions ?
          <Overlay
            key="dateoverlay"
            show={this.state.showOptions}
            target={() => ReactDOM.findDOMNode(this.inputDateTarget)}
            placement="bottom"
            container={this.context.searchComponent}
          >
            <Popover id="input_date_options" className="popover_options">
              <div className="date_wrapper">
                <DatePicker ref={(datepicker)=>{this.DatePickerComp= datepicker;}} data={this.state.data} setValue={this.setValue} {...this.state.data.options || {}} />
              </div>
            </Popover>
          </Overlay>
        :null}
      </span>);
    } else {
      return (
        <ValueComponent value={this.state.data.value} onValueClick={this.onValueClick} />
      );
    }
  }

  render(){
    return (this.renderInput());
  }
}

InputDate.contextTypes = {
  searchComponent: PropTypes.object
};

InputDate.childContextTypes = {
  InputDate: PropTypes.object
};
