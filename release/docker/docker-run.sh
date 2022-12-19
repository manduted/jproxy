#!/bin/bash
docker run --name jproxy \
-v /docker/jproxy/config:/app/config \
-e TZ=Asia/Shanghai \
-e "JAVA_OPTS=-Xms256m -Xmx256m" \
--net=host \
--restart unless-stopped \
-d manduted/jproxy:latest
