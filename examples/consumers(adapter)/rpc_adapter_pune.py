#!/usr/bin/env python
import pika
import sys
import time
import ssl
import os
import json
import datetime


username = ''
password = ''
host = ''
port = 1234
vhost=''
context = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)

ssl_options=pika.SSLOptions(context)
credentials = pika.PlainCredentials(username,password)

connection = pika.BlockingConnection(
    pika.URLParameters(f'amqps://{username}:{password}@{host}:{port}/{vhost}'))
    # cp)

channel = connection.channel()

def on_request(ch, method, props, body):

    print(" [INFO] Pune-aqm")
    print(" [INFO] Received request with body")
    print(" [INFO] "+str(body))
    print(" [INFO] "+str(props))

    requestJson=json.loads(body)
    requestId=requestJson['searchType'];

    dictionary = {'adapter':'pune-aqm','correlation_id':props.correlation_id, 'reply_to_queue':props.reply_to, 'id':'requestId','response':requestJson}
    jsonString = json.dumps(dictionary, indent=4)
    
    reply_queue=props.reply_to

    #print(" [INFO] Sleep for 10 sec")
    #time.sleep(10);
    print(" [INFO] Publishing query response")
    ch.basic_publish(exchange='',
                     routing_key=reply_queue,
                     properties=pika.BasicProperties(correlation_id = props.correlation_id),
                     body=str(jsonString))
    ch.basic_ack(delivery_tag=method.delivery_tag)

channel.basic_qos(prefetch_count=1)
channel.basic_consume(queue='rpc_pune-aqm', on_message_callback=on_request)

print(" [x] Awaiting RPC requests")
channel.start_consuming()
