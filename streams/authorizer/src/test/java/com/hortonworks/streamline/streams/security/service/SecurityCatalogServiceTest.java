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
package com.hortonworks.streamline.streams.security.service;

import com.google.common.collect.Sets;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.streams.catalog.Project;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.catalog.AclEntry;
import com.hortonworks.streamline.streams.security.catalog.OwnerGroup;
import com.hortonworks.streamline.streams.security.catalog.Role;
import com.hortonworks.streamline.streams.security.catalog.User;
import mockit.Expectations;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.hortonworks.streamline.streams.security.catalog.AclEntry.SidType.USER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecurityCatalogServiceTest {

    @Test
    public void checkUserPermissions() throws Exception {
        SecurityCatalogService catalogService = new SecurityCatalogService(null);
        AclEntry userAclEntry = new AclEntry();
        userAclEntry.setSidType(AclEntry.SidType.USER);
        userAclEntry.setSidId(1L);
        userAclEntry.setObjectId(1L);
        userAclEntry.setObjectNamespace("topology");
        userAclEntry.setPermissions(EnumSet.of(Permission.WRITE));

        AclEntry roleAclEntry = new AclEntry();
        roleAclEntry.setSidType(AclEntry.SidType.ROLE);
        roleAclEntry.setSidId(1L);
        roleAclEntry.setObjectId(1L);
        roleAclEntry.setObjectNamespace("topology");
        roleAclEntry.setPermissions(EnumSet.of(Permission.READ));

        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_FOO");
        List<QueryParam> qps1 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE, USER.toString(),
                AclEntry.SID_ID, "1");

        List<QueryParam> qps2 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE, AclEntry.SidType.ROLE.toString());

        List<QueryParam> qps3 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE,
                AclEntry.SidType.GROUP.toString());

        User user = new User();
        user.setRoles(Sets.newHashSet("ROLE_FOO"));

        new Expectations(catalogService) {{
            catalogService.getUser(anyLong);
            result = user;
            catalogService.listAcls(qps1);
            result = Arrays.asList(userAclEntry);
            catalogService.getAllUserRoles(user);
            result = Sets.newHashSet(role);
            catalogService.listAcls(qps2);
            result = Arrays.asList(roleAclEntry);
            catalogService.listAcls(qps3);
            result = new ArrayList<AclEntry>();
            catalogService.getRole(1L);
            result = role;
        }};

        assertTrue(catalogService.checkUserPermissions("topology", 1L, 1L, Collections.emptySet(), EnumSet.of(Permission.READ)));
        assertTrue(catalogService.checkUserPermissions("topology", 1L, 1L, Collections.emptySet(), EnumSet.of(Permission.WRITE)));
        assertTrue(catalogService.checkUserPermissions("topology", 1L, 1L, Collections.emptySet(), EnumSet.of(Permission.WRITE, Permission.READ)));
        assertFalse(catalogService.checkUserPermissions("topology", 1L, 1L, Collections.emptySet(), EnumSet.of(Permission.WRITE, Permission.DELETE)));
    }

    @Test
    public void checkUserPermissionsForParenrtObject() throws Exception {
        SecurityCatalogService catalogService = new SecurityCatalogService(null);
        AclEntry userAclEntry = new AclEntry();
        userAclEntry.setSidType(AclEntry.SidType.USER);
        userAclEntry.setSidId(1L);
        userAclEntry.setObjectId(1L);
        userAclEntry.setObjectNamespace("topology");
        userAclEntry.setPermissions(EnumSet.of(Permission.WRITE, Permission.READ));

        AclEntry roleAclEntry = new AclEntry();
        roleAclEntry.setSidType(AclEntry.SidType.ROLE);
        roleAclEntry.setSidId(1L);
        roleAclEntry.setObjectId(1L);
        roleAclEntry.setObjectNamespace("topology");
        roleAclEntry.setPermissions(EnumSet.of(Permission.READ));

        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_FOO");
        List<QueryParam> qps1 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE, USER.toString(),
                AclEntry.SID_ID, "1");

        List<QueryParam> qps2 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE, AclEntry.SidType.ROLE.toString());

        List<QueryParam> qps3 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, Project.NAMESPACE,
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE, USER.toString(),
                AclEntry.SID_ID, "1");

        User user = new User();
        user.setRoles(Sets.newHashSet("ROLE_FOO"));

        Topology topology = new Topology();
        topology.setId(1L);
        topology.setProjectId(1L);
        List<Topology> listTopology = new ArrayList<>();
        listTopology.add(topology);

        AclEntry parentAclEntry = new AclEntry();
        parentAclEntry.setObjectNamespace(Project.NAMESPACE);
        parentAclEntry.setObjectId(1L);
        parentAclEntry.setOwner(false);
        parentAclEntry.setGrant(true);
        parentAclEntry.setSidId(1L);
        parentAclEntry.setSidType(USER);
        parentAclEntry.setPermissions(EnumSet.of(Permission.READ));


        new Expectations(catalogService) {{
            catalogService.getUser(anyLong);
            result = user;
            catalogService.listAcls(qps1);
            result = Arrays.asList(userAclEntry);
            catalogService.listAcls(qps3);
            result = Arrays.asList(parentAclEntry);
            catalogService.getEntity(Topology.NAMESPACE, 1L);
            result = listTopology;
            catalogService.getAcl(1L, 1L, Project.NAMESPACE, Permission.READ);
            result = new ArrayList<AclEntry>();
            catalogService.addAcl(parentAclEntry);
            result = parentAclEntry;
        }};

        catalogService.shouldAllowParentReadPermissions(userAclEntry);

        assertTrue(catalogService.checkUserPermissions(Topology.NAMESPACE, 1L, 1L, Collections.emptySet(), EnumSet.of(Permission.READ)));
        assertTrue(catalogService.checkUserPermissions(Topology.NAMESPACE, 1L, 1L, Collections.emptySet(), EnumSet.of(Permission.WRITE)));

        assertTrue(catalogService.checkUserPermissions(Project.NAMESPACE, 1L, 1L, Collections.emptySet(), EnumSet.of(Permission.READ)));
    }

    @Test
    public void checkUserPermissionsByGroup() throws Exception {
        SecurityCatalogService catalogService = new SecurityCatalogService(null);

        AclEntry userAclEntry = new AclEntry();
        userAclEntry.setSidType(AclEntry.SidType.USER);
        userAclEntry.setSidId(1L);
        userAclEntry.setObjectId(1L);
        userAclEntry.setObjectNamespace("topology");
        userAclEntry.setPermissions(EnumSet.of(Permission.WRITE));

        AclEntry roleAclEntry = new AclEntry();
        roleAclEntry.setSidType(AclEntry.SidType.ROLE);
        roleAclEntry.setSidId(1L);
        roleAclEntry.setObjectId(1L);
        roleAclEntry.setObjectNamespace("topology");
        roleAclEntry.setPermissions(EnumSet.of(Permission.READ));

        AclEntry groupAclEntry = new AclEntry();
        groupAclEntry.setSidType(AclEntry.SidType.GROUP);
        groupAclEntry.setSidId(111L);
        groupAclEntry.setObjectId(1L);
        groupAclEntry.setObjectNamespace("topology");
        groupAclEntry.setPermissions(EnumSet.of(Permission.WRITE, Permission.READ));

        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_FOO");

        List<QueryParam> qps1 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE, USER.toString(),
                AclEntry.SID_ID, "1");

        List<QueryParam> qps2 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE,
                AclEntry.SidType.ROLE.toString());

        List<QueryParam> qps3 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE,
                AclEntry.SidType.GROUP.toString());

        Set<String> userGroups = new HashSet<>();
        userGroups.add("111");
        User user = new User();

        OwnerGroup ownerGroup = new OwnerGroup();
        ownerGroup.setId(111L);
        ownerGroup.setName("111");

        new Expectations(catalogService) {{
            catalogService.getUser(anyLong);
            result = user;
            catalogService.listAcls(qps1);
            result = new ArrayList<AclEntry>();
            catalogService.listAcls(qps3);
            result = groupAclEntry;
            catalogService.getGroup(111L);
            result = ownerGroup;
        }};

        assertTrue(catalogService.checkUserPermissions("topology", 1L, 1L, userGroups, EnumSet.of(Permission.READ)));
        assertTrue(catalogService.checkUserPermissions("topology", 1L, 1L, userGroups, EnumSet.of(Permission.WRITE)));
        assertTrue(catalogService.checkUserPermissions("topology", 1L, 1L, userGroups, EnumSet.of(Permission.WRITE, Permission.READ)));
        assertFalse(catalogService.checkUserPermissions("topology", 1L, 1L, userGroups, EnumSet.of(Permission.WRITE, Permission.DELETE)));

        // Making user part of different group
        userGroups = new HashSet<>();
        userGroups.add("222");
        // Should not have access since user is not part topoloy's owner group
        assertFalse(catalogService.checkUserPermissions("topology", 1L, 1L, userGroups, EnumSet.of(Permission.READ)));
    }
}