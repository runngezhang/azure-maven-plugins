/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;

@Builder
@Getter
@Setter
public class SpringCloudAppConfig {
    private String subscriptionId;
    private String clusterName;
    private String appName;
    private String resourceGroup;
    private Boolean isPublic;
    private String runtimeVersion;
    private String activeDeploymentName;
    private SpringCloudDeploymentConfig deployment;

    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }
}
