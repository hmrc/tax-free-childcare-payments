
# tax-free-childcare-payments

acronyms used:
<br />
**TFC** - the government held tax free childcare account, from which parent can benefit from gov top-ups
<br />
**EPP** - external apps that parent typically use to pay for their childcare invoices.
<br />
**CCP** - childcare providers

Parents usually, either use TFC account or EPP account to pay for their children activities invoices.
By using the EPP account they might miss out on the advantage of top-up payments.
They only benefit from the top-up payment contributions if they log in into their TFC account.

The purpose of this set of these apis is to bridge this gap to allow parent to link, request balance and instruct payment to
CCPs. This not only helps them to access top-up payment from their EPP app, it also assists them to having to switch between 2 accounts.

the process is for any government registered EEP app, the parent upon logging into their EPP account:

they get a chance to authorise and link their EPP to their TFC account for a child in question.
The parent is then directed to log in into government gateway and give permission to the EPP to act on their behalf.

Once the permission is given then the EPP app can request balance from their corresponding TFC account.

Then, if the parent has an invoice to pay, then they can select the amount and target CCP or EPP.

## Run locally
```java
./run_local.sh
```