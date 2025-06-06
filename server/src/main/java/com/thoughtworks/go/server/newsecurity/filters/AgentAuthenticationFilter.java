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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.server.newsecurity.models.AgentToken;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrincipal;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.Clock;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class AgentAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentAuthenticationFilter.class);

    private final GoConfigService goConfigService;
    private final AgentService agentService;
    private final Clock clock;

    private HmacUtils hmacUtil;

    @Autowired
    public AgentAuthenticationFilter(GoConfigService goConfigService, Clock clock, AgentService agentService) {
        this.goConfigService = goConfigService;
        this.clock = clock;
        this.agentService = agentService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        tokenBasedFilter(request, response, filterChain);
    }

    private void tokenBasedFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String uuid = request.getHeader("X-Agent-GUID");
        String token = request.getHeader("Authorization");

        if (isBlank(uuid) || isBlank(token)) {
            LOGGER.debug("Denying access, either the UUID or token is not provided.");
            response.setStatus(403);
            return;
        }

        if (!agentService.isRegistered(uuid)) {
            LOGGER.debug("Denying access, agent with uuid '{}' is not registered.", uuid);
            response.setStatus(403);
            return;
        }

        AuthenticationToken<?> authenticationToken = SessionUtils.getAuthenticationToken(request);
        AgentToken agentToken = new AgentToken(uuid, token);

        if (isAuthenticated(agentToken, authenticationToken)) {
            LOGGER.debug("Agent is already authenticated");
        } else {
            if (!hmacOf(uuid).equals(token)) {
                LOGGER.debug("Denying access, agent with uuid '{}' submitted bad token.", uuid);
                response.setStatus(403);
                return;
            }

            GoUserPrincipal agentUser = new GoUserPrincipal("_go_agent_" + uuid, "", GoAuthority.ROLE_AGENT.asAuthority());
            AuthenticationToken<AgentToken> authentication = new AuthenticationToken<>(agentUser, agentToken, null, clock.currentTimeMillis(), null);

            LOGGER.debug("Adding agent user to current session and proceeding.");
            SessionUtils.setAuthenticationTokenAfterRecreatingSession(authentication, request);
        }

        filterChain.doFilter(request, response);
    }

    /*Fixes:#8427 HMAC generation is not thread safe, if multiple agents try to authenticate at the same time the hmac
    generated using the Agent UUID would not match the actual token.*/
    synchronized String hmacOf(String uuid) {
        if (hmacUtil == null) {
            hmacUtil = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, goConfigService.serverConfig().getTokenGenerationKey().getBytes());
        }
        return Base64.getEncoder().encodeToString(hmacUtil.hmac(uuid.getBytes()));
    }

    private boolean isAuthenticated(AgentToken agentToken, AuthenticationToken<?> authenticationToken) {
        return authenticationToken != null
                && authenticationToken.getCredentials() instanceof AgentToken
                && authenticationToken.getCredentials().equals(agentToken);
    }
}
