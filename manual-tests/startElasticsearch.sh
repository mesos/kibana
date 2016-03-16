#!/bin/bash
ssh -i $KEY ubuntu@$SLAVE0 sudo docker run -d -p 9200:9200 elasticsearch:latest
