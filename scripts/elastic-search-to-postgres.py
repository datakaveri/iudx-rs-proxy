import elasticsearch
from elasticsearch import Elasticsearch
import elasticsearch_dsl
from elasticsearch_dsl import Search
import psycopg2
import json


#reading configuration file from system
with open("config-file-name.json") as file:
    config=json.load(file)


#init local variables
dataBaseHost = config["dataBaseHost"]
dataBasePort = config["dataBasePort"]
dataBaseUser = config["dataBaseUser"]
dataBasePassword = config["dataBasePassword"]
databaseIndex = config["index"]

postgersDatabaseName = config["postgersDatabaseName"]
postgersUser = config["postgersUser"]
postgersPassword = config["postgersPassword"]
postgersHost = config["postgersHost"]
postgersPort = config["postgersPort"]
postgreTableName= config["postgreTableName"]

# create an elasticsearch object.

es=Elasticsearch([dataBaseHost], port=dataBasePort,http_auth=(dataBaseUser,dataBasePassword),use_ssl=False,verify_certs=False)


# getting doc from elastic search
res = es.search(index=databaseIndex, doc_type="_doc", body = {
'size' : 10000,
'query': {
    'match_all' : {}
}
})

# connecting with postgers
conn = psycopg2.connect(host=postgersHost, database=postgersDatabaseName, user=postgersUser, password=postgersPassword,port=postgersPort)
cur = conn.cursor()

# create table in postgers
create_stmt = ("CREATE TABLE {} (columnName1 VARCHAR(255), columnName2 VARCHAR(255),columnName3 VARCHAR(255),columnName4 TIMESTAMP,columnName5 VARCHAR(255),columnName6 VARCHAR(255),columnName7 INTEGER)")
cur.execute(create_stmt.format(postgreTableName))

# insert data in postgers
temp_insert_stmt = ("INSERT INTO {} (columnName1,columnName2,columnName3,columnName4,columnName5,columnName6,columnName7)""VALUES (%s,%s,%s,%s,%s,%s,%s)")
insert_stmt= cur.mogrify(temp_insert_stmt.format(postgreTableName))
data = [doc for doc in res['hits']['hits']]

for doc in data:
  data=(doc['_source']['columnName1'],doc['_source']['columnName2'],doc['_source']['columnName3'],doc['_source']['columnName4'],doc['_source']['columnName5'],doc['_source']['columnName6'],doc['_source']['columnName7'])
  cur.execute(insert_stmt, data)
  conn.commit()
  
print("Data inserted")

# Closing the connection
conn.close()  

