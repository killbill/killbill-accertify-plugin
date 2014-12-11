Accertify plugin
================

Kill Bill fraud plugin using [Accertify](http://www.accertify.com/).

Release builds are available on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.kill-bill.billing.plugin.java%22%20AND%20a%3A%22accertify-plugin%22) with coordinates `org.kill-bill.billing.plugin.java:accertify-plugin`.

Requirements
------------

The plugin needs a database. The latest version of the schema can be found here: https://raw.github.com/killbill/killbill-accertify-plugin/master/src/main/resources/ddl.sql.

Configuration
-------------

The following System Properties are required:

* `org.killbill.billing.plugin.accertify.url`: your Accertify url
* `org.killbill.billing.plugin.accertify.username`: your username
* `org.killbill.billing.plugin.accertify.password`: your password

The following System Properties are optional:

* `org.killbill.billing.plugin.accertify.plugins`: names of the payment plugins for which automatic rejection is enabled (separated by comma)
* `org.killbill.billing.plugin.accertify.proxyHost`: proxy host
* `org.killbill.billing.plugin.accertify.proxyPort`: proxy port
* `org.killbill.billing.plugin.accertify.strictSSL`: if false, unverified certificates are trusted

Usage
-----

Because your organization will have its own XML schema configured in Accertify, you need to pass relevant data for Accertify via plugin properties.
Any property starting with `accertify_` will be passed through, with the following naming convention:

* Single keys like `accertify_ipAddress` are passed as top elements in the `<transaction>` request element
* Composed keys like `accertify_collectionTransaction->billingFirstName` are passed as children elements
* Use brackets to create list elements: `orderDetails->orderDetail[0]->shippingName`

For example, the properties

* `accertify_ipAddress=127.0.0.1`
* `accertify_collectionTransaction->billingFirstName=John`
* `orderDetails->orderDetail[0]->shippingName=John Doe`

will produce the following XML:

```
<transactions>
  <transaction>
    <ipAddress>127.0.0.1</ipAddress>
    <collectionTransaction>
      <billingFirstName>John</billingFirstName>
    </collectionTransaction>
    <orderDetails>
      <orderDetail>
        <shippingName>John Doe</shippingName>
      </orderDetail>
    </orderDetails>
  </transaction>
</transactions>
```

By default, the plugin simply calls Accertify and store the response. Automatic rejection of the payments needs to be enabled on a per payment plugin basis.

For example, if the plugins `killbill-cybersource`, `killbill-paypal-express` and `killbill-bitpay` are installed and you want to reject only payments going to CyberSource and PayPal, specify:

```
org.killbill.billing.plugin.accertify.plugins=killbill-cybersource,killbill-paypal-express
```

Any payment for these plugins for which the assessment is `REJECT` will be aborted.
