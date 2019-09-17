# Peltodata application update guide

### 1. Server
1. `git clone http://github..`
   1. first time `git clone`, later just `git pull` in the folder
2. `mvn clean install package -Dmaven.test.skip=true`
   1. Compiles and packages the new backend war file
3. `cp webapp-map/target/oskari-map.war ~/jetty-dist.../webapps`
   1. copy the compiled war file into the jetty server  

### 2. Frontend

1. `git clone http://github..`
   1. first time use `git clone` later just `git pull`
2. `npm run build`
   1. generates the frontend as static javascript files under `dist` folder
3. `cp -r dist ~/jett-dist.../peltodata/dist`
   1. copy the dist folder into jetty folder
