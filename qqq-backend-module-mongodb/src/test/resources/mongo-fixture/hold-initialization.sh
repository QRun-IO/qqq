#!/bin/bash
# Owned fixture gate, sourced by mongo:7.0 after creating the root user.
echo QQQ_OWNED_INIT_GATE_ENTERED
while [ ! -f /tmp/qqq-owned-init-release ]; do
   sleep 0.1
done
