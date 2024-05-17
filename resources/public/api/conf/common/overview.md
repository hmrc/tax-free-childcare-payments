It is a government initiative to encourage more parents to use the tax-free childcare (TFC) allowance available to them. To promote this service we want to make it easier for parents to make payments using the funds in their tax-free childcare account.   
This API allows online cashless payment providers for schools to offer their users the opportunity to use their TFC balance to pay for their childcare through the providers platform.
There are three different operations involved in this process:
- Linking the parent's TFC account with their account on the online payment provider platform
- Checking the balance on the child's TFC account.
- Making a payment from the child's TFC account to the childcare provider.

Requests are passed for processing to National Savings and Investments (NS&I), who hold the TFC account funds.

### Prerequisites
The online payment provider will need to be registered with NS&I. NS&I will provide them with a unique ID which is required by this API.

1. Ensure that you have an HMRC developer account - if you do not have one, you must register for an account.
2. Add your subscription to this API to your application.
3. Learn about the user-restricted authentication used by the API.
4. Create an application in our sandbox environment.


### Security considerations

Click [here](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/user-restricted-endpoints) to learn about the user-restricted authentication used by the API.

HMRC supports OAuth 2.0 for authenticating user restricted API requests using an OAuth 2.0 Bearer Token in the AUTHORIZATION header. Click [here](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation) to find out more about authorisation.

- Flow type: authorizationCode
- Authorization URL: https://api.service.hmrc.gov.uk/oauth/authorize
- Token URL: https://api.service.hmrc.gov.uk/oauth/token
- Refresh URL: https://api.service.hmrc.gov.uk/oauth/refresh
- Required scopes: tax-free-childcare-payments
