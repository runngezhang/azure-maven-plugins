/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.tools.common.model.Region;
import com.microsoft.azure.tools.common.model.ResourceGroup;
import com.microsoft.azure.tools.common.model.Subscription;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

@Getter
public abstract class AbstractAppServiceCreator<T> implements IAppServiceCreator<T> {

    public static final Runtime DEFAULT_RUNTIME = Runtime.LINUX_JAVA8;
    public static final Region DEFAULT_REGION = Region.EUROPE_WEST;
    public static final PricingTier DEFAULT_PRICING = PricingTier.BASIC_B1;

    private Runtime runtime = DEFAULT_RUNTIME;
    private Optional<String> name = null;
    private Optional<Subscription> subscription = null;
    private Optional<ResourceGroup> resourceGroup = null;
    private Optional<AppServicePlanEntity> appServicePlanEntity = null;
    private Optional<DockerConfiguration> dockerConfiguration = null;
    private Optional<Map<String, String>> appSettings = null;

    @Override
    public IAppServiceCreator<T> withName(String name) {
        this.name = Optional.of(name);
        return this;
    }

    @Override
    public IAppServiceCreator<T> withSubscription(String subscriptionId) {
        this.subscription = Optional.of(Subscription.builder().id(subscriptionId).build());
        return this;
    }

    @Override
    public IAppServiceCreator<T> withResourceGroup(String resourceGroupName) {
        this.resourceGroup = Optional.of(ResourceGroup.builder().name(resourceGroupName).build());
        return this;
    }

    public IAppServiceCreator<T> withPlan(String appServicePlanId) {
        this.appServicePlanEntity = Optional.of(AppServicePlanEntity.builder().id(appServicePlanId).build());
        return this;
    }

    public IAppServiceCreator<T> withPlan(String resourceGroup, String planName) {
        this.appServicePlanEntity = Optional.of(AppServicePlanEntity.builder().resourceGroup(resourceGroup).name(planName).build());
        return this;
    }

    @Override
    public IAppServiceCreator<T> withRuntime(Runtime runtime) {
        this.runtime = runtime;
        return this;
    }

    @Override
    public IAppServiceCreator<T> withDockerConfiguration(DockerConfiguration dockerConfiguration) {
        this.dockerConfiguration = Optional.of(dockerConfiguration);
        return this;
    }

    @Override
    public IAppServiceCreator<T> withAppSettings(Map<String, String> appSettings) {
        this.appSettings = Optional.ofNullable(appSettings);
        return this;
    }
}
