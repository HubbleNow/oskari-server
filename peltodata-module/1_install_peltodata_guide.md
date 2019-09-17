# Installing peltodata changes over existing oskari installation

## Required configuration changes

### 1. jetty-distribution-xxxx/resources/oskari-ext.properties

- Activate the bundle
  - Done so that `my fields` is activated and shown in the Ui for logged in user with role `User`
  - Add peltodata to row below
    - `actionhandler.GetAppSetup.dynamic.bundles = admin-layerselector, admin-layerrights, admin-users, admin, peltodata`
  - Add row
    - `actionhandler.GetAppSetup.dynamic.bundle.peltodata.roles = User`

- Activate peltodata backend module
  - If not modified, the database migrations wont be run and there will be errors
  - Modify row
    - `db.additional.modules=myplaces, userlayer, example, peltodata`

- Configure path for uploaded files
  - Configures where NEW files are stored, existing files do not care about this setting (path saved in db)
  - Optional
    - if not configured, it is under `jetty-dist.../geoserver_data`
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

### 2. jetty-distribution-xxxx/webapps/oskari-front.properties
- Change the folder where frontend package can be found.
  - change
    - `<Set name="resourceBase"><SystemProperty name="jetty.base" default="."/>/peltodata</Set>`

### 3. nginx.conf
- allow big file uploads
- can be put in `http`, `server` or `location` block. Best probably server 
  - `client_max_body_size 2G;`
