#~/bin/bash
# usage: ./testingScript.sh <numberOfIterations>

function waitForRabbitUp {
    local rabbitGuiUrl=$1
    while ! curl -s -o /dev/null $rabbitGuiUrl
    do
        echo waiting for rabbit to show GUI on $rabbitGuiUrl
        sleep 2
    done
}

sudo docker-compose up -d rabbit1
waitForRabbitUp "http://localhost:15671/#/"
sudo docker-compose up -d rabbit2
waitForRabbitUp "http://localhost:15672/#/"

count=${1:-10}
delay=20
echo "Will do $count test iterations"

for i in $(seq $count); do
    echo "Iteration $i"

    sudo docker-compose stop rabbit2
    sleep $delay
    sudo docker-compose up -d
    waitForRabbitUp "http://localhost:15672/#/"

    sudo docker-compose stop rabbit1
    sleep $delay
    sudo docker-compose up -d
    waitForRabbitUp "http://localhost:15671/#/"
done

echo "Finishing"
