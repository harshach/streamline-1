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
    /*{
      label: 'minute',
      value: 'minute',
      prep: ''
    },*/
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
    }/*,
    {
      label: 'month',
      value: 'month',
      prep: 'on the'
    },
    {
      label: 'year',
      value: 'year',
      prep: 'on the'
    }*/
  ];
}

// display matrix
const toDisplay = {
  "minute" : [],
  "hour"   : ["min"],
  "day"    : ["time"],
  "week"   : ["dow", "time"],
  "month"  : ["dom", "time"],
  "year"   : ["dom", "mon", "time"]
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
  return `${value.min} ${value.hour} * * ${value.dow}`;
}

function getMonthCron(value) {
  return `${value.min} ${value.hour} ${value.dom} * *`;
}

function getYearCron(value) {
  return `${value.min} ${value.hour} ${value.dom} ${value.mon} *`;
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
    this._value = '0 0 * * 0';
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
      const value = event.target.value || event.target.value;
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
      const value = event.target.value || event.target.dataset.value;
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
      const value = event.target.value || event.target.dataset.value;
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
      const value = event.target.value || event.target.dataset.value;
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

  getTextComp(str){
    return <div className="cron-string">{str}</div>;
  }

  findHourOption(hour){
    const {hourOptions} = this.state;
    return hourOptions.find((h) => {
      return h.value == hour;
    });
  }

  findMinuteOption(min){
    const {minuteOptions} = this.state;
    return minuteOptions.find((h) => {
      return h.value == min;
    });
  }

  getHourSelect(selectedOption, onChangeCB){
    const {disabled} = this.props;
    const { hourOptions } = this.state;

    return (
      <select value={selectedOption.hour} onChange={onChangeCB} disabled={disabled}>
        {hourOptions.map(this.getOptionComponent('hour_option'))}
      </select>
    );
  }
  getMinuteSelect(selectedOption, onChangeCB){
    const {disabled} = this.props;
    const { minuteOptions } = this.state;

    return (
      <select value={selectedOption.min} onChange={onChangeCB} disabled={disabled}>
        {minuteOptions.map(this.getOptionComponent('minute_option'))}
      </select>
    );
  }
  getBadgeOptions(options, value, substrVal, onClick){
    const {disabled} = this.props;
    const optionComps = [];
    options.forEach((o) => {
      let strVal = o.label;
      if(substrVal){
        strVal = strVal.substr(0, substrVal);
      }
      const comp = <span
        className={`cron-badge-option ${o.value == value ? 'active' : ''} ${disabled ? 'disabled' : ''}`}
        data-value={o.value}
        onClick={disabled ? null : onClick}
      >
        {strVal}
      </span>;
      optionComps.push(comp);
    });
    return optionComps;
  }

  getMinuteComponent = (cronPeriodString) => {
    const {disabled} = this.props;
    const { minuteOptions, selectedHourOption } = this.state;

    return (
      (this.state.selectedPeriod === 'minute') &&
      <cron-minute-component>
        {this.getTextComp(`${cronPeriodString}`)}
      </cron-minute-component>
    );
  }

  getHourComponent = (cronPeriodString) => {
    const {disabled} = this.props;
    const { minuteOptions, selectedHourOption, dayOptions } = this.state;

    return (
      (this.state.selectedPeriod === 'hour') &&
      <cron-hour-component>
        <span className='m-l-xs'>Minute : </span>
        {this.getMinuteSelect(selectedHourOption, this.onHourOptionSelect('min'))}
        {this.getTextComp(`${cronPeriodString} ${selectedHourOption.min} minute past the hour`)}
      </cron-hour-component>
    );
  }

  getDayComponent = (cronPeriodString) => {
    const {disabled} = this.props;
    const { hourOptions, minuteOptions, selectedDayOption, dayOptions } = this.state;

    const hourLabel = this.findHourOption(selectedDayOption.hour).label;
    const minuteLabel = this.findMinuteOption(selectedDayOption.min).label;

    return (
      (this.state.selectedPeriod === 'day') &&
      <cron-day-component>
        <span className='m-l-xs'>Time : </span>
        {this.getHourSelect(selectedDayOption, this.onDayOptionSelect('hour'))}
        :
        {this.getMinuteSelect(selectedDayOption, this.onDayOptionSelect('min'))}
        {this.getTextComp(`${cronPeriodString} at ${hourLabel}:${minuteLabel}`)}
      </cron-day-component>
    );
  }

  getWeekComponent = (cronPeriodString) => {
    const {disabled} = this.props;
    const { hourOptions, minuteOptions, dayOptions, selectedWeekOption } = this.state;

    const hourLabel = this.findHourOption(selectedWeekOption.hour).label;
    const minuteLabel = this.findMinuteOption(selectedWeekOption.min).label;

    const dayLabel = dayOptions.find((d) => {
      return d.value == selectedWeekOption.dow;
    }).label;

    return (
      (this.state.selectedPeriod === 'week') &&
      <cron-week-component>
        <div className="cron-field-row">
          <span className='m-l-xs'>Day : </span>
          {/*<select value={selectedWeekOption.dow} onChange={this.onWeekOptionSelect('dow')} disabled={disabled}>
            {dayOptions.map(this.getOptionComponent('week_option'))}
          </select>*/}
          <div className="cron-badge-option-container week-opt-container">
            {this.getBadgeOptions(dayOptions, selectedWeekOption.dow, 1, this.onWeekOptionSelect('dow'))}
          </div>
        </div>
        <div className="cron-field-row">
          <span className='m-l-xs'>Time : </span>
          {this.getHourSelect(selectedWeekOption, this.onWeekOptionSelect('hour'))}
          :
          {this.getMinuteSelect(selectedWeekOption, this.onWeekOptionSelect('min'))}
        </div>
        {this.getTextComp(`${cronPeriodString} on ${dayLabel} at ${hourLabel}:${minuteLabel}`)}
      </cron-week-component>
    );
  }

  getMonthComponent = (cronPeriodString) => {
    const {disabled} = this.props;
    const { monthDaysOptions, hourOptions, minuteOptions, selectedMonthOption } = this.state;

    const hourLabel = this.findHourOption(selectedMonthOption.hour).label;
    const minuteLabel = this.findMinuteOption(selectedMonthOption.min).label;

    const dateLabel = monthDaysOptions.find((d) => {
      return d.value == selectedMonthOption.dom;
    }).label;

    return (
      (this.state.selectedPeriod === 'month') &&
      <cron-month-component>
        <div className="cron-field-row">
          <span className='m-l-xs'>Date : </span>
          {/*<select value={selectedMonthOption.dom} onChange={this.onMonthOptionSelect('dom')} disabled={disabled}>
            {monthDaysOptions.map(this.getOptionComponent('month_days_option'))}
          </select>*/}
          <div className="cron-badge-option-container month-opt-container">
            {this.getBadgeOptions(monthDaysOptions, selectedMonthOption.dom, 0, this.onMonthOptionSelect('dom'))}
          </div>
        </div>
        <div className="cron-field-row">
          <span className='m-l-xs'>Time : </span>
          {this.getHourSelect(selectedMonthOption, this.onMonthOptionSelect('hour'))}
          :
          {this.getMinuteSelect(selectedMonthOption, this.onMonthOptionSelect('min'))}
        </div>
        {this.getTextComp(`${cronPeriodString} on ${dateLabel} at ${hourLabel}:${minuteLabel}`)}
      </cron-month-component>
    );
  }

  getYearComponent = (cronPeriodString) => {
    const {disabled} = this.props;
    const { monthOptions, monthDaysOptions, hourOptions, minuteOptions, selectedYearOption } = this.state;

    const hourLabel = this.findHourOption(selectedYearOption.hour).label;
    const minuteLabel = this.findMinuteOption(selectedYearOption.min).label;

    const dateLabel = monthDaysOptions.find((d) => {
      return d.value == selectedYearOption.dom;
    }).label;
    const monthLabel = monthOptions.find((d) => {
      return d.value == selectedYearOption.mon;
    }).label;

    return (
      (this.state.selectedPeriod === 'year') &&
      <cron-year-component>
        <div className="cron-field-row">
          <span className='m-l-xs'>Month : </span>
          {/*<select value={selectedYearOption.mon} onChange={this.onYearOptionSelect('mon')} disabled={disabled}>
            {monthOptions.map(this.getOptionComponent('month_option'))}
          </select>*/}
          <div className="cron-badge-option-container month-opt-container">
            {this.getBadgeOptions(monthOptions, selectedYearOption.mon, 3, this.onYearOptionSelect('mon'))}
          </div>
        </div>
        <div className="cron-field-row">
          <span className='m-l-xs'>Date : </span>
          {/*<select value={selectedYearOption.dom} onChange={this.onYearOptionSelect('dom')} disabled={disabled}>
            {monthDaysOptions.map(this.getOptionComponent('month_days_option'))}
          </select>*/}
          <div className="cron-badge-option-container month-opt-container">
            {this.getBadgeOptions(monthDaysOptions, selectedYearOption.dom, 0, this.onYearOptionSelect('dom'))}
          </div>
        </div>
        <div className="cron-field-row">
          <span className='m-l-xs'>Time : </span>
          {this.getHourSelect(selectedYearOption, this.onYearOptionSelect('hour'))}
          :
          {this.getMinuteSelect(selectedYearOption, this.onYearOptionSelect('min'))}
        </div>
        {this.getTextComp(`${cronPeriodString} on ${dateLabel} of ${monthLabel} at ${hourLabel}:${minuteLabel}`)}
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
      selectedPeriod: 'week',
      selectedHourOption: {
        min: 0
      },
      selectedDayOption: {
        hour: 0,
        min: 0
      },
      selectedWeekOption: {
        dow: 1,
        hour: 0,
        min: 0
      },
      selectedMonthOption: {
        dom: 1,
        hour: 0,
        min: 0
      },
      selectedYearOption: {
        dom: 1,
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
      "mon" : d[3],
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

    const option = periodOptions.find((o) => (o.value === selectedPeriod));
    const getPeriodPrep = () => {
      return (
        <span className=''>{option.prep}</span>
      );
    };

    const startText = 'Repeat every';
    const cronPeriodString = `${startText} ${selectedPeriod}`;

    return (
      <div className={`${className} cron-row`}>
        <div className=''>
          <div className=''>
            <div className="cron-field-row">
              <span className='m-l-xs'>Every :</span>
              <select value={selectedPeriod} onChange={this.onPeriodSelect()} disabled={disabled} className='m-l-xs'>
                {periodOptions.map((t,index) => {
                  return (
                    <option key={`period_option_${index}`} value={t.value}>{t.label}</option>
                  );
                })}
              </select>
            </div>
            {/* getPeriodPrep() */}
            {this.getMinuteComponent(cronPeriodString)}
            {this.getHourComponent(cronPeriodString)}
            {this.getDayComponent(cronPeriodString)}
            {this.getWeekComponent(cronPeriodString)}
            {this.getMonthComponent(cronPeriodString)}
            {this.getYearComponent(cronPeriodString)}
          </div>
          {/*<input type='text' readOnly name='cron_tab' className='cron-input' value={getCron(this.state)}/>*/}
        </div>
      </div>
    );
  }
}