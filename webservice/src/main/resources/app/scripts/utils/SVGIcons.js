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
import React from 'react';

const folder = (isClicked) =>{
  return <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" title="folder">
  <path className="path1" fillRule="evenodd" clipRule="evenodd" d="M10.0012 3.99976H4.00122C2.90122 3.99976 2.01122 4.89976 2.01122 5.99976L2.00122 17.9998C2.00122 19.0998 2.90122 19.9998 4.00122 19.9998H20.0012C21.1012 19.9998 22.0012 19.0998 22.0012 17.9998V7.99976C22.0012 6.89976 21.1012 5.99976 20.0012 5.99976H12.0012L10.0012 3.99976Z" fill={isClicked ? "#1B6DE0" : "#999999"}/>
 </svg>;
};

const editIcon = <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M9.59991 4.40015L11.5999 6.40015L3.33331 14.6668H1.33331V12.6668L9.59991 4.40015Z" fill="#999999"/>
                    <path d="M12.714 1.32617L11.0641 2.97607L13.044 4.95595L14.6939 3.30605L12.714 1.32617Z" fill="#999999"/>
                </svg>;

const graphIcon = <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path fillRule="evenodd" clipRule="evenodd" d="M19 3H5C3.9 3 3 3.9 3 5V19C3 20.1 3.9 21 5 21H19C20.1 21 21 20.1 21 19V5C21 3.9 20.1 3 19 3ZM8.99902 17.0001H6.99902V10.0001H8.99902V17.0001ZM13.001 17.0001H11.001V7.00012H13.001V17.0001ZM17 17.0001H15V13.0001H17V17.0001Z" fill="#999999"/>
                  </svg>;

const shareIcon = <svg width="12" height="14" viewBox="0 0 12 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path fillRule="evenodd" clipRule="evenodd" d="M10 9.72016C9.49333 9.72016 9.04 9.92016 8.69333 10.2335L3.94 7.46683C3.97333 7.3135 4 7.16016 4 7.00016C4 6.84016 3.97333 6.68683 3.94 6.5335L8.64 3.7935C9 4.12683 9.47333 4.3335 10 4.3335C11.1067 4.3335 12 3.44016 12 2.3335C12 1.22683 11.1067 0.333496 10 0.333496C8.89333 0.333496 8 1.22683 8 2.3335C8 2.4935 8.02667 2.64683 8.06 2.80016L3.36 5.54016C3 5.20683 2.52667 5.00016 2 5.00016C0.893333 5.00016 0 5.8935 0 7.00016C0 8.10683 0.893333 9.00016 2 9.00016C2.52667 9.00016 3 8.7935 3.36 8.46016L8.10667 11.2335C8.07333 11.3735 8.05333 11.5202 8.05333 11.6668C8.05333 12.7402 8.92667 13.6135 10 13.6135C11.0733 13.6135 11.9467 12.7402 11.9467 11.6668C11.9467 10.5935 11.0733 9.72016 10 9.72016Z" fill="#999999"/>
                  </svg>;

const trashIcon = <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path fillRule="evenodd" clipRule="evenodd" d="M5.99988 18.9997C5.99988 20.0997 6.89988 20.9997 7.99988 20.9997H15.9999C17.0999 20.9997 17.9999 20.0997 17.9999 18.9997V6.9997H5.99988V18.9997ZM18.9999 4H15.4999L14.4999 3H9.49988L8.49988 4H4.99988V6H18.9999V4Z" fill="#999999"/>
                  </svg>;

const clockIcon = <svg className="dropdown-icon" height="16" viewBox="0 0 17 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M8.75 0.75C4.175 0.75 0.5 4.425 0.5 9C0.5 13.575 4.175 17.25 8.75 17.25C13.325 17.25 17 13.575 17 9C17 4.425 13.325 0.75 8.75 0.75ZM13.25 10.5H7.25V3H9.5V8.25H13.25V10.5Z" fill="#666666"/>
                  </svg>;

const dbIcon = <svg className="dropdown-icon" height="16" viewBox="0 0 19 19" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9.49984 0.791687C4.67067 0.791687 0.791504 3.08752 0.791504 5.93752C0.791504 5.93752 0.791504 10.2125 0.791504 13.0625C0.791504 15.9125 4.67067 18.2084 9.49984 18.2084C14.329 18.2084 18.2082 15.9125 18.2082 13.0625C18.2082 10.8459 18.2082 5.93752 18.2082 5.93752C18.2082 3.08752 14.329 0.791687 9.49984 0.791687ZM9.49984 3.16669C12.9832 3.16669 15.8332 4.75002 15.8332 5.93752C15.8332 7.60002 11.8748 8.70835 9.49984 8.70835C6.0165 8.70835 3.1665 7.12502 3.1665 5.93752C3.1665 4.27502 7.12484 3.16669 9.49984 3.16669Z" fill="#666666"/>
                </svg>;

export default {
  editIcon,
  graphIcon,
  shareIcon,
  trashIcon,
  clockIcon,
  dbIcon,
  folder
};
