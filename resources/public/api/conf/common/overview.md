There is a government initiative to encourage more parents to use the tax-free childcare (TFC) allowance available to them. To promote this service, this API allows online cashless payment providers for schools to offer parents the opportunity to use their TFC balance to pay for their childcare through the providers platform.
There are three different operations involved in this process:
- Linking the parent's TFC account with their account on the online payment provider's platform
- Checking the balance on the child's TFC account.
- Making a payment from the child's TFC account to the childcare provider.

Requests are passed for processing to National Savings and Investments (NS&I), who hold the TFC account funds.

### Prerequisites

For the technical details on how to integrate the API with your application, please read the [Using the Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/using-the-hub) page.
, 
The online payment provider will need to be registered with NS&I. NS&I will provide them with a unique ID which is required by this API.

### Security considerations

Refer to the [user restricted endpoints page](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/user-restricted-endpoints) to learn about the user-restricted authentication used by the API.

HMRC supports OAuth 2.0 for authenticating user restricted API requests using an OAuth 2.0 Bearer Token in the AUTHORIZATION header. The [Authorisation](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation) page describes this approach in detail.

- Flow type: authorizationCode
- Authorization URL: https://api.service.hmrc.gov.uk/oauth/authorize
- Token URL: https://api.service.hmrc.gov.uk/oauth/token
- Refresh URL: https://api.service.hmrc.gov.uk/oauth/refresh
- Required scopes: tax-free-childcare-payments
