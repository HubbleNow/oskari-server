# Peltodata module

* Contains classes and resources for peltodata.fi adaptation

## Development settings (IntelliJ IDEA)

* Download and unzip bundle
* Delete oskari-map.war and oskari-map.xml from jetty-distribution/webapps
* Use builtin jetty local server
** Choose webapp-app exploded in Deployments
** set custom context-root to `/`
* Edit jetty-distribution/resources/oskari-ext.properties add following module
````
db.additional.modules=myplaces, userlayer, example, peltodata
````
* Start debugging/running jetty

## Production settings
