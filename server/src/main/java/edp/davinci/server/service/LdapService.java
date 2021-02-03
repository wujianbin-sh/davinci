/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2020 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.server.service;

import edp.davinci.server.exception.ServerException;
import edp.davinci.server.model.LdapPerson;
import edp.davinci.core.dao.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public interface LdapService {

    boolean existLdapServer();

    /**
     * 校验ldap用户
     *
     * @param username
     * @param password
     * @return
     * @throws Exception
     */
    LdapPerson checkUser(String username, String password);

    /**
     * 查询ldap用户
     *
     * @param username
     * @return
     * @throws Exception
     */
    LdapPerson searchUser(String username);

    User registPerson(LdapPerson ldapPerson) throws ServerException;
}
