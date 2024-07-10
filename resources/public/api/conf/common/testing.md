> _This guidance is in draft and is subject to change._

To allow users to be able to test the new TFC Payments API, we have stubbed the integration with downstream services. To act as a realistic representation of the production API, the stub provides predictable responses based on the provided test scenarios. 

You can simulate common error responses by sending messages with our predefined test inputs. These are detailed in the test scenarios below.

#### Before you start

1. Obtain your [Client ID and Client secret credentials](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/credentials).
2. Set the Redirect URI on your test app to `urn:ietf:wg:oauth:2.0:oob`.
3. Use the [Create Test User API](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/api-platform-test-user/1.0) or it's [frontend service](https://developer.service.hmrc.gov.uk/api-test-user) to create test users.


#### 1. Generate Authorisation code

1. Visit [https://test-www.tax.service.gov.uk/oauth/authorize?client_id=\[CLIENT-ID\]&redirect_uri=urn:ietf:wg:oauth:2.0:oob&scope=tax-free-childcare-payments&response_type=code](https://test-www.tax.service.gov.uk/oauth/authorize?client_id=%5BCLIENT-ID%5D&redirect_uri=\urn:ietf:wg:oauth:2.0:oob\&scope=tax-free-childcare-payments&response_type=code).
2. Click **Continue**.
3. Click **Sign in to the HMRC online service**.
4. Enter your User ID and Password, click **Sign in**.
5. Click **Give permission**.

You should now have view of the Authorisation code required to generate the bearer token.

#### 2. Generate a Bearer Token:

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

#### 3. Connect to the API

> `Correlation-ID` can be any randomly generated UUID.

### Test Scenarios

We use test `outbound_child_payment_ref` values to trigger predefined responses. Only the first 4 letters determine the test response, the rest of the string has no bearing on the response. The standard format is 4 letters, 5 numbers, followed by 'TFC'.


#### individuals/tax-free-childcare/payments/link

<table>
  <tr>
    <th>outbound_child_payment_ref</th>
    <th>Scenario</th>
  </tr>
  <tr>
    <td>AAAA00000TFC</td>
    <td>Returns "Peter Pan" as the child's name</td>
  </tr>
  <tr>
    <td>AABB00000TFC</td>
    <td>Returns "Benjamin Button" as the child's name</td>
  </tr>
  <tr>
    <td>AACC00000TFC</td>
    <td>Returns "Christopher Columbus" as the child's name</td>
  </tr>
  <tr>
    <td>AADD00000TFC</td>
    <td>Returns "Donald Duck" as the child's name</td>
  </tr>
</table>

#### individuals/tax-free-childcare/payments/balance

<table>
  <tr>
    <th>outbound_child_payment_ref</th>
    <th>Scenario</th>
  </tr>
  <tr>
    <td>AAAA00000TFC <br>
      <br>AABB00000TFC <br>
      <br>AACC00000TFC <br>
      <br>AADD00000TFC
    </td>
    <td>Returns random cash amounts in pennies</td>
  </tr>
</table>

#### individuals/tax-free-childcare/payments/

<table>
  <tr>
    <th>outbound_child_payment_ref</th>
    <th>Scenario</th>
    <th></th>
  </tr>
  <tr>
    <td>AAAA00000TFC <br>
      <br>AABB00000TFC <br>
      <br>AACC00000TFC <br>
      <br>AADD00000TFC
    </td>
    <td>Returns a random payment reference and a random date</td>
  </tr>
  <tr></tr>
</table>