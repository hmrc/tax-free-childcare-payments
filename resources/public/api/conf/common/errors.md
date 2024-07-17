We use standard HTTP status codes to show whether an API request succeeded or not. 

The following table contains a list of each HTML status code the API returns, along with their corresponding error codes and message. For more detail on the error codes, see the Error mapping section of the [Endpoints documentation](/oas/page).

<table><colgroup><col /><col /><col /></colgroup>
<thead>
<tr>
<th tabindex="0" scope="col" data-column="0">
<div>HTML Error</div>
</th>
<th tabindex="0" scope="col" data-column="1">
<div>Error codes</div>
</th>
<th tabindex="0" scope="col" data-column="2">
<div>Error message</div>
</th>
</tr>
</thead>
<tbody>
<tr>
<td>400</td>
<td>
<p>ETFC1<br />E0001<br />E0002<br />E0003<br />E0004<br />E0006<br />E0007<br />E0008<br />E0021<br />E0022<br />E0023<br />E0024<br />E0025<br />E0026<br />E0030<br />E0031<br />E0032<br />E0033<br />E0035<br />E0040<br />E0041<br />E0042<br />E0043</p>
</td>
<td>"Request data is invalid or missing. Please refer to API Documentation for further information"</td>
</tr>
<tr>
<td>500</td>
<td>ETFC2<br />E0000<br />E0002<br />E0003<br />E0004<br />E0005<br />E0006<br />E0007<br />E0008<br />E0021<br />E0022<br />E0023<br />E0401<br />E0001</td>
<td>"The server encountered an error and couldn't process the request. Please refer to API Documentation for further information"</td>
</tr>
<tr>
<td>502</td>
<td>E0020</td>
<td>"Bad Gateway. Please refer to API Documentation for further information"</td>
</tr>
<tr>
<td>503</td>
<td>E0034<br />E9000<br />E9999<br />E8000<br />E8001</td>
<td>"The service is currently unavailable. Please refer to API Documentation for further information"</td>
</tr>
</tbody>
</table>