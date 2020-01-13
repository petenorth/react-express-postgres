I focused on wanting to demonstrate a three tier app that used React as the frontend (a single page app) accessing a REST API as the middle tier with the database tier as PostgreSQL.

To this end I found https://levelup.gitconnected.com/build-a-multi-container-application-with-docker-compose-460f6199ef3c which uses docker-compose to spin up the different tiers as docker containers .

I forked the corresponding repo so I could show docker images with optimized production builds.


So firstly with dev servers and NOT using docker-compose as the example blog post only works because the docker-compose.yml includes a build which delays the starting of the API server, the API server must start after successful Postgres server startup hence we fall back to the docker run command.

    git clone https://github.com/petenorth/react-express-postgres
    cd react-express-postgres
    cd client
    docker build -t react-web-app:latest . 
    docker build -f Dockerfile.dev -t react-web-app-dev:latest . 
    cd ../server/
    docker build -t to-do-api:latest . 
    docker build -f Dockerfile.dev -t to-do-api-dev:latest . 
    docker run --name=postgres -p 5432:5432 -d  postgres:latest
    docker run --link postgres:postgres -e PGUSER=postgres -e PGHOST=postgres -e PGDATABASE=postgres -e PGPASSWORD= -e PGPORT=5432 -p 3001:3001 --name to-do-api -d to-do-api-dev:latest

    docker run --name web-app -p 3000:3000 -e REACT_APP_TO_DO_ITEMS_API=http://localhost:3001/v1 -d react-web-app-dev:latest

To test 

    curl --header "Content-Type: application/json"   --request POST   --data '{"item_name": "Speed Speed Speed"}' http://localhost:3001/v1/items

And then access the web app via http://localhost:3000

this starts two express servers (frontend and backend) but in development mode meaning that the delivery of assets to the client is not optimized and the serving of the REST API responses is not optimized. The React and REST API applications should be optimized for production use and also the React application is essentially serving static content (html, js and images) . There is no need for a Javascript language runtime on the frontend server. To that end the javascript content is copied into a nginx container which is much more efficient.

    docker stop web-app
    docker rm web-app
    docker stop to-do-api
    docker rm to-do-api
    docker run --link postgres:postgres -e PGUSER=postgres -e PGHOST=postgres -e PGDATABASE=postgres -e PGPASSWORD= -e PGPORT=5432 -e PORT=8080 -p 8080:8080 --name to-do-api -d to-do-api:latest 
    docker run --name web-app -p 8081:8080 --link to-do-api:to-do-api -d react-web-app:latest
  
And then access the web app via http://localhost:8081 . The interesting thing with the production version of the web-app image is that since it is static content there is no way to make the to-do-api REST service endpoint configurable. Instead we make the nginx server a reverse proxy for this service (see the `default.conf` file that is copied into the image). By default the variable `REACT_APP_TO_DO_ITEMS_API` in `.env` file for the node app points to a relative URL for the to-do-api end point. This is then directed via the reverse proxy to `http://to-do-api:8080` which because of the `--link to-do-api` of the docker run command will resolve to the to-do-api docker container.

As a demonstration of the benefits of three tier archtiectures (or architectural layers in general) there is an alternative API layer implementaton in Java using Spring Boot and Tomcat.

    cd ../server-spring-boot
    mvn clean install
    docker build -t to-do-api-springboot:latest .
    docker stop to-do-api
    docker rm to-do-api
    docker run --name to-do-api --link postgres:postgres -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/postgres -d to-do-api-springboot:latest

Running in Kubernetes

Kubernetes is a container orchestration technology available in most public clouds and can be used to run a private cloud. The easiest way to run Kubernetes for local testing is via minikube. 
The Docker daemon in minikube by default cannot pull locally built images. Therefore if you are going onto run the examples in Kubernetes do the following

    minikube start
    eval $(minikube docker-env)

this points your current shell to the Docker daemon running within minikube. Then we rebuild the images

    cd ../client
    docker build -t react-web-app:latest . 
    cd ../server-spring-boot
    mvn clean install
    docker build -t to-do-api-springboot:latest .

The ingress minikube addon must be enabled

    minikube addons enable ingress

wait until it appears in

    kubectl get pod -n kube-system -w

Then to create the deployment, service and ingress  resources 

    cd ..
    kubectl create -f kubernetes/resources.yml
    
Check the three pods are running

    kubectl get pod -w
    
Check the ingress points have been assigned an IP address

    kubectl get ing -w

Then to confirm the end points are accessible

    curl http://$(minikube ip)/v1/items
  
And that the static html content can be served

    curl http://$(minikube ip)

Then to see the app working open in a browser http://$(minikube ip) .

The reverse proxy of nginx now resolves http://to-do-api:8080 since to-do-api is known by the DNS server of the cluster (this might need to be explicitely enabled see https://kubernetes.io/docs/concepts/services-networking/connect-applications-service/#dns ) as pointing to the clusterIP address of the to-do-api service which load balances traffic to any of the pods associated with the service (in our case the deployment resource definitions specify only one replica).

Note that the deployment resources in `kubernetes/resources.yml` include liveness and readiness checks so that the pods are killed and restarted if the configured GET requests fail. For the web-app nginx container it relies on a status page configured via `client/nginx/status.conf` . For the Spring Boot to-do-api container it relies on the Spring Boot health endpoint made availble via the Spring Boot Actuator dependency in the pom.xml file. Both the nginx and Spring Boot endpoints have been configured to be served via a a private 8081 port which is not exposed as a kubernetes service or ingress.



