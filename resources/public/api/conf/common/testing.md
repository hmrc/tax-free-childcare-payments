> _This guidance is in draft and is subject to change._

To allow users to be able to test the new TFC Payments API, we have stubbed the integration with downstream services. To act as a realistic representation of the production API, the stub provides predictable responses based on the provided test scenarios. 

You can simulate common error responses by sending messages with our predefined test inputs. These are detailed in the test scenarios below.

#### Before you start

1. Obtain your [Client ID and Client secret credentials](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/credentials).
2. Use the [Create Test User API](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/api-platform-test-user/1.0) or its [frontend service](https://developer.service.hmrc.gov.uk/api-test-user) to create test users.
3. Refer to the guidance on [user-restricted endpoints](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/user-restricted-endpoints) to generate an authorisation code and bearer token.

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
    <td>Returns the following: <br>
      <br><pre class="code--block">
        {
          "tfc_account_status": "ACTIVE",
          "government_top_up": 14159,
          "top_up_allowance": 26535,
          "paid_in_by_you": 89793,
          "total_balance": 23846
          "cleared_funds": 26433
        }
        </pre>
      </td>
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
    <td>Return the following: <br>
      <br><pre class="code--block">
        {
          "payment_reference": "8427950288419716",
          "estimated_payment_date": "2024-10-01",
        }
        </pre>
    </td>
  </tr>
  <tr></tr>
</table>

#### Error scenarios

The following scenarios relate to all endpoints:

<table>
    <tr>
        <td>outbound_child_payment_ref</td>
        <td>Scenario</td>
        <td>Example response</td>
    </tr>
    <tr>
        <td>EETT00000TFC</td>
        <td>Online payment provider's registration is not Active</td>
        <td>400<br>
           <pre class="code--block">
              {
               "errorCode": "E0030",
               "errorDescription": "Request data is invalid or missing. Please refer to API Documentation for further information",
              }
           </pre></td>
    </tr>
    <tr>
        <td>EEXX00000TFC</td>
        <td>Error returned from banking services</td>
        <td>503<br>
           <pre class="code--block">
              {
               "errorCode": "E0034",
               "errorDescription": "The service is unavailable. Please refer to API Documentation for further information.",
              }
           </pre></td>
    </tr>
</table>

##### Linking endpoint errors

<table>
    <tr>
        <td>outbound_child_payment_ref</td>
        <td>Scenario</td>
        <td>Example response</td>
    </tr>
    <tr>
        <td>EEQQ00000TFC</td>
        <td>The given child_dob does not correlate with the provided outbound_child_payment_ref</td>
        <td>400<br>
           <pre class="code--block">
              {
               "errorCode": "E0025",
               "errorDescription": "Request data is invalid or missing. Please refer to API Documentation for further information",
              }
           </pre></td>
    </tr>
</table>

##### Payment endpoint errors

<table>
    <tr>
        <td>outbound_child_payment_ref</td>
        <td>Scenario</td>
        <td>Example response</td>
    </tr>
    <tr>
        <td>EEUU00000TFC</td>
        <td>The childcare provider's registration is not Active</td>
        <td>400<br>
           <pre class="code--block">
              {
               "errorCode": "E0031",
               "errorDescription": "Request data is invalid or missing. Please refer to API Documentation for further information",
              }
           </pre></td>
    </tr>
    <tr>
        <td>EEWW00000TFC</td>
        <td>Insufficient funds</td>
        <td>400<br>
           <pre class="code--block">
              {
               "errorCode": "E0033",
               "errorDescription": "Request data is invalid or missing. Please refer to API Documentation for further information.",
              }
           </pre></td>
    </tr>
    <tr>
        <td>EEYY00000TFC</td>
        <td>Payments from this TFC account are blocked</td>
        <td>400<br>
           <pre class="code--block">
              {
               "errorCode": "E0035",
               "errorDescription": "Request data is invalid or missing. Please refer to API Documentation for further information.",
              }
           </pre></td>
    </tr>
</table>
