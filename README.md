localserve
==========

**WARNING** THE SOURCE COMES WITH NO WARRANTY WHATSOEVER AND IS DEFINITELY NOT INTENDED FOR USE IN A PRODUCTION ENVIRONMENT.
THIS IS A PROTOTYPING TOOL ONLY.

Localserve is a simple Java based web-server for use in development/testing/prototyping. Every now and again I develop 
the urge to knock a simple website up as a test/play area and got fed-up installing/configuring/removing various 
webservers when all I wanted to do was serve a few files. Using the local static files isn't really an option these days
since AJAX and the like isn't authorised to run in such an environment and in some cases I also want to pull in some
'dynamic' content from a database (SQLite being my preferred DB for such cases).

The localserve.jar file should contain everything needed to run the server by typing:

java -jar localserve.jar

This will spawn a web-server on port 8765 by default serving content from the current directory.

Additional parameters can be specified as below:

java -jar localserve.jar <port> <sql.json>
e.g. java -jar localserve.jar 8080 test.json

This allows another port than 8765 to be used and for a SQLite JSON configuration file to be specified.

The SQLite JSON file contains a pointer to a sqlite database file, indication of the driver to be used (bundled in the 
localserve.jar file), and an array of operations which each includes:

* path: URL path name for the SQL command
* statement: SQL statement to execute with bind variables include (e.g. ... where name = {NAME} ... will bind the HTTP 
"NAME" parameter to the placeholder in the SQL statement.
* methods: POST,GET - Indicates which methods the statement can be executed with.
* redirect: (optional) - Path of where the requestor should be sent once the current operation is completed. 
Useful for chaining requests and foran insert/update/delete SQL statement.

Example JSON file is shown below.

{
    "wsm":  {
        "connString": "jdbc:sqlite:/path/to/test.db",
        "driver": "org.sqlite.JDBC",
        "operations": [
            { "path": "listbob", "statement": "select name, id from bob", "methods": "GET,POST" },
            { "path": "querybob", "statement": "select id, name from bob where id={ID} order by name desc", "methods": "GET,POST" },
            { "path": "insertbob", "statement": "insert into bob (id, name) values ({ID}, {N})", "redirect": "/sql/querybob", "methods": "GET,POST" }
         ]
    }
}

