PROJECT_NETWORK='finalproject-network'
SERVER_IMAGE='finalproj-server-image'
SERVER_CONTAINER='finalproj-server'
CLIENT_IMAGE='finalproj-client-image'
#CLIENT_CONTAINER='p2-client'
#PORT_NUMBER='5555'
SERVER_ID=$(docker ps --filter name=$SERVER_CONTAINER --format "{{.ID}}")

echo $SERVER_ID
# run client docker container with cmd args
docker run -it --rm  --network $PROJECT_NETWORK $CLIENT_IMAGE java client.Client $SERVER_ID $1 $2
