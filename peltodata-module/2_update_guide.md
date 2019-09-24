# Peltodata application update guide

### 1. Server
1. `git clone http://bitbucket..`
   1. first time `git clone`, later just `git pull` in the folder
2. `git checkout peltodata`
   1. correct branch      
3. `mvn clean install package -Dmaven.test.skip=true`
   1. Compiles and packages the new backend war file
4. `cp webapp-map/target/oskari-map.war ~/oskari-server/webapps`
   1. copy the compiled application war file into the jetty server  
5. `cp webapp-transport/target/transport.war ~/oskari-server/webapps`
   1. copy the compiled transport war file into the jetty server  

### 2. Frontend

1. `git clone http://github..`
   1. first time use `git clone` later just `git pull`
2. `git checkout peltodata`
   1. correct branch
3. `npm install && npm run build`
   1. generates the frontend as static javascript files under `dist` folder
4. `cp -r dist ~/oskari-server/peltodata`
   1. copy the dist folder into jetty folder
