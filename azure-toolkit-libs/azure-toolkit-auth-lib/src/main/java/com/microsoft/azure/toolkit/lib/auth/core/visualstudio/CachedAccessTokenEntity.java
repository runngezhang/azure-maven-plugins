/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.visualstudio;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
class CachedAccessTokenEntity {
    @JsonProperty("home_account_id")
    private String homeAccountId;
    @JsonProperty("environment")
    private String environment;
    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("target")
    private String target;
}
