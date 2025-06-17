// Warning: This script will delete all existing nodes and relationships in the database.

MATCH (n)
DETACH DELETE n;

DROP CONSTRAINT activity_name_unique IF EXISTS;
CREATE CONSTRAINT activity_name_unique FOR (a:Activity) REQUIRE a.name IS UNIQUE;

CREATE (paris:Place {name: 'Paris'})

CREATE (hiking:Activity {name: 'Hiking'})
CREATE (cycling:Activity {name: 'Cycling'})
CREATE (classicalMusic:Activity {name: 'Classical Music'})
CREATE(food:Activity {name: 'Food'})
CREATE (wine:Activity {name: 'Wine'})

CREATE (rod:Person {name: 'Rod'})
CREATE (rod)-[:ENJOYS]->(hiking)
CREATE (rod)-[:ENJOYS]->(cycling)
CREATE (rod)-[:ENJOYS]->(classicalMusic)
CREATE (rod) -[:ENJOYS]->(food)
CREATE (rod)-[:ENJOYS]->(wine)
CREATE (rod)-[:VISITED]->(rvp: Visit {date: '2023-10-01', rating: 9, comment: 'Always a beautiful city!'})-[:TO_PLACE]->(paris)

CREATE (lynda:Person {name: 'Lynda'})
CREATE (lynda)-[:ENJOYS]->(hiking)
CREATE (lynda)-[:ENJOYS]->(cycling)
CREATE (lynda)-[:ENJOYS]->(classicalMusic)
CREATE (lynda)-[:ENJOYS]->(food)
CREATE (lynda)-[:ENJOYS]->(wine)