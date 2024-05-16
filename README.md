# tax-free-childcare-payments

acronyms used:
<br />
**TFC** - the government held tax-free childcare account, from which parent can benefit from gov top-ups
<br />
**EPP** - external apps that parent typically use to pay for their childcare invoices.
<br />
**CCP** - childcare providers

Parents usually, either use TFC account or EPP account to pay for their children activities invoices.
By using the EPP account they might miss out on the advantage of top-up payments.
They only benefit from the top-up payment contributions if they log in into their TFC account.

The purpose of this set of these apis is to bridge this gap to allow parent to link, request balance and instruct
payment to
CCPs. This not only helps them to access top-up payment from their EPP app, it also assists them to having to switch
between 2 accounts.

the process is for any government registered EEP app, the parent upon logging into their EPP account:

they get a chance to authorise and link their EPP to their TFC account for a child in question.
The parent is then directed to log in into government gateway and give permission to the EPP to act on their behalf.

Once the permission is given then the EPP app can request balance from their corresponding TFC account.

Then, if the parent has an invoice to pay, then they can select the amount and target CCP or EPP.

## Run locally

```bash
./run_local.sh
```

## Logging

### Implicit

| Scenario        | Level   | Logger Name                                               | Message Pattern                                                                      | Source              |
|-----------------|---------|-----------------------------------------------------------|--------------------------------------------------------------------------------------|---------------------|
| Inbound Request | `INFO`  | `uk.gov.hmrc.play.bootstrap.filters.DefaultLoggingFilter` | `[A-Z]+ \S+ \d{3} \d+ms`                                                             | [Bootstrap][BS]     |
| Outbound 200    | `DEBUG` | `connector`                                               | `\S*:[A-Z]+:\d+:\d+\.\d{3}[mnu]?s:\d+:\d+\.\d{3}[mnu]?s:[0-9a-f]{1,4}:\S+:ok`        | [HTTP Verbs][HV200] |
| Outbound 404    | `INFO`  | `connector`                                               | `\S*:[A-Z]+:\d+:\d+\.\d{3}[mnu]?s:\d+:\d+\.\d{3}[mnu]?s:[0-9a-f]{1,4}:\S+:failed .*` | [HTTP Verbs][HV404] |
| Outbound Error  | `WARN`  | `connector`                                               | `\S*:[A-Z]+:\d+:\d+\.\d{3}[mnu]?s:\d+:\d+\.\d{3}[mnu]?s:[0-9a-f]{1,4}:\S+:failed .*` | [HTTP Verbs][HVE]   |

### Explicit

| Scenario               | Level  | Logger Name                      | Message Pattern                                    |
|------------------------|--------|----------------------------------|----------------------------------------------------|
| Empty Nino Retrieval   | `INFO` | `controllers.actions.AuthAction` | `Unable to retrieve NI number.`                    |
| Missing Correlation ID | `INFO` | `controllers.actions.AuthAction` | `Correlation ID is missing.`                       |
| Invalid Correlation ID | `INFO` | `controllers.actions.AuthAction` | `(UUID string too large\|Invalid UUID string: .*)` |
| Bad Request Payload    | `INFO` | `config.customJsonErrorHandler`  | `Json validation error.*`                          |

[BS]: https://github.com/hmrc/bootstrap-play/blob/7a8a302b63cda07119f13ec21ad7ae82a45c966f/bootstrap-common-play-30/src/main/scala/uk/gov/hmrc/play/bootstrap/filters/LoggingFilter.scala#L61-L69

[HV200]: https://github.com/hmrc/http-verbs/blob/6af33f916da3d82297409ddacd7b18a4d454bdb4/http-verbs-play-30/src/main/scala/uk/gov/hmrc/http/logging/ConnectionTracing.scala#L39

[HV404]: https://github.com/hmrc/http-verbs/blob/6af33f916da3d82297409ddacd7b18a4d454bdb4/http-verbs-play-30/src/main/scala/uk/gov/hmrc/http/logging/ConnectionTracing.scala#L40-L43

[HVE]: https://github.com/hmrc/http-verbs/blob/6af33f916da3d82297409ddacd7b18a4d454bdb4/http-verbs-play-30/src/main/scala/uk/gov/hmrc/http/logging/ConnectionTracing.scala#L44
