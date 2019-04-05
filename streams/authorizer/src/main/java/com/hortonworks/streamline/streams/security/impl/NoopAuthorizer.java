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
package com.hortonworks.streamline.streams.security.impl;

import com.hortonworks.streamline.streams.security.AuthenticationContext;
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.catalog.AclEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class NoopAuthorizer implements StreamlineAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(NoopAuthorizer.class);

    @Override
    public void init(Map<String, Object> config) {
    }

    @Override
    public boolean hasPermissions(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, EnumSet<Permission> permissions) {
        LOG.debug("NoopAuthorizer hasPermissions, AuthenticationContext: {}, targetEntityNamespace: {}, targetEntityId: {}, " +
                "permissions: {}", ctx, targetEntityNamespace, targetEntityId, permissions);
        return true;
    }

    @Override
    public boolean hasPermissions(AuthenticationContext ctx, Set<String> userGroups, String targetEntityNamespace, Long targetEntityId, EnumSet<Permission> permissions) {
        LOG.debug("NoopAuthorizer hasPermissions, AuthenticationContext: {}, targetEntityNamespace: {}, targetEntityId: {}, " +
                "permissions: {}", ctx, targetEntityNamespace, targetEntityId, permissions);
        return true;
    }

    @Override
    public boolean hasRole(AuthenticationContext ctx, String role) {
        LOG.debug("NoopAuthorizer hasRole, AuthenticationContext: {}, Role: {}", ctx, role);
        return true;
    }

    @Override
    public void addAcl(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, boolean owner, boolean grant, EnumSet<Permission> permissions) {
        LOG.debug("NoopAuthorizer addAcl, AuthenticationContext: {}, targetEntityNamespace: {}, targetEntityId: {}, " +
                "permissions: {}", ctx, targetEntityNamespace, targetEntityId, permissions);
    }

    @Override
    public AclEntry addAcl(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, Long userId, EnumSet<Permission> permissions) {
        LOG.debug("NoopAuthorizer addAcl, AuthenticationContext: {}, targetEntityNamespace: {}, targetEntityId: {}, " +
                "permissions: {}", ctx, targetEntityNamespace, targetEntityId, permissions);
        throw new UnsupportedOperationException("Not implemented, yet");
    }

    @Override
    public AclEntry addAcl(String targetEntityNamespace, Long targetEntityId, AclEntry.SidType sidType, Long sidId, EnumSet<Permission> permissions) {
        LOG.debug("NoopAuthorizer addAcl, targetEntityNamespace: {}, targetEntityId: {}, " +
                "permissions: {}", targetEntityNamespace, targetEntityId, permissions);
        throw new UnsupportedOperationException("Not implemented, yet");
    }

        @Override
    public void removeAcl(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId) {
        LOG.debug("NoopAuthorizer removeAcl, AuthenticationContext: {}, targetEntityNamespace: {}, targetEntityId: {}",
                ctx, targetEntityNamespace, targetEntityId);
    }

}
