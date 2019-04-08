#!/usr/bin/env bash

set -e

SCRIPT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "$SCRIPT_PATH/.."

if $(curl --output /dev/null --silent --head --fail http://localhost:8080)
then
    echo "ERROR: Streamline is already running. Please STOP it first (port 8080)"
    exit 1;
fi

echo "INFO: Running full reinstall script which drops/recreates database along with migrations\n\n"
echo "This will drop streamline_db are you sure?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) break;;
        No ) exit;;
    esac
done

rm -f hortonworks-streamline-0.1.0-beta.zip
rm -Rf hortonworks-streamline-0.1.0-beta

mysql -u root -e "drop database streamline_db"
mysql -u root -e "create database streamline_db"
mysql -u root -e "Grant all privileges on streamline_db.* to 'streamline_user'@'localhost' identified by 'streamline_password'"

mvn install -DskipTests -Dfindbugs.skip=true
cd streamline-dist
mvn -DskipTests clean package
cd ..
cp streamline-dist/target/hortonworks-streamline-0.1.0-beta.zip .
unzip hortonworks-streamline-0.1.0-beta.zip
cd hortonworks-streamline-0.1.0-beta
./bootstrap/bootstrap-storage.sh migrate
cerberus -s schema-service
./bin/streamline start

echo 'waiting for UI...'
until $(curl --output /dev/null --silent --head --fail http://localhost:8080); do
  >&2 printf "."
  sleep 5
done
echo ""

./bootstrap/bootstrap.sh migrate

/usr/bin/open "http://localhost:8080"
echo "\n\nStreamline running at: http://localhost:8080"
echo "To stop, type './hortonworks-streamline-0.1.0-beta/bin/streamline stop'"
