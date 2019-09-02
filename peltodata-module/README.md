# Peltodata module

* Contains classes and resources for peltodata.fi adaptation

## Development settings (IntelliJ IDEA)

* Download and unzip bundle
* Delete oskari-map.war and oskari-map.xml from jetty-distribution/webapps
* Use builtin jetty local server
  * Choose webapp-app exploded in Deployments
  * set custom context-root to `/`
* Edit jetty-distribution/resources/oskari-ext.properties add following module
````
db.additional.modules=myplaces, userlayer, example, peltodata
````
* Also oskari-ext.properties, configure peltodata UI bundle so that logged in user can see it
  * Add peltodata to dynamic bundles as below
  * Add configuration row for that peltodatabundle which allows Admin and User see the bundle
````
# Row below exists, add peltodata to it
actionhandler.GetAppSetup.dynamic.bundles = admin-layerselector, admin-layerrights, admin-users, admin, peltodata
# Row below does not exist, copy paste the row into the file
actionhandler.GetAppSetup.dynamic.bundle.peltodata.roles = Admin, User
```` 

* Start debugging/running jetty

### Geoserver
* Peltodata module expects certain geoserver configurations
  * url=localhost:8080/geoserver
  * user=admin
  * password=geoserver
* If different, configure to oskari-ext.properties or oskari.properties in same way that for production 

## Production settings

### Oskari
* Configure peltodata UI bundle so that logged in user can see it
  * oskari-ext.properties
  * Add peltodata to dynamic bundles as below
  * Add configuration row for that peltodatabundle which allows Admin and User see the bundle
````
# Row below exists, add peltodata to it
actionhandler.GetAppSetup.dynamic.bundles = admin-layerselector, admin-layerrights, admin-users, admin, peltodata
# Row below does not exist, copy paste the row into the file
actionhandler.GetAppSetup.dynamic.bundle.peltodata.roles = Admin, User
```` 

### Geoserver
* Add user to geoserver for Oskari REST api access
* Configure geoserver addresses to oskari-ext.properties (geoserver.url, geoserver.user, geoserver.password)
  * Example:
```` 
geoserver.url=http://localhost:8080/geoserver
geoserver.user=peltodata
geoserver.password=peltodata1231DD
````
