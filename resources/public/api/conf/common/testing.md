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
  <tr>
    <td>AABB00000TFC
    </td>
    <td>Returns the following: <br>
      <br><pre class="code--block">
{
 "tfc_account_status": "INACTIVE",
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
        <th colspan="2" align="left">EEAA00000TFC</td>
    </tr>
    <tr>
        <td>Invalid input data</td>
        <td>500<br>
           <pre class="code--block">
{
 "errorCode": "E0000",
 "errorDescription": "We encountered an error on our servers and did not process your request, please try again later.",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EELL00000TFC</td>
    </tr>
    <tr>
        <td>Parent's National Insurance number does not match the expected format</td>
        <td>502<br>
           <pre class="code--block">
{
 "errorCode": "E0020",
 "errorDescription": "Bad Gateway",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EERR00000TFC</td>
    </tr>
    <tr>
        <td>outbound_child_payment_ref does not match the parent's National Insurance number</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0026",
 "errorDescription": "Please check the outbound_child_payment_ref supplied",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EETT00000TFC</td>
    </tr>
    <tr>
        <td>Online payment provider's registration is not Active</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0030",
 "errorDescription": "The External Payment Provider (EPP) record is inactive on the TFC system. Please ensure EPP completes sign up process on TFC Portal or contact HMRC POC for further information.",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEXX00000TFC</td>
    </tr>
    <tr>
        <td>An error is returned by the parent's banking services</td>
        <td>503<br>
           <pre class="code--block">
{
 "errorCode": "E0034",
 "errorDescription": "The service is currently unavailable",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEZZ00000TFC</td>
    </tr>
    <tr>
        <td>outbound_child_payment_ref not found</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0040",
 "errorDescription": "The outbound_child_payment_ref could not be found in the TFC system - please ensure parent checks their details and tries again.",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEBA00000TFC</td>
    </tr>
    <tr>
        <td>epp_reg_reference not found</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0041",
 "errorDescription": "The epp_reg_reference could not be found in the TFC system. Please check the details and try again.",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEBD00000TFC</td>
    </tr>
    <tr>
        <td>Parent's National Insurance number not found</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0043",
 "errorDescription": "Parent associated with the bearer token does not have a TFC account. Please ask the parent to create a TFC account first.",
}
           </pre></td>
    </tr>
</table>

##### Linking endpoint errors

<table>
    <tr>
        <th colspan="2" align="left">EEPP00000TFC</td>
    </tr>
    <tr>
        <td>epp_reg_reference and epp_unique_customer_id do not match</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0024",
 "errorDescription": "Please check that the epp_reg_reference and epp_unique_customer_id are both correct",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEQQ00000TFC</td>
    </tr>
    <tr>
        <td> child_date_of_birth and outbound_child_payment_ref do not match</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0025",
 "errorDescription": "Please check that the child_date_of_birth and outbound_child_payment_reference are both correct	",
}
           </pre></td>
    </tr>
</table>

##### Balance endpoint errors

<table>
    <tr>
        <th colspan="2" align="left">EESS00000TFC</td>
    </tr>
    <tr>
        <td>Authentication information is missing or invalid</td>
        <td>500<br>
           <pre class="code--block">
{
 "errorCode": "E0401",
 "errorDescription": "We encountered an error on our servers and did not process your request, please try again later.",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEVV00000TFC</td>
    </tr>
    <tr>
        <td>The Online Payment Provider's details do not match the outbound_child_payment_reference</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0032",
 "errorDescription": "The epp_unique_customer_id or epp_reg_reference is not associated with the outbound_child_payment_ref",
}
           </pre></td>
    </tr>
</table>

##### Payment endpoint errors

<table>
    <tr>
        <th colspan="2" align="left">EEPP00000TFC</td>
    </tr>
    <tr>
        <td>epp_reg_reference and epp_unique_customer_id do not match</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0024",
 "errorDescription": "Please check that the epp_reg_reference and epp_unique_customer_id are both correct",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EERS00000TFC</td>
    </tr>
    <tr>
        <td>ccp_reg_reference and outbound_child_payment_reference do not match</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0027",
 "errorDescription": "The CCP you have specified is not linked to the TFC Account. Please ensure that the parent goes into their TFC Portal and adds the CCP to their account first before attempting payment again later.",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEUU00000TFC</td>
    </tr>
    <tr>
        <td></td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0031",
 "errorDescription": "The CCP is inactive, please check the CCP details and ensure that the CCP is still registered with their childcare regulator and that they have also signed up to TFC via the TFC portal to receive TFC funds.",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEVV00000TFC</td>
    </tr>
    <tr>
        <td>The Online Payment Provider's details do not match the outbound_child_payment_reference</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0032",
 "errorDescription": "The epp_unique_customer_id or epp_reg_reference is not associated with the outbound_child_payment_ref",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEYY00000TFC</td>
    </tr>
    <tr>
        <td>Payments from this TFC account are blocked</td>
        <td>500<br>
           <pre class="code--block">
{
 "errorCode": "E0035",
 "errorDescription": "There is an issue with this TFC Account, please advise parent / carer to contact TFC customer Services.",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEYZ00000TFC</td>
    </tr>
    <tr>
        <td>The payee's bank details are incorrect</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0036",
 "errorDescription": "Error processing payment due to payee bank details",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEBC00000TFC</td>
    </tr>
    <tr>
        <td>ccp_reg_reference not found</td>
        <td>400<br>
           <pre class="code--block">
{
 "errorCode": "E0042",
 "errorDescription": "The ccp_reg_reference could not be found in the TFC system or does not correlate with the ccp_postcode. Please check the details and try again.",
}
           </pre></td>
    </tr>
    <tr>
        <th colspan="2" align="left">EEBE00000TFC</td>
    </tr>
    <tr>
        <td>Internal server error</td>
        <td>503<br>
           <pre class="code--block">
{
 "errorCode": "E9000",
 "errorDescription": "The service is currently unavailable",
}
           </pre></td>
    </tr>
</table>
