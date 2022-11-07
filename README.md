# Terra Azure Relay Listener
The Terra Azure Relay Listener enables secure communications with private resources deployed in a customer subscription using Azure Relay.

The Terra Azure Relay Listener establishes a bi-directional channel with Azure Relay. Once the channel is established, the listener forwards HTTP requests to a private endpoint and returns the responses to the caller. The listener also supports WebSockets. The listener establishes a persistent WebSocket connection with the private resource and forwards data bidirectionally to and from the caller and private endpoint.

## Configuration Properties

`listener.relayConectionString`: Connection string for the Azure Relay instance.  e.g.:

```yaml
  relayConnectionString: "Endpoint=<END-PONT>;SharedAccessKeyName=<SAS TOKEN>;EntityPath=<HYBRID CONNECTION>`

```

>*Note*: The connection string must contain an EntityPath parameter.
> The value of EntityPath must be the Hybrid Connection name.

`relayConnectionName`: Hybrid Connection name. Must the same value as the EntityPath.

`listener.targetProperties.targetHost`: The default local or private endpoint where the listener must forward all requests.

`requestInspectors`: A list of request inspectors to be enabled.

`listener.targetProperties.removeEntityPathFromHttpUrl` If `true` the HTTP request to the target won't include the Entity Path (Hybrid Connection name) in the URL. The default value is `false`.

`listener.corsSupportProperties.preflightMethods` Methods that we support CORS. Default to `OPTIONS, POST, PUT, GET, DELETE, HEAD, PATCH`.

`listener.targetProperties.targetRoutingRules` A list of routing rules. A rule is a tuple of the string the URI must contain for a match and the target host.
The default `targetHost` is used if the request URI does not match any rule.

In a rule, you can specify path segments the listener must remove when creating the target URI
by using the property `removeFromPath`. In the value, you can use `$hc-name` to represent the hybrid connection name (entity path) as a segment to remove. The listener replaces `$hc-name` with the value in `relayConnectionName` to construct the string to find in the URI at runtime.

Example configuration:

```yaml
  targetProperties:
    targetHost: "http://localhost:8080"
    targetRoutingRules:
      -
        pathContains: "welder"
        targetHost: "http://localhost:8081"
        removeFromPath: "$hc-name/welder"
```
### Sam Inspector config options

By using the Sam Checker inspector, the Listener can be configured to allow access only for users
that have write access to a specific Sam resource.

`listener.samInspectorProperties.samUrl`: URL to the Sam instance we should talk to

`listener.samInspectorProperties.samResourceId`: The id of the Sam resource to check access to

`listener.samInspectorProperties.samResourceType`: The type of the Sam resource to check access to.
Defaults to `controlled-application-private-workspace-resource`, which corresponds Leo-managed resources.

`listener.samInspectorProperties.samAction`: The Sam action to check. Default value is `write`

## Running Jupyter Notebooks

To enable access to a Jupyter Notebooks server instance via Azure Relay using the listener,
the following configuration settings are required.

`c.NotebookApp.allow_origin = '*'`

*Note*: You could add a specific Azure Relay origin if required.

`c.NotebookApp.base_url = '/<HYBRID CONNECTION NAME>/'`

Where HYBRID_CONNECTION_NAME is the configured Hybrid Connection name.

`c.NotebookApp.websocket_url= 'wss://<AZURE_RELAYED_HOST>>/$hc'`

## Running Jupiter Labs (DSVM)

In DSVM, Jupyter server config file lives in /home/azureuser/.jupyter/jupyter_server_config.py. Modify the same variables similar to Jupyter notebooks

e.g
```markdown
c.ServerApp.allow_origin = '*'
c.ServerApp.base_url = '/qi-2-16/'
c.ServerApp.websocket_url = 'wss://qi-relay.servicebus.windows.net/$hc/qi-2-16'
```
## Request Inspectors

The listener enables the inspection of the relayed HTTP requests.
HTTP requests could be accepted or rejected after being inspected.
All enabled request inspectors will be executed for each request.
If at least one returns `false` the request will be rejected.

An inspector is an implementation of the following interface:
```java
public interface RequestInspector {

  public boolean inspectWebSocketUpgradeRequest(
      RelayedHttpListenerRequest relayedHttpListenerRequest);

  public boolean inspectRelayedHttpRequest(RelayedHttpListenerRequest relayedHttpListenerRequest);
}
```

- `inspectRelayedHttpRequest` is called before the relayed HTTP request is forwarded to the target.
  If the implementation returns `false` the client will receive a `403` response.


- `inspectWebSocketUpgradeRequest` is called before the listener accepts the relayed WebSocket connection.
  If the implementation returns `false`, the listener will deny the WebSocket upgrade request.
  Azure Relay expects a response in less than 30 seconds; if an inspector blocks the requests for longer than that, the client will receive a timeout.

 - The implementation must be a named component, e.g.:

```java
@Component(InspectorNameConstants.HEADERS_LOGGER)
public class HeaderLoggerInspector implements RequestInspector {...}
```

For a new inspector, an entry must be added to:

```java
public enum InspectorType
```

An inspector can be enabled by adding its name to the `requestInspectors` list in the configuration file.

## Additional Considerations

- `Host` and `Via` HTTP headers are not forwarded to the private endpoint.
- WebSocket connections must target a specific URI pattern:
  - wss://<RELAY_HOST>/$hc/<HYBRID_CONNECTION_NAME>
