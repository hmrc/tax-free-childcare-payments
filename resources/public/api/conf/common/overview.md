There is a government initiative to encourage more parents to use the tax-free childcare (TFC) allowance available to them. To promote this service, this API allows online cashless payment providers for schools to offer parents the opportunity to use their TFC balance to pay for their childcare through the providers platform.
There are three different operations involved in this process:
- Linking the parent's TFC account with their account on the online payment provider's platform
- Checking the balance on the child's TFC account.
- Making a payment from the child's TFC account to the childcare provider.

Requests are passed for processing to National Savings and Investments (NS&I), who hold the TFC account funds.

### Prerequisites

For the technical details on how to start using our APIs, please read the [Using the Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/using-the-hub) page.

#### Subscribing to the TFC API

Due to the nature of the TFC API, you will need to request to subscribe to the API from our Software Developers Support Team (SDST).

1. In the Developer hub, click [Support](https://developer.service.hmrc.gov.uk/developer/support) in the menu bar to go to the Support request page.
2. Enter your details in the **Full name** and **Email address** fields.
3. In the **What do you need help with?** field, enter your sandbox application's client ID and request that you would like this application to be subscribed to the TFC APIs. You will also need to provide a brief
explanation of why you need this access.
4. Click **Submit**, this will generate a ticket which will be actioned or acknowledged within two working days.
 
### Security considerations

Refer to the [Authorisation page](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/user-restricted-endpoints) to learn about the user-restricted authentication used by the API.

HMRC supports OAuth 2.0 for authenticating user restricted API requests using an OAuth 2.0 Bearer Token in the AUTHORIZATION header. The [Authorisation](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation) page describes this approach in detail.

- Flow type: authorizationCode
- Authorization URL: https://api.service.hmrc.gov.uk/oauth/authorize
- Token URL: https://api.service.hmrc.gov.uk/oauth/token
- Refresh URL: https://api.service.hmrc.gov.uk/oauth/refresh
- Required scopes: tax-free-childcare-payments
