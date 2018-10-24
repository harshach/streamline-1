package com.hortonworks.streamline.webservice.health;

import com.codahale.metrics.health.HealthCheck;

public class DummyHealthCheck extends HealthCheck {

    @Override
    protected Result check() {
        return Result.healthy();
    }
}

