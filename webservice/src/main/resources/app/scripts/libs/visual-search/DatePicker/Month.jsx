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
import {Button} from 'react-bootstrap';
import moment from 'moment';

const months = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "June",
  "July",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec"
];

export default class Month extends Component {

  constructor(props) {
    super(props);
    this.state = {};
    this.setYear(props);
  }

  setYear = (props) => {
    let year = props.date.year();
    this.state.year = year;
  }

  componentWillReceiveProps(nextProps) {
    this.setYear(nextProps);
  }

  onPrevBtnClick = (e) => {
    e.stopPropagation();
    this.context.DatePicker.context.InputDate.setCurrentDate = false;
    let year = this.state.year - 1;
    this.setState({year: year});
  }

  onNextBtnClick = (e) => {
    e.stopPropagation();
    this.context.DatePicker.context.InputDate.setCurrentDate = false;
    let year = this.state.year + 1;
    this.setState({year: year});
  }

  onYearClick = (e) => {
    e.stopPropagation();
    this.context.DatePicker.context.InputDate.setCurrentDate = false;
    this.props.setMode("Year");
  }

  onMonthClick = (e) => {
    e.stopPropagation();
    this.context.DatePicker.context.InputDate.setCurrentDate = false;
    let node = null;
    if (e.target.hasAttribute("data-month")) {
      node = e.target;
    } else if (e.target.querySelector('[data-month]') !== null) {
      node = e.target.querySelector('[data-month]');
    }
    if (node !== null && node.hasAttribute("data-month")) {
      let month = parseInt(node.getAttribute("data-month"));
      let selectedDate = moment("01/" + (
      month + 1) + "/" + this.state.year, "DD/MM/YYYY");
      if (this.context.DatePicker.props.mode.toLowerCase() === "month") {
        this.context.DatePicker.setValue(selectedDate);
      } else {
        this.context.DatePicker.setState({date: selectedDate, mode: "days"});
      }
    }
  }

  render() {
    return (<div>
      <div className="pickerheader">
        <div className="col-md-2">
          {
            (this.state.year - 1) > 1800
              ? <span className="spanbtn" onMouseDown={this.onPrevBtnClick}>{"<"}</span>
              : null
          }
        </div>
        <div className="col-md-8">
          <div className="text-center">
            <span className="spanbtn" onMouseDown={this.onYearClick}>{this.state.year}</span>
          </div>
        </div>
        <div className="col-md-2">
          {
            (this.state.year + 1) < 2300
              ? <span className="spanbtn" onMouseDown={this.onNextBtnClick}>{">"}</span>
              : null
          }
        </div>
      </div>
      <div className="text-center" onMouseDown={this.onMonthClick}>
        {
          months.map((month, i) => {
            return (<div className="col-md-4 pickermonth" key={i}>
              <span data-month={i}>{month}</span>
            </div>);
          })
        }
      </div>
    </div>);
  }
}

Month.contextTypes = {
  DatePicker: PropTypes.object
};
