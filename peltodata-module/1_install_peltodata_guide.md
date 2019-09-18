# Installing peltodata changes over existing oskari installation

## Required configuration changes

### 1. oskari-server/resources/oskari-ext.properties

- Configure the "version" path for peltodata frontend files
  - this has to correspond with /peltodata/dist/<version>/ which is also specified in package.json (now devapp)
  - Modify row
    - `oskari.client.version=dist/devapp`
- Activate the bundle
  - Done so that `my fields` is activated and shown in the Ui for logged in user with role `User`
  - Add peltodata to row below
    - `actionhandler.GetAppSetup.dynamic.bundles = admin-layerselector, admin-layerrights, admin-users, admin, peltodata`
  - Add row
    - `actionhandler.GetAppSetup.dynamic.bundle.peltodata.roles = User`

- configure correct geoserver url 
  - This url has to be accessible from outside because clients will access the layers from there
    - `geoserver.url=https://xxx/geoserver`

- Activate peltodata backend module
  - If not modified, the database migrations wont be run and there will be errors
  - also remove `myapp` from this list
  - Modify row
    - `db.additional.modules=myplaces, userlayer, peltodata`

- Configure path for uploaded files
  - Configures where NEW files are stored, existing files do not care about this setting (path saved in db)
  - Optional
    - if not configured, it is under `oskari-server/geoserver_data`
  - `peltodata.upload.root.dir`
  - Under this folder, a folder structure will be made, for example:
    - ```
        /farms
        /farms/{farmid}/
        /farms/{farmid}/{year}/
        /farms/{farmid}/{year}/<filetimestamp>_CROP_ESTIMATION_ORIGINAL.tiff              
      ```

- Python script configuration
  - path to python script
  - if value is `dev`, for dev purposes and better developer experience, only copy will be made (input -> output).
    - `peltodata.scripts.crop_estimation=/home/oskari/copy-file.py`
    - `peltodata.scripts.yield=dev`
  - scripts will be called as `python <SCRIPT> 123123123123_CROP_ESTIMATION.json`
    - json file format can be found in `example_json.json` 

### 2. oskari-server/webapps/oskari-front.properties
- Change the folder where frontend package can be found.
  - change
    - `<Set name="resourceBase"><SystemProperty name="jetty.base" default="."/>/peltodata</Set>`

### 3. nginx.conf
- allow big file uploads
- can be put in `http`, `server` or `location` block. Best probably server 
  - `client_max_body_size 2G;`

### 4. jetty/start.d/console-capture.ini
- Enable logging for jetty. after this, logfiles are in logs folder 
```
# ---------------------------------------
# Module: console-capture
# Redirects JVMs console stderr and stdout to a log file,
# including output from Jetty's default StdErrLog logging.
# ---------------------------------------
--module=console-capture

## Logging directory (relative to $jetty.base)
# jetty.console-capture.dir=logs

## Whether to append to existing file
# jetty.console-capture.append=true

## How many days to retain old log files
jetty.console-capture.retainDays=10

## Timezone of the log timestamps
# jetty.console-capture.timezone=GMT
```
