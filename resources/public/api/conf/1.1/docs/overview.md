---
title: Overview
weight: 1
---

# Overview
  It is a government initiative to encourage more parents to use the tax-free childcare (TFC) allowance available to them. To promote this service we want to make it easier for parents to make payments using the funds in their tax-free childcare account.   
This API allows online cashless payment providers for schools to offer their users the opportunity to use their TFC balance to pay for their childcare through the providers platform.
There are three different operations involved in this process:
- Linking the parent's TFC account with their account on the online payment provider platform
- Checking the balance on the child's TFC account'.
- Making a payment from the child's TFC account to the childcare provider.

Requests are passed for processing to National Savings and Investments (NS&I), who hold the TFC account funds.

## Prerequisites
- The online payment provider will need to be registered with NS&I. NS&I will provide them with a unique ID which is required by this API.

## Security considerations

## Testing
You can use the HMRC Developer Sandbox to test the API. The Sandbox is an enhanced testing service that functions as a simulator of HMRC’s production environment.

## Errors
We use standard HTTP status codes to show whether an API request succeeded or not. They are usually in the range:
- 200 to 299 if it succeeded, including code 202 if it was accepted by an API that needs to wait for further action
- 400 to 499 if it failed because of a client error by your application
- 500 to 599 if it failed because of an error on our server

Errors specific to each API are shown in the Endpoints section, under Response. See our [reference guide](https://developer.service.hmrc.gov.uk/api-documentation/docs/reference-guide#errors) for more on errors.

## 

