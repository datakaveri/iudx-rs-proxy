import pika
import ssl
import json
import base64
from nacl.public import SealedBox, PublicKey


username = ''
password = ''
host = 'databroker.iudx.io'
port = 24567
vhost = 'IUDX-INTERNAL'
context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)

ssl_options=pika.SSLOptions(context)
credentials = pika.PlainCredentials(username,password)



connection = pika.BlockingConnection(
    pika.URLParameters(f'amqps://{username}:{password}@{host}:{port}/{vhost}'))
# cp)

channel = connection.channel()

print("[INFO] Connecting to RabbitMQ: Credentials Verifies :) ")

def on_message_received(ch, method, properties, body):

    print("\n [INFO] Received new message")

    # Get the JSON message from the body

    json_data = open(r"data_txt.json", "w")
    json_data.write(body.decode())
    json_data.close()
    # convert properties to dictionary format

    props_dict = properties.__dict__
    print("properties : ",properties)
    # Get the public key from the properties

    str_b_public_key = props_dict['headers']['publicKey']
    print("encoded public key : ", str_b_public_key)
    if str_b_public_key is None or str_b_public_key.isspace():
        print("Public key is null or empty")
        requestJson = json.loads(body)
        requestId = requestJson['searchType'];
        dictionary = {'adapter': 'pune-aqm', 'correlation_id': properties.correlation_id, 'reply_to_queue': properties.reply_to,
                      'id': 'requestId', 'response': requestJson}
        jsonString = json.dumps(dictionary, indent=4)
        reply_queue = properties.reply_to
        print(" [INFO] Publishing query response")
        ch.basic_publish(exchange='',
                         routing_key=reply_queue,
                         properties=pika.BasicProperties(correlation_id=properties.correlation_id),
                         body=str(jsonString))
        ch.basic_ack(delivery_tag=method.delivery_tag)
        print(method.delivery_tag)
    else:
        b64_b_public_key = base64.urlsafe_b64decode(str_b_public_key)

        # convert the bytes to object type

        public_key = PublicKey(b64_b_public_key)

        # Create a SealedBox object

        sealed_box = SealedBox(public_key)

        # Load the JSON data to a python variable

        with open('data_txt.json') as f:
            message = json.load(f)

        message_bytes = json.dumps(message).encode()

        encrypted = sealed_box.encrypt(message_bytes)

        # encrypted is of type bytes and it is not JSON Serializable
        # We convert it to string and send it

        b64_encrypted = base64.urlsafe_b64encode(encrypted)

        str_encrypted = b64_encrypted.decode("utf-8")

        sample_dataset = {
            "results":[
                {
                    "encrypted_data": [str_encrypted]
                }
            ]
        }
        requestJson = json.loads(body)
        requestId = requestJson['searchType'];
        dictionary = {'adapter':'pune-aqm','correlation_id':properties.correlation_id, 'reply_to_queue':properties.reply_to, 'id':requestId,'response':requestJson,'results': {'encryptedData':str_encrypted}}
        jsonString = json.dumps(dictionary, indent=4)


        print("\n[INFO] Publishing query response to ")

        reply_queue = props_dict['reply_to']
        print(reply_queue)

        # publish the encrypted data to the reply queue
        ch.basic_publish(exchange='',
                         routing_key=reply_queue,
                         properties=pika.BasicProperties(correlation_id=properties.correlation_id),
                         body=str(jsonString))
        ch.basic_ack(delivery_tag=method.delivery_tag)



channel.basic_qos(prefetch_count=1)
channel.basic_consume(queue='rpc_pune-aqm', auto_ack=False,
                      on_message_callback=on_message_received)


print(" [x] Awaiting RPC requests")
channel.start_consuming()
