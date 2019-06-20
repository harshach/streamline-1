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
import ReactDOM from 'react-dom';
import {Overlay, Popover} from 'react-bootstrap';
import _ from 'lodash';

export default class InputSearch extends Component {

  constructor(props){
    super(props);
    this.state = {
      showOptions: false,
      inputSearchValue: "",
      selectedOptions: [],
      cursor : -1
    };
  }

  hideOptions = () => {
    if (this.state.showOptions) {
      this.setState({showOptions: false});
    }
  }

  componentDidMount() {
    document.addEventListener('click', this.handleOutsideClick, false);
  }

  componentWillUnmount() {
    document.removeEventListener('click', this.handleOutsideClick, false);
  }

  handleOutsideClick = (e) => {
    if (this.state.showOptions) {
      let flag = true;
      let element = e.target.parentElement;
      while (flag) {
        if (element === null) {
          flag = false;
          if (e.target.nodeName === "HTML") {
            this.hideOptions();
          }
          return;
        }
        if (element.classList.contains("visual_search")) {
          flag = false;
        } else if (element.nodeName === "BODY") {
          flag = false;
          this.hideOptions();
        }
        element = element.parentElement;
      }
    }
  }

  onInputFocus = () => {
    this.setState({
      showOptions: true
    });
  }

  onInputKeyDown = (e) => {
    let {cursor} = this.state;
    if(e.which === 8 && e.target.value === "" && this.context.visualSearch.state.selectedValue.length > 0){
      this.props.onBackspaceRemove();
    }else if(e.which === 27 && e.target.value === "" && this.context.visualSearch.state.selectedValue.length >= 0){
      this.setState({
        //on Escape Button
        showOptions : false
      });
    }else if (e.which === 38 && cursor > 0) {
      this.setState( prevState => ({
        //on Up Arrow Key
        cursor: prevState.cursor - 1
      }));
    } else if (e.which === 40 && cursor < this.context.visualSearch.props.category.length -1 ) {
      this.setState( prevState => ({
        //On Down Arrow Key
        cursor: prevState.cursor + 1,
        showOptions : true
      }));
    }
  }

  onValueSearch = (e) => {
    // if (this.context.visualSearch.props.filterOptions) {
    let value = e.target.value;
    this.setState({
      inputSearchValue: value,
      showOptions: true
    });
    // }
  }

  onInputKeyPress = (e) => {
    let {cursor} = this.state;
    let selectedOptions = this.getSelectedOptions();
    if (e.which === 13 && this.state.inputSearchValue !== "") {
      if (selectedOptions.length > 0) {
        this.onOptionClick(selectedOptions[0]);
      }
    }else if(e.which === 13 && this.state.inputSearchValue === ""){
      this.onOptionClick(selectedOptions[cursor]);
    }
  }

  getSelectedOptions = () => {
    let options = this.context.visualSearch.props.category;
    let selectedOptions = options;
    if (this.context.visualSearch.props.removeOnSelect && this.context.visualSearch.state.selectedValue.length > 0) {
      selectedOptions = _.filter(selectedOptions, (option) => {
        let obj = _.find(this.context.visualSearch.state.selectedValue, {name: option.name});
        return obj === undefined;
      });
    }
    // if (this.context.visualSearch.props.filterOptions) {
    if (this.state.inputSearchValue !== "") {
      selectedOptions = _.filter(selectedOptions, (option) => {
        return option.label.toLowerCase().indexOf(this.state.inputSearchValue.toLowerCase()) >= 0;
      });
    }
    // }
    return selectedOptions;
  }

  renderOptions = () => {
    let {cursor} = this.state;
    let selectedOptions = this.getSelectedOptions();
    if (selectedOptions.length > 0) {
      return selectedOptions.map((option, i) => {
        let optionClass = "option";
        if (this.context.visualSearch.props.removeOnSelect === false) {
          let opt = _.find(this.context.visualSearch.state.selectedValue, {name: option.name});
          if (opt !== undefined) {
            optionClass += " selected";
          }
        }
        return (
          <span key={i} className={cursor === i ? `${optionClass} active` : optionClass} onClick={()=>{this.onOptionClick(option);}}>{option.label}</span>
        );
      });
    } else {
      return (<span className="option">No Record's Found !!!</span>);
    }
  }

  onOptionClick = (option) => {
    this.props.onOptionClick(option);
    this.setState({
      showOptions: false,
      inputSearchValue: ""
    });
  }

  render(){
    return(<span>
      <input className="search_input" placeholder="Search"
          key="search_input" onClick={this.onInputFocus} autoFocus
          value={this.state.inputSearchValue} onChange={this.onValueSearch} onKeyDown={this.onInputKeyDown} onKeyPress={this.onInputKeyPress}/>
        {this.state.showOptions ?
        <Overlay
          key="overlay"
          show={this.state.showOptions}
          target={() => ReactDOM.findDOMNode(this.context.visualSearch.searchTarget)}
          placement="bottom"
          container={this.context.visualSearch}
          >
          <Popover id="visual_search_option_list">
            <div className="options_wrapper">
              {
                this.renderOptions()
              }
            </div>
          </Popover>
        </Overlay>
        :null}
    </span>
    );
  }
}


InputSearch.contextTypes = {
  visualSearch: PropTypes.object
};
