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

`relayConnectionName:` Hybrid Connection name. Must the same value as the EntityPath.

`targetProperties.targetHost:` The local or private endpoint where the listener must forward all requests.

## Running Jupyter Notebooks

To enable access to a Jupyter Notebooks server instance via Azure Relay using the listener,
the following configuration settings are required.

`c.NotebookApp.allow_origin = '*'`

*Note*: You could add an specific Azure Relay origin if required.

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


## Additional Considerations

- `Host` and `Via` HTTP headers are not forwarded to the private endpoint.
- WebSocket connections must target a specific URI pattern:
  - wss://<RELAY_HOST>/$hc/<HYBRID_CONNECTION_NAME>


