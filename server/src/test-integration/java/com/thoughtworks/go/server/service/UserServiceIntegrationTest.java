/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.UserRoleMatcherMother;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.presentation.UserModel;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.presentation.UserSourceType;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.UserSqlMapDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TriState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})
public class UserServiceIntegrationTest {
    @Autowired
    private UserSqlMapDao userDao;
    @Autowired
    private UserService userService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoCache goCache;

    private static GoConfigFileHelper configFileHelper = new GoConfigFileHelper(ConfigFileFixture.ONE_PIPELINE);

    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configFileHelper.onSetUp();
        configFileHelper.usingCruiseConfigDao(goConfigDao);
        goCache.clear();
    }

    @AfterEach
    public void teardown() throws Exception {
        configFileHelper.onTearDown();
        dbHelper.onTearDown();
        goCache.clear();
    }

    @Test
    public void shouldSaveUser() throws ValidationException {
        User user = new User("name1", new String[]{"regx"}, "user@mail.com", true);
        userService.saveOrUpdate(user);
        User savedUser = userDao.findUser("name1");
        assertThat(savedUser).isEqualTo(user);
        assertThat(userService.load(savedUser.getId())).isEqualTo(user);
    }

    @Test
    public void shouldUpdateWhenUserAlreadyExist() throws ValidationException {
        addUser(new User("name1", new String[]{"regx"}, "user@mail.com", true));
        User updatedUser = userService.findUserByName("name1");
        updatedUser.setEmail("user2@mail.com");
        updatedUser.setMatcher("regx2");

        userService.saveOrUpdate(updatedUser);

        User user = userDao.findUser("name1");
        assertThat(user).isEqualTo(updatedUser);
        assertThat(user.getId()).isEqualTo(updatedUser.getId());
    }

    @Test
    public void addOrUpdateUser_shouldAddUserIfDoesNotExist() {
        assertThat(userDao.findUser("new_user")).isInstanceOf(NullUser.class);
        SecurityAuthConfig authConfig = new SecurityAuthConfig();
        userService.addOrUpdateUser(new User("new_user"), authConfig);
        User loadedUser = userDao.findUser("new_user");
        assertThat(loadedUser).isEqualTo(new User("new_user", "new_user", ""));
        assertThat(loadedUser).isNotInstanceOf(NullUser.class);
    }

    @Test
    public void addOrUpdateUser_shouldNotAddUserIfExistsAndIsNotUpdated() {
        User user = new User("old_user");
        addUser(user);
        SecurityAuthConfig authConfig = new SecurityAuthConfig();
        userService.addOrUpdateUser(user, authConfig);
    }

    @Test
    public void addOrUpdateUser_shouldUpdateUserIfExistsAndEitherEmailOrDisplayNameChanged() {
        String name = "old_user";
        User user = new User(name);
        addUser(user);
        User updatedUser = new User(name, name, "");
        SecurityAuthConfig authConfig = new SecurityAuthConfig();
        userService.addOrUpdateUser(updatedUser, authConfig);

        User loadedUser = userDao.findUser(name);
        assertThat(loadedUser).isEqualTo(updatedUser);
        assertThat(loadedUser).isNotInstanceOf(NullUser.class);
    }

    @Test
    public void addOrUpdateUser_shouldNotAddUserIfAnonymous() {
        SecurityAuthConfig authConfig = new SecurityAuthConfig();
        userService.addOrUpdateUser(new User(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername())), authConfig);
        assertThat(userDao.findUser(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()))).isInstanceOf(NullUser.class);
        assertThat(userDao.findUser(Username.ANONYMOUS.getDisplayName())).isInstanceOf(NullUser.class);
    }

    @Test
    public void shouldValidateUser() {
        try {
            userService.validate(new User("username", new String[]{"committer"}, "mail.com", false));
            fail("should have thrown when email is invalid");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    public void shouldNotSaveUserWhenValidationFailed() {
        try {
            userService.saveOrUpdate(new User("username", new String[]{"committer"}, "mail.com", false));
            fail("should have thrown when email is invalid");
        } catch (ValidationException e) {
            assertThat(userService.findUserByName("username")).isInstanceOf(NullUser.class);
        }
    }

    @Test
    public void shouldAddNotificationFilterForExistingUser() throws ValidationException {
        User user = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        userService.saveOrUpdate(user);
        user = userDao.findUser(user.getName());
        NotificationFilter filter = new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false);
        userService.addNotificationFilter(user.getId(), filter);
        user = userService.findUserByName("jez");
        assertThat(user.getNotificationFilters().size()).isEqualTo(1);
        assertThat(user.getNotificationFilters()).contains(filter);
    }

    @Test
    public void shouldRemoveNotificationFilterForUser() {
        User user = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        addUser(user);
        user = userDao.findUser(user.getName());
        NotificationFilter filter = new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false);
        userService.addNotificationFilter(user.getId(), filter);
        user = userService.findUserByName(user.getName());
        assertThat(user.getNotificationFilters().size()).isEqualTo(1);
        long deletedNotificationId = user.getNotificationFilters().get(0).getId();
        userService.removeNotificationFilter(user.getId(), deletedNotificationId);
        assertThat(userService.findUserByName(user.getName()).getNotificationFilters().size()).isEqualTo(0);
    }

    @Test
    public void shouldNotAddDuplicateNotificationFilter() {
        User user = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        NotificationFilter filter = new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false);
        addUserWithNotificationFilter(user, filter);
        user = userDao.findUser(user.getName());

        try {
            userService.addNotificationFilter(user.getId(), filter);
            fail("shouldNotAddDuplicateNotificationFilter");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Duplicate notification filter");
        }
    }

    @Test
    public void shouldNotAddUnnecessaryNotificationFilter() throws ValidationException {
        User user = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        userService.saveOrUpdate(user);
        user = userDao.findUser(user.getName());
        userService.addNotificationFilter(user.getId(), new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false));

        try {
            userService.addNotificationFilter(user.getId(), new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false));
            fail("shouldNotAddUnnecessaryNotificationFilter");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Duplicate notification filter");
        }
    }

    @Test
    public void shouldLoadUsersWhoSubscribedNotificationOnStage() {
        User tom = new User("tom", new String[]{"tom"}, "tom@mail.com", true);
        addUserWithNotificationFilter(tom, new NotificationFilter("p1", "s1", StageEvent.Breaks, true));

        User jez = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        addUserWithNotificationFilter(jez,
            new NotificationFilter("pipeline1", "stage", StageEvent.All, false),
            new NotificationFilter("mingle", "dev", StageEvent.All, false));

        Users users = userService.findValidSubscribers(new StageConfigIdentifier("pipeline1", "stage"));
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0)).isEqualTo(jez);
        assertThat(users.get(0).getNotificationFilters().size()).isEqualTo(2);
    }

    @Test
    public void shouldLoadAuthorizedUser() {
        givingJezViewPermissionToMingle();

        User tom = new User("tom", new String[]{"tom"}, "tom@mail.com", true);
        User jez = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        addUserWithNotificationFilter(jez,
            new NotificationFilter("mingle", "dev", StageEvent.All, false));
        addUserWithNotificationFilter(tom,
            new NotificationFilter("mingle", "dev", StageEvent.All, false));

        Users users = userService.findValidSubscribers(new StageConfigIdentifier("mingle", "dev"));
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0)).isEqualTo(jez);
        assertThat(users.get(0).getNotificationFilters().size()).isEqualTo(1);
    }

    @Test
    public void shouldCreateANewUser() {
        UserSearchModel foo = new UserSearchModel(new User("fooUser", "Mr Foo", "foo@cruise.com"), UserSourceType.PLUGIN);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(List.of(foo), result);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("User 'fooUser' successfully added.");
    }

    @Test
    public void shouldReturnErrorWhenTryingToAddAnonymousUser() {
        UserSearchModel anonymous = new UserSearchModel(new User(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()), "Mr. Anonymous", "anon@cruise.com"), UserSourceType.PLUGIN);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(List.of(anonymous), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("Failed to add user. Username 'anonymous' is not permitted.");
    }

    @Test
    public void shouldReturnErrorWhenUserAlreadyExists() {
        UserSearchModel foo = new UserSearchModel(new User("fooUser", "Mr Foo", "foo@cruise.com"), UserSourceType.PLUGIN);
        addUser(foo.getUser());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(List.of(foo), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.User.alreadyExists("fooUser"));
    }

    @Test
    public void create_shouldReturnErrorWhenNoUsersSelected() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(new ArrayList<>(), result);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("No users selected.");
    }

    @Test
    public void disableUsers_shouldDisableUsers() {
        addUser(new User("user_one"));
        addUser(new User("user_two"));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.disable(List.of("user_one"), result);

        assertThat(result.isSuccessful()).isTrue();
        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.ASC);
        assertThat(models.get(0).isEnabled()).isFalse();
        assertThat( models.get(1).isEnabled()).isTrue();
    }

    @Test
    public void shouldEnableUsers() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        User user1 = new User("user_one");
        user1.disable();
        addUser(user1);

        createDisabledUser("user_two");

        userService.enable(List.of("user_one"));

        assertThat(result.isSuccessful()).isTrue();
        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.ASC);
        assertThat(models.get(0).isEnabled()).isTrue();
        assertThat(models.get(1).isEnabled()).isFalse();
    }

    @Test
    public void shouldKnowEnabledAndDisbaledUsersCount() {
        addUser(new User("user_one"));
        addUser(new User("user_three"));

        createDisabledUser("user_two");

        assertThat(userService.enabledUserCount()).isEqualTo(2L);
        assertThat(userService.disabledUserCount()).isEqualTo(1L);
    }

    @Test
    public void shouldReturnErrorMessageWhenUserValidationsFail() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        User invalidUser = new User("fooUser", "Foo User", "invalidEmail");
        UserSearchModel searchModel = new UserSearchModel(invalidUser, UserSourceType.PLUGIN);

        userService.create(List.of(searchModel), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("Failed to add user. Validations failed. Invalid email address.");
    }

    @Test
    public void shouldReturnErrorMessageWhenTheLastAdminIsBeingDisabled() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        configFileHelper.enableSecurity();
        configFileHelper.addAdmins("Jake", "Pavan", "Yogi");

        userService.create(users("Jake", "Pavan", "Shilpa", "Yogi"), new HttpLocalizedOperationResult());

        userService.disable(List.of("Yogi"), result);
        assertThat(result.isSuccessful()).isTrue();

        userService.disable(List.of("Pavan", "Jake"), result);//disable remaining admins

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(result.message()).isEqualTo("There must be at least one admin user enabled!");
    }

    @Test
    public void modifyRoles_shouldAddUserToExistingRole() {
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("dev")));
        addUser(new User("user-1"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.add)), result);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1"))).isTrue();
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void modifyRoles_shouldNotAddUserToExistingRoleIfAlreadyAMember() {
        addUser(new User("user-1"));
        // first time
        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        // second time
        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1"))).isTrue();
    }

    @Test
    public void modifyRoles_shouldCreateRoleAndAddUserIfRoleDoesntExist() {
        addUser(new User("user-1"));
        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1"))).isTrue();
    }

    @Test
    public void modifyRoles_shouldNotCreateRoleIfItHasInvalidCharacters() {
        addUser(new User("user-1"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection(".dev+", TriStateSelection.Action.add)), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).contains("Failed to add role. Reason - ");
    }

    @Test
    public void modifyRoles_shouldRemoveUserFromRole() {
        addUser(new User("user-1"));
        // add it first
        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        // now remove it
        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.remove)), new HttpLocalizedOperationResult());
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1"))).isFalse();
    }

    @Test
    public void modifyRoles_shouldNotModifyRolesWhenActionIsNoChange() {
        addUser(new User("user-1"));
        // add it first
        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        // no change
        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.nochange)), new HttpLocalizedOperationResult());
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1"))).isTrue();
    }

    @Test
    public void modifyRoles_shouldNotModifyRolesForAUserThatDoesNotExistInDb() {
        assertThat(userDao.findUser("user-1")).isInstanceOf(NullUser.class);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.add)), result);

        assertThat(userDao.findUser("user-1")).isInstanceOf(NullUser.class);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).contains("User 'user-1' does not exist in the database.");
    }

    @Test
    public void shouldModifyRolesAndAdminPrivilegeAtTheSameTime() {
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("dev")));
        addUser(new User("user-1"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(List.of("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add), List.of(new TriStateSelection("dev", TriStateSelection.Action.add)), result);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1"))).isTrue();
        assertThat(cruiseConfig.server().security().adminsConfig().hasUser(new CaseInsensitiveString("user-1"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isTrue();
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void shouldAddAdminPrivilegeToMultipleUsers() {
        addUser(new User("user"));
        addUser(new User("loser"));
        addUser(new User("boozer"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(List.of("user", "boozer"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add), new ArrayList<>(), result);
        CruiseConfig cruiseConfig = goConfigDao.load();
        final AdminsConfig adminsConfig = cruiseConfig.server().security().adminsConfig();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isTrue();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isFalse();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isTrue();
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void shouldRemoveUserLevelAdminPrivilegeFromMultipleUsers_withoutModifingRoleLevelPrvileges() {
        configFileHelper.addAdmins("user", "boozer");
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("mastersOfTheWorld"), new RoleUser(new CaseInsensitiveString("loser")), new RoleUser(new CaseInsensitiveString("boozer"))));
        configFileHelper.addAdminRoles("mastersOfTheWorld");
        addUser(new User("user"));
        addUser(new User("loser"));
        addUser(new User("boozer"));

        CruiseConfig cruiseConfig = goConfigDao.load();
        SecurityConfig securityConfig = cruiseConfig.server().security();
        AdminsConfig adminsConfig = securityConfig.adminsConfig();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isTrue();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isFalse();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isTrue();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(List.of("user", "boozer"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.remove), new ArrayList<>(), result);

        cruiseConfig = goConfigDao.load();
        securityConfig = cruiseConfig.server().security();
        adminsConfig = securityConfig.adminsConfig();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isFalse();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isFalse();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isFalse();
        final SecurityService.UserRoleMatcherImpl groupMatcher = new SecurityService.UserRoleMatcherImpl(securityConfig);
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), groupMatcher)).isFalse();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), groupMatcher)).isTrue();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), groupMatcher)).isTrue();
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void shouldNotModifyAdminPrivilegesWhen_NoChange_requested() {
        configFileHelper.addAdmins("user", "boozer");
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("mastersOfTheWorld"), new RoleUser(new CaseInsensitiveString("loser")), new RoleUser(new CaseInsensitiveString("boozer"))));
        configFileHelper.addAdminRoles("mastersOfTheWorld");
        addUser(new User("user"));
        addUser(new User("loser"));
        addUser(new User("boozer"));


        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(List.of("user", "boozer"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), new ArrayList<>(), result);

        final CruiseConfig cruiseConfig = goConfigDao.load();
        final SecurityConfig securityConfig = cruiseConfig.server().security();
        final AdminsConfig adminsConfig = securityConfig.adminsConfig();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isTrue();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isFalse();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isTrue();
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void getRoleSelection() {
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("dev")));
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("boy")));
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("girl")));
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("none")));
        addUser(new User("yogi"));
        addUser(new User("shilpa"));
        addUser(new User("pavan"));
        userService.modifyRolesAndUserAdminPrivileges(List.of("yogi", "shilpa"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        userService.modifyRolesAndUserAdminPrivileges(List.of("shilpa"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("girl", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        userService.modifyRolesAndUserAdminPrivileges(List.of("yogi"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("boy", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        userService.modifyRolesAndUserAdminPrivileges(List.of("pavan"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("none", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        List<TriStateSelection> selections = userService.getAdminAndRoleSelections(List.of("yogi", "shilpa")).getRoleSelections();
        assertThat(selections.size()).isEqualTo(4);
        assertRoleSelection(selections.get(0), "boy", TriStateSelection.Action.nochange);
        assertRoleSelection(selections.get(1), "dev", TriStateSelection.Action.add);
        assertRoleSelection(selections.get(2), "girl", TriStateSelection.Action.nochange);
        assertRoleSelection(selections.get(3), "none", TriStateSelection.Action.remove);
    }

    @Test
    public void getRoleSelectionOnlyForNonPluginRoles() {
        configFileHelper.addSecurityAuthConfig(new SecurityAuthConfig("auth_id", "plugin_id"));
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("core-role")));
        configFileHelper.addRole(new PluginRoleConfig("plugin-role", "auth_id"));
        addUser(new User("yogi"));
        addUser(new User("shilpa"));
        userService.modifyRolesAndUserAdminPrivileges(List.of("yogi", "shilpa"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), List.of(new TriStateSelection("core-role", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        List<TriStateSelection> selections = userService.getAdminAndRoleSelections(List.of("yogi", "shilpa")).getRoleSelections();
        assertThat(selections.size()).isEqualTo(1);
        assertRoleSelection(selections.get(0), "core-role", TriStateSelection.Action.add);
    }

    @Test
    public void shouldGetAdminSelectionWithCorrectState() {
        configFileHelper.addAdmins("foo", "quux");
        assertThat(userService.getAdminAndRoleSelections(List.of("foo")).getAdminSelection()).isEqualTo(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add));
        assertThat(userService.getAdminAndRoleSelections(List.of("foo", "bar")).getAdminSelection()).isEqualTo(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange));
        assertThat(userService.getAdminAndRoleSelections(List.of("foo", "quux")).getAdminSelection()).isEqualTo(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add));
        assertThat(userService.getAdminAndRoleSelections(List.of("baz", "bar")).getAdminSelection()).isEqualTo(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.remove));
    }

    @Test
    public void shouldDisableAdminSelectionWhenUserIsMemberOfAdminRole() {
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("foo-grp"), new RoleUser(new CaseInsensitiveString("foo")), new RoleUser(new CaseInsensitiveString("foo-one"))));
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("quux-grp"), new RoleUser(new CaseInsensitiveString("quux"))));
        configFileHelper.addRole(new RoleConfig(new CaseInsensitiveString("bar-grp"), new RoleUser(new CaseInsensitiveString("bar")), new RoleUser(new CaseInsensitiveString("bar-one"))));
        configFileHelper.addAdminRoles("foo-grp", "quux-grp");

        assertThat(userService.getAdminAndRoleSelections(List.of("foo")).getAdminSelection()).isEqualTo(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add, false));
        assertThat(userService.getAdminAndRoleSelections(List.of("foo", "bar")).getAdminSelection()).isEqualTo(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange, false));
        assertThat(userService.getAdminAndRoleSelections(List.of("bar", "baz")).getAdminSelection()).isEqualTo(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.remove));
        assertThat(userService.getAdminAndRoleSelections(List.of("baz")).getAdminSelection()).isEqualTo(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.remove));
        assertThat(userService.getAdminAndRoleSelections(List.of("foo", "quux")).getAdminSelection()).isEqualTo(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add, false));
    }

    @Test
    public void shouldUpdateEnabledStateToFalse() {
        User user = new User("user-1");
        user.enable();
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.FALSE, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    public void shouldUpdateEnabledStateToTrue() {
        User user = new User("user-1");
        user.disable();
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.TRUE, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    public void shouldNotUpdateEnabledStateWhenAskedToBeLeftUnset() {
        User user = new User("user-1");
        user.disable();
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    public void updateShouldUpdateEmailMeStateToTrue() {
        User user = new User("user-1");
        user.enable();
        user.setEmailMe(false);
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.TRUE, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.isEmailMe()).isTrue();
    }


    @Test
    public void updateShouldUpdateEmailMeStateToFalse() {
        User user = new User("user-1");
        user.enable();
        user.setEmailMe(true);
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.FALSE, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.isEmailMe()).isFalse();
    }

    @Test
    public void updateShouldUpdateEmailMeStateWHenAskedToBeLeftUnset() {
        User user = new User("user-1");
        user.enable();
        user.setEmailMe(true);
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(user.isEmailMe()).isTrue();
    }

    @Test
    public void updateShouldUpdateEmail() {
        User user = new User("user-1");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, "foo@example.com", null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getEmail()).isEqualTo("foo@example.com");

        result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.TRUE, TriState.UNSET, "", null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getEmail()).isEqualTo("");
    }

    @Test
    public void updateShouldNotUpdateEmailWhenNull() {
        User user = new User("user-1");
        user.setEmail("foo@example.com");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getEmail()).isEqualTo("foo@example.com");
    }

    @Test
    public void updateShouldUpdateMatcher() {
        User user = new User("user-1");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, "foo,bar", result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getMatcher()).isEqualTo("foo,bar");

        result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, "", result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getMatcher()).isEqualTo("");
    }

    @Test
    public void updateShouldNotUpdateMatcherWhenNull() {
        User user = new User("user-1");
        user.setMatcher("foo,bar");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getMatcher()).isEqualTo("foo,bar");
    }

    private void assertRoleSelection(TriStateSelection selection, String roleName, TriStateSelection.Action action) {
        assertThat(selection.getValue()).isEqualTo(roleName);
        assertThat(selection.getAction()).isEqualTo(action);
    }

    private void createDisabledUser(String username) {
        User user = new User(username);
        user.disable();
        addUser(user);
    }

    private List<UserSearchModel> users(String... usernames) {
        List<UserSearchModel> models = new ArrayList<>();
        for (String username : usernames) {
            models.add(new UserSearchModel(new User(username, username, "foo@cruise.com"), UserSourceType.PLUGIN));
        }
        return models;
    }

    private void givingJezViewPermissionToMingle() {
        configFileHelper.enableSecurity();
        configFileHelper.addPipeline("mingle", "dev");
        configFileHelper.setViewPermissionForGroup("defaultGroup", "jez");
        configFileHelper.addSecurityWithAdminConfig();
    }

    private void addUser(User user) {
        userDao.saveOrUpdate(user);
    }

    private void addUserWithNotificationFilter(User user, NotificationFilter... filters) {
        for (NotificationFilter filter : filters) {
            user.addNotificationFilter(filter);
        }
        addUser(user);
    }
}
