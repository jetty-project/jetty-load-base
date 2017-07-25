#!/bin/bash

for ((i=0;i<1000;i++));
do
    u=$(printf "user%04d.i.surname" $i)
    U=$(printf "User%04d I Surname" $i)
    N=$(printf "10000%04d" $i)
    curl -X POST \
      -H "Content-Type:application/json" \
      -d "{\"email\":\"$u@mail.example.com\",\"fullName\":\"$U\",\"phone\":\"$N\"}" \
      http://localhost:8080/api/contact
done
