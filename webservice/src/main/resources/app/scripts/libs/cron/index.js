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
import _ from 'lodash';

function getPeriodOptions() {
  return [
    {
      label: 'minute',
      value: 'minute',
      prep: ''
    },
    {
      label: 'hour',
      value: 'hour',
      prep: 'at'
    },
    {
      label: 'day',
      value: 'day',
      prep: 'at'
    },
    {
      label: 'week',
      value: 'week',
      prep: 'on'
    },
    {
      label: 'month',
      value: 'month',
      prep: 'on the'
    },
    {
      label: 'year',
      value: 'year',
      prep: 'on the'
    }
  ];
}

// display matrix
const toDisplay = {
  "minute" : [],
  "hour"   : ["min"],
  "day"    : ["time"],
  "week"   : ["dow", "time"],
  "month"  : ["dom", "time"],
  "year"   : ["dom", "month", "time"]
};

const combinations = {
  "minute" : /^(\*\s){4}\*$/,                    // "* * * * *"
  "hour"   : /^\d{1,2}\s(\*\s){3}\*$/,           // "? * * * *"
  "day"    : /^(\d{1,2}\s){2}(\*\s){2}\*$/,      // "? ? * * *"
  "week"   : /^(\d{1,2}\s){2}(\*\s){2}\d{1,2}$/, // "? ? * * ?"
  "month"  : /^(\d{1,2}\s){3}\*\s\*$/,           // "? ? ? * *"
  "year"   : /^(\d{1,2}\s){4}\*$/                // "? ? ? ? *"
};


function getRange(n) {
  return [...Array(n).keys()];
}

function getRangeOptions(n) {
  return getRange(n).map((v) => {
    return {
      label: `0${v}`.slice(-2),
      value: v
    };
  });
}

function getMinuteOptions() {
  return getRangeOptions(60);
}

function getHourOptions() {
  return getRangeOptions(24);
}

function ordinalSuffix(n) {
  const suffixes = ['th', 'st', 'nd', 'rd'];
  const val = n%100;

  return `${n}${suffixes[(val-20)%10] || suffixes[val] || suffixes[0]}`;
}

function getDayOptions() {
  return [
    {
      label: 'Sunday',
      value: 0
    },
    {
      label: 'Monday',
      value: 1
    },
    {
      label: 'Tuesday',
      value: 2
    },
    {
      label: 'Wednesday',
      value: 3
    },
    {
      label: 'Thursday',
      value: 4
    },
    {
      label: 'Friday',
      value: 5
    },
    {
      label: 'Saturday',
      value: 6
    }
  ];
}

function getMonthDaysOptions() {
  return getRange(31).map((v) => {
    return {
      label: ordinalSuffix(v + 1),
      value: v + 1
    };
  });
}

function monthsList() {
  return [ 'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];
}

function getMonthOptions() {
  return monthsList().map((m, index) => {
    return {
      label: m,
      value: index + 1
    };
  });
}

function getMinuteCron(value) {
  return '* * * * *';
}

function getHourCron(value) {
  return `${value.min} * * * *`;
}

function getDayCron(value) {
  return `${value.min} ${value.hour} * * *`;
}

function getWeekCron(value) {
  return `${value.min} ${value.hour} * * ${value.day}`;
}

function getMonthCron(value) {
  return `${value.min} ${value.hour} ${value.day} * *`;
}

function getYearCron(value) {
  return `${value.min} ${value.hour} ${value.day} ${value.mon} *`;
}

function getCron(state) {
  const { selectedPeriod, selectedHourOption, selectedDayOption,
          selectedWeekOption, selectedMonthOption, selectedYearOption } = state;

  switch (selectedPeriod) {
  case 'minute':
    return getMinuteCron({});
  case 'hour':
    return getHourCron(selectedHourOption);
  case 'day':
    return getDayCron(selectedDayOption);
  case 'week':
    return getWeekCron(selectedWeekOption);
  case 'month':
    return getMonthCron(selectedMonthOption);
  case 'year':
    return getYearCron(selectedYearOption);
  default:
    return '* * * * *';
  }
}

export default class ReactCron extends Component {
  static propTypes = {
    className: PropTypes.string
  }

  constructor(props){
    super(props);
    this._value = '* * * * *';
    if(props.value){
      this._value = props.value;
    }else{
      props.onChange(this._value);
    }
    const stateValue = this.getStateValue(this._value);

    this.state = {
      ...stateValue,
      periodOptions: getPeriodOptions(),
      minuteOptions: getMinuteOptions(),
      hourOptions: getHourOptions(),
      dayOptions: getDayOptions(),
      monthDaysOptions: getMonthDaysOptions(),
      monthOptions: getMonthOptions()
    };
  }

  onPeriodSelect = () => {
    return (event) => {
      this.setState({
        selectedPeriod: event.target.value
      }, this.changeValue);
    };
  }

  onHourOptionSelect = (key) => {
    return (event) => {
      const value = event.target.value;
      const obj = {};
      obj[key] = value;
      const { selectedHourOption } = this.state;
      const hourOption = Object.assign({}, selectedHourOption, obj);
      this.setState({
        selectedHourOption: hourOption
      }, this.changeValue);
    };
  }

  onDayOptionSelect = (key) => {
    return (event) => {
      const value = event.target.value;
      const obj = {};
      obj[key] = value;
      const { selectedDayOption } = this.state;
      const dayOption = Object.assign({}, selectedDayOption, obj);
      this.setState({
        selectedDayOption: dayOption
      }, this.changeValue);
    };
  }

  onWeekOptionSelect = (key) => {
    return (event) => {
      const value = event.target.value;
      const obj = {};
      obj[key] = value;
      const { selectedWeekOption } = this.state;
      const weekOption = Object.assign({}, selectedWeekOption, obj);
      this.setState({
        selectedWeekOption: weekOption
      }, this.changeValue);
    };
  }

  onMonthOptionSelect = (key) => {
    return (event) => {
      const value = event.target.value;
      const obj = {};
      obj[key] = value;
      const { selectedMonthOption } = this.state;
      const monthOption = Object.assign({}, selectedMonthOption, obj);
      this.setState({
        selectedMonthOption: monthOption
      }, this.changeValue);
    };
  }

  onYearOptionSelect = (key) => {
    return (event) => {
      const value = event.target.value;
      const obj = {};
      obj[key] = value;
      const { selectedYearOption } = this.state;
      const yearOption = Object.assign({}, selectedYearOption, obj);
      this.setState({
        selectedYearOption: yearOption
      }, this.changeValue);
    };
  }

  getOptionComponent = (key) => {
    return (o, i) => {
      return (
        <option key={`${key}_${i}`} value={o.value}>{o.label}</option>
      );
    };
  }

  getHourComponent = () => {
    const {disabled} = this.props;
    const { minuteOptions, selectedHourOption } = this.state;

    return (
      (this.state.selectedPeriod === 'hour') &&
      <cron-hour-component>
        <select value={selectedHourOption.min} onChange={this.onHourOptionSelect('min')} disabled={disabled} className='m-r-xs'>
          {minuteOptions.map(this.getOptionComponent('minute_option'))}
        </select>
        minutes past the hour
      </cron-hour-component>
    );
  }

  getDayComponent = () => {
    const {disabled} = this.props;
    const { hourOptions, minuteOptions, selectedDayOption } = this.state;

    return (
      (this.state.selectedPeriod === 'day') &&
      <cron-day-component>
        <select value={selectedDayOption.hour} onChange={this.onDayOptionSelect('hour')} disabled={disabled}>
          {hourOptions.map(this.getOptionComponent('hour_option'))}
        </select>
        :
        <select value={selectedDayOption.min} onChange={this.onDayOptionSelect('min')}>
          {minuteOptions.map(this.getOptionComponent('minute_option'))}
        </select>
      </cron-day-component>
    );
  }

  getWeekComponent = () => {
    const {disabled} = this.props;
    const { hourOptions, minuteOptions, dayOptions, selectedWeekOption } = this.state;

    return (
      (this.state.selectedPeriod === 'week') &&
      <cron-week-component>
        <select value={selectedWeekOption.day} onChange={this.onWeekOptionSelect('day')} disabled={disabled}>
          {dayOptions.map(this.getOptionComponent('week_option'))}
        </select>
        <span className='m-l-xs m-r-xs'>at</span>
        <select value={selectedWeekOption.hour} onChange={this.onWeekOptionSelect('hour')} disabled={disabled}>
          {hourOptions.map(this.getOptionComponent('hour_option'))}
        </select>
        :
        <select value={selectedWeekOption.min} onChange={this.onWeekOptionSelect('min')} disabled={disabled}>
          {minuteOptions.map(this.getOptionComponent('minute_option'))}
        </select>
      </cron-week-component>
    );
  }

  getMonthComponent = () => {
    const {disabled} = this.props;
    const { monthDaysOptions, hourOptions, minuteOptions, selectedMonthOption } = this.state;

    return (
      (this.state.selectedPeriod === 'month') &&
      <cron-month-component>
        <select value={selectedMonthOption.day} onChange={this.onMonthOptionSelect('day')} disabled={disabled}>
          {monthDaysOptions.map(this.getOptionComponent('month_days_option'))}
        </select>
        <span className='m-l-xs m-r-xs'>at</span>
        <select value={selectedMonthOption.hour} onChange={this.onMonthOptionSelect('hour')} disabled={disabled}>
          {hourOptions.map(this.getOptionComponent('hour_option'))}
        </select>
        :
        <select value={selectedMonthOption.min} onChange={this.onMonthOptionSelect('min')} disabled={disabled}>
          {minuteOptions.map(this.getOptionComponent('minute_option'))}
        </select>
      </cron-month-component>
    );
  }

  getYearComponent = () => {
    const {disabled} = this.props;
    const { monthOptions, monthDaysOptions, hourOptions, minuteOptions, selectedYearOption } = this.state;

    return (
      (this.state.selectedPeriod === 'year') &&
      <cron-year-component>
        <select value={selectedYearOption.day} onChange={this.onYearOptionSelect('day')} disabled={disabled}>
          {monthDaysOptions.map(this.getOptionComponent('month_days_option'))}
        </select>
        <span className='m-l-xs m-r-xs'>of</span>
        <select value={selectedYearOption.mon} onChange={this.onYearOptionSelect('mon')} disabled={disabled}>
          {monthOptions.map(this.getOptionComponent('month_option'))}
        </select>
        <span className='m-l-xs m-r-xs'>at</span>
        <select value={selectedYearOption.hour} onChange={this.onYearOptionSelect('hour')} disabled={disabled}>
          {hourOptions.map(this.getOptionComponent('hour_option'))}
        </select>
        :
        <select value={selectedYearOption.min} onChange={this.onYearOptionSelect('min')} disabled={disabled}>
          {minuteOptions.map(this.getOptionComponent('minute_option'))}
        </select>
      </cron-year-component>
    );
  }

  getValue(){
    return getCron(this.state);
  }

  getCronType(cron_str) {
    for (var t in combinations) {
      if (combinations[t].test(cron_str)) { return t; }
    }
    return undefined;
  }

  getStateValue(valueStr){
    var stateVal = {
      selectedPeriod: 'minute',
      selectedHourOption: {
        min: 0
      },
      selectedDayOption: {
        hour: 0,
        min: 0
      },
      selectedWeekOption: {
        day: 1,
        hour: 0,
        min: 0
      },
      selectedMonthOption: {
        day: 1,
        hour: 0,
        min: 0
      },
      selectedYearOption: {
        day: 1,
        mon: 1,
        hour: 0,
        min: 0
      }
    };
    var t = this.getCronType(valueStr);
    
    var d = valueStr.split(" ");
    var v = {
      "min"  : d[0],
      "hour"  : d[1],
      "dom"   : d[2],
      "month" : d[3],
      "dow"   : d[4]
    };

    stateVal.selectedPeriod = t || stateVal.selectedPeriod;

    const selectedPeriodObj = stateVal['selected'+(t.charAt(0).toUpperCase() + t.substr(1))+'Option'];

    // update appropriate select boxes
    var targets = toDisplay[t];
    for (var i = 0; i < targets.length; i++) {
      var tgt = targets[i];
      if (tgt == "time") {
        selectedPeriodObj.hour = v.hour;
        selectedPeriodObj.min = v.min;
        // var btgt = block[tgt].find("select.cron-time-hour").val(v["hour"]);

        // btgt = block[tgt].find("select.cron-time-min").val(v["mins"]);
      } else {
        selectedPeriodObj[tgt] = v[tgt];
        // var btgt = block[tgt].find("select").val(v[tgt]);
      }
    }
    
    // trigger change event
    // var bp = block["period"].find("select").val(t);
    return stateVal;
  }

  setValue(valueStr){
    if (!valueStr) { return getCron(this.state); }

    const stateVal = this.getStateValue(valueStr);
    this.setState(stateVal, this.changeValue);
  }

  changeValue() {
    const {onChange} = this.props;
    this._value = getCron(this.state);
    onChange(this._value);
  }

  render() {
    const { className, disabled } = this.props;
    const { selectedPeriod, periodOptions } = this.state;

    const getPeriodPrep = () => {
      const option = periodOptions.find((o) => (o.value === selectedPeriod));
      return (
        <span className='m-l-xs m-r-xs'>{option.prep}</span>
      );
    };

    return (
      <div className={`${className} cron-row`}>
        <div className=''>
          <div className=''>
            Every
            <select value={selectedPeriod} onChange={this.onPeriodSelect()} disabled={disabled} className='m-l-xs'>
              {periodOptions.map((t,index) => {
                return (
                  <option key={`period_option_${index}`} value={t.value}>{t.label}</option>
                );
              })}
            </select>
            {getPeriodPrep()}
            {this.getHourComponent()}
            {this.getDayComponent()}
            {this.getWeekComponent()}
            {this.getMonthComponent()}
            {this.getYearComponent()}
          </div>
          {/*<input type='text' readOnly name='cron_tab' className='cron-input' value={getCron(this.state)}/>*/}
        </div>
      </div>
    );
  }
}