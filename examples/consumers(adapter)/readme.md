## RMQ RPC PoC 

This document contains the configurations and installations to run this PoC.


<p align="center">
<img src="RMQ RPC.png">
</p>







### Components involved.
- Producer (in this case rs-proxy-server acts as producer and publish queries on RMQ to be consumed)
- RabbitMQ (used to receive and distribute the queries to appropriate consumer)
- Consumer (usually adpter scripts, who will consume the queries and publish the result back to RMQ) 


### Prerequisite

1. Setting up RMQ exchanges, queues and bindings

 - **Exchange** : An exchange is required to push all the queries. 
      > current setup uses `rpc-adapter-requests` exchange to push all queries.
  
  - **Queue**    : different queues are needed according to the resource items/group. these queues are subscribed by consumer/adapter to receive queries.
  > current setup uses two queues `rpc_pune-aqm` & `rpc_surat-itms`
  
  - **Bindings** : An appropriate routing key will be used to bind exchanges to queues. bindings in RMQ helps to route messages from exchanges to queues.
  > Current setup use an `id` based binding to route messages/queries from exchanges  to queues.
 
2. Starting `rs-proxy-server`.
Start your rs-proxy-server following this guide [rs-proxy-setup](https://github.com/datakaveri/iudx-rs-proxy/blob/main/README.md "rs-proxy-setup")
3. Starting Consumer/adapter scripts.
Consumer/adapter script can be a a client written in any language to consume messages from queues and returning response to the appropriate queues.

### Message format for consumers

A typical message received by consumer will be a query in json format (exact symentics can be decided). In addition to json body a consumer strictly required to look for `correlation_Id ` and `reply_to` fields in message properties.

> **correlation_id** : This property will represent an unique id for every request. consumer is strictly required to send this correlation_id back in the response properties as it was received,
> **reply_to** : This property will represent the **queue name** consumer needs to use to publish response. Consumer is required to publish response on this queue only else response will not be received by the proxy-server.

### Starting consumer scripts

Two example python scripts can be found here, these scripts can be used to start a consumers and reply back to the proxy-server.

*start surat consumer*
``` console
kailash@kailash ~$ python3 rpc_adapter_surat.py
```

*start pune consumer*
``` console
kailash@kailash ~$ python3 rpc_adapter_pune.py
```

