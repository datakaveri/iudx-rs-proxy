import elasticsearch
from elasticsearch import Elasticsearch
import elasticsearch_dsl
from elasticsearch_dsl import Search
import psycopg2 

# create an elasticsearch object.

es=Elasticsearch(["host"], port=0000,http_auth=("username","password"),use_ssl=False,verify_certs=False)


# getting doc from elastic search
res = es.search(index="label", doc_type="_doc", body = {
'size' : 10000,
'query': {
    'match_all' : {}
}
})

# connecting with database
conn = psycopg2.connect(host="", database="", user="", password="",port=00)
cur = conn.cursor()

# create table in database
create_stmt = ("CREATE TABLE table_name (columne_name1 VARCHAR(255), columne_name2 VARCHAR(255),columne_name3 VARCHAR(255),columne_name4 VARCHAR(255))")
cur.execute(create_stmt)

# insert data in database
insert_stmt = ("INSERT INTO table_name(column_name1,column_name2,column_name3,column_name4...)""VALUES (%s, %s, %s, %s....)")

data = [doc for doc in res['hits']['hits']]
for doc in data:
  data=(doc['_source']['route_id'],doc['_source']['observationDateTime'],doc['_source']['occupancyLevel'],doc['_source']['id'],doc['_source']['trip_id'],doc['_source']['passengerCount'],doc['_source']['vehicle_label'])
  
  cur.execute(insert_stmt, data)
  conn.commit()
  
print("Data inserted")

# Closing the connection
conn.close()  
