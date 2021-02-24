/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.visualstudio;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.SharedTokenCacheCredential;
import com.azure.identity.SharedTokenCacheCredentialBuilder;
import com.azure.identity.implementation.IdentityClient;
import com.azure.identity.implementation.SynchronizedAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.TokenCache;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialProvider;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.AccountBuilderUtils;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class VisualStudioAccountEntityBuilder implements IAccountEntityBuilder {
    private static final String VISUAL_STUDIO_CLIENT_ID = "872cd9fa-d31f-45e0-9eab-6e460a02d1f1";
    private AzureEnvironment environment;

    @Override
    public AccountEntity build() {
        AccountEntity accountEntity = AccountBuilderUtils.createAccountEntity(AuthMethod.VISUAL_STUDIO);
        try {
            return buildInner(accountEntity);
        } catch (IllegalAccessException | JsonProcessingException | ExecutionException | InterruptedException e) {
            accountEntity.setError(e);
        }
        return accountEntity;
    }

    private AccountEntity buildInner(AccountEntity accountEntity)
            throws IllegalAccessException, JsonProcessingException, ExecutionException, InterruptedException {
        Map<String, AzureEnvironment> envEndpoints = Utils.groupByIgnoreDuplicate(
                AzureEnvironment.knownEnvironments(), AzureEnvironment::getManagementEndpoint);
        SharedTokenCacheCredential cred = new SharedTokenCacheCredentialBuilder().clientId(VISUAL_STUDIO_CLIENT_ID).username("test-account").build();
        IdentityClient identityClient = (IdentityClient) FieldUtils.readField(cred, "identityClient", true);
        SynchronizedAccessor<PublicClientApplication> publicClientApplicationAccessor = (SynchronizedAccessor<PublicClientApplication>)
                FieldUtils.readField(identityClient, "publicClientApplicationAccessor", true);
        TokenCache tc = publicClientApplicationAccessor.getValue().block().tokenCache();
        publicClientApplicationAccessor.getValue().block().getAccounts().get();
        TokenCacheEntity tokenCacheEntity = convertByJson(tc, TokenCacheEntity.class);
        final Map<String, CachedAccountEntity> accountMap = Utils.groupByIgnoreDuplicate(
                tokenCacheEntity.getAccounts().values(), CachedAccountEntity::getHomeAccountId);

        Set<Pair<AzureEnvironment, CachedAccountEntity>> sharedAccounts = new HashSet<>();
        tokenCacheEntity.getAccessTokens().values().stream().forEach(refreshTokenCache -> {
            if (StringUtils.equalsIgnoreCase(refreshTokenCache.getClientId(), VISUAL_STUDIO_CLIENT_ID)) {
                CachedAccountEntity accountCache = accountMap.get(refreshTokenCache.getHomeAccountId());
                Optional<String> envKey = envEndpoints.keySet().stream().filter(q -> refreshTokenCache.getTarget().startsWith(q)).findFirst();
                if (envKey.isPresent() && accountCache != null) {
                    if (this.environment != null && this.environment != envEndpoints.get(envKey.get())) {
                        // if env is specified, we need to ignore the accounts on other environments
                        return;
                    }
                    sharedAccounts.add(Pair.of(envEndpoints.get(envKey.get()), accountCache));
                }
            }
        });

        // where there are multiple accounts, we will prefer azure global accounts
        Optional<Pair<AzureEnvironment, CachedAccountEntity>> firstAccount = sharedAccounts.stream()
                .filter(accountInCache -> accountInCache.getKey() == AzureEnvironment.AZURE).findFirst();

        if (!firstAccount.isPresent()) {
            // where there are multiple non-global accounts, select any of them
            firstAccount = sharedAccounts.stream().findFirst();
        }
        if (!firstAccount.isPresent()) {
            return accountEntity;
        }
        accountEntity.setEnvironment(firstAccount.get().getKey());
        accountEntity.setEmail(firstAccount.get().getValue().getUsername());
        AzureEnvironmentUtils.setupAzureEnvironment(accountEntity.getEnvironment());
        accountEntity.setCredentialBuilder(new ICredentialProvider() {
            @Override
            public TokenCredential provideCredentialForTenant(String tenantId) {
                return new SharedTokenCacheCredentialBuilder().clientId(VISUAL_STUDIO_CLIENT_ID).tenantId(tenantId).username(accountEntity.getEmail()).build();
            }

            @Override
            public TokenCredential provideCredentialCommon() {
                return new SharedTokenCacheCredentialBuilder().clientId(VISUAL_STUDIO_CLIENT_ID).username(accountEntity.getEmail()).build();
            }
        });
        AccountBuilderUtils.listTenants(accountEntity);
        return accountEntity;
    }

    private static <T> T convertByJson(Object from, Class<T> toClass) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = objectMapper.writeValueAsString(from);
        return objectMapper.readValue(json, toClass);
    }
}
