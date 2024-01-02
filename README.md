# Terra Azure Relay Listener

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DataBiosphere_terra-azure-relay-listeners&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DataBiosphere_terra-azure-relay-listeners)

The Terra Azure Relay Listener enables secure bidirectional communication with private resources
deployed in a customer subscription using the
[Azure Relay](https://learn.microsoft.com/en-us/azure/azure-relay/relay-what-is-it).

Once the channel is established, the listener forwards HTTP requests to a private endpoint and
returns the responses to the caller. The listener also supports WebSockets.
The listener establishes a persistent WebSocket connection with the private resource and
forwards data bidirectionally to and from the caller and private endpoint.

## Configuration Properties

Configuration for the service is documented in the [`application.yml` file.](./service/src/main/resources/application.yml)

##### Usage

By default, any request successfully relayed by this listener (a maximum of once per `callWindowInSeconds`) will trigger a request to PATCH `${serviceHost}/api/v2/runtimes/${workspaceId}/${runtimeName}/updateDateAccessed`. To tell the listener to pass a request without sending this PATCH, include the *custom HTTP header* `X-SetDateAccessedInspector-Action=ignore` in the request headers. For example, from a Scala service using Http4s, you could add the following to your `Headers()` object:

```
Header.Raw(CIString("X-SetDateAccessedInspector-Action"), "ignore")
```

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
