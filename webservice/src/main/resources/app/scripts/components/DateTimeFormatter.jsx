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
import Cleave from 'cleave.js/dist/cleave-react';

class DateTimeFormatter extends Component {
  constructor(props){
    super(props);
    this.state = {
      date: props.value.date ? props.value.date : '',
      time: props.value.time ? props.value.time : ''
    };
    this.handleChangeOnDate = this.handleChangeOnDate.bind(this);
    this.handleChangeOnTime = this.handleChangeOnTime.bind(this);
  }

  handleChangeOnDate = (date) => {
    this.setState({date: date.target.value}, () => {this.props.callBack(this.state);});
  }
  handleChangeOnTime = (date) => {
    this.setState({time: date.target.value}, () => {this.props.callBack(this.state);});
  }

  render(){
    return(
      <div className="row datepicker-contriner">
        <div>
          <Cleave
            className="form-control"
            placeholder="MM/DD/YY"
            options={{date: true, datePattern: ['m', 'd', 'y']}}
            onChange={this.handleChangeOnDate}
            value={this.state.date}
          />
          <span className="help-block">MM/DD/YY</span>
        </div>
        <div>
          <Cleave className="form-control" placeholder="00:00:00" options={{time: true, timePattern: ['h', 'm', 's']}}
                          onChange={this.handleChangeOnTime}
                          value={this.state.time}/>
          <span className="help-block">HH/MM/SS</span>
        </div>
      </div>
    );
  }
}

export default DateTimeFormatter;
