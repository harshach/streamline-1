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

import InputText from './InputText';
import InputList from './InputList';
import InputDate from './InputDate';

export default class SearchComponent extends Component {
  constructor(props) {
    super(props);
  }

  onCancelClick = () => {
    this.props.onCancelClick(this.props.index);
  }

  getChildContext = () => {
    return {
      searchComponent: this
    };
  }

  renderType = (data) => {
    let type = data.type;
    let defaultProps = {
      onUpdateFilter: this.context.visualSearch.onUpdateFilter
    };
    switch (type) {
    case "text":
      return (
          <InputText data={data} {...defaultProps} />
      );
      break;
    case "list":
      return (
        <InputList data={data} {...defaultProps} />
      );
    case "date":
      return (
        <InputDate data={data} {...defaultProps} />
      );
    default: throw('Please set valid Type');
    }
  }

  render(){
    return(
      <span className="search_value_wrapper">
        <span className="search-label">{this.props.data.label + " : "}</span>
        {
          this.renderType(this.props.data)
        }
        <span className="value_close" onMouseDown={this.onCancelClick}><i className="fa fa-close"></i></span>
      </span>
    );
  }
}

SearchComponent.contextTypes = {
  visualSearch: PropTypes.object
};

SearchComponent.childContextTypes = {
  searchComponent: PropTypes.object
};
