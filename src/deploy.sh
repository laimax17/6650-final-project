PROJECT_NETWORK='finalproject-network'
SERVER_IMAGE='finalproj-server-image'
SERVER_CONTAINER='finalproj-server'
CLIENT_IMAGE='finalproj-client-image'
#CLIENT_CONTAINER='p2-client'
#PORT_NUMBER='5555'

# clean up existing resources, if any
echo "----------Cleaning up existing resources----------"
docker container stop $SERVER_CONTAINER 2> /dev/null && docker container rm $SERVER_CONTAINER 2> /dev/null
docker container stop $CLIENT_CONTAINER 2> /dev/null && docker container rm $CLIENT_CONTAINER 2> /dev/null
docker network rm $PROJECT_NETWORK 2> /dev/null

# only cleanup
if [ "$1" == "cleanup-only" ]
then
  exit
fi

# create a custom virtual network
echo "----------creating a virtual network----------"
docker network create $PROJECT_NETWORK

# build the images from Dockerfile
echo "----------Building images----------"
docker build -t $CLIENT_IMAGE --target client-build .
docker build -t $SERVER_IMAGE --target server-build .

# run the image and open the required ports
echo "----------Running sever app----------"
docker run -d -p $1 --name $SERVER_CONTAINER --network $PROJECT_NETWORK $SERVER_IMAGE java coordinator.Coordinator $1

echo "----------watching logs from server----------"
docker logs $SERVER_CONTAINER -f