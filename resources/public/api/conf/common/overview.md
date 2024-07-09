There is a government initiative to encourage parents to use the Tax-Free Childcare (TFC) allowance available to them. This API allows online cashless payment providers to offer parents the opportunity to pay for their childcare with their TFC balance through the provider's platform.
There are 3 different operations involved in this process:
- Linking the parent's TFC account with their account on the online payment provider's platform
- Checking the balance of the child's TFC account
- Making a payment from the child's TFC account to the childcare provider

### Prerequisites

For the technical details on how to start using our APIs, please read the [Using the Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/using-the-hub) page. Once you have created an application in our sandbox environment, you will need to follow the below process to subscribe to our API.

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

### Testing

> _This guidance is in draft and is subject to change._

To allow users to be able to test the new TFC Payments API, we have stubbed the integration with downstream services. To act as a realistic representation of the production API, the stub provides predictable responses based on the provided test scenarios. 

You can simulate common error responses by sending messages with our predefined test inputs. These are detailed in the test scenarios below.

#### Before you start

1. Obtain your [Client ID and Client secret credentials](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/credentials).
2. Set the Redirect URI on your test app to `\urn:ietf:wg:oauth:2.0:oob\`.

#### 1. Create a Test User

1. Go to the [test user service page](https://developer.service.hmrc.gov.uk/api-test-user).
2. Select 'Individual' and click **Create**.
3. Make note of the User ID and Password.

#### 2. Generate Authorisation code

1. Visit [https://test-www.tax.service.gov.uk/oauth/authorize?client_id=\[CLIENT-ID\]&redirect_uri=\urn:ietf:wg:oauth:2.0:oob\&scope=tax-free-childcare-payments&response_type=code](https://test-www.tax.service.gov.uk/oauth/authorize?client_id=%5BCLIENT-ID%5D&redirect_uri=\urn:ietf:wg:oauth:2.0:oob\&scope=tax-free-childcare-payments&response_type=code).
2. Click **Continue**.
3. Click **Sign in to the HMRC online service**.
4. Enter your User ID and Password, click **Sign in**.
5. Click **Give permission**.

You should now have view of the Authorisation code required to generate the bearer token.

#### 3. Generate a Bearer Token:

Use the below `POST` to generate the bearer token (replace the \[AUTHORIZATION-CODE\],\[CLIENT-CODE\],\[CLIENT-SECRET\] and \[REDIRECT-URI\]).

````
POST /oauth/token HTTP/1.1
Accept: application/vnd.hmrc.1.0+json
Cache-Control: no-cache
Content-Type: application/x-www-form-urlencoded
Host: test-api.service.hmrc.gov.uk
code=\[AUTHORIZATION-CODE\]&client_id=\[CLIENT-CODE\]&client_secret=\[CLIENT-SECRET\]&grant_type=authorization_code&redirect_uri=\[REDIRECT-URI\]
````

A successful response will return a user-restricted access bearer token.

#### 4. Connect to the API

Note: Correlation-ID can be any randomly generate UUID.

### Test Scenarios

We use test `outbound_child_payment_ref` values to trigger predefined responses. Only the first 4 letters determine the test response, the rest of the string has no bearing on the response. The standard format is 4 letters, 5 numbers, followed by 'TFC'.


#### individuals/tax-free-childcare/payments/link

| **outbound_child_payment_ref** | **Scenario** | **Example response** |
| --- | --- | --- |
| AAAA00000TFC | Returns "Peter Pan" as the child's name | <pre class="code--block">{<br>"child_name": "Peter Pan"<br>}</pre> |
| AABB00000TFC | Returns "Benjamin Button" as the child's name | <pre class="code--block">{<br>"child_name": "Benjamin Button"<br>}</pre> |
| AACC00000TFC | Returns "Christopher Columbus" as the child's name | <pre class="code--block">{<br>"child_name": "Christopher Columbus"<br>}</pre> |
| AADD00000TFC | Returns "Donald Duck" as the child's name | <pre class="code--block">{<br>"child_name": "Donald Duck"<br>}</pre> |


#### individuals/tax-free-childcare/payments/balance

| **outbound_child_payment_ref** | **Scenario** | **Example response** |
| --- | --- | --- |
| AAAA00000TFC<br><br>AABB00000TFC<br><br>AACC00000TFC<br><br>AADD00000TFC | Returns random cash amounts in pennies | <pre class="code--block">{<br>"tfc_account_status": "Active",<br> "paid_in_by_you": 80000,<br> "government_top_up": 20000,<br> "total_balance": 100000,<br> "cleared_funds": 100000,<br> "top_up_allowance": 15000<br>}</pre> |


#### individuals/tax-free-childcare/payments/

| **outbound_child_payment_ref** | **Scenario** | **Example response** |
| --- | --- | --- |
| AAAA00000TFC<br><br>AABB00000TFC<br><br>AACC00000TFC<br><br>AADD00000TFC | Returns a random payment reference and a random date | <pre class="code--block">{<br>"payment_reference":"C4E09ZY4A00A0009"<br> "estimated_payment_date": "2024-10-10"<br>}</pre> |
