/*
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.accertify.core;

import java.util.Collection;
import java.util.Hashtable;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.plugin.accertify.api.AccertifyPaymentRoutingPluginApi;
import org.killbill.billing.plugin.accertify.client.AccertifyClient;
import org.killbill.billing.plugin.accertify.dao.AccertifyDao;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.routing.plugin.api.PaymentRoutingPluginApi;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.osgi.framework.BundleContext;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class AccertifyActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-accertify";

    public static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.accertify.";

    private AccertifyConfigurationHandler accertifyConfigurationHandler;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final AccertifyClient globalAccertifyClient = new AccertifyClient(configProperties.getProperties());
        accertifyConfigurationHandler.setDefaultConfigurable(globalAccertifyClient);

        // Configurable globally only
        final String paymentPluginsString = configProperties.getString(PROPERTY_PREFIX + "plugins");
        final String[] paymentPlugins = Strings.isNullOrEmpty(paymentPluginsString) ? new String[]{} : paymentPluginsString.split(",");
        final Collection<String> paymentPluginsSubjectToAutomaticRejection = ImmutableList.<String>copyOf(paymentPlugins);

        final AccertifyDao dao = new AccertifyDao(dataSource.getDataSource());
        final Clock clock = new DefaultClock();

        // Register the PaymentControlPluginApi
        final PaymentRoutingPluginApi paymentControlPluginApi = new AccertifyPaymentRoutingPluginApi(paymentPluginsSubjectToAutomaticRejection,
                                                                                                     dao,
                                                                                                     accertifyConfigurationHandler,
                                                                                                     killbillAPI,
                                                                                                     configProperties,
                                                                                                     logService,
                                                                                                     clock);
        registerPaymentControlPluginApi(context, paymentControlPluginApi);
    }

    @Override
    public OSGIKillbillEventDispatcher.OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        accertifyConfigurationHandler = new AccertifyConfigurationHandler(PLUGIN_NAME, killbillAPI, logService);
        return new PluginConfigurationEventHandler(accertifyConfigurationHandler);
    }

    private void registerPaymentControlPluginApi(final BundleContext context, final PaymentRoutingPluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentRoutingPluginApi.class, api, props);
    }
}
