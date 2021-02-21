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
    @Builder.Default
    private Boolean isPublic = false;
    @Builder.Default
    private String runtimeVersion = "java_8";
    private String activeDeploymentName;
    @Builder.Default
    private SpringCloudDeploymentConfig deployment = SpringCloudDeploymentConfig.builder().build();

    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }
}
