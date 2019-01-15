package com.hortonworks.streamline.streams.security;

import com.google.common.collect.Sets;
import com.hortonworks.streamline.streams.security.catalog.Role;
import com.hortonworks.streamline.streams.security.catalog.User;
import com.hortonworks.streamline.streams.security.impl.DefaultStreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.service.SecurityCatalogService;
import mockit.Expectations;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static com.hortonworks.streamline.streams.security.impl.DefaultStreamlineAuthorizer.CONF_CATALOG_SERVICE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultStreamlineAuthorizerTest {
    @Test
    public void testHasRoleForExisitingUser() throws Exception {
        SecurityCatalogService catalogService = new SecurityCatalogService(null);
        AuthenticationContext authenticationContext = new AuthenticationContext();
        authenticationContext.setPrincipal(new StreamlinePrincipal("User1"));

        User user = new User();
        user.setRoles(Sets.newHashSet(Roles.ROLE_TOPOLOGY_ADMIN));

        Role roleAdmin = new Role();
        roleAdmin.setId(1L);
        roleAdmin.setName(Roles.ROLE_ADMIN);

        Role roleTopologyAdmin = new Role();
        roleTopologyAdmin.setId(1L);
        roleTopologyAdmin.setName(Roles.ROLE_TOPOLOGY_ADMIN);

        new Expectations(catalogService) {{
            catalogService.getUser("User1");
            result = user;

            catalogService.getRole(Roles.ROLE_ADMIN);
            result = Optional.of(roleAdmin);

            catalogService.getRole(Roles.ROLE_TOPOLOGY_ADMIN);
            result = Optional.of(roleTopologyAdmin);

            catalogService.getChildRoles(1L);
            result = new HashSet<>();
        }};

        DefaultStreamlineAuthorizer defaultStreamlineAuthorizer = new DefaultStreamlineAuthorizer();
        Map<String, Object> authorizerConfig = new HashMap<>();
        authorizerConfig.putIfAbsent(CONF_CATALOG_SERVICE, catalogService);
        authorizerConfig.put(DefaultStreamlineAuthorizer.CONF_ADMIN_PRINCIPALS, new HashSet<>());
        authorizerConfig.put(DefaultStreamlineAuthorizer.CONF_PRINCIPAL_DOMAIN, "test");
        authorizerConfig.put(DefaultStreamlineAuthorizer.CONF_AUTO_REGISTRATION_USER, true);
        defaultStreamlineAuthorizer.init(authorizerConfig);
        assertTrue(defaultStreamlineAuthorizer.hasRole(authenticationContext, Roles.ROLE_TOPOLOGY_ADMIN));
    }

    @Test
    public void testHasRoleForNonExisitingUser() throws Exception {
        SecurityCatalogService catalogService = new SecurityCatalogService(null);
        AuthenticationContext authenticationContext = new AuthenticationContext();
        authenticationContext.setPrincipal(new StreamlinePrincipal("User1"));

        Role roleAdmin = new Role();
        roleAdmin.setId(1L);
        roleAdmin.setName(Roles.ROLE_ADMIN);

        Role roleDeveloper = new Role();
        roleDeveloper.setId(1L);
        roleDeveloper.setName(Roles.ROLE_DEVELOPER);

        User userWithDefaulRole = new User();
        userWithDefaulRole.setName("User1");
        userWithDefaulRole.addRole(Roles.ROLE_DEVELOPER);

        new Expectations(catalogService) {{
            catalogService.getUser("User1");
            result = null;

            catalogService.getRole(Roles.ROLE_ADMIN);
            result = Optional.of(roleAdmin);

            catalogService.addUser(userWithDefaulRole);
            result = userWithDefaulRole;

            catalogService.getRole(Roles.ROLE_DEVELOPER);
            result = Optional.of(roleDeveloper);

            catalogService.getChildRoles(1L);
            result = new HashSet<>();
        }};

        DefaultStreamlineAuthorizer defaultStreamlineAuthorizer = new DefaultStreamlineAuthorizer();
        Map<String, Object> authorizerConfig = new HashMap<>();
        authorizerConfig.putIfAbsent(CONF_CATALOG_SERVICE, catalogService);
        authorizerConfig.put(DefaultStreamlineAuthorizer.CONF_ADMIN_PRINCIPALS, new HashSet<>());
        authorizerConfig.put(DefaultStreamlineAuthorizer.CONF_PRINCIPAL_DOMAIN, "test");
        authorizerConfig.put(DefaultStreamlineAuthorizer.CONF_AUTO_REGISTRATION_USER, true);
        defaultStreamlineAuthorizer.init(authorizerConfig);
        assertTrue(defaultStreamlineAuthorizer.hasRole(authenticationContext, Roles.ROLE_DEVELOPER));
        assertFalse(defaultStreamlineAuthorizer.hasRole(authenticationContext, Roles.ROLE_TOPOLOGY_ADMIN));
    }
}
