There is a government initiative to encourage parents to use the Tax-Free Childcare (TFC) allowance available to them. This API allows online cashless payment providers for schools to offer parents the opportunity to pay for their childcare with their TFC balance through the provider's platform.
There are 3 different operations involved in this process:
- Linking the parent's TFC account with their account on the online payment provider's platform
- Checking the balance of the child's TFC account
- Making a payment from the child's TFC account to the childcare provider

Requests are sent to National Savings and Investments (NS&I), who hold the TFC account funds.

### Prerequisites

For the technical details on how to start using our APIs, please read the [Using the Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/using-the-hub) page.

#### Subscribing to the TFC API

You will need to request to subscribe to the API from our Software Developers Support Team (SDST).

1. Go to  [HMRC Developer Hub > Support](https://developer.service.hmrc.gov.uk/developer/support).
2. Enter your full name and email address.
3. In **What do you need help with?**, enter your sandbox application's client ID and ask for this application to be subscribed to the TFC API. Provide a brief
explanation of why you need this access.
4. Click **Submit**, a ticket will be generated which will be acknowledged within 2 working days.

 
### Security considerations

You must refer to the [Authorisation page](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/user-restricted-endpoints) to learn about the user-restricted authentication used by the API.

HMRC supports OAuth 2.0 for authenticating user-restricted API requests using an OAuth 2.0 Bearer Token in the AUTHORIZATION header. Read more about this approach on the [Authorisation](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation) page.

- Flow type: authorizationCode
- Authorization URL: https://api.service.hmrc.gov.uk/oauth/authorize
- Token URL: https://api.service.hmrc.gov.uk/oauth/token
- Refresh URL: https://api.service.hmrc.gov.uk/oauth/refresh
- Required scopes: tax-free-childcare-payments
