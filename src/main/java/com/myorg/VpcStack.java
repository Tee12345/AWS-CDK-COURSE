package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;

public class VpcStack extends Stack {

    private final Vpc vpc;

    public VpcStack(final Construct scope, final String id, final StackProps props) {
          super(scope, id, props);

          this.vpc = new Vpc(this, "Vpc", VpcProps.builder()
                  .vpcName("ECommerceVPC")
                  .maxAzs(2)
                  //Do not do the below in Production!!!
                  //.natGateways(0)
                  .build());
    }

    public Vpc getVpc() {
        return vpc;
    }
}