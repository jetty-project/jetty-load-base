#!/bin/bash
targetHost=$1

if [ $# -eq 0 ]
  then
    targetHost=localhost
fi

for ((i=0;i<1000;i++));
do
    u=$(printf "user%04d.surname" $i)
    U=$(printf "User%04d Surname" $i)
    N=$(printf "10000%04d" $i)
    curl -s -X POST \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"$u@mail.example.com\",\"fullName\":\"$U\",\"phone\":\"$N\"}" \
      http://$targetHost:8080/api/contact > /dev/null
done
