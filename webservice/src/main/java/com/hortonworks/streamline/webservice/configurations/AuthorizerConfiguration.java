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
package com.hortonworks.streamline.webservice.configurations;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.Set;

public class AuthorizerConfiguration {
    @NotEmpty
    private String className;

    @NotEmpty
    private Set<String> adminPrincipals;

    private String containerRequestFilter;

    @NotEmpty
    private String principalDomain;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Set<String> getAdminPrincipals() {
        return adminPrincipals;
    }

    public void setAdminPrincipals(Set<String> adminPrincipals) {
        this.adminPrincipals = adminPrincipals;
    }

    public String getContainerRequestFilter() {
        return containerRequestFilter;
    }

    public void setContainerRequestFilter(String containerRequestFilter) {
        this.containerRequestFilter = containerRequestFilter;
    }

    public String getPrincipalDomain() {
        return principalDomain;
    }

    public void setPrincipalDomain(String principalDomain) {
        this.principalDomain = principalDomain;
    }
}
