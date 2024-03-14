'''
Surat-ITMS Adapter Code

This code serves as an adapter for querying Surat-ITMS data from an Elasticsearch
database. It interacts with a RabbitMQ server to receive search requests and publish
the results, and utilizes an Elasticsearch client for data retrieval.

It supports various types of search and count queries, including spatial search,
spatial count, latest search, complex search, and complex count. These capabilities are indicated
by the "applicableFilters" parameter, which includes "ATTR" and "TEMPORAL".

Additionally, the adapter includes logic for temporal and attribute search and count queries,
making it versatile and suitable for a wide range of querying scenarios related to surat itms data.

This adapter code can be utilized whenever there is a need to query surat itms data stored in an Elasticsearch database.
'''


import json
import re
import logging
import random
from dateutil import parser as date_parser
from elasticsearch import Elasticsearch
from elasticsearch_dsl import Search, Q
import pika
from configparser import ConfigParser
from datetime import datetime, timedelta

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

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

class SearchDatabase:
    def __init__(self, config):
        self.config = config

    def search_surat_itms_data(self, json_object, query, rout_key, corr_id, method):
        elk_config = self.config['elasticsearch']
        client = Elasticsearch(
                    [f"http://{elk_config['databaseURI']}:{elk_config['databasePort']}"],
                    basic_auth=(elk_config['databaseUser'], elk_config['databasePassword'])
                )
        index_name = elk_config['index_name']
        limit = None
        offset = None
        limit = json_object.get('limit')  # Get limit value from JSON request
        offset = json_object.get('offset')  # Get offset value from JSON request
        #integrating the adapter code with async search and status APIs
        apiEndpoint = json_object.get('api')
        if apiEndpoint == '/ngsi-ld/v1/async/search':
            response_payload = {
                              "searchId": "48c279f3-ed90-4c05-bbfa-ffa91dd3d8a2",
                              "statusCode": 201
                              }
        elif apiEndpoint == '/ngsi-ld/v1/async/status':
            searchId = json_object.get("searchId")
            # Randomly choose between  (complete) and  (in-progress)
            if random.choice([True, False]):
                                response_payload = {
                                    "statusCode": 200,
                                    "results": [
                                    {
                                    "status": "COMPLETE",
                                    "progress": 100,
                                    "file-download-url": "https://example.com/filename",
                                    "searchId": searchId
                                    }
                                    ]
                                }
            else:
                response_payload = {
                                    "statusCode": 200,
                                    "results": [
                                    {
                                           "status": "IN_PROGRESS",
                                           "progress": 70,
                                           "searchId": searchId
                                            }
                                    ]
                 }
        elif query:
            if "options" in json_object and json_object["options"] == "count":
                # Convert query to a dictionary
                query_dict = query.to_dict()
                # Perform count query using the "_count" endpoint
                # Define the Elasticsearch query
                query =   {
                             "query": {
                                 "bool": {
                                      "must": [
                                         query_dict
                                        ]
                                            }
                                        }
                                    }
                count_response = client.count(index=index_name, body=query)
                logging.info(count_response)
                if "error" in count_response:
                    status_code = response.get("status")
                else:
                    count = count_response['count']
                    if count == 0:
                        status_code = 204
                    else:
                        status_code = 200
                if(limit is not None and offset is not None):
                    response_payload = {
                        "totalHits": count,
                        "statusCode": status_code, # Placeholder for status code
                        "limit": int(limit),
                        "offset": int(offset)
                    }
                else:
                    response_payload = {
                        "totalHits": count,
                        "statusCode": status_code
                        }
            else:
                search = Search(index=index_name).using(client)
                logging.info("entire query..")
                logging.info(query)
                search = search.query(query)
                # Adjusting the handling of limit and offset
                if limit is not None and offset is not None:
                    int_limit = int(limit)  # Convert limit to integer
                    int_offset = int(offset)  # Convert offset to integer
                    search = search[int_offset:int_offset+int_limit]
                elif limit is not None:
                    # Only limit is present
                    int_limit = int(limit)
                    search = search[:int_limit]
                elif offset is not None:
                    # Only offset is present
                    int_offset = int(offset)
                    search = search[int_offset:]
                else:
                    # Neither limit nor offset is present
                    search = search[0:10000]  # Set a default limit or adjust as needed
                response = search.execute()
                # Extract relevant data from the response object
                # Check if the response contains an "error" key
                if "error" in response:
                    status_code = response.get("status")
                else:
                    hits = [hit.to_dict() for hit in response.hits]
                    status_code = 200
                    logging.info("status")
                    logging.info(status_code)
                    # Extract totalHits from the response
                    total_hits = response.hits.total.value if hasattr(response.hits.total, 'value') else 0
                    if total_hits == 0:
                        status_code = 204
                # Serialize the extracted data to JSON
                # Adjusting the response payload creation
                if limit is not None and offset is not None:
                    response_payload = {
                          "results": hits,
                          "statusCode": status_code,  # Placeholder for status code
                          "totalHits": total_hits,
                          "limit": int_limit,
                          "offset": int_offset
                        }
                elif limit is not None:
                    response_payload = {
                        "results": hits,
                        "statusCode": status_code,
                        "totalHits": total_hits,
                        "limit": int_limit
                    }
                elif offset is not None:
                    logging.info("Inside offset")
                    response_payload = {
                        "results": hits,
                        "statusCode": status_code,
                        "totalHits": total_hits,
                        "offset": int_offset
                    }
                else:
                    response_payload = {
                        "results": hits,
                        "statusCode": status_code,
                        "totalHits": total_hits
                    }
        else:
            logging.info("Empty query")

        if response_payload:
            #logging.info("Adapter response: ")
            #logging.info(response_payload)
            server.publish(response_payload, rout_key, corr_id, method)

        logging.info("Query Completed for Surat-ITMS data")

def process_request(ch, method, properties, body):
    logging.info("Request JSON received")
    logging.info("Received request with body: %s", body)
    json_object = json.loads(body)
    rout_key = properties.reply_to
    corr_id = properties.correlation_id
    surat_itms_db_search = SearchDatabase(config=config)
    #Async status
    apiEndpoint = json_object.get('api')
    #logging.info("api.."+apiEndpoint)
    if apiEndpoint == '/ngsi-ld/v1/async/status':
        searchId = json_object.get("searchId")
        surat_itms_db_search.search_surat_itms_data(json_object, None, rout_key, corr_id, method)

    else:
        search_types = json_object['searchType'].split('_')
        # Remove 'latestSearch' from search_types if it exists
        if 'latestSearch' in search_types:
            search_types.remove('latestSearch')
            #logging.info("**Removed LatestSearch!!**")

        if len(search_types) == 1:
            search_type = search_types[0]
            if search_type == 'temporalSearch':
                temporal_query = build_temporal_query(json_object.get('temporal-query'))
                id = json_object.get('id')
                combined_query = build_combined_query(None, None, None, id)
                surat_itms_db_search.search_surat_itms_data(json_object, temporal_query, rout_key, corr_id, method)
            elif search_type == 'attributeSearch':
                attribute_query = build_attribute_query(json_object.get('attr-query'))
                id = json_object.get('id')

                # Read time reference from config
                start_time = config['time_reference']['start_time']
                end_time = config['time_reference']['end_time']

                # Add time range query
                time_range_query = build_temporal_query({"time": start_time, "endtime": end_time, "timerel": "during"})
                combined_query = build_combined_query(time_range_query, attribute_query, None, id)
                surat_itms_db_search.search_surat_itms_data(json_object, combined_query, rout_key, corr_id, method)
            elif search_type == 'geoSearch':
                logging.info(json_object.get('geo-query'))
                geo_query = build_geo_query(json_object)
                logging.info("geo query is..")
                logging.info(geo_query)
                id = json_object.get('id')

                # Read time reference from config
                start_time = config['time_reference']['start_time']
                end_time = config['time_reference']['end_time']

                # Add time range query
                time_range_query = build_temporal_query({"time": start_time, "endtime": end_time, "timerel": "during"})
                combined_query = build_combined_query(time_range_query, None, geo_query, id)
                surat_itms_db_search.search_surat_itms_data(json_object, combined_query, rout_key, corr_id, method)
            else:
                logging.error("Unsupported searchType: %s", search_type)
                return
        else:
            temporal_query = build_temporal_query(json_object.get('temporal-query'))
            attribute_query = build_attribute_query(json_object.get('attr-query'))
            geo_query = build_geo_query(json_object)
            id = json_object.get('id')
            combined_query = build_combined_query(temporal_query, attribute_query, geo_query, id)
            surat_itms_db_search.search_surat_itms_data(json_object, combined_query, rout_key, corr_id, method)

#temporal query
def build_temporal_query(temporal_query_params):
    if not temporal_query_params:
        return None
    timerel = temporal_query_params.get('timerel')

    if timerel == 'during'or timerel == 'between':
        return build_during_query(temporal_query_params)
    elif timerel == 'before':
        return build_before_query(temporal_query_params)
    elif timerel == 'after':
        return build_after_query(temporal_query_params)
    else:
        logging.error("Unsupported timerel value: %s", timerel)
        return None

# Inside the build_during_query function
def build_during_query(temporal_query_params):
    if not temporal_query_params:
        return None
    time = temporal_query_params.get('time')
    endtime = temporal_query_params.get('endtime')
    try:
        # Parse the time data with timezone info
        time = date_parser.parse(time)
        endtime = date_parser.parse(endtime)
    except ValueError:
        logging.error("Invalid time format: %s", time)
        return None
    return Q('range', observationDateTime={'gte': time.isoformat(), 'lte': endtime.isoformat()})

def build_before_query(temporal_query_params):
    time = temporal_query_params.get('time')
    # Convert the time string to a datetime object
    time = date_parser.parse(time)
    # Subtract 10 days from the specified time
    new_time = time - timedelta(days=10)
    return Q('range', observationDateTime={'gte': new_time.isoformat(), 'lte': time.isoformat()})

def build_after_query(temporal_query_params):
    time = temporal_query_params.get('time')
    # Convert the time string to a datetime object
    time = date_parser.parse(time)
    # Add 10 days to the specified time
    new_time = time + timedelta(days=10)
    return Q('range', observationDateTime={'gte': time.isoformat(), 'lte': new_time.isoformat()})

#Attribute Query
def build_single_attribute_query(condition):
    parts = re.split('(==|>=|<=|>|<)', condition)
    if len(parts) == 3:
        # Equality or Inequality condition
        field = parts[0]
        operator = parts[1]
        value = parts[2]
        if operator == '==':
            return Q('term', **{field: value})
        elif operator == '>=':
            return Q('range', **{field: {'gte': value}})
        elif operator == '<=':
            return Q('range', **{field: {'lte': value}})
        elif operator == '>':
            return Q('range', **{field: {'gt': value}})
        elif operator == '<':
            return Q('range', **{field: {'lt': value}})
        else:
            logging.error("Unsupported operator in attribute query: %s", operator)
            return None
    else:
        logging.error("Unsupported attribute query condition: %s", condition)
        return None

def build_attribute_query(attribute_query_params):
    if not attribute_query_params:
        logging.info("No query params..")
        return None

    attr_query = attribute_query_params

    if ';' in attr_query:
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
def build_geo_query(request_json):
    geo_query_params = request_json.get('geo-query')
    if not geo_query_params:
        return None
    # Construct and return geo query
    geo_type = geo_query_params.get('geometry')
    if geo_type == 'Polygon' or geo_type == 'polygon':
        return build_geo_polygon_query(request_json)
    elif geo_type == 'bbox':
        return build_geo_bbox_query(request_json)
    elif geo_type == 'linestring' or geo_type == 'Linestring':
        return build_geo_linestring_query(request_json)
    else:
        return build_geo_circle_query(geo_query_params)


def build_geo_circle_query(geo_query_params):
    lat = geo_query_params['lat']
    lon = geo_query_params['lon']
    radius = geo_query_params['radius']
    return Q('geo_distance', distance = radius, location={"lat": lat, "lon": lon})

def build_geo_polygon_query(request_json):
    geo_query_params = request_json.get('geo-query')
    # Parse coordinates from string to list of floats
    coordinates_str = geo_query_params['coordinates']
    coordinates_list = json.loads(coordinates_str)
    coordinates_float = [[float(coord[0]), float(coord[1])] for coord in coordinates_list[0]]
    return Q('geo_shape', location={'shape': {'type': 'Polygon', 'coordinates': [coordinates_float]}, 'relation': geo_query_params['georel']})


def build_geo_bbox_query(request_json):
    geo_query_params = request_json.get('geo-query')
    # Parse coordinates from string to list of floats
    coordinates_str = geo_query_params['coordinates']
    coordinates_list = json.loads(coordinates_str)
    # Ensure correct ordering of latitude values
    latitudes = [float(coord[1]) for coord in coordinates_list]
    latitudes.sort()
    return Q('geo_bounding_box', location={'top_left': {'lat': latitudes[1], 'lon': float(coordinates_list[0][0])},
                                            'bottom_right': {'lat': latitudes[0], 'lon': float(coordinates_list[1][0])}})

def build_geo_linestring_query(request_json):
    geo_query_params = request_json.get('geo-query')
    # Parse coordinates from string to list of floats
    coordinates_str = geo_query_params['coordinates']
    coordinates_list = json.loads(coordinates_str)
    # Convert coordinates to list of floats
    coordinates_float = [[float(coord[0]), float(coord[1])] for coord in coordinates_list]
    # Construct and return geo linestring query
    return Q('geo_shape', location={'shape': {'type': 'linestring', 'coordinates': coordinates_float}, 'relation': geo_query_params['georel']})

#complex query
def build_combined_query(temporal_query, attribute_query, geo_query, id):
    # Combine temporal, attribute, and geo queries into a single query
    combined_query = Q('bool')

    if temporal_query:
        #logging.info("in complex temp...")
        combined_query &= temporal_query

    if attribute_query:
        #logging.info("in complex attribute_query...")
        combined_query &= attribute_query

    if geo_query:
        #logging.info("in complex geo_query...")
        combined_query &= geo_query
    # Include the id in the query
    combined_query &= Q('terms', id=id)
    logging.info(combined_query)
    return combined_query

if __name__ == '__main__':
    config = ConfigParser(interpolation=None)
    config.read("./example-secrets/secrets/config.ini")

    username = config["server_setup"]["username"]
    password = config["server_setup"]["password"]
    host = config["server_setup"]["host"]
    port = config["server_setup"]["port"]
    vhost = config["server_setup"]["vhost"]
    queue = config["collection_queue"]["queue"]

    server_configure = RabbitMqServerConfigure(username, password, host, port, vhost, queue)
    server = RabbitmqServer(server=server_configure)
    server.start_server(on_request=process_request)