/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.serviceplan;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkits.appservice.AzureAppService;
import com.microsoft.azure.toolkits.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlanCreator;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlanUpdater;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class AppServicePlan implements IAppServicePlan {

    private AppServicePlanEntity entity;
    private AzureAppService azureAppService;
    private AzureResourceManager azureClient;
    private com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlanClient;

    public AppServicePlan(AppServicePlanEntity appServicePlanEntity, AzureAppService appService) {
        this.entity = appServicePlanEntity;
        this.azureAppService = appService;
        this.azureClient = appService.getAzureResourceManager();
    }

    @Override
    public IAppServicePlanCreator create() {
        return new AppServicePlanCreator();
    }

    @Override
    public boolean exists() {
        return getPlanService(true) != null;
    }

    @Override
    public AppServicePlanEntity entity() {
        this.entity = AppServicePlanEntity.createFromServiceModel(getAppServicePlanClient());
        return entity;
    }

    @Override
    public AppServicePlanUpdater update() {
        return new AppServicePlanUpdater();
    }

    public com.azure.resourcemanager.appservice.models.AppServicePlan getAppServicePlanClient() {
        return getPlanService(false);
    }

    public synchronized com.azure.resourcemanager.appservice.models.AppServicePlan getPlanService(boolean force) {
        if (appServicePlanClient == null || force) {
            appServicePlanClient = StringUtils.isEmpty(entity.getId()) ?
                    azureClient.appServicePlans().getById(entity.getId()) :
                    azureClient.appServicePlans().getByResourceGroup(entity.getResourceGroup(), entity.getName());
        }
        return appServicePlanClient;
    }

    public class AppServicePlanCreator implements IAppServicePlanCreator {

        @Override
        public IAppServicePlanCreator withResourceGroup(String name) {
            return null;
        }

        @Override
        public AppServicePlan commit() {
            return AppServicePlan.this;
        }
    }

    public class AppServicePlanUpdater implements IAppServicePlanUpdater {
        private Optional<PricingTier> pricingTier;

        public AppServicePlanUpdater withPricingTier(PricingTier pricingTier) {
            this.pricingTier = Optional.of(pricingTier);
            return this;
        }

        @Override
        public AppServicePlan commit() {
            if (pricingTier != null && pricingTier.isPresent()) {
                final com.azure.resourcemanager.appservice.models.PricingTier newPricingTier = PricingTier.convertToServiceModel(pricingTier.get());
                if (newPricingTier != appServicePlanClient.pricingTier()) {
                    appServicePlanClient = appServicePlanClient.update().withPricingTier(newPricingTier).apply();
                }
            }
            return AppServicePlan.this;
        }
    }
}
