package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;
import software.constructs.*;

public class ClusterStack extends Stack  {

    private final Cluster cluster;

    public ClusterStack(final Construct scope, final String id, final StackProps props,
            ClusterStackProps clusterStackProps) {
        super(scope, id, props);

        this.cluster = new Cluster(this, "Cluster", ClusterProps.builder()
                .clusterName("ECommerce")
                .vpc(clusterStackProps.vpc())
                .containerInsights(true)
                .build());
    }

    public Cluster getCluster() {
        return cluster;
    }
}

record ClusterStackProps(Vpc vpc){}
