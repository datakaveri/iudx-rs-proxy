import json
import re
import logging
from elasticsearch import Elasticsearch
from elasticsearch_dsl import Search, Q
import pika
from configparser import ConfigParser

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

#ElasticsearchLogHandler to handle the logs from elk client. so that we can access the status codes
class ElasticsearchLogHandler(logging.Handler):
    def __init__(self):
        super().__init__()
        self.logs = []

    def emit(self, record):
        log_message = self.format(record)
        if "http://database.iudx.io:24034/iudx__" in log_message:
            # Clear the logs list before appending new log messages
            self.logs.clear()
            self.logs.append(log_message)
        #print("Logs so far:", self.logs)

# Instantiate the ElasticsearchLogHandler
elasticsearch_log_handler = ElasticsearchLogHandler()

# Set the logging level of the ElasticsearchLogHandler to INFO
elasticsearch_log_handler.setLevel(logging.INFO)

# Configure the logging system
logger = logging.getLogger()
logger.addHandler(elasticsearch_log_handler)
logger.setLevel(logging.INFO)

class RabbitMqServerConfigure:
    def __init__(self, username, password, host, port, vhost, queue):
        self.username = username
        self.password = password
        self.host = host
        self.port = port
        self.vhost = vhost
        self.queue = queue

class RabbitmqServer:
    def __init__(self, server):
        self.server = server
        self.connection = pika.BlockingConnection(
            pika.URLParameters(f'amqps://{self.server.username}:{self.server.password}@{self.server.host}:{self.server.port}/{self.server.vhost}'))
        self.channel = self.connection.channel()
        logging.info("Server started waiting for Messages")

    def start_server(self, on_request):
        self.channel.basic_qos(prefetch_count=1)
        self.channel.basic_consume(
            queue=self.server.queue,
            on_message_callback=on_request
        )
        self.channel.start_consuming()

    def publish(self, payload, rout_key, corr_id, method):
        message = json.dumps(payload)
        self.channel.basic_publish(
            exchange='',
            routing_key=rout_key,
            properties=pika.BasicProperties(correlation_id=corr_id),
            body=message
        )
        self.channel.basic_ack(delivery_tag=method.delivery_tag)
        logging.info("Message is published")

class ElasticsearchDataHandler:
    def __init__(self, config, elasticsearch_log_handler):
        self.config = config
        self.elasticsearch_log_handler = elasticsearch_log_handler
        # Configure logging for Elasticsearch client
        elasticsearch_logger = logging.getLogger('elasticsearch.trace')
        logging.info(elasticsearch_logger.handlers)  # Check if ElasticsearchLogHandler is present among the handlers
        elasticsearch_logger.setLevel(logging.INFO)
        # Add the ElasticsearchLogHandler to the Elasticsearch logger
        elasticsearch_logger.addHandler(elasticsearch_log_handler)

    def search_pune_flood_data(self, json_object, query, rout_key, corr_id, method):
        elk_config = self.config['elasticsearch']
        client = Elasticsearch(
            [f"http://{elk_config['databaseIP']}:{elk_config['databasePort']}"],
            basic_auth=(elk_config['databaseUser'], elk_config['databasePassword'])
        )
        index_name = elk_config['index_name']

        if query:
            if "options" in json_object and json_object["options"] == "count":
                # Convert count object to a dictionary
                count_dict = query.to_dict()
                # Perform count query using the "_count" endpoint
                # Define the Elasticsearch query
                query = {
                    "query": {
                        "bool": {
                            "must": [
                                count_dict
                            ]
                        }
                    }
                }
                count_response = client.count(index=index_name, body=query)
                logging.info(count_response)
                count = count_response['count']
                response_payload = {
                    "count": count,
                    "statusCode": None # Placeholder for status code
                    }
            else:
                search = Search(index=index_name).using(client)
                search = search.query(query)
                response = search.execute()
                # Extract relevant data from the response object
                hits = [hit.to_dict() for hit in response.hits]
                # Serialize the extracted data to JSON
                response_payload = {
                    "hits": hits,
                    "statusCode": None  # Placeholder for status code
                }
        else:
            logging.info("Empty query")

        logging.info("Number of logs: %s", len(elasticsearch_log_handler.logs))
        # Extract relevant information from captured log messages of elk client
        for log in elasticsearch_log_handler.logs:
            logging.info(log)
            # Extract status code from log message using regular expression
            status_code_match = re.search(r'status:(\d+)', log)
            if status_code_match:
                status_code = int(status_code_match.group(1))
                print("Status Code:", status_code)
            else:
                logging.info("Not matched..")

        if response_payload:
            response_payload["statusCode"] = status_code  # Include the status code in the response payload
            server.publish(response_payload, rout_key, corr_id, method)

        logging.info("Query Completed for Pune-Flood data")

def process_request(ch, method, properties, body):
    logging.info("Request JSON received")
    logging.info("Received request with body: %s", body)
    json_object = json.loads(body)
    search_types = json_object['searchType'].split('_')
    rout_key = properties.reply_to
    corr_id = properties.correlation_id
    elasticsearch_log_handler = ElasticsearchLogHandler()
    pune_flood_db_search = ElasticsearchDataHandler(config=config, elasticsearch_log_handler=elasticsearch_log_handler)
    # Remove 'latestSearch' from search_types if it exists
    if 'latestSearch' in search_types:
        search_types.remove('latestSearch')
        logging.info("**Removed LatestSearch!!**")

    if len(search_types) == 1:
        search_type = search_types[0]
        if search_type == 'temporalSearch':
            temporal_query = build_temporal_query(json_object.get('temporal-query'))
            pune_flood_db_search.search_pune_flood_data(json_object, temporal_query, rout_key, corr_id, method)
        elif search_type == 'attributeSearch':
            attribute_query = build_attribute_query(json_object.get('attr-query'))
            pune_flood_db_search.search_pune_flood_data(json_object, attribute_query, rout_key, corr_id, method)
        elif search_type == 'geoSearch':
            logging.info(json_object.get('geo-query'))
            geo_query = build_geo_query(json_object.get('geo-query'))
            pune_flood_db_search.search_pune_flood_data(json_object, geo_query, rout_key, corr_id, method)
        else:
            logging.error("Unsupported searchType: %s", search_type)
            return
    else:
        logging.info("Inside complex query...")
        temporal_query = build_temporal_query(json_object.get('temporal-query'))
        attribute_query = build_attribute_query(json_object.get('attr-query'))
        geo_query = build_geo_query(json_object.get('geo-query'))

        combined_query = build_combined_query(temporal_query, attribute_query, geo_query)

        pune_flood_db_search.search_pune_flood_data(json_object, combined_query, rout_key, corr_id, method)

#temporal query
def build_temporal_query(temporal_query_params):
    if not temporal_query_params:
        return None
    timerel = temporal_query_params.get('timerel')

    if timerel == 'during':
        return build_during_query(temporal_query_params)
    elif timerel == 'before':
        return build_before_query(temporal_query_params)
    elif timerel == 'after':
        return build_after_query(temporal_query_params)
    else:
        logging.error("Unsupported timerel value: %s", timerel)
        return None

def build_during_query(temporal_query_params):
    time = temporal_query_params.get('time')
    endtime = temporal_query_params.get('endtime')
    return Q('range', observationDateTime={'gte': time, 'lte': endtime})

def build_before_query(temporal_query_params):
    time = temporal_query_params.get('time')
    return Q('range', observationDateTime={'lt': time})

def build_after_query(temporal_query_params):
    time = temporal_query_params.get('time')
    return Q('range', observationDateTime={'gt': time})

#Attribute Query
def build_single_attribute_query(condition):
    logging.info("am here in single attr query method..")
    parts = condition.split('==')
    if len(parts) == 2:
        # Equality condition
        field = parts[0]
        value = parts[1]
        return Q('term', **{field: value})
    else:
        # Inequality condition
        parts = condition.split('>')
        if len(parts) == 2:
            operator = 'gt'
        else:
            parts = condition.split('<')
            if len(parts) == 2:
                operator = 'lt'
            else:
                parts = condition.split('>=')
                if len(parts) == 2:
                    operator = 'gte'
                else:
                    parts = condition.split('<=')
                    if len(parts) == 2:
                        operator = 'lte'
                    else:
                        logging.error("Unsupported attribute query condition: %s", json.dumps(condition))
                        return None
        field = parts[0]
        value = parts[1]
        return Q('range', **{field: {operator: value}})

def build_attribute_query(attribute_query_params):
    if not attribute_query_params:
        logging.info("No query params..")
        return None

    attr_query = attribute_query_params

    if ';' in attr_query:
        logging.info("am here in multiple attr method..")
        # Multiple conditions in attribute query separated by ';'
        conditions = attr_query.split(';')
        must_queries = []
        for condition in conditions:
            must_queries.append(build_single_attribute_query(condition))
        return Q('bool', must=must_queries)
    else:
        # Single condition in attribute query
        return build_single_attribute_query(attr_query)

#geo query
def build_geo_query(geo_query_params):
    if not geo_query_params:
        return None
    # Construct and return geo query
    geo_type = geo_query_params.get('geometry')
    if geo_type == 'Polygon':
        return build_geo_polygon_query(geo_query_params)
    elif geo_type == 'bbox':
        return build_geo_bbox_query(geo_query_params)
    elif geo_type == 'linestring':
        return build_geo_linestring_query(geo_query_params)
    else:
        return build_geo_circle_query(geo_query_params)

def build_geo_circle_query(geo_query_params):
    lat = geo_query_params['lat']
    lon = geo_query_params['lon']
    radius = geo_query_params['radius']
    return Q('geo_distance', distance = radius, location={"lat": lat, "lon": lon})

def build_geo_polygon_query(geo_query_params):
    # Parse coordinates from string to list of floats
    coordinates_str = geo_query_params['coordinates']
    coordinates_list = json.loads(coordinates_str)
    coordinates_float = [[float(coord[0]), float(coord[1])] for coord in coordinates_list[0]]
    return Q('geo_shape', location={'shape': {'type': 'Polygon', 'coordinates': [coordinates_float]}, 'relation': 'within'})


def build_geo_bbox_query(geo_query_params):
    # Parse coordinates from string to list of floats
    coordinates_str = geo_query_params['coordinates']
    coordinates_list = json.loads(coordinates_str)
    # Ensure correct ordering of latitude values
    latitudes = [float(coord[1]) for coord in coordinates_list]
    latitudes.sort()
    return Q('geo_bounding_box', location={'top_left': {'lat': latitudes[1], 'lon': float(coordinates_list[0][0])},
                                            'bottom_right': {'lat': latitudes[0], 'lon': float(coordinates_list[1][0])}})

def build_geo_linestring_query(geo_query_params):
    # Parse coordinates from string to list of floats
    coordinates_str = geo_query_params['coordinates']
    coordinates_list = json.loads(coordinates_str)
    # Convert coordinates to list of floats
    coordinates_float = [[float(coord[0]), float(coord[1])] for coord in coordinates_list]
    # Construct and return geo linestring query
    return Q('geo_shape', location={'shape': {'type': 'linestring', 'coordinates': coordinates_float}, 'relation': 'intersects'})

#complex query
def build_combined_query(temporal_query, attribute_query, geo_query):
    # Combine temporal, attribute, and geo queries into a single query
    combined_query = Q('bool')

    if temporal_query:
        logging.info("in complex temp...")
        combined_query &= temporal_query

    if attribute_query:
        logging.info("in complex attribute_query...")
        combined_query &= attribute_query

    if geo_query:
        logging.info("in complex geo_query...")
        combined_query &= geo_query
    logging.info(combined_query)
    return combined_query

if __name__ == '__main__':
    config = ConfigParser(interpolation=None)
    config.read('./secrets/config.ini')

    username = config["server_setup"]["username"]
    password = config["server_setup"]["password"]
    host = config["server_setup"]["host"]
    port = config["server_setup"]["port"]
    vhost = config["server_setup"]["vhost"]
    queue = config["collection_queue"]["queue"]

    server_configure = RabbitMqServerConfigure(username, password, host, port, vhost, queue)
    server = RabbitmqServer(server=server_configure)
    server.start_server(on_request=process_request)